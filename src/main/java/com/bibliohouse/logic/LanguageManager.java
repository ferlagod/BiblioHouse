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

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Gestiona el idioma de la aplicación.
 *
 * @author Fernando Lago
 * @version 1.2
 */
public class LanguageManager {

    private static ResourceBundle bundle;
    private static Preferences preferencias;
    private static final String LANGUAGE_KEY = "appLanguage";
    private static final String COUNTRY_KEY = "appCountry";

    // Bloque estático para cargar el idioma al iniciar la clase
    static {
        preferencias = Preferences.userNodeForPackage(LanguageManager.class);
        String language = preferencias.get(LANGUAGE_KEY, Locale.getDefault().getLanguage());
        String country = preferencias.get(COUNTRY_KEY, Locale.getDefault().getCountry());
        Locale initialLocale = new Locale(language, country);

        cargarIdioma(initialLocale);
    }

    /**
     * Carga el archivo de idioma correspondiente al locale especificado. Si no
     * encuentra el archivo de idioma, utiliza el inglés como idioma por
     * defecto.
     *
     * @param locale El locale para el que se quiere cargar el archivo de
     * mensajes.
     *
     */
    private static void cargarIdioma(Locale locale) {
        try {
            bundle = ResourceBundle.getBundle("com.bibliohouse.lang.messages", locale);
        } catch (MissingResourceException e) {
            System.err.println("No se encontró el archivo de idioma para " + locale + ". Usando inglés como fallback.");
            bundle = ResourceBundle.getBundle("com.bibliohouse.lang.messages", Locale.ENGLISH);

        }

    }

    /**
     * Cambia el idioma de la aplicación y guarda la preferencia.
     *
     * @param language El código del idioma
     * @param country El código del país
     */
    public static void cambiarIdioma(String language, String country) {
        if (language == null || country == null || language.isEmpty() || country.isEmpty()) {
            throw new IllegalArgumentException("El idioma y el país no pueden ser nulos o vacíos.");
        }
        preferencias.put(LANGUAGE_KEY, language);
        preferencias.put(COUNTRY_KEY, country);
        Locale newLocale = new Locale(language, country);
        cargarIdioma(newLocale);
    }

    /**
     * Obtiene una cadena de texto traducida a partir de su clave.
     *
     * @param key La clave del texto
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
     * Obtiene una cadena de texto traducida a partir de su clave, devolviendo
     * un valor por defecto si la clave no se encuentra.
     *
     * @param key La clave del texto.
     * @param defaultValue El texto a devolver si la clave no existe.
     * @return El texto traducido o valor por defecto.
     */
    public static String getString(String key, String defaultValue) {
        try {
            // El bundle no puede ser null, sino devuelve el valor por defecto
            if (bundle == null) {
                System.err.println("Error: ResourceBundle no inicializado en LanguageManager.");
                return defaultValue;
            }
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // Si no se encuentra la clave, devuelve el valor por defecto
            return defaultValue;
        } catch (Exception e) {
            System.err.println("Error inesperado obteniendo clave '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }

}
