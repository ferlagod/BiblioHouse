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
import javafx.application.Platform;
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
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class App extends Application {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(App.class.getName());
    private static Scene scene;
    private static App instance;

    /**
     * El punto de entrada principal de la aplicación. Carga las librerías
     * necesarias y arranca la interfaz gráfica.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "BiblioHouse");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "BiblioHouse");
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
     * Establece el idioma de la aplicación delegando en LanguageManager.
     *
     * @param lang código de idioma (es, en, ca, gl, eu, pt)
     */
    public static void setLocale(String lang) {
        LOGGER.info("[App] Cambiando idioma a: " + lang);
        com.bibliohouse.logic.LanguageManager.setLocale(lang);
    }

    /**
     * Obtiene el locale actual configurado en la aplicación delegando en
     * LanguageManager.
     *
     * @return Objeto {@link java.util.Locale} que representa el locale actual.
     */
    public static java.util.Locale getCurrentLocale() {
        return com.bibliohouse.logic.LanguageManager.getLocale();
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
     * Java delegando en LanguageManager.
     *
     */
    public static final String DENSITY_COMPACT = "Compacto";
    public static final String DENSITY_STANDARD = "Estándar";
    public static final String DENSITY_ACCESSIBLE = "Grande / Accesible";

    public static final String THEME_HIGH_CONTRAST_LIGHT = "Alto Contraste Claro (High Contrast Light)";
    public static final String THEME_HIGH_CONTRAST_DARK = "Alto Contraste Oscuro (High Contrast Dark)";

    private static String currentDensity = DENSITY_STANDARD;
    private static String currentTheme = "Automático (Sistema)";
    private static final java.util.Set<Scene> activeScenes = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static java.util.ResourceBundle getBundle() {
        return com.bibliohouse.logic.LanguageManager.getBundle();
    }

    /**
     * Registra una escena para mantener sincronizada su escala de densidad y
     * tema visual de accesibilidad en tiempo real.
     *
     * @param sc Escena a registrar.
     */
    public static void registerScene(Scene sc) {
        if (sc == null) {
            return;
        }
        activeScenes.add(sc);
        sc.rootProperty().addListener((obs, oldR, newR) -> {
            if (newR != null) {
                applyThemeToScene(sc, currentTheme);
                applyDensityToScene(sc, currentDensity);
            }
        });
        applyThemeToScene(sc, currentTheme);
        applyDensityToScene(sc, currentDensity);
    }

    /**
     * Aplica la escala tipográfica y de espaciado a una escena específica.
     * @param sc Escala tipográfica
     * @param density Espacio de una escena
     */
    public static void applyDensityToScene(Scene sc, String density) {
        if (sc == null || sc.getRoot() == null) {
            return;
        }
        Parent root = sc.getRoot();
        root.getStyleClass().removeAll("density-compact", "density-standard", "density-accessible");
        String styleClass;
        double fontSize;
        if (DENSITY_COMPACT.equalsIgnoreCase(density)) {
            styleClass = "density-compact";
            fontSize = 11.5;
        } else if (DENSITY_ACCESSIBLE.equalsIgnoreCase(density) || "Grande".equalsIgnoreCase(density)) {
            styleClass = "density-accessible";
            fontSize = 16.0;
        } else {
            styleClass = "density-standard";
            fontSize = 13.0;
        }
        root.getStyleClass().add(styleClass);

        String curStyle = root.getStyle();
        if (curStyle == null) {
            curStyle = "";
        }
        curStyle = curStyle.replaceAll("-fx-font-size:[^;]+;?", "").trim();
        root.setStyle((curStyle.isEmpty() ? "" : curStyle + " ") + "-fx-font-size: " + fontSize + "px;");
    }

    /**
     * Aplica las clases de alto contraste según el tema activo a una escena
     * específica.
     * @param sc Clases de contraste
     * @param themeName Tema activo
     */
    public static void applyThemeToScene(Scene sc, String themeName) {
        if (sc == null || sc.getRoot() == null) {
            return;
        }
        Parent root = sc.getRoot();
        root.getStyleClass().removeAll("theme-high-contrast-light", "theme-high-contrast-dark", "high-contrast");
        if (themeName != null && (themeName.contains("Alto Contraste") || themeName.contains("High Contrast"))) {
            root.getStyleClass().add("high-contrast");
            if (themeName.contains("Claro") || themeName.contains("Light")) {
                root.getStyleClass().add("theme-high-contrast-light");
            } else {
                root.getStyleClass().add("theme-high-contrast-dark");
            }
        }
    }

    /**
     * Aplica globalmente una densidad / escala de interfaz en toda la
     * aplicación.
     *
     * @param density Densidad elegida ("Compacto", "Estándar", "Grande /
     * Accesible").
     */
    public static void applyDensity(String density) {
        currentDensity = density != null ? density : DENSITY_STANDARD;
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        prefs.put("uiDensity", currentDensity);

        for (Scene sc : new java.util.ArrayList<>(activeScenes)) {
            applyDensityToScene(sc, currentDensity);
        }
    }

    /**
     * Devuelve la densidad tipográfica actual configurada.
     * @return Densidad de la tipografía
     */
    public static String getCurrentDensity() {
        return currentDensity;
    }

    /**
     * Devuelve el tema visual actual configurado.
     * @return Tema visual actual
     */
    public static String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Aplica un tema visual de AtlantaFX a toda la aplicación. Centraliza la
     * lógica para evitar duplicación entre App y ConfiguracionController,
     * incorporando soporte para temas de Alto Contraste (WCAG AA/AAA).
     *
     * @param themeName Nombre del tema tal como aparece en el ComboBox de
     * configuración.
     */
    public static void applyTheme(String themeName) {
        currentTheme = themeName != null ? themeName : "Automático (Sistema)";
        String stylesheet;
        if ("Automático (Sistema)".equalsIgnoreCase(currentTheme)) {
            boolean dark = com.bibliohouse.utils.OsThemeDetector.isDarkMode();
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                stylesheet = dark ? new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet()
                        : new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet();
            } else {
                stylesheet = dark ? new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet()
                        : new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet();
            }
        } else {
            stylesheet = switch (currentTheme) {
                case "Oscuro (Primer Dark)" ->
                    new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet();
                case "Nord Claro (Nord Light)" ->
                    new atlantafx.base.theme.NordLight().getUserAgentStylesheet();
                case "Nord Oscuro (Nord Dark)" ->
                    new atlantafx.base.theme.NordDark().getUserAgentStylesheet();
                case "Cupertino Claro (macOS Light)" ->
                    new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet();
                case "Cupertino Oscuro (macOS Dark)" ->
                    new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet();
                case "Dracula" ->
                    new atlantafx.base.theme.Dracula().getUserAgentStylesheet();
                case THEME_HIGH_CONTRAST_LIGHT ->
                    new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet();
                case THEME_HIGH_CONTRAST_DARK ->
                    new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet();
                default ->
                    new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet();
            };
        }
        Application.setUserAgentStylesheet(stylesheet);

        for (Scene sc : new java.util.ArrayList<>(activeScenes)) {
            applyThemeToScene(sc, currentTheme);
        }
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
        String savedTheme = prefs.get("theme", "Automático (Sistema)");
        applyTheme(savedTheme);

        // Cargar y aplicar densidad / escala de interfaz
        String savedDensity = prefs.get("uiDensity", DENSITY_STANDARD);
        applyDensity(savedDensity);

        // Sincronización automática con el Modo Oscuro/Claro del Sistema Operativo
        com.bibliohouse.utils.OsThemeDetector.startAutoSync(dark -> {
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(App.class);
            String current = p.get("theme", "Automático (Sistema)");
            if ("Automático (Sistema)".equalsIgnoreCase(current)) {
                applyTheme("Automático (Sistema)");
            }
        });

        // Cargar preferencia de idioma si existe (simplificado: por defecto es)
        String lang = prefs.get("language", "es");
        setLocale(lang);

        try {
            // Al arrancar, cargamos primero la pantalla de SPLASH
            FXMLLoader loader = new FXMLLoader(App.class.getResource("splash.fxml"));
            loader.setResources(getBundle());
            Parent root = loader.load();

            scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
            registerScene(scene);
            stage.setScene(scene);
            stage.setTitle(getBundle().getString("app.title"));
            stage.setResizable(false);

            // Icono
            stage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));
            stage.setOnCloseRequest(e -> Platform.exit());

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
            loader.setResources(getBundle());
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
        loader.setResources(getBundle());
        Parent root = loader.load();

        // Pasamos los datos al controlador
        PrimaryController controller = loader.getController();
        // El controlador lee las preferencias (tema y maximizado) aquí
        controller.initData(username, path);

        // Creamos una NUEVA ventana (Stage) para la aplicación principal
        Stage mainStage = new Stage();
        String title = java.text.MessageFormat.format(getBundle().getString("app.title.main"), username);
        mainStage.setTitle(title);

        Scene mainScene = new Scene(root);
        mainScene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        registerScene(mainScene);
        mainStage.setScene(mainScene);
        mainStage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

        // 1. Preparar el efecto Fade-In (arranque suave)
        root.setOpacity(0);

        // 2. LEER Y APLICAR MAXIMIZADO ANTES DE MOSTRAR (Vital para Linux)
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        boolean isMaximized = Boolean.parseBoolean(prefs.get("maximized", "true"));
        mainStage.setMaximized(isMaximized);

        // 3. MOSTRAR LA VENTANA (Ahora el SO ya sabe que debe nacer maximizada)
        mainStage.setOnCloseRequest(e -> Platform.exit());
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
        loader.setResources(getBundle()); // Diccionario nuevo desde LanguageManager
        Parent root = loader.load();

        // Re-configurar los datos del controlador
        PrimaryController controller = loader.getController();
        controller.initData(username, path);

        // Cambiar el contenido de la ventana sin cerrarla
        stage.getScene().setRoot(root);
        if (!stage.getScene().getStylesheets().contains(App.class.getResource("styles.css").toExternalForm())) {
            stage.getScene().getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        }
        registerScene(stage.getScene());

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
        try {
            com.bibliohouse.utils.ImageLoader.shutdown();
        } catch (Throwable ignored) {
        }
        try {
            com.bibliohouse.utils.SystemNotificationService.shutdown();
        } catch (Throwable ignored) {
        }
        try {
            com.bibliohouse.utils.OsThemeDetector.shutdown();
        } catch (Throwable ignored) {
        }
        LOGGER.info("[App] Bye bye!");
        System.exit(0);
    }
}
