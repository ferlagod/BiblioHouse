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
import com.bibliohouse.utils.ImageLoader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Controlador del Gestor de Sagas y Colecciones.
 *
 * Agrupa los libros por serie, los muestra con sus portadas ordenados por tomo,
 * y detecta automáticamente los huecos en la colección (tomos que faltan) para
 * resaltarlos en rojo.
 *
 * @author Fernando Lago
 * @version 1.5
 */
public class SagasController {

    @FXML
    private ListView<String> listaSagas;
    @FXML
    private Label lblTituloSaga;
    @FXML
    private Label lblResumenSaga;
    @FXML
    private FlowPane panelLibros;

    //Mapa: nombre de serie → lista de libros pertenecientes.
    private Map<String, List<Libro>> sagasMap;

    // -----------------------------------------------------------------------
    //  Inicialización pública
    // -----------------------------------------------------------------------
    /**
     * Recibe la biblioteca completa, filtra los libros que pertenecen a una
     * serie y construye el listado lateral ordenado alfabéticamente de forma
     * robusta.
     *
     * @param todosLosLibros Lista completa de libros del usuario.
     */
    public void initData(List<Libro> todosLosLibros) {
        // 1. Agrupar los libros usando el nombre NORMALIZADO (fusión "antitorpes")
        // Así "Harry Potter", "harry potter" y "Harry Pótter" caen en el mismo saco.
        Map<String, List<Libro>> agrupadoNormalizado = todosLosLibros.stream()
                .filter(l -> l.getSerie() != null && !l.getSerie().trim().isEmpty())
                .collect(Collectors.groupingBy(l -> com.bibliohouse.utils.ProcesadorSagas.normalizar(l.getSerie())));

        // 2. Reconstruir el mapa para la interfaz visual. 
        // Usamos el nombre original (con sus mayúsculas y tildes) del primer libro del grupo.
        sagasMap = agrupadoNormalizado.values().stream()
                .collect(Collectors.toMap(
                        lista -> lista.get(0).getSerie(), // Nombre "bonito" para mostrar
                        lista -> lista
                ));

        // Llenar la lista lateral en orden alfabético
        List<String> nombres = sagasMap.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        listaSagas.getItems().clear(); // Limpiamos por si se recarga la vista
        listaSagas.getItems().addAll(nombres);

        // Estilizar las celdas de la lista lateral
        listaSagas.setCellFactory(lv -> new SagaListCell());

        // Reaccionar a la selección
        listaSagas.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        mostrarSaga(newVal);
                    }
                });

        // Seleccionar la primera saga automáticamente si existe
        if (!nombres.isEmpty()) {
            listaSagas.getSelectionModel().selectFirst();
        }
    }

    // -----------------------------------------------------------------------
    //  Lógica de visualización
    // -----------------------------------------------------------------------
    /**
     * Renderiza el panel de portadas para la saga indicada, detectando huecos.
     *
     * @param nombreSaga Nombre de la saga seleccionada.
     */
    private void mostrarSaga(String nombreSaga) {
        lblTituloSaga.setText(nombreSaga);
        panelLibros.getChildren().clear();

        List<Libro> libros = sagasMap.get(nombreSaga);
        if (libros == null || libros.isEmpty()) {
            lblResumenSaga.setText("");
            return;
        }

        // Ordenar por número de tomo
        libros.sort(Comparator.comparingDouble(Libro::getOrdenEnSerie));

        double maxOrden = libros.get(libros.size() - 1).getOrdenEnSerie();
        int totalTomos = (int) maxOrden;
        int encontrados = libros.size();
        int huecos = totalTomos - encontrados;

        // Actualizar resumen
        if (huecos > 0) {
            lblResumenSaga.setText("Tienes " + encontrados + " de " + totalTomos
                    + " tomo" + (totalTomos != 1 ? "s" : "") + " · Faltan " + huecos
                    + " tomo" + (huecos != 1 ? "s" : ""));
        } else {
            lblResumenSaga.setText("Colección completa · " + encontrados
                    + " tomo" + (encontrados != 1 ? "s" : ""));
        }

        // Iterar desde el tomo 1 hasta el máximo para detectar huecos
        for (double i = 1.0; i <= maxOrden; i += 1.0) {
            final double tomo = i;
            boolean encontrado = false;
            Libro libroActual = null;

            for (Libro l : libros) {
                if (Math.abs(l.getOrdenEnSerie() - tomo) < 0.1) {
                    encontrado = true;
                    libroActual = l;
                    break;
                }
            }

            if (encontrado && libroActual != null) {
                panelLibros.getChildren().add(crearTarjetaLibro(libroActual, false));
            } else {
                // ¡Hueco! Falta este tomo
                panelLibros.getChildren().add(crearTarjetaHueco((int) tomo));
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Construcción de tarjetas
    // -----------------------------------------------------------------------
    /**
     * Crea la tarjeta visual para un libro que sí está en la colección.
     */
    private VBox crearTarjetaLibro(Libro libro, boolean esHueco) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(130);
        tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8;");

        // Número de tomo
        String numTomo = libro.getOrdenEnSerie() > 0
                ? "Tomo " + formatarTomo(libro.getOrdenEnSerie())
                : "";

        Label lblNumero = new Label(numTomo);
        lblNumero.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;"); // Letra gris oscura

        // Portada con sombra
        ImageView img = new ImageView();
        ImageLoader.load(libro.getPortadaURL(), img, 120, 178);
        img.setFitWidth(120);
        img.setFitHeight(178);
        img.setPreserveRatio(true);

        DropShadow sombra = new DropShadow(8, Color.color(0, 0, 0, 0.3)); // Sombra más suave
        img.setEffect(sombra);

        Tooltip.install(img, new Tooltip(libro.getTitulo()));

        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(125);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #333333;"); // Letra muy oscura

        // Hover: resaltar tarjeta con gris clarito
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(
                "-fx-padding: 6; -fx-background-radius: 8; -fx-background-color: #e0e0e0;"));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(
                "-fx-padding: 6; -fx-background-radius: 8;"));

        tarjeta.getChildren().addAll(lblNumero, img, lblTitulo);
        return tarjeta;
    }

    /**
     * Crea la tarjeta visual para un hueco (tomo que falta en la colección).
     */
    private VBox crearTarjetaHueco(int numeroTomo) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(130);
        tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8;");

        Label lblNumero = new Label("Tomo " + numeroTomo);
        lblNumero.setStyle("-fx-font-size: 10px; -fx-text-fill: #d32f2f;"); // Letra roja

        // Placeholder rojo claro con interrogación
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(120, 178);
        placeholder.setMaxSize(120, 178);
        placeholder.setStyle(
                "-fx-background-color: #ffebee; "
                + "-fx-background-radius: 6; "
                + "-fx-border-color: #d32f2f; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 6;");

        Label lblInterrogacion = new Label("?");
        lblInterrogacion.setStyle(
                "-fx-font-size: 48px; -fx-font-weight: bold; "
                + "-fx-text-fill: #d32f2f; -fx-opacity: 0.8;");

        placeholder.getChildren().add(lblInterrogacion);

        Label lblTitulo = new Label("¡Falta el Tomo " + numeroTomo + "!");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(125);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-text-fill: #d32f2f;");

        tarjeta.getChildren().addAll(lblNumero, placeholder, lblTitulo);
        return tarjeta;
    }

    // -----------------------------------------------------------------------
    //  Utilidades
    // -----------------------------------------------------------------------
    /**
     * Formatea el número de tomo: si es entero lo muestra sin decimales, si es
     * decimal (p.ej. 1.5) lo muestra tal cual.
     */
    private String formatarTomo(double orden) {
        if (orden == Math.floor(orden)) {
            return String.valueOf((int) orden);
        }
        return String.valueOf(orden);
    }

    // -----------------------------------------------------------------------
    //  Celda personalizada para la lista lateral
    // -----------------------------------------------------------------------
    /**
     * Celda de la lista lateral que muestra el nombre de la saga y cuántos
     * tomos tiene.
     */
    private class SagaListCell extends javafx.scene.control.ListCell<String> {

        @Override
        protected void updateItem(String saga, boolean empty) {
            super.updateItem(saga, empty);
            if (empty || saga == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox contenido = new VBox(2);

                Label nombre = new Label(saga);
                nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333333;");
                nombre.setWrapText(true);

                List<Libro> libros = sagasMap.get(saga);
                int total = libros != null ? libros.size() : 0;
                Label info = new Label(total + " tomo" + (total != 1 ? "s" : ""));
                info.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

                contenido.getChildren().addAll(nombre, info);
                setGraphic(contenido);
                setText(null);
                setStyle("-fx-padding: 8 10; -fx-background-color: transparent;");
            }
        }
    }
}
