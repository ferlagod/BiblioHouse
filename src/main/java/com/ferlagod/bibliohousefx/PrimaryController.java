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

import com.bibliohouse.logic.JsonManager;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
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

/**
 * Este es el controlador principal. Aquí manejo la tabla de libros, los
 * préstamos y todo eso. Es como el cerebro de la pantalla principal.
 *
 * @author Ferlagod
 * @version 1.1
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

    // --- LÍMITE DE PRÉSTAMO ---
    /**
     * Días límite para considerar un préstamo como vencido. Por defecto es 30,
     * pero puede ser configurado por el usuario.
     */
    private int dueDaysLimit = 30;

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
     * Esta función arranca todo cuando se abre la ventana.Configura las
     * columnas de las tablas y los botones.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.resources = rb; // Guardamos el bundle

        // Configurar columnas Libros
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colEditorial.setCellValueFactory(new PropertyValueFactory<>("editorial"));
        colGenero.setCellValueFactory(new PropertyValueFactory<>("genero"));
        colAnio.setCellValueFactory(new PropertyValueFactory<>("año"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoLectura"));
        colSerie.setCellValueFactory(new PropertyValueFactory<>("serie")); // <-- VINCULACIÓN
        colOrden.setCellValueFactory(new PropertyValueFactory<>("ordenEnSerie")); // <-- VINCULACIÓN
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        // Configurar columnas Prestamos
        colPrestamoLibro.setCellValueFactory(new PropertyValueFactory<>("tituloLibro"));
        colPrestamoSocio.setCellValueFactory(new PropertyValueFactory<>("nombreSocio"));
        colPrestamoFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPrestamoFormateada"));
        colPrestamoDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionFormateada"));

        // Configurar columnas Historial
        colHistorialLibro.setCellValueFactory(new PropertyValueFactory<>("tituloLibro"));
        colHistorialSocio.setCellValueFactory(new PropertyValueFactory<>("nombreSocio"));
        colHistorialFechaPrestamo.setCellValueFactory(new PropertyValueFactory<>("fechaPrestamoFormateada"));
        colHistorialFechaDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionFormateada"));

        // Spinner
        if (spinnerCantidad != null) {
            spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        }

        // Configurar selección inicial de idioma
        updateLanguageMenuSelection();

        // Configurar Atajos de Teclado
        setupShortcuts();

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

        // Listener de Estanterías (Filtro) - CONECTADO AL FILTRO DINÁMICO
        if (listaEstanterias != null) {
            listaEstanterias.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    actualizarFiltros();
                }
            });
        }

        // Listener para búsqueda incremental por Título (Dinámica)
        if (txtBusquedaLocal != null) {
            txtBusquedaLocal.textProperty().addListener((observable, oldValue, newValue) -> {
                actualizarFiltros(); // Filtra automáticamente al escribir
            });
        }

        // Listener para filtro por Autor
        if (txtFiltroAutor != null) {
            txtFiltroAutor.textProperty().addListener((observable, oldValue, newValue) -> {
                actualizarFiltros();
            });
        }

        // Listener para filtro por ISBN
        if (txtFiltroISBN != null) {
            txtFiltroISBN.textProperty().addListener((observable, oldValue, newValue) -> {
                actualizarFiltros();
            });
        }

        // Listener para filtro de ESTADO
        if (cmbFiltroEstado != null) {
            cmbFiltroEstado.setItems(FXCollections.observableArrayList(
                    "Todos", "Leído", "Leyendo", "Pendiente"));
            cmbFiltroEstado.setValue("Todos");
            cmbFiltroEstado.valueProperty().addListener((obs, old, newVal) -> actualizarFiltros());
        }

        // Row Factory para marcar préstamos vencidos
        if (tablaPrestamos != null) {
            tablaPrestamos.setRowFactory(tv -> new TableRow<Prestamo>() {
                @Override
                protected void updateItem(Prestamo item, boolean empty) {
                    super.updateItem(item, empty);

                    // Siempre limpiamos la clase antes de decidir si aplicarla
                    getStyleClass().remove("overdue-loan");

                    if (item != null && !empty) {
                        // Solo préstamos activos (fechaDevolucion es null)
                        if (item.getFechaDevolucion() == null && item.getFechaPrestamo() != null) {

                            // Límite configurable
                            LocalDate dueDate = item.getFechaPrestamo().plusDays(dueDaysLimit);

                            if (dueDate.isBefore(LocalDate.now())) {
                                getStyleClass().add("overdue-loan"); // Aplicar la clase CSS
                            }
                        }
                    }
                }
            });
        }
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
        // Inicializar ImageLoader con la ruta de portadas del usuario
        // Ruta: /UserHome/BiblioHouse/users/<username>/covers
        String coversPath = this.rutaUsuario + java.io.File.separator + "covers";
        com.bibliohouse.utils.ImageLoader.setCacheDir(coversPath);

        this.jsonManager = new JsonManager(userPath);

        this.preferencias = jsonManager.cargarPreferencias();
        cargarDatos();
        aplicarPreferenciasGuardadas();

        if (resources != null) {
            lblEstado.setText(java.text.MessageFormat.format(resources.getString("status.welcome"), username));
        } else {
            lblEstado.setText("Bienvenido, " + username);
        }

        checkOverdueLoans();
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
            Platform.runLater(() -> {
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
                        tabPane.getSelectionModel().select(1); // La pestaña de Préstamos es la segunda (índice 1)
                    }
                }
            });
        }
    }

    // Método para aplicar el tema guardado
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
            // help.title=Manual de Usuario
            String title = resources.containsKey("help.title") ? resources.getString("help.title")
                    : "Manual de Usuario";
            stage.setTitle(title);
            setScene(stage, root);

            // Icono
            stage.getIcons()
                    .add(new javafx.scene.image.Image(App.class.getResourceAsStream("/resources/LogoBiblioHouse.png")));

            stage.initModality(Modality.NONE); // Ventana no modal, permite seguir usando la app
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
                            // Seleccionar tab de préstamos (índice 1)
                            tabPaneVistaLibros.getSelectionModel().select(1);
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
        // Cargar días de préstamo (por defecto 30 si no existe o hay error)
        try {
            String diasStr = preferencias.getOrDefault("dias_prestamo", "30");
            this.dueDaysLimit = Integer.parseInt(diasStr);
        } catch (NumberFormatException e) {
            this.dueDaysLimit = 30;
        }

        // Mover la aplicación de UI al hilo de JavaFX después de la renderización
        // inicial
        Platform.runLater(() -> {
            Scene scene = tablaLibros.getScene();
            if (scene == null) {
                return;
            }

            // 1. APLICAR MAXIMIZADO
            Stage mainStage = (Stage) scene.getWindow();
            boolean isMaximized = Boolean.parseBoolean(preferencias.getOrDefault("maximized", "false"));
            if (isMaximized) {
                mainStage.setMaximized(true);
            }
        });
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
                if (categoriaSeleccionada.equals("Lista de Deseos")) {
                    // En la lista de deseos solo mostramos los NO poseídos
                    if (libro.isPoseido()) {
                        return false;
                    }
                } else {
                    // En cualquier otra vista (incluyendo "Todos los libros") solo mostramos los
                    // poseídos
                    if (!libro.isPoseido()) {
                        return false;
                    }

                    // Si es una estantería específica, comprobar pertenencia
                    if (!categoriaSeleccionada.equals("Todos los libros")) {
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
                if (libro.getIsbn() == null || !libro.getIsbn().toLowerCase().contains(filtroISBN)) {
                    return false;
                }
            }

            // Si pasó todos los filtros, mostrar el libro
            return true;
        });

        lblEstado.setText("Mostrando " + filteredData.size() + " de " + listaLibrosCompleta.size() + " libros.");
    }

    // Método auxiliar para refrescar el desplegable de libros
    /**
     * Actualiza el ComboBox de libros disponibles para préstamo. Solo incluye
     * libros que tengan stock disponible (cantidad > 0).
     */
    private void actualizarComboLibrosDisponibles() {
        ObservableList<Libro> librosConStock = FXCollections.observableArrayList();
        for (Libro l : listaLibrosCompleta) {
            if (l.getCantidad() > 0) {
                librosConStock.add(l);
            }
        }
        comboLibrosPrestamo.setItems(librosConStock);
    }

    /**
     * Muestra una ventana modal con información sobre la aplicación
     * BiblioHouse.
     *
     * @param event El evento de acción que desencadena la apertura de la
     * ventana.
     *
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

        // --- INICIO MODIFICACIÓN: Estanterías por defecto ---
        if (estanterias == null || estanterias.isEmpty()) {
            estanterias = new ArrayList<>();
            estanterias.add("Novela");
            estanterias.add("Ciencia Ficción");
            estanterias.add("Fantasía");
            estanterias.add("Historia");
            estanterias.add("Tecnología");
            estanterias.add("Aventura");
            estanterias.add("Biografía");
            estanterias.add("Romántica");
            estanterias.add("Poesía");
            estanterias.add("Teatro");
            estanterias.add("Infantil");
            estanterias.add("Ensayo");

            // Guardamos las estanterías por defecto para que persistan
            jsonManager.guardarEstanterias(estanterias);
        }
        // --- FIN MODIFICACIÓN ---

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Todos los libros");
        items.add("Lista de Deseos");
        if (estanterias != null) {
            items.addAll(estanterias);
        }
        listaEstanterias.setItems(items);
        listaEstanterias.getSelectionModel().select(0);
    }

    // --- MÉTODO PARA SELECCIONAR IMAGEN MANUALMENTE ---
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
            stage.setScene(new Scene(root));
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
     * Busca libros en internet. Lanza hilos para buscar en OpenLi, Google e
     * Inventaire a la vez.
     *
     * @param event El botón pulsado.
     */
    @FXML
    private void buscarLibroOpenLibrary(ActionEvent event) {
        String query = txtBusquedaOpenLibrary.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        System.out.println("[DEBUG] Iniciando búsqueda MULTI-PROVEEDOR para: " + query);
        lblEstado.setText("Buscando en OpenLibrary, Google Books e Inventaire...");

        // Ejecutar búsqueda en segundo plano con CompletableFuture para paralelismo
        // real
        Thread searchThread = new Thread(() -> {
            try {
                System.out.println("[DEBUG] Hilo de orquestación de búsqueda iniciado");
                
                // 1. Definir las tareas de búsqueda (Futures)
                java.util.concurrent.CompletableFuture<List<Libro>> futureOpenLib = java.util.concurrent.CompletableFuture
                        .supplyAsync(() -> {
                            System.out.println("[DEBUG] Buscando en OpenLibrary...");
                            return OpenLibraryCliente.buscarLibros(query);
                        }).exceptionally(ex -> {
                            System.err.println("[ERROR] Error en OpenLibrary: " + ex.getMessage());
                            return new ArrayList<>(); // Retornar lista vacía en caso de error
                        });
                
                java.util.concurrent.CompletableFuture<List<Libro>> futureGoogle = java.util.concurrent.CompletableFuture
                        .supplyAsync(() -> {
                            System.out.println("[DEBUG] Buscando en Google Books...");
                            return com.bibliohouse.logic.GoogleBooksCliente.buscarLibros(query);
                        }).exceptionally(ex -> {
                            System.err.println("[ERROR] Error en Google Books: " + ex.getMessage());
                            return new ArrayList<>();
                        });
                
                java.util.concurrent.CompletableFuture<List<Libro>> futureInventaire = java.util.concurrent.CompletableFuture
                        .supplyAsync(() -> {
                            System.out.println("[DEBUG] Buscando en Inventaire...");
                            return com.bibliohouse.logic.InventaireCliente.buscarLibros(query);
                        }).exceptionally(ex -> {
                            System.err.println("[ERROR] Error en Inventaire: " + ex.getMessage());
                            return new ArrayList<>();
                        });
                
                // 2. Esperar a que TODAS terminen (join)
                // Usamos allOf para esperar, pero luego extraemos resultados individualmente
                java.util.concurrent.CompletableFuture<Void> allFutures = java.util.concurrent.CompletableFuture
                        .allOf(futureOpenLib, futureGoogle, futureInventaire);
                
                allFutures.join(); // Bloquea este hilo (searchThread) hasta que todos terminen
                
                // 3. Recolectar resultados
                List<Libro> resultadosTotales = new ArrayList<>();
                
                // OpenLibrary
                List<Libro> resOL = futureOpenLib.get();
                if (resOL != null) {
                    resultadosTotales.addAll(resOL);
                }
                
                // Google
                List<Libro> resGB = futureGoogle.get();
                if (resGB != null) {
                    resultadosTotales.addAll(resGB);
                }
                
                // Inventaire
                List<Libro> resIV = futureInventaire.get();
                if (resIV != null) {
                    resultadosTotales.addAll(resIV);
                }
                
                System.out.println("[DEBUG] Búsqueda completada. Total resultados: " + resultadosTotales.size());
                System.out.println(String.format("[DEBUG] Desglose: OL=%d, GB=%d, IV=%d",
                        (resOL != null ? resOL.size() : 0), (resGB != null ? resGB.size() : 0),
                        (resIV != null ? resIV.size() : 0)));
                
                // 4. Actualizar UI
                Platform.runLater(() -> {
                    if (resultadosTotales.isEmpty()) {
                        System.out.println("[DEBUG] No se encontraron resultados en ningún proveedor");
                        mostrarAlerta("Sin resultados", "No se encontró nada en ninguna de las librerías conectadas.");
                        lblEstado.setText("Búsqueda finalizada sin éxito.");
                    } else {
                        // --- ABRIR VENTANA DE RESULTADOS ---
                        System.out.println("[DEBUG] Abriendo ventana con " + resultadosTotales.size() + " libros");
                        abrirVentanaResultados(resultadosTotales);
                        lblEstado.setText("Búsqueda finalizada. Resultados: " + resultadosTotales.size());
                    }
                });
                
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("[ERROR] Excepción general en hilo de búsqueda: " + e.getMessage());
                Platform.runLater(() -> {
                    lblEstado.setText("Error en la búsqueda.");
                    mostrarAlerta("Error", "Error crítico al buscar: " + e.getMessage());
                });
            }
        });

        searchThread.setDaemon(true);
        searchThread.setName("UniSearch-Orchestrator");
        searchThread.start();
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
            stage.setTitle("Resultados OpenLibrary");
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            System.out.println("[DEBUG] Mostrando ventana de resultados...");
            stage.showAndWait();
            System.out.println("[DEBUG] Ventana de resultados cerrada");

            // Recoger el libro seleccionado al cerrar
            Libro elegido = controller.getLibroSeleccionado();
            if (elegido != null) {
                System.out.println("[DEBUG] Libro seleccionado: " + elegido.getTitulo());
                rellenarFormularioManual(elegido);
            } else {
                System.out.println("[DEBUG] No se seleccionó ningún libro");
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
            Parent root = loader.load();

            EditarLibroController controller = loader.getController();
            controller.setLibro(libroSeleccionado);
            controller.setEstanteriasDisponibles(jsonManager.cargarEstanterias());

            Stage stage = new Stage();
            stage.setTitle("Editar: " + libroSeleccionado.getTitulo());
            setScene(stage, root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tablaLibros.getScene().getWindow());

            stage.showAndWait();

            if (controller.isGuardado()) {
                tablaLibros.refresh();
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                cargarListaEstanterias();
                lblEstado.setText("Libro editado correctamente.");
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar Base de Datos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = fileChooser.showOpenDialog(tablaLibros.getScene().getWindow());

        if (archivo != null) {
            List<Libro> importados = jsonManager.importarLibrosDesdeArchivo(archivo);
            if (importados != null && !importados.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Importar");
                alert.setContentText("¿Añadir a los existentes o Reemplazar todo?");
                ButtonType btnAdd = new ButtonType("Añadir");
                ButtonType btnReplace = new ButtonType("Reemplazar");
                ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnAdd, btnReplace, btnCancel);

                Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent()) {
                    if (res.get() == btnAdd) {

                        listaLibrosCompleta.addAll(importados);
                    } else if (res.get() == btnReplace) {

                        listaLibrosCompleta.setAll(importados);
                    }
                    if (res.get() != btnCancel) {
                        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                        cargarListaEstanterias();
                        actualizarComboLibrosDisponibles(); // Actualizar combo de préstamos
                    }
                }
            }
        }
    }

    /**
     * Exporta la base de datos actual a un archivo JSON de respaldo.
     *
     * @param event El evento del menú.
     */
    @FXML
    private void exportarBaseDatos(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Backup");
        fileChooser.setInitialFileName("biblioteca_backup.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = fileChooser.showSaveDialog(tablaLibros.getScene().getWindow());

        if (archivo != null) {
            boolean ok = jsonManager.exportarLibros(archivo, new ArrayList<>(listaLibrosCompleta));
            if (ok) {
                mostrarAlerta("Éxito", "Copia guardada correctamente.");
            } else {
                mostrarAlerta("Error", "Fallo al exportar.");
            }
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("escaner.fxml"));
            Parent root = loader.load();

            EscanerController escanerController = loader.getController();
            escanerController.setListener(isbn -> {
                // Cuando se detecta un ISBN:
                txtIsbn.setText(isbn);
                // Si estamos en la pestaña principal, buscar en OpenLibrary automáticamente
                Platform.runLater(() -> {
                    txtBusquedaOpenLibrary.setText(isbn);
                    buscarLibroOpenLibrary(null);
                });
            });

            Stage stage = new Stage();
            stage.setTitle("Escáner de Código de Barras");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            // Iniciar cámara al mostrar
            stage.setOnShown(e -> escanerController.init());
            // Asegurar cierre de cámara al cerrar ventana
            stage.setOnCloseRequest(e -> escanerController.shutdown());

            stage.show();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir el escáner: " + e.getMessage());
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
        List<Libro> librosParaBorrar = new ArrayList<>();
        int contadorFusionados = 0;

        // Buscamos duplicados
        for (int i = 0; i < listaLibrosCompleta.size(); i++) {
            Libro original = listaLibrosCompleta.get(i);

            // Si este libro ya está marcado para borrar, lo saltamos
            if (librosParaBorrar.contains(original)) {
                continue;
            }

            for (int j = i + 1; j < listaLibrosCompleta.size(); j++) {
                Libro duplicado = listaLibrosCompleta.get(j);

                // Criterio de duplicidad: ISBN igual O (Título y Autor iguales)
                boolean esMismoIsbn = !original.getIsbn().isEmpty() && original.getIsbn().equals(duplicado.getIsbn());
                boolean esMismoTitulo = original.getTitulo().equalsIgnoreCase(duplicado.getTitulo())
                        && original.getAutor().equalsIgnoreCase(duplicado.getAutor());

                if (esMismoIsbn || esMismoTitulo) {

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Duplicado Encontrado");
                    alert.setHeaderText(
                            "Conflicto entre:\n1. " + original.getTitulo() + " (Stock: " + original.getCantidad()
                            + ")\n2. " + duplicado.getTitulo() + " (Stock: " + duplicado.getCantidad() + ")");
                    alert.setContentText("¿Deseas fusionarlos en uno solo y sumar su stock?");

                    ButtonType btnFusionar = new ButtonType("Fusionar y Eliminar duplicado");
                    ButtonType btnIgnorar = new ButtonType("Ignorar");

                    alert.getButtonTypes().setAll(btnFusionar, btnIgnorar);
                    Optional<ButtonType> res = alert.showAndWait();

                    if (res.isPresent() && res.get() == btnFusionar) {
                        // 1. Sumar stock al original
                        original.setCantidad(original.getCantidad() + duplicado.getCantidad());

                        // 2. Marcar el segundo para borrar
                        librosParaBorrar.add(duplicado);
                        contadorFusionados++;
                    }
                }
            }
        }

        if (contadorFusionados > 0) {
            // Aplicar borrados

            listaLibrosCompleta.removeAll(librosParaBorrar);

            // Guardar
            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
            tablaLibros.refresh();

            mostrarAlerta("Limpieza completada", "Se han fusionado " + contadorFusionados + " libros duplicados.");
        } else {
            mostrarAlerta("Duplicados", "No se encontraron duplicados o no se realizaron cambios.");
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

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Aseguramos que se cargue el estilo base
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

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
        // 1. Obtener datos de LOS COMBOS (No de la tabla)
        Libro libro = comboLibrosPrestamo.getValue();
        Socio socio = comboSocios.getValue();

        if (libro == null || socio == null) {
            mostrarAlerta("Datos faltantes", "Por favor, selecciona un libro y un socio de las listas.");
            return;
        }

        // Validación de stock
        if (libro.getCantidad() <= 0) {
            mostrarAlerta("Sin stock", "No quedan ejemplares disponibles de este libro.");
            return;
        }

        // 2. Crear préstamo
        Prestamo p = new Prestamo(libro, socio);
        listaPrestamosCompleta.add(p);

        // 3. Restar Stock
        libro.setCantidad(libro.getCantidad() - 1);

        // 4. Guardar y Refrescar
        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        tablaLibros.refresh(); // Refrescar tabla principal
        actualizarComboLibrosDisponibles();

        lblEstado.setText("Préstamo realizado: " + libro.getTitulo() + " -> " + socio.getNombre());
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
        Prestamo p = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (p == null) {
            mostrarAlerta("Selección necesaria", "Selecciona un préstamo de la lista para devolverlo.");
            return;
        }

        // 1. COMPROBACIÓN: ¿Ya estaba devuelto?
        if (p.getFechaDevolucion() != null) {
            mostrarAlerta("Aviso", "Este préstamo ya figura como devuelto el " + p.getFechaDevolucionFormateada());
            return;
        }

        // 2. ACTUALIZAR ESTADO DEL PRÉSTAMO
        p.setFechaDevolucion(LocalDate.now());

        // 3. DEVOLVER STOCK AL LIBRO
        for (Libro l : listaLibrosCompleta) {
            // Buscamos el libro por título (idealmente sería por ISBN, pero usamos lo que
            // tenemos)
            if (l.getTitulo().equals(p.getTituloLibro())) {
                l.setCantidad(l.getCantidad() + 1);
                break;
            }
        }

        // 4. GUARDAR CAMBIOS
        // Guardamos la lista de préstamos
        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        // 5. REFRESCAR UI
        // Forzamos el re-filtrado de ambas tablas para que el préstamo pase de Activos
        // a Historial
        filteredPrestamos.setPredicate(p2 -> p2.getFechaDevolucion() == null);
        filteredHistory.setPredicate(p2 -> p2.getFechaDevolucion() != null);

        tablaPrestamos.refresh();
        tablaHistorial.refresh();
        actualizarComboLibrosDisponibles(); // El libro vuelve a estar disponible en el combo

        lblEstado.setText("Devolución registrada correctamente.");
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
            stage.showAndWait();

            if (controller.isModified()) {
                jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
            }

            tablaLibros.refresh();
        } catch (IOException e) {
        }
    }

    /**
     * Muestra una alerta informativa al usuario.
     *
     * @param titulo Título de la alerta.
     * @param mensaje Contenido del mensaje.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
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
    }
}
