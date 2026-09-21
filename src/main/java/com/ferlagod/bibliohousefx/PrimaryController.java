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
import com.bibliohouse.logic.BusquedaSagas;
import com.bibliohouse.logic.BusquedaService;
import com.bibliohouse.logic.ImportarExportarBD;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.LibroService;
import com.bibliohouse.logic.NextCloudSyncService;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.PrestamoService;
import com.bibliohouse.logic.ServicioEtiquetasFisicas;
import com.bibliohouse.logic.Socio;
import com.bibliohouse.utils.ImageLoader;
import com.bibliohouse.utils.UpdateChecker;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.controlsfx.control.NotificationPane;

/**
 * Controlador principal y orquestador de BiblioHouse. Coordina la carga de datos,
 * el menú global, los atajos de teclado y la comunicación entre los subcontroladores
 * modulares (Sidebar, Catálogo, Gestión de Libros, Préstamos, Historial, Wishlist y Sagas).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class PrimaryController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PrimaryController.class.getName());

    // --- ELEMENTOS FXML RAÍZ ---
    @FXML
    private NotificationPane notificationPane;
    @FXML
    private BorderPane mainContainer;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab tabPrestamos;
    @FXML
    private Tab tabHistorial;
    @FXML
    private HBox bannerPrestamos;
    @FXML
    private Label lblTextoBannerPrestamos;
    @FXML
    private Label lblEstado;

    // --- SUBCONTROLADORES INYECTADOS POR JAVAFX (vía fx:include) ---
    @FXML
    private SidebarController sidebarController;
    @FXML
    private CatalogoGridController catalogoGridController;
    @FXML
    private GestionLibrosController gestionLibrosController;
    @FXML
    private PrestamosController pestanaPrestamosController;
    @FXML
    private HistorialController pestanaHistorialController;
    @FXML
    private WishlistController pestanaWishlistController;
    @FXML
    private SagasController pestanaSagasController;

    // --- DATOS Y SERVICIOS ---
    private JsonManager jsonManager;
    private ResourceBundle resources;
    private String usuarioActual;
    private String rutaUsuario;
    private Map<String, String> preferencias = new HashMap<>();
    private int dueDaysLimit = 30;

    private ObservableList<Libro> listaLibrosCompleta;
    private ObservableList<Socio> listaSocios;
    private ObservableList<Prestamo> listaPrestamosCompleta;
    private List<Libro> listaDeseos;
    private FilteredList<Prestamo> filteredPrestamos;
    private FilteredList<Prestamo> filteredHistory;

    private Libro libroSeleccionado;
    private LibroService libroService;
    private PrestamoService prestamoService;
    private final BusquedaService busquedaService = new BusquedaService();
    private final BusquedaSagas busquedaSagas = new BusquedaSagas();
    private final ImportarExportarBD gestorArchivos = new ImportarExportarBD();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.resources = rb;

        // Suscribirse a eventos del bus desacoplado
        AppEventBus.getInstance().subscribe(AppEventBus.StatusMessageEvent.class, e -> setMensajeEstado(e.getMensaje()));
        AppEventBus.getInstance().subscribe(AppEventBus.LibroModificadoEvent.class, e -> {
            guardarLibrosEnDisco();
            actualizarComboLibrosDisponibles();
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
        });
        AppEventBus.getInstance().subscribe(AppEventBus.LibroEliminadoEvent.class, e -> {
            guardarLibrosEnDisco();
            actualizarComboLibrosDisponibles();
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
        });
        AppEventBus.getInstance().subscribe(AppEventBus.PrestamoModificadoEvent.class, e -> {
            actualizarVistasPrestamo();
            checkOverdueLoans();
        });
    }

    /**
     * Inicializa los datos de la aplicación y arranca la carga de colecciones.
     *
     * @param username Nombre del usuario autenticado.
     * @param userPath Ruta física al directorio de datos del usuario.
     */
    public void initData(String username, String userPath) {
        this.usuarioActual = username;
        this.rutaUsuario = userPath;

        String coversPath = this.rutaUsuario + File.separator + "covers";
        ImageLoader.setCacheDir(coversPath);

        this.jsonManager = new JsonManager(userPath);
        this.preferencias = jsonManager.cargarPreferencias();

        // Configuración de sincronización NextCloud si está configurada
        configurarNextCloudSync();

        // Cargar colecciones en segundo plano
        cargarDatos();

        // Aplicar estado maximizado
        aplicarPreferenciasGuardadas();

        // Configurar atajos de teclado globales
        setupShortcuts();

        // Mensaje de bienvenida
        if (resources != null && resources.containsKey("status.welcome")) {
            setMensajeEstado(MessageFormat.format(resources.getString("status.welcome"), username));
        } else {
            setMensajeEstado("Bienvenido, " + username);
        }

        // Comprobación de actualizaciones en segundo plano
        UpdateChecker.comprobarActualizaciones(versionNueva -> Platform.runLater(() -> notificar(
                "✨ Nueva versión disponible: BiblioHouse " + versionNueva + ". ¡Visita la web para descargarla!")));
    }

    private void configurarNextCloudSync() {
        String ncUrl = preferencias.getOrDefault("nextcloud.url", "");
        String ncUser = preferencias.getOrDefault("nextcloud.user", "");

        java.util.prefs.Preferences osPrefs = java.util.prefs.Preferences.userRoot().node("com/ferlagod/bibliohousefx/nextcloud");
        String ncPass = com.bibliohouse.utils.SeguridadUtil.desencriptar(osPrefs.get("password", ""));

        if (!ncUrl.isBlank() && !ncUser.isBlank() && !ncPass.isBlank()) {
            try {
                NextCloudSyncService syncService = new NextCloudSyncService(ncUrl, ncUser, ncPass);
                final String localDir = this.rutaUsuario;
                jsonManager.setAutoSyncTask(() -> {
                    try {
                        syncService.subirBaseDatos(localDir);
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Auto-sync fallido: {0}", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error al iniciar servicio de sincronización NextCloud", ex);
            }
        }
    }

    private void cargarDatos() {
        javafx.concurrent.Task<Void> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                List<Libro> deseos = jsonManager.cargarDeseos();
                List<Libro> libros = jsonManager.cargarLibros();
                List<Socio> socios = jsonManager.cargarSocios();
                List<Prestamo> prestamos = jsonManager.cargarPrestamos();

                Platform.runLater(() -> {
                    listaDeseos = deseos != null ? deseos : new ArrayList<>();
                    listaLibrosCompleta = FXCollections.observableArrayList(libros != null ? libros : new ArrayList<>());
                    listaSocios = FXCollections.observableArrayList(socios != null ? socios : new ArrayList<>());
                    listaPrestamosCompleta = FXCollections.observableArrayList(prestamos != null ? prestamos : new ArrayList<>());

                    libroService = new LibroService(jsonManager, listaLibrosCompleta);
                    prestamoService = new PrestamoService(jsonManager, listaPrestamosCompleta, listaLibrosCompleta);

                    filteredPrestamos = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() == null);
                    filteredHistory = new FilteredList<>(listaPrestamosCompleta, p -> p.getFechaDevolucion() != null);

                    // Inicializar subcontroladores con los datos cargados
                    if (sidebarController != null) {
                        sidebarController.initData(jsonManager, listaLibrosCompleta, preferencias, resources);
                    }
                    if (catalogoGridController != null) {
                        catalogoGridController.initData(PrimaryController.this, jsonManager, listaLibrosCompleta, listaDeseos, rutaUsuario, resources);
                    }
                    if (gestionLibrosController != null) {
                        gestionLibrosController.initData(PrimaryController.this, jsonManager, listaLibrosCompleta, busquedaService, rutaUsuario, resources);
                    }
                    if (pestanaWishlistController != null) {
                        pestanaWishlistController.initData(PrimaryController.this, jsonManager, listaDeseos, resources);
                    }
                    if (pestanaPrestamosController != null) {
                        pestanaPrestamosController.initData(PrimaryController.this, prestamoService, obtenerLibrosDisponibles(), listaSocios, filteredPrestamos, resources, dueDaysLimit);
                    }
                    if (pestanaHistorialController != null) {
                        pestanaHistorialController.initData(PrimaryController.this, filteredHistory);
                    }
                    if (pestanaSagasController != null) {
                        pestanaSagasController.initData(listaLibrosCompleta, () -> {
                            guardarLibrosEnDisco();
                            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
                        });
                    }

                    checkOverdueLoans();
                });
                return null;
            }
        };

        Thread thread = new Thread(loadTask, "CargaDatosBiblioHouse");
        thread.setDaemon(true);
        thread.start();
    }

    // =========================================================================
    // ATAJOS DE TECLADO Y PREFERENCIAS
    // =========================================================================

    private void setupShortcuts() {
        Platform.runLater(() -> {
            Window window = getWindow();
            if (window != null && window.getScene() != null) {
                Scene scene = window.getScene();
                // Ctrl+F -> Foco en búsqueda (Tab 1: Gestionar Libros)
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+F"), () -> {
                    if (mainTabPane != null) mainTabPane.getSelectionModel().select(1);
                });
                // Ctrl+L -> Pestaña Préstamos
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+L"), () -> {
                    if (mainTabPane != null && tabPrestamos != null) mainTabPane.getSelectionModel().select(tabPrestamos);
                });
            }
        });
    }

    private void aplicarPreferenciasGuardadas() {
        if (preferencias != null && preferencias.containsKey("maximized")) {
            boolean maximized = Boolean.parseBoolean(preferencias.get("maximized"));
            Platform.runLater(() -> {
                Window win = getWindow();
                if (win instanceof Stage stage) {
                    stage.setMaximized(maximized);
                }
            });
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS DE ACCESO Y COORDINACIÓN
    // =========================================================================

    public String getUsuarioActual() {
        return usuarioActual;
    }

    public String getRutaUsuario() {
        return rutaUsuario;
    }

    public ObservableList<Libro> getListaLibrosCompleta() {
        return listaLibrosCompleta;
    }

    public ObservableList<Socio> getListaSocios() {
        return listaSocios;
    }

    public ObservableList<Prestamo> getListaPrestamos() {
        return listaPrestamosCompleta;
    }

    public int getDueDaysLimit() {
        return dueDaysLimit;
    }

    public void setDueDaysLimit(int days) {
        this.dueDaysLimit = days;
        checkOverdueLoans();
    }

    public void seleccionarLibro(Libro libro) {
        this.libroSeleccionado = libro;
    }

    public Libro getLibroSeleccionado() {
        return libroSeleccionado;
    }

    public void setMensajeEstado(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
        }
    }

    public void notificar(String mensaje) {
        if (notificationPane != null) {
            notificationPane.setText(mensaje);
            notificationPane.show();
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> notificationPane.hide());
            delay.play();
        }
    }

    public Window getWindow() {
        if (mainContainer != null && mainContainer.getScene() != null) {
            return mainContainer.getScene().getWindow();
        }
        return null;
    }

    public void setScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();
    }

    public void mostrarAlertaPublic(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public void guardarLibrosEnDisco() {
        if (jsonManager != null && listaLibrosCompleta != null) {
            jsonManager.guardarLibrosDebounced(new ArrayList<>(listaLibrosCompleta));
        }
    }

    // =========================================================================
    // GESTIÓN DE PRÉSTAMOS Y SOCIOS
    // =========================================================================

    public ObservableList<Libro> obtenerLibrosDisponibles() {
        if (listaLibrosCompleta == null) {
            return FXCollections.observableArrayList();
        }
        return listaLibrosCompleta.filtered(l -> l.isPoseido() && l.getCantidad() > 0);
    }

    public void actualizarComboLibrosDisponibles() {
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.refrescarLibrosDisponibles(obtenerLibrosDisponibles());
        }
    }

    public void recargarDatosPrestamos() {
        List<Socio> socios = jsonManager.cargarSocios();
        listaSocios.clear();
        listaSocios.addAll(socios);
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.initData(this, prestamoService, obtenerLibrosDisponibles(), listaSocios, filteredPrestamos, resources, dueDaysLimit);
        }
    }

    public void actualizarVistasPrestamo() {
        if (filteredPrestamos != null) {
            filteredPrestamos.setPredicate(p -> p.getFechaDevolucion() == null);
        }
        if (filteredHistory != null) {
            filteredHistory.setPredicate(p -> p.getFechaDevolucion() != null);
        }
        actualizarComboLibrosDisponibles();
        if (catalogoGridController != null) {
            catalogoGridController.refrescarCuadricula();
        }
    }

    public void prepararPrestamoLibro(Libro libro) {
        if (libro == null) return;
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.seleccionarLibro(libro);
        }
    }

    public void prepararPrestamoSocio(Socio socio) {
        if (socio == null) return;
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.seleccionarSocio(socio);
        }
    }

    @FXML
    private void irAPrestamos(ActionEvent event) {
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }
    }

    private void checkOverdueLoans() {
        if (prestamoService == null || bannerPrestamos == null) {
            return;
        }
        List<Prestamo> overdueLoans = prestamoService.obtenerPrestamosVencidos(dueDaysLimit);
        Platform.runLater(() -> {
            if (!overdueLoans.isEmpty()) {
                bannerPrestamos.setVisible(true);
                bannerPrestamos.setManaged(true);
                bannerPrestamos.setMouseTransparent(false);
                if (overdueLoans.size() == 1) {
                    Prestamo p = overdueLoans.get(0);
                    lblTextoBannerPrestamos.setText("El libro '" + p.getTituloLibro() + "' prestado a " + p.getNombreSocio() + " está " + prestamoService.calcularDiasRetraso(p, dueDaysLimit) + " días retrasado.");
                } else {
                    lblTextoBannerPrestamos.setText("Tienes " + overdueLoans.size() + " libros pendientes de devolución cuyo plazo ha vencido.");
                }
            } else {
                bannerPrestamos.setVisible(false);
                bannerPrestamos.setManaged(false);
                bannerPrestamos.setMouseTransparent(true);
            }
        });
    }

    public void nuevoSocioPublic() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gestion_socios.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            GestionSociosController controller = loader.getController();
            controller.setListaSocios(listaSocios);

            Stage stage = new Stage();
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.showAndWait();

            Socio nuevo = controller.getSocioCreado();
            if (nuevo != null) {
                listaSocios.add(nuevo);
                jsonManager.guardarSocios(listaSocios);
                mostrarAlertaPublic("Éxito", "Socio añadido.");
            }
        } catch (IOException e) {
            mostrarAlertaPublic("Error Crítico", "No se pudo abrir la ventana de Nuevo Socio.\n" + e.getMessage());
        }
    }

    public void gestionarSociosPublic() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("socios_manager.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            SociosManagerController controller = loader.getController();
            controller.initData(listaSocios, jsonManager, this);

            Stage stage = new Stage();
            stage.setTitle("Gestionar Socios");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic("Error Crítico", "No se pudo abrir la ventana de Gestión de Socios.\n" + e.getMessage());
        }
    }

    // =========================================================================
    // WISHLIST Y DETALLE DE LIBROS
    // =========================================================================

    public void moverDeseoABiblioteca(Libro libro) {
        if (listaDeseos != null) {
            listaDeseos.remove(libro);
            jsonManager.guardarDeseos(listaDeseos);
        }
        libro.setPoseido(true);
        libro.setCantidad(1);
        if (listaLibrosCompleta != null && !listaLibrosCompleta.contains(libro)) {
            listaLibrosCompleta.add(libro);
            guardarLibrosEnDisco();
        }
        if (pestanaWishlistController != null) {
            pestanaWishlistController.actualizarPanelDeseos();
        }
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, true));
        setMensajeEstado(resources != null && resources.containsKey("wishlist.moved.status") ? resources.getString("wishlist.moved.status") : "Libro movido a la biblioteca.");
    }

    public void buscarLibroParaDeseos() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buscar Libro");
        dialog.setHeaderText("Añadir a Lista de Deseos");
        dialog.setContentText("Introduce el título, autor o ISBN:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            if (gestionLibrosController != null) {
                mainTabPane.getSelectionModel().select(1);
            }
        }
    }

    public void mostrarDetalleLibro(Libro libro) {
        if (libro == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("detalle_libro.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            DetalleLibroController controller = loader.getController();
            controller.setLibro(libro);
            controller.setListaGlobalEstanterias(jsonManager.cargarEstanterias());
            controller.setRutaUsuario(this.rutaUsuario);
            controller.setOnSyncRequested(() -> {
                jsonManager.guardarProgresoLecturaDebounced(libro.getId(), libro.getPaginaActual(), libro.getPaginasTotales());
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
            });

            Stage stage = new Stage();
            stage.setTitle("Detalles: " + libro.getTitulo());
            setScene(stage, root);
            stage.initOwner(getWindow());
            stage.centerOnScreen();
            stage.showAndWait();

            jsonManager.flushProgresoLectura();
            if (controller.isModified()) {
                guardarLibrosEnDisco();
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir detalle del libro", e);
        }
    }

    public void editarLibro(Libro libro) {
        if (libro == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editar_libro.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(libro);
            controller.setEstanteriasDisponibles(jsonManager.cargarEstanterias());
            controller.setRutaUsuario(this.rutaUsuario);

            Stage stage = new Stage();
            stage.setTitle("Editar: " + libro.getTitulo());
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMaximized(true);
            stage.showAndWait();

            if (controller.isGuardado()) {
                guardarLibrosEnDisco();
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                setMensajeEstado("Libro editado correctamente.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de edición", e);
        }
    }

    public void cambiarPortada(Libro libro) {
        if (libro == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Portada para: " + libro.getTitulo());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fileChooser.showOpenDialog(getWindow());
        if (file != null) {
            String rutaLocal = ImageLoader.hacerPortadaLocalOffline(file.getAbsolutePath(), libro.getId(), this.rutaUsuario);
            libro.setPortadaURL(rutaLocal);
            guardarLibrosEnDisco();
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
            mostrarAlertaPublic("Éxito", "Portada actualizada.");
        }
    }

    public void eliminarLibro(Libro libro) {
        if (libro == null) return;
        if (libro.getCantidad() > 1) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Gestión de Stock");
            alert.setHeaderText("Tienes " + libro.getCantidad() + " copias de este libro.");
            alert.setContentText("¿Qué deseas hacer?");
            ButtonType btnEliminarUno = new ButtonType("Eliminar solo 1 unidad");
            ButtonType btnEliminarTodo = new ButtonType("Borrar el libro entero");
            ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnEliminarUno, btnEliminarTodo, btnCancelar);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnEliminarUno) {
                    libroService.actualizarStock(libro, -1);
                    setMensajeEstado("Se ha eliminado una copia. Quedan: " + libro.getCantidad());
                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                } else if (result.get() == btnEliminarTodo) {
                    libroService.borrarTotalmente(libro);
                    setMensajeEstado("Libro eliminado definitivamente: " + libro.getTitulo());
                    AppEventBus.getInstance().publish(new AppEventBus.LibroEliminadoEvent(libro));
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Eliminar Libro");
            alert.setHeaderText("¿Estás seguro de que quieres borrar este libro?");
            alert.setContentText("Vas a eliminar: " + libro.getTitulo() + "\nEsta acción no se puede deshacer.");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                libroService.borrarTotalmente(libro);
                setMensajeEstado("Libro eliminado definitivamente: " + libro.getTitulo());
                AppEventBus.getInstance().publish(new AppEventBus.LibroEliminadoEvent(libro));
            }
        }
    }

    public void generarEtiquetaFisica(Libro libro) {
        if (libro == null) return;
        if (libro.isEsDigital()) {
            mostrarAlertaPublic("Sin libros físicos", "El libro seleccionado es digital. Las etiquetas solo se generan para libros físicos.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Etiqueta PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        fileChooser.setInitialFileName("Etiqueta_" + libro.getTitulo().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(getWindow());
        if (file != null) {
            try {
                ServicioEtiquetasFisicas.generarEtiquetasPDF(List.of(libro), file);
                notificar("PDF con etiqueta generado con éxito.");
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception ignored) {}
            } catch (Exception e) {
                mostrarAlertaPublic("Error", "Hubo un problema al generar la etiqueta: " + e.getMessage());
            }
        }
    }

    public void abrirLectorDigital(Libro libro) {
        if (libro == null || !libro.isEsDigital() || libro.getRutaArchivoDigital() == null) {
            mostrarAlertaPublic("Aviso", "Este libro no tiene un archivo digital enlazado.");
            return;
        }

        File archivo = new File(libro.getRutaArchivoDigital());
        if (!archivo.exists()) {
            mostrarAlertaPublic("Archivo no encontrado", "No se encuentra el archivo en: " + archivo.getAbsolutePath());
            return;
        }

        if (archivo.getName().toLowerCase().endsWith(".epub")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("lector_digital.fxml"));
                loader.setResources(this.resources);
                Parent root = loader.load();
                LectorDigitalController controller = loader.getController();
                controller.setLibro(libro);
                if (libro.getEstadoLecturaEnum() != com.bibliohouse.logic.EstadoLectura.LEYENDO) {
                    libro.setEstadoLecturaEnum(com.bibliohouse.logic.EstadoLectura.LEYENDO);
                }
                controller.setOnSyncRequested(() -> {
                    jsonManager.guardarProgresoLecturaDebounced(libro.getId(), libro.getPaginaActual(), libro.getPaginasTotales());
                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                });
                Stage stage = new Stage();
                stage.setTitle("Lector: " + libro.getTitulo());
                stage.setScene(new Scene(root, 900, 700));
                stage.centerOnScreen();
                stage.initOwner(getWindow());
                stage.setOnCloseRequest(e -> {
                    jsonManager.flushProgresoLectura();
                    controller.detenerServidor();
                });
                stage.show();
            } catch (Exception ex) {
                try {
                    java.awt.Desktop.getDesktop().open(archivo);
                } catch (Exception ignored) {}
            }
        } else {
            try {
                java.awt.Desktop.getDesktop().open(archivo);
            } catch (IOException ex) {
                mostrarAlertaPublic("Error", "No se pudo abrir el archivo digital.");
            }
        }
    }

    // =========================================================================
    // MENÚ ARCHIVO
    // =========================================================================

    @FXML
    private void importarBaseDatos(ActionEvent event) {
        gestorArchivos.importarBaseDatos(getWindow(), jsonManager, listaLibrosCompleta, () -> {
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, true));
            if (sidebarController != null) sidebarController.cargarListaEstanterias();
            actualizarComboLibrosDisponibles();
        });
    }

    @FXML
    private void exportarBaseDatos(ActionEvent event) {
        gestorArchivos.exportarBaseDatos(getWindow(), jsonManager, listaLibrosCompleta);
    }

    @FXML
    private void importarDesdeCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar biblioteca desde CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File archivoSeleccionado = fileChooser.showOpenDialog(getWindow());
        if (archivoSeleccionado != null) {
            List<Libro> librosImportados = com.bibliohouse.logic.ImportadorCSV.importar(archivoSeleccionado);
            if (librosImportados == null || librosImportados.isEmpty()) {
                mostrarAlertaPublic("Importación CSV", "No se encontraron libros válidos en el archivo.");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Importación completada");
            alert.setHeaderText("Se han encontrado " + librosImportados.size() + " libros en el archivo.");
            alert.setContentText("¿Deseas añadirlos a tu biblioteca actual?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                int anadidos = 0;
                for (Libro nuevo : librosImportados) {
                    boolean existe = listaLibrosCompleta.stream().anyMatch(l ->
                            (nuevo.getIsbn() != null && !nuevo.getIsbn().isEmpty() && nuevo.getIsbn().equals(l.getIsbn()))
                            || (nuevo.getTitulo().equalsIgnoreCase(l.getTitulo()) && nuevo.getAutor().equalsIgnoreCase(l.getAutor())));
                    if (!existe) {
                        listaLibrosCompleta.add(nuevo);
                        anadidos++;
                    }
                }
                guardarLibrosEnDisco();
                actualizarComboLibrosDisponibles();
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, true));
                mostrarAlertaPublic("Éxito", "Se han importado " + anadidos + " libros correctamente.");
            }
        }
    }

    @FXML
    private void exportarPDF(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exportar_pdf.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            ExportarPDFController controller = loader.getController();
            List<Libro> libros = new ArrayList<>(listaLibrosCompleta);
            List<String> estanterias = jsonManager.cargarEstanterias();
            List<String> generos = libros.stream().map(Libro::getGenero).filter(g -> g != null && !g.isEmpty()).distinct().sorted().collect(Collectors.toList());

            controller.setDatos(libros, estanterias, generos);

            Stage stage = new Stage();
            stage.setTitle("Exportar Informe PDF");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMinWidth(820);
            stage.setMinHeight(720);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo abrir la ventana de exportación PDF.\n" + e.getMessage());
        }
    }

    @FXML
    private void exportarWeb(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("exportar_web.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            ExportarWebController controller = loader.getController();
            List<Libro> libros = new ArrayList<>(listaLibrosCompleta);
            List<String> estanterias = jsonManager.cargarEstanterias();
            List<String> generos = libros.stream().map(Libro::getGenero).filter(g -> g != null && !g.isEmpty()).distinct().sorted().collect(Collectors.toList());

            controller.setDatos(libros, estanterias, generos);

            Stage stage = new Stage();
            stage.setTitle("Exportar Catálogo Web");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMinWidth(820);
            stage.setMinHeight(720);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo abrir la ventana de exportación web.\n" + e.getMessage());
        }
    }

    @FXML
    private void cerrarAplicacion(ActionEvent event) {
        Window win = getWindow();
        if (win instanceof Stage mainStage) {
            preferencias.put("maximized", String.valueOf(mainStage.isMaximized()));
            jsonManager.guardarPreferencias(preferencias);
        }
        Platform.exit();
    }

    // =========================================================================
    // MENÚ HERRAMIENTAS Y AYUDA
    // =========================================================================

    @FXML
    private void buscarDuplicados(ActionEvent event) {
        String reporte = libroService.buscarYFusionarDuplicados();
        if (reporte == null) {
            mostrarAlertaPublic("Búsqueda de Duplicados", "No se encontraron libros repetidos.");
            return;
        }
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("duplicados.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            DuplicadosController controller = loader.getController();
            controller.setTextoResultados(reporte);

            Stage stage = new Stage();
            stage.setTitle("Informe de Duplicados");
            setScene(stage, root);
            stage.show();
        } catch (IOException e) {
            mostrarAlertaPublic("Éxito", "Proceso completado. Revisa la tabla.");
        }
    }

    @FXML
    private void buscarPortadasFaltantes(ActionEvent event) {
        List<Libro> librosSinPortada = listaLibrosCompleta.stream()
                .filter(l -> l.getPortadaURL() == null || l.getPortadaURL().isEmpty() || l.getPortadaURL().contains("default_cover"))
                .collect(Collectors.toList());

        if (librosSinPortada.isEmpty()) {
            mostrarAlertaPublic("Búsqueda de Portadas", "No hay libros pendientes de portada.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Búsqueda masiva");
        confirmacion.setHeaderText("Se van a procesar " + librosSinPortada.size() + " libros.");
        confirmacion.setContentText("Este proceso conectará con servidores externos. ¿Deseas continuar?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                int actualizadas = 0;
                for (int i = 0; i < librosSinPortada.size(); i++) {
                    if (isCancelled()) break;
                    Libro libro = librosSinPortada.get(i);
                    updateMessage("Buscando: " + libro.getTitulo());
                    updateProgress(i + 1, librosSinPortada.size());

                    String query = (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) ? libro.getIsbn() : libro.getTitulo();
                    String urlEncontrada = busquedaService.buscarImagenEnApisMasivo(query);
                    if (urlEncontrada.isEmpty() && libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
                        urlEncontrada = busquedaService.buscarImagenEnApisMasivo(libro.getTitulo());
                    }

                    if (!urlEncontrada.isEmpty()) {
                        String rutaLocal = ImageLoader.hacerPortadaLocalOffline(urlEncontrada, libro.getId(), rutaUsuario);
                        libro.setPortadaURL(rutaLocal);
                        actualizadas++;
                    }
                    Thread.sleep(300);
                }
                return actualizadas;
            }
        };

        org.controlsfx.dialog.ProgressDialog progressDialog = new org.controlsfx.dialog.ProgressDialog(task);
        progressDialog.setTitle("BiblioHouse - Descarga de Portadas");
        progressDialog.setHeaderText("Procesando colección...");
        progressDialog.initOwner(getWindow());

        task.setOnSucceeded(e -> {
            guardarLibrosEnDisco();
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
            mostrarAlertaPublic("Búsqueda de Portadas", "¡Completado! Se han actualizado " + task.getValue() + " portadas.");
        });

        task.setOnFailed(e -> mostrarAlertaPublic("Error", "Ocurrió un error durante la descarga masiva."));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void buscarSagasFaltantes(ActionEvent event) {
        busquedaSagas.buscarSagasFaltantes(listaLibrosCompleta, jsonManager, () -> {
            guardarLibrosEnDisco();
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
        });
    }

    @FXML
    private void abrirConfiguracion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("configuracion.fxml"));
            loader.setResources(ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages", App.getCurrentLocale()));
            Parent root = loader.load();

            ConfiguracionController controller = loader.getController();
            controller.initData(jsonManager, this);

            Stage stage = new Stage();
            stage.setTitle("Configuración");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo abrir la configuración.");
        }
    }

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
            stage.initOwner(getWindow());
            stage.setMinWidth(750);
            stage.setMinHeight(600);
            stage.show();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo abrir la ventana de estadísticas.");
        }
    }

    @FXML
    private void mostrarAyudaManual(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("help.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Manual de Usuario");
            setScene(stage, root);
            stage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));
            stage.initModality(Modality.NONE);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo cargar el Manual de Ayuda.");
        }
    }

    @FXML
    private void mostrarAcercaDe(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("acercade.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Acerca de BiblioHouse");
            setScene(stage, root);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo abrir la ventana Acerca De.");
        }
    }

    // =========================================================================
    // CAMBIO DE IDIOMA (I18N)
    // =========================================================================

    @FXML private void cambiarAEspanol() { cambiarIdioma("es"); }
    @FXML private void cambiarAIngles() { cambiarIdioma("en"); }
    @FXML private void cambiarACatalan() { cambiarIdioma("ca"); }
    @FXML private void cambiarAGallego() { cambiarIdioma("gl"); }
    @FXML private void cambiarAEuskera() { cambiarIdioma("eu"); }
    @FXML private void cambiarAPortugues() { cambiarIdioma("pt"); }

    private void cambiarIdioma(String codigoLang) {
        try {
            com.bibliohouse.logic.LanguageManager.setLocale(codigoLang);
            Window win = getWindow();
            if (win instanceof Stage currentStage) {
                App.reloadUI(currentStage, this.usuarioActual, this.rutaUsuario);
            }
        } catch (IOException e) {
            mostrarAlertaPublic("Error", "No se pudo cambiar el idioma correctamente.");
        }
    }

    // =========================================================================
    // SOPORTE PARA COMBOBOX FILTRABLES (Para PrestamosController)
    // =========================================================================

    public <T> void setupFilteringComboBoxPublic(ComboBox<T> comboBox, java.util.function.Function<T, String> displayFunc) {
        if (comboBox == null) return;
        comboBox.setEditable(true);
        ObservableList<T> originalItems = FXCollections.observableArrayList(comboBox.getItems());

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            ObservableList<T> sourceList;
            if ("comboLibrosPrestamo".equals(comboBox.getId())) {
                sourceList = (ObservableList<T>) obtenerLibrosDisponibles();
            } else if ("comboSocios".equals(comboBox.getId())) {
                sourceList = (ObservableList<T>) listaSocios;
            } else {
                sourceList = originalItems;
            }

            if (newText == null || newText.isEmpty()) {
                comboBox.setItems(sourceList);
                return;
            }

            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null && displayFunc.apply(selected).equals(newText)) {
                return;
            }

            FilteredList<T> filtered = new FilteredList<>(sourceList, item -> {
                String itemText = displayFunc.apply(item).toLowerCase();
                return itemText.contains(newText.toLowerCase());
            });

            comboBox.setItems(filtered);
            if (!filtered.isEmpty() && !comboBox.isShowing()) {
                Platform.runLater(comboBox::show);
            }
        });

        comboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : displayFunc.apply(object);
            }

            @Override
            public T fromString(String string) {
                return comboBox.getItems().stream()
                        .filter(item -> displayFunc.apply(item).equalsIgnoreCase(string))
                        .findFirst().orElse(null);
            }
        });
    }
}
