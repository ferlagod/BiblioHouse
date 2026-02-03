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
 * @author Ferlagod
 * @version 1.1
 */
public class App extends Application {

    private static java.util.Locale currentLocale = new java.util.Locale("es");
    private static java.util.ResourceBundle bundle;
    private static Scene scene;

    public static void main(String[] args) {
        // Cargar librerías nativas de OpenCV AL INICIO para evitar conflictos
        try {
            nu.pattern.OpenCV.loadLocally();
            System.out.println("[App] OpenCV cargado correctamente al inicio.");
        } catch (Throwable e) {
            System.err.println("[App] Error cargando OpenCV: " + e.getMessage());
        }
        launch(args);
    }

    /**
     * Establece el idioma de la aplicación.
     *
     * @param lang código de idioma (es, en, ca, gl, eu, pt)
     */
    public static void setLocale(String lang) {
        currentLocale = new java.util.Locale(lang);
        // Limpiamos la caché para asegurar que no se use una versión antigua
        java.util.ResourceBundle.clearCache();

        System.out.println("[App] Cambiando idioma a: " + lang);

        // Al cambiar locale, recargamos el bundle
        bundle = java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages", currentLocale);
    }

    public static java.util.Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Cargamos el login.
     *
     * @param stage La ventana principal (el escenario).
     * @throws IOException Si el FXML del login ha desaparecido misteriosamente.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Configurar logs y "securizar" carpeta de datos (ocultarla)
        com.bibliohouse.logic.ConfiguracionLogs.setup();

        // Cargar preferencia de idioma si existe (simplificado: por defecto es)
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        String lang = prefs.get("language", "es");
        setLocale(lang);

        // 1. Al arrancar, cargamos la pantalla de BIENVENIDA (Registro vs Invitado)
        FXMLLoader loader = new FXMLLoader(App.class.getResource("welcome.fxml"));
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
    }

    /**
     * Esto abre la ventana grande, la buena.Se llama cuando el usuario ya ha
     * demostrado que sabe su contraseña.
     *
     * @param username Nombre de usuario
     * @param path     ruta de la carpeta
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
        // app.title.main=BiblioHouse - Biblioteca de {0}
        String title = java.text.MessageFormat.format(bundle.getString("app.title.main"), username);
        mainStage.setTitle(title);

        // IMPORTANTE: Creamos una escena nueva con la raíz nueva
        Scene mainScene = new Scene(root);
        mainStage.setScene(mainScene);

        // Icono también para esta ventana
        mainStage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

        mainStage.show();
    }

    /**
     * Recarga la interfaz principal para aplicar cambios de idioma sin cerrar
     * la ventana.
     *
     * @param stage    estado.
     * @param username El nombre de usuario actual.
     * @param path     La ruta de la biblioteca actual.
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
        System.out.println("[App] Deteniendo aplicación...");
        // 1. Detener el pool de hilos de imágenes
        com.bibliohouse.utils.ImageLoader.shutdown();

        // 2. Cerrar cualquier otra cosa si fuera necesario
        System.out.println("[App] Bye bye!");
        // Forzamos el cierre de la JVM por si quedan hilos "zombie" (como el de AWT o
        // Swing interop)
        System.exit(0);
    }
}
