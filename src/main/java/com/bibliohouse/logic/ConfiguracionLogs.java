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

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Esto configura el sistema de logs de la aplicación. Básicamente, hace que
 * todos los mensajes importantes (errores, advertencias, info) se guarden en un
 * archivo, en vez de solo mostrarse por consola. Así es más fácil revisar qué
 * ha pasado si algo falla.
 *
 * @author Fernando Lago
 * @version 1.3
 */
public class ConfiguracionLogs {

    // Aquí es donde se guardan los archivos de la app (en la carpeta del usuario)
    private static final String APP_DIRECTORY_PATH = System.getProperty("user.home") + File.separator + "BiblioHouse";
    // Y este es el nombre del archivo donde se escriben los logs
    private static final String LOG_FILE_PATH = APP_DIRECTORY_PATH + File.separator + "bibliohouse.log";

    /**
     * Configura el sistema de logging. Esto hace que los mensajes de log se
     * guarden en un archivo en disco. Se llama solo una vez cuando arranca la
     * app.
     *
     * @throws SecurityException Si el sistema operativo no nos deja escribir en
     * esa carpeta (raro, pero puede pasar).
     */
    public static void setup() {
        File appDir = new File(APP_DIRECTORY_PATH);
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                System.err.println("Advertencia: No se pudo crear el directorio para los logs: " + APP_DIRECTORY_PATH);
            }
        }

        // Intentamos ocultar la carpeta para "securizarla"
        ocultarDirectorio(appDir);

        try {
            // Obtener el logger raíz
            Logger rootLogger = Logger.getLogger("");

            // Crear el FileHandler
            // - LOG_FILE_PATH: Ruta al archivo.
            // - 1024 * 1024: Tamaño máximo del archivo en bytes (1MB).
            // - 3: Número máximo de archivos a rotar.
            // - true: Añadir al archivo existente (append). Si es false, se sobrescribe
            // cada vez.
            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, 1024 * 1024, 3, true);

            // Se establece un formato simple y se establece el nivel mínimo para guardar en
            // archivo
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            fileHandler.setLevel(Level.INFO);

            // Se añade el handler al logger raíz
            rootLogger.addHandler(fileHandler);
            System.out.println("INFO: Logging configurado. Los mensajes se guardarán en: " + LOG_FILE_PATH);

        } catch (IOException e) {
            // Si falla la configuración del archivo, muestra el error en la consola
            System.err.println("GRAVE: Error al configurar el FileHandler para logging: " + e.getMessage());
        }
    }

    /**
     * Intenta ocultar el directorio para que el usuario no lo borre
     * accidentalmente. En macOS usa 'chflags hidden', en Windows usa el
     * atributo 'dos:hidden'.
     *
     * @param dir El directorio a ocultar.
     */
    private static void ocultarDirectorio(File dir) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                // En macOS usamos el comando chflags
                new ProcessBuilder("chflags", "hidden", dir.getAbsolutePath()).start();
            } else if (os.contains("win")) {
                // En Windows usamos la API de NIO
                java.nio.file.Files.setAttribute(dir.toPath(), "dos:hidden", true);
            }
        } catch (IOException e) {
            // No es crítico si falla, solo mostramos aviso
            System.err.println("Advertencia: No se pudo ocultar la carpeta de datos: " + e.getMessage());
        }
    }

}
