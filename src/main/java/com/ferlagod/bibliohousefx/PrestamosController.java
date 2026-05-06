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
 * SIN NINGUNA GARANTÍA; sin incluso la garantía implícita de
 * COMERCIABILIDAD o APTITUD PARA UN PROPÓSITO PARTICULAR. Vea la
 * Licencia Pública General de GNU para más detalles.
 *
 * Usted debería haber recibido una copia de la Licencia Pública General de GNU
 * junto con este programa. Si no es así, vea <https://www.gnu.org/licenses/>.
 */
package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.PrestamoService;
import com.bibliohouse.logic.Socio;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Controlador para la gestión de préstamos de libros. Permite realizar
 * préstamos, marcar devoluciones y visualizar el estado de los préstamos
 * activos.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class PrestamosController {

    @FXML
    private ComboBox<Libro> comboLibrosPrestamo;
    @FXML
    private ComboBox<Socio> comboSocios;
    @FXML
    private TableView<Prestamo> tablaPrestamos;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoLibro;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoSocio;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoFecha;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoDevolucion;

    private PrimaryController mainController;
    private PrestamoService prestamoService;

    /**
     * Inicializa el controlador con los datos necesarios.
     *
     * @param mainController Controlador principal.
     * @param prestamoService Servicio de préstamos.
     * @param librosDisponibles Lista de libros disponibles para préstamo.
     * @param socios Lista de socios.
     * @param prestamosActivos Lista filtrada de préstamos activos.
     * @param resources Recursos de internacionalización.
     * @param dueDaysLimit Límite de días para considerar un préstamo como
     * vencido.
     */
    public void initData(PrimaryController mainController, PrestamoService prestamoService,
            ObservableList<Libro> librosDisponibles, ObservableList<Socio> socios,
            FilteredList<Prestamo> prestamosActivos, ResourceBundle resources, int dueDaysLimit) {

        this.mainController = mainController;
        this.prestamoService = prestamoService;

        comboLibrosPrestamo.setItems(librosDisponibles);
        comboSocios.setItems(socios);
        tablaPrestamos.setItems(prestamosActivos);

        configurarColumnas();
        configurarFiltros();
        configurarEstilosFilas(dueDaysLimit);
    }

    /**
     * Configura las columnas de la tabla con sus CellValueFactory. Maneja casos
     * donde el libro o socio puedan haber sido borrados.
     */
    private void configurarColumnas() {
        colPrestamoLibro.setCellValueFactory(cellData -> {
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

        colPrestamoSocio.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            return mainController.getListaSocios().stream()
                    .filter(s -> s.getNumeroSocio() == p.getNumeroSocio())
                    .findFirst()
                    .map(Socio::getNombreCompleto)
                    .map(javafx.beans.property.SimpleStringProperty::new)
                    .orElse(new javafx.beans.property.SimpleStringProperty(p.getNombreSocio() + " (Borrado)"));
        });

        colPrestamoFecha.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fechaPrestamoFormateada"));
        colPrestamoDevolucion.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fechaDevolucionFormateada"));
    }

    /**
     * Configura los filtros para los ComboBox de libros y socios.
     */
    private void configurarFiltros() {
        mainController.setupFilteringComboBoxPublic(comboLibrosPrestamo, Libro::getTitulo);
        mainController.setupFilteringComboBoxPublic(comboSocios, Socio::getNombreCompleto);
    }

    /**
     * Configura los estilos de las filas para resaltar préstamos vencidos.
     *
     * @param dueDaysLimit Límite de días para considerar un préstamo como
     * vencido.
     */
    private void configurarEstilosFilas(int dueDaysLimit) {
        tablaPrestamos.setRowFactory(tv -> new TableRow<Prestamo>() {
            @Override
            protected void updateItem(Prestamo item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("overdue-loan");
                if (item != null && !empty) {
                    if (item.getFechaDevolucion() == null && item.getFechaPrestamo() != null) {
                        java.time.LocalDate dueDate = item.getFechaPrestamo().plusDays(dueDaysLimit);
                        if (dueDate.isBefore(java.time.LocalDate.now())) {
                            getStyleClass().add("overdue-loan");
                        }
                    }
                }
            }
        });
    }

    @FXML
    private void nuevoSocio(ActionEvent event) {
        mainController.nuevoSocioPublic();
    }

    @FXML
    private void gestionarSocios(ActionEvent event) {
        mainController.gestionarSociosPublic();
    }

    /**
     * Realiza un nuevo préstamo con el libro y socio seleccionados. Valida los
     * datos y muestra mensajes de éxito o error.
     */
    @FXML
    private void realizarPrestamo(ActionEvent event) {
        Libro libroSeleccionado = comboLibrosPrestamo.getValue();
        Socio socio = comboSocios.getValue();

        if (libroSeleccionado == null || socio == null) {
            mainController.mostrarAlertaPublic("Datos faltantes", "Por favor, selecciona un libro y un socio de las listas.");
            return;
        }

        Libro libroOriginal = mainController.getListaLibrosCompleta().stream()
                .filter(l -> l.getId().equals(libroSeleccionado.getId()))
                .findFirst()
                .orElse(null);

        if (libroOriginal == null) {
            mainController.mostrarAlertaPublic("Error", "No se pudo localizar el libro en la base de datos.");
            return;
        }

        try {
            prestamoService.realizarPrestamo(libroOriginal, socio);
            mainController.actualizarVistasPrestamo();
            mainController.setMensajeEstado("Préstamo realizado: " + libroOriginal.getTitulo());
            mainController.mostrarAlertaPublic("Éxito", "Préstamo registrado correctamente.");
        } catch (IllegalArgumentException e) {
            mainController.mostrarAlertaPublic("Error al prestar", e.getMessage());
        }
    }

    /**
     * Marca un préstamo como devuelto. Valida la selección y muestra mensajes
     * de éxito o error.
     */
    @FXML
    private void marcarDevuelto(ActionEvent event) {
        Prestamo p = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (p == null) {
            mainController.mostrarAlertaPublic("Selección necesaria", "Selecciona un préstamo de la lista para devolverlo.");
            return;
        }

        try {
            prestamoService.marcarDevuelto(p);
            mainController.actualizarVistasPrestamo();
            mainController.setMensajeEstado("Devolución registrada correctamente (ID: " + p.getLibroId() + ")");
        } catch (IllegalArgumentException e) {
            mainController.mostrarAlertaPublic("Aviso", e.getMessage());
        }
    }

    /**
     * Selecciona un libro en el ComboBox de préstamos.
     *
     * @param libro Libro a seleccionar.
     */
    public void seleccionarLibro(Libro libro) {
        for (Libro l : comboLibrosPrestamo.getItems()) {
            if (l.equals(libro)) {
                comboLibrosPrestamo.getSelectionModel().select(l);
                break;
            }
        }
        comboLibrosPrestamo.requestFocus();
    }

    /**
     * Selecciona un socio en el ComboBox de préstamos.
     *
     * @param socio Socio a seleccionar.
     */
    public void seleccionarSocio(Socio socio) {
        for (Socio s : comboSocios.getItems()) {
            if (s.equals(socio)) {
                comboSocios.getSelectionModel().select(s);
                break;
            }
        }
        comboSocios.requestFocus();
    }

    /**
     * Actualiza los libros disponibles en el ComboBox de préstamos.
     *
     * @param librosDisponibles Lista actualizada de libros disponibles.
     */
    public void refrescarLibrosDisponibles(ObservableList<Libro> librosDisponibles) {
        if (comboLibrosPrestamo != null) {
            comboLibrosPrestamo.setItems(librosDisponibles);
        }
        if (tablaPrestamos != null) {
            tablaPrestamos.refresh();
        }
    }
}
