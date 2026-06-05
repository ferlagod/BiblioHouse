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
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
 * @version 1.8
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
            case "Leído":
                lblEstadoLectura.setText("Leído");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #e6f4ea; -fx-text-fill: #1e8e3e; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
                break;
            case "Leyendo":
                lblEstadoLectura.setText("Leyendo");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
                break;
            default:
                lblEstadoLectura.setText("Pendiente");
                lblEstadoLectura.setStyle(
                        "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold;");
                break;
        }
        lblEstadoLectura.setVisible(true);

        // Calificación (convertida a estrellas)
        int calif = libroActual.getCalificacion();
        lblEstrellas.setText(generarEstrellas(calif));

        // Cargar imagen de portada
        cargarImagenPortada(libroActual.getPortadaURL());
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
}
