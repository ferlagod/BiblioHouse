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
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.Socio;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controlador para la vista de historial de préstamos. Muestra los préstamos
 * devueltos con información del libro, socio y fechas.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class HistorialController {

    @FXML
    private TableView<Prestamo> tablaHistorial;
    @FXML
    private TableColumn<Prestamo, String> colHistorialLibro;
    @FXML
    private TableColumn<Prestamo, String> colHistorialSocio;
    @FXML
    private TableColumn<Prestamo, String> colHistorialFechaPrestamo;
    @FXML
    private TableColumn<Prestamo, String> colHistorialFechaDevolucion;

    private PrimaryController mainController;

    /**
     * Inicializa el controlador con los datos necesarios.
     *
     * @param mainController Controlador principal para acceder a listas
     * globales.
     * @param historial Lista filtrada de préstamos devueltos.
     */
    public void initData(PrimaryController mainController, FilteredList<Prestamo> historial) {
        this.mainController = mainController;
        tablaHistorial.setItems(historial);
        configurarColumnas();
    }

    /**
     * Configura las columnas de la tabla con sus correspondientes
     * CellValueFactory. Maneja casos donde el libro o socio puedan haber sido
     * borrados.
     */
    private void configurarColumnas() {
        colHistorialLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p.getLibroId() != null) {
                return mainController.getListaLibrosCompleta().stream()
                        .filter(l -> l.getId().equals(p.getLibroId()))
                        .findFirst()
                        .map(Libro::getTitulo)
                        .map(javafx.beans.property.SimpleStringProperty::new)
                        .orElse(new javafx.beans.property.SimpleStringProperty(p.getTituloLibro() + " (Borrado)"));
            }
            return new javafx.beans.property.SimpleStringProperty(p.getTituloLibro());
        });

        colHistorialSocio.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            return mainController.getListaSocios().stream()
                    .filter(s -> s.getNumeroSocio() == p.getNumeroSocio())
                    .findFirst()
                    .map(Socio::getNombreCompleto)
                    .map(javafx.beans.property.SimpleStringProperty::new)
                    .orElse(new javafx.beans.property.SimpleStringProperty(p.getNombreSocio() + " (Borrado)"));
        });

        colHistorialFechaPrestamo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fechaPrestamoFormateada"));
        colHistorialFechaDevolucion.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fechaDevolucionFormateada"));
    }
}
