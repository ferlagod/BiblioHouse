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

import com.ferlagod.bibliohousefx.App;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Fuente única de verdad para la gestión de internacionalización (i18n)
 * e idiomas en BiblioHouse.
 *
 * Sincroniza las preferencias del sistema, mantiene el ResourceBundle activo
 * y notifica a los componentes visuales mediante AppEventBus.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class LanguageManager {

    private static final Logger LOGGER = Logger.getLogger(LanguageManager.class.getName());
    private static final String BUNDLE_BASE_NAME = "com.ferlagod.bibliohousefx.messages";

    private static final String APP_LANG_KEY = "language";
    private static final String LM_LANG_KEY = "appLanguage";
    private static final String LM_COUNTRY_KEY = "appCountry";

    private static Locale currentLocale;
    private static ResourceBundle bundle;

    static {
        inicializarDesdePreferencias();
    }

    /**
     * Carga el idioma inicial leyendo de forma unificada las preferencias.
     */
    private static synchronized void inicializarDesdePreferencias() {
        try {
            Preferences prefsApp = Preferences.userNodeForPackage(App.class);
            Preferences prefsLM = Preferences.userNodeForPackage(LanguageManager.class);

            String lang = prefsApp.get(APP_LANG_KEY, null);
            if (lang == null || lang.isBlank()) {
                lang = prefsLM.get(LM_LANG_KEY, null);
            }

            if (lang == null || lang.isBlank()) {
                lang = "es";
            }

            String country = prefsLM.get(LM_COUNTRY_KEY, "");
            Locale initialLocale;
            if (country != null && !country.isBlank()) {
                initialLocale = new Locale(lang, country);
            } else {
                initialLocale = Locale.forLanguageTag(lang);
            }

            cargarIdioma(initialLocale, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al inicializar preferencias de idioma, usando español por defecto", e);
            cargarIdioma(Locale.forLanguageTag("es"), false);
        }
    }

    /**
     * Carga el archivo de idioma correspondiente al locale especificado.
     *
     * @param locale El locale objetivo.
     * @param notificarEventBus Si true, emite IdiomaCambiadoEvent en el bus de eventos.
     */
    private static synchronized void cargarIdioma(Locale locale, boolean notificarEventBus) {
        currentLocale = locale;
        ResourceBundle.clearCache();
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
        } catch (MissingResourceException e) {
            LOGGER.log(Level.WARNING, "No se encontró bundle para {0}. Recurriendo a inglés.", locale);
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
            } catch (MissingResourceException ex) {
                bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ROOT);
            }
        }

        if (notificarEventBus) {
            AppEventBus.getInstance().publish(new AppEventBus.IdiomaCambiadoEvent(currentLocale, bundle));
        }
    }

    /**
     * Obtiene el Locale actual de la aplicación.
     *
     * @return El Locale activo.
     */
    public static synchronized Locale getLocale() {
        if (currentLocale == null) {
            currentLocale = Locale.forLanguageTag("es");
        }
        return currentLocale;
    }

    /**
     * Obtiene el ResourceBundle actual.
     *
     * @return El ResourceBundle cargado.
     */
    public static synchronized ResourceBundle getBundle() {
        if (bundle == null) {
            inicializarDesdePreferencias();
        }
        return bundle;
    }

    /**
     * Establece el idioma activo a partir de su código de idioma ISO (ej: "es", "en", "gl", "ca", "eu", "pt").
     * Guarda la preferencia en todos los nodos correspondientes y notifica al bus de eventos.
     *
     * @param langCode Código del idioma.
     */
    public static synchronized void setLocale(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            langCode = "es";
        }
        setLocale(Locale.forLanguageTag(langCode));
    }

    /**
     * Establece el Locale activo de la aplicación.
     *
     * @param locale El nuevo Locale a aplicar.
     */
    public static synchronized void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.forLanguageTag("es");
        }

        // Persistir en Preferences de App.class (usado por el recargador de UI)
        try {
            Preferences prefsApp = Preferences.userNodeForPackage(App.class);
            prefsApp.put(APP_LANG_KEY, locale.getLanguage());

            // Persistir también en Preferences de LanguageManager.class para compatibilidad
            Preferences prefsLM = Preferences.userNodeForPackage(LanguageManager.class);
            prefsLM.put(LM_LANG_KEY, locale.getLanguage());
            if (locale.getCountry() != null) {
                prefsLM.put(LM_COUNTRY_KEY, locale.getCountry());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudieron guardar las preferencias de idioma", e);
        }

        LOGGER.log(Level.INFO, "Idioma cambiado a: {0}", locale.toLanguageTag());
        cargarIdioma(locale, true);
    }

    /**
     * Cambia el idioma de la aplicación (sobrecarga de compatibilidad).
     *
     * @param language Código del idioma.
     * @param country Código del país (opcional, puede ser nulo o vacío).
     */
    public static void cambiarIdioma(String language, String country) {
        if (language == null || language.isBlank()) {
            language = "es";
        }
        if (country != null && !country.isBlank()) {
            setLocale(new Locale(language, country));
        } else {
            setLocale(language);
        }
    }

    /**
     * Cambia el idioma de la aplicación únicamente con código de idioma.
     *
     * @param language Código del idioma.
     */
    public static void cambiarIdioma(String language) {
        setLocale(language);
    }

    /**
     * Obtiene una cadena de texto traducida a partir de su clave.
     *
     * @param key La clave del texto
     * @return El texto traducido, o la clave misma si no existe.
     */
    public static String getString(String key) {
        ResourceBundle b = getBundle();
        try {
            return b.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * Obtiene una cadena de texto traducida a partir de su clave, devolviendo
     * un valor por defecto si la clave no se encuentra.
     *
     * @param key La clave del texto.
     * @param defaultValue El texto a devolver si la clave no existe.
     * @return El texto traducido o el valor por defecto.
     */
    public static String getString(String key, String defaultValue) {
        ResourceBundle b = getBundle();
        try {
            if (b == null) {
                return defaultValue;
            }
            return b.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
