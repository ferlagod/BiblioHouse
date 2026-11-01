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


import com.bibliohouse.logic.LanguageManager;
import com.bibliohouse.logic.AppEventBus;
import com.bibliohouse.logic.BusquedaSagas;
import com.bibliohouse.logic.BusquedaService;
import com.bibliohouse.logic.EbookMetadataService;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
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
import com.bibliohouse.utils.SystemNotificationService;

/**
 * Controlador principal y orquestador de BiblioHouse. Coordina la carga de
 * datos, el menú global, los atajos de teclado y la comunicación entre los
 * subcontroladores modulares (Sidebar, Catálogo, Gestión de Libros, Préstamos,
 * Historial, Wishlist y Sagas).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class PrimaryController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PrimaryController.class.getName());

    // --- ELEMENTOS FXML RAÍZ ---
    @FXML
    private NotificationPane notificationPane;
    @FXML
    private BorderPane mainContainer;
    @FXML
    private MenuBar menuBarPrincipal;
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
    @FXML
    private Button btnSyncRapido;

    private Label lblSyncIcon;
    private RotateTransition syncRotateTransition;

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
    private final AtomicBoolean busquedaPortadasEnCurso = new AtomicBoolean(false);

    /**
     * Si la última sincronización automática falló, para no repetir la
     * notificación de error en cada intento.
     */
    private final AtomicBoolean autoSyncFallando = new AtomicBoolean(false);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.resources = rb;
        configurarIntegracionSistema();

        // Suscribirse a eventos del bus desacoplado
        AppEventBus.getInstance().subscribe(this, AppEventBus.StatusMessageEvent.class, e -> setMensajeEstado(e.getMensaje()));
        AppEventBus.getInstance().subscribe(this, AppEventBus.LibroModificadoEvent.class, e -> {
            guardarLibrosEnDisco();
            actualizarComboLibrosDisponibles();
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
        });
        AppEventBus.getInstance().subscribe(this, AppEventBus.LibroEliminadoEvent.class, e -> {
            guardarLibrosEnDisco();
            actualizarComboLibrosDisponibles();
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
        });
        AppEventBus.getInstance().subscribe(this, AppEventBus.PrestamoModificadoEvent.class, e -> {
            actualizarVistasPrestamo();
            checkOverdueLoans();
        });
        AppEventBus.getInstance().subscribe(this, AppEventBus.CatalogoSincronizadoEvent.class, e -> {
            actualizarComboLibrosDisponibles();
            if (pestanaSagasController != null) {
                pestanaSagasController.initData(listaLibrosCompleta);
            }
            if (pestanaWishlistController != null) {
                pestanaWishlistController.actualizarPanelDeseos();
            }
            checkOverdueLoans();
        });

        // Configurar botón de sincronización rápida superior
        if (btnSyncRapido != null) {
            lblSyncIcon = new Label("🔄");
            lblSyncIcon.setStyle("-fx-font-size: 11px;");
            btnSyncRapido.setGraphic(lblSyncIcon);
            btnSyncRapido.setText(LanguageManager.getString("ui.sincronizar", " Sincronizar"));
        }
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
        configurarNextCloudSync(true);

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
            setMensajeEstado(LanguageManager.getString("ui.bienvenido", "Bienvenido, ") + username);
        }

        // Comprobación de actualizaciones en segundo plano
        UpdateChecker.comprobarActualizaciones(versionNueva -> Platform.runLater(() -> notificar(
                LanguageManager.getString("ui.nueva_version_disponible_bibliohouse", "✨ Nueva versión disponible: BiblioHouse ") + versionNueva + LanguageManager.getString("ui.visita_la_web_para_descargarla", ". ¡Visita la web para descargarla!"))));
    }

    /**
     * Vuelve a leer la configuración de NextCloud guardada (p. ej. tras
     * cambiarla en Configuración) y reprograma la sincronización automática.
     * Actualiza también las preferencias en memoria para que al salir no se
     * sobrescriban los valores nuevos con los antiguos.
     */
    public void reconfigurarNextCloudSync() {
        Map<String, String> enDisco = jsonManager.cargarPreferencias();
        preferencias.put("nextcloud.url", enDisco.getOrDefault("nextcloud.url", ""));
        preferencias.put("nextcloud.user", enDisco.getOrDefault("nextcloud.user", ""));
        preferencias.remove("nextcloud.password");
        jsonManager.setAutoSyncTask(null);
        configurarNextCloudSync(false);
    }

    /**
     * Recarga las colecciones tras una sincronización que ha cambiado los
     * datos locales y avisa al resto de vistas.
     *
     * @param mensaje Mensaje de estado a mostrar.
     */
    private void recargarTrasSincronizacion(String mensaje) {
        cargarDatos(() -> {
            int total = listaLibrosCompleta != null ? listaLibrosCompleta.size() : 0;
            AppEventBus.getInstance().publish(new AppEventBus.CatalogoSincronizadoEvent(total));
            AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(mensaje));
        });
    }

    private void configurarNextCloudSync(boolean pullInicial) {
        String ncUrl = preferencias.getOrDefault("nextcloud.url", "");
        String ncUser = preferencias.getOrDefault("nextcloud.user", "");

        java.util.prefs.Preferences osPrefs = java.util.prefs.Preferences.userRoot().node("com/ferlagod/bibliohousefx/nextcloud");
        String ncPass = com.bibliohouse.utils.SeguridadUtil.desencriptar(osPrefs.get("password", ""));

        if (!ncUrl.isBlank() && !ncUser.isBlank() && !ncPass.isBlank()) {
            try {
                NextCloudSyncService syncService = new NextCloudSyncService(ncUrl, ncUser, ncPass);
                syncService.setJsonManager(jsonManager);
                final String localDir = this.rutaUsuario;
                jsonManager.setAutoSyncTask(() -> {
                    try {
                        boolean cambiosRemotos = syncService.subirBaseDatos(localDir);
                        autoSyncFallando.set(false);
                        // Sincronización automática correcta: aviso discreto en la barra
                        // de estado, sin notificación del sistema
                        Platform.runLater(() -> {
                            if (cambiosRemotos) {
                                recargarTrasSincronizacion(LanguageManager.getString("ui.biblioteca_actualizada_con_cambios_nextcloud",
                                        "Biblioteca actualizada con cambios de NextCloud."));
                            } else {
                                setMensajeEstado(LanguageManager.getString("ui.copia_de_seguridad_en_la_nube",
                                        "Copia de seguridad en la nube completada con éxito."));
                            }
                        });
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Auto-sync fallido: {0}", ex.getMessage());
                        // Solo se notifica el primer fallo seguido (p. ej. sin conexión)
                        if (!autoSyncFallando.getAndSet(true)) {
                            Platform.runLater(() -> {
                                SystemNotificationService.notificarAlerta(LanguageManager.getString("ui.sincronizacion_nextcloud", "Sincronización NextCloud"), LanguageManager.getString("ui.error_al_sincronizar_con_nextcloud", "Error al sincronizar con NextCloud: ") + ex.getMessage());
                            });
                        }
                    }
                });

                // Pull on Startup: Comprobación automática en segundo plano sin bloquear la UI
                if (pullInicial) {
                    iniciarPullOnStartup(syncService, localDir);
                }

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error al iniciar servicio de sincronización NextCloud", ex);
            }
        }
    }

    /**
     * Comprueba en segundo plano si existen cambios más recientes en NextCloud
     * al iniciar. Si los hay, descarga los datos actualizados y las nuevas
     * portadas sin bloquear la interfaz, recarga las colecciones y notifica a
     * través de AppEventBus.
     *
     * @param syncService Servicio de sincronización configurado con NextCloud.
     * @param localDir Directorio local de datos del usuario.
     */
    private void iniciarPullOnStartup(NextCloudSyncService syncService, String localDir) {
        Platform.runLater(this::iniciarAnimacionSincronizacion);
        Thread pullThread = new Thread(() -> {
            try {
                LOGGER.info("NextCloud: Comprobando en segundo plano si hay modificaciones remotas al iniciar...");
                boolean huboCambios = syncService.sincronizarSiRemotoMasReciente(localDir);
                if (huboCambios) {
                    LOGGER.info("NextCloud: Cambios remotos detectados y descargados. Actualizando catálogo...");
                    Platform.runLater(() -> {
                        cargarDatos(() -> {
                            int total = listaLibrosCompleta != null ? listaLibrosCompleta.size() : 0;
                            AppEventBus.getInstance().publish(new AppEventBus.CatalogoSincronizadoEvent(total));
                            AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(
                                    LanguageManager.getString("ui.biblioteca_sincronizada_automaticamente_desde_nextcloud", "Biblioteca sincronizada automáticamente desde NextCloud.")));
                            SystemNotificationService.notificarInfo(LanguageManager.getString("ui.sincronizacion_nextcloud", "Sincronización NextCloud"),
                                    LanguageManager.getString("ui.se_han_sincronizado_nuevos_cambios_remotos", "Se han sincronizado nuevos cambios remotos en tu biblioteca."));
                            detenerAnimacionSincronizacion(true);
                        });
                    });
                } else {
                    LOGGER.info("NextCloud: Biblioteca local ya se encuentra al día.");
                    Platform.runLater(() -> detenerAnimacionSincronizacion(true));
                }
            } catch (IOException ex) {
                // Silencioso para no bloquear ni molestar al usuario si no hay conexión a internet
                LOGGER.log(Level.INFO, "Sincronización al inicio omitida (sin conexión o servidor no disponible): {0}", ex.getMessage());
                Platform.runLater(() -> detenerAnimacionSincronizacion(false));
            }
        }, "NextCloud-PullOnStartup");
        pullThread.setDaemon(true);
        pullThread.start();
    }

    /**
     * Ejecuta una sincronización bidireccional manual completa al pulsar el
     * botón de la barra superior. Sube cambios locales, descarga cambios
     * remotos y nuevas portadas con animación visual.
     */
    @FXML
    public void sincronizarNextCloudManual() {
        String ncUrl = preferencias.getOrDefault("nextcloud.url", "");
        String ncUser = preferencias.getOrDefault("nextcloud.user", "");
        java.util.prefs.Preferences osPrefs = java.util.prefs.Preferences.userRoot().node("com/ferlagod/bibliohousefx/nextcloud");
        String ncPass = com.bibliohouse.utils.SeguridadUtil.desencriptar(osPrefs.get("password", ""));

        if (ncUrl.isBlank() || ncUser.isBlank() || ncPass.isBlank()) {
            SystemNotificationService.notificarAlerta(LanguageManager.getString("ui.nextcloud_no_configurado", "NextCloud no configurado"),
                    LanguageManager.getString("ui.configura_tu_cuenta_nextcloud_en_herramientas", "Configura tu cuenta NextCloud en Herramientas -> Configuración para sincronizar."));
            abrirConfiguracion();
            return;
        }

        iniciarAnimacionSincronizacion();

        Thread syncThread = new Thread(() -> {
            try {
                NextCloudSyncService syncService = new NextCloudSyncService(ncUrl, ncUser, ncPass);
                syncService.setJsonManager(jsonManager);
                final String localDir = this.rutaUsuario;

                // 1. Subir cambios locales pendientes
                syncService.subirBaseDatos(localDir);

                // 2. Descargar cambios remotos y portadas
                syncService.descargarBaseDatos(localDir);

                Platform.runLater(() -> {
                    cargarDatos(() -> {
                        int total = listaLibrosCompleta != null ? listaLibrosCompleta.size() : 0;
                        AppEventBus.getInstance().publish(new AppEventBus.CatalogoSincronizadoEvent(total));
                        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(
                                LanguageManager.getString("ui.sincronizacion_con_nextcloud_completada_con_exito", "Sincronización con NextCloud completada con éxito.")));
                        SystemNotificationService.notificarInfo(LanguageManager.getString("ui.nextcloud_sincronizado", "NextCloud Sincronizado"),
                                LanguageManager.getString("ui.tu_biblioteca_esta_completamente_sincronizada_con", "Tu biblioteca está completamente sincronizada con la nube."));
                        detenerAnimacionSincronizacion(true);
                    });
                });
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error en sincronización manual rápida: {0}", ex.getMessage());
                Platform.runLater(() -> {
                    SystemNotificationService.notificarAlerta(LanguageManager.getString("ui.error_de_sincronizacion", "Error de Sincronización"),
                            LanguageManager.getString("ui.no_se_pudo_sincronizar_con_nextcloud", "No se pudo sincronizar con NextCloud: ") + ex.getMessage());
                    detenerAnimacionSincronizacion(false);
                });
            }
        }, "NextCloud-ManualSync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    /**
     * Inicia la animación de rotación continua del icono del botón de
     * sincronización rápida.
     */
    public void iniciarAnimacionSincronizacion() {
        if (btnSyncRapido == null) {
            return;
        }
        btnSyncRapido.setDisable(true);
        btnSyncRapido.getStyleClass().removeAll("sync-success", "sync-error");
        btnSyncRapido.getStyleClass().add("syncing");
        btnSyncRapido.setText(LanguageManager.getString("ui.sincronizando", " Sincronizando..."));
        if (lblSyncIcon != null) {
            lblSyncIcon.setText("🔄");
            if (syncRotateTransition == null) {
                syncRotateTransition = new RotateTransition(Duration.millis(800), lblSyncIcon);
                syncRotateTransition.setByAngle(360);
                syncRotateTransition.setCycleCount(Animation.INDEFINITE);
                syncRotateTransition.setInterpolator(Interpolator.LINEAR);
            }
            syncRotateTransition.playFromStart();
        }
    }

    /**
     * Detiene la animación de sincronización mostrando el estado final (éxito o
     * aviso de reintento).
     *
     * @param exito true si la operación terminó con éxito, false si falló.
     */
    public void detenerAnimacionSincronizacion(boolean exito) {
        if (btnSyncRapido == null) {
            return;
        }
        if (syncRotateTransition != null) {
            syncRotateTransition.stop();
        }
        if (lblSyncIcon != null) {
            lblSyncIcon.setRotate(0);
        }
        btnSyncRapido.getStyleClass().remove("syncing");

        if (exito) {
            btnSyncRapido.getStyleClass().add("sync-success");
            btnSyncRapido.setText(LanguageManager.getString("ui.al_dia", " Al día"));
            if (lblSyncIcon != null) {
                lblSyncIcon.setText("✓");
            }
        } else {
            btnSyncRapido.getStyleClass().add("sync-error");
            btnSyncRapido.setText(LanguageManager.getString("ui.reintentar", " Reintentar"));
            if (lblSyncIcon != null) {
                lblSyncIcon.setText("⚠️");
            }
        }

        PauseTransition resetPause = new PauseTransition(Duration.seconds(exito ? 2.5 : 3.5));
        resetPause.setOnFinished(e -> {
            btnSyncRapido.getStyleClass().removeAll("sync-success", "sync-error", "syncing");
            btnSyncRapido.setText(LanguageManager.getString("ui.sincronizar", " Sincronizar"));
            if (lblSyncIcon != null) {
                lblSyncIcon.setText("🔄");
                lblSyncIcon.setRotate(0);
            }
            btnSyncRapido.setDisable(false);
        });
        resetPause.play();
    }

    private void cargarDatos() {
        cargarDatos(null);
    }

    private void cargarDatos(Runnable onCompletado) {
        javafx.concurrent.Task<Void> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                List<Libro> deseos = jsonManager.cargarDeseos();
                List<Libro> libros = jsonManager.cargarLibros();
                List<Socio> socios = jsonManager.cargarSocios();
                List<Prestamo> prestamos = jsonManager.cargarPrestamos();

                Platform.runLater(() -> {
                    listaDeseos = deseos != null ? deseos : new ArrayList<>();
                    if (listaLibrosCompleta == null) {
                        listaLibrosCompleta = FXCollections.observableArrayList(libros != null ? libros : new ArrayList<>());
                    } else {
                        listaLibrosCompleta.setAll(libros != null ? libros : java.util.Collections.emptyList());
                    }
                    if (listaSocios == null) {
                        listaSocios = FXCollections.observableArrayList(socios != null ? socios : new ArrayList<>());
                    } else {
                        listaSocios.setAll(socios != null ? socios : java.util.Collections.emptyList());
                    }
                    if (listaPrestamosCompleta == null) {
                        listaPrestamosCompleta = FXCollections.observableArrayList(prestamos != null ? prestamos : new ArrayList<>());
                    } else {
                        listaPrestamosCompleta.setAll(prestamos != null ? prestamos : java.util.Collections.emptyList());
                    }

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

                    if (onCompletado != null) {
                        onCompletado.run();
                    }
                });
                return null;
            }
        };

        com.bibliohouse.utils.Hilos.ejecutar(loadTask);
    }

    // =========================================================================
    // ATAJOS DE TECLADO Y PREFERENCIAS
    // =========================================================================
    private void configurarIntegracionSistema() {
        if (menuBarPrincipal != null) {
            // Se mantiene dentro de la ventana de la app para que siempre muestre la marca BiblioHouse
            // y evitar que macOS reemplace el nombre del menú por el ejecutable del sistema ("java").
            menuBarPrincipal.useSystemMenuBarProperty().set(false);
        }
        SystemNotificationService.inicializar();
    }

    private void setupShortcuts() {
        Platform.runLater(() -> {
            Window window = getWindow();
            if (window != null && window.getScene() != null) {
                Scene scene = window.getScene();
                // Cmd+K / Ctrl+K -> Paleta de comandos global Spotlight/Raycast
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+K"), this::abrirPaletaComandos);
                // Cmd+, / Ctrl+, -> Preferencias y configuración
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+,"), this::abrirConfiguracion);
                // Cmd+N / Ctrl+N -> Añadir nuevo libro / Gestión de libros
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+N"), this::irAGestionLibros);
                // Cmd+F / Ctrl+F -> Foco en búsqueda (Tab 1: Gestionar Libros)
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+F"), () -> {
                    seleccionarPestanaDirecta(1);
                });
                // Cmd+L / Ctrl+L -> Pestaña Préstamos
                scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+L"), () -> {
                    seleccionarPestanaDirecta(2);
                });
                // Cmd+1 a Cmd+6 -> Navegación directa instantánea entre pestañas
                for (int i = 1; i <= 6; i++) {
                    final int tabIdx = i - 1;
                    scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+" + i), () -> {
                        seleccionarPestanaDirecta(tabIdx);
                    });
                }
            }
        });
    }

    @FXML
    public void abrirPaletaComandos() {
        PaletaComandosDialog.mostrar(this);
    }

    @FXML
    public void irAGestionLibros() {
        seleccionarPestanaDirecta(1);
    }

    public void abrirEscaner() {
        seleccionarPestanaDirecta(1);
        if (gestionLibrosController != null) {
            gestionLibrosController.abrirEscaner(null);
        }
    }

    public void seleccionarPestanaDirecta(int index) {
        if (mainTabPane != null && index >= 0 && index < mainTabPane.getTabs().size()) {
            mainTabPane.getSelectionModel().select(index);
        }
    }

    public void abrirDetalleLibroDirecto(Libro libro) {
        if (libro == null) {
            return;
        }
        this.libroSeleccionado = libro;
        seleccionarPestanaDirecta(1);
        if (gestionLibrosController != null) {
            gestionLibrosController.rellenarFormularioManual(libro);
        }
    }

    public void abrirConfiguracion() {
        abrirConfiguracion(null);
    }

    public void mostrarEstadisticas() {
        mostrarEstadisticas(null);
    }

    public void exportarPDF() {
        exportarPDF(null);
    }

    public void exportarWeb() {
        exportarWeb(null);
    }

    public void buscarDuplicados() {
        buscarDuplicados(null);
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

    /**
     * Establece el límite de días permitidos para un préstamo antes de
     * considerarlo vencido.
     *
     * @param days Número de días de margen.
     */
    public void setDueDaysLimit(int days) {
        this.dueDaysLimit = days;
        checkOverdueLoans();
    }

    /**
     * Establece el libro actualmente seleccionado por el usuario en cualquiera
     * de las vistas.
     *
     * @param libro Libro seleccionado.
     */
    public void seleccionarLibro(Libro libro) {
        this.libroSeleccionado = libro;
    }

    /**
     * Obtiene el libro actualmente seleccionado en la aplicación.
     *
     * @return Libro seleccionado, o {@code null} si ninguno está seleccionado.
     */
    public Libro getLibroSeleccionado() {
        return libroSeleccionado;
    }

    /**
     * Actualiza el texto mostrado en la barra de estado inferior de la ventana
     * principal.
     *
     * @param mensaje Mensaje de estado.
     */
    public void setMensajeEstado(String mensaje) {
        if (lblEstado != null) {
            if (Platform.isFxApplicationThread()) {
                lblEstado.setText(mensaje);
            } else {
                Platform.runLater(() -> lblEstado.setText(mensaje));
            }
        }
    }

    /**
     * Muestra una notificación emergente temporal animada en la parte inferior
     * de la ventana.
     *
     * @param mensaje Texto descriptivo a notificar.
     */
    public void notificar(String mensaje) {
        if (notificationPane != null) {
            notificationPane.setText(mensaje);
            notificationPane.show();
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> notificationPane.hide());
            delay.play();
        }
        SystemNotificationService.notificarInfo("BiblioHouse", mensaje);
    }

    /**
     * Obtiene la ventana nativa (Window / Stage) contenedora de la escena
     * principal.
     *
     * @return Instancia de {@link Window} o {@code null} si aún no está
     * asociada.
     */
    public Window getWindow() {
        if (mainContainer != null && mainContainer.getScene() != null) {
            return mainContainer.getScene().getWindow();
        }
        return null;
    }

    /**
     * Configura la escena en el escenario especificado aplicando la hoja de
     * estilos global.
     *
     * @param stage Escenario (Stage) a configurar.
     * @param root Nodo raíz cargado del FXML.
     */
    public void setScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        App.registerScene(scene);
        stage.setScene(scene);
        stage.sizeToScene();
    }

    /**
     * Muestra una alerta informativa garantizando su ejecución en el hilo
     * gráfico de JavaFX.
     *
     * @param titulo Título de la ventana de diálogo.
     * @param mensaje Contenido textual de la alerta.
     */
    public void mostrarAlertaPublic(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    /**
     * Solicita la persistencia en disco de los libros en segundo plano con
     * control de concurrencia debounced.
     */
    public void guardarLibrosEnDisco() {
        if (jsonManager != null && listaLibrosCompleta != null) {
            jsonManager.guardarLibrosDebounced(new ArrayList<>(listaLibrosCompleta));
        }
    }

    // =========================================================================
    // GESTIÓN DE PRÉSTAMOS Y SOCIOS
    // =========================================================================
    /**
     * Obtiene una sublista filtrada con los libros poseídos que tienen stock
     * disponible (> 0).
     *
     * @return Lista observable de libros disponibles para prestar.
     */
    public ObservableList<Libro> obtenerLibrosDisponibles() {
        if (listaLibrosCompleta == null) {
            return FXCollections.observableArrayList();
        }
        return listaLibrosCompleta.filtered(l -> l.isPoseido() && l.getCantidad() > 0);
    }

    /**
     * Refresca el desplegable de libros disponibles en el formulario de
     * préstamos.
     */
    public void actualizarComboLibrosDisponibles() {
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.refrescarLibrosDisponibles(obtenerLibrosDisponibles());
        }
    }

    /**
     * Recarga la lista de socios desde el archivo JSON y refresca el
     * controlador de préstamos.
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
     * Actualiza los predicados de filtrado de préstamos activos e históricos y
     * refresca la cuadrícula.
     */
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

    /**
     * Cambia a la pestaña de préstamos y preselecciona el libro indicado en el
     * formulario.
     *
     * @param libro Libro para el que se va a emitir un préstamo.
     */
    public void prepararPrestamoLibro(Libro libro) {
        if (libro == null) {
            return;
        }
        if (mainTabPane != null && tabPrestamos != null) {
            mainTabPane.getSelectionModel().select(tabPrestamos);
        }
        if (pestanaPrestamosController != null) {
            pestanaPrestamosController.seleccionarLibro(libro);
        }
    }

    /**
     * Cambia a la pestaña de préstamos y preselecciona el socio indicado en el
     * formulario.
     *
     * @param socio Socio para el que se va a emitir un préstamo.
     */
    public void prepararPrestamoSocio(Socio socio) {
        if (socio == null) {
            return;
        }
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
                String msg;
                if (overdueLoans.size() == 1) {
                    Prestamo p = overdueLoans.get(0);
                    msg = "El libro '" + p.getTituloLibro() + "' prestado a " + p.getNombreSocio() + " está " + prestamoService.calcularDiasRetraso(p, dueDaysLimit) + " días retrasado.";
                } else {
                    msg = "Tienes " + overdueLoans.size() + " libros pendientes de devolución cuyo plazo ha vencido.";
                }
                lblTextoBannerPrestamos.setText(msg);
                SystemNotificationService.notificarAlerta(LanguageManager.getString("ui.prestamos_vencidos", "Préstamos Vencidos"), msg);
            } else {
                bannerPrestamos.setVisible(false);
                bannerPrestamos.setManaged(false);
                bannerPrestamos.setMouseTransparent(true);
            }
        });
    }

    /**
     * Despliega el diálogo modal para crear y registrar un nuevo socio en la
     * base de datos local.
     */
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
                mostrarAlertaPublic(LanguageManager.getString("ui.exito", "Éxito"), LanguageManager.getString("ui.socio_anadido", "Socio añadido."));
            }
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error_critico", "Error Crítico"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana", "No se pudo abrir la ventana de Nuevo Socio.\n") + e.getMessage());
        }
    }

    /**
     * Despliega la ventana de administración integral de socios, permitiendo
     * editar, eliminar o ver el historial de préstamos asociados.
     */
    public void gestionarSociosPublic() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("socios_manager.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            SociosManagerController controller = loader.getController();
            controller.initData(listaSocios, jsonManager, this);

            Stage stage = new Stage();
            stage.setTitle(LanguageManager.getString("ui.gestionar_socios", "Gestionar Socios"));
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error_critico", "Error Crítico"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana_2", "No se pudo abrir la ventana de Gestión de Socios.\n") + e.getMessage());
        }
    }

    // =========================================================================
    // WISHLIST Y DETALLE DE LIBROS
    // =========================================================================
    /**
     * Traslada un libro de la lista de deseos a la biblioteca personal como
     * libro poseído (stock 1) y actualiza ambas listas en disco y en el bus de
     * eventos.
     *
     * @param libro Libro deseado adquirido por el usuario.
     */
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

    /**
     * Solicita una búsqueda rápida de libros para incorporar directamente a la
     * lista de deseos.
     */
    public void buscarLibroParaDeseos() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(LanguageManager.getString("ui.buscar_libro", "Buscar Libro"));
        dialog.setHeaderText(LanguageManager.getString("ui.anadir_a_lista_de_deseos", "Añadir a Lista de Deseos"));
        dialog.setContentText(LanguageManager.getString("ui.introduce_el_titulo_autor_o_isbn", "Introduce el título, autor o ISBN:"));
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            if (gestionLibrosController != null) {
                mainTabPane.getSelectionModel().select(1);
            }
        }
    }

    /**
     * Abre la ventana modal de visualización detallada del libro con ficha
     * completa, notas de lectura, avance y portadas.
     *
     * @param libro Libro del que se mostrarán los detalles.
     */
    public void mostrarDetalleLibro(Libro libro) {
        if (libro == null) {
            return;
        }
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
            stage.setTitle(LanguageManager.getString("ui.detalles", "Detalles: ") + libro.getTitulo());
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

    /**
     * Despliega la ventana modal de edición de metadatos de un libro.
     *
     * @param libro Libro a editar.
     */
    public void editarLibro(Libro libro) {
        if (libro == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editar_libro.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(libro);
            controller.setEstanteriasDisponibles(jsonManager.cargarEstanterias());
            controller.setRutaUsuario(this.rutaUsuario);

            Stage stage = new Stage();
            stage.setTitle(LanguageManager.getString("ui.editar", "Editar: ") + libro.getTitulo());
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMaximized(true);
            stage.showAndWait();

            if (controller.isGuardado()) {
                guardarLibrosEnDisco();
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                setMensajeEstado(LanguageManager.getString("ui.libro_editado_correctamente", "Libro editado correctamente."));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de edición", e);
        }
    }

    /**
     * Permite seleccionar una nueva imagen de portada desde el explorador de
     * archivos y la asocia al libro indicado.
     *
     * @param libro Libro al que se le cambiará la portada.
     */
    public void cambiarPortada(Libro libro) {
        if (libro == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getString("ui.seleccionar_portada_para", "Seleccionar Portada para: ") + libro.getTitulo());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fileChooser.showOpenDialog(getWindow());
        if (file != null) {
            String rutaLocal = ImageLoader.hacerPortadaLocalOffline(file.getAbsolutePath(), libro.getId(), this.rutaUsuario);
            libro.setPortadaURL(rutaLocal);
            guardarLibrosEnDisco();
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
            mostrarAlertaPublic(LanguageManager.getString("ui.exito", "Éxito"), LanguageManager.getString("ui.portada_actualizada", "Portada actualizada."));
        }
    }

    /**
     * Elimina un ejemplar o la totalidad de un libro tras confirmación
     * interactiva del usuario.
     *
     * @param libro Libro que se va a eliminar.
     */
    public void eliminarLibro(Libro libro) {
        if (libro == null) {
            return;
        }
        if (libro.getCantidad() > 1) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(LanguageManager.getString("ui.gestion_de_stock", "Gestión de Stock"));
            alert.setHeaderText(LanguageManager.getString("ui.tienes", "Tienes ") + libro.getCantidad() + LanguageManager.getString("ui.copias_de_este_libro", " copias de este libro."));
            alert.setContentText(LanguageManager.getString("ui.que_deseas_hacer", "¿Qué deseas hacer?"));
            ButtonType btnEliminarUno = new ButtonType("Eliminar solo 1 unidad");
            ButtonType btnEliminarTodo = new ButtonType("Borrar el libro entero");
            ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnEliminarUno, btnEliminarTodo, btnCancelar);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnEliminarUno) {
                    libroService.actualizarStock(libro, -1);
                    setMensajeEstado(LanguageManager.getString("ui.se_ha_eliminado_una_copia_quedan", "Se ha eliminado una copia. Quedan: ") + libro.getCantidad());
                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                } else if (result.get() == btnEliminarTodo) {
                    libroService.borrarTotalmente(libro);
                    setMensajeEstado(LanguageManager.getString("ui.libro_eliminado_definitivamente", "Libro eliminado definitivamente: ") + libro.getTitulo());
                    AppEventBus.getInstance().publish(new AppEventBus.LibroEliminadoEvent(libro));
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(LanguageManager.getString("ui.eliminar_libro", "Eliminar Libro"));
            alert.setHeaderText(LanguageManager.getString("ui.estas_seguro_de_que_quieres_borrar", "¿Estás seguro de que quieres borrar este libro?"));
            alert.setContentText(LanguageManager.getString("ui.vas_a_eliminar", "Vas a eliminar: ") + libro.getTitulo() + LanguageManager.getString("ui.nesta_accion_no_se_puede_deshacer", "\nEsta acción no se puede deshacer."));
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                libroService.borrarTotalmente(libro);
                setMensajeEstado(LanguageManager.getString("ui.libro_eliminado_definitivamente", "Libro eliminado definitivamente: ") + libro.getTitulo());
                AppEventBus.getInstance().publish(new AppEventBus.LibroEliminadoEvent(libro));
            }
        }
    }

    /**
     * Genera un archivo PDF con la etiqueta física que incluye código de barras
     * para lomos y estanterías.
     *
     * @param libro Libro físico del que se generará la etiqueta.
     */
    public void generarEtiquetaFisica(Libro libro) {
        if (libro == null) {
            return;
        }
        if (libro.isEsDigital()) {
            mostrarAlertaPublic(LanguageManager.getString("ui.sin_libros_fisicos", "Sin libros físicos"), LanguageManager.getString("ui.el_libro_seleccionado_es_digital_las", "El libro seleccionado es digital. Las etiquetas solo se generan para libros físicos."));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getString("ui.guardar_etiqueta_pdf", "Guardar Etiqueta PDF"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        fileChooser.setInitialFileName("Etiqueta_" + libro.getTitulo().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(getWindow());
        if (file != null) {
            try {
                ServicioEtiquetasFisicas.generarEtiquetasPDF(List.of(libro), file);
                notificar(LanguageManager.getString("ui.pdf_con_etiqueta_generado_con_exito", "PDF con etiqueta generado con éxito."));
                try {
                    com.bibliohouse.utils.AbrirExterno.abrir(file);
                } catch (IOException ignored) {
                }
            } catch (Exception e) {
                mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.hubo_un_problema_al_generar_la", "Hubo un problema al generar la etiqueta: ") + e.getMessage());
            }
        }
    }

    /**
     * Abre el lector digital integrado en caso de archivos EPUB o abre la
     * aplicación predeterminada del sistema.
     *
     * @param libro Libro digital a leer.
     */
    public void abrirLectorDigital(Libro libro) {
        if (libro == null || !libro.isEsDigital() || libro.getRutaArchivoDigital() == null) {
            mostrarAlertaPublic(LanguageManager.getString("ui.aviso", "Aviso"), LanguageManager.getString("ui.este_libro_no_tiene_un_archivo", "Este libro no tiene un archivo digital enlazado."));
            return;
        }

        File archivo = new File(libro.getRutaArchivoDigital());
        if (!archivo.exists()) {
            mostrarAlertaPublic(LanguageManager.getString("ui.archivo_no_encontrado", "Archivo no encontrado"), LanguageManager.getString("ui.no_se_encuentra_el_archivo_en", "No se encuentra el archivo en: ") + archivo.getAbsolutePath());
            return;
        }

        if (archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".epub")) {
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
                stage.setTitle(LanguageManager.getString("ui.lector", "Lector: ") + libro.getTitulo());
                Scene readerScene = new Scene(root, 900, 700);
                App.registerScene(readerScene);
                stage.setScene(readerScene);
                stage.centerOnScreen();
                stage.initOwner(getWindow());
                stage.setOnCloseRequest(e -> {
                    jsonManager.flushProgresoLectura();
                    controller.detenerServidor();
                });
                stage.show();
            } catch (IOException ex) {
                try {
                    com.bibliohouse.utils.AbrirExterno.abrir(archivo);
                } catch (IOException ignored) {
                }
            }
        } else {
            try {
                com.bibliohouse.utils.AbrirExterno.abrir(archivo);
            } catch (IOException ex) {
                mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_el_archivo", "No se pudo abrir el archivo digital."));
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
            if (sidebarController != null) {
                sidebarController.cargarListaEstanterias();
            }
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
        fileChooser.setTitle(LanguageManager.getString("ui.importar_biblioteca_desde_csv", "Importar biblioteca desde CSV"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File archivoSeleccionado = fileChooser.showOpenDialog(getWindow());
        if (archivoSeleccionado != null) {
            List<Libro> librosImportados = com.bibliohouse.logic.ImportadorCSV.importar(archivoSeleccionado);
            if (librosImportados == null || librosImportados.isEmpty()) {
                mostrarAlertaPublic(LanguageManager.getString("ui.importacion_csv", "Importación CSV"), LanguageManager.getString("ui.no_se_encontraron_libros_validos_en", "No se encontraron libros válidos en el archivo."));
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(LanguageManager.getString("ui.importacion_completada", "Importación completada"));
            alert.setHeaderText(LanguageManager.getString("ui.se_han_encontrado", "Se han encontrado ") + librosImportados.size() + LanguageManager.getString("ui.libros_en_el_archivo", " libros en el archivo."));
            alert.setContentText(LanguageManager.getString("ui.deseas_anadirlos_a_tu_biblioteca_actual", "¿Deseas añadirlos a tu biblioteca actual?"));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                int anadidos = 0;
                for (Libro nuevo : librosImportados) {
                    boolean existe = listaLibrosCompleta.stream().anyMatch(l
                            -> (nuevo.getIsbn() != null && !nuevo.getIsbn().isEmpty() && nuevo.getIsbn().equals(l.getIsbn()))
                            || (nuevo.getTitulo().equalsIgnoreCase(l.getTitulo()) && nuevo.getAutor().equalsIgnoreCase(l.getAutor())));
                    if (!existe) {
                        listaLibrosCompleta.add(nuevo);
                        anadidos++;
                    }
                }
                guardarLibrosEnDisco();
                actualizarComboLibrosDisponibles();
                AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, true));
                mostrarAlertaPublic(LanguageManager.getString("ui.exito", "Éxito"), LanguageManager.getString("ui.se_han_importado", "Se han importado ") + anadidos + LanguageManager.getString("ui.libros_correctamente", " libros correctamente."));
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
            stage.setTitle(LanguageManager.getString("ui.exportar_informe_pdf", "Exportar Informe PDF"));
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMinWidth(820);
            stage.setMinHeight(720);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana_3", "No se pudo abrir la ventana de exportación PDF.\n") + e.getMessage());
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
            stage.setTitle(LanguageManager.getString("ui.exportar_catalogo_web", "Exportar Catálogo Web"));
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setMinWidth(820);
            stage.setMinHeight(720);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana_4", "No se pudo abrir la ventana de exportación web.\n") + e.getMessage());
        }
    }

    @FXML
    private void cerrarAplicacion(ActionEvent event) {
        prepararCierre();
        Window win = getWindow();
        if (win instanceof Stage mainStage) {
            mainStage.close();
        }
        Platform.exit();
    }

    /**
     * Devuelve el gestor de datos de esta vista.
     *
     * @return Gestor de datos del usuario actual.
     */
    public JsonManager getJsonManager() {
        return jsonManager;
    }

    /**
     * Guarda el estado de la ventana y vuelca a disco todo lo pendiente. Se
     * llama tanto desde el menú «Salir» como al cerrar con el botón de la
     * ventana.
     */
    public void prepararCierre() {
        try {
            Window win = getWindow();
            if (win instanceof Stage mainStage && preferencias != null) {
                preferencias.put("maximized", String.valueOf(mainStage.isMaximized()));
                java.util.prefs.Preferences.userNodeForPackage(App.class)
                        .put("maximized", String.valueOf(mainStage.isMaximized()));
            }
            if (jsonManager != null && preferencias != null) {
                jsonManager.guardarPreferencias(preferencias);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "No se pudo guardar el estado de la ventana al cerrar", e);
        }
        liberarRecursos();
    }

    /**
     * Libera este controlador: retira sus suscripciones (y las de sus
     * subcontroladores) del bus de eventos y cierra su gestor de datos tras
     * volcar lo pendiente. Debe llamarse cuando la vista se sustituye (cambio
     * de idioma) o al salir.
     */
    public void liberarRecursos() {
        AppEventBus bus = AppEventBus.getInstance();
        bus.unsubscribeAll(this);
        bus.unsubscribeAll(sidebarController);
        bus.unsubscribeAll(catalogoGridController);
        if (jsonManager != null) {
            jsonManager.setAutoSyncTask(null);
            jsonManager.shutdown();
        }
    }

    // =========================================================================
    // MENÚ HERRAMIENTAS Y AYUDA
    // =========================================================================
    @FXML
    private void buscarDuplicados(ActionEvent event) {
        String reporte = libroService.buscarYFusionarDuplicados();
        if (reporte == null) {
            mostrarAlertaPublic(LanguageManager.getString("ui.busqueda_de_duplicados", "Búsqueda de Duplicados"), LanguageManager.getString("ui.no_se_encontraron_libros_repetidos", "No se encontraron libros repetidos."));
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
            stage.setTitle(LanguageManager.getString("ui.informe_de_duplicados", "Informe de Duplicados"));
            setScene(stage, root);
            stage.show();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.exito", "Éxito"), LanguageManager.getString("ui.proceso_completado_revisa_la_tabla", "Proceso completado. Revisa la tabla."));
        }
    }

    @FXML
    private void buscarPortadasFaltantes(ActionEvent event) {
        if (!busquedaPortadasEnCurso.compareAndSet(false, true)) {
            notificar(LanguageManager.getString("ui.ya_hay_una_busqueda_de_portadas", "Ya hay una búsqueda de portadas ejecutándose en segundo plano."));
            return;
        }

        List<Libro> librosSinPortada = listaLibrosCompleta.stream()
                .filter(l -> {
                    String p = l.getPortadaURL();
                    if (p == null || p.isBlank() || p.contains("default_cover")) {
                        return true;
                    }
                    if (!p.startsWith("http://") && !p.startsWith("https://")) {
                        File local = com.bibliohouse.utils.ImageLoader.resolverArchivoLocal(p);
                        return local == null || !local.exists();
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (librosSinPortada.isEmpty()) {
            busquedaPortadasEnCurso.set(false);
            mostrarAlertaPublic(LanguageManager.getString("ui.busqueda_de_portadas", "Búsqueda de Portadas"), LanguageManager.getString("ui.no_hay_libros_pendientes_de_portada", "No hay libros pendientes de portada."));
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(LanguageManager.getString("ui.busqueda_masiva_de_portadas", "Búsqueda masiva de portadas"));
        confirmacion.setHeaderText(LanguageManager.getString("ui.se_van_a_procesar", "Se van a procesar ") + librosSinPortada.size() + LanguageManager.getString("ui.libros", " libros."));
        confirmacion.setContentText(LanguageManager.getString("ui.el_proceso_se_ejecutara_en_segundo", "El proceso se ejecutará en segundo plano para que puedas seguir leyendo o gestionando tus libros. ¿Deseas iniciar?"));

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            busquedaPortadasEnCurso.set(false);
            return;
        }

        int total = librosSinPortada.size();
        String inicioMsg = "Iniciando búsqueda de portadas para " + total + " libros en segundo plano...";
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(inicioMsg));
        notificar(inicioMsg);

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                int actualizadas = 0;
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    Libro libro = librosSinPortada.get(i);
                    String status = "Buscando portada (" + (i + 1) + "/" + total + "): " + libro.getTitulo();
                    updateMessage(status);
                    updateProgress(i + 1, total);
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(status));

                    boolean conseguida = false;

                    // 1. Si es e-book digital (EPUB / PDF), intentar extracción local instantánea primero
                    if (libro.isEsDigital() && libro.getRutaArchivoDigital() != null && !libro.getRutaArchivoDigital().isBlank()) {
                        try {
                            conseguida = EbookMetadataService.asegurarPortadaEbook(libro, rutaUsuario);
                        } catch (Exception ex) {
                            LOGGER.log(Level.FINE, "No se pudo extraer portada local incrustada de: " + libro.getTitulo(), ex);
                        }
                    }

                    // 2. Si no es e-book o no tenía portada incrustada, buscar en servicios remotos
                    if (!conseguida) {
                        String isbn = (libro.getIsbn() != null) ? libro.getIsbn().trim() : "";
                        String urlEncontrada = "";

                        // 2.1 Intento directo y rápido por ISBN
                        if (!isbn.isEmpty()) {
                            urlEncontrada = busquedaService.buscarImagenPorIsbnDirecto(isbn);
                        }

                        // 2.2 Intento en APIs por ISBN
                        if (urlEncontrada.isEmpty() && !isbn.isEmpty()) {
                            urlEncontrada = busquedaService.buscarImagenEnApisMasivo(isbn);
                        }

                        // 2.3 Intento en APIs por título
                        if (urlEncontrada.isEmpty() && libro.getTitulo() != null && !libro.getTitulo().isBlank()) {
                            urlEncontrada = busquedaService.buscarImagenEnApisMasivo(libro.getTitulo());
                        }

                        if (!urlEncontrada.isEmpty()) {
                            String rutaLocal = ImageLoader.hacerPortadaLocalOffline(urlEncontrada, libro.getId(), rutaUsuario);
                            libro.setPortadaURL(rutaLocal);
                            conseguida = true;
                        }
                    }

                    if (conseguida) {
                        actualizadas++;
                        // Notificar actualización en tiempo real para refrescar portadas en el catálogo
                        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
                    }

                    Thread.sleep(150);
                }
                return actualizadas;
            }
        };

        task.setOnSucceeded(e -> {
            busquedaPortadasEnCurso.set(false);
            guardarLibrosEnDisco();
            AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(null, false));
            int actualizadas = task.getValue() != null ? task.getValue() : 0;
            String finMsg = "Búsqueda completada: se han actualizado " + actualizadas + " de " + total + " portadas.";
            AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(finMsg));
            notificar(finMsg);
        });

        task.setOnFailed(e -> {
            busquedaPortadasEnCurso.set(false);
            String errorMsg = "Ocurrió un error durante la búsqueda de portadas en segundo plano.";
            AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(errorMsg));
            notificar(errorMsg);
        });

        task.setOnCancelled(e -> {
            busquedaPortadasEnCurso.set(false);
            String cancelMsg = "Búsqueda de portadas en segundo plano cancelada.";
            AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent(cancelMsg));
            notificar(cancelMsg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.setName("bg-cover-searcher");
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
            stage.setTitle(LanguageManager.getString("ui.configuracion", "Configuración"));
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_la_configuracion", "No se pudo abrir la configuración."));
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
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana_5", "No se pudo abrir la ventana de estadísticas."));
        }
    }

    @FXML
    private void mostrarAyudaManual(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("help.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(LanguageManager.getString("ui.manual_de_usuario", "Manual de Usuario"));
            setScene(stage, root);
            stage.getIcons().add(new Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));
            stage.initModality(Modality.NONE);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_cargar_el_manual", "No se pudo cargar el Manual de Ayuda."));
        }
    }

    @FXML
    private void mostrarAcercaDe(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("acercade.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(LanguageManager.getString("ui.acerca_de_bibliohouse", "Acerca de BiblioHouse"));
            setScene(stage, root);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_abrir_la_ventana_6", "No se pudo abrir la ventana Acerca De."));
        }
    }

    // =========================================================================
    // CAMBIO DE IDIOMA (I18N)
    // =========================================================================
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

    private void cambiarIdioma(String codigoLang) {
        try {
            com.bibliohouse.logic.LanguageManager.setLocale(codigoLang);
            Window win = getWindow();
            if (win instanceof Stage currentStage) {
                App.reloadUI(currentStage, this.usuarioActual, this.rutaUsuario);
            }
        } catch (IOException e) {
            mostrarAlertaPublic(LanguageManager.getString("ui.error", "Error"), LanguageManager.getString("ui.no_se_pudo_cambiar_el_idioma", "No se pudo cambiar el idioma correctamente."));
        }
    }

    // =========================================================================
    // SOPORTE PARA COMBOBOX FILTRABLES (Para PrestamosController)
    // =========================================================================
    public <T> void setupFilteringComboBoxPublic(ComboBox<T> comboBox, java.util.function.Function<T, String> displayFunc) {
        if (comboBox == null) {
            return;
        }
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
                String itemText = displayFunc.apply(item).toLowerCase(java.util.Locale.ROOT);
                return itemText.contains(newText.toLowerCase(java.util.Locale.ROOT));
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
