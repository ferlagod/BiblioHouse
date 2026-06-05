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
import com.bibliohouse.logic.Libro;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Controlador para la vista de la lista de deseos. Gestiona la interfaz y las
 * acciones de los libros que el usuario quiere guardar para el futuro.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class WishlistController {

    @FXML
    private FlowPane panelDeseos;

    private PrimaryController mainController;
    private JsonManager jsonManager;
    private List<Libro> listaDeseos;
    private ResourceBundle resources;

    /**
     * Inicializa el controlador con los datos necesarios para funcionar.
     *
     * @param mainController El controlador principal de la aplicación.
     * @param jsonManager El gestor encargado de guardar y cargar datos.
     * @param listaDeseos La lista actual de libros guardados en la lista de
     * deseos.
     * @param resources El archivo de textos para los idiomas de la interfaz.
     */
    public void initData(PrimaryController mainController, JsonManager jsonManager, List<Libro> listaDeseos, ResourceBundle resources) {
        this.mainController = mainController;
        this.jsonManager = jsonManager;
        this.listaDeseos = listaDeseos;
        this.resources = resources;
        actualizarPanelDeseos();
    }

    /**
     * Acción que se ejecuta al pulsar el botón de buscar un libro. Pide al
     * controlador principal que inicie la búsqueda.
     *
     * @param event El evento del clic en el botón.
     */
    @FXML
    private void buscarLibroParaDeseos(ActionEvent event) {
        mainController.buscarLibroParaDeseos();
    }

    /**
     * Limpia la pantalla y vuelve a dibujar todos los libros que están en la
     * lista de deseos.
     */
    public void actualizarPanelDeseos() {
        if (panelDeseos == null) {
            return;
        }
        panelDeseos.getChildren().clear();
        for (Libro libro : listaDeseos) {
            panelDeseos.getChildren().add(crearTarjetaDeseo(libro));
        }
    }

    /**
     * Crea la tarjeta visual para un solo libro. Incluye la imagen, el título y
     * los botones para moverlo a la biblioteca o eliminarlo.
     *
     * @param libro El libro que se va a mostrar en la tarjeta.
     * @return Un contenedor (VBox) listo para mostrarse en la pantalla.
     */
    private VBox crearTarjetaDeseo(Libro libro) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tarjeta.setPrefWidth(140);
        tarjeta.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        javafx.scene.image.ImageView img = new javafx.scene.image.ImageView();
        com.bibliohouse.utils.ImageLoader.load(libro.getPortadaURL(), img, 110, 160);

        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(130);
        lblTitulo.setAlignment(javafx.geometry.Pos.CENTER);
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #333;");

        Button btnMover = new Button(resources.getString("wishlist.move"));
        btnMover.setStyle("-fx-font-size: 10px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        btnMover.setMaxWidth(Double.MAX_VALUE);
        btnMover.setOnAction(e -> mainController.moverDeseoABiblioteca(libro));

        Button btnBorrar = new Button(resources.getString("wishlist.delete"));
        btnBorrar.setStyle("-fx-font-size: 10px; -fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-cursor: hand;");
        btnBorrar.setOnAction(e -> {
            listaDeseos.remove(libro);
            jsonManager.guardarDeseos(listaDeseos);
            actualizarPanelDeseos();
        });

        tarjeta.getChildren().addAll(img, lblTitulo, btnMover, btnBorrar);
        return tarjeta;
    }
}
