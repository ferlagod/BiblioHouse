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

import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.NextCloudSyncService;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de configuración. Permite cambiar opciones como
 * el tema, la ruta de datos, etc.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class ConfiguracionController {

    @FXML
    private CheckBox chkConfirmarBorrado;
    @FXML
    private CheckBox chkAbrirMaximizada;
    @FXML
    private TextField txtNombreBiblioteca;
    @FXML
    private TextField txtRutaDatos;
    @FXML
    private ComboBox<String> comboIdioma;
    @FXML
    private ComboBox<String> comboTema;
    @FXML
    private Spinner<Integer> spinnerDiasPrestamo;

    // NextCloud Sync fields
    @FXML
    private TextField txtNextcloudUrl;
    @FXML
    private TextField txtNextcloudUser;
    @FXML
    private PasswordField txtNextcloudPass;
    @FXML
    private Label lblNextcloudStatus;

    @FXML
    private java.util.ResourceBundle resources;
    private JsonManager jsonManager;
    private PrimaryController mainController;

    private static final Logger LOGGER = Logger.getLogger(ConfiguracionController.class.getName());
    /**
     * Nodo de Preferences donde se guarda la contraseña de NextCloud (SEC-02).
     */
    private static final String NC_PREFS_NODE = "com/ferlagod/bibliohousefx/nextcloud";
    private static final String NC_PREF_PASS = "password";

    /**
     * Inicializa los datos de la ventana de configuración. Carga las
     * preferencias actuales y configura los listeners.
     *
     * @param manager El gestor de datos JSON.
     * @param main El controlador principal de la aplicación.
     */
    public void initData(JsonManager manager, PrimaryController main) {
        this.jsonManager = manager;
        this.mainController = main;

        // Cargar la ruta de datos actual
        txtRutaDatos.setText(mainController.getRutaUsuario());

        // Cargar credenciales NextCloud
        // URL y usuario desde JSON (no sensibles), contraseña desde Preferences/llavero (SEC-02)
        Map<String, String> savedPrefs = jsonManager.cargarPreferencias();
        if (txtNextcloudUrl != null) {
            txtNextcloudUrl.setText(savedPrefs.getOrDefault("nextcloud.url", ""));
        }
        if (txtNextcloudUser != null) {
            txtNextcloudUser.setText(savedPrefs.getOrDefault("nextcloud.user", ""));
        }

        // --- INICIO BLOQUE CORREGIDO ---
        if (txtNextcloudPass != null) {
            Preferences ncPrefs = Preferences.userRoot().node(NC_PREFS_NODE);
            String passEncriptada = ncPrefs.get(NC_PREF_PASS, "");
            String passLimpia = "";

            if (!passEncriptada.isEmpty()) {
                // AQUI ESTA LA CLAVE: Desencriptar antes de ponerla en la interfaz
                passLimpia = com.bibliohouse.utils.SeguridadUtil.desencriptar(passEncriptada);
            } else {
                // Migración silenciosa desde el antiguo JSON
                String legacyPass = savedPrefs.getOrDefault("nextcloud.password", "");
                if (!legacyPass.isEmpty()) {
                    passLimpia = legacyPass;
                    ncPrefs.put(NC_PREF_PASS, com.bibliohouse.utils.SeguridadUtil.encriptar(legacyPass));
                    savedPrefs.remove("nextcloud.password");
                    jsonManager.guardarPreferencias(savedPrefs);
                    LOGGER.log(Level.INFO, "Contraseña de NextCloud migrada al llavero del sistema.");
                }
            }

            // Ponemos la contraseña legible en el campo
            txtNextcloudPass.setText(passLimpia);
        }
        // --- FIN BLOQUE CORREGIDO ---

        // Configurar el ComboBox de Idioma
        comboIdioma.getItems().setAll("Español", "English", "Català", "Galego", "Euskara", "Português");

        // Seleccionar idioma actual
        String actualLang = App.getCurrentLocale().getLanguage();
        switch (actualLang) {
            case "en":
                comboIdioma.getSelectionModel().select("English");
                break;
            case "ca":
                comboIdioma.getSelectionModel().select("Català");
                break;
            case "gl":
                comboIdioma.getSelectionModel().select("Galego");
                break;
            case "eu":
                comboIdioma.getSelectionModel().select("Euskara");
                break;
            case "pt":
                comboIdioma.getSelectionModel().select("Português");
                break;
            default:
                comboIdioma.getSelectionModel().select("Español");
                break;
        }

        comboIdioma.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends String> obs, String oldVal, String newVal) -> {
                    if (newVal != null) {
                        String langCode = "es";
                        switch (newVal) {
                            case "English":
                                langCode = "en";
                                break;
                            case "Català":
                                langCode = "ca";
                                break;
                            case "Galego":
                                langCode = "gl";
                                break;
                            case "Euskara":
                                langCode = "eu";
                                break;
                            case "Português":
                                langCode = "pt";
                                break;
                            default:
                                break;
                        }
                        // Guardar preferencia
                        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
                        prefs.put("language", langCode);
                        // Actualizar JSON también si es necesario
                        java.util.Map<String, String> prefsMap = jsonManager.cargarPreferencias();
                        prefsMap.put("language", langCode);
                        jsonManager.guardarPreferencias(prefsMap);
                        // Cambiar locale global
                        App.setLocale(langCode);
                        // Recargar ventana principal (Hot-Swap) y mantener Configuración abierta
                        Stage settingsStage = (Stage) comboIdioma.getScene().getWindow();
                        Stage mainStage = (Stage) settingsStage.getOwner();
                        try {
                            // Recargar Main y obtener nuevo controlador
                            PrimaryController newMainController = App.reloadUI(mainStage,
                                    mainController.getUsuarioActual(),
                                    mainController.getRutaUsuario());
                            // Recargar esta misma ventana de Configuración para aplicar el idioma
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                    ConfiguracionController.this.getClass().getResource("configuracion.fxml"));
                            loader.setResources(
                                    java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages",
                                            App.getCurrentLocale()));
                            javafx.scene.Parent newConfigRoot = loader.load();
                            ConfiguracionController newConfigController = loader.getController();
                            newConfigController.initData(jsonManager, newMainController);
                            // Reemplazar contenido (manteniendo tamaño y posición)
                            settingsStage.getScene().setRoot(newConfigRoot);
                        } catch (IOException e) {
                        }
                    }
                });

        // Configurar el ComboBox de Tema (7 temas de AtlantaFX)
        comboTema.getItems().setAll(
                "Claro (Primer Light)",
                "Oscuro (Primer Dark)",
                "Nord Claro (Nord Light)",
                "Nord Oscuro (Nord Dark)",
                "Cupertino Claro (macOS Light)",
                "Cupertino Oscuro (macOS Dark)",
                "Dracula"
        );
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        String savedTheme = prefs.get("theme", "Claro (Primer Light)");
        comboTema.getSelectionModel().select(savedTheme);

        comboTema.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends String> obs, String oldVal, String newVal) -> {
                    if (newVal != null) {
                        prefs.put("theme", newVal);
                        App.applyTheme(newVal);
                    }
                });

        // Configurar el Spinner de días de préstamo (como ya tenías)
        if (spinnerDiasPrestamo != null) {
            int currentDays = mainController.getDueDaysLimit();
            spinnerDiasPrestamo
                    .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, currentDays));
        }

    }

    /**
     * Guarda los cambios realizados en la configuración (como el nombre de la
     * biblioteca) y cierra la ventana.
     *
     * @param event El evento del botón Guardar.
     */
    @FXML
    private void guardarCambios(ActionEvent event) {
        // Lógica de guardado del título
        if (!txtNombreBiblioteca.getText().isEmpty() && mainController != null) {
            Stage mainStage = (Stage) ((Stage) txtNombreBiblioteca.getScene().getWindow()).getOwner();
            if (mainStage != null) {
                mainStage.setTitle(txtNombreBiblioteca.getText());
            }
        }

        // Guardar días de préstamo
        if (spinnerDiasPrestamo != null && mainController != null) {
            mainController.setDueDaysLimit(spinnerDiasPrestamo.getValue());
        }

        // --- GESTIÓN DE CREDENCIALES NEXTCLOUD CON CIFRADO ---
        if (txtNextcloudUrl != null) {
            Map<String, String> ncPrefs = jsonManager.cargarPreferencias();
            String url = txtNextcloudUrl.getText().trim();
            String user = txtNextcloudUser.getText().trim();
            String passClaro = txtNextcloudPass.getText(); // Contraseña escrita por el usuario

            ncPrefs.put("nextcloud.url", url);
            ncPrefs.put("nextcloud.user", user);
            ncPrefs.remove("nextcloud.password"); // Seguridad: Asegurar que nunca vaya al JSON
            jsonManager.guardarPreferencias(ncPrefs);

            // Cifrar la contraseña antes de guardarla en el registro del SO
            Preferences osPrefs = Preferences.userRoot().node(NC_PREFS_NODE);
            if (passClaro.isBlank()) {
                osPrefs.remove(NC_PREF_PASS);
            } else {
                // USAMOS SeguridadUtil para que no sea legible por humanos
                String passEncriptada = com.bibliohouse.utils.SeguridadUtil.encriptar(passClaro);
                osPrefs.put(NC_PREF_PASS, passEncriptada);
            }

            // Activar / desactivar auto-sync inmediatamente
            if (!url.isBlank() && !user.isBlank() && !passClaro.isBlank()) {
                try {
                    // El servicio recibe la contraseña en claro para poder conectar
                    NextCloudSyncService syncService = new NextCloudSyncService(url, user, passClaro);
                    final String localDir = jsonManager.getRutaDatosUsuario();
                    jsonManager.setAutoSyncTask(() -> {
                        try {
                            syncService.subirBaseDatos(localDir);
                        } catch (IOException ex) {
                            LOGGER.log(Level.WARNING, "Auto-sync NextCloud fallido: " + ex.getMessage());
                        }
                    });
                } catch (IllegalArgumentException ignored) {
                    jsonManager.setAutoSyncTask(null);
                }
            } else {
                jsonManager.setAutoSyncTask(null);
            }
        }

        // Cerrar ventana
        cancelar(event);
    }

    /**
     * Prueba la conexión con el servidor NextCloud configurado. La operación se
     * ejecuta en un hilo de fondo para no bloquear la UI.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void probarConexionNextcloud(ActionEvent event) {
        if (!validarCamposNextcloud()) {
            return;
        }
        NextCloudSyncService service = crearServicioNextcloud();
        if (service == null) {
            return;
        }

        lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        lblNextcloudStatus.setText(resources.getString("config.sync.test.loading"));

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return service.testConexionConMensaje();
            }
        };

        task.setOnSucceeded(e -> {
            String error = task.getValue();
            if (error == null) {
                // Todo OK
                lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
                lblNextcloudStatus.setText(resources.getString("config.sync.status.ok"));

                // Mostrar alerta de éxito
                mostrarAlerta(resources.getString("config.sync.test.success.title"), resources.getString("config.sync.test.success.content"));
            } else {
                // Hay un error de autenticación, URL, etc.
                lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
                lblNextcloudStatus.setText(resources.getString("config.sync.test.error"));
                
                // Mostrar alerta de ERROR
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(resources.getString("config.sync.test.error.title"));
                alert.setHeaderText(resources.getString("config.sync.test.error.header"));
                alert.setContentText(error);
                alert.showAndWait();
            }
        });

        task.setOnFailed(e -> {
            // Error crítico del hilo o del programa
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(resources.getString("config.sync.test.critical"));

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resources.getString("config.sync.test.critical.title"));
            alert.setHeaderText(resources.getString("config.sync.test.critical.header"));
            alert.setContentText(task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task, "nextcloud-test").start();
    }

    /**
     * Sube los archivos JSON de la base de datos al servidor NextCloud. La
     * operación se ejecuta en un hilo de fondo.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void subirNextcloud(ActionEvent event) {
        if (!validarCamposNextcloud()) {
            return;
        }
        NextCloudSyncService service = crearServicioNextcloud();
        if (service == null) {
            return;
        }
        String localDir = jsonManager.getRutaDatosUsuario();

        lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        lblNextcloudStatus.setText(resources.getString("config.sync.upload.loading"));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                service.subirBaseDatos(localDir);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
            lblNextcloudStatus.setText(resources.getString("config.sync.upload.success"));

            // Mostrar alerta de ÉXITO
            mostrarAlerta(resources.getString("config.sync.upload.success.title"), resources.getString("config.sync.upload.success.content"));
        });

        task.setOnFailed(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(resources.getString("config.sync.upload.error"),
                    task.getException().getMessage()));

            // Mostrar alerta de ERROR
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resources.getString("config.sync.upload.error.title"));
            alert.setHeaderText(resources.getString("config.sync.upload.error.header"));
            alert.setContentText(task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task, "nextcloud-upload").start();
    }

    /**
     * Descarga los archivos JSON desde NextCloud y sobreescribe la base de
     * datos local. Muestra un diálogo de confirmación antes de proceder. La
     * operación se ejecuta en un hilo de fondo.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void descargarNextcloud(ActionEvent event) {
        if (!validarCamposNextcloud()) {
            return;
        }

        // Confirmación antes de sobreescribir
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("NextCloud");
        confirm.setHeaderText(null);
        confirm.setContentText(resources.getString("config.sync.download.confirm"));
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        NextCloudSyncService service = crearServicioNextcloud();
        if (service == null) {
            return;
        }
        String localDir = jsonManager.getRutaDatosUsuario();

        lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        lblNextcloudStatus.setText(resources.getString("config.sync.download.loading"));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                service.descargarBaseDatos(localDir);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
            lblNextcloudStatus.setText(resources.getString("config.sync.download.success"));

            // Recargar datos en el controlador principal
            mainController.initData(mainController.getUsuarioActual(), mainController.getRutaUsuario());

            // Mostrar alerta de ÉXITO
            mostrarAlerta(resources.getString("config.sync.download.success.title"), resources.getString("config.sync.download.success.content"));
        });

        task.setOnFailed(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(resources.getString("config.sync.download.error"),
                    task.getException().getMessage()));

            // Mostrar alerta de ERROR
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resources.getString("config.sync.download.error.title"));
            alert.setHeaderText(resources.getString("config.sync.download.error.header"));
            alert.setContentText(task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task, "nextcloud-download").start();
    }

    /**
     * Valida que los campos de NextCloud no estén vacíos y muestra un mensaje
     * de error si faltan datos.
     *
     * @return {@code true} si todos los campos tienen valor, {@code false} si
     * alguno falta.
     */
    private boolean validarCamposNextcloud() {
        if (txtNextcloudUrl.getText().isBlank() || txtNextcloudUser.getText().isBlank()
                || txtNextcloudPass.getText().isBlank()) {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(
                    resources.getString("config.sync.status.error"), resources.getString("config.sync.validation.error")));
            return false;
        }
        return true;
    }

    /**
     * Crea un {@link NextCloudSyncService} con los valores actuales de los
     * campos UI, o muestra un error y devuelve {@code null} si los parámetros
     * son inválidos.
     *
     * @return El servicio configurado, o {@code null} si los campos son
     * inválidos.
     */
    private NextCloudSyncService crearServicioNextcloud() {
        try {
            return new NextCloudSyncService(
                    txtNextcloudUrl.getText().trim(),
                    txtNextcloudUser.getText().trim(),
                    txtNextcloudPass.getText());
        } catch (IllegalArgumentException ex) {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(
                    resources.getString("config.sync.status.error"), ex.getMessage()));
            return null;
        }
    }

    /**
     * Abre la carpeta donde se guardan los datos de la aplicación en el
     * explorador de archivos.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void abrirCarpetaDatos(ActionEvent event) {
        try {
            File ruta = new File(txtRutaDatos.getText());
            // Si la carpeta no existe, intentamos abrir la home
            if (!ruta.exists()) {
                ruta = new File(System.getProperty("user.home"));
            }
            Desktop.getDesktop().open(ruta);
        } catch (IOException e) {
            mostrarAlerta(resources.getString("config.alert.error.title"),
                    resources.getString("config.alert.error.browser"));
        }
    }

    /**
     * Exporta una copia de seguridad (ZIP) de toda la carpeta de datos.
     *
     * @param event Evento del botón.
     */
    @FXML
    private void exportarBackup(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(resources.getString("config.backup.button"));
        fileChooser.setInitialFileName("BiblioHouse_Backup_" + java.time.LocalDate.now() + ".zip");
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("ZIP files (*.zip)", "*.zip"));

        File destZip = fileChooser.showSaveDialog(txtRutaDatos.getScene().getWindow());

        if (destZip != null) {
            File sourceDir = new File(txtRutaDatos.getText());
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                mostrarAlerta(resources.getString("config.alert.error.title"), resources.getString("config.backup.error.folder"));
                return;
            }

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(destZip))) {
                java.nio.file.Path sourcePath = sourceDir.toPath();

                java.nio.file.Files.walk(sourcePath)
                        .filter(path -> !java.nio.file.Files.isDirectory(path))
                        .forEach((Path path) -> {
                            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(
                                    sourcePath.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                java.nio.file.Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                System.err.println("Error zippeando: " + path);
                            }
                        });

                String msg = java.text.MessageFormat.format(resources.getString("config.backup.success"),
                        destZip.getName());
                mostrarAlerta(resources.getString("config.backup.button"), msg);

            } catch (IOException e) {
                String msg = java.text.MessageFormat.format(resources.getString("config.backup.error"), e.getMessage());
                mostrarAlerta(resources.getString("config.alert.error.title"), msg);
            }
        }
    }

    /**
     * Borra todos los libros de la biblioteca tras confirmar con el usuario.
     * Acción destructiva
     *
     * @param event El evento del botón.
     */
    @FXML
    private void borrarTodosLosLibros(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(resources.getString("config.alert.warning.title"));
        alert.setHeaderText(resources.getString("config.alert.warning.header"));
        alert.setContentText(resources.getString("config.alert.warning.content"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            jsonManager.guardarLibros(new ArrayList<>());
            // Refrescar ventana principal llamando a initData de nuevo
            mainController.initData(mainController.getUsuarioActual(), txtRutaDatos.getText());
            mostrarAlerta(resources.getString("config.alert.done.title"),
                    resources.getString("config.alert.done.content"));
        }
    }

    /**
     * Restaura la configuración de fábrica, borrando todos los datos del
     * usuario.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void restaurarFabrica(ActionEvent event) {
        // Pedimos confirmación de seguridad escribiendo "BORRAR"
        TextInputDialog confirmDialog = new TextInputDialog();
        confirmDialog.setTitle(resources.getString("config.reset.title"));
        confirmDialog.setHeaderText(resources.getString("config.reset.header"));
        confirmDialog.setContentText(resources.getString("config.reset.content"));

        Optional<String> result = confirmDialog.showAndWait();
        if (result.isPresent()) {
            if (result.get().trim().equalsIgnoreCase(resources.getString("config.reset.keyword"))) {
                ejecutarBorradoDeFabrica();
            } else {
                mostrarAlerta(resources.getString("config.reset.cancel.title"), resources.getString("config.reset.cancel.content"));
            }
        }
    }

    /**
     * Ejecuta el borrado completo de todos los datos de la aplicación. Este
     * método no puede deshacerse y eliminará permanentemente todos los datos.
     */
    private void ejecutarBorradoDeFabrica() {
        try {
            // 1. Borrar archivos de la carpeta del usuario (biblioteca, covers, etc.)
            if (mainController != null && mainController.getRutaUsuario() != null) {
                File dirUsuario = new File(mainController.getRutaUsuario());
                if (dirUsuario.exists() && dirUsuario.isDirectory()) {
                    borrarDirectorioRecursivo(dirUsuario);
                }
            }

            // 2. Limpiar las credenciales y configuraciones del registro del Sistema Operativo
            Preferences osPrefs = Preferences.userRoot().node(NC_PREFS_NODE);
            osPrefs.clear(); // Elimina las credenciales cifradas de NextCloud

            Preferences appPrefs = Preferences.userNodeForPackage(App.class);
            appPrefs.clear(); // Elimina idioma y configuraciones globales

            // 3. Detener la sincronización activa si la hubiera
            if (jsonManager != null) {
                jsonManager.setAutoSyncTask(null);
            }

            // 4. Mostrar aviso y cerrar la app
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle(resources.getString("config.reset.success.title"));
            info.setHeaderText(resources.getString("config.reset.success.header"));
            info.setContentText(resources.getString("config.reset.success.content"));
            info.showAndWait();

            Platform.exit(); // Cerrar interfaz de JavaFX
            System.exit(0);  // Forzar cierre total para matar hilos en segundo plano (ej. descargas de portadas pendientes)

        } catch (BackingStoreException e) {
            LOGGER.log(Level.SEVERE, "Error durante la restauración a fábrica", e);
            mostrarAlerta(resources.getString("config.alert.error.title"), resources.getString("config.reset.error.content") + " " + e.getMessage());
        }
    }

    /**
     * Elimina un directorio y todo su contenido de forma recursiva. Este método
     * borra todos los archivos y subdirectorios dentro del directorio
     * especificado, y finalmente elimina el directorio vacío.
     *
     * @param directorio El directorio a eliminar.
     */
    private void borrarDirectorioRecursivo(File directorio) {
        if (directorio.exists()) {
            File[] archivos = directorio.listFiles();
            if (archivos != null) {
                for (File f : archivos) {
                    if (f.isDirectory()) {
                        borrarDirectorioRecursivo(f);
                    } else {
                        f.delete();
                    }
                }
            }
            directorio.delete();
        }
    }

    /**
     * Cierra la ventana de configuración sin guardar cambios.
     *
     * @param event El evento del botón Cancelar.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        // Obtenemos el botón que fue pulsado
        Node source = (Node) event.getSource();
        // Obtenemos la ventana que contiene ese botón
        Stage stage = (Stage) source.getScene().getWindow();
        // La cerramos
        stage.close();
    }

    /**
     * Muestra una alerta simple de información.
     *
     * @param titulo Título de la alerta.
     * @param contenido Mensaje de la alerta.
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    /**
     * Abre la página de Liberapay del proyecto en el navegador por defecto del
     * sistema operativo. Detecta automáticamente el sistema operativo (Windows,
     * macOS o Linux) y usa el comando apropiado para abrir el navegador.
     *
     * <p>
     * URL destino: https://liberapay.com/ferlagod./</p>
     *
     */
    @FXML
    private void abrirLiberapay() {
        String url = "https://liberapay.com/ferlagod./";
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el navegador para Liberapay", e);
        }
    }
}
