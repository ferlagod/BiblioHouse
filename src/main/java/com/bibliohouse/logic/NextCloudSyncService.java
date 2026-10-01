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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class NextCloudSyncService {

    private static final Logger LOGGER = Logger.getLogger(NextCloudSyncService.class.getName());

    private static final String[] DB_FILES = {
        "biblioteca.json",
        "prestamos.json",
        "socios.json",
        "estanterias.json",
        "deseos.json",
        "progreso_lectura.json"
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
        } catch (URISyntaxException e) {
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
                if (portadas != null && portadas.length > 0) {
                    Set<String> nombresEnRemoto = new java.util.HashSet<>();
                    try {
                        List<DavResource> resources = sardine.list(remoteCoversUrl);
                        for (DavResource r : resources) {
                            if (r.getName() != null) {
                                nombresEnRemoto.add(r.getName().toLowerCase());
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "No se pudo listar directorio remoto de covers, verificando creación: {0}", ex.getMessage());
                        crearDirectorioSiNoExiste(sardine, remoteCoversUrl);
                    }

                    for (File portada : portadas) {
                        if (portada.isFile() && !portada.getName().startsWith(".") && portada.length() > 0) {
                            String nombre = portada.getName();
                            if (!nombresEnRemoto.contains(nombre.toLowerCase())) {
                                try {
                                    String remoteFileUrl = remoteCoversUrl + encodeUrlSegment(nombre);
                                    byte[] imgData = Files.readAllBytes(portada.toPath());
                                    sardine.put(remoteFileUrl, imgData, determinarMimeTypeImagen(nombre));
                                    LOGGER.log(Level.INFO, "Nueva portada subida (incremental): {0}", nombre);
                                } catch (Exception ex) {
                                    LOGGER.log(Level.WARNING, "Error al subir portada individual ({0}): {1}", new Object[]{nombre, ex.getMessage()});
                                }
                            }
                        }
                    }
                }
            }

            // Sincronización incremental de ebooks
            String remoteEbooksUrl = remoteFolderUrl + "ebooks/";
            crearDirectorioSiNoExiste(sardine, remoteEbooksUrl);

            File carpetaLocalEbooks = new File(localDir, "ebooks");
            if (carpetaLocalEbooks.exists() && carpetaLocalEbooks.isDirectory()) {
                File[] ebooks = carpetaLocalEbooks.listFiles();
                if (ebooks != null && ebooks.length > 0) {
                    Set<String> nombresEbooksEnRemoto = new java.util.HashSet<>();
                    try {
                        List<DavResource> resourcesEbooks = sardine.list(remoteEbooksUrl);
                        for (DavResource r : resourcesEbooks) {
                            if (r.getName() != null) {
                                nombresEbooksEnRemoto.add(r.getName().toLowerCase());
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "No se pudo listar directorio remoto de ebooks: {0}", ex.getMessage());
                    }

                    for (File ebook : ebooks) {
                        if (ebook.isFile() && !ebook.getName().startsWith(".") && ebook.length() > 0) {
                            String nombreEbook = ebook.getName();
                            if (!nombresEbooksEnRemoto.contains(nombreEbook.toLowerCase())) {
                                try {
                                    String remoteFileUrl = remoteEbooksUrl + encodeUrlSegment(nombreEbook);
                                    byte[] data = Files.readAllBytes(ebook.toPath());
                                    sardine.put(remoteFileUrl, data, "application/octet-stream");
                                    LOGGER.log(Level.INFO, "Nuevo ebook subido (incremental): {0}", nombreEbook);
                                } catch (Exception ex) {
                                    LOGGER.log(Level.WARNING, "Error al subir ebook individual ({0}): {1}", new Object[]{nombreEbook, ex.getMessage()});
                                }
                            }
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

    private static String determinarMimeTypeImagen(String nombre) {
        if (nombre == null) return "image/jpeg";
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
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
     * Comprueba si el recurso remoto de NextCloud es más reciente que el archivo local correspondiente.
     * Si el archivo local no existe, se considera que el remoto debe descargarse.
     *
     * @param remoteModified Fecha de modificación remota (DavResource.getModified()).
     * @param localFile      Archivo físico local.
     * @return true si el remoto es más reciente que el local (con un margen de tolerancia de 2 segundos),
     *         o si el archivo local no existe. false en caso contrario.
     */
    static boolean esRecursoRemotoMasReciente(Date remoteModified, File localFile) {
        if (localFile == null || !localFile.exists()) {
            return true;
        }
        if (remoteModified == null) {
            return false;
        }
        long remoteTime = remoteModified.getTime();
        long localTime = localFile.lastModified();
        // Margen de 2000 ms para tolerar diferencias de precisión en sistemas de archivos (FAT32, ext4, etc.)
        return (remoteTime - localTime) > 2000;
    }

    /**
     * Consulta si la versión remota de los archivos JSON de base de datos en NextCloud
     * es más reciente que la versión local (o si faltan archivos o portadas).
     *
     * @param localDir Directorio local donde se encuentran los archivos de datos.
     * @return true si existen cambios remotos más recientes que los locales, false si está al día.
     * @throws IOException Si ocurre un error de comunicación con NextCloud.
     */
    public boolean esRemotoMasReciente(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String davBase = resolverDavBase(sardine);
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);

            if (!sardine.exists(remoteFolderUrl)) {
                return false;
            }

            List<DavResource> remoteResources = sardine.list(remoteFolderUrl);
            Map<String, DavResource> remoteFilesMap = new HashMap<>();
            for (DavResource res : remoteResources) {
                if (!res.isDirectory() && res.getName() != null) {
                    remoteFilesMap.put(res.getName().toLowerCase(), res);
                }
            }

            // 1. Comprobar si algún archivo de base de datos es más reciente en NextCloud
            for (String dbFile : DB_FILES) {
                DavResource remoteRes = remoteFilesMap.get(dbFile.toLowerCase());
                if (remoteRes != null) {
                    File localFile = new File(localDir, dbFile);
                    if (esRecursoRemotoMasReciente(remoteRes.getModified(), localFile)) {
                        LOGGER.log(Level.INFO, "Cambio remoto detectado en {0}: remoto ({1}), local ({2})",
                                new Object[]{dbFile, remoteRes.getModified(),
                                        localFile.exists() ? new Date(localFile.lastModified()) : "no existe"});
                        return true;
                    }
                }
            }

            // 2. Comprobar si hay portadas remotas pendientes de descargar
            String remoteCoversUrl = remoteFolderUrl + "covers/";
            if (sardine.exists(remoteCoversUrl)) {
                List<DavResource> remoteCovers = sardine.list(remoteCoversUrl);
                File localCovers = new File(localDir, "covers");
                for (DavResource c : remoteCovers) {
                    String coverName = c.getName();
                    if (coverName == null || c.isDirectory() || coverName.equalsIgnoreCase("covers") || coverName.startsWith(".")) {
                        continue;
                    }
                    File localCoverFile = new File(localCovers, coverName);
                    if (!localCoverFile.exists()) {
                        LOGGER.log(Level.INFO, "Nueva portada remota detectada sin descargar: {0}", coverName);
                        return true;
                    }
                }
            }

            return false;
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Sincroniza desde NextCloud (Pull) si la versión remota de la base de datos
     * es más reciente que la versión local (o si faltan portadas).
     *
     * @param localDir Directorio local donde se encuentran los datos del usuario.
     * @return true si se descargaron cambios remotos, false si la versión local ya estaba al día.
     * @throws IOException Si ocurre un error de comunicación con NextCloud.
     */
    public boolean sincronizarSiRemotoMasReciente(String localDir) throws IOException {
        if (!esRemotoMasReciente(localDir)) {
            LOGGER.info("NextCloud Pull: La biblioteca local ya está al día con la nube.");
            return false;
        }

        LOGGER.info("NextCloud Pull: Descargando versión remota más reciente...");
        descargarBaseDatos(localDir);
        return true;
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
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);

            Map<String, DavResource> remoteFilesMap = new HashMap<>();
            try {
                if (sardine.exists(remoteFolderUrl)) {
                    List<DavResource> remoteResources = sardine.list(remoteFolderUrl);
                    for (DavResource res : remoteResources) {
                        if (!res.isDirectory() && res.getName() != null) {
                            remoteFilesMap.put(res.getName().toLowerCase(), res);
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "No se pudo pre-listar recursos remotos: {0}", ex.getMessage());
            }

            for (String fileName : DB_FILES) {
                String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                DavResource remoteRes = remoteFilesMap.get(fileName.toLowerCase());
                if (remoteRes == null && !sardine.exists(remoteFileUrl)) {
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

                if (remoteRes != null && remoteRes.getModified() != null) {
                    localFile.setLastModified(remoteRes.getModified().getTime());
                }
            }

            // Sincronización incremental de portadas (descarga)
            String remoteCoversUrl = buildRemoteFolderUrl(davBase) + "covers/";
            File carpetaLocalCovers = new File(localDir, "covers");

            if (!carpetaLocalCovers.exists()) {
                carpetaLocalCovers.mkdirs();
            }

            try {
                if (sardine.exists(remoteCoversUrl)) {
                    List<DavResource> remoteCovers = sardine.list(remoteCoversUrl);
                    for (DavResource res : remoteCovers) {
                        String coverName = res.getName();
                        // Ignorar el propio directorio o elementos ocultos/vacíos
                        if (coverName == null || coverName.isBlank() || res.isDirectory()
                                || coverName.equalsIgnoreCase("covers") || coverName.startsWith(".")) {
                            continue;
                        }

                        File localCover = new File(carpetaLocalCovers, coverName);

                        if (!localCover.exists()) {
                            try {
                                String fileUrl = remoteCoversUrl + encodeUrlSegment(coverName);
                                try (InputStream in = sardine.get(fileUrl); FileOutputStream out = new FileOutputStream(localCover)) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while ((bytesRead = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                    }
                                    LOGGER.log(Level.INFO, "Portada descargada desde NextCloud: {0}", coverName);
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Error al descargar portada individual ({0}): {1}", new Object[]{coverName, ex.getMessage()});
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error al sincronizar portadas desde NextCloud: {0}", ex.getMessage());
            }

            // Sincronización incremental de ebooks (descarga)
            String remoteEbooksUrl = buildRemoteFolderUrl(davBase) + "ebooks/";
            File carpetaLocalEbooks = new File(localDir, "ebooks");

            if (!carpetaLocalEbooks.exists()) {
                carpetaLocalEbooks.mkdirs();
            }

            try {
                if (sardine.exists(remoteEbooksUrl)) {
                    List<DavResource> remoteEbooks = sardine.list(remoteEbooksUrl);
                    for (DavResource res : remoteEbooks) {
                        String ebookName = res.getName();
                        if (ebookName == null || ebookName.isBlank() || res.isDirectory()
                                || ebookName.equalsIgnoreCase("ebooks") || ebookName.startsWith(".")) {
                            continue;
                        }

                        File localEbook = new File(carpetaLocalEbooks, ebookName);

                        if (!localEbook.exists()) {
                            try {
                                String fileUrl = remoteEbooksUrl + encodeUrlSegment(ebookName);
                                try (InputStream in = sardine.get(fileUrl); FileOutputStream out = new FileOutputStream(localEbook)) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while ((bytesRead = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                    }
                                    LOGGER.log(Level.INFO, "Ebook descargado desde NextCloud: {0}", ebookName);
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Error al descargar ebook individual ({0}): {1}", new Object[]{ebookName, ex.getMessage()});
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error al sincronizar ebooks desde NextCloud: {0}", ex.getMessage());
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
