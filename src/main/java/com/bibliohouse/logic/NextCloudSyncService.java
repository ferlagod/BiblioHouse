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
 * SIN NINGUNA GARANTÍA; sin incluso la garantía implícita de
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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio para sincronizar la base de datos local de BiblioHouse con un
 * servidor NextCloud mediante WebDAV. Detecta automáticamente la ruta WebDAV
 * correcta y gestiona la subida/bajada de archivos JSON y portadas de forma
 * incremental.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class NextCloudSyncService {

    private static final Logger LOGGER = Logger.getLogger(NextCloudSyncService.class.getName());

    private static final String[] DB_FILES = {
        "biblioteca.json",
        "prestamos.json",
        "socios.json",
        "estanterias.json",
        "deseos.json"
    };

    private static final String[] DAV_CANDIDATES = {
        "/remote.php/dav/files/{user}/",
        "/remote.php/webdav/"
    };

    private final String serverUrl;
    private final String username;
    private final String password;
    private String davBaseUrl;
    private final String usernameEncoded;

    /**
     * Crea una nueva instancia del servicio de sincronización.
     *
     * @param serverUrl URL del servidor NextCloud.
     * @param username Usuario de NextCloud.
     * @param password Contraseña de NextCloud.
     * @throws IllegalArgumentException Si algún parámetro es nulo/vacío o si se
     * usa HTTP en servidores externos.
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

        String extractedUrl = extractBaseUrl(serverUrl.trim());

        // OWASP A02: Bloquear credenciales en texto plano sobre HTTP
        if (extractedUrl.startsWith("http://") && !extractedUrl.contains("localhost") && !extractedUrl.contains("127.0.0.1")) {
            throw new IllegalArgumentException("Se requiere HTTPS para conexiones NextCloud externas (evita robo de credenciales).");
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

    /**
     * Detecta el endpoint WebDAV correcto probando las rutas candidatas.
     *
     * @param sardine Cliente Sardine para realizar las peticiones.
     * @return URL base del endpoint WebDAV encontrado.
     * @throws IOException Si no se puede conectar a ninguna de las rutas
     * candidatas.
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

    private String buildRemoteFolderUrl(String davBase) {
        return davBase + "BiblioHouse/";
    }

    private String buildRemoteFileUrl(String davBase, String fileName) {
        return buildRemoteFolderUrl(davBase) + fileName;
    }

    /**
     * Prueba la conexión con NextCloud.
     *
     * @return true si la conexión es exitosa, false en caso contrario.
     */
    public boolean testConexion() {
        return testConexionConMensaje() == null;
    }

    /**
     * Prueba la conexión con NextCloud y devuelve un mensaje de error si falla.
     *
     * @return Mensaje de error si la conexión falla, null si es exitosa.
     */
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

    /**
     * Sube la base de datos local a NextCloud. Crea los directorios necesarios
     * y sincroniza archivos JSON y portadas de forma incremental.
     *
     * @param localDir Directorio local donde están los archivos a sincronizar.
     * @throws IOException Si ocurre un error durante la subida.
     */
    public void subirBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String davBase = resolverDavBase(sardine);
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);
            String remoteCoversUrl = remoteFolderUrl + "covers/";

            crearDirectorioSiNoExiste(sardine, remoteFolderUrl);
            crearDirectorioSiNoExiste(sardine, remoteCoversUrl);

            // Sincronizar archivos JSON
            for (String fileName : DB_FILES) {
                File localFile = new File(localDir, fileName);
                if (localFile.exists() && localFile.length() > 0) {
                    String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                    byte[] fileData = Files.readAllBytes(localFile.toPath());
                    sardine.put(remoteFileUrl, fileData, "application/json");
                    LOGGER.log(Level.INFO, "JSON sincronizado: {0}", fileName);
                }
            }

            // Sincronización incremental de portadas
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
                            byte[] imgData = Files.readAllBytes(portada.toPath());
                            sardine.put(remoteFileUrl, imgData, "image/jpeg");
                            LOGGER.log(Level.INFO, "Nueva portada subida (incremental): {0}", portada.getName());
                        }
                    }
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IOException("Error de red con NextCloud: " + msg, e);
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Crea un directorio remoto si no existe.
     *
     * @param sardine Cliente Sardine.
     * @param url URL del directorio a crear.
     */
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

    /**
     * Descarga la base de datos desde NextCloud. Crea backups de los archivos
     * locales existentes antes de sobrescribirlos.
     *
     * @param localDir Directorio local donde guardar los archivos descargados.
     * @throws IOException Si ocurre un error durante la descarga.
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

            // Sincronización incremental de portadas (descarga)
            String remoteCoversUrl = buildRemoteFolderUrl(davBase) + "covers/";
            File carpetaLocalCovers = new File(localDir, "covers");

            if (!carpetaLocalCovers.exists()) {
                carpetaLocalCovers.mkdirs();
            }

            if (sardine.exists(remoteCoversUrl)) {
                List<DavResource> remoteCovers = sardine.list(remoteCoversUrl);
                for (DavResource res : remoteCovers) {
                    if (res.isDirectory()) {
                        continue;
                    }

                    String coverName = res.getName();
                    File localCover = new File(carpetaLocalCovers, coverName);

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
