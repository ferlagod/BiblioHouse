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
import com.bibliohouse.logic.Socio;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la gestión general de la lista de socios. Muestra la tabla
 * de socios y permite añadir, editar o eliminarlos.
 *
 * @author Fernando Lago
 * @version 1.0
 */
public class SociosManagerController {

    @FXML
    private TableView<Socio> tablaSocios;
    @FXML
    private TableColumn<Socio, Integer> colNumero;
    @FXML
    private TableColumn<Socio, String> colNombre;
    @FXML
    private TableColumn<Socio, String> colApellidos;
    @FXML
    private TableColumn<Socio, String> colDni;
    @FXML
    private TableColumn<Socio, String> colDomicilio;

    private ObservableList<Socio> listaSocios;
    private JsonManager jsonManager;
    private PrimaryController mainController; // Referencia al controlador principal para refrescar

    /**
     * Inicializa el controlador. Configura las columnas de la tabla de socios.
     */
    @FXML
    public void initialize() {
        // Configurar columnas
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroSocio"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colDomicilio.setCellValueFactory(new PropertyValueFactory<>("domicilio"));

        // Permite doble clic para editar
        tablaSocios.setRowFactory(tv -> {
            TableRow<Socio> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    editarSocio(null);
                }
            });
            return row;
        });
    }

    /**
     * Inicializa los datos del controlador con la lista de socios y referencias
     * necesarias.
     *
     * @param socios Lista de socios a mostrar.
     * @param manager Gestor de JSON para guardar cambios.
     * @param controller Referencia al controlador principal.
     */
    public void initData(List<Socio> socios, JsonManager manager, PrimaryController controller) {
        this.jsonManager = manager;
        this.mainController = controller;
        // Creamos una observable list a partir de la lista que viene del controlador
        // principal (por referencia)
        this.listaSocios = FXCollections.observableArrayList(socios);
        tablaSocios.setItems(this.listaSocios);
        tablaSocios.refresh();
    }

    /**
     * Abre la ventana de edición para el socio seleccionado.
     *
     * @param event El evento del botón Editar.
     */
    @FXML
    private void editarSocio(ActionEvent event) {
        Socio socioSeleccionado = tablaSocios.getSelectionModel().getSelectedItem();
        if (socioSeleccionado == null) {
            mostrarAlerta("Selección Requerida", "Por favor, selecciona un socio para editar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gestion_socios.fxml"));
            Parent root = loader.load();

            GestionSociosController controller = loader.getController();

            // Llama al método para configurar el controlador en modo EDICIÓN
            controller.setSocioToEdit(socioSeleccionado);

            Stage stage = new Stage();
            stage.setTitle("Editar Socio: " + socioSeleccionado.getNombreCompleto());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaSocios.getScene().getWindow());
            stage.showAndWait();

            // Al cerrar, si se guardó, refrescamos
            if (controller.isGuardado()) {
                // Como los objetos en Java se pasan por referencia, socioSeleccionado ya está
                // modificado.
                // Simplemente guardamos toda la lista en JSON y refrescamos la UI.
                jsonManager.guardarSocios(listaSocios);
                tablaSocios.refresh(); // Refresca la tabla
                mainController.recargarDatosPrestamos(); // Notifica al principal para refrescar combos/tablas
            }

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la ventana de edición.");
        }
    }

    /**
     * Elimina el socio seleccionado de la lista. Verifica que no tenga
     * préstamos activos.
     *
     * @param event El evento del botón Eliminar.
     */
    @FXML
    private void eliminarSocio(ActionEvent event) {
        Socio socioSeleccionado = tablaSocios.getSelectionModel().getSelectedItem();
        if (socioSeleccionado == null) {
            mostrarAlerta("Selección Requerida", "Por favor, selecciona un socio para eliminar.");
            return;
        }

        // Comprobación de préstamos activos
        // Usamos el método getListaPrestamos() del PrimaryController
        boolean hasActiveLoans = mainController.getListaPrestamos().stream()
                .anyMatch(p -> p.getNumeroSocio() == socioSeleccionado.getNumeroSocio()
                && p.getFechaDevolucion() == null);

        if (hasActiveLoans) {
            mostrarAlerta("Error de Eliminación",
                    "No puedes eliminar a este socio porque tiene préstamos activos pendientes de devolución.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar a " + socioSeleccionado.getNombreCompleto());
        alert.setContentText("¿Estás seguro de que quieres eliminar a este socio? Esta acción es permanente.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            listaSocios.remove(socioSeleccionado);
            jsonManager.guardarSocios(listaSocios);
            mainController.recargarDatosPrestamos(); // Notifica al principal
            mostrarAlerta("Eliminación Completa", "El socio ha sido eliminado correctamente.");
        }
    }

    /**
     * Cierra la ventana de gestión de socios.
     *
     * @param event El evento del botón Cerrar.
     */
    @FXML
    private void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) tablaSocios.getScene().getWindow();
        stage.close();
    }

    /**
     * Muestra una alerta al usuario.
     *
     * @param titulo Título de la alerta.
     * @param contenido Mensaje de la alerta.
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
