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
import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controlador de la ventana que muestra resultados de búsqueda de OpenLibrary.
 * Permite al usuario seleccionar un libro de los resultados.
 *
 * @author Fernando Lago
 * @version 1.0
 */
public class ResultadosBusquedaController {

    /**
     * Tabla que muestra los libros encontrados.
     */
    @FXML
    private TableView<Libro> tablaResultados;

    /**
     * Columna de título.
     */
    @FXML
    private TableColumn<Libro, String> colTitulo;

    /**
     * Columna de autor.
     */
    @FXML
    private TableColumn<Libro, String> colAutor;

    /**
     * Columna de año de publicación.
     */
    @FXML
    private TableColumn<Libro, String> colAnio;

    /**
     * Columna de editorial.
     */
    @FXML
    private TableColumn<Libro, String> colEditorial;

    /**
     * Libro que el usuario ha seleccionado.
     */
    private Libro libroSeleccionado = null;

    /**
     * Inicializa el controlador. Configura las columnas y añade doble clic para
     * selección rápida.
     */
    @FXML
    public void initialize() {
        // Configurar qué dato de Libro mostrar en cada columna
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colAnio.setCellValueFactory(new PropertyValueFactory<>("año"));
        colEditorial.setCellValueFactory(new PropertyValueFactory<>("editorial"));

        // Permitir selección doble clic
        tablaResultados.setRowFactory(tv -> {
            TableRow<Libro> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                // Si es doble clic y la fila no está vacía, seleccionar libro
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    seleccionar(null);
                }
            });
            return row;
        });
    }

    /**
     * Carga los libros en la tabla.
     *
     * @param resultados Lista de libros a mostrar.
     */
    public void setResultados(List<Libro> resultados) {
        tablaResultados.setItems(FXCollections.observableArrayList(resultados));
    }

    /**
     * Devuelve el libro seleccionado por el usuario.
     *
     * @return El libro seleccionado, o null si no se eligió ninguno.
     */
    public Libro getLibroSeleccionado() {
        return libroSeleccionado;
    }

    /**
     * Confirma la selección del libro actual y cierra la ventana.
     *
     * @param event El evento del botón Seleccionar.
     */
    @FXML
    private void seleccionar(ActionEvent event) {
        libroSeleccionado = tablaResultados.getSelectionModel().getSelectedItem();
        if (libroSeleccionado != null) {
            cerrarVentana();
        }
    }

    /**
     * Cancela la selección y cierra la ventana sin devolver ningún libro.
     *
     * @param event El evento del botón Cancelar.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        libroSeleccionado = null;
        cerrarVentana();
    }

    /**
     * Cierra la ventana actual.
     */
    private void cerrarVentana() {
        Stage stage = (Stage) tablaResultados.getScene().getWindow();
        stage.close();
    }
}
