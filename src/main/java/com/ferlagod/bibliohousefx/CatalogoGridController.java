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
import com.bibliohouse.logic.LanguageManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.utils.ImageLoader;
import com.bibliohouse.utils.TextoUtil;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import javafx.scene.layout.Region;
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

    /**
     * Scroll continuo (modo por defecto): en lugar de páginas se pintan
     * {@link #LOTE_CONTINUO} tarjetas y se añaden más al acercarse al final.
     */
    private boolean modoContinuo = true;
    static final int LOTE_CONTINUO = 48;
    /** Número de tarjetas pintadas en modo continuo. */
    private int cargadosContinuo = LOTE_CONTINUO;
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

    /**
     * Marcador del orden «más recientes primero». No compara por sí mismo:
     * {@link #refrescarCuadricula(boolean)} lo sustituye por un comparador
     * basado en la posición de cada libro en la lista completa.
     */
    private static final Comparator<Libro> ORDEN_RECIENTE = (a, b) -> 0;

    /** Duración del fundido al cambiar de página, filtro, orden o vista. */
    private static final Duration DURACION_TRANSICION = Duration.millis(180);

    private Comparator<Libro> currentComparator = Comparator.comparing(
            l -> l.getTitulo() != null ? l.getTitulo() : "",
            String::compareToIgnoreCase);

    /**
     * Espera tras la última pulsación antes de filtrar, para no reconstruir la
     * cuadrícula en cada tecla.
     */
    private final javafx.animation.PauseTransition esperaBusqueda = new javafx.animation.PauseTransition(Duration.millis(220));

    /**
     * Tarjetas ya construidas, reutilizadas mientras el libro no cambie lo que
     * muestran (evita recrear nodos y recargar portadas al filtrar o paginar).
     */
    private final Map<Libro, Object[]> cacheTarjetas = new java.util.IdentityHashMap<>();

    // Preferencias del catálogo que se recuerdan entre sesiones
    static final String PREF_VISTA = "catalogo.vista";
    static final String PREF_ORDEN = "catalogo.orden";
    static final String PREF_CHIP = "catalogo.chip";
    static final String PREF_POR_PAGINA = "catalogo.por_pagina";
    static final String ORDEN_TITULO_AZ = "titulo_az";
    static final String ORDEN_TITULO_ZA = "titulo_za";
    static final String ORDEN_AUTOR = "autor";
    static final String ORDEN_ANIO = "anio";
    static final String ORDEN_RECIENTES = "reciente";

    /**
     * Mapa de preferencias del usuario (compartido con el controlador
     * principal, que lo guarda en disco al cerrar). Puede ser {@code null}.
     */
    private Map<String, String> preferencias;
    private String ordenActual = ORDEN_TITULO_AZ;

    // Selección múltiple
    static final String PROP_LIBRO = "catalogo.libro";
    private static final javafx.css.PseudoClass SELECCIONADA = javafx.css.PseudoClass.getPseudoClass("selected");
    /** Libros seleccionados (por identidad: dos ejemplares iguales son libros distintos). */
    private final java.util.Set<Libro> seleccion = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** Último libro pulsado sin Mayús: origen de la selección por rango. */
    private Libro anclaSeleccion;
    /** Libros que pasan los filtros actuales, en el orden mostrado. */
    private List<Libro> librosFiltradosActuales = List.of();
    /** Evita que el listener de la tabla pise la selección mientras se repinta. */
    private boolean sincronizandoTabla;

    @FXML
    private HBox barraSeleccion;
    private Label lblSeleccion;

    @FXML
    public void initialize() {
        if (txtBuscarMisLibros != null) {
            esperaBusqueda.setOnFinished(e -> {
                paginaActual = 1;
                refrescarCuadricula(true);
            });
            txtBuscarMisLibros.textProperty().addListener((obs, oldVal, newVal) -> esperaBusqueda.playFromStart());
            txtBuscarMisLibros.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE && !txtBuscarMisLibros.getText().isEmpty()) {
                    // Escape borra la búsqueda y aplica el cambio al instante
                    txtBuscarMisLibros.clear();
                    esperaBusqueda.stop();
                    paginaActual = 1;
                    refrescarCuadricula(true);
                    e.consume();
                } else if (e.getCode() == KeyCode.DOWN && panelMisLibros != null && !panelMisLibros.getChildren().isEmpty() && !esVistaLista) {
                    // Flecha abajo salta de la búsqueda a la primera tarjeta
                    panelMisLibros.getChildren().get(0).requestFocus();
                    e.consume();
                }
            });
        }

        if (comboLibrosPorPagina != null) {
            String opcionTodos = LanguageManager.getString("catalog.per_page.all", "Todos");
            String opcionContinuo = LanguageManager.getString("catalog.per_page.continuous", "Continuo");
            comboLibrosPorPagina.getItems().setAll(opcionContinuo, "24", "48", "96", opcionTodos);
            comboLibrosPorPagina.setValue(modoContinuo ? opcionContinuo : String.valueOf(librosPorPagina));
            comboLibrosPorPagina.valueProperty().addListener((obs, oldVal, newVal) -> {
                modoContinuo = opcionContinuo.equals(newVal);
                if (modoContinuo) {
                    cargadosContinuo = LOTE_CONTINUO;
                } else if (opcionTodos.equals(newVal)) {
                    librosPorPagina = Integer.MAX_VALUE;
                } else if (newVal != null) {
                    try {
                        librosPorPagina = Integer.parseInt(newVal.trim());
                    } catch (NumberFormatException ignored) {
                        librosPorPagina = 48;
                    }
                }
                guardarPreferencia(PREF_POR_PAGINA, modoContinuo ? "continuo"
                        : librosPorPagina == Integer.MAX_VALUE ? "todos" : String.valueOf(librosPorPagina));
                paginaActual = 1;
                refrescarCuadricula(true);
            });
        }

        configurarTablaMisLibros();
        configurarBarraSeleccion();
        if (scrollMisLibros != null) {
            scrollMisLibros.vvalueProperty().addListener((obs, antes, ahora) -> {
                if (modoContinuo && ahora.doubleValue() >= 0.9 && cargarMasContinuo()) {
                    rellenarSiCabe();
                }
            });
        }
        configurarAccesibilidadControles();

        // Suscripción al bus de eventos para reaccionar a cambios
        AppEventBus.getInstance().subscribe(this, AppEventBus.FiltroEstanteriaEvent.class, e -> {
            this.categoriaActual = e.getEstanteria();
            this.paginaActual = 1;
            refrescarCuadricula(true);
        });

        AppEventBus.getInstance().subscribe(this, AppEventBus.LibroModificadoEvent.class, e -> {
            // La portada puede haberse reemplazado en la misma ruta (covers/<id>.jpg):
            // la firma de la tarjeta no cambia, así que se descarta explícitamente.
            if (e.getLibro() != null) {
                cacheTarjetas.remove(e.getLibro());
            } else {
                cacheTarjetas.clear();
            }
            refrescarCuadricula();
        });
        AppEventBus.getInstance().subscribe(this, AppEventBus.LibroEliminadoEvent.class, e -> refrescarCuadricula());
        AppEventBus.getInstance().subscribe(this, AppEventBus.CatalogoSincronizadoEvent.class, e -> refrescarCuadricula());
    }

    /**
     * Configura la accesibilidad semántica (AccessibleRole y AccessibleText)
     * para lectores de pantalla en los controles interactivos del catálogo.
     */
    private void configurarAccesibilidadControles() {
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.setAccessibleRole(AccessibleRole.TEXT_FIELD);
            txtBuscarMisLibros.setAccessibleText(LanguageManager.getString("ui.buscar_libros_por_titulo_o_autor", "Buscar libros por título o autor en el catálogo"));
        }
        if (btnOrdenar != null) {
            btnOrdenar.setAccessibleRole(AccessibleRole.BUTTON);
            btnOrdenar.setAccessibleText(LanguageManager.getString("ui.opciones_de_ordenacion_del_catalogo", "Opciones de ordenación del catálogo"));
        }
        if (btnVistaCuadricula != null) {
            btnVistaCuadricula.setAccessibleRole(AccessibleRole.BUTTON);
            btnVistaCuadricula.setAccessibleText(LanguageManager.getString("ui.cambiar_a_vista_de_cuadricula_con", "Cambiar a vista de cuadrícula con portadas de libros"));
        }
        if (btnVistaLista != null) {
            btnVistaLista.setAccessibleRole(AccessibleRole.BUTTON);
            btnVistaLista.setAccessibleText(LanguageManager.getString("ui.cambiar_a_vista_de_lista_tabular", "Cambiar a vista de lista tabular detallada"));
        }
        if (chipTodos != null) {
            chipTodos.setAccessibleRole(AccessibleRole.BUTTON);
            chipTodos.setAccessibleText(LanguageManager.getString("ui.mostrar_todos_los_libros_de_la", "Mostrar todos los libros de la colección"));
        }
        if (chipLeyendo != null) {
            chipLeyendo.setAccessibleRole(AccessibleRole.BUTTON);
            chipLeyendo.setAccessibleText(LanguageManager.getString("ui.filtrar_libros_actualmente_en_lectura", "Filtrar libros actualmente en lectura"));
        }
        if (chipLeidos != null) {
            chipLeidos.setAccessibleRole(AccessibleRole.BUTTON);
            chipLeidos.setAccessibleText(LanguageManager.getString("ui.filtrar_libros_ya_leidos", "Filtrar libros ya leídos"));
        }
        if (chipPendientes != null) {
            chipPendientes.setAccessibleRole(AccessibleRole.BUTTON);
            chipPendientes.setAccessibleText(LanguageManager.getString("ui.filtrar_libros_pendientes_de_lectura", "Filtrar libros pendientes de lectura"));
        }
        if (chipDigitales != null) {
            chipDigitales.setAccessibleRole(AccessibleRole.BUTTON);
            chipDigitales.setAccessibleText(LanguageManager.getString("ui.filtrar_unicamente_libros_electronicos_e_books", "Filtrar únicamente libros electrónicos e-books"));
        }
        if (btnPrimeraPagina != null) {
            btnPrimeraPagina.setAccessibleRole(AccessibleRole.BUTTON);
            btnPrimeraPagina.setAccessibleText(LanguageManager.getString("ui.ir_a_la_primera_pagina_de", "Ir a la primera página de libros"));
        }
        if (btnAnterior != null) {
            btnAnterior.setAccessibleRole(AccessibleRole.BUTTON);
            btnAnterior.setAccessibleText(LanguageManager.getString("ui.ir_a_la_pagina_anterior_de", "Ir a la página anterior de libros"));
        }
        if (btnSiguiente != null) {
            btnSiguiente.setAccessibleRole(AccessibleRole.BUTTON);
            btnSiguiente.setAccessibleText(LanguageManager.getString("ui.ir_a_la_pagina_siguiente_de", "Ir a la página siguiente de libros"));
        }
        if (btnUltima != null) {
            btnUltima.setAccessibleRole(AccessibleRole.BUTTON);
            btnUltima.setAccessibleText(LanguageManager.getString("ui.ir_a_la_ultima_pagina_de", "Ir a la última página de libros"));
        }
        if (comboLibrosPorPagina != null) {
            comboLibrosPorPagina.setAccessibleRole(AccessibleRole.COMBO_BOX);
            comboLibrosPorPagina.setAccessibleText(LanguageManager.getString("ui.seleccionar_cantidad_de_libros_por_pagina", "Seleccionar cantidad de libros por página"));
        }
        if (lblInfoPaginacion != null) {
            lblInfoPaginacion.setAccessibleRole(AccessibleRole.TEXT);
        }
        if (panelMisLibros != null) {
            panelMisLibros.setFocusTraversable(true);
            panelMisLibros.setAccessibleRole(AccessibleRole.PARENT);
            panelMisLibros.setAccessibleText(LanguageManager.getString("ui.cuadricula_interactiva_de_libros", "Cuadrícula interactiva de libros"));
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
        cambiarOrden(ORDEN_TITULO_AZ);
    }

    /**
     * Ordena los libros alfabéticamente por título de forma descendente (Z - A)
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorTituloZA() {
        cambiarOrden(ORDEN_TITULO_ZA);
    }

    /**
     * Ordena los libros alfabéticamente por autor de la A a la Z y reinicia la
     * paginación a la primera página.
     */
    @FXML
    public void ordenarPorAutor() {
        cambiarOrden(ORDEN_AUTOR);
    }

    /**
     * Ordena los libros por año de publicación de más reciente a más antiguo y
     * reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorAnio() {
        cambiarOrden(ORDEN_ANIO);
    }

    /**
     * Ordena los libros por orden de inserción o modificación más reciente y
     * reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorReciente() {
        cambiarOrden(ORDEN_RECIENTES);
    }

    private void cambiarOrden(String clave) {
        aplicarOrden(clave);
        guardarPreferencia(PREF_ORDEN, ordenActual);
        paginaActual = 1;
        refrescarCuadricula(true);
    }

    /**
     * Fija el comparador correspondiente a la clave de orden y muestra el orden
     * activo en el botón «Ordenar».
     */
    private void aplicarOrden(String clave) {
        Comparator<Libro> porTitulo = Comparator.comparing((Libro l) -> l.getTitulo() != null ? l.getTitulo() : "", String::compareToIgnoreCase);
        int indiceMenu;
        switch (clave == null ? "" : clave) {
            case ORDEN_TITULO_ZA -> {
                currentComparator = porTitulo.reversed();
                indiceMenu = 1;
            }
            case ORDEN_AUTOR -> {
                currentComparator = Comparator.comparing((Libro l) -> l.getAutor() != null ? l.getAutor() : "", String::compareToIgnoreCase);
                indiceMenu = 2;
            }
            case ORDEN_ANIO -> {
                currentComparator = (l1, l2) -> {
                    String a1 = l1.getAño() != null ? l1.getAño() : "";
                    String a2 = l2.getAño() != null ? l2.getAño() : "";
                    return a2.compareTo(a1); // Descendente
                };
                indiceMenu = 3;
            }
            case ORDEN_RECIENTES -> {
                // Antes se usaba indexOf() dentro del comparador (O(n² log n)).
                // Ahora la posición de cada libro se calcula una vez por refresco.
                currentComparator = ORDEN_RECIENTE;
                indiceMenu = 4;
            }
            default -> {
                clave = ORDEN_TITULO_AZ;
                currentComparator = porTitulo;
                indiceMenu = 0;
            }
        }
        ordenActual = clave;
        if (btnOrdenar != null && indiceMenu < btnOrdenar.getItems().size()) {
            btnOrdenar.setText("⇅ " + btnOrdenar.getItems().get(indiceMenu).getText());
        }
    }

    /**
     * Clave del orden activo (por ejemplo {@code "autor"}).
     *
     * @return Clave del orden.
     */
    public String getOrdenActual() {
        return ordenActual;
    }

    // =========================================================================
    // PREFERENCIAS RECORDADAS
    // =========================================================================
    /**
     * Recibe el mapa de preferencias del usuario y restaura la vista, el orden,
     * el chip de filtro y el tamaño de página de la última sesión. Debe
     * llamarse antes de {@link #initData}.
     *
     * @param preferencias Mapa de preferencias (se modifica al cambiar
     * opciones; el controlador principal lo guarda al cerrar).
     */
    public void setPreferencias(Map<String, String> preferencias) {
        if (this.preferencias == preferencias) {
            return; // Ya aplicadas (p. ej. al recargar tras sincronizar)
        }
        this.preferencias = preferencias;
        if (preferencias == null) {
            return;
        }
        aplicarOrden(preferencias.get(PREF_ORDEN));

        String chip = preferencias.get(PREF_CHIP);
        if (chip != null) {
            try {
                FiltroChip filtro = FiltroChip.valueOf(chip);
                activarChip(botonDeChip(filtro), filtro);
            } catch (IllegalArgumentException ignored) {
                // Valor de una versión anterior: se mantiene «Todos»
            }
        }

        String porPagina = preferencias.get(PREF_POR_PAGINA);
        if (porPagina != null) {
            modoContinuo = "continuo".equals(porPagina);
            if (modoContinuo) {
                if (comboLibrosPorPagina != null) {
                    comboLibrosPorPagina.setValue(LanguageManager.getString("catalog.per_page.continuous", "Continuo"));
                }
            } else if ("todos".equals(porPagina)) {
                librosPorPagina = Integer.MAX_VALUE;
            } else {
                try {
                    librosPorPagina = Math.max(1, Integer.parseInt(porPagina));
                } catch (NumberFormatException ignored) {
                    // Se conserva el valor por defecto
                }
            }
            if (comboLibrosPorPagina != null && !modoContinuo) {
                String valor = librosPorPagina == Integer.MAX_VALUE
                        ? LanguageManager.getString("catalog.per_page.all", "Todos")
                        : String.valueOf(librosPorPagina);
                if (comboLibrosPorPagina.getItems().contains(valor)) {
                    comboLibrosPorPagina.setValue(valor);
                }
            }
        }

        if ("lista".equals(preferencias.get(PREF_VISTA))) {
            cambiarAVistaLista();
        }
    }

    private void guardarPreferencia(String clave, String valor) {
        if (preferencias != null) {
            preferencias.put(clave, valor);
        }
    }

    private Button botonDeChip(FiltroChip filtro) {
        return switch (filtro) {
            case LEYENDO -> chipLeyendo;
            case LEIDOS -> chipLeidos;
            case PENDIENTES -> chipPendientes;
            case DIGITALES -> chipDigitales;
            default -> chipTodos;
        };
    }

    /**
     * Chip de filtro rápido activo.
     *
     * @return Filtro activo.
     */
    public FiltroChip getFiltroChipActual() {
        return filtroChipActual;
    }

    /**
     * Indica si está activa la vista de lista (en lugar de la cuadrícula).
     *
     * @return {@code true} en vista de lista.
     */
    public boolean isVistaLista() {
        return esVistaLista;
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
            refrescarCuadricula(true);
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
            refrescarCuadricula(true);
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
            refrescarCuadricula(true);
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
            refrescarCuadricula(true);
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
        // En modo continuo no hay páginas: se ocultan los botones de navegación
        for (Button b : new Button[]{btnPrimeraPagina, btnAnterior, btnSiguiente, btnUltima}) {
            if (b != null) {
                b.setVisible(!modoContinuo);
                b.setManaged(!modoContinuo);
            }
        }
        if (modoContinuo && total > 0) {
            if (lblInfoPaginacion != null) {
                lblInfoPaginacion.setText(String.format(LanguageManager.getString("catalog.continuous.info", "%d de %d libros"), fin, total));
            }
            return;
        }

        if (total == 0) {
            if (lblInfoPaginacion != null) {
                lblInfoPaginacion.setText(LanguageManager.getString("ui.0_libros", "0 libros"));
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
            lblInfoPaginacion.setText(String.format(LanguageManager.getString("ui.mostrando_d_d_de_d_pag", "Mostrando %d-%d de %d (Pág. %d/%d)"),
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

        // Selección múltiple nativa de la tabla (Cmd/Ctrl/Mayús + clic)
        tablaMisLibros.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        tablaMisLibros.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Libro>) c -> {
            if (sincronizandoTabla || !esVistaLista) {
                return;
            }
            seleccion.clear();
            seleccion.addAll(tablaMisLibros.getSelectionModel().getSelectedItems());
            actualizarBarraSeleccion();
        });
        tablaMisLibros.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                eliminarSeleccion();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                limpiarSeleccion();
                e.consume();
            }
        });
    }

    @FXML
    public void cambiarAVistaCuadricula() {
        esVistaLista = false;
        guardarPreferencia(PREF_VISTA, "cuadricula");
        seleccion.clear();
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
        refrescarCuadricula(true);
    }

    @FXML
    public void cambiarAVistaLista() {
        esVistaLista = true;
        guardarPreferencia(PREF_VISTA, "lista");
        seleccion.clear();
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
        refrescarCuadricula(true);
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
        guardarPreferencia(PREF_CHIP, nuevoFiltro.name());
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
        refrescarCuadricula(true);
    }

    // =========================================================================
    // RENDERIZADO DE LA CUADRÍCULA
    // =========================================================================
    /**
     * Dibuja las tarjetas de libros respetando el filtro lateral de estantería,
     * el texto de búsqueda rápida y la paginación activa.
     */
    public void refrescarCuadricula() {
        refrescarCuadricula(false);
    }

    /**
     * Redibuja la cuadrícula o la lista.
     *
     * @param animar {@code true} cuando el usuario ha navegado (página,
     * filtro, orden, búsqueda, vista): el contenido nuevo entra con un fundido
     * suave. Los refrescos por cambios de datos no se animan para no parpadear.
     */
    public void refrescarCuadricula(boolean animar) {
        if (panelMisLibros == null || listaLibrosCompleta == null) {
            return;
        }

        Comparator<Libro> comparador = currentComparator;
        if (comparador == ORDEN_RECIENTE) {
            Map<Libro, Integer> posicion = new java.util.IdentityHashMap<>(listaLibrosCompleta.size() * 2);
            for (int i = 0; i < listaLibrosCompleta.size(); i++) {
                posicion.put(listaLibrosCompleta.get(i), i);
            }
            comparador = Comparator.comparingInt((Libro l) -> posicion.getOrDefault(l, -1)).reversed();
        }

        String busquedaRapida = txtBuscarMisLibros != null ? TextoUtil.normalizarBusqueda(txtBuscarMisLibros.getText()) : "";

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
                .filter(l -> coincideBusqueda(l, busquedaRapida))
                .sorted(comparador)
                .collect(Collectors.toList());

        totalLibrosFiltrados = filtrados.size();
        librosFiltradosActuales = filtrados;
        podarSeleccion(filtrados);

        // Estado vacío
        if (filtrados.isEmpty()) {
            totalPaginas = 1;
            paginaActual = 1;
            actualizarBarraPaginacion(0, 0, 0);

            boolean hayFiltros = filtroChipActual != FiltroChip.TODOS || !busquedaRapida.isEmpty();
            // Un nodo solo puede tener un padre: la cuadrícula y la tabla
            // necesitan cada una su propio estado vacío. Antes se compartía y la
            // tabla «robaba» el nodo, dejando la cuadrícula en blanco.
            panelMisLibros.getChildren().setAll(crearEstadoVacio(hayFiltros));
            if (tablaMisLibros != null) {
                tablaMisLibros.setItems(FXCollections.emptyObservableList());
                tablaMisLibros.setPlaceholder(crearEstadoVacio(hayFiltros));
            }
            actualizarBarraSeleccion();
            if (animar) {
                animarEntradaContenido();
            }
            return;
        }

        if (modoContinuo) {
            pintarContinuo(filtrados, animar);
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

        // Construir (o reutilizar) tarjetas solo para la página actual
        List<Node> tarjetas = paginaLibros.stream()
                .map(this::obtenerTarjeta)
                .collect(Collectors.toList());
        panelMisLibros.getChildren().setAll(tarjetas);
        podarCacheTarjetas();

        // Actualizar la tabla para la vista de lista compacta
        if (tablaMisLibros != null) {
            sincronizandoTabla = true;
            try {
                tablaMisLibros.setItems(FXCollections.observableArrayList(paginaLibros));
                for (Libro l : paginaLibros) {
                    if (seleccion.contains(l)) {
                        tablaMisLibros.getSelectionModel().select(l);
                    }
                }
            } finally {
                sincronizandoTabla = false;
            }
        }
        aplicarEstiloSeleccion();
        actualizarBarraSeleccion();
        if (animar) {
            animarEntradaContenido();
        }
    }

    // =========================================================================
    // SCROLL CONTINUO
    // =========================================================================
    /**
     * Pinta las primeras tarjetas de la lista filtrada. Al navegar (filtro,
     * orden, búsqueda) se vuelve al primer lote; en refrescos por cambios de
     * datos se conservan las ya cargadas para no perder la posición.
     */
    private void pintarContinuo(List<Libro> filtrados, boolean reiniciar) {
        totalPaginas = 1;
        paginaActual = 1;
        if (reiniciar) {
            cargadosContinuo = LOTE_CONTINUO;
            scrollearArriba();
        }
        int hasta = Math.min(Math.max(cargadosContinuo, LOTE_CONTINUO), filtrados.size());
        cargadosContinuo = hasta;
        panelMisLibros.getChildren().setAll(filtrados.subList(0, hasta).stream()
                .map(this::obtenerTarjeta)
                .collect(Collectors.toList()));
        podarCacheTarjetas();
        actualizarBarraPaginacion(1, hasta, filtrados.size());

        // La lista ya está virtualizada por TableView: muestra todos los libros
        if (tablaMisLibros != null) {
            sincronizandoTabla = true;
            try {
                tablaMisLibros.setItems(FXCollections.observableArrayList(filtrados));
                filtrados.stream().filter(seleccion::contains).forEach(l -> tablaMisLibros.getSelectionModel().select(l));
            } finally {
                sincronizandoTabla = false;
            }
        }
        aplicarEstiloSeleccion();
        actualizarBarraSeleccion();
        if (reiniciar) {
            animarEntradaContenido();
        }
        rellenarSiCabe();
    }

    /**
     * Añade el siguiente lote de tarjetas al final de la cuadrícula sin
     * reconstruir las existentes.
     *
     * @return {@code true} si se añadió alguna tarjeta.
     */
    public boolean cargarMasContinuo() {
        if (!modoContinuo || panelMisLibros == null || cargadosContinuo >= librosFiltradosActuales.size()) {
            return false;
        }
        int desde = cargadosContinuo;
        int hasta = Math.min(desde + LOTE_CONTINUO, librosFiltradosActuales.size());
        List<Node> nuevas = librosFiltradosActuales.subList(desde, hasta).stream()
                .map(this::obtenerTarjeta)
                .collect(Collectors.toList());
        panelMisLibros.getChildren().addAll(nuevas);
        cargadosContinuo = hasta;
        aplicarEstiloSeleccion();
        actualizarBarraPaginacion(1, hasta, librosFiltradosActuales.size());
        return true;
    }

    /**
     * Si las tarjetas pintadas no llenan la ventana (pantallas grandes), no
     * habría scroll con el que pedir más: se cargan lotes hasta llenarla.
     */
    private void rellenarSiCabe() {
        if (scrollMisLibros == null || scrollMisLibros.getScene() == null) {
            return;
        }
        Platform.runLater(() -> {
            double alto = scrollMisLibros.getViewportBounds() != null ? scrollMisLibros.getViewportBounds().getHeight() : 0;
            if (modoContinuo && alto > 0 && panelMisLibros.getHeight() < alto && cargarMasContinuo()) {
                rellenarSiCabe();
            }
        });
    }

    // =========================================================================
    // SELECCIÓN MÚLTIPLE Y ACCIONES EN LOTE
    // =========================================================================
    /**
     * Libros seleccionados actualmente, en el orden en que se muestran.
     *
     * @return Copia de la selección.
     */
    public List<Libro> getSeleccion() {
        return librosFiltradosActuales.stream().filter(seleccion::contains).collect(Collectors.toList());
    }

    /** Selecciona solo este libro (clic normal). */
    public void seleccionarSolo(Libro libro) {
        seleccion.clear();
        seleccion.add(libro);
        anclaSeleccion = libro;
        alCambiarSeleccion();
    }

    /** Añade o quita el libro de la selección (Cmd/Ctrl + clic). */
    public void alternarSeleccion(Libro libro) {
        if (!seleccion.remove(libro)) {
            seleccion.add(libro);
        }
        anclaSeleccion = libro;
        alCambiarSeleccion();
    }

    /**
     * Selecciona todos los libros entre el último pulsado y este, según el
     * orden actual (Mayús + clic). Incluye libros de otras páginas.
     */
    public void seleccionarRango(Libro libro) {
        int fin = indiceEnFiltrados(libro);
        int inicio = anclaSeleccion != null ? indiceEnFiltrados(anclaSeleccion) : -1;
        if (fin < 0) {
            return;
        }
        if (inicio < 0) {
            seleccionarSolo(libro);
            return;
        }
        seleccion.clear();
        for (int i = Math.min(inicio, fin); i <= Math.max(inicio, fin); i++) {
            seleccion.add(librosFiltradosActuales.get(i));
        }
        alCambiarSeleccion();
    }

    /** Selecciona todos los libros que pasan los filtros (Cmd/Ctrl + A). */
    public void seleccionarTodo() {
        seleccion.clear();
        seleccion.addAll(librosFiltradosActuales);
        alCambiarSeleccion();
    }

    /** Vacía la selección (Escape o botón ✕ de la barra). */
    public void limpiarSeleccion() {
        seleccion.clear();
        anclaSeleccion = null;
        if (tablaMisLibros != null) {
            sincronizandoTabla = true;
            tablaMisLibros.getSelectionModel().clearSelection();
            sincronizandoTabla = false;
        }
        alCambiarSeleccion();
    }

    private int indiceEnFiltrados(Libro libro) {
        for (int i = 0; i < librosFiltradosActuales.size(); i++) {
            if (librosFiltradosActuales.get(i) == libro) {
                return i;
            }
        }
        return -1;
    }

    /** Quita de la selección los libros que ya no se muestran. */
    private void podarSeleccion(List<Libro> visibles) {
        if (seleccion.isEmpty()) {
            return;
        }
        java.util.Set<Libro> vivos = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        vivos.addAll(visibles);
        seleccion.retainAll(vivos);
    }

    private void alCambiarSeleccion() {
        aplicarEstiloSeleccion();
        actualizarBarraSeleccion();
    }

    /** Marca visualmente las tarjetas seleccionadas de la página actual. */
    private void aplicarEstiloSeleccion() {
        if (panelMisLibros == null) {
            return;
        }
        for (Node n : panelMisLibros.getChildren()) {
            Object l = n.getProperties().get(PROP_LIBRO);
            n.pseudoClassStateChanged(SELECCIONADA, l instanceof Libro libro && seleccion.contains(libro));
        }
    }

    /**
     * Crea el contenido de la barra de selección (oculta hasta que haya dos o
     * más libros seleccionados).
     */
    private void configurarBarraSeleccion() {
        if (barraSeleccion == null) {
            return;
        }
        lblSeleccion = new Label();
        lblSeleccion.getStyleClass().add("selection-bar-count");

        MenuButton marcar = new MenuButton(LanguageManager.getString("ctx.mark_as", "Marcar como..."));
        marcar.setOnShowing(e -> marcar.getItems().setAll(crearItemsEstado()));
        MenuButton estanteria = new MenuButton(LanguageManager.getString("bulk.add_to_shelf", "Añadir a estantería"));
        estanteria.setOnShowing(e -> estanteria.getItems().setAll(crearItemsEstanteria()));
        Button etiquetas = new Button(LanguageManager.getString("bulk.labels", "Imprimir etiquetas"));
        etiquetas.setOnAction(e -> imprimirEtiquetasSeleccion());
        Button eliminar = new Button(LanguageManager.getString("ctx.delete", "Eliminar"));
        eliminar.getStyleClass().add("danger");
        eliminar.setOnAction(e -> eliminarSeleccion());
        Button cerrar = new Button("✕");
        cerrar.getStyleClass().add("flat");
        cerrar.setAccessibleText(LanguageManager.getString("bulk.clear", "Quitar selección"));
        cerrar.setTooltip(new javafx.scene.control.Tooltip(cerrar.getAccessibleText()));
        cerrar.setOnAction(e -> limpiarSeleccion());

        Region hueco = new Region();
        HBox.setHgrow(hueco, javafx.scene.layout.Priority.ALWAYS);
        barraSeleccion.getChildren().setAll(lblSeleccion, hueco, marcar, estanteria, etiquetas, eliminar, cerrar);
        barraSeleccion.setVisible(false);
        barraSeleccion.setManaged(false);
    }

    private void actualizarBarraSeleccion() {
        if (barraSeleccion == null || lblSeleccion == null) {
            return;
        }
        int n = seleccion.size();
        boolean mostrar = n > 1;
        if (mostrar) {
            lblSeleccion.setText(String.format(LanguageManager.getString("bulk.count", "%d libros seleccionados"), n));
        }
        if (barraSeleccion.isVisible() != mostrar) {
            barraSeleccion.setVisible(mostrar);
            barraSeleccion.setManaged(mostrar);
            if (mostrar) {
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(150), barraSeleccion);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }
        }
    }

    private List<MenuItem> crearItemsEstado() {
        List<MenuItem> items = new java.util.ArrayList<>();
        for (EstadoLectura est : EstadoLectura.values()) {
            MenuItem item = new MenuItem(est.getEtiqueta());
            item.setOnAction(e -> marcarSeleccionComo(est));
            items.add(item);
        }
        return items;
    }

    private List<MenuItem> crearItemsEstanteria() {
        List<String> estanterias = jsonManager != null ? jsonManager.cargarEstanterias() : List.of();
        List<MenuItem> items = new java.util.ArrayList<>();
        if (estanterias == null || estanterias.isEmpty()) {
            MenuItem vacio = new MenuItem(LanguageManager.getString("bulk.no_shelves", "No hay estanterías creadas"));
            vacio.setDisable(true);
            items.add(vacio);
            return items;
        }
        for (String nombre : estanterias) {
            MenuItem item = new MenuItem(nombre);
            item.setOnAction(e -> anadirSeleccionAEstanteria(nombre));
            items.add(item);
        }
        return items;
    }

    /** Opciones del menú contextual cuando hay varios libros seleccionados. */
    private List<MenuItem> crearItemsAccionesLote() {
        Menu estado = new Menu(LanguageManager.getString("ctx.mark_as", "Marcar como..."));
        estado.getItems().setAll(crearItemsEstado());
        Menu estanteria = new Menu(LanguageManager.getString("bulk.add_to_shelf", "Añadir a estantería"));
        estanteria.getItems().setAll(crearItemsEstanteria());
        MenuItem etiquetas = new MenuItem(LanguageManager.getString("bulk.labels", "Imprimir etiquetas"));
        etiquetas.setOnAction(e -> imprimirEtiquetasSeleccion());
        MenuItem eliminar = new MenuItem(String.format(LanguageManager.getString("bulk.delete_n", "Eliminar %d libros"), seleccion.size()));
        eliminar.getStyleClass().add("danger");
        eliminar.setOnAction(e -> eliminarSeleccion());
        return List.of(estado, estanteria, etiquetas, new SeparatorMenuItem(), eliminar);
    }

    /**
     * Cambia el estado de lectura de todos los libros seleccionados.
     *
     * @param estado Nuevo estado.
     */
    public void marcarSeleccionComo(EstadoLectura estado) {
        List<Libro> libros = getSeleccion();
        if (libros.isEmpty()) {
            return;
        }
        libros.forEach(l -> l.setEstadoLecturaEnum(estado));
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(String.format(
                LanguageManager.getString("bulk.marked", "%d libros marcados como %s"), libros.size(), estado.getEtiqueta())));
    }

    /**
     * Añade todos los libros seleccionados a una estantería (sin duplicarla en
     * los que ya la tienen).
     *
     * @param estanteria Nombre de la estantería.
     */
    public void anadirSeleccionAEstanteria(String estanteria) {
        List<Libro> libros = getSeleccion();
        if (libros.isEmpty() || estanteria == null) {
            return;
        }
        for (Libro l : libros) {
            List<String> actuales = l.getEstanterias() != null ? new java.util.ArrayList<>(l.getEstanterias()) : new java.util.ArrayList<>();
            if (!actuales.contains(estanteria)) {
                actuales.add(estanteria);
                l.setEstanterias(actuales);
            }
        }
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(String.format(
                LanguageManager.getString("bulk.shelved", "%d libros añadidos a «%s»"), libros.size(), estanteria)));
    }

    private void imprimirEtiquetasSeleccion() {
        if (mainController != null) {
            mainController.generarEtiquetasFisicas(getSeleccion());
        }
    }

    /**
     * Elimina los libros seleccionados (o el enfocado si no hay selección),
     * con opción de deshacer.
     */
    public void eliminarSeleccion() {
        List<Libro> libros = getSeleccion();
        if (libros.isEmpty() || mainController == null) {
            return;
        }
        if (libros.size() == 1) {
            mainController.eliminarLibro(libros.get(0));
        } else {
            mainController.eliminarLibros(libros);
        }
    }

    /**
     * Indica si un libro coincide con la búsqueda rápida. Busca en título,
     * autor, género, serie y editorial (sin distinguir tildes ni mayúsculas) y
     * en el ISBN ignorando guiones y espacios, de modo que «978-84-376» y
     * «97884376» encuentran el mismo libro.
     *
     * @param l Libro a comprobar.
     * @param consulta Consulta ya normalizada con
     * {@link TextoUtil#normalizarBusqueda(String)}.
     * @return {@code true} si coincide o la consulta está vacía.
     */
    static boolean coincideBusqueda(Libro l, String consulta) {
        if (consulta == null || consulta.isEmpty()) {
            return true;
        }
        if (TextoUtil.contiene(l.getTitulo(), consulta)
                || TextoUtil.contiene(l.getAutor(), consulta)
                || TextoUtil.contiene(l.getGenero(), consulta)
                || TextoUtil.contiene(l.getSerie(), consulta)
                || TextoUtil.contiene(l.getEditorial(), consulta)) {
            return true;
        }
        String isbnConsulta = consulta.replaceAll("[\\s-]", "");
        return !isbnConsulta.isEmpty() && l.getIsbn() != null
                && l.getIsbn().replaceAll("[\\s-]", "").toLowerCase(java.util.Locale.ROOT).contains(isbnConsulta);
    }

    /**
     * Lleva el foco al campo de búsqueda del catálogo y selecciona su texto,
     * para escribir directamente una nueva búsqueda.
     */
    public void enfocarBusqueda() {
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.requestFocus();
            txtBuscarMisLibros.selectAll();
        }
    }

    /**
     * Construye el mensaje que se muestra cuando no hay libros que enseñar. Si
     * el vacío se debe a la búsqueda o a un chip, ofrece limpiarlos con un
     * clic en vez de obligar a deshacerlos a mano.
     */
    private VBox crearEstadoVacio(boolean hayFiltros) {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("catalog-empty-state");

        Label lblIcon = new Label(hayFiltros ? "🔍" : "📚");
        lblIcon.getStyleClass().add("catalog-empty-icon");

        Label lblVacio = new Label(hayFiltros
                ? LanguageManager.getString("catalog.empty.filtered.title", "Ningún libro coincide con los filtros")
                : LanguageManager.getString("catalog.empty.title", "Tu biblioteca está vacía aquí"));
        lblVacio.getStyleClass().add("catalog-empty-title");

        Label lblSub = new Label(hayFiltros
                ? LanguageManager.getString("catalog.empty.filtered.subtitle", "Prueba con otra búsqueda o quita el filtro activo.")
                : LanguageManager.getString("catalog.empty.subtitle",
                        "Prueba a cambiar el filtro de estantería o chip\no añade libros nuevos desde «Gestionar Libros»"));
        lblSub.getStyleClass().add("catalog-empty-subtitle");
        lblSub.setWrapText(true);
        lblSub.setMaxWidth(400);
        lblSub.setAlignment(Pos.CENTER);
        lblSub.setTextAlignment(TextAlignment.CENTER);

        emptyState.getChildren().addAll(lblIcon, lblVacio, lblSub);

        if (hayFiltros) {
            Button btnLimpiar = new Button(LanguageManager.getString("catalog.empty.clear_filters", "Limpiar filtros"));
            btnLimpiar.getStyleClass().add("accent");
            btnLimpiar.setOnAction(e -> limpiarFiltros());
            emptyState.getChildren().add(btnLimpiar);
        }
        return emptyState;
    }

    /**
     * Quita la búsqueda rápida y el chip activo, y vuelve a la primera página.
     */
    public void limpiarFiltros() {
        esperaBusqueda.stop();
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.clear();
            esperaBusqueda.stop(); // clear() dispara el listener de texto
        }
        activarChip(chipTodos, FiltroChip.TODOS);
    }

    /**
     * Hace entrar el contenido visible (cuadrícula o tabla) con un fundido y
     * un desplazamiento vertical corto, para que los cambios de página o
     * filtro no sean un salto brusco.
     */
    private void animarEntradaContenido() {
        Node objetivo = esVistaLista && tablaMisLibros != null ? tablaMisLibros : panelMisLibros;
        if (objetivo == null || objetivo.getScene() == null) {
            return;
        }
        Object anterior = objetivo.getProperties().remove("catalog_transition");
        if (anterior instanceof javafx.animation.Animation a) {
            a.stop();
        }
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(DURACION_TRANSICION, objetivo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(DURACION_TRANSICION, objetivo);
        slide.setFromY(8);
        slide.setToY(0);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        javafx.animation.ParallelTransition entrada = new javafx.animation.ParallelTransition(fade, slide);
        entrada.setOnFinished(e -> {
            objetivo.setOpacity(1.0);
            objetivo.setTranslateY(0);
            objetivo.getProperties().remove("catalog_transition");
        });
        objetivo.getProperties().put("catalog_transition", entrada);
        entrada.play();
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
        this.modoContinuo = false;
    }

    /**
     * Activa o desactiva el scroll continuo.
     *
     * @param continuo {@code true} para cargar tarjetas al hacer scroll en
     * lugar de paginar.
     */
    public void setModoContinuo(boolean continuo) {
        this.modoContinuo = continuo;
        this.cargadosContinuo = LOTE_CONTINUO;
    }

    /**
     * Indica si está activo el scroll continuo.
     *
     * @return {@code true} en modo continuo.
     */
    public boolean isModoContinuo() {
        return modoContinuo;
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
    /**
     * Devuelve la tarjeta de un libro, reutilizando la ya construida si lo que
     * muestra no ha cambiado.
     */
    private Node obtenerTarjeta(Libro libro) {
        String firma = firmaTarjeta(libro);
        Object[] cacheada = cacheTarjetas.get(libro);
        if (cacheada != null && firma.equals(cacheada[0])) {
            return (Node) cacheada[1];
        }
        Node tarjeta = crearTarjetaMisLibros(libro);
        cacheTarjetas.put(libro, new Object[]{firma, tarjeta});
        return tarjeta;
    }

    /**
     * Resume los datos que pinta una tarjeta; si cambia, la tarjeta se rehace.
     */
    private static String firmaTarjeta(Libro l) {
        return l.getTitulo() + '\u0000' + l.getAutor() + '\u0000' + l.getPortadaURL() + '\u0000'
                + l.getEstadoLecturaEnum() + '\u0000' + l.getCalificacion() + '\u0000' + l.isEsDigital() + '\u0000'
                + l.getPaginaActual() + '/' + l.getPaginasTotales();
    }

    /**
     * Elimina de la caché las tarjetas de libros que ya no están en la
     * biblioteca, para que no crezca sin límite.
     */
    private void podarCacheTarjetas() {
        if (listaLibrosCompleta == null || cacheTarjetas.size() <= listaLibrosCompleta.size() + 64) {
            return;
        }
        java.util.Set<Libro> vivos = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        vivos.addAll(listaLibrosCompleta);
        cacheTarjetas.keySet().retainAll(vivos);
    }

    private VBox crearTarjetaMisLibros(Libro libro) {
        VBox tarjeta = new VBox(6);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(146);
        tarjeta.getStyleClass().add("book-card");
        tarjeta.getProperties().put(PROP_LIBRO, libro);
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
            if (code == KeyCode.A && event.isShortcutDown()) {
                seleccionarTodo();
                event.consume();
                return;
            }
            if (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
                if (!seleccion.contains(libro)) {
                    seleccionarSolo(libro);
                }
                eliminarSeleccion();
                event.consume();
                return;
            }
            if (code == KeyCode.ESCAPE && !seleccion.isEmpty()) {
                limpiarSeleccion();
                event.consume();
                return;
            }
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
                if (e.isShortcutDown()) {
                    alternarSeleccion(libro);
                } else if (e.isShiftDown()) {
                    seleccionarRango(libro);
                } else {
                    seleccionarSolo(libro);
                    if (e.getClickCount() == 2) {
                        abrirDetalle(libro);
                    }
                }
                tarjeta.requestFocus();
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                if (!seleccion.contains(libro)) {
                    seleccionarSolo(libro);
                }
                mostrarContextMenu(tarjeta, libro, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        tarjeta.setOnContextMenuRequested(e -> {
            if (mainController != null) {
                mainController.seleccionarLibro(libro);
            }
            if (!seleccion.contains(libro)) {
                seleccionarSolo(libro);
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
        img.setAccessibleText(LanguageManager.getString("ui.portada_de", "Portada de ") + libro.getTitulo());
        Rectangle clip = new Rectangle(120, 175);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        img.setClip(clip);
        ImageLoader.load(libro.getPortadaURL(), img, 120, 175);

        StackPane contenedorPortada = new StackPane(img);
        contenedorPortada.getStyleClass().add("book-card-cover-container");
        // No es transparente al ratón para que la barra de acciones rápidas sea
        // pulsable; los clics fuera de los botones siguen llegando a la tarjeta.
        contenedorPortada.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        contenedorPortada.setAccessibleText(LanguageManager.getString("ui.portada_de", "Portada de ") + libro.getTitulo());

        // Micro-animación al pasar el ratón: la portada se eleva y crece un
        // poco, sin alterar el layout del FlowPane. Se reutiliza una única
        // transición por tarjeta: antes se creaba una nueva en cada evento y,
        // al entrar y salir rápido, dos animaciones competían y la portada
        // daba saltos.
        TranslateTransition elevar = new TranslateTransition(Duration.millis(160), contenedorPortada);
        javafx.animation.ScaleTransition escalar = new javafx.animation.ScaleTransition(Duration.millis(160), contenedorPortada);
        javafx.animation.ParallelTransition hover = new javafx.animation.ParallelTransition(elevar, escalar);
        elevar.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        escalar.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        tarjeta.hoverProperty().addListener((obs, antes, encima) -> {
            hover.stop();
            elevar.setToY(encima ? -5 : 0);
            escalar.setToX(encima ? 1.03 : 1.0);
            escalar.setToY(encima ? 1.03 : 1.0);
            hover.playFromStart();
        });

        // Badge de estado de lectura en portada (diseño en cápsula con texto completo y borde de alto contraste)
        EstadoLectura estado = libro.getEstadoLecturaEnum();
        Label badge = null;
        if (estado == EstadoLectura.LEIDO) {
            badge = new Label(LanguageManager.getString("catalog.badge.read", "✓ Leído"));
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-leido");
        } else if (estado == EstadoLectura.LEYENDO) {
            badge = new Label(LanguageManager.getString("catalog.badge.reading", "● Leyendo"));
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-leyendo");
        } else if (estado == EstadoLectura.ABANDONADO) {
            badge = new Label(LanguageManager.getString("catalog.badge.abandoned", "✕ Abandonado"));
            badge.getStyleClass().addAll("book-card-badge-pill", "badge-abandonado");
        }

        if (badge != null) {
            badge.setAccessibleRole(AccessibleRole.TEXT);
            badge.setAccessibleText(LanguageManager.getString("ui.estado_de_lectura", "Estado de lectura: ") + estado.getEtiqueta());
            badge.setMouseTransparent(true);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(6, 6, 0, 0));
            contenedorPortada.getChildren().add(badge);
        }

        // Badge de formato digital
        if (libro.isEsDigital()) {
            Label badgeDigital = new Label(LanguageManager.getString("catalog.badge.ebook", "📱 E-book"));
            badgeDigital.setAccessibleRole(AccessibleRole.TEXT);
            badgeDigital.setAccessibleText(LanguageManager.getString("catalog.badge.ebook.a11y", "Formato digital: E-book"));
            badgeDigital.getStyleClass().addAll("book-card-badge-pill", "badge-digital");
            badgeDigital.setMouseTransparent(true);
            StackPane.setAlignment(badgeDigital, Pos.BOTTOM_LEFT);
            StackPane.setMargin(badgeDigital, new Insets(0, 0, 6, 6));
            contenedorPortada.getChildren().add(badgeDigital);
        }

        // Acciones rápidas sobre la portada (aparecen con hover o foco)
        HBox acciones = crearAccionesRapidas(libro, tarjeta);
        StackPane.setAlignment(acciones, Pos.BOTTOM_CENTER);
        contenedorPortada.getChildren().add(acciones);
        javafx.animation.FadeTransition fundidoAcciones = new javafx.animation.FadeTransition(Duration.millis(140), acciones);
        javafx.beans.InvalidationListener mostrarAcciones = obs -> {
            boolean visible = tarjeta.isHover() || tarjeta.isFocused();
            fundidoAcciones.stop();
            if (visible) {
                acciones.setVisible(true);
            }
            fundidoAcciones.setToValue(visible ? 1.0 : 0.0);
            fundidoAcciones.setOnFinished(e -> acciones.setVisible(acciones.getOpacity() > 0));
            fundidoAcciones.playFromStart();
        };
        tarjeta.hoverProperty().addListener(mostrarAcciones);
        tarjeta.focusedProperty().addListener(mostrarAcciones);

        // Título del libro
        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setAccessibleRole(AccessibleRole.TEXT);
        lblTitulo.setAccessibleText(LanguageManager.getString("ui.titulo", "Título: ") + libro.getTitulo());
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
            lblAutor.setAccessibleText(LanguageManager.getString("ui.autor", "Autor: ") + libro.getAutor());
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
            lblRating.setAccessibleText(LanguageManager.getString("ui.calificacion", "Calificación: ") + stars + LanguageManager.getString("ui.de_5_estrellas", " de 5 estrellas"));
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
            pBar.getStyleClass().add("book-card-progress");
            pBar.setMouseTransparent(true);

            int pct = (int) Math.round(progreso * 100);
            pBar.setAccessibleRole(AccessibleRole.PROGRESS_INDICATOR);
            pBar.setAccessibleText(LanguageManager.getString("ui.progreso_de_lectura", "Progreso de lectura: ") + pct + LanguageManager.getString("ui.por_ciento", " por ciento"));

            Label lblProgreso = new Label(libro.getPaginaActual() + " / " + libro.getPaginasTotales() + LanguageManager.getString("ui.pag", " pág. (") + pct + "%)");
            lblProgreso.getStyleClass().add("book-card-progress-label");
            lblProgreso.setAccessibleRole(AccessibleRole.TEXT);
            lblProgreso.setAccessibleText(LanguageManager.getString("ui.progreso", "Progreso: ") + libro.getPaginaActual() + LanguageManager.getString("ui.de", " de ") + libro.getPaginasTotales() + LanguageManager.getString("ui.paginas_leidas", " páginas leídas (") + pct + "%)");
            lblProgreso.setMouseTransparent(true);

            tarjeta.getChildren().addAll(pBar, lblProgreso);
        }

        return tarjeta;
    }

    /**
     * Barra de botones que aparece sobre la portada al pasar el ratón: leer
     * (e-books), marcar como leído, prestar (libros físicos) y «más», que abre
     * el menú contextual completo. Evita tener que abrir el menú o la ficha
     * para las acciones más habituales.
     */
    HBox crearAccionesRapidas(Libro libro, Node tarjeta) {
        HBox barra = new HBox(6);
        barra.getStyleClass().add("book-card-quick-actions");
        barra.setAlignment(Pos.CENTER);
        barra.setMaxHeight(Region.USE_PREF_SIZE);
        barra.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setMargin(barra, new Insets(0, 0, 8, 0));
        barra.setOpacity(0);
        barra.setVisible(false);
        // Los clics en la barra no deben seleccionar ni abrir la ficha (doble clic)
        barra.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        boolean esEbook = libro.isEsDigital() && libro.getRutaArchivoDigital() != null && !libro.getRutaArchivoDigital().isEmpty();
        if (esEbook) {
            barra.getChildren().add(botonAccionRapida("▶", "quick.read", "Leer",
                    () -> mainController.abrirLectorDigital(libro)));
        }
        if (libro.getEstadoLecturaEnum() != EstadoLectura.LEIDO) {
            barra.getChildren().add(botonAccionRapida("✓", "quick.mark_read", "Marcar como leído",
                    () -> cambiarEstadoLectura(libro, EstadoLectura.LEIDO)));
        }
        if (!libro.isEsDigital()) {
            barra.getChildren().add(botonAccionRapida("⇄", "quick.loan", "Prestar",
                    () -> mainController.prepararPrestamoLibro(libro)));
        }
        Button mas = botonAccionRapida("⋯", "quick.more", "Más acciones", null);
        mas.setOnAction(e -> {
            javafx.geometry.Bounds b = mas.localToScreen(mas.getBoundsInLocal());
            if (b != null) {
                mostrarContextMenu(tarjeta, libro, b.getMinX(), b.getMaxY());
            }
        });
        barra.getChildren().add(mas);
        return barra;
    }

    private Button botonAccionRapida(String simbolo, String claveTexto, String textoPorDefecto, Runnable accion) {
        String texto = LanguageManager.getString(claveTexto, textoPorDefecto);
        Button b = new Button(simbolo);
        b.getStyleClass().add("book-card-quick-btn");
        // Fuera del orden de tabulación: con teclado ya hay Intro y el menú contextual,
        // y así las flechas siguen moviéndose entre tarjetas.
        b.setFocusTraversable(false);
        b.setTooltip(new javafx.scene.control.Tooltip(texto));
        b.setAccessibleText(texto);
        if (accion != null) {
            b.setOnAction(e -> {
                if (mainController != null) {
                    accion.run();
                }
            });
        }
        return b;
    }

    private void navegarTarjetaSiguiente(Node actual) {
        if (panelMisLibros == null) {
            return;
        }
        int idx = panelMisLibros.getChildren().indexOf(actual);
        if (idx == panelMisLibros.getChildren().size() - 1) {
            cargarMasContinuo(); // Al llegar al final en modo continuo
        }
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
        } else if (abajo && cargarMasContinuo()) {
            // Última fila en modo continuo: cargar más y bajar cuando se maqueten
            panelMisLibros.applyCss();
            panelMisLibros.layout();
            navegarTarjetaFila(actual, true);
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

        if (seleccion.size() > 1 && seleccion.contains(libro)) {
            contextMenuLibros.getItems().setAll(crearItemsAccionesLote());
            contextMenuLibros.show(owner, screenX, screenY);
            return;
        }

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
        itemEliminar.getStyleClass().add("danger");
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
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(LanguageManager.getString("ui.estado_actualizado_a", "Estado actualizado a ") + nuevoEstado.getEtiqueta() + ": " + libro.getTitulo()));
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
        // Se escucha en el ScrollPane y en la tabla (no solo en el FlowPane),
        // para que soltar en la zona vacía bajo las tarjetas o en la vista de
        // lista también funcione.
        for (Node zona : new Node[]{scrollMisLibros, tablaMisLibros}) {
            if (zona == null) {
                continue;
            }
            zona.setOnDragOver(event -> {
                if (event.getGestureSource() == null && contieneEbooks(event.getDragboard())) {
                    event.acceptTransferModes(TransferMode.COPY);
                    if (!zona.getStyleClass().contains("drop-target-active")) {
                        zona.getStyleClass().add("drop-target-active");
                    }
                }
                event.consume();
            });
            zona.setOnDragExited(event -> {
                zona.getStyleClass().remove("drop-target-active");
                event.consume();
            });
            zona.setOnDragDropped(event -> {
                zona.getStyleClass().remove("drop-target-active");
                Dragboard db = event.getDragboard();
                List<File> ebooks = db.hasFiles()
                        ? db.getFiles().stream().filter(EbookMetadataService::esArchivoEbook).collect(Collectors.toList())
                        : List.of();
                if (!ebooks.isEmpty()) {
                    importarEbooks(ebooks);
                }
                event.setDropCompleted(!ebooks.isEmpty());
                event.consume();
            });
        }
    }

    private static boolean contieneEbooks(Dragboard db) {
        return db.hasFiles() && db.getFiles().stream().anyMatch(EbookMetadataService::esArchivoEbook);
    }

    /**
     * Importa en segundo plano los e-books soltados sobre el catálogo (antes
     * solo se procesaba el primero) y los añade de una vez, omitiendo los que
     * ya existen por título.
     */
    private void importarEbooks(List<File> archivos) {
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(
                LanguageManager.getString("ui.importando_e_book", "Importando e-book...")));

        Thread importThread = new Thread(() -> {
            List<Libro> leidos = new java.util.ArrayList<>();
            for (File archivo : archivos) {
                Libro libro = EbookMetadataService.crearLibroDesdeArchivo(archivo, this.rutaUsuario);
                if (libro != null) {
                    leidos.add(libro);
                }
            }
            Platform.runLater(() -> {
                List<String> duplicados = new java.util.ArrayList<>();
                Libro ultimo = null;
                int importados = 0;
                for (Libro nuevoLibro : leidos) {
                    boolean existe = listaLibrosCompleta.stream()
                            .anyMatch(l -> l.getTitulo() != null && l.getTitulo().equalsIgnoreCase(nuevoLibro.getTitulo()));
                    if (existe) {
                        duplicados.add(nuevoLibro.getTitulo());
                        continue;
                    }
                    nuevoLibro.setId(UUID.randomUUID().toString());
                    listaLibrosCompleta.add(nuevoLibro);
                    ultimo = nuevoLibro;
                    importados++;
                }
                if (importados > 0) {
                    // El controlador principal escucha este evento y guarda en
                    // segundo plano; antes se guardaba aquí en el hilo de la UI.
                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(importados == 1 ? ultimo : null, true));
                    String mensaje = importados == 1
                            ? LanguageManager.getString("ui.e_book_importado", "E-book importado: ") + ultimo.getTitulo()
                            : String.format(LanguageManager.getString("ui.e_books_importados", "%d e-books importados"), importados);
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(mensaje));
                }
                if (!duplicados.isEmpty() && mainController != null) {
                    mainController.mostrarAlertaPublic(
                            LanguageManager.getString("ui.libro_duplicado", "Libro duplicado"),
                            LanguageManager.getString("ui.ya_existe_un_libro_con_el", "Ya existe un libro con el título: ")
                            + String.join(", ", duplicados));
                }
            });
        }, "ImportarEbooks");
        importThread.setDaemon(true);
        importThread.start();
    }
}
