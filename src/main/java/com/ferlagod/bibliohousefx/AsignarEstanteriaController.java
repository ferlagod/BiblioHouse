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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Este es el controlador para la ventanita que sale cuando añades un libro y
 * tienes que decirle en qué estantería va. Básicamente, te saca una lista de
 * las estanterías que ya tienes y marcas en cuáles quieres meter el libro.
 * También puedes crear estanterías nuevas desde aquí si se te ocurre una sobre
 * la marcha.
 *
 * @author Fernando Lago Dávila
 * @version 1.9
 */
public class AsignarEstanteriaController {

    @FXML
    private ListView<String> listaEstanterias;

    // Mapa para saber qué estantería está marcada y cuál no
    private Map<String, BooleanProperty> mapaSeleccion = new HashMap<>();
    private boolean confirmado = false;

    /**
     * Esto se ejecuta cuando se abre la ventana. Lo que hace es "engañar" a la
     * lista para que en vez de texto normal, muestre cajitas de selección
     * (CheckBoxes). Así puedes marcar varias estanterías a la vez.
     */
    @FXML
    public void initialize() {
        // Configuramos la lista para que use Checkboxes
        listaEstanterias.setCellFactory(CheckBoxListCell.forListView(mapaSeleccion::get));
    }

    /**
     * Con esto cargamos la lista de estanterías. Le pasamos todas las que
     * existen y las que el libro ya tiene marcadas, para que salgan con el
     * check puesto.
     *
     * @param todas Una lista con los nombres de todas las estanterías que hay.
     * @param actuales Una lista con las estanterías donde el libro ya está.
     */
    public void setEstanterias(List<String> todas, List<String> actuales) {
        ObservableList<String> items = FXCollections.observableArrayList();

        for (String estanteria : todas) {
            items.add(estanteria);
            // Si el libro ya está en esa estantería, marcamos true, si no false
            boolean estaMarcada = actuales.contains(estanteria);
            mapaSeleccion.put(estanteria, new SimpleBooleanProperty(estaMarcada));
        }

        listaEstanterias.setItems(items);
    }

    /**
     * Cuando la otra ventana necesita saber qué estanterías se han marcado,
     * llama a este método. Le devuelve una lista con los nombres de las que
     * hemos dejado seleccionadas.
     *
     * @return Una lista con las estanterías elegidas.
     */
    public List<String> getResultado() {
        List<String> seleccionadas = new ArrayList<>();
        if (confirmado) {
            for (String estanteria : mapaSeleccion.keySet()) {
                if (mapaSeleccion.get(estanteria).get()) {
                    seleccionadas.add(estanteria);
                }
            }
        }
        return seleccionadas;
    }

    /**
     * Indica si el usuario confirmó la selección pulsando "Aceptar".
     *
     * @return true si se confirmó, false si se canceló.
     */
    public boolean isConfirmado() {
        return confirmado;
    }

    /**
     * Crea una nueva estantería solicitando el nombre al usuario mediante un
     * diálogo. Si el nombre es válido, la añade a la lista y la selecciona.
     *
     * @param event Evento del botón.
     */
    @FXML
    private void crearNuevaEstanteria(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Estantería");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la estantería:");
        dialog.setGraphic(null); // Quitar icono para estilo más limpio

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            if (!nombre.trim().isEmpty() && !mapaSeleccion.containsKey(nombre)) {
                // Añadir a la lista visual y al mapa
                mapaSeleccion.put(nombre, new SimpleBooleanProperty(true)); // La marcamos por defecto
                listaEstanterias.getItems().add(nombre);
            }
        });
    }

    /**
     * Confirma la selección actual y cierra la ventana.
     *
     * @param event Evento del botón Aceptar.
     */
    @FXML
    private void confirmar(ActionEvent event) {
        confirmado = true;
        cerrarVentana();
    }

    /**
     * Cancela la operación y cierra la ventana sin guardar cambios.
     *
     * @param event Evento del botón Cancelar.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        confirmado = false;
        cerrarVentana();
    }

    /**
     * Cierra la ventana actual.
     */
    private void cerrarVentana() {
        Stage stage = (Stage) listaEstanterias.getScene().getWindow();
        stage.close();
    }
}
