/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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

import com.bibliohouse.ui.VentanaPrincipal;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Gestiona el idioma de la aplicación.
 *
 * @author ferlagod
 * @version 1.0
 */
public class LanguageManager {

    private static ResourceBundle bundle;
    private static Preferences prefs;
    private static final String LANGUAGE_KEY = "appLanguage";
    private static final String COUNTRY_KEY = "appCountry";

    // Bloque estático para cargar el idioma al iniciar la clase
    static {
        prefs = Preferences.userNodeForPackage(VentanaPrincipal.class);
        String language = prefs.get(LANGUAGE_KEY, Locale.getDefault().getLanguage());
        String country = prefs.get(COUNTRY_KEY, Locale.getDefault().getCountry());
        loadLanguage(new Locale(language, country));
    }

    private static void loadLanguage(Locale locale) {
        try {
            // Carga el archivo de propiedades (ej: messages_es.properties)
            bundle = ResourceBundle.getBundle("com.bibliohouse.lang.messages", locale);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el archivo de idioma para " + locale + ". Usando el idioma por defecto.");
            // Si falla, intenta cargar el inglés como fallback
            bundle = ResourceBundle.getBundle("com.bibliohouse.lang.messages", Locale.ENGLISH);
        }
    }

    /**
     * Obtiene una cadena de texto traducida a partir de su clave.
     * @param key La clave del texto (ej: "menu.file").
     * @return El texto traducido.
     */
    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            // Si no se encuentra una clave, devuelve la clave misma para que sea fácil de depurar
            return key;
        }
    }

    /**
     * Cambia el idioma de la aplicación y guarda la preferencia.
     * @param language El código del idioma (ej: "es", "en").
     * @param country El código del país (ej: "ES", "US").
     */
    public static void setLanguage(String language, String country) {
        prefs.put(LANGUAGE_KEY, language);
        prefs.put(COUNTRY_KEY, country);
        loadLanguage(new Locale(language, country));
    }
}
