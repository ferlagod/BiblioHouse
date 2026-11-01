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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para sincronizar la base de datos local de BiblioHouse con un
 * servidor NextCloud mediante WebDAV. Detecta automáticamente la ruta WebDAV
 * correcta y gestiona la subida/bajada de archivos JSON y portadas de forma
 * incremental.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class NextCloudSyncService {

    private static final Logger LOGGER = Logger.getLogger(NextCloudSyncService.class.getName());

    private static final String[] DB_FILES = {
        // Las marcas de borrado van primero: el resto de fusiones las usan
        JsonManager.ARCHIVO_ELIMINADOS,
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
     * Gestor de datos de la interfaz. Si se usa el mismo que la aplicación, las
     * fusiones comparten su bloqueo y no compiten con los guardados de la UI.
     */
    private JsonManager jsonManager;

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
        if (!extractedUrl.startsWith("https://") && !(extractedUrl.startsWith("http://") && esHostLocal(extractedUrl))) {
            throw new IllegalArgumentException("Se requiere HTTPS para conexiones NextCloud externas (evita robo de credenciales).");
        }

        this.serverUrl = extractedUrl;
        this.username = username.trim();
        this.usernameEncoded = encodeUrlSegment(this.username);
        this.password = password;
    }

    /**
     * Indica el gestor de datos que debe usarse al fusionar. Si no se indica
     * (o es de otra carpeta), se crea uno temporal para cada sincronización.
     *
     * @param jsonManager Gestor de datos activo de la aplicación.
     */
    public void setJsonManager(JsonManager jsonManager) {
        this.jsonManager = jsonManager;
    }

    private static boolean esHostLocal(String url) {
        try {
            String host = new java.net.URI(url).getHost();
            return host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")
                    || host.equals("[::1]") || host.equals("::1"));
        } catch (URISyntaxException e) {
            return false;
        }
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
     * Sube la base de datos local a NextCloud. Los archivos de datos se
     * fusionan elemento a elemento con la versión remota antes de subirse, y
     * las portadas y ebooks nuevos se suben de forma incremental.
     *
     * @param localDir Directorio local donde están los archivos a sincronizar.
     * @return {@code true} si la fusión ha modificado los datos locales (hay
     * que recargar la interfaz).
     * @throws IOException Si ocurre un error durante la subida.
     */
    public boolean subirBaseDatos(String localDir) throws IOException {
        return sincronizar(localDir, true);
    }

    /**
     * Descarga la base de datos desde NextCloud. Los archivos de datos se
     * fusionan con los locales (y se vuelven a subir si la nube no tenía los
     * cambios locales); las portadas y ebooks que falten se descargan.
     *
     * @param localDir Directorio local donde guardar los archivos descargados.
     * @return {@code true} si han cambiado los datos locales.
     * @throws IOException Si ocurre un error durante la descarga.
     */
    public boolean descargarBaseDatos(String localDir) throws IOException {
        return sincronizar(localDir, false);
    }

    private boolean sincronizar(String localDir, boolean subida) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        boolean gestorTemporal = jsonManager == null
                || !new File(jsonManager.getRutaDatosUsuario()).getAbsoluteFile().equals(new File(localDir).getAbsoluteFile());
        JsonManager gestor = gestorTemporal ? new JsonManager(localDir) : jsonManager;
        try {
            // Que lo que la interfaz tenga pendiente esté en disco antes de fusionar
            gestor.flushPendientes();

            String davBase = resolverDavBase(sardine);
            String remoteFolderUrl = buildRemoteFolderUrl(davBase);
            String remoteCoversUrl = remoteFolderUrl + "covers/";
            String remoteEbooksUrl = remoteFolderUrl + "ebooks/";

            crearDirectorioSiNoExiste(sardine, remoteFolderUrl);

            Map<String, DavResource> remoteFilesMap = new HashMap<>();
            try {
                for (DavResource res : sardine.list(remoteFolderUrl)) {
                    if (!res.isDirectory() && res.getName() != null) {
                        remoteFilesMap.put(res.getName().toLowerCase(java.util.Locale.ROOT), res);
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "No se pudo pre-listar recursos remotos: {0}", ex.getMessage());
            }

            boolean localCambiado = false;
            for (String fileName : DB_FILES) {
                DavResource remoteRes = remoteFilesMap.get(fileName.toLowerCase(java.util.Locale.ROOT));
                localCambiado |= sincronizarArchivoDatos(sardine, gestor, davBase, localDir, fileName, remoteRes, subida);
            }

            if (subida) {
                crearDirectorioSiNoExiste(sardine, remoteCoversUrl);
                crearDirectorioSiNoExiste(sardine, remoteEbooksUrl);
                subirArchivosNuevos(sardine, new File(localDir, "covers"), remoteCoversUrl, true);
                subirArchivosNuevos(sardine, new File(localDir, "ebooks"), remoteEbooksUrl, false);
            } else {
                localCambiado |= descargarArchivosNuevos(sardine, remoteCoversUrl, new File(localDir, "covers"), "covers");
                localCambiado |= descargarArchivosNuevos(sardine, remoteEbooksUrl, new File(localDir, "ebooks"), "ebooks");
            }
            return localCambiado;
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IOException((subida ? "Error de red con NextCloud: " : "Error al descargar desde NextCloud: ") + msg, e);
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ignored) {
            }
            if (gestorTemporal) {
                gestor.shutdown();
            }
        }
    }

    /**
     * Sincroniza un archivo JSON de datos. Los fusionables se combinan con la
     * versión remota y, si el resultado difiere de lo que hay en la nube, se
     * sube. El resto se sube (en subida) o se descarga si el remoto es más
     * reciente (en descarga).
     *
     * @return {@code true} si ha cambiado el archivo local.
     */
    private boolean sincronizarArchivoDatos(Sardine sardine, JsonManager gestor, String davBase, String localDir,
            String fileName, DavResource remoteRes, boolean subida) throws IOException {
        File localFile = new File(localDir, fileName);
        String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);

        byte[] remotoBytes = null;
        if (remoteRes != null) {
            try (InputStream in = sardine.get(remoteFileUrl)) {
                remotoBytes = in.readAllBytes();
            }
        }

        boolean localCambiado = false;
        if (JsonManager.ARCHIVOS_FUSIONABLES.contains(fileName)) {
            if (remotoBytes != null && remotoBytes.length > 0) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(remotoBytes), StandardCharsets.UTF_8)) {
                    localCambiado = gestor.fusionarConRemoto(fileName, reader);
                } catch (com.google.gson.JsonParseException | IllegalStateException e) {
                    LOGGER.log(Level.WARNING, "Contenido remoto de {0} no válido; se conserva la versión local: {1}",
                            new Object[]{fileName, e.getMessage()});
                }
            }
            subirSiDistinto(sardine, remoteFileUrl, localFile, remotoBytes, remoteRes);
        } else if (subida) {
            subirSiDistinto(sardine, remoteFileUrl, localFile, remotoBytes, remoteRes);
        } else if (remotoBytes != null && remotoBytes.length > 0 && remoteRes != null
                && esRecursoRemotoMasReciente(remoteRes.getModified(), localFile)) {
            if (localFile.exists()) {
                Files.copy(localFile.toPath(), new File(localDir, fileName + ".bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            escribirAtomico(localFile, remotoBytes);
            if (remoteRes.getModified() != null) {
                localFile.setLastModified(remoteRes.getModified().getTime());
            }
            LOGGER.log(Level.INFO, "Descargado desde NextCloud: {0}", fileName);
            localCambiado = true;
        }
        return localCambiado;
    }

    /**
     * Sube el archivo local si su contenido difiere del remoto. Si coinciden,
     * alinea la fecha local con la remota para no provocar descargas
     * innecesarias.
     */
    private void subirSiDistinto(Sardine sardine, String remoteFileUrl, File localFile,
            byte[] remotoBytes, DavResource remoteRes) throws IOException {
        if (!localFile.exists() || localFile.length() == 0) {
            return;
        }
        byte[] localBytes = Files.readAllBytes(localFile.toPath());
        if (!Arrays.equals(localBytes, remotoBytes)) {
            sardine.put(remoteFileUrl, localBytes, "application/json");
            LOGGER.log(Level.INFO, "JSON sincronizado: {0}", localFile.getName());
        } else if (remoteRes != null && remoteRes.getModified() != null) {
            localFile.setLastModified(remoteRes.getModified().getTime());
        }
    }

    /**
     * Sube los archivos de una carpeta local que todavía no existen en el
     * directorio remoto. Se envían por streaming, sin cargarlos en memoria.
     */
    private void subirArchivosNuevos(Sardine sardine, File carpetaLocal, String remoteDirUrl, boolean sonImagenes) {
        File[] archivos = carpetaLocal.listFiles();
        if (archivos == null || archivos.length == 0) {
            return;
        }
        Set<String> nombresEnRemoto = new java.util.HashSet<>();
        try {
            for (DavResource r : sardine.list(remoteDirUrl)) {
                if (r.getName() != null) {
                    nombresEnRemoto.add(r.getName().toLowerCase(java.util.Locale.ROOT));
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No se pudo listar el directorio remoto {0}: {1}", new Object[]{remoteDirUrl, ex.getMessage()});
        }

        for (File archivo : archivos) {
            String nombre = archivo.getName();
            if (!archivo.isFile() || archivo.length() == 0 || !esNombreArchivoSeguro(nombre) || nombre.endsWith(".tmp")
                    || nombresEnRemoto.contains(nombre.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            try {
                String mime = sonImagenes ? determinarMimeTypeImagen(nombre) : "application/octet-stream";
                sardine.put(remoteDirUrl + encodeUrlSegment(nombre), archivo, mime);
                LOGGER.log(Level.INFO, "Archivo subido (incremental): {0}", nombre);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error al subir {0}: {1}", new Object[]{nombre, ex.getMessage()});
            }
        }
    }

    /**
     * Descarga los archivos remotos que faltan en la carpeta local. Los nombres
     * remotos se validan para que nunca puedan escribir fuera de la carpeta, y
     * cada descarga se escribe primero en un temporal para no dejar archivos a
     * medias si se corta la conexión.
     *
     * @return {@code true} si se ha descargado algún archivo.
     */
    private boolean descargarArchivosNuevos(Sardine sardine, String remoteDirUrl, File carpetaLocal, String nombreCarpeta) {
        boolean descargado = false;
        try {
            if (!sardine.exists(remoteDirUrl)) {
                return false;
            }
            if (!carpetaLocal.exists()) {
                carpetaLocal.mkdirs();
            }
            for (DavResource res : sardine.list(remoteDirUrl)) {
                String nombre = res.getName();
                if (res.isDirectory() || nombre == null || nombre.equalsIgnoreCase(nombreCarpeta)) {
                    continue;
                }
                File destino = resolverDentroDe(carpetaLocal, nombre);
                if (destino == null) {
                    LOGGER.log(Level.WARNING, "Nombre de archivo remoto rechazado por seguridad: {0}", nombre);
                    continue;
                }
                if (destino.exists()) {
                    continue;
                }
                try (InputStream in = sardine.get(remoteDirUrl + encodeUrlSegment(nombre))) {
                    escribirAtomico(destino, in);
                    descargado = true;
                    LOGGER.log(Level.INFO, "Descargado desde NextCloud: {0}/{1}", new Object[]{nombreCarpeta, nombre});
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error al descargar {0}: {1}", new Object[]{nombre, ex.getMessage()});
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error al sincronizar {0} desde NextCloud: {1}", new Object[]{nombreCarpeta, ex.getMessage()});
        }
        return descargado;
    }

    /**
     * Indica si un nombre recibido del servidor es un nombre de archivo simple
     * (sin separadores, sin "..", sin unidad de Windows ni ocultos).
     *
     * @param nombre Nombre a comprobar.
     * @return {@code true} si es seguro usarlo como nombre de archivo local.
     */
    public static boolean esNombreArchivoSeguro(String nombre) {
        return nombre != null && !nombre.isBlank()
                && !nombre.startsWith(".")
                && !nombre.contains("/") && !nombre.contains("\\")
                && !nombre.contains(":") && nombre.indexOf('\0') < 0;
    }

    /**
     * Resuelve un nombre remoto dentro de una carpeta local, devolviendo
     * {@code null} si el resultado quedara fuera de ella.
     *
     * @param carpeta Carpeta local de destino.
     * @param nombre Nombre del archivo remoto.
     * @return Archivo destino o {@code null} si el nombre no es seguro.
     */
    public static File resolverDentroDe(File carpeta, String nombre) {
        if (!esNombreArchivoSeguro(nombre)) {
            return null;
        }
        try {
            File base = carpeta.getCanonicalFile();
            File destino = new File(base, nombre).getCanonicalFile();
            return base.equals(destino.getParentFile()) ? destino : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void escribirAtomico(File destino, byte[] datos) throws IOException {
        escribirAtomico(destino, new ByteArrayInputStream(datos));
    }

    private static void escribirAtomico(File destino, InputStream in) throws IOException {
        File tmp = new File(destino.getParentFile(), "." + destino.getName() + ".part");
        try {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(tmp.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp.toPath());
        }
    }

    private static String determinarMimeTypeImagen(String nombre) {
        if (nombre == null) {
            return "image/jpeg";
        }
        String lower = nombre.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
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
     * Comprueba si el recurso remoto de NextCloud es más reciente que el
     * archivo local correspondiente. Si el archivo local no existe, se
     * considera que el remoto debe descargarse.
     *
     * @param remoteModified Fecha de modificación remota
     * (DavResource.getModified()).
     * @param localFile Archivo físico local.
     * @return true si el remoto es más reciente que el local (con un margen de
     * tolerancia de 2 segundos), o si el archivo local no existe. false en caso
     * contrario.
     */
    public static boolean esRecursoRemotoMasReciente(Date remoteModified, File localFile) {
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
     * Consulta si la versión remota de los archivos JSON de base de datos en
     * NextCloud es más reciente que la versión local (o si faltan archivos o
     * portadas).
     *
     * @param localDir Directorio local donde se encuentran los archivos de
     * datos.
     * @return true si existen cambios remotos más recientes que los locales,
     * false si está al día.
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
                    remoteFilesMap.put(res.getName().toLowerCase(java.util.Locale.ROOT), res);
                }
            }

            // 1. Comprobar si algún archivo de base de datos es más reciente en NextCloud
            for (String dbFile : DB_FILES) {
                DavResource remoteRes = remoteFilesMap.get(dbFile.toLowerCase(java.util.Locale.ROOT));
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
                    if (c.isDirectory() || !esNombreArchivoSeguro(coverName) || coverName.equalsIgnoreCase("covers")) {
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
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Sincroniza desde NextCloud (Pull) si la versión remota de la base de
     * datos es más reciente que la versión local (o si faltan portadas).
     *
     * @param localDir Directorio local donde se encuentran los datos del
     * usuario.
     * @return true si se descargaron cambios remotos, false si la versión local
     * ya estaba al día.
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
     * Comprueba si la colección fusionada aporta libros nuevos o versiones con
     * timestamps más recientes que los presentes en la colección base de
     * referencia.
     *
     * @param fusionados Lista unificada.
     * @param base Lista de referencia (ej. remota).
     * @return true si la lista fusionada contiene novedades frente a la base.
     */
    public static boolean coleccionTieneCambiosNuevos(List<Libro> fusionados, List<Libro> base) {
        if (base == null || base.isEmpty()) {
            return fusionados != null && !fusionados.isEmpty();
        }
        if (fusionados == null || fusionados.isEmpty()) {
            return false;
        }
        if (fusionados.size() != base.size()) {
            return true;
        }
        Map<String, Long> mapaBase = new HashMap<>();
        for (Libro b : base) {
            if (b != null) {
                mapaBase.put(JsonManager.obtenerClaveUnicaLibro(b), b.getUltimaModificacion());
            }
        }
        for (Libro f : fusionados) {
            if (f == null) {
                continue;
            }
            String clave = JsonManager.obtenerClaveUnicaLibro(f);
            Long timeBase = mapaBase.get(clave);
            if (timeBase == null || f.getUltimaModificacion() > timeBase) {
                return true;
            }
        }
        return false;
    }
}
