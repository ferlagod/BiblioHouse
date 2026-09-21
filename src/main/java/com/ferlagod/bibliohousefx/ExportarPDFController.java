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
import com.bibliohouse.logic.ServicioExportarPdf;
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
 * Controlador para la ventana de exportación de informes PDF. Permite filtrar
 * libros por diversos criterios y exportarlos a PDF.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class ExportarPDFController {

    @FXML
    private TextField txtBuscar;
    @FXML
    private TextField txtAutor;
    @FXML
    private ListView<String> listaEstanterias;
    @FXML
    private ListView<String> listaGeneros;
    @FXML
    private CheckBox chkTodasEstanterias;
    @FXML
    private CheckBox chkTodosGeneros;
    @FXML
    private TextField txtAnioDesde;
    @FXML
    private TextField txtAnioHasta;
    @FXML
    private ComboBox<String> cmbEstadoLectura;
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
     * contador de libros.
     */
    @FXML
    public void initialize() {
        seleccionEstanterias = new HashMap<>();
        seleccionGeneros = new HashMap<>();

        // Configurar estados de lectura
        String txtTodos = LanguageManager.getString("export.status.all", "Todos");
        ObservableList<String> itemsEstado = FXCollections.observableArrayList(txtTodos);
        for (EstadoLectura e : EstadoLectura.values()) {
            itemsEstado.add(e.getEtiqueta());
        }
        cmbEstadoLectura.setItems(itemsEstado);
        cmbEstadoLectura.setValue(txtTodos);

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
     * Carga las estanterías disponibles en un ListView con checkboxes para
     * permitir la selección múltiple. Este método:
     *
     * El estado de selección de cada estantería se almacena en el mapa
     * {@code seleccionEstanterias} para su posterior uso.
     */
    private void cargarEstanterias() {
        ObservableList<String> items = FXCollections.observableArrayList(todasEstanterias);
        listaEstanterias.setItems(items);

        // Configurar checkboxes: todas las estanterías seleccionadas por defecto
        for (String estanteria : todasEstanterias) {
            seleccionEstanterias.put(estanteria, new SimpleBooleanProperty(true));
        }

        // Configurar fábrica de celdas con checkboxes
        listaEstanterias.setCellFactory(CheckBoxListCell.forListView(estanteria -> {
            SimpleBooleanProperty prop = seleccionEstanterias.get(estanteria);
            // Listeners para actualizar el estado al cambiar la selección
            prop.addListener((obs, old, newVal) -> {
                actualizarContador();
                verificarTodasEstanterias();
            });
            return prop;
        }));
    }

    /**
     * Carga los géneros en un ListView con checkboxes, todos seleccionados por
     * defecto. Asocia listeners para actualizar el contador y verificar el
     * estado de selección.
     */
    private void cargarGeneros() {
        ObservableList<String> items = FXCollections.observableArrayList(todosGeneros);
        listaGeneros.setItems(items);

        // Configurar checkboxes
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
        lblContadorLibros.setText(LanguageManager.getString("export.matching", "Libros que coinciden con los filtros: ") + librosFiltrados.size());
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
     * @return true si cumple con todos los filtros, false en caso contrario.
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
                // Libro sin estantería - verificar si hay alguna seleccionada
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
     * Exporta los libros filtrados a PDF.
     */
    @FXML
    private void exportarPDF(ActionEvent event) {
        List<Libro> librosFiltrados = aplicarFiltros();

        if (librosFiltrados.isEmpty()) {
            mostrarAlerta(LanguageManager.getString("export.empty.title", "No hay libros"),
                    LanguageManager.getString("export.empty.content", "No hay libros que coincidan con los filtros seleccionados."),
                    Alert.AlertType.WARNING);
            return;
        }

        // Mostrar diálogo para seleccionar ubicación del archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LanguageManager.getString("export.pdf.save.title", "Guardar Informe PDF"));
        fileChooser.setInitialFileName(LanguageManager.getString("export.pdf.save.default", "informe_biblioteca.pdf"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(LanguageManager.getString("export.pdf.save.pdf", "Archivos PDF"), "*.pdf"));

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino == null) {
            return; // Usuario canceló
        }

        // Asegurar que el archivo tenga extensión .pdf
        if (!archivoDestino.getName().toLowerCase().endsWith(".pdf")) {
            archivoDestino = new File(archivoDestino.getAbsolutePath() + ".pdf");
        }

        // Generar descripción de filtros
        String criteriosFiltro = generarDescripcionFiltros();

        // Agrupar libros por estantería
        Map<String, List<Libro>> librosPorEstanteria = agruparPorEstanteria(librosFiltrados);

        // Mostrar indicador de progreso
        progressIndicator.setVisible(true);
        btnExportar.setDisable(true);

        // Exportar en hilo separado para no bloquear la UI
        File finalDestino = archivoDestino;
        new Thread(() -> {
            ServicioExportarPdf pdfService = new ServicioExportarPdf();
            boolean exito = pdfService.exportarLibrosPDF(librosFiltrados, librosPorEstanteria,
                    finalDestino, criteriosFiltro);

            // Actualizar UI en el hilo de JavaFX
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                btnExportar.setDisable(false);

                if (exito) {
                    String formatExito = LanguageManager.getString("export.pdf.success.content", "El informe PDF se ha generado correctamente en:\n%s");
                    mostrarAlerta(LanguageManager.getString("export.pdf.success.title", "Exportación exitosa"),
                            String.format(formatExito, finalDestino.getAbsolutePath()),
                            Alert.AlertType.INFORMATION);
                    stage.close();
                } else {
                    mostrarAlerta(LanguageManager.getString("export.pdf.error.title", "Error al exportar"),
                            LanguageManager.getString("export.pdf.error.content", "Hubo un error al generar el archivo PDF. Revise los logs para más detalles."),
                            Alert.AlertType.ERROR);
                }
            });
        }).start();
    }

    /**
     * Genera una descripción textual de los filtros aplicados.
     *
     * @return Descripción de los filtros.
     */
    private String generarDescripcionFiltros() {
        StringBuilder sb = new StringBuilder();

        if (!txtBuscar.getText().trim().isEmpty()) {
            sb.append(LanguageManager.getString("export.filter.search", "Búsqueda: ")).append(txtBuscar.getText().trim()).append("\n");
        }

        if (!txtAutor.getText().trim().isEmpty()) {
            sb.append(LanguageManager.getString("export.filter.author", "Autor: ")).append(txtAutor.getText().trim()).append("\n");
        }

        if (!chkTodasEstanterias.isSelected()) {
            List<String> estanteriasSeleccionadas = seleccionEstanterias.entrySet().stream()
                    .filter(e -> e.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (!estanteriasSeleccionadas.isEmpty()) {
                sb.append(LanguageManager.getString("export.filter.shelves", "Estanterías: ")).append(String.join(", ", estanteriasSeleccionadas)).append("\n");
            }
        }

        if (!chkTodosGeneros.isSelected()) {
            List<String> generosSeleccionados = seleccionGeneros.entrySet().stream()
                    .filter(e -> e.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (!generosSeleccionados.isEmpty()) {
                sb.append(LanguageManager.getString("export.filter.genres", "Géneros: ")).append(String.join(", ", generosSeleccionados)).append("\n");
            }
        }

        if (!txtAnioDesde.getText().trim().isEmpty() || !txtAnioHasta.getText().trim().isEmpty()) {
            sb.append(LanguageManager.getString("export.filter.years", "Años: "));
            if (!txtAnioDesde.getText().trim().isEmpty()) {
                sb.append(LanguageManager.getString("export.filter.years.from", "desde ")).append(txtAnioDesde.getText().trim());
            }
            if (!txtAnioHasta.getText().trim().isEmpty()) {
                if (!txtAnioDesde.getText().trim().isEmpty()) {
                    sb.append(" ");
                }
                sb.append(LanguageManager.getString("export.filter.years.to", "hasta ")).append(txtAnioHasta.getText().trim());
            }
            sb.append("\n");
        }

        String estadoSeleccionado = cmbEstadoLectura.getValue();
        if (estadoSeleccionado != null && !estadoSeleccionado.equals(LanguageManager.getString("export.status.all", "Todos"))) {
            sb.append(LanguageManager.getString("export.filter.status", "Estado de lectura: ")).append(estadoSeleccionado).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Agrupa los libros por estantería.
     *
     * @param libros Lista de libros a agrupar.
     * @return Mapa con libros agrupados por estantería.
     */
    private Map<String, List<Libro>> agruparPorEstanteria(List<Libro> libros) {
        Map<String, List<Libro>> mapa = new LinkedHashMap<>();

        for (Libro libro : libros) {
            List<String> estanterias = libro.getEstanterias();

            if (estanterias == null || estanterias.isEmpty()) {
                // Libros sin estantería
                mapa.computeIfAbsent("", k -> new ArrayList<>()).add(libro);
            } else {
                // Agregar a cada estantería del libro
                for (String estanteria : estanterias) {
                    mapa.computeIfAbsent(estanteria, k -> new ArrayList<>()).add(libro);
                }
            }
        }

        // Ordenar por nombre de estantería
        return mapa.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
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
