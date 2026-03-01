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
 * Las credenciales del servidor se pasan al constructor y deben haberse
 * obtenido desde las preferencias del usuario ({@code preferences.json}).
 *
 * <p>
 * Todos los métodos públicos de E/S pueden lanzar {@link IOException} si
 * ocurre algún problema de red o de sistema de archivos.
 *
 * @author Fernando Lago
 * @version 1.0
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
            "estanterias.json"
    };

    /** URL base del servidor NextCloud, ej. {@code https://cloud.example.com}. */
    private final String serverUrl;

    /** Nombre de usuario de NextCloud. */
    private final String username;

    /** Contraseña (preferiblemente una «app password» de NextCloud). */
    private final String password;

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
        // Normalizar la URL eliminando la barra final si existe
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Construye la URL WebDAV para la carpeta del usuario en NextCloud.
     *
     * @return URL completa de la carpeta {@code BiblioHouse} en el WebDAV del
     *         usuario.
     */
    private String buildRemoteFolderUrl() {
        return serverUrl + "/remote.php/dav/files/" + username + "/BiblioHouse/";
    }

    /**
     * Construye la URL WebDAV para un archivo concreto en NextCloud.
     *
     * @param fileName Nombre del archivo (ej. {@code biblioteca.json}).
     * @return URL completa del archivo en el WebDAV del usuario.
     */
    private String buildRemoteFileUrl(String fileName) {
        return buildRemoteFolderUrl() + fileName;
    }

    /**
     * Verifica que las credenciales proporcionadas son correctas realizando
     * una operación PROPFIND sobre la raíz del WebDAV del usuario.
     *
     * @return {@code true} si la conexión y autenticación son correctas,
     *         {@code false} en caso contrario.
     */
    public boolean testConexion() {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String rootUrl = serverUrl + "/remote.php/dav/files/" + username + "/";
            sardine.list(rootUrl);
            LOGGER.log(Level.INFO, "Test de conexión NextCloud exitoso para: {0}", serverUrl);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Test de conexión NextCloud fallido: {0}", e.getMessage());
            return false;
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Error cerrando cliente Sardine", ex);
            }
        }
    }

    /**
     * Verifica la conexión y devuelve un mensaje de error descriptivo si falla.
     *
     * @return {@code null} si la conexión es correcta, o un String con el
     *         mensaje del error si falla.
     */
    public String testConexionConMensaje() {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String rootUrl = serverUrl + "/remote.php/dav/files/" + username + "/";
            sardine.list(rootUrl);
            LOGGER.log(Level.INFO, "Test de conexión NextCloud exitoso para: {0}", serverUrl);
            return null; // null indica éxito
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Test de conexión NextCloud fallido: {0}", e.getMessage());
            return e.getMessage();
        } finally {
            try {
                sardine.shutdown();
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Error cerrando cliente Sardine", ex);
            }
        }
    }

    /**
     * Sube los archivos JSON de la base de datos local al servidor NextCloud.
     * Si la carpeta {@code BiblioHouse} no existe en NextCloud, la crea
     * automáticamente antes de subir los archivos.
     *
     * <p>
     * Solo se suben los archivos que existan localmente; los que no existen
     * se omiten sin producir error.
     *
     * @param localDir Ruta al directorio local del usuario donde están los
     *                 archivos JSON (ej. {@code ~/BiblioHouse/usuario}).
     * @throws IOException si se produce un error de E/S al subir algún archivo.
     */
    public void subirBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            String remoteFolderUrl = buildRemoteFolderUrl();

            // Crear la carpeta remota si no existe
            if (!sardine.exists(remoteFolderUrl)) {
                LOGGER.log(Level.INFO, "Creando carpeta remota en NextCloud: {0}", remoteFolderUrl);
                sardine.createDirectory(remoteFolderUrl);
            }

            // Subir cada archivo de la BD
            for (String fileName : DB_FILES) {
                File localFile = new File(localDir, fileName);
                if (!localFile.exists()) {
                    LOGGER.log(Level.FINE, "Archivo local no encontrado, omitiendo subida: {0}", fileName);
                    continue;
                }
                String remoteFileUrl = buildRemoteFileUrl(fileName);
                try (InputStream in = new FileInputStream(localFile)) {
                    sardine.put(remoteFileUrl, in, "application/json");
                    LOGGER.log(Level.INFO, "Subido a NextCloud: {0}", fileName);
                }
            }
        } finally {
            sardine.shutdown();
        }
    }

    /**
     * Descarga los archivos JSON de la base de datos desde NextCloud y los
     * sobreescribe en el directorio local del usuario.
     *
     * <p>
     * Antes de sobreescribir cada archivo se crea una copia de seguridad
     * con extensión {@code .bak} para permitir la recuperación en caso de
     * error.
     *
     * <p>
     * Solo se descargan los archivos que existan en NextCloud; si alguno no
     * está disponible remotamente, se omite.
     *
     * @param localDir Ruta al directorio local del usuario donde se guardarán
     *                 los archivos JSON.
     * @throws IOException si se produce un error de E/S al descargar algún archivo.
     */
    public void descargarBaseDatos(String localDir) throws IOException {
        Sardine sardine = SardineFactory.begin(username, password);
        try {
            for (String fileName : DB_FILES) {
                String remoteFileUrl = buildRemoteFileUrl(fileName);
                if (!sardine.exists(remoteFileUrl)) {
                    LOGGER.log(Level.FINE, "Archivo remoto no encontrado, omitiendo descarga: {0}", fileName);
                    continue;
                }

                File localFile = new File(localDir, fileName);

                // Crear backup del archivo local antes de sobreescribir
                if (localFile.exists()) {
                    File backup = new File(localDir, fileName + ".bak");
                    Files.copy(localFile.toPath(), backup.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.FINE, "Backup creado: {0}.bak", fileName);
                }

                // Descargar y guardar
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
            sardine.shutdown();
        }
    }
}
