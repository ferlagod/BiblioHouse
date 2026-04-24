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
import javafx.application.Platform;
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
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

/**
 * Controlador del Gestor de Sagas y Colecciones.
 *
 * Agrupa los libros por serie, los muestra con sus portadas ordenados por tomo,
 * y detecta automáticamente los huecos en la colección (tomos que faltan) para
 * resaltarlos en rojo.
 *
 * @author Fernando Lago
 * @version 1.6
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
    private List<Libro> listaLibrosPrincipal;
    private Runnable onDatosCambiados;

    // -----------------------------------------------------------------------
    //  Inicialización pública
    // -----------------------------------------------------------------------
    /**
     * Inicializa el controlador con la lista completa de libros y un callback
     * para guardar los cambios realizados.
     *
     * @param todosLosLibros Lista completa de libros de la biblioteca.
     * @param onDatosCambiados Callback que se ejecutará para guardar los
     * cambios.
     */
    public void initData(List<Libro> todosLosLibros, Runnable onDatosCambiados) {
        this.listaLibrosPrincipal = todosLosLibros;
        this.onDatosCambiados = onDatosCambiados;
        actualizarVistaLateral();
    }

    /**
     * Sobrecarga de {@code initData} para compatibilidad interna al refrescar
     * la vista. No establece un callback de guardado.
     *
     * @param todosLosLibros Lista completa de libros de la biblioteca.
     */
    public void initData(List<Libro> todosLosLibros) {
        this.listaLibrosPrincipal = todosLosLibros;
        actualizarVistaLateral();
    }

    /**
     * Actualiza la lista lateral de sagas, agrupando los libros por su serie
     * normalizada. Crea un mapa de sagas y actualiza el ListView con los
     * nombres de las sagas ordenadas.
     */
    private void actualizarVistaLateral() {
        // Agrupar libros por saga normalizada
        Map<String, List<Libro>> agrupadoNormalizado = listaLibrosPrincipal.stream()
                .filter(l -> l.getSerie() != null && !l.getSerie().trim().isEmpty())
                .collect(Collectors.groupingBy(l -> com.bibliohouse.utils.ProcesadorSagas.normalizar(l.getSerie())));

        // Crear mapa de sagas con nombres originales
        sagasMap = agrupadoNormalizado.values().stream()
                .collect(Collectors.toMap(
                        lista -> lista.get(0).getSerie().trim(),
                        lista -> lista
                ));

        // Ordenar y actualizar la lista de sagas
        List<String> nombres = sagasMap.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        listaSagas.getItems().clear();
        listaSagas.getItems().addAll(nombres);
        listaSagas.setCellFactory(lv -> new SagaListCell());

        // Configurar listener para cambios de selección
        listaSagas.getSelectionModel().selectedItemProperty().removeListener(this::cambioSeleccionListener);
        listaSagas.getSelectionModel().selectedItemProperty().addListener(this::cambioSeleccionListener);
    }

    /**
     * Listener para cambios en la selección de sagas. Muestra la saga
     * seleccionada en el panel principal.
     *
     * @param obs ObservableValue asociado al cambio.
     * @param oldVal Valor anterior de la selección.
     * @param newVal Nuevo valor seleccionado (nombre de la saga).
     */
    private void cambioSeleccionListener(javafx.beans.value.ObservableValue<? extends String> obs, String oldVal, String newVal) {
        if (newVal != null) {
            mostrarSaga(newVal);
        }
    }

    /**
     * Muestra los libros de una saga en el panel principal, incluyendo los
     * tomos faltantes. Ordena los libros por su número en la serie y muestra un
     * resumen de la colección.
     *
     * @param nombreSaga Nombre de la saga a mostrar.
     */
    private void mostrarSaga(String nombreSaga) {
        lblTituloSaga.setText(nombreSaga);
        panelLibros.getChildren().clear();

        List<Libro> libros = sagasMap.get(nombreSaga);
        if (libros == null || libros.isEmpty()) {
            lblResumenSaga.setText("");
            return;
        }

        // Ordenar libros por su posición en la saga
        libros.sort(Comparator.comparingDouble(Libro::getOrdenEnSerie));

        // Calcular tomos totales y faltantes
        double maxOrden = libros.get(libros.size() - 1).getOrdenEnSerie();
        int totalTomos = (int) maxOrden;
        int encontrados = libros.size();
        int huecos = totalTomos - encontrados;

        // Actualizar resumen de la saga
        if (huecos > 0) {
            lblResumenSaga.setText("Tienes " + encontrados + " de " + totalTomos
                    + " tomo" + (totalTomos != 1 ? "s" : "") + " · Faltan " + huecos
                    + " tomo" + (huecos != 1 ? "s" : ""));
        } else {
            lblResumenSaga.setText("Colección completa · " + encontrados
                    + " tomo" + (encontrados != 1 ? "s" : ""));
        }

        // Mostrar los tomos en el panel
        Platform.runLater(() -> {
            for (double i = 1.0; i <= maxOrden; i += 1.0) {
                final double tomo = i;
                boolean encontrado = false;
                Libro libroActual = null;

                // Buscar el libro correspondiente al tomo actual
                for (Libro l : libros) {
                    if (Math.abs(l.getOrdenEnSerie() - tomo) < 0.1) {
                        encontrado = true;
                        libroActual = l;
                        break;
                    }
                }

                // Mostrar tarjeta de libro o hueco
                if (encontrado && libroActual != null) {
                    panelLibros.getChildren().add(crearTarjetaLibro(libroActual, false));
                } else {
                    panelLibros.getChildren().add(crearTarjetaHueco((int) tomo));
                }
            }
        });
    }

    /**
     * Crea una tarjeta visual para un libro en la saga.
     *
     * @param libro Libro del que se creará la tarjeta.
     * @param esHueco Indica si es un hueco (no utilizado aquí, solo para
     * sobrecarga).
     * @return VBox configurado como tarjeta de libro.
     */
    private VBox crearTarjetaLibro(Libro libro, boolean esHueco) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(130);
        tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8;");

        // Número de tomo
        String numTomo = libro.getOrdenEnSerie() > 0 ? "Tomo " + formatarTomo(libro.getOrdenEnSerie()) : "";
        Label lblNumero = new Label(numTomo);
        lblNumero.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

        // Imagen de portada
        ImageView img = new ImageView();
        ImageLoader.load(libro.getPortadaURL(), img, 120, 178);
        img.setFitWidth(120);
        img.setFitHeight(178);
        img.setPreserveRatio(true);
        img.setEffect(new DropShadow(8, Color.color(0, 0, 0, 0.3)));
        Tooltip.install(img, new Tooltip(libro.getTitulo()));

        // Título del libro
        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(125);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        // Efectos de hover
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8; -fx-background-color: #e0e0e0;"));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8;"));

        tarjeta.getChildren().addAll(lblNumero, img, lblTitulo);
        return tarjeta;
    }

    /**
     * Crea una tarjeta visual para un tomo faltante en la saga.
     *
     * @param numeroTomo Número del tomo faltante.
     * @return VBox configurado como tarjeta de hueco.
     */
    private VBox crearTarjetaHueco(int numeroTomo) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(130);
        tarjeta.setStyle("-fx-padding: 6; -fx-background-radius: 8;");

        // Número de tomo
        Label lblNumero = new Label("Tomo " + numeroTomo);
        lblNumero.setStyle("-fx-font-size: 10px; -fx-text-fill: #d32f2f;");

        // Placeholder visual
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(120, 178);
        placeholder.setMaxSize(120, 178);
        placeholder.setStyle("-fx-background-color: #ffebee; -fx-background-radius: 6; -fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 6;");

        Label lblInterrogacion = new Label("?");
        lblInterrogacion.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #d32f2f; -fx-opacity: 0.8;");
        placeholder.getChildren().add(lblInterrogacion);

        // Texto informativo
        Label lblTitulo = new Label("¡Falta el Tomo " + numeroTomo + "!");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(125);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");

        tarjeta.getChildren().addAll(lblNumero, placeholder, lblTitulo);
        return tarjeta;
    }

    /**
     * Formatea el número de tomo para mostrarlo sin decimales si es un número
     * entero.
     *
     * @param orden Número de orden del tomo.
     * @return Cadena formateada del número de tomo.
     */
    private String formatarTomo(double orden) {
        if (orden == Math.floor(orden)) {
            return String.valueOf((int) orden);
        }
        return String.valueOf(orden);
    }

    /**
     * Celda personalizada para mostrar sagas en el ListView. Muestra el nombre
     * de la saga y el número de tomos que contiene, además de un menú
     * contextual con opciones de edición.
     */
    private class SagaListCell extends javafx.scene.control.ListCell<String> {

        private ContextMenu menu;
        private javafx.scene.layout.VBox contenido;
        private javafx.scene.control.Label nombre;
        private javafx.scene.control.Label info;

        public SagaListCell() {
            setStyle("-fx-padding: 8 10; -fx-background-color: transparent;");

            contenido = new javafx.scene.layout.VBox(2);
            nombre = new javafx.scene.control.Label();
            nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333333;");

            info = new javafx.scene.control.Label();
            info.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

            contenido.getChildren().addAll(nombre, info);

            // Menú contextual
            menu = new ContextMenu();
            MenuItem itemRenombrar = new MenuItem("Renombrar saga");
            MenuItem itemBorrarSaga = new MenuItem("Eliminar saga (mantener libros)");
            MenuItem itemBorrarTodo = new MenuItem("Eliminar saga y sus libros");

            itemRenombrar.setOnAction(e -> renombrarSaga(getItem()));
            itemBorrarSaga.setOnAction(e -> eliminarSaga(getItem(), false));
            itemBorrarTodo.setOnAction(e -> eliminarSaga(getItem(), true));

            menu.getItems().addAll(itemRenombrar, itemBorrarSaga, itemBorrarTodo);
        }

        @Override
        protected void updateItem(String saga, boolean empty) {
            super.updateItem(saga, empty);

            if (empty || saga == null) {
                setGraphic(null);
                setText(null);
                setContextMenu(null);
            } else {
                nombre.setText(saga);
                java.util.List<com.bibliohouse.logic.Libro> libros = sagasMap.get(saga);
                int total = libros != null ? libros.size() : 0;
                info.setText(total + " tomo" + (total != 1 ? "s" : ""));

                setGraphic(contenido);
                setText(null);
                setContextMenu(menu);
            }
        }
    }

    /**
     * Elimina una saga, con opción de borrar también sus libros.
     *
     * @param nombreSaga Nombre de la saga a eliminar.
     * @param borrarLibros Si es true, elimina también los libros de la saga.
     */
    private void eliminarSaga(String nombreSaga, boolean borrarLibros) {
        if (nombreSaga == null || !sagasMap.containsKey(nombreSaga)) {
            return;
        }

        List<Libro> librosDeSaga = sagasMap.get(nombreSaga);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");

        if (borrarLibros) {
            alert.setHeaderText("¿Seguro que deseas eliminar la saga y TODOS sus libros?");
            alert.setContentText("Se borrarán " + librosDeSaga.size() + " libros de tu biblioteca. Esta acción no se puede deshacer.");
        } else {
            alert.setHeaderText("¿Seguro que deseas desvincular estos libros de la saga?");
            alert.setContentText("Los libros seguirán en tu biblioteca, pero ya no formarán parte de la colección.");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (borrarLibros) {
                listaLibrosPrincipal.removeAll(librosDeSaga);
            } else {
                for (Libro l : librosDeSaga) {
                    l.setSerie(null);
                    l.setOrdenEnSerie(0);
                }
            }

            ejecutarGuardado();
            initData(listaLibrosPrincipal);

            if (lblTituloSaga.getText().equals(nombreSaga)) {
                lblTituloSaga.setText("Selecciona una saga...");
                lblResumenSaga.setText("");
                panelLibros.getChildren().clear();
            }
        }
    }

    /**
     * Permite renombrar una saga existente. Muestra un diálogo para introducir
     * el nuevo nombre y actualiza todos los libros de la saga con el nuevo
     * nombre.
     *
     * @param nombreAntiguo Nombre actual de la saga.
     */
    private void renombrarSaga(String nombreAntiguo) {
        if (nombreAntiguo == null || !sagasMap.containsKey(nombreAntiguo)) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(nombreAntiguo);
        dialog.setTitle("Renombrar saga");
        dialog.setHeaderText("Introduce el nuevo nombre para la colección:");
        dialog.setContentText("Nombre:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String nuevoNombre = result.get().trim();

            if (!nuevoNombre.isEmpty() && !nuevoNombre.equals(nombreAntiguo)) {
                List<Libro> librosDeSaga = sagasMap.get(nombreAntiguo);

                for (Libro l : librosDeSaga) {
                    l.setSerie(nuevoNombre);
                }

                ejecutarGuardado();
                initData(listaLibrosPrincipal);

                if (lblTituloSaga.getText().equals(nombreAntiguo)) {
                    listaSagas.getSelectionModel().select(nuevoNombre);
                }
            }
        }
    }

    /**
     * Ejecuta el callback de guardado para persistir los cambios realizados.
     * Solo actúa si el callback está definido.
     */
    private void ejecutarGuardado() {
        if (onDatosCambiados != null) {
            onDatosCambiados.run();
        }
    }
}
