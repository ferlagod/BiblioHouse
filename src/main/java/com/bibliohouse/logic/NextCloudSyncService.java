/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 *
 * Este programa se distribuye con la esperanza de que sea útil, pero
 * SIN NINGUNA GARANTÍA; sin siquiera la garantía implícita de
 * COMERCIABILIDAD o APTITUD PARA UN PROPÓSITO PARTICULAR. Vea la
 * Licencia Pública General de GNU para más detalles.
 *
 * Usted debería haber recibido una copia de la Licencia Pública General de GNU
 * junto con este programa. Si no es así, vea <https://www.gnu.org/licenses/>.
 */
package com.bibliohouse.logic;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona la sincronización de la base de datos local de
 * BiblioHouse con un servidor NextCloud mediante el protocolo WebDAV.
 *
 * Detecta automáticamente cuál de las dos rutas WebDAV estándar de NextCloud
 * está disponible:
 *
 *
 * @author Fernando Lago
 * @version 1.6
 */
public class NextCloudSyncService {

    private static final Logger LOGGER = Logger.getLogger(NextCloudSyncService.class.getName());

    /**
     * Nombres de los archivos JSON que componen la base de datos local.
     */
    private static final String[] DB_FILES = {
        "biblioteca.json",
        "prestamos.json",
        "socios.json",
        "estanterias.json",
        "deseos.json",
        "portadas.zip"
    };

    /**
     * Posibles rutas WebDAV raíz en un servidor NextCloud. Se prueban en orden;
     * la primera que responda correctamente se adopta.
     */
    private static final String[] DAV_CANDIDATES = {
        "/remote.php/dav/files/{user}/", // DAV moderno (NC ≥ 9)
        "/remote.php/webdav/" // WebDAV clásico
    };

    /**
     * URL base del servidor NextCloud, ej. {@code https://cloud.example.com}.
     */
    private final String serverUrl;

    /**
     * Nombre de usuario de NextCloud.
     */
    private final String username;

    /**
     * Contraseña (preferiblemente una «app password» de NextCloud).
     */
    private final String password;

    /**
     * URL raíz WebDAV detectada automáticamente (incluye barra final). Es
     * {@code null} hasta que se llame a {@link #resolverDavBase(Sardine)}.
     */
    private String davBaseUrl;

    /**
     * Nombre de usuario de NextCloud codificado para uso en URLs de path.
     */
    private final String usernameEncoded;

    /**
     * Construye un nuevo servicio de sincronización con NextCloud.
     *
     * @param serverUrl URL del servidor NextCloud.
     * @param username Nombre de usuario de NextCloud.
     * @param password Contraseña o app password de NextCloud.
     * @throws IllegalArgumentException si alguno de los parámetros es nulo o
     * vacío.
     */
    public NextCloudSyncService(String serverUrl, String username, String password) {
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("La URL del servidor no puede ser nula o vacía.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario de NextCloud no puede ser nulo o vacío.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña de NextCloud no puede ser nula o vacía.");
        }

        // Normalizar la URL: extraer solo esquema + host + puerto, ignorando
        // cualquier ruta WebDAV que el usuario haya pegado por error.
        String extractedUrl = extractBaseUrl(serverUrl.trim());
        
        // OWASP A02: Cryptographic Failures. Bloquear credenciales en texto plano sobre HTTP
        if (extractedUrl.startsWith("http://") && !extractedUrl.contains("localhost") && !extractedUrl.contains("127.0.0.1")) {
            throw new IllegalArgumentException("Seguridad: Se requiere HTTPS para conexiones NextCloud externas (evita robo de credenciales).");
        }
        
        this.serverUrl = extractedUrl;
        this.username = username.trim();
        // Codificar username para paths de URL (@ → %40, espacios → %20, etc.)
        this.usernameEncoded = encodeUrlSegment(this.username);
        this.password = password;
    }

    /**
     * Extrae la URL base (esquema + host + puerto) descartando cualquier path.
     *
     */
    private static String extractBaseUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            int port = uri.getPort();
            String base = uri.getScheme() + "://" + uri.getHost();
            if (port != -1) {
                base += ":" + port;
            }
            return base;
        } catch (java.net.URISyntaxException e) {
            // Si la URL no es válida, eliminar al menos la barra final y rutas conocidas
            String s = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            int idx = s.indexOf("/remote.php");
            if (idx == -1) {
                idx = s.indexOf("/nextcloud");
            }
            return idx > 0 ? s.substring(0, idx) : s;
        }
    }

    /**
     * Codifica un segmento de path de URL (RFC 3986). Convierte caracteres como
     * {@code @} en {@code %40}.
     */
    private static String encodeUrlSegment(String segment) {
        try {
            // URLEncoder usa codificación de formulario (+) — reemplazamos por %20
            return java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados de utilidad
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Detecta y cachea la URL base WebDAV correcta probando los candidatos.
     *
     * @param sardine Cliente Sardine ya inicializado con credenciales.
     * @return URL base WebDAV con barra final.
     * @throws IOException si ninguno de los candidatos responde correctamente.
     */
    private String resolverDavBase(Sardine sardine) throws IOException {
        if (davBaseUrl != null) {
            return davBaseUrl;
        }

        for (String candidate : DAV_CANDIDATES) {
            String url = serverUrl + candidate.replace("{user}", usernameEncoded);
            try {
                sardine.list(url);
                davBaseUrl = url;
                LOGGER.log(Level.INFO, "Endpoint WebDAV detectado: {0}", davBaseUrl);
                return davBaseUrl;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Candidato WebDAV no disponible ({0}): {1}",
                        new Object[]{url, e.getMessage()});
            }
        }

        throw new IOException(
                "No se pudo conectar a NextCloud. Comprueba la URL y el usuario.\n"
                + "Rutas probadas:\n"
                + "  · " + serverUrl + DAV_CANDIDATES[0].replace("{user}", usernameEncoded) + "\n"
                + "  · " + serverUrl + DAV_CANDIDATES[1]);
    }

    /**
     * Construye la URL de la carpeta remota para BiblioHouse. Añade el nombre
     * de la carpeta "BiblioHouse/" a la URL base proporcionada.
     *
     * @param davBase URL base del servidor WebDAV.
     * @return URL completa de la carpeta remota de BiblioHouse.
     */
    private String buildRemoteFolderUrl(String davBase) {
        return davBase + "BiblioHouse/";
    }

    /**
     * Construye la URL completa de un archivo remoto en BiblioHouse. Combina la
     * URL base, la carpeta de BiblioHouse y el nombre del archivo.
     *
     * @param davBase URL base del servidor WebDAV.
     * @param fileName nombre del archivo a añadir a la URL.
     * @return URL completa del archivo remoto.
     */
    private String buildRemoteFileUrl(String davBase, String fileName) {
        return buildRemoteFolderUrl(davBase) + fileName;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Verifica que las credenciales son correctas y que el servidor es
     * accesible.
     *
     * @return {@code true} si la conexión es correcta, {@code false} en caso
     * contrario.
     */
    public boolean testConexion() {
        return testConexionConMensaje() == null;
    }

    /**
     * Verifica la conexión y devuelve un mensaje de error descriptivo si falla.
     *
     * @return {@code null} si la conexión es correcta, o un String con el
     * error.
     */
    public String testConexionConMensaje() {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            davBaseUrl = null; // forzar re-detección en cada test
            resolverDavBase(sardine);
            LOGGER.log(Level.INFO, "Test de conexión NextCloud exitoso: {0}", davBaseUrl);
            return null;
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.log(Level.WARNING, "Test de conexión NextCloud fallido: {0}", msg);
            return msg;
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Sube los archivos JSON de la base de datos local al servidor NextCloud.
     * Detecta el endpoint WebDAV automáticamente y crea la carpeta
     * {@code BiblioHouse/} si no existe.
     *
     * @param localDir Ruta al directorio local del usuario.
     * @throws IOException si se produce un error de red o de E/S.
     */
    public void subirBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String davBase = resolverDavBase(sardine);
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);
            String remoteCoversUrl = remoteFolderUrl + "covers/";

            // 1. Asegurar directorios en la nube
            crearDirectorioSiNoExiste(sardine, remoteFolderUrl);
            crearDirectorioSiNoExiste(sardine, remoteCoversUrl);

            // 2. Sincronizar archivos JSON (Se suben siempre porque cambian constantemente)
            for (String fileName : DB_FILES) {
                File localFile = new File(localDir, fileName);
                if (localFile.exists()) {
                    String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                    // Usamos put con InputStream para archivos que podrían ser algo más grandes
                    try (InputStream fis = new FileInputStream(localFile)) {
                        sardine.put(remoteFileUrl, fis, "application/json");
                    }
                    LOGGER.log(Level.INFO, "JSON sincronizado: {0}", fileName);
                }
            }

            // 3. Sincronización INCREMENTAL de portadas
            // Usamos la carpeta "covers" que es la estándar que definimos
            File carpetaLocalCovers = new File(localDir, "covers");
            if (carpetaLocalCovers.exists() && carpetaLocalCovers.isDirectory()) {
                File[] portadas = carpetaLocalCovers.listFiles();
                if (portadas != null) {
                    // Listamos la nube una sola vez para comparar
                    List<DavResource> resources = sardine.list(remoteCoversUrl);
                    Set<String> nombresEnRemoto = resources.stream()
                            .map(DavResource::getName)
                            .collect(Collectors.toSet());

                    for (File portada : portadas) {
                        // Solo subimos si es archivo, no es oculto y NO está ya en la nube
                        if (portada.isFile() && !portada.getName().startsWith(".") && !nombresEnRemoto.contains(portada.getName())) {
                            String remoteFileUrl = remoteCoversUrl + portada.getName();
                            try (InputStream fis = new FileInputStream(portada)) {
                                sardine.put(remoteFileUrl, fis, "image/jpeg");
                            }
                            LOGGER.log(Level.INFO, "Nueva portada subida (incremental): {0}", portada.getName());
                        }
                    }
                }
            }
        } finally {
            sardine.shutdown();
        }
    }

    /**
     * Método auxiliar para evitar errores 405 si la carpeta ya existe
     */
    private void crearDirectorioSiNoExiste(Sardine sardine, String url) {
        try {
            if (!sardine.exists(url)) {
                sardine.createDirectory(url);
            }
        } catch (IOException e) {
            // Puede ser un 405 "Method Not Allowed" si la carpeta ya existe en algunos servidores.
            // No es crítico, pero lo registramos para diagnóstico.
            LOGGER.log(Level.WARNING, "No se pudo crear/verificar directorio remoto ({0}): {1}",
                    new Object[]{url, e.getMessage()});
        }
    }

    /**
     * Descarga los archivos JSON desde NextCloud y sobreescribe los locales.
     * Crea un backup {@code .bak} de cada archivo antes de sobreescribirlo.
     *
     * @param localDir Ruta al directorio local del usuario.
     * @throws IOException si se produce un error de red o de E/S.
     */
    public void descargarBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String davBase = resolverDavBase(sardine);
            for (String fileName : DB_FILES) {
                String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                if (!sardine.exists(remoteFileUrl)) {
                    LOGGER.log(Level.FINE, "Archivo remoto no encontrado, omitiendo descarga: {0}", fileName);
                    continue;
                }

                File localFile = new File(localDir, fileName);
                if (localFile.exists()) {
                    File backup = new File(localDir, fileName + ".bak");
                    Files.copy(localFile.toPath(), backup.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.FINE, "Backup creado: {0}.bak", fileName);
                }

                try (InputStream in = sardine.get(remoteFileUrl); FileOutputStream out = new FileOutputStream(localFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    LOGGER.log(Level.INFO, "Descargado desde NextCloud: {0}", fileName);
                }
            }
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
        }
    }

}
