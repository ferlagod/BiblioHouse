package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.Libro;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.File;
import java.net.URL;

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

    public void setLibro(Libro libro) {
        this.libroActual = libro;
        if (libro != null) {
            lblTituloLector.setText(libro.getTitulo());
            cargarLector();
        }
    }

    public void setOnSyncRequested(Runnable onSyncRequested) {
        this.onSyncRequested = onSyncRequested;
    }

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

    @FXML
    private void paginaSiguiente(ActionEvent event) {
        webViewLector.getEngine().executeScript("nextPage()");
    }

    @FXML
    private void paginaAnterior(ActionEvent event) {
        webViewLector.getEngine().executeScript("prevPage()");
    }
    
    @FXML
    private void aumentarLetra(ActionEvent event) {
        fontSizePercent += 10;
        webViewLector.getEngine().executeScript("setFontSize('" + fontSizePercent + "%')");
    }

    @FXML
    private void disminuirLetra(ActionEvent event) {
        if (fontSizePercent > 50) {
            fontSizePercent -= 10;
            webViewLector.getEngine().executeScript("setFontSize('" + fontSizePercent + "%')");
        }
    }

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

    public void logError(String message) {
        System.err.println("[Visor EPUB] " + message);
    }
}
