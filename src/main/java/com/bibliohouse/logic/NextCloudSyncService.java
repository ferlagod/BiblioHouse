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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona la sincronización de la base de datos local de
 * BiblioHouse con un servidor NextCloud mediante el protocolo WebDAV.
 *
 * Detecta automáticamente cuál de las dos rutas WebDAV estándar de NextCloud
 * está disponible.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
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
        "deseos.json"
    };

    /**
     * Posibles rutas WebDAV raíz en un servidor NextCloud. Se prueban en orden;
     * la primera que responda correctamente se adopta.
     */
    private static final String[] DAV_CANDIDATES = {
        "/remote.php/dav/files/{user}/", // DAV moderno (NC ≥ 9)
        "/remote.php/webdav/" // WebDAV clásico
    };

    private final String serverUrl;
    private final String username;
    private final String password;
    private String davBaseUrl;
    private final String usernameEncoded;

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

        String extractedUrl = extractBaseUrl(serverUrl.trim());

        // OWASP A02: Cryptographic Failures. Bloquear credenciales en texto plano sobre HTTP
        if (extractedUrl.startsWith("http://") && !extractedUrl.contains("localhost") && !extractedUrl.contains("127.0.0.1")) {
            throw new IllegalArgumentException("Seguridad: Se requiere HTTPS para conexiones NextCloud externas (evita robo de credenciales).");
        }

        this.serverUrl = extractedUrl;
        this.username = username.trim();
        this.usernameEncoded = encodeUrlSegment(this.username);
        this.password = password;
    }

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
            String s = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            int idx = s.indexOf("/remote.php");
            if (idx == -1) {
                idx = s.indexOf("/nextcloud");
            }
            return idx > 0 ? s.substring(0, idx) : s;
        }
    }

    private static String encodeUrlSegment(String segment) {
        try {
            return new java.net.URI(null, null, segment, null).getRawPath();
        } catch (Exception e) {
            return segment;
        }
    }

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

    private String buildRemoteFolderUrl(String davBase) {
        return davBase + "BiblioHouse/";
    }

    private String buildRemoteFileUrl(String davBase, String fileName) {
        return buildRemoteFolderUrl(davBase) + fileName;
    }

    public boolean testConexion() {
        return testConexionConMensaje() == null;
    }

    public String testConexionConMensaje() {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            davBaseUrl = null;
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

    public void subirBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String davBase = resolverDavBase(sardine);
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);
            String remoteCoversUrl = remoteFolderUrl + "covers/";

            // 1. Asegurar directorios en la nube
            crearDirectorioSiNoExiste(sardine, remoteFolderUrl);
            crearDirectorioSiNoExiste(sardine, remoteCoversUrl);

            // 2. Sincronizar archivos JSON 
            for (String fileName : DB_FILES) {
                File localFile = new File(localDir, fileName);
                if (localFile.exists() && localFile.length() > 0) {
                    String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);

                    // FIX NEXTCLOUD: Usamos readAllBytes() en vez de InputStream.
                    // Al cargar el byte[], Sardine le envía a NextCloud el Content-Length exacto. 
                    // Si usamos InputStream, envía datos "chunked" y el servidor lo aborta silenciosamente.
                    byte[] fileData = Files.readAllBytes(localFile.toPath());
                    sardine.put(remoteFileUrl, fileData, "application/json");
                    LOGGER.log(Level.INFO, "JSON sincronizado: {0}", fileName);
                }
            }

            // 3. Sincronización INCREMENTAL de portadas
            File carpetaLocalCovers = new File(localDir, "covers");
            if (carpetaLocalCovers.exists() && carpetaLocalCovers.isDirectory()) {
                File[] portadas = carpetaLocalCovers.listFiles();
                if (portadas != null) {
                    List<DavResource> resources = sardine.list(remoteCoversUrl);
                    Set<String> nombresEnRemoto = resources.stream()
                            .filter(r -> r.getName() != null)
                            .map(DavResource::getName)
                            .collect(Collectors.toSet());

                    for (File portada : portadas) {
                        if (portada.isFile() && !portada.getName().startsWith(".") && !nombresEnRemoto.contains(portada.getName())) {
                            String remoteFileUrl = remoteCoversUrl + portada.getName();

                            // FIX NEXTCLOUD: También se protegen las imágenes enviando el Content-Length
                            byte[] imgData = Files.readAllBytes(portada.toPath());
                            sardine.put(remoteFileUrl, imgData, "image/jpeg");

                            LOGGER.log(Level.INFO, "Nueva portada subida (incremental): {0}", portada.getName());
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Protección contra errores silenciosos sin mensaje de Apache HTTP (EOFException, SocketException...)
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IOException("Error de red con NextCloud: " + msg, e);
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
        }
    }

    private void crearDirectorioSiNoExiste(Sardine sardine, String url) {
        try {
            if (!sardine.exists(url)) {
                sardine.createDirectory(url);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear/verificar directorio remoto ({0}): {1}",
                    new Object[]{url, e.getMessage()});
        }
    }

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
            // 4. Sincronización INCREMENTAL de portadas (Descarga)
            String remoteCoversUrl = buildRemoteFolderUrl(davBase) + "covers/";
            File carpetaLocalCovers = new File(localDir, "covers");

            if (!carpetaLocalCovers.exists()) {
                carpetaLocalCovers.mkdirs();
            }

            if (sardine.exists(remoteCoversUrl)) {
                List<DavResource> remoteCovers = sardine.list(remoteCoversUrl);
                for (DavResource res : remoteCovers) {
                    // Ignoramos el propio directorio
                    if (res.isDirectory()) {
                        continue;
                    }

                    String coverName = res.getName();
                    File localCover = new File(carpetaLocalCovers, coverName);

                    // Solo descargamos si no la tenemos en local
                    if (!localCover.exists()) {
                        String fileUrl = remoteCoversUrl + coverName;
                        try (InputStream in = sardine.get(fileUrl); FileOutputStream out = new FileOutputStream(localCover)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            LOGGER.log(Level.INFO, "Portada descargada desde NextCloud: {0}", coverName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IOException("Error al descargar desde NextCloud: " + msg, e);
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
        }
    }
}
