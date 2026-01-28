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
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de configuración. Permite cambiar opciones como
 * el tema, la ruta de datos, etc.
 *
 * @author Fernando Lago
 * @version 1.1
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

    /**
     * Selector numérico para configurar los días límite de préstamo.
     */
    @FXML
    private Spinner<Integer> spinnerDiasPrestamo;

    @FXML
    private java.util.ResourceBundle resources;

    private JsonManager jsonManager;
    private PrimaryController mainController;

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

        comboIdioma.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> obs, String oldVal, String newVal) -> {
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
                    PrimaryController newMainController = App.reloadUI(mainStage, mainController.getUsuarioActual(),
                            mainController.getRutaUsuario());
                    // Recargar esta misma ventana de Configuración para aplicar el idioma
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(ConfiguracionController.this.getClass().getResource("configuracion.fxml"));
                    loader.setResources(java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages",
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

        // Cerrar al guardar
        cancelar(event);
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
}
