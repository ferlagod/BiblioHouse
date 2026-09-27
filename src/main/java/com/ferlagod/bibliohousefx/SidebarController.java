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

import com.bibliohouse.logic.AppEventBus;
import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controlador de la barra lateral (Sidebar). Gestiona la lista de estanterías,
 * los filtros por categoría y el widget interactivo del reto de lectura anual.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class SidebarController {

    public static final String VISTA_TODOS = "Todos los libros";
    public static final String VISTA_DESEOS = "Lista de Deseos";
    public static final String VISTA_DIGITAL = "E-books";

    private static final List<String> ESTANTERIAS_DEFAULT = List.of(
            "Novela", "Ciencia Ficción", "Fantasía", "Historia", "Tecnología",
            "Aventura", "Biografía", "Romántica", "Poesía", "Teatro", "Infantil", "Ensayo"
    );

    @FXML
    private ListView<String> listaEstanterias;
    @FXML
    private VBox widgetRetoAnual;
    @FXML
    private VBox widgetRetoVacio;
    @FXML
    private Label lblTituloReto;
    @FXML
    private Label lblPorcentajeReto;
    @FXML
    private ProgressBar progresoReto;
    @FXML
    private Label lblEstadoReto;

    private JsonManager jsonManager;
    private ObservableList<Libro> listaLibrosCompleta;
    private Map<String, String> preferencias;
    private ResourceBundle resources;

    /**
     * Inicializa el controlador suscribiéndose a los eventos reactivos de libros modificados,
     * eliminados y estanterías actualizadas a través de {@link AppEventBus}.
     */
    @FXML
    public void initialize() {
        // Suscribirse a cambios en los libros para actualizar el reto anual y contadores en tiempo real
        AppEventBus.getInstance().subscribe(AppEventBus.LibroModificadoEvent.class, e -> {
            actualizarRetoAnual();
            if (listaEstanterias != null) {
                listaEstanterias.refresh();
            }
        });
        AppEventBus.getInstance().subscribe(AppEventBus.LibroEliminadoEvent.class, e -> {
            actualizarRetoAnual();
            if (listaEstanterias != null) {
                listaEstanterias.refresh();
            }
        });
        AppEventBus.getInstance().subscribe(AppEventBus.EstanteriasActualizadasEvent.class, e -> cargarListaEstanterias());
    }

    /**
     * Inicializa los datos necesarios para la barra lateral.
     *
     * @param jsonManager Gestor de datos JSON.
     * @param listaLibros Lista observable de todos los libros.
     * @param preferencias Preferencias del usuario.
     * @param resources Textos localizados.
     */
    public void initData(JsonManager jsonManager, ObservableList<Libro> listaLibros,
                         Map<String, String> preferencias, ResourceBundle resources) {
        this.jsonManager = jsonManager;
        this.listaLibrosCompleta = listaLibros;
        this.preferencias = preferencias;
        this.resources = resources;

        cargarListaEstanterias();
        configurarEventosSeleccion();
        actualizarRetoAnual();
    }

    /**
     * Configura la escucha de selección de estanterías y publica el evento en el bus.
     */
    private void configurarEventosSeleccion() {
        if (listaEstanterias != null) {
            listaEstanterias.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    AppEventBus.getInstance().publish(new AppEventBus.FiltroEstanteriaEvent(newVal));
                }
            });
        }
    }

    /**
     * Carga y refresca las estanterías en el ListView con sus iconos representativos y contadores numéricos.
     */
    public void cargarListaEstanterias() {
        if (listaEstanterias == null || jsonManager == null) {
            return;
        }

        String seleccionActual = listaEstanterias.getSelectionModel().getSelectedItem();
        List<String> estanterias = jsonManager.cargarEstanterias();

        if (estanterias == null || estanterias.isEmpty()) {
            estanterias = new ArrayList<>(ESTANTERIAS_DEFAULT);
            jsonManager.guardarEstanterias(estanterias);
        }

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add(VISTA_TODOS);
        items.add(VISTA_DESEOS);
        items.add(VISTA_DIGITAL);
        items.addAll(estanterias);
        listaEstanterias.setItems(items);

        listaEstanterias.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    HBox fila = new HBox(8);
                    fila.setAlignment(Pos.CENTER_LEFT);
                    fila.setPadding(new Insets(2, 4, 2, 4));

                    String icono = switch (item) {
                        case VISTA_TODOS -> "📚";
                        case VISTA_DESEOS -> "⭐";
                        case VISTA_DIGITAL -> "📱";
                        default -> "📁";
                    };
                    Label lblIcono = new Label(icono);
                    lblIcono.setStyle("-fx-font-size: 13px;");

                    Label lblTexto = new Label(item);
                    lblTexto.setStyle("-fx-font-size: 12px; -fx-font-weight: 500;");
                    HBox.setHgrow(lblTexto, Priority.ALWAYS);

                    long cantidad = contarLibrosEnEstanteria(item);
                    Label lblBadge = new Label(String.valueOf(cantidad));
                    lblBadge.getStyleClass().add("sidebar-item-badge");

                    fila.getChildren().addAll(lblIcono, lblTexto, lblBadge);
                    setGraphic(fila);
                }
            }
        });

        if (seleccionActual != null && items.contains(seleccionActual)) {
            listaEstanterias.getSelectionModel().select(seleccionActual);
        } else {
            listaEstanterias.getSelectionModel().select(0);
        }
    }

    /**
     * Cuenta dinámicamente los libros correspondientes a una categoría o estantería.
     */
    private long contarLibrosEnEstanteria(String item) {
        if (listaLibrosCompleta == null) {
            return 0;
        }
        return switch (item) {
            case VISTA_TODOS -> listaLibrosCompleta.stream().filter(Libro::isPoseido).count();
            case VISTA_DESEOS -> listaLibrosCompleta.stream().filter(l -> !l.isPoseido()).count();
            case VISTA_DIGITAL -> listaLibrosCompleta.stream().filter(l -> l.isPoseido() && l.isEsDigital()).count();
            default -> listaLibrosCompleta.stream()
                    .filter(l -> l.isPoseido() && l.getEstanterias() != null && l.getEstanterias().contains(item))
                    .count();
        };
    }

    /**
     * Obtiene la estantería seleccionada actualmente.
     *
     * @return Nombre de la estantería o VISTA_TODOS por defecto.
     */
    public String getEstanteriaSeleccionada() {
        if (listaEstanterias != null && listaEstanterias.getSelectionModel().getSelectedItem() != null) {
            return listaEstanterias.getSelectionModel().getSelectedItem();
        }
        return VISTA_TODOS;
    }

    /**
     * Selecciona programáticamente una estantería en la lista.
     *
     * @param estanteria Nombre de la estantería a seleccionar.
     */
    public void seleccionarEstanteria(String estanteria) {
        if (listaEstanterias != null && estanteria != null) {
            listaEstanterias.getSelectionModel().select(estanteria);
        }
    }

    /**
     * Abre un diálogo modal para configurar el reto anual de lectura.
     */
    @FXML
    private void configurarRetoAnual() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reto de Lectura");
        dialog.setHeaderText("Configurar Reto Anual");
        dialog.setContentText("¿Cuántos libros te propones leer este año? (Pon 0 para desactivar)");

        String retoActualStr = preferencias != null ? preferencias.getOrDefault("reto_anual", "0") : "0";
        dialog.getEditor().setText(retoActualStr);

        dialog.showAndWait().ifPresent(resultado -> {
            try {
                int reto = Integer.parseInt(resultado.trim());
                if (preferencias != null) {
                    preferencias.put("reto_anual", String.valueOf(reto));
                    if (jsonManager != null) {
                        jsonManager.guardarPreferencias(preferencias);
                    }
                }
                actualizarRetoAnual();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Por favor introduce un número válido.");
                alert.showAndWait();
            }
        });
    }

    /**
     * Actualiza la visibilidad y el porcentaje de avance del widget de reto anual.
     */
    public void actualizarRetoAnual() {
        if (widgetRetoAnual == null) {
            return;
        }

        int reto = 0;
        try {
            if (preferencias != null) {
                reto = Integer.parseInt(preferencias.getOrDefault("reto_anual", "0"));
            }
        } catch (NumberFormatException ignored) {
        }

        if (reto <= 0) {
            widgetRetoAnual.setVisible(false);
            widgetRetoAnual.setManaged(false);
            if (widgetRetoVacio != null) {
                widgetRetoVacio.setVisible(true);
                widgetRetoVacio.setManaged(true);
            }
            return;
        }

        if (widgetRetoVacio != null) {
            widgetRetoVacio.setVisible(false);
            widgetRetoVacio.setManaged(false);
        }
        widgetRetoAnual.setVisible(true);
        widgetRetoAnual.setManaged(true);

        int currentYear = LocalDate.now().getYear();
        lblTituloReto.setText("Reto de Lectura " + currentYear);

        long librosLeidos = 0;
        if (listaLibrosCompleta != null) {
            librosLeidos = listaLibrosCompleta.stream()
                    .filter(l -> l.isPoseido() && l.getEstadoLecturaEnum() == EstadoLectura.LEIDO)
                    .count();
        }

        double progress = (double) librosLeidos / reto;
        if (progress > 1.0) {
            progress = 1.0;
        }

        if (progresoReto != null) {
            progresoReto.setProgress(progress);
        }
        if (lblPorcentajeReto != null) {
            lblPorcentajeReto.setText((int) Math.round(progress * 100) + "%");
        }
        if (lblEstadoReto != null) {
            lblEstadoReto.setText(librosLeidos + " de " + reto + " libros leídos");
        }
    }
}
