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

import java.util.List;
import com.bibliohouse.logic.Libro;
import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Ventana para ver la info de un libro. Sale la portada, resumen y las
 * estrellitas.
 *
 * @author Fernando Lago Dávila
 * @version 1.9
 */
public class DetalleLibroController {

    private static final java.util.logging.Logger LOGGER
            = java.util.logging.Logger.getLogger(DetalleLibroController.class.getName());

    @FXML
    private ImageView imgPortada;
    @FXML
    private Label lblEstadoLectura;
    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblAutor;
    @FXML
    private Label lblEditorial;
    @FXML
    private Label lblAnio;
    @FXML
    private Label lblGenero;
    @FXML
    private Label lblIsbn;
    @FXML
    private Label lblEstrellas;
    @FXML
    private TextArea txtResena;
    @FXML
    private Label lblBadgeDigital;
    @FXML
    private Label lblFormatoLabel;
    @FXML
    private Label lblFormato;
    @FXML
    private Label lblArchivoLabel;
    @FXML
    private Label lblArchivo;
    @FXML
    private Button btnLeerDigital;

    // --- DIARIO Y TRACKER ---
    @FXML
    private javafx.scene.control.TabPane tabPaneDetalles;
    @FXML
    private javafx.scene.control.Spinner<Integer> spinnerPaginaActual;
    @FXML
    private javafx.scene.control.Spinner<Integer> spinnerPaginasTotales;
    @FXML
    private javafx.scene.control.ListView<com.bibliohouse.logic.NotaLectura> listaDiario;

    private List<String> listaGlobalEstanterias;
    private Libro libroActual;
    private String rutaUsuario;

    /**
     * Asigna el libro que se va a visualizar. Al recibirlo, rellena los datos
     * de la ventana automáticamente.
     *
     * @param libro El libro a mostrar.
     */
    public void setLibro(Libro libro) {
        this.libroActual = libro;
        cargarDatos();
    }

    /**
     * Carga los datos del libro actual en los componentes de la interfaz de
     * usuario. Rellena los campos de texto con la información del libro y
     * aplica estilos visuales según el estado de lectura y la calificación.
     *
     */
    private void cargarDatos() {
        if (libroActual == null) {
            return;
        }

        // Textos básicos
        lblTitulo.setText(libroActual.getTitulo());
        lblAutor.setText(libroActual.getAutor() != null ? libroActual.getAutor() : "");
        lblEditorial.setText(libroActual.getEditorial() != null ? libroActual.getEditorial() : "");
        lblAnio.setText(libroActual.getAño() != null ? libroActual.getAño() : "");
        lblGenero.setText(libroActual.getGenero() != null ? libroActual.getGenero() : "");
        lblIsbn.setText(libroActual.getIsbn() != null ? libroActual.getIsbn() : "");

        // Reseña personal
        if (txtResena != null) {
            String resena = libroActual.getReseña();
            txtResena.setText(resena != null ? resena : "");
        }

        // Estado de lectura con 3 estados visuales (no solo leído/pendiente)
        String estadoLectura = libroActual.getEstadoLectura();
        if (estadoLectura == null || estadoLectura.isEmpty()) {
            estadoLectura = "Pendiente";
        }
        switch (estadoLectura) {
            case "Leído" -> {
                lblEstadoLectura.setText("Leído");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #e6f4ea; -fx-text-fill: #1e8e3e; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
            }
            case "Leyendo" -> {
                lblEstadoLectura.setText("Leyendo");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
            }
            default -> {
                lblEstadoLectura.setText("Pendiente");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
            }
        }
        lblEstadoLectura.setVisible(true);

        // Calificación (convertida a estrellas)
        int calif = libroActual.getCalificacion();
        lblEstrellas.setText(generarEstrellas(calif));

        // Cargar imagen de portada
        cargarImagenPortada(libroActual.getPortadaURL());

        // --- E-BOOK / DIGITAL ---
        boolean digital = libroActual.isEsDigital();
        if (lblBadgeDigital != null) {
            lblBadgeDigital.setVisible(digital);
            lblBadgeDigital.setManaged(digital);
        }
        if (lblFormatoLabel != null && lblFormato != null) {
            lblFormatoLabel.setVisible(digital);
            lblFormatoLabel.setManaged(digital);
            lblFormato.setVisible(digital);
            lblFormato.setManaged(digital);
            if (digital && libroActual.getFormatoDigital() != null) {
                lblFormato.setText(libroActual.getFormatoDigital());
            }
        }
        if (lblArchivoLabel != null && lblArchivo != null) {
            lblArchivoLabel.setVisible(digital);
            lblArchivoLabel.setManaged(digital);
            lblArchivo.setVisible(digital);
            lblArchivo.setManaged(digital);
            lblArchivo.setText(libroActual.getNombreArchivoDigital());
        }
        if (btnLeerDigital != null) {
            btnLeerDigital.setVisible(digital);
            btnLeerDigital.setManaged(digital);
        }

        // --- SPINNERS TRACKER ---
        if (spinnerPaginasTotales != null) {
            spinnerPaginasTotales.setEditable(true);
            spinnerPaginasTotales.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, libroActual.getPaginasTotales()));
            spinnerPaginasTotales.valueProperty().addListener((obs, oldVal, newVal) -> {
                libroActual.setPaginasTotales(newVal);
                wasModified = true;
            });
        }
        if (spinnerPaginaActual != null) {
            spinnerPaginaActual.setEditable(true);
            spinnerPaginaActual.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, libroActual.getPaginaActual()));
            spinnerPaginaActual.valueProperty().addListener((obs, oldVal, newVal) -> {
                libroActual.setPaginaActual(newVal);
                wasModified = true;
            });
        }

        // --- DIARIO DE LECTURA ---
        if (listaDiario != null) {
            listaDiario.getItems().setAll(libroActual.getDiario());
            listaDiario.setCellFactory(listView -> new javafx.scene.control.ListCell<com.bibliohouse.logic.NotaLectura>() {
                @Override
                protected void updateItem(com.bibliohouse.logic.NotaLectura nota, boolean empty) {
                    super.updateItem(nota, empty);
                    if (empty || nota == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        javafx.scene.layout.VBox celda = new javafx.scene.layout.VBox(5);
                        celda.setStyle("-fx-padding: 10; -fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-border-color: -color-border-default; -fx-border-radius: 8;");

                        javafx.scene.layout.HBox cabecera = new javafx.scene.layout.HBox(10);
                        Label lblTipo = new Label(nota.getTipo() == com.bibliohouse.logic.NotaLectura.TipoNota.CITA ? "📝 Cita" : "💡 Nota");
                        lblTipo.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");

                        Label lblPagina = new Label("Pág. " + nota.getPaginaReferencia());
                        lblPagina.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");

                        String fechaSolo = nota.getFechaHora().split("T")[0];
                        Label lblFecha = new Label(fechaSolo);
                        lblFecha.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");

                        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                        cabecera.getChildren().addAll(lblTipo, lblPagina, spacer, lblFecha);

                        Label lblTexto = new Label(nota.getTexto());
                        lblTexto.setWrapText(true);

                        if (nota.getTipo() == com.bibliohouse.logic.NotaLectura.TipoNota.CITA) {
                            lblTexto.setStyle("-fx-font-style: italic; -fx-border-color: transparent transparent transparent -color-accent-fg; -fx-border-width: 0 0 0 3; -fx-padding: 0 0 0 8;");
                        }

                        celda.getChildren().addAll(cabecera, lblTexto);
                        setGraphic(celda);
                        setText(null);
                    }
                }
            });
        }
    }

    /**
     * Convierte la puntuación numérica (0-5) en representación gráfica de
     * estrellas. ★ = llena, ☆ = vacía.
     *
     * @param valor Puntuación (0-5).
     * @return El string con los caracteres de estrellas.
     */
    private String generarEstrellas(int valor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < valor) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        return sb.toString();
    }

    /**
     * Carga la imagen de portada. Usa ImageLoader para hacerlo en segundo plano
     * y no congelar la ventana.
     *
     * @param ruta URL o ruta local.
     */
    private void cargarImagenPortada(String ruta) {
        // Usar el cargador asíncrono centralizado con dimensiones de detalle (400x600)
        // ImageLoader ya gestiona si rutas es null o vacía cargando el default.
        com.bibliohouse.utils.ImageLoader.load(ruta, imgPortada, 400, 600);
    }

    /**
     * Establece la lista global de estanterías disponibles por si se quisiera
     * editar el libro.
     *
     * @param lista Lista de nombres de estanterías.
     */
    public void setListaGlobalEstanterias(List<String> lista) {
        this.listaGlobalEstanterias = lista;
    }

    /**
     * Establece la ruta de datos del usuario, necesaria para guardar portadas
     * localmente.
     *
     * @param rutaUsuario Ruta al directorio del usuario.
     */
    public void setRutaUsuario(String rutaUsuario) {
        this.rutaUsuario = rutaUsuario;
    }

    /**
     * Abre la ventana de edición para el libro actual. Si se guardan cambios,
     * recarga los datos en esta ventana.
     *
     * @param event El evento del botón Editar.
     */
    @FXML
    private void editarLibro(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editar_libro.fxml"));

            // 1. PRIMERO cargamos el idioma y se lo pasamos al loader
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(
                    "com.ferlagod.bibliohousefx.messages", App.getCurrentLocale());
            loader.setResources(bundle);

            // 2. LUEGO cargamos la vista (¡Este orden es obligatorio!)
            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(this.libroActual);
            // Pasar rutaUsuario para que la portada se guarde localmente correctamente
            if (this.rutaUsuario != null) {
                controller.setRutaUsuario(this.rutaUsuario);
            }

            if (this.listaGlobalEstanterias != null) {
                controller.setEstanteriasDisponibles(this.listaGlobalEstanterias);
            }

            Stage stage = new Stage();
            stage.setTitle("Editar: " + libroActual.getTitulo());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(lblTitulo.getScene().getWindow());
            stage.setMaximized(true);
            stage.showAndWait();

            // Si guardó los cambios, refrescamos la vista de detalles
            if (controller.isGuardado()) {
                cargarDatos();
                this.wasModified = true;
            }

        } catch (IOException e) {
            LOGGER.log(java.util.logging.Level.SEVERE,
                    "[DetalleLibroController] Error al abrir la ventana de edición", e);
        }
    }

    private boolean wasModified = false;

    /**
     * Indica si el libro ha sufrido alguna modificación (edición) durante la
     * visualización.
     *
     * @return true si se guardaron cambios, false en caso contrario.
     */
    public boolean isModified() {
        return wasModified;
    }

    /**
     * Cierra la ventana de detalles.
     *
     * @param event El evento del botón Cerrar.
     */
    @FXML
    private void cerrar(ActionEvent event) {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }

    /**
     * Abre el archivo digital enlazado con el lector por defecto del sistema.
     *
     * @param event El evento del botón Leer.
     */
    @FXML
    private void abrirArchivoDigital(ActionEvent event) {
        if (libroActual == null || libroActual.getRutaArchivoDigital() == null) {
            return;
        }

        File archivo = new File(libroActual.getRutaArchivoDigital());
        if (!archivo.exists()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("El archivo ya no existe en la ruta guardada:\n" + archivo.getAbsolutePath());
            alert.showAndWait();
            return;
        }

        try {
            java.awt.Desktop.getDesktop().open(archivo);
        } catch (IOException e) {
            LOGGER.log(java.util.logging.Level.WARNING,
                    "No se pudo abrir el archivo digital", e);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo abrir el archivo.");
            alert.showAndWait();
        }
    }

    @FXML
    private void abrirDialogoNuevaNota(ActionEvent event) {
        javafx.scene.control.Dialog<com.bibliohouse.logic.NotaLectura> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Nueva Nota");
        dialog.setHeaderText("Añadir al Diario de Lectura");

        javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        javafx.scene.control.ComboBox<com.bibliohouse.logic.NotaLectura.TipoNota> cmbTipo = new javafx.scene.control.ComboBox<>();
        cmbTipo.getItems().addAll(com.bibliohouse.logic.NotaLectura.TipoNota.values());
        cmbTipo.setValue(com.bibliohouse.logic.NotaLectura.TipoNota.CITA);

        javafx.scene.control.Spinner<Integer> spnPagina = new javafx.scene.control.Spinner<>(0, 10000, libroActual.getPaginaActual());
        spnPagina.setEditable(true);

        javafx.scene.control.TextArea txtContenido = new javafx.scene.control.TextArea();
        txtContenido.setPromptText("Escribe aquí tu nota o cita...");
        txtContenido.setPrefRowCount(4);
        txtContenido.setWrapText(true);

        grid.add(new Label("Tipo:"), 0, 0);
        grid.add(cmbTipo, 1, 0);
        grid.add(new Label("Página:"), 0, 1);
        grid.add(spnPagina, 1, 1);
        grid.add(new Label("Contenido:"), 0, 2);
        grid.add(txtContenido, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                return new com.bibliohouse.logic.NotaLectura(cmbTipo.getValue(), txtContenido.getText(), spnPagina.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nota -> {
            libroActual.getDiario().add(nota);
            libroActual.setPaginaActual(spnPagina.getValue());
            if (spinnerPaginaActual != null) {
                spinnerPaginaActual.getValueFactory().setValue(spnPagina.getValue());
            }
            if (listaDiario != null) {
                listaDiario.getItems().setAll(libroActual.getDiario());
            }
            wasModified = true;
        });
    }
}
