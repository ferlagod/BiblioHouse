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
import javafx.stage.Stage;

/**
 * Controlador para la ventana de configuración. Permite cambiar opciones como
 * el tema, la ruta de datos, etc.
 *
 * @author Fernando Lago
 * @version 1.3
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

    /**
     * Inicializa los datos de la ventana de configuración. Carga las
     * preferencias actuales y configura los listeners.
     *
     * @param manager El gestor de datos JSON.
     * @param main    El controlador principal de la aplicación.
     */
    public void initData(JsonManager manager, PrimaryController main) {
        this.jsonManager = manager;
        this.mainController = main;

        // Cargar la ruta de datos actual
        txtRutaDatos.setText(mainController.getRutaUsuario());

        // Cargar credenciales NextCloud desde preferencias
        Map<String, String> savedPrefs = jsonManager.cargarPreferencias();
        if (txtNextcloudUrl != null) {
            txtNextcloudUrl.setText(savedPrefs.getOrDefault("nextcloud.url", ""));
        }
        if (txtNextcloudUser != null) {
            txtNextcloudUser.setText(savedPrefs.getOrDefault("nextcloud.user", ""));
        }
        if (txtNextcloudPass != null) {
            txtNextcloudPass.setText(savedPrefs.getOrDefault("nextcloud.password", ""));
        }

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
        // Lógica de guardado
        if (!txtNombreBiblioteca.getText().isEmpty() && mainController != null) {
            // Obtenemos el Stage principal a través del controlador
            Stage mainStage = (Stage) ((Stage) txtNombreBiblioteca.getScene().getWindow()).getOwner();
            if (mainStage != null) {
                mainStage.setTitle(txtNombreBiblioteca.getText());
            }
        }

        // Guardar días de préstamo
        if (spinnerDiasPrestamo != null && mainController != null) {
            mainController.setDueDaysLimit(spinnerDiasPrestamo.getValue());
        }

        // Guardar credenciales NextCloud en preferencias
        if (txtNextcloudUrl != null) {
            Map<String, String> prefs = jsonManager.cargarPreferencias();
            prefs.put("nextcloud.url", txtNextcloudUrl.getText().trim());
            prefs.put("nextcloud.user", txtNextcloudUser.getText().trim());
            prefs.put("nextcloud.password", txtNextcloudPass.getText());
            jsonManager.guardarPreferencias(prefs);
        }

        // Cerrar al guardar
        cancelar(event);
    }

    /**
     * Prueba la conexión con el servidor NextCloud configurado.
     * La operación se ejecuta en un hilo de fondo para no bloquear la UI.
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
        lblNextcloudStatus.setText("...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return service.testConexionConMensaje();
            }
        };
        task.setOnSucceeded(e -> {
            String error = task.getValue();
            if (error == null) {
                lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
                lblNextcloudStatus.setText(resources.getString("config.sync.status.ok"));
            } else {
                lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
                lblNextcloudStatus
                        .setText(MessageFormat.format(resources.getString("config.sync.status.error"), error));
            }
        });
        task.setOnFailed(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(resources.getString("config.sync.status.error"),
                    task.getException().getMessage()));
        });
        new Thread(task, "nextcloud-test").start();
    }

    /**
     * Sube los archivos JSON de la base de datos al servidor NextCloud.
     * La operación se ejecuta en un hilo de fondo.
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
        lblNextcloudStatus.setText("...");

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
        });
        task.setOnFailed(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(resources.getString("config.sync.upload.error"),
                    task.getException().getMessage()));
        });
        new Thread(task, "nextcloud-upload").start();
    }

    /**
     * Descarga los archivos JSON desde NextCloud y sobreescribe la base de datos
     * local.
     * Muestra un diálogo de confirmación antes de proceder.
     * La operación se ejecuta en un hilo de fondo.
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
        lblNextcloudStatus.setText("...");

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
        });
        task.setOnFailed(e -> {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(resources.getString("config.sync.download.error"),
                    task.getException().getMessage()));
        });
        new Thread(task, "nextcloud-download").start();
    }

    /**
     * Valida que los campos de NextCloud no estén vacíos y muestra un mensaje
     * de error si faltan datos.
     *
     * @return {@code true} si todos los campos tienen valor, {@code false} si
     *         alguno falta.
     */
    private boolean validarCamposNextcloud() {
        if (txtNextcloudUrl.getText().isBlank() || txtNextcloudUser.getText().isBlank()
                || txtNextcloudPass.getText().isBlank()) {
            lblNextcloudStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #c62828;");
            lblNextcloudStatus.setText(MessageFormat.format(
                    resources.getString("config.sync.status.error"), "Rellena URL, usuario y contraseña."));
            return false;
        }
        return true;
    }

    /**
     * Crea un {@link NextCloudSyncService} con los valores actuales de los campos
     * UI,
     * o muestra un error y devuelve {@code null} si los parámetros son inválidos.
     *
     * @return El servicio configurado, o {@code null} si los campos son inválidos.
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
                mostrarAlerta("Error", "No se encuentra la carpeta de datos para hacer backup.");
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
                mostrarAlerta("Backup", msg);

            } catch (IOException e) {
                String msg = java.text.MessageFormat.format(resources.getString("config.backup.error"), e.getMessage());
                mostrarAlerta("Error", msg);
            }
        }
    }

    /**
     * Borra todos los libros de la biblioteca tras confirmar con el usuario.
     * ¡Acción destructiva!
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
     * Restaura la configuración de fábrica (pendiente de implementar).
     *
     * @param event El evento del botón.
     */
    @FXML
    private void restaurarFabrica(ActionEvent event) {
        mostrarAlerta(resources.getString("config.alert.info.title"),
                resources.getString("config.alert.factory.pending"));
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
     * @param titulo    Título de la alerta.
     * @param contenido Mensaje de la alerta.
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
