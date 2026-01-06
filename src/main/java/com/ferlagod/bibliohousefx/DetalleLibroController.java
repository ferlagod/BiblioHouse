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

import com.bibliohouse.logic.Libro;
import java.io.IOException;

import java.util.List;
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
 * Muestra el detalle completo de un libro.
 * Incluye la portada, resumen y valoración. Es la ficha técnica del libro.
 *
 * @author Fernando Lago
 * @version 1.0
 */
public class DetalleLibroController {

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

    /**
     * Asigna el libro que se va a visualizar.
     * Al recibirlo, rellena los datos de la ventana automáticamente.
     *
     * @param libro El libro a mostrar.
     */
    public void setLibro(Libro libro) {
        this.libroActual = libro;
        cargarDatos();
    }

    /**
     * Pone cada dato en su sitio: título, autor, estado de lectura...
     * También pinta las estrellas de valoración y carga la portada.
     */
    private void cargarDatos() {
        if (libroActual == null) {
            return;
        }

        // Textos básicos
        lblTitulo.setText(libroActual.getTitulo());
        lblAutor.setText(libroActual.getAutor());
        lblEditorial.setText(libroActual.getEditorial());
        lblAnio.setText(String.valueOf(libroActual.getAño()));
        lblGenero.setText(libroActual.getGenero());
        lblIsbn.setText(libroActual.getIsbn());

        // Reseña (controlamos nulos)
        // Estado Leído
        if (libroActual.isLeido()) {
            lblEstadoLectura.setText("Leído");
            lblEstadoLectura.setStyle(
                    "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-background-radius: 12; -fx-padding: 4 12 4 12;");
            lblEstadoLectura.setVisible(true);
        } else {
            lblEstadoLectura.setText("Pendiente");
            lblEstadoLectura.setStyle(
                    "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-background-radius: 12; -fx-padding: 4 12 4 12;");
            lblEstadoLectura.setVisible(true);
        }

        // Calificación (Estrellas)
        int calif = libroActual.getCalificacion();
        lblEstrellas.setText(generarEstrellas(calif));

        // Portada
        cargarImagenPortada(libroActual.getPortadaURL());
    }

    /**
     * Convierte la puntuación numérica (0-5) en representación gráfica de
     * estrellas.
     * ★ = llena, ☆ = vacía.
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
     * Carga la imagen de portada.
     * Usa ImageLoader para hacerlo en segundo plano y no congelar la ventana.
     *
     * @param ruta URL o ruta local.
     */
    private void cargarImagenPortada(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            imgPortada.setImage(null);
            return; // Se queda la imagen por defecto del FXML (si la hubiera) o null
        }

        // Usar el cargador asíncrono centralizado con dimensiones de detalle (400x600)
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
     * Abre la ventana de edición para el libro actual. Si se guardan cambios,
     * recarga los datos en esta ventana.
     *
     * @param event El evento del botón Editar.
     */
    @FXML
    private void editarLibro(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editar_libro.fxml"));
            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(this.libroActual); // Pasamos el libro actual

            if (this.listaGlobalEstanterias != null) {
                controller.setEstanteriasDisponibles(this.listaGlobalEstanterias);
            }

            Stage stage = new Stage();
            stage.setTitle("Editar: " + libroActual.getTitulo());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(lblTitulo.getScene().getWindow());

            stage.showAndWait(); // Esperamos a que cierre

            // Si guardó los cambios, refrescamos la vista de detalles
            if (controller.isGuardado()) {
                cargarDatos(); // Recargamos los datos en esta ventana
                this.wasModified = true;
            }

        } catch (IOException e) {
            e.printStackTrace();
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
