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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.File;
import java.net.URL;

/**
 * Controlador del lector digital de libros electrónicos. Permite abrir y
 * visualizar archivos EPUB directamente dentro de la aplicación usando un
 * {@link WebView} con un visor HTML/JS integrado. Soporta navegación entre
 * páginas, ajuste del tamaño de fuente y seguimiento del progreso de lectura,
 * que se sincroniza de vuelta al modelo del libro.
 *
 * <p>La comunicación entre JavaFX y el visor JavaScript se realiza mediante
 * un puente {@code javaBridge} inyectado en el contexto del WebView, lo que
 * permite que el JS llame a métodos como {@link #updateProgress(int)} y
 * {@link #logError(String)} directamente.</p>
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class LectorDigitalController {

    @FXML
    private Label lblTituloLector;
    @FXML
    private WebView webViewLector;
    @FXML
    private Label lblProgreso;

    private Libro libroActual;
    private Runnable onSyncRequested;
    private int currentPercentage = 0;
    private int fontSizePercent = 100;

    /**
     * Establece el libro que se va a leer en el visor. Actualiza el título
     * mostrado en la cabecera y carga el contenido EPUB en el WebView.
     *
     * @param libro El libro a visualizar. Si es {@code null}, no se realiza
     *              ninguna acción.
     */
    public void setLibro(Libro libro) {
        this.libroActual = libro;
        if (libro != null) {
            lblTituloLector.setText(libro.getTitulo());
            cargarLector();
        }
    }

    /**
     * Registra un callback que se ejecutará cada vez que el progreso de
     * lectura cambie y deba sincronizarse (por ejemplo, para guardar en JSON).
     *
     * @param onSyncRequested Acción a ejecutar cuando se solicite sincronización.
     */
    public void setOnSyncRequested(Runnable onSyncRequested) {
        this.onSyncRequested = onSyncRequested;
    }

    /**
     * Carga el visor EPUB en el WebView. Lee el archivo digital del libro
     * actual, lo convierte a Base64 y lo envía al visor JavaScript mediante
     * el puente {@code javaBridge}. Calcula la posición inicial de lectura
     * a partir de la página guardada en el modelo del libro.
     */
    private void cargarLector() {
        if (libroActual.getRutaArchivoDigital() == null) return;

        File archivo = new File(libroActual.getRutaArchivoDigital());
        if (!archivo.exists()) return;

        URL urlHTML = getClass().getResource("/com/ferlagod/bibliohousefx/reader/epub_reader.html");
        if (urlHTML == null) {
            System.err.println("No se encontró epub_reader.html");
            return;
        }

        webViewLector.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webViewLector.getEngine().executeScript("window");
                window.setMember("javaBridge", this);
                
                try {
                    byte[] fileContent = java.nio.file.Files.readAllBytes(archivo.toPath());
                    String base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
                    
                    double startPercent = 0;
                    if (libroActual.getPaginasTotales() > 0) {
                        startPercent = ((double) libroActual.getPaginaActual() / libroActual.getPaginasTotales()) * 100;
                    }
                    
                    window.setMember("epubBase64", base64);
                    webViewLector.getEngine().executeScript("openEpubBase64(window.epubBase64, " + startPercent + ")");
                } catch (Exception e) {
                    System.err.println("Error procesando EPUB a Base64: " + e.getMessage());
                }
            }
        });

        webViewLector.getEngine().load(urlHTML.toExternalForm());
    }

    /**
     * Avanza a la página siguiente del EPUB invocando la función JavaScript
     * {@code nextPage()} en el visor.
     *
     * @param event El evento del botón pulsado.
     */
    @FXML
    private void paginaSiguiente(ActionEvent event) {
        webViewLector.getEngine().executeScript("nextPage()");
    }

    /**
     * Retrocede a la página anterior del EPUB invocando la función JavaScript
     * {@code prevPage()} en el visor.
     *
     * @param event El evento del botón pulsado.
     */
    @FXML
    private void paginaAnterior(ActionEvent event) {
        webViewLector.getEngine().executeScript("prevPage()");
    }
    
    /**
     * Aumenta el tamaño de la fuente del visor en un 10%. El cambio se
     * aplica invocando la función JavaScript {@code setFontSize()} con el
     * nuevo porcentaje.
     *
     * @param event El evento del botón pulsado.
     */
    @FXML
    private void aumentarLetra(ActionEvent event) {
        fontSizePercent += 10;
        webViewLector.getEngine().executeScript("setFontSize('" + fontSizePercent + "%')");
    }

    /**
     * Disminuye el tamaño de la fuente del visor en un 10%, con un mínimo
     * del 50% para evitar que el texto sea ilegible.
     *
     * @param event El evento del botón pulsado.
     */
    @FXML
    private void disminuirLetra(ActionEvent event) {
        if (fontSizePercent > 50) {
            fontSizePercent -= 10;
            webViewLector.getEngine().executeScript("setFontSize('" + fontSizePercent + "%')");
        }
    }

    /**
     * Callback invocado desde JavaScript a través del puente {@code javaBridge}
     * para actualizar el progreso de lectura. Actualiza la etiqueta de progreso
     * en la UI y sincroniza la página actual en el modelo del libro. Si se ha
     * registrado un callback de sincronización, lo ejecuta.
     *
     * @param percentage El porcentaje de progreso (0-100).
     */
    public void updateProgress(int percentage) {
        this.currentPercentage = percentage;
        
        // Actualizar UI
        javafx.application.Platform.runLater(() -> {
            lblProgreso.setText("Progreso: " + percentage + "%");
        });

        // Actualizar en el modelo
        if (libroActual != null) {
            if (libroActual.getPaginasTotales() <= 0) {
                libroActual.setPaginasTotales(100);
            }
            int pagina = (int) ((percentage / 100.0) * libroActual.getPaginasTotales());
            libroActual.setPaginaActual(pagina);
            if (onSyncRequested != null) {
                onSyncRequested.run();
            }
        }
    }

    /**
     * Callback invocado desde JavaScript para registrar errores del visor
     * EPUB en la consola de errores estándar.
     *
     * @param message El mensaje de error a registrar.
     */
    public void logError(String message) {
        System.err.println("[Visor EPUB] " + message);
    }
}
