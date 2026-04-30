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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para cargar configuración desde el archivo config.properties.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;

    /**
     * Obtiene el valor de una propiedad de configuración.
     *
     * @param key Clave de la propiedad
     * @return Valor de la propiedad o null si no existe
     */
    public static String getProperty(String key) {
        if (properties == null) {
            loadProperties();
        }
        return properties.getProperty(key);
    }

    /**
     * Carga las propiedades del archivo de configuración. Intenta cargar desde
     * la raíz del proyecto (para desarrollo) o desde el classpath (para
     * producción).
     */
    private static void loadProperties() {
        properties = new Properties();

        // Intentar cargar desde la raíz del proyecto primero (prioridad en desarrollo)
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            LOGGER.log(Level.INFO, "Configuración cargada desde archivo local: {0}", CONFIG_FILE);
            return;
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "No se encontró {0} en la raíz del proyecto. Intentando buscar en classpath...",
                    CONFIG_FILE);
        }

        // Si falla, intentar cargar desde el classpath 
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                LOGGER.log(Level.WARNING, "No se encontró el archivo de configuración: {0}", CONFIG_FILE);
                return;
            }
            properties.load(input);
            LOGGER.log(Level.INFO, "Configuración cargada desde classpath: {0}", CONFIG_FILE);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error al cargar el archivo de configuración", ex);
        }
    }
}
