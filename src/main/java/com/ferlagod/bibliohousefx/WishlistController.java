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

public class WishlistController {

    @FXML
    private FlowPane panelDeseos;

    private PrimaryController mainController;
    private JsonManager jsonManager;
    private List<Libro> listaDeseos;
    private ResourceBundle resources;

    public void initData(PrimaryController mainController, JsonManager jsonManager, List<Libro> listaDeseos, ResourceBundle resources) {
        this.mainController = mainController;
        this.jsonManager = jsonManager;
        this.listaDeseos = listaDeseos;
        this.resources = resources;
        actualizarPanelDeseos();
    }

    @FXML
    private void buscarLibroParaDeseos(ActionEvent event) {
        mainController.buscarLibroParaDeseos();
    }

    public void actualizarPanelDeseos() {
        if (panelDeseos == null) return;
        panelDeseos.getChildren().clear();
        for (Libro libro : listaDeseos) {
            panelDeseos.getChildren().add(crearTarjetaDeseo(libro));
        }
    }

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
