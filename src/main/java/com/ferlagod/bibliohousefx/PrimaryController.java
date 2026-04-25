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
import com.bibliohouse.logic.GoogleBooksCliente;
import com.bibliohouse.logic.ImportarExportarBD;
import com.bibliohouse.logic.InventaireCliente;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.NextCloudSyncService;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.Socio;
import com.bibliohouse.logic.OpenLibraryCliente;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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
import javafx.scene.layout.TilePane;
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
 * @author Ferlagod
 * @version 1.6
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
    private java.util.Map<String, String> preferencias;
    private FilteredList<Libro> filteredData; // Lista que la tabla usará para filtrar
    private SortedList<Libro> sortedData; // Lista que la tabla usará para ordenar (basada en filteredData)
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(PrimaryController.class.getName());
    private BusquedaSagas busquedaSagas = new BusquedaSagas();
    private ImportarExportarBD gestorArchivos = new ImportarExportarBD();

    // --- LÍMITE DE PRÉSTAMO ---
    /**
     * Días límite para considerar un préstamo como vencido. Por defecto es 30,
     * pero puede ser configurado por el usuario.
     */
    private int dueDaysLimit = 30;

    // --- CONSTANTES DE VISTA ---
    private static final String VISTA_TODOS = "Todos los libros";
    private static final String VISTA_DESEOS = "Lista de Deseos";
    private static final String CSS_PRESTAMO_VENCIDO = "overdue-loan";
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
    private ComboBox<Libro> comboLibrosPrestamo;
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
    private TabPane tabPaneVistaLibros;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab tabGaleria;
    @FXML
    private Tab tabTabla;
    @FXML
    private TilePane tilePanePortadas; // El contenedor de la cuadrícula
    @FXML
    private ScrollPane scrollPaneGaleria;
    // PORTADA MANUAL
    @FXML
    private ImageView imgPortadaManual;
    @FXML
    private CheckBox chkPoseidoManual;
    // Search OpenLibrary
    @FXML
    private TextField txtBusquedaOpenLibrary;
    // Prestamos
    @FXML
    private ComboBox<Socio> comboSocios;
    @FXML
    private ComboBox<String> cmbFiltroPrestamos;
    @FXML
    private TableView<Prestamo> tablaPrestamos;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoLibro;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoSocio;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoFecha;
    @FXML
    private TableColumn<Prestamo, String> colPrestamoDevolucion;
    // Historial
    @FXML
    private TableView<Prestamo> tablaHistorial;
    @FXML
    private TableColumn<Prestamo, String> colHistorialLibro;
    @FXML
    private TableColumn<Prestamo, String> colHistorialSocio;
    @FXML
    private TableColumn<Prestamo, String> colHistorialFechaPrestamo;
    @FXML
    private TableColumn<Prestamo, String> colHistorialFechaDevolucion;
    // Estado y Lateral
    @FXML
    private Label lblEstado;
    @FXML
    private ListView<String> listaEstanterias;
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
    private javafx.scene.layout.FlowPane panelDeseos;
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
            configurarColumnasPrestamos();

            // Placeholder para la tabla de préstamos
            Label placeholderPrestamos = new Label("No hay préstamos activos en este momento.");
            placeholderPrestamos.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            tablaPrestamos.setPlaceholder(placeholderPrestamos);

            // Ya que estás, haz lo mismo para la tabla de libros principal
            Label placeholderLibros = new Label("La tabla está vacía. Añade libros o cambia los filtros.");
            placeholderLibros.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            tablaLibros.setPlaceholder(placeholderLibros);

            configurarContextMenu();
            configurarFiltros();
            configurarAtajosYEventos();

        } catch (Exception e) {
            System.err.println("[PrimaryController] Error CRÍTICO en initialize: " + e.getMessage());
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Inicialización");
                alert.setHeaderText("Fallo al iniciar la pantalla principal");
                alert.setContentText("Ocurrió un error inesperado al configurar la vista: " + e.getMessage());
                alert.showAndWait();
            });
        }

        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab.getContent() != null) {
                    Node content = newTab.getContent();

                    // MEJORA DE SUAVIDAD: Activamos la caché de hardware antes de la animación
                    content.setCache(true);
                    content.setCacheHint(javafx.scene.CacheHint.SPEED);

                    // Configuramos la transición de desvanecimiento
                    FadeTransition fade = new FadeTransition(Duration.millis(300), content);
                    fade.setFromValue(0.0); // Empieza totalmente transparente
                    fade.setToValue(1.0);   // Termina totalmente opaco
                    fade.setCycleCount(1);
                    fade.setAutoReverse(false);

                    // Al terminar la animación, desactivamos la caché para liberar memoria de video
                    fade.setOnFinished(e -> {
                        content.setCache(false);
                        content.setCacheHint(javafx.scene.CacheHint.DEFAULT);
                    });

                    // Pequeño efecto de desplazamiento hacia arriba (Slide + Fade)
                    content.setTranslateY(10); // Baja el contenido 10 píxeles inicialmente
                    javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(300), content);
                    slide.setFromY(10);
                    slide.setToY(0);
                    slide.play();

                    fade.play(); // Iniciamos la animación
                }
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
     * Configura las columnas de las tablas de préstamos e historial.
     */
    private void configurarColumnasPrestamos() {
        colPrestamoLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p.getLibroId() != null) {
                return FXCollections.observableArrayList(listaLibrosCompleta).stream()
                        .filter(l -> l.getId().equals(p.getLibroId()))
                        .findFirst()
                        .map(Libro::getTitulo)
                        .map(javafx.beans.property.SimpleStringProperty::new)
                        .orElse(new javafx.beans.property.SimpleStringProperty(p.getTituloLibro() + " (Borrado)"));
            }
            return new javafx.beans.property.SimpleStringProperty(p.getTituloLibro());
        });

        colPrestamoSocio.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            return FXCollections.observableArrayList(listaSocios).stream()
                    .filter(s -> s.getNumeroSocio() == p.getNumeroSocio())
                    .findFirst()
                    .map(Socio::getNombreCompleto)
                    .map(javafx.beans.property.SimpleStringProperty::new)
                    .orElse(new javafx.beans.property.SimpleStringProperty(p.getNombreSocio() + " (Borrado)"));
        });

        colPrestamoFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPrestamoFormateada"));
        colPrestamoDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionFormateada"));

        colHistorialLibro.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p.getLibroId() != null) {
                return FXCollections.observableArrayList(listaLibrosCompleta).stream()
                        .filter(l -> l.getId().equals(p.getLibroId()))
                        .findFirst()
                        .map(Libro::getTitulo)
                        .map(javafx.beans.property.SimpleStringProperty::new)
                        .orElse(new javafx.beans.property.SimpleStringProperty(p.getTituloLibro()));
            }
            return new javafx.beans.property.SimpleStringProperty(p.getTituloLibro());
        });

        colHistorialSocio.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            return FXCollections.observableArrayList(listaSocios).stream()
                    .filter(s -> s.getNumeroSocio() == p.getNumeroSocio())
                    .findFirst()
                    .map(Socio::getNombreCompleto)
                    .map(javafx.beans.property.SimpleStringProperty::new)
                    .orElse(new javafx.beans.property.SimpleStringProperty(p.getNombreSocio()));
        });

        colHistorialFechaPrestamo.setCellValueFactory(new PropertyValueFactory<>("fechaPrestamoFormateada"));
        colHistorialFechaDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionFormateada"));
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

        MenuItem itemEditar = new MenuItem(resources.getString("ctx.edit"));
        itemEditar.setOnAction(e -> editarLibroSeleccionado(null));

        MenuItem itemPortada = new MenuItem(resources.getString("ctx.cover"));
        itemPortada.setOnAction(e -> cambiarPortadaDesdePrincipal(null));

        MenuItem itemEliminar = new MenuItem(resources.getString("ctx.delete"));
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> eliminarLibro(null));

        contextMenuLibros.getItems().addAll(itemPrestar, new SeparatorMenuItem(), itemEditar, itemPortada,
                new SeparatorMenuItem(), itemEliminar);
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

        // Búsqueda incremental por Título
        if (txtBusquedaLocal != null) {
            txtBusquedaLocal.textProperty().addListener((observable, oldValue, newValue) -> actualizarFiltros());
        }

        if (txtFiltroAutor != null) {
            txtFiltroAutor.textProperty().addListener((observable, oldValue, newValue) -> actualizarFiltros());
        }

        if (txtFiltroISBN != null) {
            txtFiltroISBN.textProperty().addListener((observable, oldValue, newValue) -> actualizarFiltros());
        }

        if (cmbFiltroEstado != null) {
            cmbFiltroEstado.setItems(FXCollections.observableArrayList("Todos", "Leído", "Leyendo", "Pendiente"));
            cmbFiltroEstado.setValue("Todos");
            cmbFiltroEstado.valueProperty().addListener((obs, old, newVal) -> actualizarFiltros());
        }

        // Row Factory para marcar préstamos vencidos
        if (tablaPrestamos != null) {
            tablaPrestamos.setRowFactory(tv -> new TableRow<Prestamo>() {
                @Override
                protected void updateItem(Prestamo item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove(CSS_PRESTAMO_VENCIDO);
                    if (item != null && !empty) {
                        if (item.getFechaDevolucion() == null && item.getFechaPrestamo() != null) {
                            LocalDate dueDate = item.getFechaPrestamo().plusDays(dueDaysLimit);
                            if (dueDate.isBefore(LocalDate.now())) {
                                getStyleClass().add(CSS_PRESTAMO_VENCIDO);
                            }
                        }
                    }
                }
            });
        }

        // Buscador visual rápido en la pestaña Mis Libros
        // En PrimaryController.java
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
        this.listaDeseos = jsonManager.cargarDeseos();
        actualizarPanelDeseos();

        // 2. Cargar datos maestros
        cargarDatos();

        // IMPORTANTE: Configurar el callback de guardado para el gestor de sagas
        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta, () -> {
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                tablaLibros.refresh();
                actualizarFiltros();
            });
        }

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
            lblEstado.setText(java.text.MessageFormat.format(resources.getString("status.welcome"), username));
        } else {
            lblEstado.setText("Bienvenido, " + username);
        }

        checkOverdueLoans();

        // 5. Lógica para pantalla completa / Maximizado en Linux
        Platform.runLater(() -> {
            if (lblEstado.getScene() != null && lblEstado.getScene().getWindow() != null) {
                Stage stage = (Stage) lblEstado.getScene().getWindow();

                boolean iniciarMaximizado = Boolean.parseBoolean(preferencias.getOrDefault("maximized", "true"));

                if (iniciarMaximizado) {
                    // Intento 1: Vía estándar nativa
                    stage.setMaximized(true);

                    // Intento 2: Fuerza bruta asíncrona para gestores de ventanas de Linux
                    // Retrasamos la ejecución 150 milisegundos para darle tiempo al SO a asimilar la escena
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
                    delay.setOnFinished(e -> {
                        stage.setMaximized(true); // Insistimos

                        // Si el SO ignoró el comando, fijamos las dimensiones a la fuerza usando los límites de la pantalla
                        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                        if (stage.getWidth() < bounds.getWidth() * 0.9) {
                            stage.setX(bounds.getMinX());
                            stage.setY(bounds.getMinY());
                            stage.setWidth(bounds.getWidth());
                            stage.setHeight(bounds.getHeight());
                        }
                    });
                    delay.play();
                }
            }
        });

        // 6. Actualizaciones en segundo plano
        com.bibliohouse.utils.UpdateChecker.comprobarActualizaciones(versionNueva -> {
            // Lógica de notificación de nueva versión
        });
    }

    /**
     * Comprueba los préstamos activos y notifica si hay vencidos (más de
     * DUE_DAYS_LIMIT).
     */
    private void checkOverdueLoans() {
        // Filtramos solo los préstamos activos (no devueltos) y que han superado el
        // límite
        List<Prestamo> overdueLoans = listaPrestamosCompleta.stream()
                .filter(p -> p.getFechaDevolucion() == null)
                .filter(p -> p.getFechaPrestamo() != null)
                .filter(p -> p.getFechaPrestamo().isBefore(LocalDate.now().minusDays(dueDaysLimit))) // Usar variable
                // dinámica
                .collect(Collectors.toList());

        if (!overdueLoans.isEmpty()) {
            // Retrasar la alerta 1 segundo para evitar bloqueos en macOS al iniciar la ventana principal
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            delay.setOnFinished(e -> {
                // Construir el mensaje de alerta
                StringBuilder sb = new StringBuilder();
                sb.append("Se han detectado ").append(overdueLoans.size()).append(" préstamos vencidos:\n\n");

                overdueLoans.stream().limit(5).forEach(p -> {
                    // Calcular días de retraso
                    long daysOverdue = ChronoUnit.DAYS.between(p.getFechaPrestamo().plusDays(dueDaysLimit),
                            LocalDate.now());
                    sb.append("• ").append(p.getTituloLibro()).append(" (Socio #").append(p.getNumeroSocio())
                            .append("): ");
                    sb.append(daysOverdue).append(" días de retraso.\n");
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

                // Botón para saltar a la pestaña de Préstamos
                ButtonType viewLoansButton = new ButtonType("Ver Préstamos", ButtonBar.ButtonData.OK_DONE);
                ButtonType dismissButton = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(viewLoansButton, dismissButton);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == viewLoansButton) {
                    // Encontrar el TabPane (asumiendo que es el único en BorderPane.center)
                    TabPane tabPane = (TabPane) tablaLibros.getScene().lookup(".tab-pane");
                    if (tabPane != null) {
                        tabPane.getSelectionModel().select(2);
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
        // Necesitamos esperar a que la escena esté lista
        Platform.runLater(() -> {
            Scene scene = tablaLibros.getScene();
            if (scene != null) {
                // Ctrl+F (Cmd+F) -> Foco en búsqueda
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+F"),
                        () -> {
                            tabPaneVistaLibros.getSelectionModel().select(tabTabla);
                            txtBusquedaLocal.requestFocus();
                        });

                // Ctrl+N (Cmd+N) -> Foco en añadir manual
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+N"),
                        () -> {
                            tabPaneVistaLibros.getSelectionModel().select(tabTabla);
                            txtTitulo.requestFocus();
                        });

                // Ctrl+L (Cmd+L) -> Foco en préstamos
                scene.getAccelerators().put(
                        javafx.scene.input.KeyCombination.keyCombination("Shortcut+L"),
                        () -> {
                            // Seleccionar tab de préstamos
                            tabPaneVistaLibros.getSelectionModel().select(2);
                            comboLibrosPrestamo.requestFocus();
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

        if (tablaPrestamos != null) {
            tablaPrestamos.refresh();
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
        // 1. Cargar la lista maestra de libros
        List<Libro> libros = jsonManager.cargarLibros();
        listaLibrosCompleta = FXCollections.observableArrayList(libros);

        // 2. Inicializar las listas de filtrado/ordenación
        filteredData = new FilteredList<>(listaLibrosCompleta, p -> true); // Predicado inicial: mostrar todo
        sortedData = new SortedList<>(filteredData);

        // 3. Unir la SortedList con el comparador de la tabla, PERO personalizando
        // Establecer comparador por defecto (Series)
        sortedData.setComparator(this::compareBySeries);

        // 3.B Lógica de ordenación personalizada
        // Escuchamos cambios en el orden de la tabla
        tablaLibros.comparatorProperty().addListener((obs, oldComp, newComp) -> {
            if (newComp == null) {
                // Si la tabla no tiene orden (estado inicial o reseteado), usasmos el nuestro
                sortedData.setComparator(this::compareBySeries);
            } else {
                // Si la tabla tiene orden (usuario hizo click), usamos ese.
                sortedData.setComparator(newComp);
            }
        });

        // 4. Asignar la lista DINÁMICA a la tabla (FIX)
        tablaLibros.setItems(sortedData);

        // Socios
        List<Socio> socios = jsonManager.cargarSocios();
        listaSocios = FXCollections.observableArrayList(socios);
        comboSocios.setItems(listaSocios);

        // Prestamos
        List<Prestamo> prestamos = jsonManager.cargarPrestamos();
        listaPrestamosCompleta = FXCollections.observableArrayList(prestamos);

        // Préstamos Activos (solo los que NO tienen fecha de devolución)
        filteredPrestamos = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() == null);
        tablaPrestamos.setItems(filteredPrestamos);

        // Historial (solo los que SÍ tienen fecha de devolución)
        filteredHistory = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() != null);
        tablaHistorial.setItems(filteredHistory);

        // Estanterías Lateral
        cargarListaEstanterias();
        actualizarComboLibrosDisponibles();

        // Aplicar filtros iniciales para que la tabla se muestre correctamente al
        // cargar
        actualizarFiltros();

        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta, () -> {
                // Esto es el Runnable (callback). Se ejecutará cuando SagasController llame a ejecutarGuardado()
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                tablaLibros.refresh(); // Refrescamos la tabla principal también por si borraron libros
            });
        }
        // --- CONFIGURAR FILTRADO EN COMBOS ---
        // Configurar filtrado para Libros
        setupFilteringComboBox(comboLibrosPrestamo, Libro::getTitulo);

        // Configurar filtrado para Socios
        setupFilteringComboBox(comboSocios, Socio::getNombreCompleto);
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
            if (comboBox == comboLibrosPrestamo) {
                // Solo queremos libros que tengan stock (cantidad > 0)
                sourceList = FXCollections.observableArrayList();
                for (Object o : listaLibrosCompleta) {
                    Libro l = (Libro) o;
                    if (l.getCantidad() > 0) {
                        sourceList.add((T) l);
                    }
                }
            } else if (comboBox == comboSocios) {
                // Para socios usamos la lista de socios actual
                sourceList = (ObservableList<T>) listaSocios;
            } else {
                // Para otros combos usamos la lista original guardada
                sourceList = originalItems;
            }

            if (!comboBox.isShowing()) {
                // Si el combo está cerrado, a veces da problemas filtrar, así que mejor no
                // hacemos nada
            }

            // Si el usuario borra el texto, mostramos todos los elementos (actualizados)
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
            @Override
            public String toString(T object) {
                if (object == null) {
                    return null;
                }
                return displayFunc.apply(object);
            }

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

        // 3. Ni serie ni orden: Por Título
        return l1.getTitulo().compareToIgnoreCase(l2.getTitulo());
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
                    if (libro.isPoseido()) {
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
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(2); // Seleccionar pestaña Préstamos
        }

        if (comboLibrosPrestamo != null) {
            // Buscamos el libro en el combo para seleccionarlo correctamente
            for (Libro l : comboLibrosPrestamo.getItems()) {
                // Comparamos por ID o Título/Autor si no hay ID único, asumiendo objetos
                // iguales
                if (l.equals(libro)) {
                    comboLibrosPrestamo.getSelectionModel().select(l);
                    break;
                }
            }
            comboLibrosPrestamo.requestFocus();
        }
    }

    /**
     * Prepara la pestaña de préstamos con el socio seleccionado.
     *
     * @param socio El socio al que prestar.
     */
    public void prepararPrestamoSocio(Socio socio) {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(2); // Seleccionar pestaña Préstamos
        }

        if (comboSocios != null) {
            for (Socio s : comboSocios.getItems()) {
                if (s.equals(socio)) {
                    comboSocios.getSelectionModel().select(s);
                    break;
                }
            }
            comboSocios.requestFocus();
        }
    }

    // Método auxiliar para refrescar el desplegable de libros
    /**
     * Actualiza el ComboBox de libros disponibles para préstamo. Solo incluye
     * libros que tengan stock disponible (cantidad > 0).
     */
    private void actualizarComboLibrosDisponibles() {
        ObservableList<Libro> librosConStock = listaLibrosCompleta.stream()
                .filter(l -> l.getCantidad() > 0)
                .collect(java.util.stream.Collectors.toCollection(FXCollections::observableArrayList));
        comboLibrosPrestamo.setItems(librosConStock);
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
        items.addAll(estanterias);
        listaEstanterias.setItems(items);
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
            if (!isbn.isEmpty() && !l.getIsbn().isEmpty() && l.getIsbn().equals(isbn)) {
                libroExistente = l;
                break;
            }
            // Coincidencia por Título y Autor (si no hay ISBN o es distinto)
            if (l.getTitulo().equalsIgnoreCase(titulo) && l.getAutor().equalsIgnoreCase(autor)) {
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
        // NUEVO: SECUESTRAR LA PORTADA PARA GUARDARLA EN LOCAL (MODO OFFLINE)
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
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
        lblEstado.setText(mensaje);
        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta);
        }
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
            System.out.println("[DEBUG] Cargando FXML de resultados_busqueda.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resultados_busqueda.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("[ERROR] No se pudo encontrar el archivo resultados_busqueda.fxml");
                mostrarAlerta("Error", "No se encontró el archivo de la ventana de resultados.");
                return;
            }

            System.out.println("[DEBUG] Archivo FXML encontrado en: " + loader.getLocation());
            Parent root = loader.load();
            System.out.println("[DEBUG] FXML cargado exitosamente");

            ResultadosBusquedaController controller = loader.getController();
            System.out.println("[DEBUG] Controller obtenido: " + controller);
            controller.setResultados(resultados);
            System.out.println("[DEBUG] Resultados configurados en el controller");

            Stage stage = new Stage();
            stage.setTitle("Resultados de búsqueda");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            System.out.println("[DEBUG] Mostrando ventana de resultados...");
            stage.showAndWait();
            System.out.println("[DEBUG] Ventana de resultados cerrada");

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
                    actualizarPanelDeseos();
                    lblEstado.setText("Añadido a tu Lista de Deseos: " + elegido.getTitulo());
                } else {
                    // SE PULSÓ "IMPORTAR A BIBLIOTECA"
                    rellenarFormularioManual(elegido);
                }
            }

        } catch (IOException e) {
            System.err.println("[ERROR] IOException al abrir ventana de resultados: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir la ventana de resultados: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Excepción inesperada al abrir ventana de resultados: " + e.getMessage());
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
            stage.setMinWidth(900);
            stage.setMinHeight(900);
            stage.sizeToScene();
            stage.showAndWait();

            if (controller.isGuardado()) {
                tablaLibros.refresh();
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                cargarListaEstanterias();
                if (pestanaSagasController != null) {
                    pestanaSagasController.initData(listaLibrosCompleta);
                }
                lblEstado.setText("Libro editado correctamente.");
            }
        } catch (IOException e) {
            System.err.println("[PrimaryController] Error al abrir ventana de edición: " + e.getMessage());
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
            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
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
        // 1. Obtener el libro seleccionado
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();

        if (libroSeleccionado == null) {
            mostrarAlerta("Ningún libro seleccionado", "Por favor, selecciona un libro de la tabla para eliminarlo.");
            return;
        }

        // 2. CASO A: TIENE MÁS DE 1 UNIDAD (STOCK MÚLTIPLE)
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
                    // RESTAR 1 AL STOCK
                    libroSeleccionado.setCantidad(libroSeleccionado.getCantidad() - 1);

                    // Refrescar tabla y guardar
                    tablaLibros.refresh();
                    jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                    lblEstado.setText("Se ha eliminado una copia. Quedan: " + libroSeleccionado.getCantidad());

                } else if (result.get() == btnEliminarTodo) {
                    // BORRARLO DEL MAPA
                    borrarTotalmente(libroSeleccionado);
                }
            }

        } else {
            // 3. CASO B: SOLO QUEDA 1 UNIDAD (Comportamiento normal)
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Eliminar Libro");
            alert.setHeaderText("¿Estás seguro de que quieres borrar este libro?");
            alert.setContentText(
                    "Vas a eliminar: " + libroSeleccionado.getTitulo() + "\nEsta acción no se puede deshacer.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                borrarTotalmente(libroSeleccionado);
            }
        }
    }

    /**
     * Elimina completamente un libro de la lista y del archivo JSON.
     *
     * @param libro El libro a eliminar.
     */
    private void borrarTotalmente(Libro libro) {

        if (listaLibrosCompleta != null) {
            listaLibrosCompleta.remove(libro);
        }
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
        lblEstado.setText("Libro eliminado definitivamente: " + libro.getTitulo());

        if (pestanaSagasController != null) {
            pestanaSagasController.initData(listaLibrosCompleta);
        }

        // Refrescar la cuadrícula de "Mis Libros"
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
     * Cierra la aplicación de forma segura, guardando preferencias antes de
     * salir.
     *
     * @param event El evento del menú Salir.
     */
    @FXML
    private void abrirEscaner(ActionEvent event) {

        System.out.println("[PrimaryController] Solicitud para abrir escáner recibida.");
        try {
            System.out.println("[PrimaryController] Cargando escaner.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("escaner.fxml"));
            Parent root = loader.load();
            System.out.println("[PrimaryController] FXML cargado.");

            EscanerController escanerController = loader.getController();
            escanerController.setListener(isbns -> {
                // Cuando se detecta un lote de ISBNs:
                if (isbns != null && !isbns.isEmpty()) {
                    // Tomamos el último para rellenar el campo (simulando comportamiento anterior)
                    // O si es ráfaga, podríamos procesarlos todos.
                    // Por ahora, procesamos el último para mantener la funcionalidad de búsqueda
                    // inmediata
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

            // Iniciar cámara al mostrar
            stage.setOnShown(e -> {
                System.out.println("[PrimaryController] Ventana mostrada. Invocando escanerController.init()...");
                escanerController.init();
            });
            // Asegurar cierre de cámara al cerrar ventana
            stage.setOnCloseRequest(e -> escanerController.shutdown());

            stage.show();
            System.out.println("[PrimaryController] Ventana de escáner visible.");

        } catch (IOException e) {
            System.err.println("[PrimaryController] Error IO al abrir escáner: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo abrir el escáner: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[PrimaryController] Error general al abrir escáner: " + e.getMessage());
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
        if (listaLibrosCompleta == null || listaLibrosCompleta.isEmpty()) {
            return;
        }

        // 1. Usar un Map para encontrar duplicados en una sola pasada (O(n))
        // Agrupamos por una "clave de identidad" (ISBN normalizado o Titulo+Autor)
        Map<String, List<Libro>> grupos = listaLibrosCompleta.stream().collect(Collectors.groupingBy(l -> {
            if (l.getIsbn() != null && !l.getIsbn().isBlank()) {
                return l.getIsbn().replaceAll("[^0-9X]", ""); // Normalizar ISBN
            }
            return (l.getTitulo() + "|" + l.getAutor()).toLowerCase().trim();
        }));

        List<Libro> librosParaBorrar = new ArrayList<>();
        StringBuilder reporte = new StringBuilder("Análisis de duplicados:\n\n");
        int contadorFusionados = 0;

        for (List<Libro> grupo : grupos.values()) {
            if (grupo.size() > 1) {
                Libro principal = grupo.get(0);
                for (int i = 1; i < grupo.size(); i++) {
                    Libro duplicado = grupo.get(i);

                    // Sumar stock al principal
                    principal.setCantidad(principal.getCantidad() + duplicado.getCantidad());
                    librosParaBorrar.add(duplicado);
                    contadorFusionados++;
                    reporte.append("✔️ ").append(principal.getTitulo()).append(" (Fusionado)\n");
                }
            }
        }

        if (contadorFusionados == 0) {
            mostrarAlerta("Búsqueda de Duplicados", "No se encontraron libros repetidos.");
            return;
        }

        // 2. Aplicar cambios
        listaLibrosCompleta.removeAll(librosParaBorrar);
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
        tablaLibros.refresh();
        actualizarPanelMisLibros();

        // 3. Mostrar informe en tu ventana de duplicados
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("duplicados.fxml"));
            Parent root = loader.load();
            DuplicadosController controller = loader.getController();
            controller.setTextoResultados(reporte.toString());

            Stage stage = new Stage();
            stage.setTitle("Informe de Duplicados");
            setScene(stage, root);
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Éxito", "Se han fusionado " + contadorFusionados + " libros.");
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
     * @param event El evento del botón Nuevo Socio.
     */
    @FXML
    private void nuevoSocio(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gestion_socios.fxml"));
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
    @FXML
    private void gestionarSocios(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("socios_manager.fxml"));
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
        comboSocios.setItems(listaSocios);

        actualizarComboLibrosDisponibles(); // Por si el stock cambió
        tablaPrestamos.refresh(); // Refrescamos la tabla de préstamos
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
     * Realiza un préstamo de un libro a un socio. Verifica el stock y registra
     * el préstamo.
     *
     * @param event El evento del botón Prestar.
     */
    @FXML
    private void realizarPrestamo(ActionEvent event) {
        // 1. Obtener datos de LOS COMBOS
        Libro libroSeleccionado = comboLibrosPrestamo.getValue();
        Socio socio = comboSocios.getValue();

        if (libroSeleccionado == null || socio == null) {
            mostrarAlerta("Datos faltantes", "Por favor, selecciona un libro y un socio de las listas.");
            return;
        }

        // 2. BUSCAR EL LIBRO ORIGINAL (Aseguramos trabajar con el objeto maestro)
        Libro libroOriginal = listaLibrosCompleta.stream()
                .filter(l -> l.getId().equals(libroSeleccionado.getId()))
                .findFirst()
                .orElse(null);

        if (libroOriginal == null) {
            mostrarAlerta("Error", "No se pudo localizar el libro en la base de datos.");
            return;
        }

        // 3. VALIDACIÓN: Stock real
        if (libroOriginal.getCantidad() <= 0) {
            mostrarAlerta("Sin stock", "No quedan ejemplares disponibles de este libro.");
            return;
        }

        // 4. VALIDACIÓN: Evitar duplicados (CON PROTECCIÓN NULA - FIX CRASH)
        boolean yaLoTiene = listaPrestamosCompleta.stream()
                .anyMatch(p -> {
                    // Comprobamos que sea el mismo socio y esté sin devolver
                    if (p.getNumeroSocio() == socio.getNumeroSocio() && p.getFechaDevolucion() == null) {
                        // PROTECCIÓN: Si el préstamo viejo no tiene ID, comparamos por título (legacy)
                        // Si tiene ID, comparamos estrictamente por ID.
                        if (p.getLibroId() != null) {
                            return p.getLibroId().equals(libroOriginal.getId());
                        } else {
                            return p.getTituloLibro() != null && p.getTituloLibro().equals(libroOriginal.getTitulo());
                        }
                    }
                    return false;
                });

        if (yaLoTiene) {
            mostrarAlerta("Préstamo duplicado", socio.getNombre() + " ya tiene una copia activa de este libro.");
            return;
        }

        // 5. REGISTRAR PRÉSTAMO
        Prestamo nuevoPrestamo = new Prestamo(libroOriginal, socio);
        listaPrestamosCompleta.add(nuevoPrestamo);

        // 6. RESTAR STOCK
        libroOriginal.setCantidad(libroOriginal.getCantidad() - 1);

        // 7. GUARDAR Y REFRESCAR
        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        tablaLibros.refresh();
        actualizarComboLibrosDisponibles();

        lblEstado.setText("Préstamo realizado: " + libroOriginal.getTitulo());
        mostrarAlerta("Éxito", "Préstamo registrado correctamente.");
    }

    /**
     * Marca un préstamo como devuelto. Actualiza la fecha de devolución y
     * repone el stock del libro.
     *
     * @param event El evento del botón Devolver.
     */
    @FXML
    private void marcarDevuelto(ActionEvent event) {
        // 1. Obtener el préstamo seleccionado
        Prestamo p = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (p == null) {
            mostrarAlerta("Selección necesaria", "Selecciona un préstamo de la lista para devolverlo.");
            return;
        }

        // 2. Comprobar si ya estaba devuelto para evitar duplicados
        if (p.getFechaDevolucion() != null) {
            mostrarAlerta("Aviso", "Este préstamo ya figura como devuelto el " + p.getFechaDevolucionFormateada());
            return;
        }

        // 3. ACTUALIZAR ESTADO DEL PRÉSTAMO
        p.setFechaDevolucion(LocalDate.now());

        // 4. DEVOLVER STOCK AL LIBRO (Uso estricto de ID único)
        // Buscamos en la lista maestra el libro que coincida exactamente con el ID guardado en el préstamo
        listaLibrosCompleta.stream()
                .filter(l -> l.getId().equals(p.getLibroId()))
                .findFirst()
                .ifPresentOrElse(
                        libro -> {
                            libro.setCantidad(libro.getCantidad() + 1);
                            LOGGER.log(java.util.logging.Level.INFO, "Stock devuelto para el libro: {0}", libro.getTitulo());
                        },
                        () -> LOGGER.log(java.util.logging.Level.WARNING, "No se encontró el libro con ID {0} para devolver stock. ¿Fue borrado?", p.getLibroId())
                );

        // 5. GUARDAR CAMBIOS (Ahora de forma atómica gracias al cambio en JsonManager)
        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        // 6. REFRESCAR INTERFAZ
        // Re-filtramos para que el préstamo desaparezca de "Activos" y aparezca en "Historial"
        filteredPrestamos.setPredicate(p2 -> p2.getFechaDevolucion() == null);
        filteredHistory.setPredicate(p2 -> p2.getFechaDevolucion() != null);

        tablaPrestamos.refresh();
        tablaHistorial.refresh();
        actualizarComboLibrosDisponibles();

        lblEstado.setText("Devolución registrada correctamente (ID: " + p.getLibroId() + ")");
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
            Parent root = loader.load();
            DetalleLibroController controller = loader.getController();
            controller.setLibro(libro);
            controller.setListaGlobalEstanterias(jsonManager.cargarEstanterias());

            Stage stage = new Stage();
            stage.setTitle("Detalles: " + libro.getTitulo());
            setScene(stage, root);
            stage.initOwner(tablaLibros.getScene().getWindow());
            stage.setMinWidth(700);
            stage.setMinHeight(600);
            stage.sizeToScene();
            stage.showAndWait();

            if (controller.isModified()) {
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
            }

            tablaLibros.refresh();
        } catch (IOException e) {
            System.err.println("[PrimaryController] Error al abrir detalle del libro: " + e.getMessage());
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
     * Actualiza el panel de deseos mostrando una tarjeta por cada libro en la
     * lista.
     */
    private void actualizarPanelDeseos() {
        panelDeseos.getChildren().clear();
        for (Libro libro : listaDeseos) {
            panelDeseos.getChildren().add(crearTarjetaDeseo(libro));
        }
    }

    /**
     * Crea una tarjeta visual para un libro en la lista de deseos.
     *
     * @param libro El libro para el que crear la tarjeta.
     * @return Un VBox con la imagen, título y botones de acción.
     */
    private javafx.scene.layout.VBox crearTarjetaDeseo(Libro libro) {
        javafx.scene.layout.VBox tarjeta = new javafx.scene.layout.VBox(8);
        tarjeta.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tarjeta.setPrefWidth(140);
        // Estilo de tarjeta bonita con sombra
        tarjeta.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        javafx.scene.image.ImageView img = new javafx.scene.image.ImageView();
        com.bibliohouse.utils.ImageLoader.load(libro.getPortadaURL(), img, 110, 160);

        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(130);
        lblTitulo.setAlignment(javafx.geometry.Pos.CENTER);
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #333;");

        // Botón verde de "Conseguido"
        Button btnMover = new Button(resources.getString("wishlist.move"));
        btnMover.setStyle("-fx-font-size: 10px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        btnMover.setMaxWidth(Double.MAX_VALUE);
        btnMover.setOnAction(e -> moverDeseoABiblioteca(libro));

        // Botón rojo de borrar
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

    /**
     * Mueve un libro de la lista de deseos a la biblioteca principal.
     */
    private void moverDeseoABiblioteca(Libro libro) {
        // 1. Quitar de deseos
        listaDeseos.remove(libro);
        jsonManager.guardarDeseos(listaDeseos);

        // 2. Añadir a la biblioteca principal
        libro.setPoseido(true);
        libro.setCantidad(1);
        listaLibrosCompleta.add(libro);
        jsonManager.guardarLibros(new java.util.ArrayList<>(listaLibrosCompleta));

        // 3. Refrescar vistas
        tablaLibros.refresh();
        actualizarPanelDeseos();
        actualizarComboLibrosDisponibles();
        lblEstado.setText(resources.getString("wishlist.moved.status"));
    }

    /**
     * Busca un libro en varios proveedores (OpenLibrary, Google Books,
     * Inventaire) de forma asíncrona.
     *
     * @param query El texto a buscar (título, autor o ISBN).
     */
    // Método unificado para lanzar la búsqueda RÁPIDA (con Timeouts)
    // En PrimaryController.java
    private void ejecutarBusquedaGlobal(String query) {
        if (resources != null && resources.containsKey("status.searching")) {
            lblEstado.setText(resources.getString("status.searching"));
        } else {
            lblEstado.setText("Buscando..."); // Texto de respaldo si falta la traducción
        }
        txtBusquedaOpenLibrary.setDisable(true); // Bloquear UI inmediatamente

        // Usamos el pool de hilos común para no saturar el sistema
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            // Ejecución en paralelo de los 3 proveedores con tiempos de espera estrictos
            var f1 = java.util.concurrent.CompletableFuture.supplyAsync(() -> OpenLibraryCliente.buscarLibros(query));
            var f2 = java.util.concurrent.CompletableFuture.supplyAsync(() -> GoogleBooksCliente.buscarLibros(query));
            var f3 = java.util.concurrent.CompletableFuture.supplyAsync(() -> InventaireCliente.buscarLibros(query));

            try {
                java.util.concurrent.CompletableFuture.allOf(f1, f2, f3).get(5, java.util.concurrent.TimeUnit.SECONDS);
                List<Libro> unidos = new ArrayList<>();
                unidos.addAll(f1.get());
                unidos.addAll(f2.get());
                unidos.addAll(f3.get());
                return unidos;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                return new ArrayList<Libro>();
            }
        }).thenAccept(resultados -> {
            // Volver al hilo de UI para mostrar resultados
            Platform.runLater(() -> {
                txtBusquedaOpenLibrary.setDisable(false);
                if (resultados.isEmpty()) {
                    mostrarAlerta("Sin resultados", "No se encontraron libros.");
                } else {
                    abrirVentanaResultados(resultados);
                }
            });
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
    private void buscarLibroParaDeseos(ActionEvent event) {
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
            notificar(resources.getString("status.no_covers_pending")); // O usa un texto directo si no tienes la clave
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
            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
            tablaLibros.refresh();
            actualizarPanelMisLibros();
            actualizarPanelDeseos();
            notificar("¡Completado! Se han actualizado " + task.getValue() + " portadas.");
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
     * Motor de búsqueda silencioso. Rastrea las 3 APIs.
     */
    private String buscarImagenEnApisMasivo(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        try {
            java.util.concurrent.CompletableFuture<java.util.List<Libro>> futureGoogle = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.GoogleBooksCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            java.util.concurrent.CompletableFuture<java.util.List<Libro>> futureOpenLib = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.OpenLibraryCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            java.util.concurrent.CompletableFuture<java.util.List<Libro>> futureInventaire = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.InventaireCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            java.util.concurrent.CompletableFuture.allOf(futureGoogle, futureOpenLib, futureInventaire).join();

            if (futureGoogle.get() != null) {
                for (Libro lib : futureGoogle.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
            if (futureOpenLib.get() != null) {
                for (Libro lib : futureOpenLib.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover") && !img.contains("-S.jpg")) {
                        return img.replace("-M.jpg", "-L.jpg");
                    }
                }
            }
            if (futureInventaire.get() != null) {
                for (Libro lib : futureInventaire.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // Falla en silencio y sigue con el siguiente libro
        }
        return "";
    }

    /**
     * Busca y asigna sagas a los libros que no tienen serie asignada. Muestra
     * un diálogo de confirmación y una barra de progreso durante la búsqueda.
     *
     */
    @FXML
    private void buscarSagasFaltantes(javafx.event.ActionEvent event) {
        busquedaSagas.buscarSagasFaltantes(listaLibrosCompleta, jsonManager, () -> {
            // Refrescar tabla principal
            tablaLibros.refresh();

            // Refrescar panel de sagas (usando el nombre nuevo sin tilde)
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            } else {
                System.err.println("Error: pestanaSagasController es null. Revisa el fx:id en primary.fxml");
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

        panelMisLibros.getChildren().clear();

        // 1. Obtener lo que el usuario ha escrito en el nuevo buscador
        String busquedaRapida = txtBuscarMisLibros != null ? txtBuscarMisLibros.getText().toLowerCase().trim() : "";

        // 2. Filtrar los libros aplicando la búsqueda rápida
        List<Libro> librosMostrados = filteredData.stream()
                .filter(Libro::isPoseido)
                .filter(l -> {
                    if (busquedaRapida.isEmpty()) {
                        return true;
                    }
                    // Buscar coincidencias en título o autor
                    boolean tituloCoincide = l.getTitulo() != null && l.getTitulo().toLowerCase().contains(busquedaRapida);
                    boolean autorCoincide = l.getAutor() != null && l.getAutor().toLowerCase().contains(busquedaRapida);
                    return tituloCoincide || autorCoincide;
                })
                .sorted((l1, l2) -> l1.getTitulo().compareToIgnoreCase(l2.getTitulo()))
                .collect(Collectors.toList());

        // 3. Comprobar si está vacío ANTES de dibujar
        if (librosMostrados.isEmpty()) {
            Label lblVacio = new Label("No hay libros aquí.\nPrueba a cambiar los filtros o añade libros nuevos.");
            lblVacio.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-alignment: center;");
            panelMisLibros.getChildren().add(lblVacio);
            return; // Salimos del método aquí
        }

        // 4. Dibujar las tarjetas si hay resultados
        for (Libro libro : librosMostrados) {
            panelMisLibros.getChildren().add(crearTarjetaMisLibros(libro));
        }
    }

    /**
     * Crea una tarjeta interactiva para la biblioteca principal.
     */
    private javafx.scene.layout.VBox crearTarjetaMisLibros(Libro libro) {
        javafx.scene.layout.VBox tarjeta = new javafx.scene.layout.VBox(8);
        tarjeta.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        tarjeta.setPrefWidth(140);
        tarjeta.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-cursor: hand;");

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
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #333;");

        // 3. Añadimos el contenedor (que lleva imagen + badge) en vez de solo la imagen
        tarjeta.getChildren().addAll(contenedorPortada, lblTitulo);
        return tarjeta;
    }

    private void notificar(String mensaje) {
        notificationPane.setText(mensaje);
        notificationPane.show();
        // Se oculta automáticamente tras 3 segundos
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> notificationPane.hide());
        delay.play();
    }
}
