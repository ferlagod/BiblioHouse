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
import com.bibliohouse.logic.ServicioExportarWeb;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.LanguageManager;

/**
 * Controlador para la ventana de exportación de catálogo web HTML. Permite
 * filtrar libros, personalizar el nombre de la biblioteca y elegir el modo de
 * agrupación antes de generar el archivo HTML estático.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class ExportarWebController {

    @FXML
    private TextField txtNombreBiblioteca;
    @FXML
    private ComboBox<String> cmbAgrupacion;
    @FXML
    private TextField txtBuscar;
    @FXML
    private TextField txtAutor;
    @FXML
    private TextField txtAnioDesde;
    @FXML
    private TextField txtAnioHasta;
    @FXML
    private ComboBox<String> cmbEstadoLectura;
    @FXML
    private ListView<String> listaEstanterias;
    @FXML
    private ListView<String> listaGeneros;
    @FXML
    private CheckBox chkTodasEstanterias;
    @FXML
    private CheckBox chkTodosGeneros;
    @FXML
    private Label lblContadorLibros;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Button btnExportar;

    // Datos
    private List<Libro> todosLosLibros;
    private List<String> todasEstanterias;
    private List<String> todosGeneros;

    // Mapas para manejar selección de checkboxes
    private Map<String, SimpleBooleanProperty> seleccionEstanterias;
    private Map<String, SimpleBooleanProperty> seleccionGeneros;

    /**
     * Inicializa el controlador. Configura los listeners para actualizar el
     * contador de libros y los ComboBox iniciales.
     */
    @FXML
    public void initialize() {
        seleccionEstanterias = new HashMap<>();
        seleccionGeneros = new HashMap<>();

        // Configurar modos de agrupación
        String txtGenero = LanguageManager.getString("export.group.genre", "Género");
        String txtEstanteria = LanguageManager.getString("export.group.shelf", "Estantería");
        cmbAgrupacion.setItems(FXCollections.observableArrayList(txtGenero, txtEstanteria));
        cmbAgrupacion.setValue(txtGenero);

        // Configurar estados de lectura
        String txtTodos = LanguageManager.getString("export.status.all", "Todos");
        ObservableList<String> itemsEstado = FXCollections.observableArrayList(txtTodos);
        for (EstadoLectura e : EstadoLectura.values()) {
            itemsEstado.add(e.getEtiqueta());
        }
        cmbEstadoLectura.setItems(itemsEstado);
        cmbEstadoLectura.setValue(txtTodos);

        // Nombre por defecto
        txtNombreBiblioteca.setText(LanguageManager.getString("export.library.default", "Mi Biblioteca"));

        // Listeners para actualizar contador
        txtBuscar.textProperty().addListener((obs, old, newVal) -> actualizarContador());
        txtAutor.textProperty().addListener((obs, old, newVal) -> actualizarContador());
        txtAnioDesde.textProperty().addListener((obs, old, newVal) -> actualizarContador());
        txtAnioHasta.textProperty().addListener((obs, old, newVal) -> actualizarContador());
        cmbEstadoLectura.valueProperty().addListener((obs, old, newVal) -> actualizarContador());
    }

    /**
     * Establece los datos necesarios para el controlador.
     *
     * @param libros Lista completa de libros.
     * @param estanterias Lista de estanterías disponibles.
     * @param generos Lista de géneros disponibles.
     */
    public void setDatos(List<Libro> libros, List<String> estanterias, List<String> generos) {
        this.todosLosLibros = new ArrayList<>(libros);
        this.todasEstanterias = new ArrayList<>(estanterias);
        this.todosGeneros = new ArrayList<>(generos);

        cargarEstanterias();
        cargarGeneros();
        actualizarContador();
    }

    /**
     * Carga las estanterías disponibles en un ListView con checkboxes.
     */
    private void cargarEstanterias() {
        ObservableList<String> items = FXCollections.observableArrayList(todasEstanterias);
        listaEstanterias.setItems(items);

        for (String estanteria : todasEstanterias) {
            seleccionEstanterias.put(estanteria, new SimpleBooleanProperty(true));
        }

        listaEstanterias.setCellFactory(CheckBoxListCell.forListView(estanteria -> {
            SimpleBooleanProperty prop = seleccionEstanterias.get(estanteria);
            prop.addListener((obs, old, newVal) -> {
                actualizarContador();
                verificarTodasEstanterias();
            });
            return prop;
        }));
    }

    /**
     * Carga los géneros en un ListView con checkboxes.
     */
    private void cargarGeneros() {
        ObservableList<String> items = FXCollections.observableArrayList(todosGeneros);
        listaGeneros.setItems(items);

        for (String genero : todosGeneros) {
            seleccionGeneros.put(genero, new SimpleBooleanProperty(true));
        }

        listaGeneros.setCellFactory(CheckBoxListCell.forListView(genero -> {
            SimpleBooleanProperty prop = seleccionGeneros.get(genero);
            prop.addListener((obs, old, newVal) -> {
                actualizarContador();
                verificarTodosGeneros();
            });
            return prop;
        }));
    }

    /**
     * Maneja el toggle de "Todas las estanterías".
     */
    @FXML
    private void toggleTodasEstanterias(ActionEvent event) {
        boolean seleccionar = chkTodasEstanterias.isSelected();
        for (SimpleBooleanProperty prop : seleccionEstanterias.values()) {
            prop.set(seleccionar);
        }
        listaEstanterias.refresh();
        actualizarContador();
    }

    /**
     * Maneja el toggle de "Todos los géneros".
     */
    @FXML
    private void toggleTodosGeneros(ActionEvent event) {
        boolean seleccionar = chkTodosGeneros.isSelected();
        for (SimpleBooleanProperty prop : seleccionGeneros.values()) {
            prop.set(seleccionar);
        }
        listaGeneros.refresh();
        actualizarContador();
    }

    /**
     * Verifica si todas las estanterías están seleccionadas.
     */
    private void verificarTodasEstanterias() {
        boolean todasSeleccionadas = seleccionEstanterias.values().stream()
                .allMatch(SimpleBooleanProperty::get);
        chkTodasEstanterias.setSelected(todasSeleccionadas);
    }

    /**
     * Verifica si todos los géneros están seleccionados.
     */
    private void verificarTodosGeneros() {
        boolean todosSeleccionados = seleccionGeneros.values().stream()
                .allMatch(SimpleBooleanProperty::get);
        chkTodosGeneros.setSelected(todosSeleccionados);
    }

    /**
     * Actualiza el contador de libros que coinciden con los filtros.
     */
    private void actualizarContador() {
        List<Libro> librosFiltrados = aplicarFiltros();
        lblContadorLibros.setText(LanguageManager.getString("export.matching", "Libros que coinciden: ") + librosFiltrados.size());
        btnExportar.setDisable(librosFiltrados.isEmpty());
    }

    /**
     * Aplica los filtros seleccionados a la lista de libros.
     *
     * @return Lista de libros filtrados.
     */
    private List<Libro> aplicarFiltros() {
        return todosLosLibros.stream()
                .filter(this::cumpleFiltros)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un libro cumple con todos los filtros seleccionados.
     *
     * @param libro Libro a verificar.
     * @return true si cumple con todos los filtros.
     */
    private boolean cumpleFiltros(Libro libro) {
        // Filtro por búsqueda de título/ISBN
        String busqueda = txtBuscar.getText().trim().toLowerCase();
        if (!busqueda.isEmpty()) {
            boolean coincideTitulo = libro.getTitulo() != null
                    && libro.getTitulo().toLowerCase().contains(busqueda);
            boolean coincideIsbn = libro.getIsbn() != null
                    && libro.getIsbn().toLowerCase().contains(busqueda);
            if (!coincideTitulo && !coincideIsbn) {
                return false;
            }
        }

        // Filtro por autor
        String autor = txtAutor.getText().trim().toLowerCase();
        if (!autor.isEmpty()) {
            if (libro.getAutor() == null || !libro.getAutor().toLowerCase().contains(autor)) {
                return false;
            }
        }

        // Filtro por estanterías
        if (!chkTodasEstanterias.isSelected()) {
            List<String> estanteriasLibro = libro.getEstanterias();
            if (estanteriasLibro == null || estanteriasLibro.isEmpty()) {
                boolean algunaSeleccionada = seleccionEstanterias.values().stream()
                        .anyMatch(SimpleBooleanProperty::get);
                if (algunaSeleccionada) {
                    return false;
                }
            } else {
                boolean algunaCoincide = estanteriasLibro.stream()
                        .anyMatch(est -> seleccionEstanterias.getOrDefault(est,
                        new SimpleBooleanProperty(false)).get());
                if (!algunaCoincide) {
                    return false;
                }
            }
        }

        // Filtro por géneros
        if (!chkTodosGeneros.isSelected()) {
            String generoLibro = libro.getGenero();
            if (generoLibro == null || generoLibro.isEmpty()) {
                return false;
            }
            SimpleBooleanProperty seleccionado = seleccionGeneros.get(generoLibro);
            if (seleccionado == null || !seleccionado.get()) {
                return false;
            }
        }

        // Filtro por año desde
        String anioDesde = txtAnioDesde.getText().trim();
        if (!anioDesde.isEmpty()) {
            try {
                int anioDesdeInt = Integer.parseInt(anioDesde);
                String anioLibro = libro.getAño();
                if (anioLibro == null || anioLibro.isEmpty()) {
                    return false;
                }
                int anioLibroInt = Integer.parseInt(anioLibro);
                if (anioLibroInt < anioDesdeInt) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Ignorar si el formato no es válido
            }
        }

        // Filtro por año hasta
        String anioHasta = txtAnioHasta.getText().trim();
        if (!anioHasta.isEmpty()) {
            try {
                int anioHastaInt = Integer.parseInt(anioHasta);
                String anioLibro = libro.getAño();
                if (anioLibro == null || anioLibro.isEmpty()) {
                    return false;
                }
                int anioLibroInt = Integer.parseInt(anioLibro);
                if (anioLibroInt > anioHastaInt) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Ignorar si el formato no es válido
            }
        }

        // Filtro por estado de lectura
        String estadoSeleccionado = cmbEstadoLectura.getValue();
        String txtTodos = LanguageManager.getString("export.status.all", "Todos");
        if (estadoSeleccionado != null && !estadoSeleccionado.equals(txtTodos)) {
            EstadoLectura estadoObjetivo = EstadoLectura.fromString(estadoSeleccionado);
            if (libro.getEstadoLecturaEnum() != estadoObjetivo) {
                return false;
            }
        }

        return true;
    }

    /**
     * Limpia todos los filtros aplicados.
     */
    @FXML
    private void limpiarFiltros(ActionEvent event) {
        txtBuscar.clear();
        txtAutor.clear();
        txtAnioDesde.clear();
        txtAnioHasta.clear();
        cmbEstadoLectura.setValue(LanguageManager.getString("export.status.all", "Todos"));

        chkTodasEstanterias.setSelected(true);
        toggleTodasEstanterias(null);

        chkTodosGeneros.setSelected(true);
        toggleTodosGeneros(null);

        actualizarContador();
    }

    /**
     * Cancela la exportación y cierra la ventana.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        Stage stage = (Stage) btnExportar.getScene().getWindow();
        stage.close();
    }

    /**
     * Exporta los libros filtrados a un catálogo HTML.
     */
    @FXML
    private void exportarWeb(ActionEvent event) {
        List<Libro> librosFiltrados = aplicarFiltros();

        if (librosFiltrados.isEmpty()) {
            mostrarAlerta(LanguageManager.getString("export.empty.title", "No hay libros"),
                    LanguageManager.getString("export.empty.content", "No hay libros que coincidan con los filtros seleccionados."),
                    Alert.AlertType.WARNING);
            return;
        }

        // Obtener nombre de la biblioteca
        String nombreBiblioteca = txtNombreBiblioteca.getText().trim();
        if (nombreBiblioteca.isEmpty()) {
            nombreBiblioteca = LanguageManager.getString("export.library.default", "Mi Biblioteca");
        }

        // Obtener modo de agrupación
        String agrupacionStr = cmbAgrupacion.getValue();
        String txtEstanteria = LanguageManager.getString("export.group.shelf", "Estantería");
        ServicioExportarWeb.ModoAgrupacion modo = txtEstanteria.equals(agrupacionStr)
                ? ServicioExportarWeb.ModoAgrupacion.ESTANTERIA
                : ServicioExportarWeb.ModoAgrupacion.GENERO;

        // Mostrar diálogo para seleccionar ubicación del archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getString("export.web.save.title", "Guardar Catálogo Web"));
        fileChooser.setInitialFileName(LanguageManager.getString("export.web.save.default", "catalogo_biblioteca.html"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(LanguageManager.getString("export.web.save.html", "Archivos HTML"), "*.html"));

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino == null) {
            return; // Usuario canceló
        }

        // Asegurar extensión .html
        if (!archivoDestino.getName().toLowerCase().endsWith(".html")) {
            archivoDestino = new File(archivoDestino.getAbsolutePath() + ".html");
        }

        // Mostrar indicador de progreso
        progressIndicator.setVisible(true);
        btnExportar.setDisable(true);

        // Exportar en hilo separado para no bloquear la UI
        File finalDestino = archivoDestino;
        String finalNombre = nombreBiblioteca;
        ServicioExportarWeb.ModoAgrupacion finalModo = modo;

        new Thread(() -> {
            ServicioExportarWeb webService = new ServicioExportarWeb();
            boolean exito = webService.exportarCatalogoWeb(librosFiltrados, finalDestino,
                    finalNombre, finalModo);

            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                btnExportar.setDisable(false);

                if (exito) {
                    String formatExito = LanguageManager.getString("export.web.success.content", "El catálogo web se ha generado correctamente en:\n%s\n\nPuedes abrirlo en cualquier navegador, subirlo a un hosting o compartirlo por WhatsApp.");
                    mostrarAlerta(LanguageManager.getString("export.web.success.title", "Exportación exitosa"),
                            String.format(formatExito, finalDestino.getAbsolutePath()),
                            Alert.AlertType.INFORMATION);
                    stage.close();
                } else {
                    mostrarAlerta(LanguageManager.getString("export.web.error.title", "Error al exportar"),
                            LanguageManager.getString("export.web.error.content", "Hubo un error al generar el catálogo web. Revise los logs para más detalles."),
                            Alert.AlertType.ERROR);
                }
            });
        }).start();
    }

    /**
     * Muestra una alerta al usuario.
     *
     * @param titulo Título de la alerta.
     * @param mensaje Mensaje de la alerta.
     * @param tipo Tipo de alerta.
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
