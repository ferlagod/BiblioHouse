package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.Socio;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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

    public void initData(PrimaryController mainController, FilteredList<Prestamo> historial) {
        this.mainController = mainController;
        tablaHistorial.setItems(historial);
        configurarColumnas();
    }

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
