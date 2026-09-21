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
import com.bibliohouse.logic.BusquedaService;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.utils.ImageLoader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la pestaña "Gestionar Libros". Permite buscar libros en
 * servicios externos (OpenLibrary, Google Books, Inventaire), usar el escáner
 * de código de barras o rellenar la ficha de un libro manualmente.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class GestionLibrosController {

    private static final Logger LOGGER = Logger.getLogger(GestionLibrosController.class.getName());

    @FXML
    private TextField txtBusquedaOpenLibrary;
    @FXML
    private VBox vboxManual;
    @FXML
    private ImageView imgPortadaManual;
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtAutor;
    @FXML
    private TextField txtEditorial;
    @FXML
    private TextField txtGenero;
    @FXML
    private TextField txtIsbn;
    @FXML
    private TextField txtAnio;
    @FXML
    private Spinner<Integer> spinnerCantidad;
    @FXML
    private TextField txtSerie;
    @FXML
    private TextField txtOrden;
    @FXML
    private CheckBox chkPoseidoManual;

    private PrimaryController mainController;
    private JsonManager jsonManager;
    private ObservableList<Libro> listaLibrosCompleta;
    private BusquedaService busquedaService;
    private String rutaUsuario;
    private ResourceBundle resources;
    private String rutaPortadaTemporal = "";

    @FXML
    public void initialize() {
        if (spinnerCantidad != null) {
            spinnerCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        }
    }

    /**
     * Inicializa las dependencias principales, la lista de libros, el servicio de búsqueda online,
     * el directorio de usuario y los paquetes de idioma.
     *
     * @param mainController   Controlador principal de la aplicación.
     * @param jsonManager      Gestor de persistencia JSON.
     * @param listaLibros      Lista observable de todos los libros del usuario.
     * @param busquedaService  Servicio para consultas multifuente asíncronas.
     * @param rutaUsuario      Ruta del directorio de datos del usuario actual.
     * @param resources        Paquete de recursos para cadenas localizadas.
     */
    public void initData(PrimaryController mainController, JsonManager jsonManager,
                         ObservableList<Libro> listaLibros, BusquedaService busquedaService,
                         String rutaUsuario, ResourceBundle resources) {
        this.mainController = mainController;
        this.jsonManager = jsonManager;
        this.listaLibrosCompleta = listaLibros;
        this.busquedaService = busquedaService != null ? busquedaService : new BusquedaService();
        this.rutaUsuario = rutaUsuario;
        this.resources = resources;
    }

    // =========================================================================
    // BÚSQUEDA EN APIS Y ESCÁNER
    // =========================================================================

    /**
     * Realiza una búsqueda asíncrona en los servicios externos online (OpenLibrary, Google Books, Inventaire)
     * a partir del texto o ISBN introducido en la caja de búsqueda.
     *
     * @param event Evento de acción disparado por el botón de búsqueda o pulsar Intro.
     */
    @FXML
    public void buscarLibroOpenLibrary(ActionEvent event) {
        if (txtBusquedaOpenLibrary == null) return;
        String query = txtBusquedaOpenLibrary.getText().trim();
        if (query.isEmpty()) return;

        txtBusquedaOpenLibrary.setDisable(true);
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Buscando en catálogo online..."));

        busquedaService.ejecutarBusquedaGlobalAsync(query)
                .thenAccept(resultados -> Platform.runLater(() -> {
                    txtBusquedaOpenLibrary.setDisable(false);
                    if (resultados.isEmpty()) {
                        mostrarAlerta("Sin resultados", "No se encontraron libros para: " + query);
                    } else {
                        abrirVentanaResultados(resultados);
                    }
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Listo."));
                }))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error en búsqueda de libros", ex);
                    Platform.runLater(() -> {
                        txtBusquedaOpenLibrary.setDisable(false);
                        mostrarAlerta("Error de red", "No se pudo completar la búsqueda: " + ex.getMessage());
                        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Error en búsqueda."));
                    });
                    return null;
                });
    }

    /**
     * Abre la ventana modal del escáner de código de barras mediante la cámara web
     * utilizando OpenCV y ZXing.
     *
     * @param event Evento disparado al pulsar el botón del escáner.
     */
    @FXML
    public void abrirEscaner(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("escaner.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            EscanerController escanerController = loader.getController();
            escanerController.setListener(isbns -> {
                if (isbns != null && !isbns.isEmpty()) {
                    String ultimoIsbn = isbns.get(isbns.size() - 1);
                    Platform.runLater(() -> {
                        txtIsbn.setText(ultimoIsbn);
                        txtBusquedaOpenLibrary.setText(ultimoIsbn);
                        buscarLibroOpenLibrary(null);
                    });
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Escáner de Código de Barras");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnShown(e -> escanerController.init());
            stage.setOnCloseRequest(e -> escanerController.shutdown());
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error abriendo escáner", e);
            mostrarAlerta("Error", "No se pudo abrir el escáner: " + e.getMessage());
        }
    }

    /**
     * Despliega la ventana modal con la lista de libros encontrados en la búsqueda externa
     * para que el usuario seleccione el deseado y autorellene la ficha.
     *
     * @param resultados Lista de libros devueltos por el servicio de búsqueda.
     */
    private void abrirVentanaResultados(List<Libro> resultados) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resultados_busqueda.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();

            ResultadosBusquedaController controller = loader.getController();
            controller.setResultados(resultados);

            Stage stage = new Stage();
            stage.setTitle("Resultados de Búsqueda");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Libro libroSeleccionado = controller.getLibroSeleccionado();
            if (libroSeleccionado != null) {
                rellenarFormularioManual(libroSeleccionado);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de resultados", e);
        }
    }

    /**
     * Rellena automáticamente los campos del formulario manual con los datos de un libro
     * obtenido de la búsqueda externa o del catálogo.
     *
     * @param libro Libro con los datos a volcar en los campos de texto.
     */
    public void rellenarFormularioManual(Libro libro) {
        if (libro == null) return;

        txtTitulo.setText(libro.getTitulo() != null ? libro.getTitulo() : "");
        txtAutor.setText(libro.getAutor() != null ? libro.getAutor() : "");
        txtEditorial.setText(libro.getEditorial() != null ? libro.getEditorial() : "");
        txtGenero.setText(libro.getGenero() != null ? libro.getGenero() : "");
        txtIsbn.setText(libro.getIsbn() != null ? libro.getIsbn() : "");
        txtAnio.setText(libro.getAño() != null ? libro.getAño() : "");

        if (libro.getSerie() != null) {
            txtSerie.setText(libro.getSerie());
            txtOrden.setText(String.valueOf(libro.getOrdenEnSerie()));
        }

        if (libro.getPortadaURL() != null && !libro.getPortadaURL().isEmpty()) {
            this.rutaPortadaTemporal = libro.getPortadaURL();
            ImageLoader.load(libro.getPortadaURL(), imgPortadaManual, 150, 220);
        }
    }

    /**
     * Abre un selector de archivos nativo para que el usuario escoja una imagen de portada local
     * (PNG, JPG, JPEG, WEBP) y la previsualiza en el formulario.
     *
     * @param event Evento disparado por el botón de selección de portada.
     */
    @FXML
    public void seleccionarImagenManual(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen de Portada");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fileChooser.showOpenDialog(vboxManual.getScene().getWindow());
        if (file != null) {
            this.rutaPortadaTemporal = file.getAbsolutePath();
            imgPortadaManual.setImage(new Image(file.toURI().toString()));
        }
    }

    /**
     * Limpia y restablece todos los campos del formulario manual a sus valores predeterminados.
     */
    @FXML
    public void limpiarCamposManuales() {
        txtTitulo.clear();
        txtAutor.clear();
        txtEditorial.clear();
        txtGenero.clear();
        txtIsbn.clear();
        txtAnio.clear();
        txtSerie.clear();
        txtOrden.clear();
        if (spinnerCantidad != null && spinnerCantidad.getValueFactory() != null) {
            spinnerCantidad.getValueFactory().setValue(1);
        }
        chkPoseidoManual.setSelected(true);
        imgPortadaManual.setImage(null);
        rutaPortadaTemporal = "";
    }

    /**
     * Valida los datos introducidos, gestiona posibles duplicados mediante diálogo interactivo,
     * copia la portada a la carpeta offline local, asigna estanterías y persiste el libro nuevo.
     *
     * @param event Evento disparado al pulsar el botón "Añadir Libro".
     */
    @FXML
    public void anadirLibro(ActionEvent event) {
        String titulo = txtTitulo.getText().trim();
        String autor = txtAutor.getText().trim();
        String isbn = txtIsbn.getText().trim();
        int cantidad = spinnerCantidad != null ? spinnerCantidad.getValue() : 1;
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
        } catch (NumberFormatException ignored) {}

        // Comprobar si ya existe
        Libro libroExistente = null;
        if (listaLibrosCompleta != null) {
            for (Libro l : listaLibrosCompleta) {
                String libroIsbn = l.getIsbn() != null ? l.getIsbn() : "";
                if (!isbn.isEmpty() && !libroIsbn.isEmpty() && libroIsbn.equals(isbn)) {
                    libroExistente = l;
                    break;
                }
                String libroTitulo = l.getTitulo() != null ? l.getTitulo() : "";
                String libroAutor = l.getAutor() != null ? l.getAutor() : "";
                if (libroTitulo.equalsIgnoreCase(titulo) && libroAutor.equalsIgnoreCase(autor)) {
                    libroExistente = l;
                    break;
                }
            }
        }

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
                    libroExistente.setCantidad(libroExistente.getCantidad() + cantidad);
                    if (jsonManager != null && listaLibrosCompleta != null) {
                        jsonManager.guardarLibros(listaLibrosCompleta);
                    }
                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libroExistente, false));
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Stock actualizado: " + libroExistente.getTitulo()));
                    limpiarCamposManuales();
                    return;
                } else if (result.get() != btnNuevo) {
                    return; // Cancelar
                }
            }
        }

        // Crear nuevo
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

        if (!serie.isEmpty()) {
            nuevoLibro.setSerie(serie);
            nuevoLibro.setOrdenEnSerie(orden);
        }

        // Guardar portada en local offline
        String rutaLocal = ImageLoader.hacerPortadaLocalOffline(
                nuevoLibro.getPortadaURL(),
                nuevoLibro.getId(),
                this.rutaUsuario);
        nuevoLibro.setPortadaURL(rutaLocal);

        // Asignar estantería
        List<String> allShelves = jsonManager != null ? jsonManager.cargarEstanterias() : new ArrayList<>();
        if (allShelves == null) allShelves = new ArrayList<>();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("asignar_estanteria.fxml"));
            loader.setResources(this.resources);
            Parent root = loader.load();
            AsignarEstanteriaController controller = loader.getController();
            controller.setEstanterias(allShelves, new ArrayList<>());

            Stage stage = new Stage();
            stage.setTitle("Asignar Estantería");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(vboxManual.getScene().getWindow());
            stage.showAndWait();

            if (controller.isConfirmado()) {
                List<String> selectedShelves = controller.getResultado();
                nuevoLibro.setEstanterias(selectedShelves);

                boolean hayNuevas = false;
                for (String shelf : selectedShelves) {
                    if (!allShelves.contains(shelf)) {
                        allShelves.add(shelf);
                        hayNuevas = true;
                    }
                }
                if (hayNuevas && jsonManager != null) {
                    jsonManager.guardarEstanterias(allShelves);
                    AppEventBus.getInstance().publish(new AppEventBus.EstanteriasActualizadasEvent(allShelves));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir asignar_estanteria.fxml", e);
        }

        if (listaLibrosCompleta != null) {
            listaLibrosCompleta.add(nuevoLibro);
        }
        if (jsonManager != null && listaLibrosCompleta != null) {
            jsonManager.guardarLibros(listaLibrosCompleta);
        }

        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(nuevoLibro, true));
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Libro añadido: " + titulo));
        limpiarCamposManuales();
    }

    /**
     * Muestra un cuadro de diálogo informativo modal.
     *
     * @param titulo  Título de la ventana de alerta.
     * @param mensaje Texto explicativo del diálogo.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
