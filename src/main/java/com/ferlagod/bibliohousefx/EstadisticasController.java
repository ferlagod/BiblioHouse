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
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para la ventana de estadísticas. Muestra métricas sobre la
 * biblioteca, como total de libros, leídos, autor preferido, etc.
 *
 * @author Fernando Lago
 * @version 1.0
 */
public class EstadisticasController {

    @FXML
    private Label lblTotal;
    @FXML
    private Label lblLeidos;
    @FXML
    private Label lblAutorTop;
    @FXML
    private Label lblGeneroTop;
    @FXML
    private Label lblMedia;
    @FXML
    private Label lblEstrellasMedia;
    @FXML
    private PieChart graficoGeneros;
    @FXML
    private Label lblLeidosAnio;
    @FXML
    private Label lblTasaPromedio;
    @FXML
    private BarChart<String, Number> graficoAutores;
    @FXML
    private StackedBarChart<String, Number> graficoEstadosPorGenero;

    /**
     * Calcula y muestra las estadísticas basándose en la lista de libros
     * proporcionada.
     *
     * @param libros Lista de libros de la biblioteca.
     */
    public void calcularEstadisticas(List<Libro> libros) {
        if (libros == null || libros.isEmpty()) {
            limpiarDatos();
            return;
        }

        // 1. Total y Leídos
        int total = libros.size();
        // Usamos el campo booleano 'leido' para el total, ya que es la métrica
        // principal
        long leidos = libros.stream().filter(Libro::isLeido).count();

        lblTotal.setText(String.valueOf(total));
        String porcentajeLeido = (total > 0) ? String.valueOf((leidos * 100 / total)) : "0";
        lblLeidos.setText(String.valueOf(leidos) + " (" + porcentajeLeido + "%)");

        // 2. Autor más frecuente
        Map<String, Long> conteoAutores = libros.stream()
                .collect(Collectors.groupingBy(Libro::getAutor, Collectors.counting()));
        String autorTop = conteoAutores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        lblAutorTop.setText(autorTop);

        // 3. Género más frecuente (para el texto) y para el gráfico
        Map<String, Long> conteoGeneros = libros.stream()
                .collect(Collectors.groupingBy(Libro::getGenero, Collectors.counting()));
        String generoTop = conteoGeneros.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        lblGeneroTop.setText(generoTop);

        ObservableList<PieChart.Data> pieChartData = conteoGeneros.entrySet().stream()
                .map(entry -> new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        graficoGeneros.setData(pieChartData);

        // 4. Calificación Media
        double media = libros.stream()
                .mapToInt(Libro::getCalificacion)
                .average()
                .orElse(0.0);

        lblMedia.setText(String.format("%.1f", media));
        lblEstrellasMedia.setText(generarEstrellasMedia(media));

        // 5. Tasa de Lectura
        calcularTasaLectura(libros);

        // 6. Gráfico de Top 10 Autores
        generarGraficoAutores(libros);

        // 7. Gráfico de Estados por Género
        generarGraficoEstadosPorGenero(libros);
    }

    /**
     * Calcula la tasa de lectura basada en libros leídos este año.
     *
     * @param libros Lista de libros para analizar.
     */
    private void calcularTasaLectura(List<Libro> libros) {
        // Usamos el campo 'estadoLectura' y 'fechaFinalizacion' para ser más precisos
        int leidosEsteAnio = 0;

        // Meses transcurridos del año (1 en enero, 12 en diciembre)
        int mesActual = LocalDate.now().getMonthValue();
        int anioActual = LocalDate.now().getYear();

        // Contamos solo los que tienen fecha de finalización y el estado "Leído"
        long totalLibrosTerminados = libros.stream()
                .filter(libro -> "Leído".equalsIgnoreCase(libro.getEstadoLectura())
                        && libro.getFechaFinalizacion() != null)
                .count();

        // Contamos los terminados ESTE AÑO
        leidosEsteAnio = (int) libros.stream()
                .filter(libro -> "Leído".equalsIgnoreCase(libro.getEstadoLectura())
                        && libro.getFechaFinalizacion() != null)
                .filter(libro -> libro.getFechaFinalizacion().getYear() == anioActual)
                .count();

        // 1. CALCULAR TASA PROMEDIO: Total terminados / Meses transcurridos
        double tasaCalculada = (totalLibrosTerminados > 0)
                ? (double) totalLibrosTerminados / (double) mesActual
                : 0.0;

        // 2. ACTUALIZAR UI
        if (lblLeidosAnio != null) {
            lblLeidosAnio.setText(String.valueOf(leidosEsteAnio));
        }
        if (lblTasaPromedio != null) {
            lblTasaPromedio.setText(String.format("%.2f", tasaCalculada));
        }
    }

    /**
     * Genera el gráfico de barras con los 10 autores más frecuentes.
     *
     * @param libros Lista de libros para analizar.
     */
    private void generarGraficoAutores(List<Libro> libros) {
        if (graficoAutores == null)
            return;

        graficoAutores.getData().clear();

        Map<String, Long> conteoAutores = libros.stream()
                .collect(Collectors.groupingBy(Libro::getAutor, Collectors.counting()));

        // Ordenar por valor (frecuencia) de mayor a menor y tomar los 10 primeros
        List<Map.Entry<String, Long>> topAutores = conteoAutores.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Libros por Autor");

        for (Map.Entry<String, Long> entry : topAutores) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        graficoAutores.getData().add(series);
    }

    /**
     * Genera el gráfico de barras apiladas mostrando el estado de lectura por
     * género.
     *
     * @param libros Lista de libros para analizar.
     */
    private void generarGraficoEstadosPorGenero(List<Libro> libros) {
        if (graficoEstadosPorGenero == null)
            return;

        graficoEstadosPorGenero.getData().clear();

        // Agrupar por género y luego por estado de lectura
        Map<String, Map<String, Long>> datos = libros.stream()
                .collect(Collectors.groupingBy(Libro::getGenero,
                        Collectors.groupingBy(l -> {
                            String estado = l.getEstadoLectura();
                            return (estado == null || estado.isEmpty()) ? "Sin estado" : estado;
                        }, Collectors.counting())));

        // Identificar todos los estados únicos presentes
        List<String> todosLosEstados = libros.stream()
                .map(l -> {
                    String estado = l.getEstadoLectura();
                    return (estado == null || estado.isEmpty()) ? "Sin estado" : estado;
                })
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Crear una serie por cada estado
        for (String estado : todosLosEstados) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(estado);

            for (Map.Entry<String, Map<String, Long>> entry : datos.entrySet()) {
                String genero = entry.getKey();
                Long count = entry.getValue().getOrDefault(estado, 0L);
                if (count > 0) {
                    series.getData().add(new XYChart.Data<>(genero, count));
                }
            }

            if (!series.getData().isEmpty()) {
                graficoEstadosPorGenero.getData().add(series);
            }
        }
    }

    /**
     * Limpia los datos de la interfaz (pone contadores a cero).
     */
    private void limpiarDatos() {
        lblTotal.setText("0");
        lblLeidos.setText("0 (0%)");
        lblAutorTop.setText("N/A");
        lblGeneroTop.setText("N/A");
        lblMedia.setText("0.0");
        lblEstrellasMedia.setText("☆☆☆☆☆");
        lblLeidosAnio.setText("0");
        lblTasaPromedio.setText("0.00");
        graficoGeneros.setData(FXCollections.observableArrayList());
        if (graficoAutores != null)
            graficoAutores.getData().clear();
        if (graficoEstadosPorGenero != null)
            graficoEstadosPorGenero.getData().clear();
    }

    /**
     * Genera una representación visual de la media de estrellas.
     *
     * @param media Valor medio (0.0 a 5.0).
     * @return String con estrellas llenas y vacías.
     */
    private String generarEstrellasMedia(double media) {
        StringBuilder sb = new StringBuilder();
        int estrellasLlenas = (int) Math.round(media);
        for (int i = 0; i < 5; i++) {
            if (i < estrellasLlenas) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        return sb.toString();
    }

    /**
     * Cierra la ventana de estadísticas.
     *
     * @param event El evento del botón Cerrar.
     */
    @FXML
    private void cerrar(ActionEvent event) {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }
}
