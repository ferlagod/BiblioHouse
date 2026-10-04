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
import com.bibliohouse.logic.EbookMetadataService;
import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.utils.ImageLoader;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Controlador para la pestaña "Mis Libros". Gestiona la cuadrícula visual de
 * portadas (tarjetas interactivas), vista de lista compacta, filtrado rápido
 * por chips y estanterías, ordenación, drag & drop de e-books y acciones de
 * menú contextual sobre cada libro.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class CatalogoGridController {

    @FXML
    private MenuButton btnOrdenar;
    @FXML
    private TextField txtBuscarMisLibros;
    @FXML
    private FlowPane panelMisLibros;
    @FXML
    private ScrollPane scrollMisLibros;
    @FXML
    private HBox boxPaginacion;
    @FXML
    private Button btnPrimeraPagina;
    @FXML
    private Button btnAnterior;
    @FXML
    private Label lblInfoPaginacion;
    @FXML
    private Button btnSiguiente;
    @FXML
    private Button btnUltima;
    @FXML
    private ComboBox<String> comboLibrosPorPagina;

    // Selector de vista Cuadrícula / Lista
    @FXML
    private Button btnVistaCuadricula;
    @FXML
    private Button btnVistaLista;
    @FXML
    private TableView<Libro> tablaMisLibros;
    @FXML
    private TableColumn<Libro, String> colPortada;
    @FXML
    private TableColumn<Libro, String> colTitulo;
    @FXML
    private TableColumn<Libro, String> colAutor;
    @FXML
    private TableColumn<Libro, String> colGenero;
    @FXML
    private TableColumn<Libro, EstadoLectura> colEstado;
    @FXML
    private TableColumn<Libro, Libro> colProgreso;
    @FXML
    private TableColumn<Libro, Integer> colCalificacion;
    private boolean esVistaLista = false;

    // Chips de filtrado rápido
    @FXML
    private Button chipTodos;
    @FXML
    private Button chipLeyendo;
    @FXML
    private Button chipLeidos;
    @FXML
    private Button chipPendientes;
    @FXML
    private Button chipDigitales;

    public enum FiltroChip {
        TODOS, LEYENDO, LEIDOS, PENDIENTES, DIGITALES
    }
    private FiltroChip filtroChipActual = FiltroChip.TODOS;

    // Estado de paginación para virtualización fluida
    private int paginaActual = 1;
    private int librosPorPagina = 48;
    private int totalPaginas = 1;
    private int totalLibrosFiltrados = 0;

    private PrimaryController mainController;
    private JsonManager jsonManager;
    private ObservableList<Libro> listaLibrosCompleta;
    private List<Libro> listaDeseos;
    private String rutaUsuario;
    private ResourceBundle resources;

    private String categoriaActual = SidebarController.VISTA_TODOS;
    private ContextMenu contextMenuLibros;

    private Comparator<Libro> currentComparator = Comparator.comparing(
            l -> l.getTitulo() != null ? l.getTitulo() : "",
            String::compareToIgnoreCase);

    @FXML
    public void initialize() {
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.textProperty().addListener((obs, oldVal, newVal) -> {
                paginaActual = 1;
                refrescarCuadricula();
            });
        }

        if (comboLibrosPorPagina != null) {
            comboLibrosPorPagina.getItems().setAll("24", "48", "96", "Todos");
            comboLibrosPorPagina.setValue(String.valueOf(librosPorPagina));
            comboLibrosPorPagina.valueProperty().addListener((obs, oldVal, newVal) -> {
                if ("Todos".equalsIgnoreCase(newVal)) {
                    librosPorPagina = Integer.MAX_VALUE;
                } else if (newVal != null) {
                    try {
                        librosPorPagina = Integer.parseInt(newVal.trim());
                    } catch (NumberFormatException ignored) {
                        librosPorPagina = 48;
                    }
                }
                paginaActual = 1;
                refrescarCuadricula();
            });
        }

        configurarTablaMisLibros();
        configurarAccesibilidadControles();

        // Suscripción al bus de eventos para reaccionar a cambios
        AppEventBus.getInstance().subscribe(AppEventBus.FiltroEstanteriaEvent.class, e -> {
            this.categoriaActual = e.getEstanteria();
            this.paginaActual = 1;
            refrescarCuadricula();
        });

        AppEventBus.getInstance().subscribe(AppEventBus.LibroModificadoEvent.class, e -> refrescarCuadricula());
        AppEventBus.getInstance().subscribe(AppEventBus.LibroEliminadoEvent.class, e -> refrescarCuadricula());
        AppEventBus.getInstance().subscribe(AppEventBus.CatalogoSincronizadoEvent.class, e -> refrescarCuadricula());
    }

    /**
     * Configura la accesibilidad semántica (AccessibleRole y AccessibleText)
     * para lectores de pantalla en los controles interactivos del catálogo.
     */
    private void configurarAccesibilidadControles() {
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.setAccessibleRole(AccessibleRole.TEXT_FIELD);
            txtBuscarMisLibros.setAccessibleText("Buscar libros por título o autor en el catálogo");
        }
        if (btnOrdenar != null) {
            btnOrdenar.setAccessibleRole(AccessibleRole.BUTTON);
            btnOrdenar.setAccessibleText("Opciones de ordenación del catálogo");
        }
        if (btnVistaCuadricula != null) {
            btnVistaCuadricula.setAccessibleRole(AccessibleRole.BUTTON);
            btnVistaCuadricula.setAccessibleText("Cambiar a vista de cuadrícula con portadas de libros");
        }
        if (btnVistaLista != null) {
            btnVistaLista.setAccessibleRole(AccessibleRole.BUTTON);
            btnVistaLista.setAccessibleText("Cambiar a vista de lista tabular detallada");
        }
        if (chipTodos != null) {
            chipTodos.setAccessibleRole(AccessibleRole.BUTTON);
            chipTodos.setAccessibleText("Mostrar todos los libros de la colección");
        }
        if (chipLeyendo != null) {
            chipLeyendo.setAccessibleRole(AccessibleRole.BUTTON);
            chipLeyendo.setAccessibleText("Filtrar libros actualmente en lectura");
        }
        if (chipLeidos != null) {
            chipLeidos.setAccessibleRole(AccessibleRole.BUTTON);
            chipLeidos.setAccessibleText("Filtrar libros ya leídos");
        }
        if (chipPendientes != null) {
            chipPendientes.setAccessibleRole(AccessibleRole.BUTTON);
            chipPendientes.setAccessibleText("Filtrar libros pendientes de lectura");
        }
        if (chipDigitales != null) {
            chipDigitales.setAccessibleRole(AccessibleRole.BUTTON);
            chipDigitales.setAccessibleText("Filtrar únicamente libros electrónicos e-books");
        }
        if (btnPrimeraPagina != null) {
            btnPrimeraPagina.setAccessibleRole(AccessibleRole.BUTTON);
            btnPrimeraPagina.setAccessibleText("Ir a la primera página de libros");
        }
        if (btnAnterior != null) {
            btnAnterior.setAccessibleRole(AccessibleRole.BUTTON);
            btnAnterior.setAccessibleText("Ir a la página anterior de libros");
        }
        if (btnSiguiente != null) {
            btnSiguiente.setAccessibleRole(AccessibleRole.BUTTON);
            btnSiguiente.setAccessibleText("Ir a la página siguiente de libros");
        }
        if (btnUltima != null) {
            btnUltima.setAccessibleRole(AccessibleRole.BUTTON);
            btnUltima.setAccessibleText("Ir a la última página de libros");
        }
        if (comboLibrosPorPagina != null) {
            comboLibrosPorPagina.setAccessibleRole(AccessibleRole.COMBO_BOX);
            comboLibrosPorPagina.setAccessibleText("Seleccionar cantidad de libros por página");
        }
        if (lblInfoPaginacion != null) {
            lblInfoPaginacion.setAccessibleRole(AccessibleRole.TEXT);
        }
        if (panelMisLibros != null) {
            panelMisLibros.setFocusTraversable(true);
            panelMisLibros.setAccessibleRole(AccessibleRole.PARENT);
            panelMisLibros.setAccessibleText("Cuadrícula interactiva de libros");
            panelMisLibros.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal && !panelMisLibros.getChildren().isEmpty()) {
                    panelMisLibros.getChildren().get(0).requestFocus();
                }
            });
        }
    }

    /**
     * Inicializa las dependencias, listas observables, rutas y recursos
     * localizados requeridos por la vista de cuadrícula del catálogo de libros.
     *
     * @param mainController Controlador principal de la ventana
     * (PrimaryController).
     * @param jsonManager Gestor de persistencia en disco de datos JSON.
     * @param listaLibros Lista observable que contiene la totalidad de libros
     * del usuario.
     * @param listaDeseos Lista de libros deseados pero no poseídos.
     * @param rutaUsuario Directorio raíz del perfil del usuario en disco.
     * @param resources Paquete de recursos para textos internacionalizados.
     */
    public void initData(PrimaryController mainController, JsonManager jsonManager,
            ObservableList<Libro> listaLibros, List<Libro> listaDeseos,
            String rutaUsuario, ResourceBundle resources) {
        this.mainController = mainController;
        this.jsonManager = jsonManager;
        this.listaLibrosCompleta = listaLibros;
        this.listaDeseos = listaDeseos;
        this.rutaUsuario = rutaUsuario;
        this.resources = resources;

        configurarContextMenu();
        configurarDragAndDrop();
        refrescarCuadricula();
    }

    // =========================================================================
    // ORDENACIÓN
    // =========================================================================
    /**
     * Ordena los libros alfabéticamente por título de forma ascendente (A - Z)
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorTituloAZ() {
        currentComparator = Comparator.comparing(l -> l.getTitulo() != null ? l.getTitulo() : "", String::compareToIgnoreCase);
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros alfabéticamente por título de forma descendente (Z - A)
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorTituloZA() {
        currentComparator = Comparator.comparing((Libro l) -> l.getTitulo() != null ? l.getTitulo() : "", String::compareToIgnoreCase).reversed();
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros alfabéticamente por autor de la A a la Z y reinicia la
     * paginación a la primera página.
     */
    @FXML
    public void ordenarPorAutor() {
        currentComparator = Comparator.comparing((Libro l) -> l.getAutor() != null ? l.getAutor() : "", String::compareToIgnoreCase);
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros por año de publicación de más reciente a más antiguo y
     * reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorAnio() {
        currentComparator = (l1, l2) -> {
            String a1 = l1.getAño() != null ? l1.getAño() : "";
            String a2 = l2.getAño() != null ? l2.getAño() : "";
            return a2.compareTo(a1); // Descendente
        };
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros por orden de inserción o modificación más reciente y
     * reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorReciente() {
        currentComparator = (l1, l2) -> {
            if (listaLibrosCompleta == null) {
                return 0;
            }
            int i1 = listaLibrosCompleta.indexOf(l1);
            int i2 = listaLibrosCompleta.indexOf(l2);
            return Integer.compare(i2, i1); // Descendente
        };
        paginaActual = 1;
        refrescarCuadricula();
    }

    // =========================================================================
    // NAVEGACIÓN Y PAGINACIÓN
    // =========================================================================
    /**
     * Salta a la primera página de la cuadrícula de libros y desplaza la vista
     * hacia arriba.
     */
    @FXML
    public void irPrimeraPagina() {
        if (paginaActual > 1) {
            paginaActual = 1;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Retrocede a la página anterior de la cuadrícula de libros si no se está
     * en la primera.
     */
    @FXML
    public void irPaginaAnterior() {
        if (paginaActual > 1) {
            paginaActual--;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Avanza a la página siguiente de la cuadrícula de libros si hay páginas
     * posteriores disponibles.
     */
    @FXML
    public void irPaginaSiguiente() {
        if (paginaActual < totalPaginas) {
            paginaActual++;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Salta directamente a la última página de la cuadrícula de libros.
     */
    @FXML
    public void irUltimaPagina() {
        if (paginaActual < totalPaginas) {
            paginaActual = totalPaginas;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Desplaza suavemente el ScrollPane de libros a la posición superior
     * inicial.
     */
    private void scrollearArriba() {
        if (scrollMisLibros != null) {
            scrollMisLibros.setVvalue(0.0);
        }
    }

    /**
     * Actualiza las etiquetas de información de paginación y el estado de
     * habilitación de los botones de navegación
     * anterior/siguiente/primera/última página.
     *
     * @param inicio Índice del primer libro mostrado en la página actual.
     * @param fin Índice del último libro mostrado en la página actual.
     * @param total Número total de libros tras aplicar los filtros de búsqueda
     * y estantería.
     */
    private void actualizarBarraPaginacion(int inicio, int fin, int total) {
        if (boxPaginacion == null) {
            return;
        }

        if (total == 0) {
            if (lblInfoPaginacion != null) {
                lblInfoPaginacion.setText("0 libros");
            }
            if (btnPrimeraPagina != null) {
                btnPrimeraPagina.setDisable(true);
            }
            if (btnAnterior != null) {
                btnAnterior.setDisable(true);
            }
            if (btnSiguiente != null) {
                btnSiguiente.setDisable(true);
            }
            if (btnUltima != null) {
                btnUltima.setDisable(true);
            }
            return;
        }

        if (lblInfoPaginacion != null) {
            lblInfoPaginacion.setText(String.format("Mostrando %d-%d de %d (Pág. %d/%d)",
                    inicio, fin, total, paginaActual, totalPaginas));
        }

        boolean puedeRetroceder = paginaActual > 1;
        boolean puedeAvanzar = paginaActual < totalPaginas;

        if (btnPrimeraPagina != null) {
            btnPrimeraPagina.setDisable(!puedeRetroceder);
        }
        if (btnAnterior != null) {
            btnAnterior.setDisable(!puedeRetroceder);
        }
        if (btnSiguiente != null) {
            btnSiguiente.setDisable(!puedeAvanzar);
        }
        if (btnUltima != null) {
            btnUltima.setDisable(!puedeAvanzar);
        }
    }

    // =========================================================================
    // SELECTOR DE VISTA Y CHIPS DE FILTRADO
    // =========================================================================
    /**
     * Configura las columnas, celdas y eventos de interacción de la vista de
     * tabla compacta.
     */
    private void configurarTablaMisLibros() {
        if (tablaMisLibros == null) {
            return;
        }

        colPortada.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPortadaURL()));
        colPortada.setCellFactory(col -> new TableCell<>() {
            private final ImageView imgView = new ImageView();

            {
                imgView.setFitWidth(28);
                imgView.setFitHeight(40);
                Rectangle clip = new Rectangle(28, 40);
                clip.setArcWidth(6);
                clip.setArcHeight(6);
                imgView.setClip(clip);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ImageLoader.load(url, imgView, 28, 40);
                    setGraphic(imgView);
                }
            }
        });

        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colTitulo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        colAutor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAutor() != null ? data.getValue().getAutor() : ""));

        colGenero.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenero() != null ? data.getValue().getGenero() : ""));

        colEstado.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEstadoLecturaEnum()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoLectura estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(estado.getEtiqueta());
                    badge.getStyleClass().add("table-status-badge");
                    switch (estado) {
                        case LEIDO ->
                            badge.getStyleClass().add("badge-leido");
                        case LEYENDO ->
                            badge.getStyleClass().add("badge-leyendo");
                        case ABANDONADO ->
                            badge.getStyleClass().add("badge-abandonado");
                        default ->
                            badge.setStyle("-fx-background-color: -color-bg-subtle; -fx-text-fill: -color-fg-muted; -fx-padding: 2 7; -fx-background-radius: 10;");
                    }
                    setAlignment(Pos.CENTER);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colProgreso.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colProgreso.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Libro libro, boolean empty) {
                super.updateItem(libro, empty);
                if (empty || libro == null) {
                    setText(null);
                    setGraphic(null);
                } else if (libro.getEstadoLecturaEnum() == EstadoLectura.LEYENDO && libro.getPaginasTotales() > 0) {
                    double progress = (double) libro.getPaginaActual() / libro.getPaginasTotales();
                    progress = Math.max(0.0, Math.min(1.0, progress));
                    ProgressBar pb = new ProgressBar(progress);
                    pb.setPrefWidth(65);
                    pb.setPrefHeight(6);
                    pb.setStyle("-fx-accent: #f59e0b;");
                    int pct = (int) Math.round(progress * 100);
                    Label lbl = new Label(libro.getPaginaActual() + "/" + libro.getPaginasTotales() + " (" + pct + "%)");
                    lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-muted;");
                    HBox box = new HBox(6, pb, lbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                } else if (libro.getPaginasTotales() > 0) {
                    setText(libro.getPaginasTotales() + " págs.");
                    setGraphic(null);
                } else {
                    setText("—");
                    setGraphic(null);
                }
            }
        });

        colCalificacion.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCalificacion()));
        colCalificacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer calificacion, boolean empty) {
                super.updateItem(calificacion, empty);
                if (empty || calificacion == null || calificacion <= 0) {
                    setText("—");
                    setStyle("-fx-text-fill: -color-fg-muted;");
                } else {
                    int stars = Math.max(1, Math.min(5, calificacion));
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < stars; i++) {
                        sb.append("★");
                    }
                    for (int i = stars; i < 5; i++) {
                        sb.append("☆");
                    }
                    setText(sb.toString());
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                }
            }
        });

        tablaMisLibros.setRowFactory(tv -> {
            TableRow<Libro> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    Libro libro = row.getItem();
                    if (mainController != null) {
                        mainController.seleccionarLibro(libro);
                    }
                    if (event.getClickCount() == 2) {
                        abrirDetalle(libro);
                    }
                } else if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    Libro libro = row.getItem();
                    if (mainController != null) {
                        mainController.seleccionarLibro(libro);
                    }
                    mostrarContextMenu(row, libro, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return row;
        });
    }

    @FXML
    public void cambiarAVistaCuadricula() {
        esVistaLista = false;
        if (btnVistaCuadricula != null) {
            btnVistaCuadricula.getStyleClass().remove("active-view-btn");
            btnVistaCuadricula.getStyleClass().add("active-view-btn");
        }
        if (btnVistaLista != null) {
            btnVistaLista.getStyleClass().remove("active-view-btn");
        }
        if (scrollMisLibros != null) {
            scrollMisLibros.setVisible(true);
            scrollMisLibros.setManaged(true);
        }
        if (tablaMisLibros != null) {
            tablaMisLibros.setVisible(false);
            tablaMisLibros.setManaged(false);
        }
        refrescarCuadricula();
    }

    @FXML
    public void cambiarAVistaLista() {
        esVistaLista = true;
        if (btnVistaLista != null) {
            btnVistaLista.getStyleClass().remove("active-view-btn");
            btnVistaLista.getStyleClass().add("active-view-btn");
        }
        if (btnVistaCuadricula != null) {
            btnVistaCuadricula.getStyleClass().remove("active-view-btn");
        }
        if (scrollMisLibros != null) {
            scrollMisLibros.setVisible(false);
            scrollMisLibros.setManaged(false);
        }
        if (tablaMisLibros != null) {
            tablaMisLibros.setVisible(true);
            tablaMisLibros.setManaged(true);
        }
        refrescarCuadricula();
    }

    @FXML
    public void filtrarChipTodos() {
        activarChip(chipTodos, FiltroChip.TODOS);
    }

    @FXML
    public void filtrarChipLeyendo() {
        activarChip(chipLeyendo, FiltroChip.LEYENDO);
    }

    @FXML
    public void filtrarChipLeidos() {
        activarChip(chipLeidos, FiltroChip.LEIDOS);
    }

    @FXML
    public void filtrarChipPendientes() {
        activarChip(chipPendientes, FiltroChip.PENDIENTES);
    }

    @FXML
    public void filtrarChipDigitales() {
        activarChip(chipDigitales, FiltroChip.DIGITALES);
    }

    private void activarChip(Button btnActivo, FiltroChip nuevoFiltro) {
        filtroChipActual = nuevoFiltro;
        Button[] chips = {chipTodos, chipLeyendo, chipLeidos, chipPendientes, chipDigitales};
        for (Button chip : chips) {
            if (chip != null) {
                chip.getStyleClass().remove("chip-active");
            }
        }
        if (btnActivo != null) {
            btnActivo.getStyleClass().add("chip-active");
        }
        paginaActual = 1;
        refrescarCuadricula();
    }

    // =========================================================================
    // RENDERIZADO DE LA CUADRÍCULA
    // =========================================================================
    /**
     * Dibuja las tarjetas de libros respetando el filtro lateral de estantería,
     * el texto de búsqueda rápida y la paginación activa.
     */
    public void refrescarCuadricula() {
        if (panelMisLibros == null || listaLibrosCompleta == null) {
            return;
        }

        String busquedaRapida = txtBuscarMisLibros != null ? txtBuscarMisLibros.getText().toLowerCase().trim() : "";

        // Filtrar según estantería seleccionada y chip rápido
        List<Libro> filtrados = listaLibrosCompleta.stream()
                .filter(l -> {
                    // Filtro de categoría
                    if (SidebarController.VISTA_DESEOS.equals(categoriaActual)) {
                        return !l.isPoseido();
                    }
                    if (SidebarController.VISTA_DIGITAL.equals(categoriaActual)) {
                        return l.isEsDigital() && l.isPoseido();
                    }
                    if (SidebarController.VISTA_TODOS.equals(categoriaActual) || categoriaActual == null) {
                        return l.isPoseido();
                    }
                    return l.isPoseido() && l.getEstanterias() != null && l.getEstanterias().contains(categoriaActual);
                })
                .filter(l -> {
                    // Filtro rápido de chips
                    return switch (filtroChipActual) {
                        case LEYENDO ->
                            l.getEstadoLecturaEnum() == EstadoLectura.LEYENDO;
                        case LEIDOS ->
                            l.getEstadoLecturaEnum() == EstadoLectura.LEIDO;
                        case PENDIENTES ->
                            l.getEstadoLecturaEnum() == EstadoLectura.PENDIENTE;
                        case DIGITALES ->
                            l.isEsDigital();
                        default ->
                            true;
                    };
                })
                .filter(l -> {
                    // Filtro de texto
                    if (busquedaRapida.isEmpty()) {
                        return true;
                    }
                    boolean tituloCoincide = l.getTitulo() != null && l.getTitulo().toLowerCase().contains(busquedaRapida);
                    boolean autorCoincide = l.getAutor() != null && l.getAutor().toLowerCase().contains(busquedaRapida);
                    return tituloCoincide || autorCoincide;
                })
                .sorted(currentComparator)
                .collect(Collectors.toList());

        totalLibrosFiltrados = filtrados.size();

        // Estado vacío
        if (filtrados.isEmpty()) {
            totalPaginas = 1;
            paginaActual = 1;
            actualizarBarraPaginacion(0, 0, 0);

            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 60 0 60 0;");

            Label lblIcon = new Label("📚");
            lblIcon.setStyle("-fx-font-size: 48px;");

            Label lblVacio = new Label("Tu biblioteca está vacía aquí");
            lblVacio.setStyle("-fx-text-fill: -color-fg-default; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label lblSub = new Label("Prueba a cambiar el filtro de estantería o chip\no añade libros nuevos desde «Gestionar Libros»");
            lblSub.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px; -fx-text-alignment: center;");
            lblSub.setWrapText(true);
            lblSub.setMaxWidth(400);
            lblSub.setAlignment(Pos.CENTER);

            emptyState.getChildren().addAll(lblIcon, lblVacio, lblSub);
            panelMisLibros.getChildren().setAll(emptyState);

            if (tablaMisLibros != null) {
                tablaMisLibros.setItems(FXCollections.emptyObservableList());
                tablaMisLibros.setPlaceholder(emptyState);
            }
            return;
        }

        // Calcular paginación para virtualización
        int pageSize = librosPorPagina > 0 ? librosPorPagina : Integer.MAX_VALUE;
        totalPaginas = (int) Math.ceil((double) filtrados.size() / pageSize);
        if (totalPaginas < 1) {
            totalPaginas = 1;
        }
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }
        if (paginaActual < 1) {
            paginaActual = 1;
        }

        int desde = (paginaActual - 1) * pageSize;
        if (desde >= filtrados.size()) {
            desde = 0;
            paginaActual = 1;
        }
        int hasta = Math.min(desde + pageSize, filtrados.size());
        List<Libro> paginaLibros = filtrados.subList(desde, hasta);

        actualizarBarraPaginacion(desde + 1, hasta, filtrados.size());

        // Construir tarjetas exclusivamente para la sublista de la página actual
        List<Node> tarjetas = paginaLibros.stream()
                .map(this::crearTarjetaMisLibros)
                .collect(Collectors.toList());
        panelMisLibros.getChildren().setAll(tarjetas);

        // Actualizar la tabla para la vista de lista compacta
        if (tablaMisLibros != null) {
            tablaMisLibros.setItems(FXCollections.observableArrayList(paginaLibros));
        }
    }

    /**
     * Obtiene el número de la página actual en la cuadrícula (1-indexed).
     *
     * @return Página actual.
     */
    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Establece el número de página actual en la cuadrícula.
     *
     * @param paginaActual Nueva página a mostrar.
     */
    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    /**
     * Obtiene la cantidad máxima de libros que se renderizan por página.
     *
     * @return Límite de libros por página.
     */
    public int getLibrosPorPagina() {
        return librosPorPagina;
    }

    /**
     * Establece la cantidad de libros que se renderizarán por página.
     *
     * @param librosPorPagina Cantidad de libros por página.
     */
    public void setLibrosPorPagina(int librosPorPagina) {
        this.librosPorPagina = librosPorPagina;
    }

    /**
     * Obtiene el total de páginas calculadas para la vista actual.
     *
     * @return Número total de páginas.
     */
    public int getTotalPaginas() {
        return totalPaginas;
    }

    /**
     * Obtiene el total de libros que coinciden con los filtros actuales.
     *
     * @return Cantidad de libros filtrados.
     */
    public int getTotalLibrosFiltrados() {
        return totalLibrosFiltrados;
    }

    /**
     * Establece la lista completa observable de libros de la biblioteca.
     *
     * @param listaLibrosCompleta Lista de libros.
     */
    public void setListaLibrosCompleta(ObservableList<Libro> listaLibrosCompleta) {
        this.listaLibrosCompleta = listaLibrosCompleta;
    }

    /**
     * Establece la categoría o estantería activa para el filtrado de libros.
     *
     * @param categoriaActual Nombre de la estantería o categoría seleccionada.
     */
    public void setCategoriaActual(String categoriaActual) {
        this.categoriaActual = categoriaActual;
    }

    /**
     * Obtiene el FlowPane contenedor de la cuadrícula de libros.
     *
     * @return Contenedor FlowPane.
     */
    public FlowPane getPanelMisLibros() {
        return panelMisLibros;
    }

    /**
     * Establece el FlowPane contenedor de la cuadrícula de libros.
     *
     * @param panelMisLibros Contenedor FlowPane.
     */
    public void setPanelMisLibros(FlowPane panelMisLibros) {
        this.panelMisLibros = panelMisLibros;
    }

    /**
     * Construye la tarjeta gráfica de un libro individual con portada, títulos,
     * insignias de lectura y soporte para eventos de clic y menú contextual.
     *
     * @param libro Libro para el que se genera la tarjeta.
     * @return Nodo {@link VBox} con la tarjeta interactiva renderizada.
     */
    private VBox crearTarjetaMisLibros(Libro libro) {
        VBox tarjeta = new VBox(6);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(146);
        tarjeta.getStyleClass().add("book-card");
        tarjeta.setFocusTraversable(true);
        tarjeta.setAccessibleRole(AccessibleRole.BUTTON);

        // Semántica descriptiva completa para lectores de pantalla
        StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Libro: ").append(libro.getTitulo());
        if (libro.getAutor() != null && !libro.getAutor().isBlank()) {
            sbDesc.append(", por ").append(libro.getAutor());
        }
        if (libro.getEstadoLecturaEnum() != null) {
            sbDesc.append(", ").append(libro.getEstadoLecturaEnum().getEtiqueta());
        }
        if (libro.getCalificacion() > 0) {
            sbDesc.append(", ").append(libro.getCalificacion()).append(" estrellas");
        }
        if (libro.isEsDigital()) {
            sbDesc.append(", Formato digital");
        }
        if (libro.getEstadoLecturaEnum() == EstadoLectura.LEYENDO && libro.getPaginasTotales() > 0) {
            int pct = (int) Math.round(((double) libro.getPaginaActual() / libro.getPaginasTotales()) * 100);
            sbDesc.append(", Progreso: ").append(libro.getPaginaActual()).append(" de ").append(libro.getPaginasTotales()).append(" páginas (").append(pct).append("%)");
        }
        tarjeta.setAccessibleText(sbDesc.toString());

        // Eventos de teclado (A11y): Enter/Espacio para abrir detalle, Flechas para navegar por la cuadrícula
        tarjeta.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                abrirDetalle(libro);
                event.consume();
            } else if (code == KeyCode.CONTEXT_MENU) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                javafx.geometry.Bounds bounds = tarjeta.localToScreen(tarjeta.getBoundsInLocal());
                if (bounds != null) {
                    mostrarContextMenu(tarjeta, libro, bounds.getMinX() + 10, bounds.getMaxY() - 10);
                }
                event.consume();
            } else if (code == KeyCode.RIGHT) {
                navegarTarjetaSiguiente(tarjeta);
                event.consume();
            } else if (code == KeyCode.LEFT) {
                navegarTarjetaAnterior(tarjeta);
                event.consume();
            } else if (code == KeyCode.DOWN) {
                navegarTarjetaFila(tarjeta, true);
                event.consume();
            } else if (code == KeyCode.UP) {
                navegarTarjetaFila(tarjeta, false);
                event.consume();
            } else if (code == KeyCode.HOME) {
                if (panelMisLibros != null && !panelMisLibros.getChildren().isEmpty()) {
                    panelMisLibros.getChildren().get(0).requestFocus();
                    event.consume();
                }
            } else if (code == KeyCode.END) {
                if (panelMisLibros != null && !panelMisLibros.getChildren().isEmpty()) {
                    panelMisLibros.getChildren().get(panelMisLibros.getChildren().size() - 1).requestFocus();
                    event.consume();
                }
            }
        });

        // Sincronizar selección activa cuando se navega con teclado
        tarjeta.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && mainController != null) {
                mainController.seleccionarLibro(libro);
            }
        });

        // Eventos de ratón
        tarjeta.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                if (e.getClickCount() == 2) {
                    abrirDetalle(libro);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                mostrarContextMenu(tarjeta, libro, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        tarjeta.setOnContextMenuRequested(e -> {
            if (mainController != null) {
                mainController.seleccionarLibro(libro);
            }
            mostrarContextMenu(tarjeta, libro, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Portada con esquinas suavemente redondeadas
        ImageView img = new ImageView();
        img.setMouseTransparent(true);
        img.setFitWidth(120);
        img.setFitHeight(175);
        img.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        img.setAccessibleText("Portada de " + libro.getTitulo());
        Rectangle clip = new Rectangle(120, 175);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        img.setClip(clip);
        ImageLoader.load(libro.getPortadaURL(), img, 120, 175);

        StackPane contenedorPortada = new StackPane(img);
        contenedorPortada.getStyleClass().add("book-card-cover-container");
        contenedorPortada.setMouseTransparent(true);
        contenedorPortada.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        contenedorPortada.setAccessibleText("Portada de " + libro.getTitulo());

        // Micro-animación suave al pasar el ratón (hover lift sobre la portada, sin alterar el layout del FlowPane)
        tarjeta.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), contenedorPortada);
            tt.setToY(-4);
            tt.play();
        });
        tarjeta.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), contenedorPortada);
            tt.setToY(0);
            tt.play();
        });

        // Badge de estado de lectura en portada (diseño en cápsula con texto completo y borde de alto contraste)
        EstadoLectura estado = libro.getEstadoLecturaEnum();
        Label badge = null;
        if (estado == EstadoLectura.LEIDO) {
            badge = new Label("✓ Leído");
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-leido");
            badge.setStyle("-fx-background-color: #15803d; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 7; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #ffffff; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.25, 0, 2);");
        } else if (estado == EstadoLectura.LEYENDO) {
            badge = new Label("● Leyendo");
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-leyendo");
            badge.setStyle("-fx-background-color: #c2410c; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 7; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #ffffff; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.25, 0, 2);");
        } else if (estado == EstadoLectura.ABANDONADO) {
            badge = new Label("✕ Abandonado");
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-abandonado");
            badge.setStyle("-fx-background-color: #475569; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 7; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #ffffff; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.25, 0, 2);");
        }

        if (badge != null) {
            badge.setAccessibleRole(AccessibleRole.TEXT);
            badge.setAccessibleText("Estado de lectura: " + estado.getEtiqueta());
            badge.setMouseTransparent(true);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(6, 6, 0, 0));
            contenedorPortada.getChildren().add(badge);
        }

        // Badge de formato digital
        if (libro.isEsDigital()) {
            Label badgeDigital = new Label("📱 E-book");
            badgeDigital.setAccessibleRole(AccessibleRole.TEXT);
            badgeDigital.setAccessibleText("Formato digital: E-book");
            badgeDigital.getStyleClass().addAll("book-card-badge-pill", "badge-digital");
            badgeDigital.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #ffffff; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.25, 0, 2);");
            badgeDigital.setMouseTransparent(true);
            StackPane.setAlignment(badgeDigital, Pos.BOTTOM_LEFT);
            StackPane.setMargin(badgeDigital, new Insets(0, 0, 6, 6));
            contenedorPortada.getChildren().add(badgeDigital);
        }

        // Título del libro
        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setAccessibleRole(AccessibleRole.TEXT);
        lblTitulo.setAccessibleText("Título: " + libro.getTitulo());
        lblTitulo.getStyleClass().add("book-card-title");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(136);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setTextAlignment(TextAlignment.CENTER);
        lblTitulo.setMouseTransparent(true);

        tarjeta.getChildren().addAll(contenedorPortada, lblTitulo);

        // Autor del libro
        if (libro.getAutor() != null && !libro.getAutor().isBlank()) {
            Label lblAutor = new Label(libro.getAutor());
            lblAutor.setAccessibleRole(AccessibleRole.TEXT);
            lblAutor.setAccessibleText("Autor: " + libro.getAutor());
            lblAutor.getStyleClass().add("book-card-author");
            lblAutor.setMaxWidth(136);
            lblAutor.setAlignment(Pos.CENTER);
            lblAutor.setTextAlignment(TextAlignment.CENTER);
            lblAutor.setTextOverrun(OverrunStyle.ELLIPSIS);
            lblAutor.setMouseTransparent(true);
            tarjeta.getChildren().add(lblAutor);
        }

        // Calificación en estrellas (⭐)
        int stars = libro.getCalificacion();
        if (stars > 0) {
            stars = Math.max(1, Math.min(5, stars));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stars; i++) {
                sb.append("★");
            }
            for (int i = 0; i < 5 - stars; i++) {
                sb.append("☆");
            }
            Label lblRating = new Label(sb.toString());
            lblRating.setAccessibleRole(AccessibleRole.TEXT);
            lblRating.setAccessibleText("Calificación: " + stars + " de 5 estrellas");
            lblRating.getStyleClass().add("book-card-rating");
            lblRating.setMouseTransparent(true);
            tarjeta.getChildren().add(lblRating);
        }

        // Reading Tracker con barra de progreso moderna
        if (estado == EstadoLectura.LEYENDO && libro.getPaginasTotales() > 0) {
            double progreso = (double) libro.getPaginaActual() / libro.getPaginasTotales();
            progreso = Math.max(0.0, Math.min(1.0, progreso));

            ProgressBar pBar = new ProgressBar(progreso);
            pBar.setPrefWidth(120);
            pBar.setPrefHeight(6);
            pBar.setStyle("-fx-accent: #d97706; -fx-background-radius: 4;");
            pBar.setMouseTransparent(true);

            int pct = (int) Math.round(progreso * 100);
            pBar.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
            pBar.setAccessibleText("Progreso de lectura: " + pct + " por ciento");

            Label lblProgreso = new Label(libro.getPaginaActual() + " / " + libro.getPaginasTotales() + " pág. (" + pct + "%)");
            lblProgreso.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-muted;");
            lblProgreso.setAccessibleRole(AccessibleRole.TEXT);
            lblProgreso.setAccessibleText("Progreso: " + libro.getPaginaActual() + " de " + libro.getPaginasTotales() + " páginas leídas (" + pct + "%)");
            lblProgreso.setMouseTransparent(true);

            tarjeta.getChildren().addAll(pBar, lblProgreso);
        }

        return tarjeta;
    }

    private void navegarTarjetaSiguiente(Node actual) {
        if (panelMisLibros == null) {
            return;
        }
        int idx = panelMisLibros.getChildren().indexOf(actual);
        if (idx >= 0 && idx < panelMisLibros.getChildren().size() - 1) {
            Node siguiente = panelMisLibros.getChildren().get(idx + 1);
            siguiente.requestFocus();
            asegurarVisibilidadEnScroll(siguiente);
        }
    }

    private void navegarTarjetaAnterior(Node actual) {
        if (panelMisLibros == null) {
            return;
        }
        int idx = panelMisLibros.getChildren().indexOf(actual);
        if (idx > 0) {
            Node anterior = panelMisLibros.getChildren().get(idx - 1);
            anterior.requestFocus();
            asegurarVisibilidadEnScroll(anterior);
        }
    }

    private void navegarTarjetaFila(Node actual, boolean abajo) {
        if (panelMisLibros == null || panelMisLibros.getChildren().isEmpty()) {
            return;
        }
        javafx.geometry.Bounds curBounds = actual.getBoundsInParent();
        if (curBounds == null) {
            return;
        }
        double curY = curBounds.getMinY();
        double curCenterX = (curBounds.getMinX() + curBounds.getMaxX()) / 2.0;

        Node mejorCandidato = null;
        double distMin = Double.MAX_VALUE;

        if (abajo) {
            double filaTargetY = Double.MAX_VALUE;
            for (Node child : panelMisLibros.getChildren()) {
                if (child.getBoundsInParent() != null) {
                    double cy = child.getBoundsInParent().getMinY();
                    if (cy > curY + 10 && cy < filaTargetY) {
                        filaTargetY = cy;
                    }
                }
            }
            if (filaTargetY != Double.MAX_VALUE) {
                for (Node child : panelMisLibros.getChildren()) {
                    if (child.getBoundsInParent() != null) {
                        double cy = child.getBoundsInParent().getMinY();
                        if (Math.abs(cy - filaTargetY) < 20) {
                            double cx = (child.getBoundsInParent().getMinX() + child.getBoundsInParent().getMaxX()) / 2.0;
                            double dist = Math.abs(cx - curCenterX);
                            if (dist < distMin) {
                                distMin = dist;
                                mejorCandidato = child;
                            }
                        }
                    }
                }
            }
        } else {
            double filaTargetY = -Double.MAX_VALUE;
            for (Node child : panelMisLibros.getChildren()) {
                if (child.getBoundsInParent() != null) {
                    double cy = child.getBoundsInParent().getMinY();
                    if (cy < curY - 10 && cy > filaTargetY) {
                        filaTargetY = cy;
                    }
                }
            }
            if (filaTargetY != -Double.MAX_VALUE) {
                for (Node child : panelMisLibros.getChildren()) {
                    if (child.getBoundsInParent() != null) {
                        double cy = child.getBoundsInParent().getMinY();
                        if (Math.abs(cy - filaTargetY) < 20) {
                            double cx = (child.getBoundsInParent().getMinX() + child.getBoundsInParent().getMaxX()) / 2.0;
                            double dist = Math.abs(cx - curCenterX);
                            if (dist < distMin) {
                                distMin = dist;
                                mejorCandidato = child;
                            }
                        }
                    }
                }
            }
        }

        if (mejorCandidato != null) {
            mejorCandidato.requestFocus();
            asegurarVisibilidadEnScroll(mejorCandidato);
        }
    }

    private void asegurarVisibilidadEnScroll(Node target) {
        if (scrollMisLibros == null || target == null || panelMisLibros == null) {
            return;
        }
        try {
            javafx.geometry.Bounds bounds = target.getBoundsInParent();
            if (bounds != null && panelMisLibros.getBoundsInLocal() != null && scrollMisLibros.getViewportBounds() != null) {
                double contentHeight = panelMisLibros.getBoundsInLocal().getHeight();
                double viewportHeight = scrollMisLibros.getViewportBounds().getHeight();
                if (viewportHeight > 0 && contentHeight > viewportHeight) {
                    double nodeCenterY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
                    double vValue = (nodeCenterY - (viewportHeight / 2.0)) / (contentHeight - viewportHeight);
                    scrollMisLibros.setVvalue(Math.max(0.0, Math.min(1.0, vValue)));
                }
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // MENÚ CONTEXTUAL DE LIBROS
    // =========================================================================
    /**
     * Inicializa la instancia compartida del menú contextual de libros.
     */
    private void configurarContextMenu() {
        this.contextMenuLibros = new ContextMenu();
    }

    /**
     * Construye dinámicamente y despliega el menú contextual sobre la tarjeta
     * del libro, adaptando las opciones disponibles según si el libro es
     * digital o físico (leer, prestar, editar, cambiar portada, cambiar estado
     * de lectura, imprimir etiqueta, eliminar).
     *
     * @param owner Nodo gráfico propietario sobre el que se despliega el menú.
     * @param libro Libro sobre el que se ejecutarán las acciones.
     * @param screenX Coordenada X absoluta de la pantalla donde se produjo el
     * evento.
     * @param screenY Coordenada Y absoluta de la pantalla donde se produjo el
     * evento.
     */
    private void mostrarContextMenu(Node owner, Libro libro, double screenX, double screenY) {
        if (contextMenuLibros == null) {
            contextMenuLibros = new ContextMenu();
        }
        contextMenuLibros.getItems().clear();

        // 1. Leer (si es ebook)
        boolean isEbook = libro.isEsDigital() && libro.getRutaArchivoDigital() != null && !libro.getRutaArchivoDigital().isEmpty();
        if (isEbook) {
            MenuItem itemLeer = new MenuItem(resources != null && resources.containsKey("ctx.read") ? resources.getString("ctx.read") : "Leer");
            itemLeer.setOnAction(e -> {
                if (mainController != null) {
                    mainController.abrirLectorDigital(libro);
                }
            });
            contextMenuLibros.getItems().addAll(itemLeer, new SeparatorMenuItem());
        }

        // 2. Prestar
        MenuItem itemPrestar = new MenuItem(resources != null && resources.containsKey("ctx.loan") ? resources.getString("ctx.loan") : "Prestar");
        itemPrestar.setOnAction(e -> {
            if (mainController != null) {
                mainController.prepararPrestamoLibro(libro);
            }
        });

        // 3. Editar
        MenuItem itemEditar = new MenuItem(resources != null && resources.containsKey("ctx.edit") ? resources.getString("ctx.edit") : "Editar");
        itemEditar.setOnAction(e -> {
            if (mainController != null) {
                mainController.editarLibro(libro);
            }
        });

        // 4. Cambiar portada
        MenuItem itemPortada = new MenuItem(resources != null && resources.containsKey("ctx.cover") ? resources.getString("ctx.cover") : "Cambiar portada");
        itemPortada.setOnAction(e -> {
            if (mainController != null) {
                mainController.cambiarPortada(libro);
            }
        });

        // 5. Estado de lectura
        Menu menuEstado = new Menu(resources != null && resources.containsKey("ctx.mark_as") ? resources.getString("ctx.mark_as") : "Marcar como...");
        for (EstadoLectura est : EstadoLectura.values()) {
            MenuItem item = new MenuItem(est.getEtiqueta());
            item.setOnAction(e -> cambiarEstadoLectura(libro, est));
            menuEstado.getItems().add(item);
        }

        // 6. Etiqueta física (si es libro físico)
        MenuItem itemEtiquetas = new MenuItem(resources != null && resources.containsKey("ctx.label") ? resources.getString("ctx.label") : "Imprimir etiqueta física");
        itemEtiquetas.setOnAction(e -> {
            if (mainController != null) {
                mainController.generarEtiquetaFisica(libro);
            }
        });

        // 7. Eliminar
        MenuItem itemEliminar = new MenuItem(resources != null && resources.containsKey("ctx.delete") ? resources.getString("ctx.delete") : "Eliminar");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> {
            if (mainController != null) {
                mainController.eliminarLibro(libro);
            }
        });

        contextMenuLibros.getItems().addAll(itemPrestar, new SeparatorMenuItem(), itemEditar, itemPortada,
                menuEstado, new SeparatorMenuItem());

        if (!libro.isEsDigital()) {
            contextMenuLibros.getItems().addAll(itemEtiquetas, new SeparatorMenuItem());
        }

        contextMenuLibros.getItems().add(itemEliminar);
        contextMenuLibros.show(owner, screenX, screenY);
    }

    /**
     * Actualiza el estado de lectura de un libro (Pendiente, Leyendo, Leído,
     * Abandonado), persistiendo el cambio con debounce y notificando al bus de
     * eventos de la aplicación.
     *
     * @param libro Libro cuyo estado será modificado.
     * @param nuevoEstado Nuevo {@link EstadoLectura} a aplicar.
     */
    private void cambiarEstadoLectura(Libro libro, EstadoLectura nuevoEstado) {
        libro.setEstadoLecturaEnum(nuevoEstado);
        if (jsonManager != null && listaLibrosCompleta != null) {
            jsonManager.guardarLibrosDebounced(listaLibrosCompleta);
        }
        refrescarCuadricula();
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Estado actualizado a " + nuevoEstado.getEtiqueta() + ": " + libro.getTitulo()));
    }

    /**
     * Solicita al controlador principal abrir la ventana con la ficha de
     * detalle del libro.
     *
     * @param libro Libro seleccionado por el usuario.
     */
    private void abrirDetalle(Libro libro) {
        if (mainController != null) {
            mainController.mostrarDetalleLibro(libro);
        }
    }

    // =========================================================================
    // DRAG & DROP DE E-BOOKS
    // =========================================================================
    /**
     * Configura el comportamiento de arrastrar y soltar (Drag & Drop) sobre el
     * panel del catálogo para permitir la importación directa y automática de
     * archivos e-book (EPUB, PDF, MOBI).
     */
    private void configurarDragAndDrop() {
        if (panelMisLibros == null) {
            return;
        }

        panelMisLibros.setOnDragOver(event -> {
            if (event.getGestureSource() != panelMisLibros && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                panelMisLibros.setStyle("-fx-background-color: -color-accent-subtle; -fx-border-color: -color-accent-emphasis; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8;");
            }
            event.consume();
        });

        panelMisLibros.setOnDragExited(event -> {
            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            event.consume();
        });

        panelMisLibros.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (EbookMetadataService.esArchivoEbook(file)) {
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Importando e-book..."));

                    Thread importThread = new Thread(() -> {
                        Libro nuevoLibro = EbookMetadataService.crearLibroDesdeArchivo(file, this.rutaUsuario);
                        Platform.runLater(() -> {
                            if (nuevoLibro != null) {
                                boolean existe = listaLibrosCompleta.stream()
                                        .anyMatch(l -> l.getTitulo() != null && l.getTitulo().equalsIgnoreCase(nuevoLibro.getTitulo()));

                                if (existe) {
                                    if (mainController != null) {
                                        mainController.mostrarAlertaPublic("Libro duplicado", "Ya existe un libro con el título: " + nuevoLibro.getTitulo());
                                    }
                                } else {
                                    nuevoLibro.setId(UUID.randomUUID().toString());
                                    listaLibrosCompleta.add(nuevoLibro);
                                    if (jsonManager != null) {
                                        jsonManager.guardarLibros(listaLibrosCompleta);
                                    }
                                    refrescarCuadricula();
                                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(nuevoLibro, true));
                                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("E-book importado: " + nuevoLibro.getTitulo()));
                                }
                            }
                        });
                    });
                    importThread.setDaemon(true);
                    importThread.start();
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }
}
