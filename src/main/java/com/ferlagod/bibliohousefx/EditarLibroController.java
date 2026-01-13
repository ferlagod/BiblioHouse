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

import com.bibliohouse.logic.Libro;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la edición de libros.
 * Permite modificar datos como título, autor, portada o gestionar las
 * estanterías.
 *
 * @author Ferlagod
 * @version 1.0
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
    private TextField txtSerie; // <-- NUEVO
    @FXML
    private TextField txtOrden; // <-- NUEVO
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

    // Estanterías
    @FXML
    private TextField txtNuevaEstanteria;
    @FXML
    private ListView<String> listaEstanterias;
    @FXML
    private ComboBox<String> cmbNuevaEstanteria;

    private Libro libro;
    private String rutaPortadaActual;
    private boolean guardado = false;
    private ObservableList<String> modeloEstanterias;

    /**
     * Configuración inicial de la ventana.
     * Prepara los desplegables de calificación y estado, y configura el selector de
     * año.
     */
    @FXML
    public void initialize() {
        // Inicializar ComboBox de estado de lectura
        cmbEstadoLectura.getItems().addAll("Pendiente", "Leyendo", "Leído");

        // Inicializar ComboBox de calificación
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
     * Carga los datos del libro actual en los controles de la interfaz. Mapea
     * las propiedades del libro a los campos visuales correspondientes.
     */
    private void cargarDatos() {
        if (libro == null) {
            return;
        }

        txtTitulo.setText(libro.getTitulo());
        txtAutor.setText(libro.getAutor());
        txtEditorial.setText(libro.getEditorial());
        txtGenero.setText(libro.getGenero());
        txtIsbn.setText(libro.getIsbn());

        if (libro.getSerie() != null)
            txtSerie.setText(libro.getSerie());
        txtOrden.setText(String.valueOf(libro.getOrdenEnSerie()));

        // Año
        try {
            int anio = Integer.parseInt(libro.getAño());
            spinnerAnio.getValueFactory().setValue(anio);
        } catch (NumberFormatException e) {
            spinnerAnio.getValueFactory().setValue(2024);
        }

        // Estado y Calificación
        // chkLeido.setSelected(libro.isLeido()); // <-- ELIMINADO
        String estado = libro.getEstadoLectura();
        if (estado == null || estado.isEmpty()) {
            estado = "Pendiente";
        }
        cmbEstadoLectura.setValue(estado);

        int calif = libro.getCalificacion();
        if (calif >= 0 && calif <= 5) {
            // El índice 0 es "Sin calificar", 1 es 1 estrella...
            cmbCalificacion.getSelectionModel().select(calif);
        }

        // Estanterías
        modeloEstanterias.setAll(libro.getEstanterias());

        // Portada
        rutaPortadaActual = libro.getPortadaURL();
        cargarImagen(rutaPortadaActual);

        // Poseído
        chkPoseido.setSelected(libro.isPoseido());
    }

    /**
     * Carga y muestra la imagen de portada desde una ruta especificada. Soporta
     * tanto rutas locales como URLs remotas (http/https).
     *
     * @param ruta La ruta del archivo o URL de la imagen.
     */
    private void cargarImagen(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            imgPortada.setImage(null);
            return;
        }

        // Usamos ImageLoader optimizado (aprox 300x450 para edición)
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
                e.printStackTrace();
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
            // Limpiamos por si acaso y añadimos todas
            cmbNuevaEstanteria.getItems().clear();
            cmbNuevaEstanteria.getItems().addAll(todas);
            System.out.println("DEBUG: Se han añadido " + todas.size() + " estanterías al desplegable.");
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
     * Guarda los cambios si las validaciones son correctas.
     * Actualiza el objeto libro con la nueva información y cierra la ventana.
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
        // libro.setLeido(chkLeido.isSelected()); // <-- ELIMINADO (Ya se maneja dentro
        // de setEstadoLectura)

        // Calificación: obtenemos el índice seleccionado (0=Sin calificar, 1=1
        // estrella...)
        int califIndex = cmbCalificacion.getSelectionModel().getSelectedIndex();
        libro.setCalificacion(califIndex < 0 ? 0 : califIndex);

        // Guardar estanterías
        libro.setEstanterias(new ArrayList<>(modeloEstanterias));

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

    private void cerrar() {
        Stage stage = (Stage) txtTitulo.getScene().getWindow();
        stage.close();
    }

    /**
     * Indica si los cambios fueron guardados exitosamente.
     *
     * @return {@code true} si el usuario pulsó Guardar y la operación fue
     *         exitosa, {@code false} en caso contrario.
     */
    public boolean isGuardado() {
        return guardado;
    }

    /**
     * Muestra una alerta modal al usuario.
     *
     * @param titulo  El título de la ventana de alerta.
     * @param mensaje El contenido del mensaje a mostrar.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}