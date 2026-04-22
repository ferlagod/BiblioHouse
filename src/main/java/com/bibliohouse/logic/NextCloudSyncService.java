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

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

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
        this.serverUrl = extractBaseUrl(serverUrl.trim());
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

            // 1. Asegurar que la carpeta BiblioHouse existe en la nube
            try {
                sardine.createDirectory(remoteFolderUrl);
                LOGGER.log(Level.INFO, "Carpeta creada en NextCloud: {0}", remoteFolderUrl);
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("405") || msg.contains("Method Not Allowed") || msg.contains("301")) {
                    LOGGER.log(Level.FINE, "Carpeta BiblioHouse ya existía (ignorado).");
                } else {
                    LOGGER.log(Level.WARNING, "Respuesta inesperada al crear carpeta: {0}", msg);
                }
            }

            // 2. EMPAQUETAR PORTADAS: Crea el archivo covers.zip en la carpeta local
            // Este paso es CRÍTICO para que las fotos viajen entre PCs
            empaquetarPortadas(localDir);

            // 3. Definir la lista de archivos a subir (JSONs + el nuevo ZIP de portadas)
            List<String> archivosParaSubir = new ArrayList<>(List.of(DB_FILES));
            archivosParaSubir.add("covers.zip"); // Añadimos el paquete de fotos

            for (String fileName : archivosParaSubir) {
                File localFile = new File(localDir, fileName);
                if (!localFile.exists()) {
                    LOGGER.log(Level.FINE, "Archivo local no encontrado, omitiendo: {0}", fileName);
                    continue;
                }

                String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                try {
                    byte[] data = Files.readAllBytes(localFile.toPath());

                    // Definimos el tipo de contenido según la extensión
                    String contentType = fileName.endsWith(".zip") ? "application/zip" : "application/json";

                    sardine.put(remoteFileUrl, data, contentType);
                    LOGGER.log(Level.INFO, "Sincronizado con éxito: {0}", fileName);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error al subir {0}: {1}", new Object[]{fileName, e.getMessage()});
                    // No lanzamos excepción aquí para que si falla una foto, al menos suba los JSON
                }
            }

            // 4. LIMPIEZA: Borramos el ZIP local después de subirlo para no ocupar espacio doble
            File zipTemporal = new File(localDir, "covers.zip");
            if (zipTemporal.exists()) {
                zipTemporal.delete();
            }

        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
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

    /**
     * Empaqueta todas las portadas de libros en un archivo ZIP. Solo incluye
     * archivos visibles (ignora subcarpetas y archivos ocultos como .DS_Store).
     *
     * @param localDir Ruta del directorio local donde se encuentra la carpeta
     * "portadas".
     */
    private void empaquetarPortadas(String localDir) {
        File dirPortadas = new File(localDir, "portadas");
        if (!dirPortadas.exists() || !dirPortadas.isDirectory()) {
            return;
        }

        File zipFile = new File(localDir, "portadas.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File[] files = dirPortadas.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Ignorar subcarpetas y archivos ocultos del sistema como .DS_Store
                    if (file.isFile() && !file.getName().startsWith(".")) {
                        zos.putNextEntry(new ZipEntry(file.getName()));
                        Files.copy(file.toPath(), zos);
                        zos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al empaquetar portadas: " + e.getMessage());
        }
    }

    /**
     * Desempaqueta el archivo ZIP de portadas en la carpeta "portadas". Si la
     * carpeta no existe, la crea. Sobrescribe los archivos existentes.
     *
     * @param localDir Ruta del directorio local donde se encuentra el archivo
     * "portadas.zip".
     */
    private void desempaquetarPortadas(String localDir) {
        File zipFile = new File(localDir, "portadas.zip");
        if (!zipFile.exists()) {
            return;
        }

        File dirPortadas = new File(localDir, "portadas");
        if (!dirPortadas.exists()) {
            dirPortadas.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File target = new File(dirPortadas, entry.getName());
                Files.copy(zis, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al desempaquetar portadas: " + e.getMessage());
        }
    }
}
