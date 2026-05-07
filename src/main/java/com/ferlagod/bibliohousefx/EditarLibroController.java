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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.bibliohouse.logic.Libro;
import java.util.concurrent.ExecutionException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controlador para la edición de libros. Permite modificar datos como título,
 * autor, portada o gestionar las estanterías.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class EditarLibroController {

    @FXML
    private TextField txtTitulo; // Campo de texto para el título del libro
    @FXML
    private TextField txtAutor; // Campo de texto para el autor
    @FXML
    private TextField txtEditorial; // Campo de texto para la editorial
    @FXML
    private TextField txtGenero; // Campo de texto para el género
    @FXML
    private TextField txtIsbn; // Campo de texto para el ISBN
    @FXML
    private TextField txtSerie;
    @FXML
    private TextField txtOrden;
    @FXML
    private Spinner<Integer> spinnerAnio; // Selector numérico para el año de publicación
    @FXML
    private ComboBox<String> cmbEstadoLectura; // Desplegable para el estado de lectura
    @FXML
    private ComboBox<String> cmbCalificacion; // Desplegable para seleccionar la calificación
    @FXML
    private ImageView imgPortada; // Componente para mostrar la imagen de portada
    @FXML
    private TextArea txtResena; // Campo de texto multilínea para la reseña (si aplica)
    @FXML
    private CheckBox chkPoseido; // Checkbox para indicar si se posee el libro o no
    @FXML
    private TextField txtNuevaEstanteria; // Estanterías
    @FXML
    private ListView<String> listaEstanterias;
    @FXML
    private ComboBox<String> cmbNuevaEstanteria;
    private Libro libro;
    private String rutaPortadaActual;
    private boolean guardado = false;
    private ObservableList<String> modeloEstanterias;
    private String rutaUsuario;
    @FXML
    private javafx.scene.control.Button btnBuscarPortada;

    /**
     * Configuración inicial de la ventana. Prepara los desplegables de
     * calificación y estado, y configura el selector de año.
     */
    @FXML
    public void initialize() {
        // Inicializar ComboBox de estado de lectura y de calificación
        cmbEstadoLectura.getItems().addAll("Pendiente", "Leyendo", "Leído");
        cmbCalificacion.getItems().addAll(
                "Sin calificar",
                "★ (Malo)",
                "★★ (Regular)",
                "★★★ (Bueno)",
                "★★★★ (Muy bueno)",
                "★★★★★ (Excelente)");

        // Configurar Spinner de año
        spinnerAnio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3000, 2024));

        // Inicializar lista de estanterías
        modeloEstanterias = FXCollections.observableArrayList();
        listaEstanterias.setItems(modeloEstanterias);

        // Configuración Drag & Drop para la portada 
        imgPortada.setOnDragOver(event -> {
            // Aceptamos solo si lo que arrastran es un archivo (o varios)
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        imgPortada.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                // Cogemos solo el primer archivo, por si sueltan varios de golpe
                File file = db.getFiles().get(0);

                // Verificamos de forma sencilla si es una imagen por la extensión
                String nombre = file.getName().toLowerCase();
                if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") || nombre.endsWith(".png") || nombre.endsWith(".gif")) {
                    try {
                        rutaPortadaActual = file.getAbsolutePath();
                        com.bibliohouse.utils.ImageLoader.load(rutaPortadaActual, imgPortada, 300, 450);
                        success = true;
                    } catch (Exception e) {
                        mostrarAlerta("Error", "No se pudo cargar la imagen soltada.");
                    }
                } else {
                    mostrarAlerta("Formato incorrecto", "Solo se aceptan archivos de imagen (.jpg, .png, .gif).");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        // Cuando el archivo entra en la zona de la imagen, la ponemos medio transparente
        imgPortada.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                imgPortada.setOpacity(0.5);
            }
        });

        // Cuando el archivo sale de la zona (o se suelta), le devolvemos su color normal
        imgPortada.setOnDragExited(event -> {
            imgPortada.setOpacity(1.0);
        });
        // FIN Drag & Drop 

        // Obligar al spinner a convertir a número lo que se escriba, ignorando letras
        javafx.util.converter.IntegerStringConverter converter = new javafx.util.converter.IntegerStringConverter();
        spinnerAnio.getValueFactory().setConverter(converter);
    }

    /**
     * Establece el libro que se va a editar y carga sus datos en la vista.
     *
     * @param libro El objeto {@link Libro} a editar.
     */
    public void setLibro(Libro libro) {
        this.libro = libro;
        cargarDatos();
    }

    /**
     * Establece la ruta del directorio personal del usuario. Esta ruta se
     * utiliza como ubicación base para guardar y cargar los datos de la
     * biblioteca, incluyendo libros, portadas y configuraciones.
     *
     * @param rutaUsuario Ruta absoluta del directorio del usuario.
     */
    public void setRutaUsuario(String rutaUsuario) {
        this.rutaUsuario = rutaUsuario;
    }

    /**
     * Carga los datos de un libro en los controles de edición de la interfaz de
     * usuario. Sincroniza todas las propiedades del libro con los campos
     * correspondientes del formulario.
     *
     */
    private void cargarDatos() {
        if (libro == null) {
            return;
        }

        // Campos de texto básicos
        txtTitulo.setText(libro.getTitulo());
        txtAutor.setText(libro.getAutor());
        txtEditorial.setText(libro.getEditorial());
        txtGenero.setText(libro.getGenero());
        txtIsbn.setText(libro.getIsbn());
        txtResena.setText(libro.getReseña() != null ? libro.getReseña() : "");

        // Serie y orden
        if (libro.getSerie() != null) {
            txtSerie.setText(libro.getSerie());
        }
        txtOrden.setText(String.valueOf(libro.getOrdenEnSerie()));

        // Año (con manejo de errores)
        try {
            int anio = Integer.parseInt(libro.getAño());
            spinnerAnio.getValueFactory().setValue(anio);
        } catch (NumberFormatException e) {
            spinnerAnio.getValueFactory().setValue(2024); // Valor por defecto
        }

        // Estado de lectura
        String estado = libro.getEstadoLectura();
        if (estado == null || estado.isEmpty()) {
            estado = "Pendiente";
        }
        cmbEstadoLectura.setValue(estado);

        // Calificación (0-5 estrellas)
        int calif = libro.getCalificacion();
        if (calif >= 0 && calif <= 5) {
            cmbCalificacion.getSelectionModel().select(calif);
        }

        // Estanterías
        modeloEstanterias.setAll(libro.getEstanterias());

        // Portada
        rutaPortadaActual = libro.getPortadaURL();
        cargarImagen(rutaPortadaActual);

        // Estado de posesión
        chkPoseido.setSelected(libro.isPoseido());
    }

    /**
     * Carga y muestra la imagen de portada desde una ruta especificada. Soporta
     * tanto rutas locales como URLs remotas (http/https).
     *
     * @param ruta La ruta del archivo o URL de la imagen.
     */
    private void cargarImagen(String ruta) {
        // Usamos ImageLoader optimizado (aprox 300x450 para edición)
        // ImageLoader se encarga de cargar el default si ruta es null o vacía
        com.bibliohouse.utils.ImageLoader.load(ruta, imgPortada, 300, 450);
    }

    /**
     * Maneja el evento de cambio de portada. Abre un selector de archivos para
     * elegir una imagen local y actualiza la vista previa.
     *
     * @param event El evento de acción que desencadena este método.
     */
    @FXML
    private void cambiarPortada(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Portada");

        // Filtro para ver solo imágenes
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

        // Mostrar diálogo
        File file = fileChooser.showOpenDialog(txtTitulo.getScene().getWindow());

        if (file != null) {
            try {
                // 1. Guardamos la ruta absoluta en la variable temporal
                rutaPortadaActual = file.getAbsolutePath();

                // 2. Cargamos la imagen visualmente para previsualizar usando ImageLoader
                // Reducimos tamaño para que no pese
                com.bibliohouse.utils.ImageLoader.load(rutaPortadaActual, imgPortada, 300, 450);

            } catch (Exception e) {
                mostrarAlerta("Error", "No se pudo cargar la imagen seleccionada.");
            }
        }
    }

    /**
     * Establece la lista de todas las estanterías disponibles en el sistema.
     * Llena el ComboBox para permitir al usuario seleccionar estanterías
     * existentes.
     *
     * @param todas Lista de nombres de todas las estanterías.
     */
    public void setEstanteriasDisponibles(List<String> todas) {
        if (todas != null) {
            cmbNuevaEstanteria.getItems().clear();
            cmbNuevaEstanteria.getItems().addAll(todas);
        }
    }

    /**
     * Añade una nueva estantería a la lista del libro. Toma el valor del
     * ComboBox de nuevas estanterías y lo añade si no está vacío ni duplicado.
     *
     * @param event El evento de acción.
     */
    @FXML
    private void anadirEstanteria(ActionEvent event) {
        // Obtenemos el valor escrito o seleccionado
        String nueva = cmbNuevaEstanteria.getValue();
        if (nueva != null && !nueva.trim().isEmpty() && !modeloEstanterias.contains(nueva.trim())) {
            modeloEstanterias.add(nueva.trim());
            cmbNuevaEstanteria.setValue(null); // Limpiar
        }
    }

    /**
     * Elimina la estantería seleccionada de la lista visual del libro.
     *
     * @param event El evento de acción.
     */
    @FXML
    private void eliminarEstanteria(ActionEvent event) {
        String seleccionada = listaEstanterias.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            modeloEstanterias.remove(seleccionada);
        }
    }

    /**
     * Guarda los cambios si las validaciones son correctas. Actualiza el objeto
     * libro con la nueva información y cierra la ventana.
     *
     * @param event El clic del botón Guardar.
     */
    @FXML
    private void guardar(ActionEvent event) {
        // Validaciones básicas
        if (txtTitulo.getText().isEmpty()) {
            mostrarAlerta("Error", "El título no puede estar vacío.");
            return;
        }

        // Actualizar objeto Libro
        libro.setTitulo(txtTitulo.getText());
        libro.setAutor(txtAutor.getText());
        libro.setEditorial(txtEditorial.getText());
        libro.setGenero(txtGenero.getText());
        libro.setIsbn(txtIsbn.getText());
        libro.setAño(String.valueOf(spinnerAnio.getValue()));

        libro.setSerie(txtSerie.getText());
        double orden = 0.0;
        try {
            if (!txtOrden.getText().isEmpty()) {
                orden = Double.parseDouble(txtOrden.getText().replace(",", "."));
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }
        libro.setOrdenEnSerie(orden);

        libro.setEstadoLectura(cmbEstadoLectura.getValue());

        // Calificación: obtenemos el índice seleccionado (0=Sin calificar, 1=1
        // estrella...)
        int califIndex = cmbCalificacion.getSelectionModel().getSelectedIndex();
        libro.setCalificacion(califIndex < 0 ? 0 : califIndex);

        // Guardar estanterías
        libro.setEstanterias(new ArrayList<>(modeloEstanterias));
        
        libro.setReseña(txtResena.getText());

        if (this.rutaUsuario != null && !this.rutaUsuario.isEmpty()) {
            rutaPortadaActual = com.bibliohouse.utils.ImageLoader.hacerPortadaLocalOffline(
                    rutaPortadaActual,
                    libro.getId(),
                    this.rutaUsuario
            );
        }

        // Guardar portada
        libro.setPortadaURL(rutaPortadaActual);

        // Guardar poseído
        libro.setPoseido(chkPoseido.isSelected());

        guardado = true;
        cerrar();
    }

    /**
     * Cancela la edición y cierra la ventana sin guardar cambios.
     *
     * @param event El evento de acción.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        guardado = false;
        cerrar();
    }

    /**
     * Cierra la ventana actual de la aplicación. Obtiene la referencia a la
     * ventana a través del campo txtTitulo y la cierra.
     */
    private void cerrar() {
        Stage stage = (Stage) txtTitulo.getScene().getWindow();
        stage.close();
    }

    /**
     * Indica si los cambios fueron guardados exitosamente.
     *
     * @return {@code true} si el usuario pulsó Guardar y la operación fue
     * exitosa, {@code false} en caso contrario.
     */
    public boolean isGuardado() {
        return guardado;
    }

    /**
     * Muestra una alerta modal al usuario.
     *
     * @param titulo El título de la ventana de alerta.
     * @param mensaje El contenido del mensaje a mostrar.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Busca la portada de un libro en línea utilizando APIs externas (Google
     * Books, OpenLibrary, Inventaire). Realiza la búsqueda en segundo plano
     * para no bloquear la interfaz de usuario.
     *
     * @param event Evento que desencadena la acción (no utilizado
     * directamente).
     */
    @FXML
    private void buscarPortadaOnline(javafx.event.ActionEvent event) {
        String isbn = txtIsbn.getText().trim();
        String titulo = txtTitulo.getText().trim();

        if (isbn.isEmpty() && titulo.isEmpty()) {
            mostrarAlerta("Atención", "Necesitas tener escrito al menos el Título o el ISBN para poder buscar la portada.");
            return;
        }

        btnBuscarPortada.setDisable(true);

        Thread searchThread = new Thread(() -> {
            try {
                String urlFinal = "";

                // 1. Primer intento: Buscar por ISBN (prioridad para edición exacta)
                if (!isbn.isEmpty()) {
                    urlFinal = buscarImagenEnApis(isbn);
                }

                // 2. Segundo intento: Buscar por título si el ISBN no dio resultados
                if (urlFinal.isEmpty() && !titulo.isEmpty()) {
                    urlFinal = buscarImagenEnApis(titulo);
                }

                final String portadaDefinitiva = urlFinal;

                javafx.application.Platform.runLater(() -> {
                    btnBuscarPortada.setDisable(false);
                    if (!portadaDefinitiva.isEmpty()) {
                        // Generar ID único si el libro no tiene ID
                        String idLibro = (libro != null && libro.getId() != null) ? libro.getId() : java.util.UUID.randomUUID().toString();
                        // Descargar y guardar localmente la portada
                        String rutaLocal = com.bibliohouse.utils.ImageLoader.hacerPortadaLocalOffline(portadaDefinitiva, idLibro, this.rutaUsuario);

                        rutaPortadaActual = rutaLocal;
                        com.bibliohouse.utils.ImageLoader.load(rutaPortadaActual, imgPortada, 300, 450);
                    } else {
                        mostrarAlerta("Sin resultados", "Ninguna de las tres bases de datos (Google, OpenLibrary, Inventaire) tiene una portada registrada para esta búsqueda. Tendrás que descargarla y añadirla manualmente.");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    btnBuscarPortada.setDisable(false);
                    mostrarAlerta("Error", "Error al conectar con los servidores.");
                });
            }
        });

        searchThread.setDaemon(true);
        searchThread.start();
    }

    /**
     * Busca la portada de un libro en múltiples APIs de libros (Google Books,
     * OpenLibrary, Inventaire) de forma concurrentes y devuelve la primera URL
     * de portada válida encontrada.
     *
     * @param query Término de búsqueda (ISBN o título del libro).
     * @return URL de la primera portada válida encontrada, o cadena vacía si no
     * se encuentra ninguna.
     */
    private String buscarImagenEnApis(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        try {
            // Consultas asíncronas a las tres APIs con timeout de 3 segundos
            java.util.concurrent.CompletableFuture<java.util.List<com.bibliohouse.logic.Libro>> futureGoogle = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.GoogleBooksCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            java.util.concurrent.CompletableFuture<java.util.List<com.bibliohouse.logic.Libro>> futureOpenLib = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.OpenLibraryCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            java.util.concurrent.CompletableFuture<java.util.List<com.bibliohouse.logic.Libro>> futureInventaire = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> com.bibliohouse.logic.InventaireCliente.buscarLibros(query))
                    .completeOnTimeout(new java.util.ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(ex -> new java.util.ArrayList<>());

            // Esperar a que todas las consultas finalicen
            java.util.concurrent.CompletableFuture.allOf(futureGoogle, futureOpenLib, futureInventaire).join();

            // Prioridad 1: Google Books
            if (futureGoogle.get() != null) {
                for (com.bibliohouse.logic.Libro lib : futureGoogle.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
            // Prioridad 2: OpenLibrary (con ajuste de tamaño)
            if (futureOpenLib.get() != null) {
                for (com.bibliohouse.logic.Libro lib : futureOpenLib.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover") && !img.contains("-S.jpg")) {
                        return img.replace("-M.jpg", "-L.jpg"); // Preferir tamaño grande
                    }
                }
            }
            // Prioridad 3: Inventaire
            if (futureInventaire.get() != null) {
                for (com.bibliohouse.logic.Libro lib : futureInventaire.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // Si falla algo, devolvemos vacío para que intente el siguiente plan
        }
        return "";
    }

}
