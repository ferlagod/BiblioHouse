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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio que gestiona la sincronización de la base de datos local de
 * BiblioHouse con un servidor NextCloud mediante el protocolo WebDAV.
 *
 * <p>
 * Detecta automáticamente cuál de las dos rutas WebDAV estándar de NextCloud
 * está disponible:
 * <ol>
 * <li>{@code /remote.php/dav/files/{usuario}/} — API DAV moderna (NC ≥ 9).</li>
 * <li>{@code /remote.php/webdav/} — WebDAV clásico.</li>
 * </ol>
 *
 * @author Fernando Lago
 * @version 1.1
 */
public class NextCloudSyncService {

    private static final Logger LOGGER = Logger.getLogger(NextCloudSyncService.class.getName());

    /** Nombres de los archivos JSON que componen la base de datos local. */
    private static final String[] DB_FILES = {
            "biblioteca.json",
            "prestamos.json",
            "socios.json",
            "estanterias.json"
    };

    /**
     * Posibles rutas WebDAV raíz en un servidor NextCloud.
     * Se prueban en orden; la primera que responda correctamente se adopta.
     */
    private static final String[] DAV_CANDIDATES = {
            "/remote.php/dav/files/{user}/", // DAV moderno (NC ≥ 9)
            "/remote.php/webdav/" // WebDAV clásico
    };

    /** URL base del servidor NextCloud, ej. {@code https://cloud.example.com}. */
    private final String serverUrl;

    /** Nombre de usuario de NextCloud. */
    private final String username;

    /** Contraseña (preferiblemente una «app password» de NextCloud). */
    private final String password;

    /**
     * URL raíz WebDAV detectada automáticamente (incluye barra final).
     * Es {@code null} hasta que se llame a {@link #resolverDavBase(Sardine)}.
     */
    private String davBaseUrl;

    /**
     * Construye un nuevo servicio de sincronización con NextCloud.
     *
     * @param serverUrl URL base del servidor NextCloud
     *                  (sin barra al final, ej. {@code https://cloud.example.com}).
     * @param username  Nombre de usuario de NextCloud.
     * @param password  Contraseña o app password de NextCloud.
     * @throws IllegalArgumentException si alguno de los parámetros es nulo o vacío.
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
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.username = username;
        this.password = password;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados de utilidad
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Detecta y cachea la URL base WebDAV correcta probando los candidatos
     * definidos en {@link #DAV_CANDIDATES}.
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
            String url = serverUrl + candidate.replace("{user}", username);
            try {
                sardine.list(url);
                davBaseUrl = url;
                LOGGER.log(Level.INFO, "Endpoint WebDAV detectado: {0}", davBaseUrl);
                return davBaseUrl;
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Candidato WebDAV no disponible ({0}): {1}",
                        new Object[] { url, e.getMessage() });
            }
        }

        throw new IOException(
                "No se pudo conectar a NextCloud. Comprueba la URL y el usuario.\n"
                        + "Rutas probadas:\n"
                        + "  · " + serverUrl + DAV_CANDIDATES[0].replace("{user}", username) + "\n"
                        + "  · " + serverUrl + DAV_CANDIDATES[1]);
    }

    private String buildRemoteFolderUrl(String davBase) {
        return davBase + "BiblioHouse/";
    }

    private String buildRemoteFileUrl(String davBase, String fileName) {
        return buildRemoteFolderUrl(davBase) + fileName;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica que las credenciales son correctas y que el servidor es accesible.
     *
     * @return {@code true} si la conexión es correcta, {@code false} en caso
     *         contrario.
     */
    public boolean testConexion() {
        return testConexionConMensaje() == null;
    }

    /**
     * Verifica la conexión y devuelve un mensaje de error descriptivo si falla.
     *
     * @return {@code null} si la conexión es correcta, o un String con el error.
     */
    public String testConexionConMensaje() {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            davBaseUrl = null; // forzar re-detección en cada test
            resolverDavBase(sardine);
            LOGGER.log(Level.INFO, "Test de conexión NextCloud exitoso: {0}", davBaseUrl);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.log(Level.WARNING, "Test de conexión NextCloud fallido: {0}", msg);
            return msg;
        } finally {
            try {
                sardine.shutdown();
            } catch (Exception ignored) {
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

            // Crear carpeta BiblioHouse; 405 = ya existe (ignorado).
            try {
                sardine.createDirectory(remoteFolderUrl);
                LOGGER.log(Level.INFO, "Carpeta creada en NextCloud: {0}", remoteFolderUrl);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("405") || msg.contains("Method Not Allowed") || msg.contains("301")) {
                    LOGGER.log(Level.FINE, "Carpeta BiblioHouse ya existía (ignorado).");
                } else {
                    LOGGER.log(Level.WARNING, "Respuesta inesperada al crear carpeta: {0}", msg);
                }
            }

            for (String fileName : DB_FILES) {
                File localFile = new File(localDir, fileName);
                if (!localFile.exists()) {
                    LOGGER.log(Level.FINE, "Archivo local no encontrado, omitiendo: {0}", fileName);
                    continue;
                }
                String remoteFileUrl = buildRemoteFileUrl(davBase, fileName);
                try (InputStream in = new FileInputStream(localFile)) {
                    sardine.put(remoteFileUrl, in, "application/json");
                    LOGGER.log(Level.INFO, "Subido a NextCloud: {0}", fileName);
                }
            }
        } finally {
            try {
                sardine.shutdown();
            } catch (Exception ignored) {
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

                try (InputStream in = sardine.get(remoteFileUrl);
                        FileOutputStream out = new FileOutputStream(localFile)) {
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
            } catch (Exception ignored) {
            }
        }
    }
}
