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
package com.ferlagod.bibliohousefx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * El corazón de la bestia. Aquí arranca todo: cargamos la configuración,
 * elegimos idioma y mostramos la primera pantalla, la de login.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class App extends Application {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(App.class.getName());
    private static java.util.Locale currentLocale = java.util.Locale.forLanguageTag("es");
    private static java.util.ResourceBundle bundle;
    private static Scene scene;
    private static App instance;

    public static void main(String[] args) {
        // Cargar librerías nativas de OpenCV AL INICIO para evitar conflictos
        try {
            nu.pattern.OpenCV.loadLocally();
            LOGGER.info("[App] OpenCV cargado correctamente al inicio.");
        } catch (Throwable e) {
            LOGGER.warning("[App] Error cargando OpenCV: " + e.getMessage());
        }
        launch(args);
    }

    /**
     * Establece el idioma de la aplicación.
     *
     * @param lang código de idioma (es, en, ca, gl, eu, pt)
     */
    public static void setLocale(String lang) {
        currentLocale = java.util.Locale.forLanguageTag(lang);
        // Limpiamos la caché para asegurar que no se use una versión antigua
        java.util.ResourceBundle.clearCache();

        LOGGER.info("[App] Cambiando idioma a: " + lang);

        // Al cambiar locale, recargamos el bundle
        bundle = java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages", currentLocale);
    }

    /**
     * Obtiene el locale actual configurado en la aplicación.
     *
     * @return Objeto {@link java.util.Locale} que representa el locale actual.
     */
    public static java.util.Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Devuelve la instancia única de la aplicación.
     *
     * Este método proporciona acceso al singleton de la clase {@code App},
     * permitiendo interactuar con la aplicación desde cualquier parte del
     * código.
     *
     * @return la instancia única de la clase {@code App}.
     */
    public static App getApp() {
        return instance;
    }

    /**
     * Devuelve el ResourceBundle actual para usar traducciones desde código
     * Java.
     *
     * @return el ResourceBundle activo.
     */
    public static java.util.ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Cargamos el login.
     *
     * @param stage La ventana principal (el escenario).
     * @throws IOException Si el FXML del login ha desaparecido misteriosamente.
     */
    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        // Configurar logs y "securizar" carpeta de datos (ocultarla)
        com.bibliohouse.logic.ConfiguracionLogs.setup();
        // Cargar preferencias del usuario
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);

        // Aplicar el tema moderno de AtlantaFX
        String savedTheme = prefs.get("theme", "Claro (Primer Light)");
        if (savedTheme.equals("Oscuro (Primer Dark)")) {
            Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());
        }

        // Cargar preferencia de idioma si existe (simplificado: por defecto es)
        String lang = prefs.get("language", "es");
        setLocale(lang);

        try {
            // Al arrancar, cargamos primero la pantalla de SPLASH
            FXMLLoader loader = new FXMLLoader(App.class.getResource("splash.fxml"));
            loader.setResources(bundle);
            Parent root = loader.load();

            scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(bundle.getString("app.title"));
            stage.setResizable(false);

            // Icono
            stage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Transición del Splash Screen a la pantalla de Bienvenida.
     */
    public void loadWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("welcome.fxml"));
            loader.setResources(bundle);
            Parent root = loader.load();
            root.setOpacity(0.0);
            scene.setRoot(root);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (IOException e) {
            LOGGER.severe("[App] Error cargando pantalla de bienvenida: " + e.getMessage());
        }
    }

    /**
     * Esto abre la ventana grande, la buena.Se llama cuando el usuario ya ha
     * demostrado que sabe su contraseña.
     *
     * @param username Nombre de usuario
     * @param path ruta de la carpeta
     * @throws java.io.IOException
     */
    public static void loadMain(String username, String path) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        loader.setResources(bundle);
        Parent root = loader.load();

        // Pasamos los datos al controlador
        PrimaryController controller = loader.getController();
        // El controlador lee las preferencias (tema y maximizado) aquí
        controller.initData(username, path);

        // Creamos una NUEVA ventana (Stage) para la aplicación principal
        Stage mainStage = new Stage();
        String title = java.text.MessageFormat.format(bundle.getString("app.title.main"), username);
        mainStage.setTitle(title);

        Scene mainScene = new Scene(root);
        mainStage.setScene(mainScene);
        mainStage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

        // 1. Preparar el efecto Fade-In (arranque suave)
        root.setOpacity(0);

        // 2. LEER Y APLICAR MAXIMIZADO ANTES DE MOSTRAR (Vital para Linux)
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        boolean isMaximized = Boolean.parseBoolean(prefs.get("maximized", "true"));
        mainStage.setMaximized(isMaximized);

        // 3. MOSTRAR LA VENTANA (Ahora el SO ya sabe que debe nacer maximizada)
        mainStage.show();

        // 4. Ejecutar la transición
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Recarga la interfaz principal para aplicar cambios de idioma sin cerrar
     * la ventana.
     *
     * @param stage estado.
     * @param username El nombre de usuario actual.
     * @param path La ruta de la biblioteca actual.
     * @return controller
     * @throws IOException Si hay error cargando el FXML.
     */
    public static PrimaryController reloadUI(Stage stage, String username, String path) throws IOException {
        // Cargar el idioma guardado
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        String lang = prefs.get("language", "es");
        setLocale(lang);

        // Cargar el FXML con el nuevo idioma
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        loader.setResources(bundle); // ESTO ES CLAVE: le damos el diccionario nuevo
        Parent root = loader.load();

        // Re-configurar los datos del controlador
        PrimaryController controller = loader.getController();
        controller.initData(username, path);

        // Cambiar el contenido de la ventana sin cerrarla
        stage.getScene().setRoot(root);

        return controller;
    }

    /**
     * Se ejecuta cuando la aplicación se está cerrando gracefully.
     *
     * @throws java.lang.Exception
     */
    @Override
    public void stop() throws Exception {
        LOGGER.info("[App] Deteniendo aplicación...");
        com.bibliohouse.utils.ImageLoader.shutdown();
        // No llamar System.exit(0) — dejar que la JVM termine limpiamente.
        // Los hilos daemon (syncScheduler, ImageLoader pool) mueren automáticamente.
        LOGGER.info("[App] Bye bye!");
    }
}
