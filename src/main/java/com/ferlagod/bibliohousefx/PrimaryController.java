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

import com.bibliohouse.logic.BusquedaSagas;
import com.bibliohouse.logic.ImportarExportarBD;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.NextCloudSyncService;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.LibroService;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.Socio;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import org.controlsfx.control.NotificationPane;

/**
 * Este es el controlador principal. Aquí manejo la tabla de libros, los
 * préstamos y todo eso. Es como el cerebro de la pantalla principal.
 *
 * @author Fernando Lago Dávila
 * @version 1.9
 */
public class PrimaryController implements Initializable {

    // --- VARIABLES DE DATOS ---
    private JsonManager jsonManager;
    private ResourceBundle resources; // Bundle para i18n
    private ObservableList<Libro> listaLibrosCompleta;
    private ObservableList<Prestamo> listaPrestamosCompleta;
    private FilteredList<Prestamo> filteredPrestamos;
    private FilteredList<Prestamo> filteredHistory;
    private ObservableList<Socio> listaSocios;
    private String usuarioActual;
    private String rutaUsuario; // Para recargar app
    private String rutaPortadaTemporal = "";
    private java.util.Map<String, String> preferencias = new java.util.HashMap<>();
    private FilteredList<Libro> filteredData; // Lista que la tabla usará para filtrar
    private SortedList<Libro> sortedData; // Lista que la tabla usará para ordenar (basada en filteredData)
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(PrimaryController.class.getName());
    private BusquedaSagas busquedaSagas = new BusquedaSagas();
    private ImportarExportarBD gestorArchivos = new ImportarExportarBD();
    private LibroService libroService;
    private com.bibliohouse.logic.BusquedaService busquedaService = new com.bibliohouse.logic.BusquedaService();
    private com.bibliohouse.logic.PrestamoService prestamoService;

    // --- LÍMITE DE PRÉSTAMO ---
    /**
     * Días límite para considerar un préstamo como vencido. Por defecto es 30,
     * pero puede ser configurado por el usuario.
     */
    private int dueDaysLimit = 30;

    // --- CONSTANTES DE VISTA ---
    private static final String VISTA_TODOS = "Todos los libros";
    private static final String VISTA_DESEOS = "Lista de Deseos";
    private static final String VISTA_DIGITAL = "E-books";
    private static final List<String> ESTANTERIAS_DEFAULT = List.of(
            "Novela", "Ciencia Ficción", "Fantasía", "Historia", "Tecnología",
            "Aventura", "Biografía", "Romántica", "Poesía", "Teatro", "Infantil", "Ensayo"
    );

    // --- COMPONENTES FXML ---
    @FXML
    private TableView<Libro> tablaLibros;
    @FXML
    private TableColumn<Libro, String> colTitulo;
    @FXML
    private TableColumn<Libro, String> colAutor;
    @FXML
    private TableColumn<Libro, String> colEditorial;
    @FXML
    private TableColumn<Libro, String> colGenero;
    @FXML
    private TableColumn<Libro, String> colAnio;
    @FXML
    private TableColumn<Libro, String> colEstado;
    @FXML
    private TableColumn<Libro, String> colIsbn;
    @FXML
    private TableColumn<Libro, String> colSerie;
    @FXML
    private TableColumn<Libro, Double> colOrden;
    @FXML
    private TableColumn<Libro, Integer> colCantidad;
    @FXML
    private PrestamosController pestanaPrestamosController;
    @FXML
    private HistorialController pestanaHistorialController;
    // Manual input
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtAutor;
    @FXML
    private TextField txtEditorial;
    @FXML
    private TextField txtSerie;
    @FXML
    private TextField txtOrden;
    @FXML
    private TextField txtGenero;
    @FXML
    private TextField txtIsbn;
    @FXML
    private TextField txtAnio;
    @FXML
    private Spinner<Integer> spinnerCantidad;
    @FXML
    private TextField txtBusquedaLocal;
    @FXML
    private TextField txtFiltroAutor;
    @FXML
    private TextField txtFiltroISBN;
    @FXML
    private ComboBox<String> cmbFiltroEstado;
    @FXML
    private TabPane mainTabPane;
    // Galería eliminada — se usa directamente mainTabPane
    // private Tab tabGaleria; (eliminado)
    // private Tab tabTabla;   (eliminado)
    // private TilePane tilePanePortadas; (eliminado)
    // private ScrollPane scrollPaneGaleria; (eliminado)
    // PORTADA MANUAL
    @FXML
    private ImageView imgPortadaManual;
    @FXML
    private CheckBox chkPoseidoManual;
    // Search OpenLibrary
    @FXML
    private TextField txtBusquedaOpenLibrary;
    // Se movieron al PrestamosController y HistorialController
    // Estado y Lateral
    @FXML
    private Label lblEstado;
    @FXML
    private ListView<String> listaEstanterias;

    // --- WIDGET RETO ANUAL ---
    @FXML
    private javafx.scene.layout.VBox widgetRetoAnual;
    @FXML
    private Label lblTituloReto;
    @FXML
    private javafx.scene.control.ProgressBar progresoReto;
    @FXML
    private Label lblEstadoReto;

    // --- IDIOMAS ---
    @FXML
    private ToggleGroup grupoIdioma;
    @FXML
    private RadioMenuItem menuEs;
    @FXML
    private RadioMenuItem menuEn;
    @FXML
    private RadioMenuItem menuCa;
    @FXML
    private RadioMenuItem menuGl;
    @FXML
    private RadioMenuItem menuEu;
    @FXML
    private RadioMenuItem menuPt;
    @FXML
    private Tab tabPrestamos;
    @FXML
    private Tab tabHistorial;
    @FXML
    private WishlistController pestanaWishlistController;
    private List<Libro> listaDeseos;
    @FXML
    private SagasController pestanaSagasController;
    @FXML
    private javafx.scene.layout.FlowPane panelMisLibros;
    @FXML
    private NotificationPane notificationPane;
    @FXML
    private TextField txtBuscarMisLibros;
    private ContextMenu contextMenuLibros;
    private javafx.animation.PauseTransition searchDelay;

    @FXML
    private void cambiarAEspanol() {
        cambiarIdioma("es");
    }

    @FXML
    private void cambiarAIngles() {
        cambiarIdioma("en");
    }

    @FXML
    private void cambiarACatalan() {
        cambiarIdioma("ca");
    }

    @FXML
    private void cambiarAGallego() {
        cambiarIdioma("gl");
    }

    @FXML
    private void cambiarAEuskera() {
        cambiarIdioma("eu");
    }

    @FXML
    private void cambiarAPortugues() {
        cambiarIdioma("pt");
    }

    /**
     * Inicializa y configura todos los componentes de la interfaz al abrir la
     * ventana. Este método se encarga de vincular las columnas de las tablas
     * con sus respectivos modelos de datos, configurar el menú contextual para
     * la tabla de libros, establecer listeners para filtros dinámicos,
     * inicializar el spinner de cantidad y los atajos de teclado, resaltar
     * préstamos vencidos en la tabla de préstamos activos, y manejar errores
     * críticos durante la inicialización.
     *
     * @param url Ubicación del archivo FXML (no utilizado directamente,
     * requerido por {@link Initializable}).
     * @param rb ResourceBundle para internacionalización (i18n).
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        try {
            this.resources = rb;

            configurarColumnasLibros();

            // Placeholder para la tabla de libros principal
            Label placeholderLibros = new Label("La tabla está vacía. Añade libros o cambia los filtros.");
            placeholderLibros.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            tablaLibros.setPlaceholder(placeholderLibros);

            configurarContextMenu();
            configurarFiltros();
            configurarAtajosYEventos();
            configurarDragAndDropPanelMisLibros();

            if (mainTabPane != null) {
                mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                    if (newTab != null && newTab.getContent() != null) {
                        Node content = newTab.getContent();

                        // MEJORA DE SUAVIDAD: Activamos la caché de hardware antes de la animación
                        content.setCache(true);
                        content.setCacheHint(javafx.scene.CacheHint.SPEED);

                        // Configuramos la transición de desvanecimiento
                        FadeTransition fade = new FadeTransition(Duration.millis(300), content);
                        fade.setFromValue(0.0);
                        fade.setToValue(1.0);
                        fade.setCycleCount(1);
                        fade.setAutoReverse(false);

                        // Al terminar la animación, desactivamos la caché para liberar memoria de video
                        fade.setOnFinished(e -> {
                            content.setCache(false);
                            content.setCacheHint(javafx.scene.CacheHint.DEFAULT);
                        });

                        // Pequeño efecto de desplazamiento hacia arriba (Slide + Fade)
                        content.setTranslateY(10);
                        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(300), content);
                        slide.setFromY(10);
                        slide.setToY(0);
                        slide.play();

                        fade.play();
                    }
                });
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Error CRÍTICO en initialize", e);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Inicialización");
                alert.setHeaderText("Fallo al iniciar la pantalla principal");
                alert.setContentText("Ocurrió un error inesperado al configurar la vista: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * Configura las columnas de la tabla de libros.
     */
    private void configurarColumnasLibros() {
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colEditorial.setCellValueFactory(new PropertyValueFactory<>("editorial"));
        colGenero.setCellValueFactory(new PropertyValueFactory<>("genero"));
        colAnio.setCellValueFactory(new PropertyValueFactory<>("año"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoLectura"));
        colSerie.setCellValueFactory(new PropertyValueFactory<>("serie"));
        colOrden.setCellValueFactory(new PropertyValueFactory<>("ordenEnSerie"));
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
    }

    /**
     * Configura el menú contextual de la tabla de libros.
     */
    private void configurarContextMenu() {
        this.contextMenuLibros = new ContextMenu();

        MenuItem itemPrestar = new MenuItem("Prestar este libro");
        itemPrestar.setOnAction(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prepararPrestamoLibro(selected);
            }
        });

        MenuItem itemLeer = new MenuItem("📖 Leer E-book");
        itemLeer.setOnAction(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isEsDigital() && selected.getRutaArchivoDigital() != null && !selected.getRutaArchivoDigital().isEmpty()) {
                File archivo = new File(selected.getRutaArchivoDigital());
                if (archivo.exists()) {
                    try {
                        java.awt.Desktop.getDesktop().open(archivo);
                    } catch (IOException ex) {
                        mostrarAlerta("Error", "No se pudo abrir el archivo.");
                    }
                } else {
                    mostrarAlerta("Error", "El archivo ya no existe en la ruta guardada:\n" + archivo.getAbsolutePath());
                }
            } else {
                mostrarAlerta("Información", "Este libro no tiene un archivo digital asociado.");
            }
        });

        MenuItem itemEditar = new MenuItem(resources.getString("ctx.edit"));
        itemEditar.setOnAction(e -> editarLibroSeleccionado(null));

        MenuItem itemPortada = new MenuItem(resources.getString("ctx.cover"));
        itemPortada.setOnAction(e -> cambiarPortadaDesdePrincipal(null));

        // --- SUBMENÚ: MARCAR ESTADO DE LECTURA ---
        Menu menuEstado = new Menu("Marcar como...");

        MenuItem itemPendiente = new MenuItem("📋 Pendiente");
        itemPendiente.setOnAction(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setEstadoLectura("Pendiente");
                jsonManager.guardarLibrosDebounced(listaLibrosCompleta);
                tablaLibros.refresh();
                actualizarPanelMisLibros();
                lblEstado.setText("Estado actualizado: Pendiente — " + selected.getTitulo());
            }
        });

        MenuItem itemLeyendo = new MenuItem("📖 Leyendo");
        itemLeyendo.setOnAction(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setEstadoLectura("Leyendo");
                jsonManager.guardarLibrosDebounced(listaLibrosCompleta);
                tablaLibros.refresh();
                actualizarPanelMisLibros();
                lblEstado.setText("Estado actualizado: Leyendo — " + selected.getTitulo());
            }
        });

        MenuItem itemLeido = new MenuItem("✅ Leído");
        itemLeido.setOnAction(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setEstadoLectura("Leído");
                jsonManager.guardarLibrosDebounced(listaLibrosCompleta);
                tablaLibros.refresh();
                actualizarPanelMisLibros();
                lblEstado.setText("Estado actualizado: Leído — " + selected.getTitulo());
            }
        });

        menuEstado.getItems().addAll(itemPendiente, itemLeyendo, itemLeido);
        // -----------------------------------------

        MenuItem itemEliminar = new MenuItem(resources.getString("ctx.delete"));
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarLibro(null));

        contextMenuLibros.getItems().addAll(itemLeer, new SeparatorMenuItem(), itemPrestar, new SeparatorMenuItem(), itemEditar, itemPortada,
                menuEstado, new SeparatorMenuItem(), itemEliminar);

        contextMenuLibros.setOnShowing(e -> {
            Libro selected = tablaLibros.getSelectionModel().getSelectedItem();
            boolean isEbook = (selected != null && selected.isEsDigital() && selected.getRutaArchivoDigital() != null && !selected.getRutaArchivoDigital().isEmpty());
            itemLeer.setVisible(isEbook);
            // Hide the separator after itemLeer if itemLeer is not visible
            contextMenuLibros.getItems().get(1).setVisible(isEbook);
        });

        tablaLibros.setContextMenu(this.contextMenuLibros);
    }

    /**
     * Configura el spinner de cantidad, filtros, row factories y atajos.
     */
    private void configurarFiltros() {
        if (spinnerCantidad != null) {
            spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        }

        updateLanguageMenuSelection();

        // Doble click para detalles
        tablaLibros.setRowFactory(tv -> {
            TableRow<Libro> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mostrarDetalleLibro(row.getItem());
                }
            });
            return row;
        });

        // Listener de Estanterías (Filtro)
        if (listaEstanterias != null) {
            listaEstanterias.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    actualizarFiltros();
                }
            });
        }

        // Búsqueda incremental por Título (con debounce para evitar rebuilds excesivos)
        if (txtBusquedaLocal != null) {
            PauseTransition filterDelayTitulo = new PauseTransition(Duration.millis(200));
            filterDelayTitulo.setOnFinished(e -> actualizarFiltros());
            txtBusquedaLocal.textProperty().addListener((observable, oldValue, newValue) -> filterDelayTitulo.playFromStart());
        }

        if (txtFiltroAutor != null) {
            PauseTransition filterDelayAutor = new PauseTransition(Duration.millis(200));
            filterDelayAutor.setOnFinished(e -> actualizarFiltros());
            txtFiltroAutor.textProperty().addListener((observable, oldValue, newValue) -> filterDelayAutor.playFromStart());
        }

        if (txtFiltroISBN != null) {
            PauseTransition filterDelayISBN = new PauseTransition(Duration.millis(200));
            filterDelayISBN.setOnFinished(e -> actualizarFiltros());
            txtFiltroISBN.textProperty().addListener((observable, oldValue, newValue) -> filterDelayISBN.playFromStart());
        }

        if (cmbFiltroEstado != null) {
            cmbFiltroEstado.setItems(FXCollections.observableArrayList("Todos", "Leído", "Leyendo", "Pendiente"));
            cmbFiltroEstado.setValue("Todos");
            cmbFiltroEstado.valueProperty().addListener((obs, old, newVal) -> actualizarFiltros());
        }

        if (txtBuscarMisLibros != null) {
            searchDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
            searchDelay.setOnFinished(event -> actualizarPanelMisLibros());

            txtBuscarMisLibros.textProperty().addListener((observable, oldValue, newValue) -> {
                searchDelay.playFromStart(); // Reinicia el cronómetro con cada letra
            });
        }
    }

    /**
     * Configura atajos de teclado globales.
     */
    private void configurarAtajosYEventos() {
        setupShortcuts();
    }

    /**
     * Obtiene el nombre del usuario actual.
     *
     * @return El nombre del usuario logueado.
     */
    public String getUsuarioActual() {
        return this.usuarioActual;
    }

    /**
     * Obtiene la ruta de datos del usuario actual.
     *
     * @return La ruta absoluta a la carpeta de datos.
     */
    public String getRutaUsuario() {
        return this.rutaUsuario;
    }

    /**
     * Inicializa los datos del usuario después del login. Carga preferencias,
     * libros, préstamos y socios.
     *
     * @param username Nombre del usuario.
     * @param userPath Ruta a la carpeta de datos del usuario.
     */
    public void initData(String username, String userPath) {
        this.usuarioActual = username;
        this.rutaUsuario = userPath;

        // 1. Inicializar servicios y gestores
        String coversPath = this.rutaUsuario + java.io.File.separator + "covers";
        com.bibliohouse.utils.ImageLoader.setCacheDir(coversPath);

        this.jsonManager = new JsonManager(userPath);

        // 2. Cargar datos maestros (ahora es asíncrono)
        cargarDatos();

        // 3. Sincronización NextCloud con seguridad mejorada
        this.preferencias = jsonManager.cargarPreferencias();
        String ncUrl = preferencias.getOrDefault("nextcloud.url", "");
        String ncUser = preferencias.getOrDefault("nextcloud.user", "");

        java.util.prefs.Preferences osPrefs = java.util.prefs.Preferences.userRoot()
                .node("com/ferlagod/bibliohousefx/nextcloud");

        String ncPassEncriptada = osPrefs.get("password", "");
        String ncPass = com.bibliohouse.utils.SeguridadUtil.desencriptar(ncPassEncriptada);

        if (!ncUrl.isBlank() && !ncUser.isBlank() && !ncPass.isBlank()) {
            try {
                NextCloudSyncService syncService = new NextCloudSyncService(ncUrl, ncUser, ncPass);
                final String localDir = userPath;
                jsonManager.setAutoSyncTask(() -> {
                    try {
                        syncService.subirBaseDatos(localDir);
                    } catch (IOException ex) {
                        LOGGER.log(java.util.logging.Level.WARNING, "Auto-sync fallido: {0}", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Error al iniciar servicio de sincronización");
            }
        }

        // 4. Configuración de interfaz y estado
        aplicarPreferenciasGuardadas();

        if (resources != null) {
            lblEstado.setText(java.text.MessageFormat.format(resources.getString("status.welcome"), username) + " (Cargando biblioteca...)");
        } else {
            lblEstado.setText("Bienvenido, " + username + " (Cargando biblioteca...)");
        }

        // 6. Actualizaciones en segundo plano
        com.bibliohouse.utils.UpdateChecker.comprobarActualizaciones(versionNueva -> {
            Platform.runLater(() -> notificar(
                    "✨ Nueva versión disponible: BiblioHouse " + versionNueva
                    + ". ¡Visita la web para descargarla!"));
        });
    }

    /**
     * Comprueba los préstamos activos y notifica si hay vencidos (más de
     * DUE_DAYS_LIMIT).
     */
    private void checkOverdueLoans() {
        if (prestamoService == null) {
            return;
        }
        List<Prestamo> overdueLoans = prestamoService.obtenerPrestamosVencidos(dueDaysLimit);

        if (!overdueLoans.isEmpty()) {
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            delay.setOnFinished(e -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Se han detectado ").append(overdueLoans.size()).append(" préstamos vencidos:\n\n");

                overdueLoans.stream().limit(5).forEach(p -> {
                    long daysOverdue = prestamoService.calcularDiasRetraso(p, dueDaysLimit);
                    sb.append("• ").append(p.getTituloLibro()).append(" (Socio #").append(p.getNumeroSocio())
                            .append("): ").append(daysOverdue).append(" días de retraso.\n");
                });

                if (overdueLoans.size() > 5) {
                    sb.append("\n... y ").append(overdueLoans.size() - 5)
                            .append(" más. Consulta la pestaña 'Préstamos'.");
                }

                Alert alert = new Alert(Alert.AlertType.WARNING);
                if (tablaLibros.getScene() != null && tablaLibros.getScene().getWindow() != null) {
                    alert.initOwner(tablaLibros.getScene().getWindow());
                }
                alert.setTitle("⚠️ ATENCIÓN: Préstamos Vencidos");
                alert.setHeaderText("¡Tienes libros pendientes de devolución!");
                alert.setContentText(sb.toString());

                ButtonType viewLoansButton = new ButtonType("Ver Préstamos", ButtonBar.ButtonData.OK_DONE);
                ButtonType dismissButton = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(viewLoansButton, dismissButton);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == viewLoansButton) {
                    if (tabPrestamos != null) {
                        mainTabPane.getSelectionModel().select(tabPrestamos);
                    }
                }
            });
            delay.play();
        }
    }

    /**
     * Actualiza la selección del menú de idiomas basándose en el locale actual
     * de la aplicación. Marca el RadioMenuItem correspondiente como
     * seleccionado.
     */
    private void updateLanguageMenuSelection() {
        java.util.Locale current = App.getCurrentLocale();
        String lang = current.getLanguage();

        if (grupoIdioma != null) {
            if (lang.equals("en") && menuEn != null) {
                grupoIdioma.selectToggle(menuEn);
            } else if (lang.equals("ca") && menuCa != null) {
                grupoIdioma.selectToggle(menuCa);
            } else if (lang.equals("gl") && menuGl != null) {
                grupoIdioma.selectToggle(menuGl);
            } else if (lang.equals("eu") && menuEu != null) {
                grupoIdioma.selectToggle(menuEu);
            } else if (lang.equals("pt") && menuPt != null) {
                grupoIdioma.selectToggle(menuPt);
            } else if (menuEs != null) {
                grupoIdioma.selectToggle(menuEs); // Default ES
            }
        }
    }

    /**
     * Muestra la ventana del Manual de Usuario.
     *
     * @param event Evento que dispara la acción.
     */
    @FXML
    private void mostrarAyudaManual(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("help.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            Stage stage = new Stage();
            String title = resources.containsKey("help.title") ? resources.getString("help.title")
                    : "Manual de Usuario";
            stage.setTitle(title);
            setScene(stage, root);

            // Icono
            stage.getIcons()
                    .add(new javafx.scene.image.Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

            stage.initModality(Modality.NONE); // Ventana no modal, permite seguir usando la app
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar el Manual de Ayuda.");
        }
    }

    /**
     * Configura los atajos de teclado globales para la escena principal.
     */
    private void setupShortcuts() {
        Platform.runLater(() -> {
            Scene scene = tablaLibros.getScene();
            if (scene != null) {
                // Ctrl+F (Cmd+F) -> Foco en búsqueda (Tab "Gestionar Libros" = índice 1)
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+F"),
                        () -> {
                            mainTabPane.getSelectionModel().select(1);
                            if (txtBusquedaOpenLibrary != null) {
                                txtBusquedaOpenLibrary.requestFocus();
                            }
                        });

                // Ctrl+N (Cmd+N) -> Foco en añadir manual (Tab "Gestionar Libros" = índice 1)
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+N"),
                        () -> {
                            mainTabPane.getSelectionModel().select(1);
                            if (txtTitulo != null) {
                                txtTitulo.requestFocus();
                            }
                        });

                // Ctrl+L (Cmd+L) -> Pestaña Préstamos (por referencia, no por índice)
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+L"),
                        () -> {
                            if (tabPrestamos != null) {
                                mainTabPane.getSelectionModel().select(tabPrestamos);
                            }
                        });
            }
        });
    }

    /**
     * Maneja el cambio de idioma desde el menú. Guarda la preferencia,
     * actualiza el locale de la aplicación y recarga la interfaz (Hot-Swap)
     * para aplicar los cambios inmediatamente.
     *
     * @param event El evento de acción disparado por el RadioMenuItem.
     */
    private void cambiarIdioma(String codigoLang) {
        try {
            // 1. Guardamos la preferencia de idioma en el ordenador
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
            prefs.put("language", codigoLang);

            // 2. Llamamos al método mágico de App para recargar todo al instante
            // Le pasamos el usuario y la ruta de datos que tenemos ahora mismo
            Stage currentStage = (Stage) tablaLibros.getScene().getWindow();
            App.reloadUI(currentStage, this.usuarioActual, this.rutaUsuario);

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cambiar el idioma correctamente.");
        }
    }

    /**
     * Aplica las preferencias guardadas (solo estado de la ventana). Se ejecuta
     * después de que la interfaz gráfica esté lista.
     */
    private void aplicarPreferenciasGuardadas() {
        try {
            String diasStr = preferencias.getOrDefault("dias_prestamo", "30");
            this.dueDaysLimit = Integer.parseInt(diasStr);
        } catch (NumberFormatException e) {
            this.dueDaysLimit = 30;
        }
    }

    /**
     * Establece el número de días límite para los préstamos. Actualiza la
     * variable y refresca la tabla de préstamos si es necesario.
     *
     * @param days Número de días.
     */
    public void setDueDaysLimit(int days) {
        this.dueDaysLimit = days;
        // Guardar preferencia
        if (preferencias != null) {
            preferencias.put("dias_prestamo", String.valueOf(days));
            jsonManager.guardarPreferencias(preferencias);
        }

        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.initData(this, prestamoService, obtenerLibrosDisponibles(), listaSocios, filteredPrestamos, resources, dueDaysLimit);
        }
    }

    /**
     * Obtiene el límite de días de préstamo actual.
     *
     * @return Días límite.
     */
    public int getDueDaysLimit() {
        return dueDaysLimit;
    }

    /**
     * Carga todos los datos necesarios para la aplicación desde los archivos
     * JSON. Incluye libros, socios, préstamos y estanterías.
     */
    private void cargarDatos() {
        javafx.concurrent.Task<Void> loadTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 1. Lectura I/O pesada en segundo plano
                List<Libro> deseos = jsonManager.cargarDeseos();
                List<Libro> libros = jsonManager.cargarLibros();
                List<Socio> socios = jsonManager.cargarSocios();
                List<Prestamo> prestamos = jsonManager.cargarPrestamos();

                // 2. Actualización de UI en el hilo principal
                Platform.runLater(() -> {
                    listaDeseos = deseos;
                    if (pestanaWishlistController != null) {
                        pestanaWishlistController.initData(PrimaryController.this, jsonManager, listaDeseos, resources);
                    }

                    listaLibrosCompleta = FXCollections.observableArrayList(libros);
                    libroService = new LibroService(jsonManager, listaLibrosCompleta);

                    filteredData = new FilteredList<>(listaLibrosCompleta, p -> true);
                    sortedData = new SortedList<>(filteredData);

                    sortedData.setComparator(PrimaryController.this::compareBySeries);
                    tablaLibros.comparatorProperty().addListener((obs, oldComp, newComp) -> {
                        if (newComp == null) {
                            sortedData.setComparator(PrimaryController.this::compareBySeries);
                        } else {
                            sortedData.setComparator(newComp);
                        }
                    });

                    tablaLibros.setItems(sortedData);

                    listaSocios = FXCollections.observableArrayList(socios);

                    listaPrestamosCompleta = FXCollections.observableArrayList(prestamos);
                    prestamoService = new com.bibliohouse.logic.PrestamoService(jsonManager, listaPrestamosCompleta, listaLibrosCompleta);

                    filteredPrestamos = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() == null);
                    filteredHistory = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() != null);

                    if (pestanaPrestamosController != null) {
                        pestanaPrestamosController.initData(PrimaryController.this, prestamoService, obtenerLibrosDisponibles(), listaSocios, filteredPrestamos, resources, dueDaysLimit);
                    }
                    if (pestanaHistorialController != null) {
                        pestanaHistorialController.initData(PrimaryController.this, filteredHistory);
                    }
                    if (pestanaSagasController != null) {
                        pestanaSagasController.initData(listaLibrosCompleta, () -> {
                            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                            tablaLibros.refresh();
                            actualizarFiltros();
                        });
                    }

                    cargarListaEstanterias();
                    actualizarFiltros();

                    if (resources != null) {
                        lblEstado.setText(java.text.MessageFormat.format(resources.getString("status.welcome"), usuarioActual));
                    } else {
                        lblEstado.setText("Bienvenido, " + usuarioActual);
                    }

                    checkOverdueLoans();
                });
                return null;
            }
        };
        Thread thread = new Thread(loadTask, "CargaDatosInicial");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Abre un diálogo para seleccionar un archivo CSV y, si es válido, importa
     * los libros a la biblioteca.
     *
     * Muestra un mensaje de confirmación antes de añadir los libros y evita
     * duplicados comparando por ISBN, título o autor.
     *
     * @param event El evento que desencadena la acción.
     */
    @FXML
    private void importarDesdeCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar biblioteca desde CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File archivoSeleccionado = fileChooser.showOpenDialog(tablaLibros.getScene().getWindow());

        if (archivoSeleccionado != null) {
            // Llamamos a nuestra nueva clase para procesar el archivo
            List<Libro> librosImportados = com.bibliohouse.logic.ImportadorCSV.importar(archivoSeleccionado);

            if (librosImportados == null) {
                mostrarAlerta("Error de importación", "No se pudo leer el archivo CSV. Asegúrate de que tenga formato correcto (debe incluir cabeceras como Título y Autor).");
                return;
            }

            if (librosImportados.isEmpty()) {
                mostrarAlerta("Sin datos", "El archivo CSV parece estar vacío o no contiene libros válidos.");
                return;
            }

            // Confirmar antes de añadir
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Importación completada");
            alert.setHeaderText("Se han encontrado " + librosImportados.size() + " libros en el archivo.");
            alert.setContentText("¿Deseas añadirlos a tu biblioteca actual?\n(Se omitirán los que ya existan con el mismo ISBN).");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                int añadidos = 0;
                for (Libro nuevo : librosImportados) {
                    // Verificación simple de duplicados por ISBN
                    boolean existe = false;
                    if (nuevo.getIsbn() != null && !nuevo.getIsbn().isEmpty()) {
                        existe = listaLibrosCompleta.stream().anyMatch(l -> nuevo.getIsbn().equals(l.getIsbn()));
                    } else {
                        // Si no tiene ISBN, comprobamos por título y autor
                        existe = listaLibrosCompleta.stream().anyMatch(l
                                -> l.getTitulo().equalsIgnoreCase(nuevo.getTitulo()) && l.getAutor().equalsIgnoreCase(nuevo.getAutor())
                        );
                    }

                    if (!existe) {
                        listaLibrosCompleta.add(nuevo);
                        añadidos++;
                    }
                }

                // Guardar y refrescar interfaz
                guardarYNotificar("Se han añadido " + añadidos + " libros desde el CSV.");
                actualizarComboLibrosDisponibles();

                if (añadidos < librosImportados.size()) {
                    mostrarAlerta("Información", añadidos + " libros añadidos. Se han omitido " + (librosImportados.size() - añadidos) + " porque ya existían en tu biblioteca.");
                }
            }
        }
    }

    /**
     * Configura el ComboBox para que se pueda filtrar escribiendo texto. He
     * modificado este método para solucionar el error de que no salían los
     * libros nuevos. La idea es que cada vez que escribes, buscamos en la lista
     * actualizada en vez de usar la antigua.
     *
     * @param <T> El tipo de objeto del combo.
     * @param comboBox El combo que vamos a configurar.
     * @param displayFunc La función para saber qué texto mostrar de cada
     * objeto.
     */
    public <T> void setupFilteringComboBoxPublic(ComboBox<T> comboBox, java.util.function.Function<T, String> displayFunc) {
        setupFilteringComboBox(comboBox, displayFunc);
    }

    @SuppressWarnings("unchecked")
    private <T> void setupFilteringComboBox(ComboBox<T> comboBox, java.util.function.Function<T, String> displayFunc) {
        if (comboBox == null) {
            return;
        }

        // Hacemos que se pueda escribir en el combo
        comboBox.setEditable(true);

        // Guardamos una copia de los items originales por si acaso
        ObservableList<T> originalItems = FXCollections.observableArrayList(comboBox.getItems());

        // Este listener salta si cambia la lista de items desde fuera
        comboBox.itemsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != originalItems) {
                // De momento no hace falta hacer nada especial aquí
            }
        });

        // Aquí es donde controlamos lo que pasa cuando el usuario escribe
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {

            // IMPORTANTE: Elegimos qué lista usar como fuente para filtrar.
            // Para los libros usamos la lista 'listaLibrosCompleta' directamente,
            // porque si usamos 'originalItems' no salen los libros que acabamos de añadir.
            ObservableList<T> sourceList;
            if (comboBox.getId() != null && comboBox.getId().equals("comboLibrosPrestamo")) {
                // Solo queremos libros que tengan stock (cantidad > 0)
                sourceList = FXCollections.observableArrayList();
                for (Object o : listaLibrosCompleta) {
                    Libro l = (Libro) o;
                    if (l.getCantidad() > 0) {
                        sourceList.add((T) l);
                    }
                }
            } else if (comboBox.getId() != null && comboBox.getId().equals("comboSocios")) {
                // Para socios usamos la lista de socios actual
                sourceList = (ObservableList<T>) listaSocios;
            } else {
                // Para otros combos usamos la lista original guardada
                sourceList = originalItems;
            }

            if (newText == null || newText.isEmpty()) {
                comboBox.setItems(sourceList);
                return;
            }

            // Si el texto coincide con lo que ya hemos seleccionado, no filtramos de nuevo
            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null && displayFunc.apply(selected).equals(newText)) {
                return;
            }

            // Filtramos la lista buscando coincidencias (ignorando mayúsculas/minúsculas)
            FilteredList<T> filtered = new FilteredList<>(sourceList, item -> {
                String itemText = displayFunc.apply(item).toLowerCase();
                return itemText.contains(newText.toLowerCase());
            });

            // Actualizamos los items del combo con los resultados filtrados
            comboBox.setItems(filtered);

            // Si hay resultados y el combo no está desplegado, lo abrimos para que se vea
            if (!filtered.isEmpty() && !comboBox.isShowing()) {
                Platform.runLater(() -> comboBox.show());
            }
        });

        // StringConverter para mostrar el nombre correctamente
        comboBox.setConverter(new javafx.util.StringConverter<T>() {
            /**
             * Convierte el objeto en texto para mostrarlo en el desplegable.
             *
             * @param object El objeto a mostrar.
             * @return El texto correspondiente.
             */
            @Override
            public String toString(T object) {
                if (object == null) {
                    return null;
                }
                return displayFunc.apply(object);
            }

            /**
             * Convierte el texto de vuelta en el objeto original (útil para
             * autocompletar o pegar texto).
             *
             * @param string El texto a buscar.
             * @return El objeto correspondiente, o null si no se encuentra.
             */
            @Override
            public T fromString(String string) {
                // Seleccionar el item que coincida con el string
                return comboBox.getItems().stream()
                        .filter(item -> displayFunc.apply(item).equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    /**
     * Compara dos libros basándose en su Serie y Orden. Si no tienen serie, se
     * comparan alfabéticamente por título. Sagas van primero.
     */
    private int compareBySeries(Libro l1, Libro l2) {
        String s1 = l1.getSerie();
        String s2 = l2.getSerie();

        // Normalizar nulos a vacíos para comparar
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }

        // 1. Comparar Nombre de Serie
        int serieCompare = s1.compareToIgnoreCase(s2);

        if (serieCompare != 0) {
            // Si una tiene serie y la otra no (vacía), la que tiene serie va antes?
            // O alfanumérico normal: "" viene antes de "Cosmere".
            // Si queremos Sagas PRIMERO (incluso antes que libros sueltos que empiezan por
            // A),
            // necesitamos lógica extra. Por ahora alfabético está bien:
            // " " (sin serie) vs "Cosmere". Cosmere irá DESPUÉS.
            // SI QUEREMOS AGRUPAR VISUALMENTE:
            if (s1.isEmpty() && !s2.isEmpty()) {
                return 1; // Sin serie AL FINAL
            }
            if (!s1.isEmpty() && s2.isEmpty()) {
                return -1; // Con serie AL PRINCIPIO
            }
            return serieCompare;
        }

        // 2. Misma Serie (o ambas vacías): Comparar por Orden
        if (!s1.isEmpty()) {
            return Double.compare(l1.getOrdenEnSerie(), l2.getOrdenEnSerie());
        }

        // 3. Ni serie ni orden: Por Título (null-safe)
        String t1 = l1.getTitulo() != null ? l1.getTitulo() : "";
        String t2 = l2.getTitulo() != null ? l2.getTitulo() : "";
        return t1.compareToIgnoreCase(t2);
    }

    /**
     * Esta función filtra la tabla. Si escribes algo en los cuadros de
     * búsqueda, aquí se decide qué libros se ven. Es un filtro acumulativo
     * (TIENE que cumplir todo).
     */
    private void actualizarFiltros() {
        if (filteredData == null || listaEstanterias == null) {
            return;
        }

        // Obtener valores de los filtros
        final String filtroTitulo = txtBusquedaLocal != null ? txtBusquedaLocal.getText().toLowerCase().trim() : "";
        final String filtroAutor = txtFiltroAutor != null ? txtFiltroAutor.getText().toLowerCase().trim() : "";
        final String filtroISBN = txtFiltroISBN != null ? txtFiltroISBN.getText().toLowerCase().trim() : "";
        final String categoriaSeleccionada = listaEstanterias.getSelectionModel().getSelectedItem();
        final String estadoFiltro = cmbFiltroEstado != null ? cmbFiltroEstado.getValue() : "Todos";

        filteredData.setPredicate(libro -> {

            // --- FILTRO 1: Estantería y Propiedad ---
            if (categoriaSeleccionada != null) {
                if (categoriaSeleccionada.equals(VISTA_DESEOS)) {
                    // Filtro especial para lista de deseos (isPoseido = false)
                    if (libro.isPoseido()) {
                        return false;
                    }
                } else if (categoriaSeleccionada.equals(VISTA_DIGITAL)) {
                    // Filtro especial para E-books
                    if (!libro.isPoseido() || !libro.isEsDigital()) {
                        return false;
                    }
                } else {
                    if (!libro.isPoseido()) {
                        return false;
                    }

                    if (!categoriaSeleccionada.equals(VISTA_TODOS)) {
                        if (libro.getEstanterias() == null || !libro.getEstanterias().contains(categoriaSeleccionada)) {
                            return false;
                        }
                    }
                }
            }

            // --- FILTRO 2: Estado de Lectura ---
            if (estadoFiltro != null && !estadoFiltro.equals("Todos")) {
                if (libro.getEstadoLectura() == null || !libro.getEstadoLectura().equals(estadoFiltro)) {
                    return false;
                }
            }

            // --- FILTRO 3: Título ---
            if (!filtroTitulo.isEmpty()) {
                if (libro.getTitulo() == null || !libro.getTitulo().toLowerCase().contains(filtroTitulo)) {
                    return false;
                }
            }

            // --- FILTRO 4: Autor ---
            if (!filtroAutor.isEmpty()) {
                if (libro.getAutor() == null || !libro.getAutor().toLowerCase().contains(filtroAutor)) {
                    return false;
                }
            }

            // --- FILTRO 5: ISBN ---
            if (!filtroISBN.isEmpty()) {
                if (libro.getIsbn() == null || !libro.getIsbn().replace("-", "").contains(filtroISBN)) {
                    return false;
                }
            }

            return true;
        });

        if (lblEstado != null && filteredData != null && listaLibrosCompleta != null) {
            lblEstado.setText("Mostrando " + filteredData.size() + " de " + listaLibrosCompleta.size() + " libros.");
        }

        actualizarPanelMisLibros();
    }

    /**
     * Prepara la pestaña de préstamos con el libro seleccionado.
     *
     * @param libro El libro a prestar.
     */
    public void prepararPrestamoLibro(Libro libro) {
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }

        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.seleccionarLibro(libro);
        }
    }

    /**
     * Prepara la pestaña de préstamos con el socio seleccionado.
     *
     * @param socio El socio al que prestar.
     */
    public void prepararPrestamoSocio(Socio socio) {
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }

        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.seleccionarSocio(socio);
        }
    }

    /**
     * Refresca el desplegable de libros disponibles en la pestaña de préstamos.
     * Útil en lugar de llamar a initData() completo, para no resetear el estado
     * del formulario de préstamos entero.
     */
    public void actualizarComboLibrosDisponibles() {
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.refrescarLibrosDisponibles(obtenerLibrosDisponibles());
        }
    }

    private ObservableList<Libro> obtenerLibrosDisponibles() {
        return listaLibrosCompleta.stream()
                .filter(l -> l.getCantidad() > 0)
                .collect(java.util.stream.Collectors.toCollection(FXCollections::observableArrayList));
    }

    /**
     * Muestra una ventana modal con información sobre la aplicación
     * BiblioHouse.
     *
     * @param event El evento de acción que desencadena la apertura de la
     * ventana.
     * @throws IOException Si ocurre un error al cargar el archivo FXML
     * "acercade.fxml". En caso de error, se muestra una alerta al usuario.
     */
    @FXML
    private void mostrarAcercaDe(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("acercade.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            // Asumo que el AcercaDeController no necesita datos, solo abrir la ventana.
            Stage stage = new Stage();
            stage.setTitle("Acerca de BiblioHouse");
            setScene(stage, root);
            // Bloqueamos la ventana principal hasta que se cierre.
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar la ventana 'Acerca de'.");
        }
    }

    /**
     * Carga la lista de estanterías en el ListView lateral. Añade la opción
     * "Todos los libros" al principio.
     */
    private void cargarListaEstanterias() {
        if (listaEstanterias == null) {
            return;
        }
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

        // Personalizamos la celda para añadir iconos
        listaEstanterias.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    // Añadir iconos para opciones especiales
                    if (item.equals(VISTA_TODOS)) {
                        setGraphic(new Label("📚"));
                    } else if (item.equals(VISTA_DESEOS)) {
                        setGraphic(new Label("⭐"));
                    } else if (item.equals(VISTA_DIGITAL)) {
                        setGraphic(new Label("📱"));
                    } else {
                        setGraphic(new Label("📁"));
                    }
                }
            }
        });

        listaEstanterias.getSelectionModel().select(0);
    }

    /**
     * Permite seleccionar manualmente una imagen de portada desde el sistema de
     * archivos.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void seleccionarImagenManual(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Portada");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(tablaLibros.getScene().getWindow());

        if (file != null) {
            try {
                // Guardamos la ruta
                rutaPortadaTemporal = file.getAbsolutePath();

                // Usamos ImageLoader para cargar la imagen (soporta local y background)
                // Solicitamos tamaño reducido para mejorar rendimiento
                com.bibliohouse.utils.ImageLoader.load(file.getAbsolutePath(), imgPortadaManual, 140, 200);

            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo cargar la imagen.");
            }
        }
    }

    /**
     * Añade un nuevo libro a la biblioteca. Recoge los datos del formulario
     * manual, comprueba duplicados y guarda el libro.
     *
     * @param event El evento del botón Añadir.
     */
    @FXML
    private void anadirLibro(ActionEvent event) {
        String titulo = txtTitulo.getText().trim();
        String autor = txtAutor.getText().trim();
        String isbn = txtIsbn.getText().trim();
        int cantidad = spinnerCantidad.getValue();
        boolean poseido = chkPoseidoManual.isSelected();

        if (titulo.isEmpty() || autor.isEmpty()) {
            mostrarAlerta("Datos incompletos", "El título y el autor son obligatorios.");
            return;
        }

        String serie = txtSerie.getText().trim();
        double orden = 0.0;
        try {
            if (!txtOrden.getText().isEmpty()) {
                orden = Double.parseDouble(txtOrden.getText().replace(",", "."));
            }
        } catch (NumberFormatException e) {
            // Ignorar error si no es un número válido, se queda en 0.0
        }

        // 1. BUSCAR SI YA EXISTE (Por ISBN o por Título+Autor)
        Libro libroExistente = null;
        for (Libro l : listaLibrosCompleta) {
            // Coincidencia por ISBN (si ambos tienen ISBN)
            String libroIsbn = l.getIsbn() != null ? l.getIsbn() : "";
            if (!isbn.isEmpty() && !libroIsbn.isEmpty() && libroIsbn.equals(isbn)) {
                libroExistente = l;
                break;
            }
            // Coincidencia por Título y Autor (null-safe)
            String libroTitulo = l.getTitulo() != null ? l.getTitulo() : "";
            String libroAutor = l.getAutor() != null ? l.getAutor() : "";
            if (libroTitulo.equalsIgnoreCase(titulo) && libroAutor.equalsIgnoreCase(autor)) {
                libroExistente = l;
                break;
            }
        }

        // 2. SI EXISTE, PREGUNTAR QUÉ HACER
        if (libroExistente != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Libro duplicado detectado");
            alert.setHeaderText("Ya tienes este libro: " + libroExistente.getTitulo());
            alert.setContentText("¿Qué quieres hacer?");

            ButtonType btnSumar = new ButtonType("Sumar al stock (+1)");
            ButtonType btnNuevo = new ButtonType("Añadir como copia separada");
            ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btnSumar, btnNuevo, btnCancelar);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == btnSumar) {
                    // OPCIÓN A: SUMAR
                    libroExistente.setCantidad(libroExistente.getCantidad() + cantidad);
                    tablaLibros.refresh();
                    guardarYNotificar("Stock actualizado: " + libroExistente.getTitulo());
                    actualizarComboLibrosDisponibles(); // Actualizar combo de préstamos
                    limpiarCamposManuales();
                    return; // Terminamos aquí
                } else if (result.get() == btnNuevo) {
                    // OPCIÓN B: AÑADIR NUEVO (Seguimos con el flujo normal de abajo)
                } else {
                    // CANCELAR
                    return;
                }
            }
        }

        // 3. FLUJO NORMAL (Crear nuevo)
        Libro nuevoLibro = new Libro(
                titulo,
                autor,
                txtEditorial.getText(),
                txtAnio.getText(),
                txtGenero.getText(),
                isbn,
                rutaPortadaTemporal);
        nuevoLibro.setCantidad(cantidad);
        nuevoLibro.setPoseido(poseido);

        // Asignar Serie y Orden
        if (!serie.isEmpty()) {
            nuevoLibro.setSerie(serie);
            nuevoLibro.setOrdenEnSerie(orden);
        }

        // ======================================================================
        // SECUESTRAR LA PORTADA PARA GUARDARLA EN LOCAL (MODO OFFLINE)
        // ======================================================================
        String rutaLocal = com.bibliohouse.utils.ImageLoader.hacerPortadaLocalOffline(
                nuevoLibro.getPortadaURL(),
                nuevoLibro.getId(),
                this.rutaUsuario);
        nuevoLibro.setPortadaURL(rutaLocal);
        // ======================================================================

        // --- SELECCIÓN DE ESTANTERÍA ---
        // Cargamos las estanterías existentes
        List<String> allShelves = jsonManager.cargarEstanterias();
        if (allShelves == null) {
            allShelves = new ArrayList<>();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("asignar_estanteria.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            AsignarEstanteriaController controller = loader.getController();

            // Pasamos las estanterías disponibles y lista vacía para las "actuales" (es
            // libro nuevo)
            controller.setEstanterias(allShelves, new ArrayList<>());

            Stage stage = new Stage();
            stage.setTitle("Asignar Estantería");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.showAndWait();

            if (controller.isConfirmado()) {
                List<String> selectedShelves = controller.getResultado();
                nuevoLibro.setEstanterias(selectedShelves);

                // Verificar si se crearon estanterías nuevas y guardarlas en la lista global
                boolean hayNuevas = false;
                for (String shelf : selectedShelves) {
                    if (!allShelves.contains(shelf)) {
                        allShelves.add(shelf);
                        hayNuevas = true;
                    }
                }

                if (hayNuevas) {
                    jsonManager.guardarEstanterias(allShelves);
                    cargarListaEstanterias(); // Refrescar el sidebar
                }
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la ventana de asignación de estanterías.");
        }

        listaLibrosCompleta.add(nuevoLibro);
        guardarYNotificar("Libro añadido: " + titulo);
        actualizarComboLibrosDisponibles(); // Actualizar combo de préstamos
        limpiarCamposManuales();
    }

    /**
     * Guarda la lista de libros en JSON y notifica al usuario en la barra de
     * estado.
     *
     * @param mensaje Mensaje a mostrar en la barra de estado.
     */
    private void guardarYNotificar(String mensaje) {
        guardarLibrosEnDisco();
        lblEstado.setText(mensaje);
        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta);
        }
    }

    /**
     * Centraliza la copia defensiva y el guardado para evitar inconsistencias.
     * Usa debounce para evitar bloqueos.
     */
    private void guardarLibrosEnDisco() {
        jsonManager.guardarLibrosDebounced(new ArrayList<>(listaLibrosCompleta));
    }

    /**
     * Limpia los campos del formulario de añadir libro manual.
     */
    @FXML
    private void limpiarCamposManuales() {
        txtTitulo.clear();
        txtAutor.clear();
        txtEditorial.clear();
        txtGenero.clear();
        txtIsbn.clear();
        txtAnio.clear();
        txtSerie.clear();
        txtOrden.clear();
        spinnerCantidad.getValueFactory().setValue(1);
        chkPoseidoManual.setSelected(true);

        // Limpiamos también la imagen
        imgPortadaManual.setImage(null);
        rutaPortadaTemporal = "";
    }

    /**
     * Abre una ventana modal con los resultados de la búsqueda en OpenLibrary.
     *
     * @param resultados Lista de libros encontrados.
     */
    private void abrirVentanaResultados(List<Libro> resultados) {
        try {
            LOGGER.log(Level.FINE, "[PrimaryController] Cargando FXML de resultados_busqueda.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resultados_busqueda.fxml"));

            if (loader.getLocation() == null) {
                LOGGER.log(Level.SEVERE, "[PrimaryController] No se pudo encontrar el archivo resultados_busqueda.fxml");
                mostrarAlerta("Error", "No se encontró el archivo de la ventana de resultados.");
                return;
            }

            loader.setResources(this.resources);
            Parent root = loader.load();

            ResultadosBusquedaController controller = loader.getController();
            controller.setResultados(resultados);

            Stage stage = new Stage();
            stage.setTitle("Resultados de búsqueda");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            stage.showAndWait();

            // Recoger el libro seleccionado al cerrar
            Libro elegido = controller.getLibroSeleccionado();
            if (elegido != null) {
                if (controller.isParaDeseos()) {
                    // SE PULSÓ "AÑADIR A DESEOS"
                    elegido.setPoseido(false);
                    String rutaLocal = com.bibliohouse.utils.ImageLoader.hacerPortadaLocalOffline(
                            elegido.getPortadaURL(), elegido.getId(), this.rutaUsuario);
                    elegido.setPortadaURL(rutaLocal);

                    listaDeseos.add(elegido);
                    jsonManager.guardarDeseos(listaDeseos);
                    if (pestanaWishlistController != null) {
                        pestanaWishlistController.actualizarPanelDeseos();
                    }
                    lblEstado.setText("Añadido a tu Lista de Deseos: " + elegido.getTitulo());
                } else {
                    // SE PULSÓ "IMPORTAR A BIBLIOTECA"
                    rellenarFormularioManual(elegido);
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] IOException al abrir ventana de resultados", e);
            mostrarAlerta("Error", "No se pudo abrir la ventana de resultados: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Excepción inesperada al abrir ventana de resultados", e);
            mostrarAlerta("Error", "Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Rellena el formulario de añadir libro con los datos de un libro
     * seleccionado.
     *
     * @param libro El libro con los datos a cargar.
     */
    private void rellenarFormularioManual(Libro libro) {
        // Rellenar campos de texto
        txtTitulo.setText(libro.getTitulo());
        txtAutor.setText(libro.getAutor());
        txtEditorial.setText(libro.getEditorial());
        txtGenero.setText(libro.getGenero());
        txtIsbn.setText(libro.getIsbn());
        txtAnio.setText(libro.getAño());
        if (libro.getSerie() != null) {
            txtSerie.setText(libro.getSerie());
        }
        txtOrden.setText(String.valueOf(libro.getOrdenEnSerie()));

        // Gestionar la imagen de portada
        String urlPortada = libro.getPortadaURL();
        // Guardamos la URL como "ruta temporal" (la original de alta calidad)
        rutaPortadaTemporal = urlPortada;

        // Usamos ImageLoader en lugar de cargar directamente.
        // Optimizamos cargando una versión pequeña (Thumbnail) para la vista previa
        // Si es de OpenLibrary y es la versión Large, usamos Medium para el preview
        String urlPreview = urlPortada;
        if (urlPreview != null && urlPreview.contains("covers.openlibrary.org") && urlPreview.endsWith("-L.jpg")) {
            urlPreview = urlPreview.replace("-L.jpg", "-M.jpg");
        }

        com.bibliohouse.utils.ImageLoader.load(urlPreview, imgPortadaManual, 140, 200);

        // Poner foco en el botón de añadir para agilizar
        lblEstado.setText("Libro seleccionado. Revisa los datos y pulsa Añadir.");
    }

    // --- GESTIÓN DE EDICIÓN, ELIMINACIÓN Y PORTADA ---
    /**
     * Abre la ventana de edición para el libro seleccionado en la tabla.
     *
     * @param event El evento del menú contextual o botón.
     */
    @FXML
    private void editarLibroSeleccionado(ActionEvent event) {
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();
        if (libroSeleccionado == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editar_libro.fxml"));

            loader.setResources(this.resources);

            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(libroSeleccionado);
            controller.setEstanteriasDisponibles(jsonManager.cargarEstanterias());
            controller.setRutaUsuario(this.rutaUsuario); // (Asegúrate de tener también esta línea que pusimos antes)

            Stage stage = new Stage();
            stage.setTitle("Editar: " + libroSeleccionado.getTitulo());
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setMaximized(true);
            stage.showAndWait();

            if (controller.isGuardado()) {
                tablaLibros.refresh();
                guardarLibrosEnDisco();
                cargarListaEstanterias();
                actualizarFiltros();
                actualizarPanelMisLibros();
                if (pestanaSagasController != null) {
                    pestanaSagasController.initData(listaLibrosCompleta);
                }
                lblEstado.setText("Libro editado correctamente.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Error al abrir ventana de edición", e);
        }
    }

    /**
     * Permite cambiar la portada del libro seleccionado directamente desde la
     * pantalla principal.
     *
     * @param event El evento del menú contextual.
     */
    @FXML
    private void cambiarPortadaDesdePrincipal(ActionEvent event) {
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();
        if (libroSeleccionado == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Portada para: " + libroSeleccionado.getTitulo());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(tablaLibros.getScene().getWindow());
        if (file != null) {
            libroSeleccionado.setPortadaURL(file.getAbsolutePath());
            guardarLibrosEnDisco();
            tablaLibros.refresh();
            mostrarAlerta("Éxito", "Portada actualizada.");
        }
    }

    /**
     * Elimina el libro seleccionado de la biblioteca. Si hay múltiples copias,
     * pregunta si borrar una unidad o todo el registro.
     *
     * @param event El evento del botón Eliminar.
     */
    @FXML
    private void eliminarLibro(ActionEvent event) {
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();
        if (libroSeleccionado == null) {
            mostrarAlerta("Ningún libro seleccionado", "Por favor, selecciona un libro de la tabla para eliminarlo.");
            return;
        }

        if (libroSeleccionado.getCantidad() > 1) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Gestión de Stock");
            alert.setHeaderText("Tienes " + libroSeleccionado.getCantidad() + " copias de este libro.");
            alert.setContentText("¿Qué deseas hacer?");
            ButtonType btnEliminarUno = new ButtonType("Eliminar solo 1 unidad");
            ButtonType btnEliminarTodo = new ButtonType("Borrar el libro entero");
            ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnEliminarUno, btnEliminarTodo, btnCancelar);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnEliminarUno) {
                    libroService.actualizarStock(libroSeleccionado, -1);
                    lblEstado.setText("Se ha eliminado una copia. Quedan: " + libroSeleccionado.getCantidad());
                } else if (result.get() == btnEliminarTodo) {
                    libroService.borrarTotalmente(libroSeleccionado);
                    lblEstado.setText("Libro eliminado definitivamente: " + libroSeleccionado.getTitulo());
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Eliminar Libro");
            alert.setHeaderText("¿Estás seguro de que quieres borrar este libro?");
            alert.setContentText("Vas a eliminar: " + libroSeleccionado.getTitulo() + "\nEsta acción no se puede deshacer.");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                libroService.borrarTotalmente(libroSeleccionado);
                lblEstado.setText("Libro eliminado definitivamente: " + libroSeleccionado.getTitulo());
            }
        }

        // Refresco general de UI tras las operaciones de borrado o stock
        tablaLibros.refresh();
        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta);
        }
        actualizarPanelMisLibros();
    }

    // --- MENÚ ARCHIVO ---
    /**
     * Importa libros desde un archivo JSON externo. Ofrece la opción de añadir
     * a los existentes o reemplazar toda la base de datos.
     *
     * @param event El evento del menú.
     */
    @FXML
    private void importarBaseDatos(ActionEvent event) {
        gestorArchivos.importarBaseDatos(tablaLibros.getScene().getWindow(), jsonManager, listaLibrosCompleta, () -> {
            // Este bloque se ejecuta solo si la importación tiene éxito
            cargarListaEstanterias();
            actualizarComboLibrosDisponibles();
            tablaLibros.refresh();
            actualizarFiltros();
            mostrarAlerta("Importación completa", "La biblioteca se ha actualizado correctamente.");
        });
    }

    /**
     * Exporta la base de datos actual a un archivo JSON de respaldo.
     *
     * @param event El evento del menú.
     */
    @FXML
    private void exportarBaseDatos(ActionEvent event) {
        gestorArchivos.exportarBaseDatos(tablaLibros.getScene().getWindow(), jsonManager, listaLibrosCompleta);
    }

    /**
     * Abre la ventana de exportación de informes en PDF. Permite filtrar libros
     * y generar un informe personalizado.
     *
     * @param event El evento del menú.
     */
    @FXML
    private void exportarPDF(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exportar_pdf.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            ExportarPDFController controller = loader.getController();

            // Preparar datos: libros, estanterías y géneros
            List<Libro> libros = new ArrayList<>(listaLibrosCompleta);
            List<String> estanterias = jsonManager.cargarEstanterias();

            // Extraer géneros únicos de los libros
            List<String> generos = libros.stream()
                    .map(Libro::getGenero)
                    .filter(g -> g != null && !g.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            controller.setDatos(libros, estanterias, generos);

            Stage stage = new Stage();
            stage.setTitle("Exportar Informe PDF");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            // Forzar el tamaño mínimo en el Stage para Linux
            stage.setMinWidth(820);
            stage.setMinHeight(720);

            stage.showAndWait();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la ventana de exportación PDF.\n" + e.getMessage());
        }
    }

    /**
     * Abre la ventana de exportación de catálogo web HTML. Permite filtrar
     * libros y generar un catálogo estático con diseño Netflix.
     *
     * @param event El evento del menú.
     */
    @FXML
    private void exportarWeb(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exportar_web.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            ExportarWebController controller = loader.getController();

            // Preparar datos: libros, estanterías y géneros
            List<Libro> libros = new ArrayList<>(listaLibrosCompleta);
            List<String> estanterias = jsonManager.cargarEstanterias();

            // Extraer géneros únicos de los libros
            List<String> generos = libros.stream()
                    .map(Libro::getGenero)
                    .filter(g -> g != null && !g.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            controller.setDatos(libros, estanterias, generos);

            Stage stage = new Stage();
            stage.setTitle("Exportar Catálogo Web");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            stage.setMinWidth(820);
            stage.setMinHeight(720);

            stage.showAndWait();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la ventana de exportación web.\n" + e.getMessage());
        }
    }

    /**
     * Cierra la aplicación de forma segura, guardando preferencias antes de
     * salir.
     *
     * @param event El evento del menú Salir.
     */
    @FXML
    private void abrirEscaner(ActionEvent event) {
        LOGGER.log(Level.INFO, "[PrimaryController] Solicitud para abrir escáner recibida.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("escaner.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            EscanerController escanerController = loader.getController();
            escanerController.setListener(isbns -> {
                if (isbns != null && !isbns.isEmpty()) {
                    String ultimoIsbn = isbns.get(isbns.size() - 1);

                    txtIsbn.setText(ultimoIsbn);

                    Platform.runLater(() -> {
                        if (isbns.size() > 1) {
                            Alert info = new Alert(Alert.AlertType.INFORMATION);
                            info.setTitle("Modo Ráfaga Completado");
                            info.setHeaderText("Se han escaneado " + isbns.size() + " libros.");
                            info.setContentText(
                                    "Códigos: " + isbns.toString() + "\n\n(Mostrando el último en la búsqueda)");
                            info.showAndWait();
                        }

                        txtBusquedaOpenLibrary.setText(ultimoIsbn);
                        buscarLibroOpenLibrary(null);
                    });
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Escáner de Código de Barras");
            setScene(stage, root);
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setOnShown(e -> escanerController.init());
            stage.setOnCloseRequest(e -> escanerController.shutdown());

            stage.show();
            LOGGER.log(Level.INFO, "[PrimaryController] Ventana de escáner visible.");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Error IO al abrir escáner", e);
            mostrarAlerta("Error", "No se pudo abrir el escáner: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Error general al abrir escáner", e);
            mostrarAlerta("Error", "Error inesperado al abrir escáner: " + e.getMessage());
        }
    }

    @FXML
    private void cerrarAplicacion(ActionEvent event) {
        // 1. Obtener la Stage actual de forma robusta
        Stage mainStage = (Stage) tablaLibros.getScene().getWindow();

        // 2. Guardar el estado maximizado
        boolean isMaximized = mainStage.isMaximized();
        preferencias.put("maximized", String.valueOf(isMaximized));

        // 3. Guardar las preferencias en disco
        jsonManager.guardarPreferencias(preferencias);

        // 4. Salir de la plataforma
        Platform.exit();
    }

    // --- HERRAMIENTAS ---
    /**
     * Busca y fusiona libros duplicados en la base de datos. Compara por ISBN y
     * por coincidencia de Título + Autor.
     *
     * @param event El evento del menú Herramientas.
     */
    @FXML
    private void buscarDuplicados(ActionEvent event) {
        String reporte = libroService.buscarYFusionarDuplicados();

        if (reporte == null) {
            mostrarAlerta("Búsqueda de Duplicados", "No se encontraron libros repetidos.");
            return;
        }

        tablaLibros.refresh();
        actualizarPanelMisLibros();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("duplicados.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            com.ferlagod.bibliohousefx.DuplicadosController controller = loader.getController();
            controller.setTextoResultados(reporte);

            Stage stage = new Stage();
            stage.setTitle("Informe de Duplicados");
            setScene(stage, root);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Éxito", "Proceso completado. Revisa la tabla.");
        }
    }

    /**
     * Abre la ventana de configuración de la aplicación.
     *
     * @param event El evento del botón Configuración.
     */
    @FXML
    private void abrirConfiguracion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("configuracion.fxml"));
            loader.setResources(
                    java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages", App.getCurrentLocale()));
            Parent root = loader.load();

            ConfiguracionController controller = loader.getController();
            // Pasamos el jsonManager y "this" (el propio controlador principal)
            controller.initData(jsonManager, this);

            Stage stage = new Stage();
            stage.setTitle("Configuración");

            setScene(stage, root);

            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setResizable(false);

            stage.showAndWait();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la configuración.");
        }
    }

    /**
     * Abre la ventana de estadísticas de la biblioteca.
     *
     * @param event El evento del botón Estadísticas.
     */
    @FXML
    private void mostrarEstadisticas(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("estadisticas.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            EstadisticasController controller = loader.getController();
            controller.calcularEstadisticas(new ArrayList<>(listaLibrosCompleta));

            Stage stage = new Stage();
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setMinWidth(750);
            stage.setMinHeight(600);
            stage.sizeToScene();
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error de estadísticas", "No se pudo abrir la ventana de estadísticas.");
        }
    }

    // --- PRÉSTAMOS Y SOCIOS ---
    /**
     * Abre la ventana para registrar un nuevo socio.
     *
     */
    public void nuevoSocioPublic() {
        nuevoSocio(null);
    }

    /**
     * Abre una ventana modal para registrar un nuevo socio en el sistema.
     *
     * Este método carga la interfaz gráfica definida en
     * {@code gestion_socios.fxml}, inicializa el controlador
     * {@link GestionSociosController} con la lista actual de socios y muestra
     * la ventana como un diálogo modal bloqueante.
     *
     * @param event el evento de acción .
     *
     * @throws IOException si falla la carga del archivo FXML
     * {@code gestion_socios.fxml}, en cuyo caso se muestra una alerta de error
     * crítico al usuario.
     *
     */
    @FXML
    private void nuevoSocio(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gestion_socios.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            GestionSociosController controller = loader.getController();
            controller.setListaSocios(listaSocios);

            Stage stage = new Stage();
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.showAndWait();

            Socio nuevo = controller.getSocioCreado();
            if (nuevo != null) {
                listaSocios.add(nuevo);
                jsonManager.guardarSocios(listaSocios);
                mostrarAlerta("Éxito", "Socio añadido.");
            }
        } catch (IOException e) {
            mostrarAlerta("Error Crítico", "No se pudo abrir la ventana de Nuevo Socio.\n" + e.getMessage());
        }
    }

    /**
     * Abre la ventana de gestión de socios para editar/eliminar.
     */
    public void gestionarSociosPublic() {
        gestionarSocios(null);
    }

    /**
     * Abre una ventana modal para la gestión avanzada de la lista de socios.
     *
     * Este método carga la interfaz definida en {@code socios_manager.fxml} e
     * inicializa el controlador {@link SociosManagerController} pasando la
     * lista actual de socios, la instancia del gestor JSON
     * ({@link JsonManager}) y una referencia al controlador actual.
     *
     * @param event el evento de acción.
     *
     * @throws IOException si ocurre un error al cargar el archivo FXML
     * {@code socios_manager.fxml}, en cuyo caso se captura la excepción y se
     * muestra una alerta de error crítico.
     *
     */
    @FXML
    private void gestionarSocios(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("socios_manager.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            SociosManagerController controller = loader.getController();

            // Le pasamos la lista de socios, el JsonManager y la referencia a sí mismo
            controller.initData(listaSocios, jsonManager, this);

            Stage stage = new Stage();
            stage.setTitle("Gestionar Socios");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.showAndWait();

        } catch (IOException e) {
            mostrarAlerta("Error Crítico", "No se pudo abrir la ventana de Gestión de Socios.\n" + e.getMessage());
        }
    }

    /**
     * Método público para forzar la recarga de datos que afectan a la pestaña
     * Préstamos (Socios y Combo de Libros) desde controladores secundarios.
     */
    public void recargarDatosPrestamos() {
        List<Socio> socios = jsonManager.cargarSocios();
        listaSocios.clear();
        listaSocios.addAll(socios);
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.initData(this, prestamoService, obtenerLibrosDisponibles(), listaSocios, filteredPrestamos, resources, dueDaysLimit);
        }
    }

    /**
     * Permite al SociosManagerController acceder a la lista de préstamos
     * activos para verificar si un socio puede ser eliminado.
     *
     * @return listado de prestamos completo
     */
    public ObservableList<Prestamo> getListaPrestamos() {
        return listaPrestamosCompleta;
    }

    /**
     * Muestra una alerta con el título y mensaje especificados.
     *
     * @param titulo Título de la alerta.
     * @param mensaje Mensaje de la alerta.
     */
    public void mostrarAlertaPublic(String titulo, String mensaje) {
        mostrarAlerta(titulo, mensaje);
    }

    /**
     * Establece el mensaje de estado en la interfaz.
     *
     * @param mensaje Mensaje a mostrar.
     */
    public void setMensajeEstado(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
        }
    }

    /**
     * Actualiza las vistas relacionadas con préstamos. Refresca los filtros de
     * préstamos activos e histórico, y actualiza la lista de libros
     * disponibles.
     */
    public void actualizarVistasPrestamo() {
        // Refrescar filtros de préstamos
        if (filteredPrestamos != null) {
            filteredPrestamos.setPredicate(p -> p.getFechaDevolucion() == null);
        }
        if (filteredHistory != null) {
            filteredHistory.setPredicate(p -> p.getFechaDevolucion() != null);
        }
        // Actualizar combo de libros disponibles
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.refrescarLibrosDisponibles(obtenerLibrosDisponibles());
        }
        // Refrescar tabla principal de libros
        if (tablaLibros != null) {
            tablaLibros.refresh();
        }
    }

    /**
     * Obtiene la lista completa de libros.
     *
     * @return Lista observable de todos los libros.
     */
    public ObservableList<Libro> getListaLibrosCompleta() {
        return listaLibrosCompleta;
    }

    /**
     * Obtiene la lista completa de socios.
     *
     * @return Lista observable de todos los socios.
     */
    public ObservableList<Socio> getListaSocios() {
        return listaSocios;
    }

    // --- DETALLES ---
    /**
     * Muestra la ventana de detalles de un libro.
     *
     * @param libro El libro del cual mostrar detalles.
     */
    private void mostrarDetalleLibro(Libro libro) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("detalle_libro.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            DetalleLibroController controller = loader.getController();
            controller.setLibro(libro);
            controller.setListaGlobalEstanterias(jsonManager.cargarEstanterias());
            controller.setRutaUsuario(this.rutaUsuario);
            controller.setOnSyncRequested(() -> {
                guardarLibrosEnDisco();
                tablaLibros.refresh();
                actualizarFiltros();
                actualizarPanelMisLibros();
            });

            Stage stage = new Stage();
            stage.setTitle("Detalles: " + libro.getTitulo());
            setScene(stage, root);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setMaximized(true);
            stage.showAndWait();

            if (controller.isModified()) {
                guardarLibrosEnDisco();
                tablaLibros.refresh();
                actualizarFiltros();
                actualizarPanelMisLibros();
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrimaryController] Error al abrir detalle del libro", e);
        }
    }

    /**
     * Muestra una alerta informativa al usuario.
     *
     * @param titulo Título de la alerta.
     * @param mensaje Contenido del mensaje.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    /**
     * Helper method to create a Scene with default styles and set it to the
     * Stage.
     *
     * @param stage The stage to set the scene on.
     * @param root The root node for the scene.
     */
    private void setScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setScene(scene);

        // La solución  para que Ubuntu no encoja las ventanas
        stage.sizeToScene();
    }

    // ==========================================
    // SECCIÓN: LISTA DE DESEOS (WISHLIST)
    // ==========================================
    /**
     * Mueve un libro de la lista de deseos a la biblioteca principal.
     *
     * @param libro libro de la biblioteca principal
     */
    public void moverDeseoABiblioteca(Libro libro) {
        // 1. Quitar de deseos
        listaDeseos.remove(libro);
        jsonManager.guardarDeseos(listaDeseos);

        // 2. Añadir a la biblioteca principal
        libro.setPoseido(true);
        libro.setCantidad(1);
        listaLibrosCompleta.add(libro);
        guardarLibrosEnDisco();

        // 3. Refrescar vistas
        tablaLibros.refresh();
        if (pestanaWishlistController != null) {
            pestanaWishlistController.actualizarPanelDeseos();
        }
        actualizarComboLibrosDisponibles();
        lblEstado.setText(resources.getString("wishlist.moved.status"));
    }

    /**
     * Busca un libro en varios proveedores (OpenLibrary, Google Books,
     * Inventaire) de forma asíncrona.
     *
     * @param query El texto a buscar (título, autor o ISBN).
     */
    private void ejecutarBusquedaGlobal(String query) {
        if (resources != null && resources.containsKey("status.searching")) {
            lblEstado.setText(resources.getString("status.searching"));
        } else {
            lblEstado.setText("Buscando...");
        }
        txtBusquedaOpenLibrary.setDisable(true);

        busquedaService.ejecutarBusquedaGlobalAsync(query)
                .thenAccept(resultados -> Platform.runLater(() -> {
            txtBusquedaOpenLibrary.setDisable(false);
            if (resultados.isEmpty()) {
                mostrarAlerta("Sin resultados", "No se encontraron libros.");
            } else {
                abrirVentanaResultados(resultados);
            }
        }))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error en búsqueda global de libros", ex);
                    Platform.runLater(() -> {
                        txtBusquedaOpenLibrary.setDisable(false);
                        mostrarAlerta("Error de red", "No se pudo completar la búsqueda: " + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Busca un libro usando el texto del campo de búsqueda. Desactiva el campo
     * mientras busca.
     */
    @FXML
    private void buscarLibroOpenLibrary(ActionEvent event) {
        txtBusquedaOpenLibrary.setDisable(true);
        String query = txtBusquedaOpenLibrary.getText().trim();
        if (query.isEmpty()) {
            txtBusquedaOpenLibrary.setDisable(false);
            return;
        }
        ejecutarBusquedaGlobal(query);
    }

    /**
     * Pide al usuario qué libro buscar y lo añade a la lista de deseos.
     */
    @FXML
    public void buscarLibroParaDeseos() {
        // En la pestaña de deseos pedimos al usuario qué quiere buscar mediante un diálogo
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Buscar Libro");
        dialog.setHeaderText("Añadir a Lista de Deseos");
        dialog.setContentText("Introduce el título, autor o ISBN:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            ejecutarBusquedaGlobal(result.get().trim());
        }
    }

    /**
     * Busca y descarga portadas para los libros que no tienen imagen asignada.
     * Muestra un diálogo de confirmación y una barra de progreso durante la
     * búsqueda.
     *
     * @param event El evento que desencadena la acción.
     */
    @FXML
    private void buscarPortadasFaltantes(ActionEvent event) {
        // 1. Filtrar libros que realmente necesiten portada
        List<Libro> librosSinPortada = listaLibrosCompleta.stream()
                .filter(l -> l.getPortadaURL() == null || l.getPortadaURL().isEmpty() || l.getPortadaURL().contains("default_cover"))
                .collect(Collectors.toList());

        if (librosSinPortada.isEmpty()) {
            javafx.application.Platform.runLater(() -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Búsqueda de Portadas");
                info.setHeaderText("Búsqueda finalizada");
                info.setContentText(resources.getString("status.no_covers_pending"));
                info.showAndWait();
            });
            return;
        }

        // 2. Confirmación previa
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Búsqueda masiva");
        confirmacion.setHeaderText("Se van a procesar " + librosSinPortada.size() + " libros.");
        confirmacion.setContentText("Este proceso conectará con servidores externos. ¿Deseas continuar?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        // 3. Crear la Tarea (Task) para segundo plano
        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                int actualizadas = 0;
                for (int i = 0; i < librosSinPortada.size(); i++) {
                    if (isCancelled()) {
                        break;
                    }

                    Libro libro = librosSinPortada.get(i);

                    // Actualizar barra de progreso y mensaje informativo
                    updateMessage("Buscando: " + libro.getTitulo());
                    updateProgress(i + 1, librosSinPortada.size());

                    // Lógica de búsqueda (ISBN primero, luego Título)
                    String query = (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) ? libro.getIsbn() : libro.getTitulo();
                    String urlEncontrada = buscarImagenEnApisMasivo(query);

                    if (urlEncontrada.isEmpty() && libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
                        urlEncontrada = buscarImagenEnApisMasivo(libro.getTitulo());
                    }

                    if (!urlEncontrada.isEmpty()) {
                        String rutaLocal = com.bibliohouse.utils.ImageLoader.hacerPortadaLocalOffline(urlEncontrada, libro.getId(), rutaUsuario);
                        libro.setPortadaURL(rutaLocal);
                        actualizadas++;
                    }

                    // Pequeña pausa para ser respetuosos con las APIs y permitir ver el progreso
                    Thread.sleep(300);
                }
                return actualizadas;
            }
        };

        // 4. Mostrar el diálogo de progreso de ControlsFX
        org.controlsfx.dialog.ProgressDialog progressDialog = new org.controlsfx.dialog.ProgressDialog(task);
        progressDialog.setTitle("BiblioHouse - Descarga de Portadas");
        progressDialog.setHeaderText("Procesando colección...");
        progressDialog.initOwner(tablaLibros.getScene().getWindow());

        // 5. Qué hacer cuando termine con éxito
        task.setOnSucceeded(e -> {
            guardarLibrosEnDisco();
            tablaLibros.refresh();
            actualizarPanelMisLibros();
            if (pestanaWishlistController != null) {
                pestanaWishlistController.actualizarPanelDeseos();
            }
            javafx.application.Platform.runLater(() -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Búsqueda de Portadas");
                info.setHeaderText("Búsqueda finalizada");
                info.setContentText("¡Completado! Se han actualizado " + task.getValue() + " portadas.");
                info.showAndWait();
            });
        });

        // 6. Qué hacer si falla
        task.setOnFailed(e -> {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error en descarga masiva", task.getException());
            mostrarAlerta("Error", "Ocurrió un error durante la descarga masiva.");
        });

        // Lanzar en un hilo nuevo
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Busca la portada de un libro en APIs externas usando el servicio de
     * búsqueda masiva.
     *
     * @param query Término de búsqueda (ISBN o título).
     * @return URL de la portada encontrada, o cadena vacía si no se encuentra.
     */
    private String buscarImagenEnApisMasivo(String query) {
        return busquedaService.buscarImagenEnApisMasivo(query);
    }

    /**
     * Busca y asigna sagas a los libros que no tienen serie asignada. Muestra
     * un diálogo de confirmación y una barra de progreso durante la búsqueda.
     *
     */
    @FXML
    private void buscarSagasFaltantes(javafx.event.ActionEvent event) {
        busquedaSagas.buscarSagasFaltantes(listaLibrosCompleta, jsonManager, () -> {
            tablaLibros.refresh();

            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            } else {
                LOGGER.log(Level.WARNING, "[PrimaryController] pestanaSagasController es null. Revisa el fx:id en primary.fxml");
            }

            actualizarFiltros();
        });
    }

    /**
     * Dibuja la cuadrícula de libros en la pestaña principal, ordenados de la A
     * a la Z. Respeta los filtros del menú lateral y de búsqueda.
     */
    private void actualizarPanelMisLibros() {
        if (panelMisLibros == null || filteredData == null) {
            return;
        }

        actualizarRetoAnual();

        // 1. Obtener lo que el usuario ha escrito en el nuevo buscador
        String busquedaRapida = txtBuscarMisLibros != null ? txtBuscarMisLibros.getText().toLowerCase().trim() : "";

        // 2. Filtrar los libros — filteredData ya aplica el predicado de estantería/deseos
        List<Libro> librosMostrados = filteredData.stream()
                .filter(l -> {
                    if (busquedaRapida.isEmpty()) {
                        return true;
                    }
                    boolean tituloCoincide = l.getTitulo() != null && l.getTitulo().toLowerCase().contains(busquedaRapida);
                    boolean autorCoincide = l.getAutor() != null && l.getAutor().toLowerCase().contains(busquedaRapida);
                    return tituloCoincide || autorCoincide;
                })
                .sorted(java.util.Comparator.comparing(
                        l -> l.getTitulo() != null ? l.getTitulo() : "",
                        String::compareToIgnoreCase))
                .collect(Collectors.toList());

        // 3. Comprobar si está vacío ANTES de dibujar
        if (librosMostrados.isEmpty()) {
            javafx.scene.layout.VBox emptyState = new javafx.scene.layout.VBox(12);
            emptyState.setAlignment(javafx.geometry.Pos.CENTER);
            emptyState.setStyle("-fx-padding: 60 0 60 0;");

            Label lblIcon = new Label("📚");
            lblIcon.setStyle("-fx-font-size: 48px;");

            Label lblVacio = new Label("Tu biblioteca está vacía aquí");
            lblVacio.setStyle("-fx-text-fill: -color-fg-default; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label lblSub = new Label("Prueba a cambiar el filtro de estantería\no añade libros nuevos desde «Gestionar Libros»");
            lblSub.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px; -fx-text-alignment: center;");
            lblSub.setWrapText(true);
            lblSub.setMaxWidth(400);
            lblSub.setAlignment(javafx.geometry.Pos.CENTER);

            emptyState.getChildren().addAll(lblIcon, lblVacio, lblSub);
            panelMisLibros.getChildren().setAll(emptyState);
            return;
        }

        // 4. Dibujar las tarjetas (setAll = un solo layout pass vs clear+add = 2 passes)
        List<javafx.scene.Node> tarjetas = librosMostrados.stream()
                .map(this::crearTarjetaMisLibros)
                .collect(Collectors.toList());
        panelMisLibros.getChildren().setAll(tarjetas);
    }

    /**
     * Crea una tarjeta interactiva para la biblioteca principal.
     */
    private javafx.scene.layout.VBox crearTarjetaMisLibros(Libro libro) {
        javafx.scene.layout.VBox tarjeta = new javafx.scene.layout.VBox(8);
        tarjeta.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tarjeta.setPrefWidth(140);
        tarjeta.setStyle("-fx-padding: 10; -fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-cursor: hand;");

        // --- CLIC IZQUIERDO (DOBLE CLIC) ---
        tarjeta.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2) {
                mostrarDetalleLibro(libro);
            }
        });

        // --- CLIC DERECHO (MENÚ CONTEXTUAL) ---
        tarjeta.setOnContextMenuRequested(e -> {
            tablaLibros.getSelectionModel().select(libro);
            if (contextMenuLibros != null) {
                contextMenuLibros.show(tarjeta, e.getScreenX(), e.getScreenY());
            }
        });

        javafx.scene.image.ImageView img = new javafx.scene.image.ImageView();
        com.bibliohouse.utils.ImageLoader.load(libro.getPortadaURL(), img, 110, 160);

        // 1. Contenedor para apilar el badge sobre la imagen
        javafx.scene.layout.StackPane contenedorPortada = new javafx.scene.layout.StackPane(img);

        // 2. Lógica del Badge de estado
        String estado = libro.getEstadoLectura();
        if (estado != null) {
            Label badge = null;
            if (estado.equalsIgnoreCase("Leído")) {
                badge = new Label("✓");
                badge.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 12; -fx-font-size: 11px;");
            } else if (estado.equalsIgnoreCase("Leyendo")) {
                badge = new Label("•••");
                badge.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 1 6 3 6; -fx-background-radius: 12; -fx-font-size: 11px;");
            }
            // "Pendiente" no tiene etiqueta, así que se queda nulo y limpio

            if (badge != null) {
                // Sombra sutil para que se vea bien incluso si la portada del libro es blanca
                badge.setStyle(badge.getStyle() + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);");
                // Alinear arriba a la derecha con 5px de margen
                javafx.scene.layout.StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
                javafx.scene.layout.StackPane.setMargin(badge, new javafx.geometry.Insets(5, 5, 0, 0));

                contenedorPortada.getChildren().add(badge);
            }

        }

        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(130);
        lblTitulo.setAlignment(javafx.geometry.Pos.CENTER);
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-default;");

        // Lógica del Badge Digital
        if (libro.isEsDigital()) {
            Label badgeDigital = new Label("📱");
            badgeDigital.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 6 3 6; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeDigital.setStyle(badgeDigital.getStyle() + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);");

            // Alinear abajo a la izquierda con 5px de margen
            javafx.scene.layout.StackPane.setAlignment(badgeDigital, javafx.geometry.Pos.BOTTOM_LEFT);
            javafx.scene.layout.StackPane.setMargin(badgeDigital, new javafx.geometry.Insets(0, 0, 5, 5));

            contenedorPortada.getChildren().add(badgeDigital);
        }

        // Añadimos el contenedor (que lleva imagen + badge) en vez de solo la imagen
        tarjeta.getChildren().addAll(contenedorPortada, lblTitulo);

        // Lógica de la barra de progreso (Reading Tracker)
        if ("Leyendo".equalsIgnoreCase(estado) && libro.getPaginasTotales() > 0) {
            double progreso = (double) libro.getPaginaActual() / libro.getPaginasTotales();
            if (progreso > 1.0) {
                progreso = 1.0;
            }
            if (progreso < 0.0) {
                progreso = 0.0;
            }

            javafx.scene.control.ProgressBar pBar = new javafx.scene.control.ProgressBar(progreso);
            pBar.setPrefWidth(110);
            pBar.setPrefHeight(6);
            pBar.setStyle("-fx-accent: #ff9800;");

            // Etiqueta pequeñita
            Label lblProgreso = new Label(libro.getPaginaActual() + " / " + libro.getPaginasTotales() + " pág.");
            lblProgreso.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-fg-muted;");

            tarjeta.getChildren().addAll(pBar, lblProgreso);
        }

        return tarjeta;
    }

    /**
     * Abre un diálogo para que el usuario configure su objetivo anual de
     * lectura.
     *
     * Muestra el valor actual guardado en las preferencias. Si el usuario
     * introduce un número válido, se guarda en las preferencias, se persiste en
     * el archivo JSON y se actualiza la interfaz del widget del reto. Si el
     * valor es 0 o negativo, el reto se considera desactivado.
     *
     */
    @FXML
    private void configurarRetoAnual() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Reto de Lectura");
        dialog.setHeaderText("Configurar Reto Anual");
        dialog.setContentText("¿Cuántos libros te propones leer este año? (Pon 0 para desactivar)");

        String retoActualStr = preferencias.getOrDefault("reto_anual", "0");
        dialog.getEditor().setText(retoActualStr);

        dialog.showAndWait().ifPresent(resultado -> {
            try {
                int reto = Integer.parseInt(resultado.trim());
                preferencias.put("reto_anual", String.valueOf(reto));
                jsonManager.guardarPreferencias(preferencias);
                actualizarRetoAnual();
            } catch (NumberFormatException ex) {
                mostrarAlerta("Error", "Por favor introduce un número válido.");
            }
        });
    }

    /**
     * Actualiza la visibilidad y el estado de la barra de progreso del reto
     * anual.
     *
     * Calcula el porcentaje de avance comparando los libros marcados como
     * "Leído" en la lista completa contra el objetivo establecido en las
     * preferencias.
     *
     */
    private void actualizarRetoAnual() {
        if (widgetRetoAnual == null) {
            return;
        }

        int reto = 0;
        try {
            reto = Integer.parseInt(preferencias.getOrDefault("reto_anual", "0"));
        } catch (NumberFormatException e) {
        }

        if (reto <= 0) {
            widgetRetoAnual.setVisible(false);
            widgetRetoAnual.setManaged(false);
            return;
        }

        widgetRetoAnual.setVisible(true);
        widgetRetoAnual.setManaged(true);

        int currentYear = java.time.LocalDate.now().getYear();
        lblTituloReto.setText("Reto de Lectura " + currentYear);

        long librosLeidos = listaLibrosCompleta.stream()
                .filter(l -> "Leído".equalsIgnoreCase(l.getEstadoLectura()))
                .count();

        double progress = (double) librosLeidos / reto;
        if (progress > 1.0) {
            progress = 1.0;
        }

        progresoReto.setProgress(progress);
        lblEstadoReto.setText(librosLeidos + " de " + reto + " libros");
    }

    /**
     * Muestra una notificación con el mensaje especificado en el panel
     * designado. La notificación se oculta automáticamente tras 3 segundos
     * mediante una pausa programada.
     *
     * @param mensaje el texto a mostrar en la notificación
     */
    private void notificar(String mensaje) {
        notificationPane.setText(mensaje);
        notificationPane.show();
        // Se oculta automáticamente tras 3 segundos
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> notificationPane.hide());
        delay.play();
    }

    /**
     * Configura el panel Mis Libros para aceptar Drag & Drop de archivos de
     * e-book.
     */
    private void configurarDragAndDropPanelMisLibros() {
        if (panelMisLibros == null) {
            return;
        }

        panelMisLibros.setOnDragOver(event -> {
            if (event.getGestureSource() != panelMisLibros && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                panelMisLibros.setStyle("-fx-background-color: -color-accent-subtle; -fx-border-color: -color-accent-emphasis; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8;");
            }
            event.consume();
        });

        panelMisLibros.setOnDragExited(event -> {
            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            event.consume();
        });

        panelMisLibros.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;

            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

            if (db.hasFiles()) {
                java.io.File file = db.getFiles().get(0);

                if (com.bibliohouse.logic.EbookMetadataService.esArchivoEbook(file)) {
                    notificar("Importando e-book...");

                    // Procesar en un hilo separado para no bloquear la UI
                    Thread importThread = new Thread(() -> {
                        Libro nuevoLibro = com.bibliohouse.logic.EbookMetadataService.crearLibroDesdeArchivo(file, this.rutaUsuario);

                        javafx.application.Platform.runLater(() -> {
                            if (nuevoLibro != null) {
                                // Evitar duplicados por título
                                boolean existe = listaLibrosCompleta.stream()
                                        .anyMatch(l -> l.getTitulo() != null && l.getTitulo().equalsIgnoreCase(nuevoLibro.getTitulo()));

                                if (existe) {
                                    mostrarAlerta("Libro duplicado", "Ya existe un libro con el título: " + nuevoLibro.getTitulo());
                                } else {
                                    nuevoLibro.setId(java.util.UUID.randomUUID().toString());
                                    listaLibrosCompleta.add(nuevoLibro);
                                    guardarLibrosEnDisco();
                                    actualizarFiltros();
                                    actualizarPanelMisLibros();
                                    notificar("E-book importado: " + nuevoLibro.getTitulo());
                                }
                            } else {
                                mostrarAlerta("Error de importación", "No se pudo extraer información del archivo: " + file.getName());
                            }
                        });
                    });
                    importThread.setDaemon(true);
                    importThread.start();

                    success = true;
                } else {
                    mostrarAlerta("Formato no soportado", "Solo puedes soltar archivos EPUB, PDF o MOBI aquí.");
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

}
