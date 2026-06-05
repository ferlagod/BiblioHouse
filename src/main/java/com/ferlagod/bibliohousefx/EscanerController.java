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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Controlador para la ventana de escaneo de códigos de barras. Utiliza OpenCV
 * (vía OpenPnP) y ZXing para leer ISBNs. VERSIÓN ACTUALIZADA PARA
 * COMPATIBILIDAD CON APPLE SILICON (M1/M2/M3).
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class EscanerController {

    @FXML
    private ImageView imgWebcam;
    @FXML
    private ListView<String> listaCodigos;
    private VideoCapture capture;
    private AtomicBoolean stopCamera = new AtomicBoolean(false);
    private EscanerListener listener;
    private ObservableList<String> codigosDetectados;

    /**
     * Interfaz para comunicar el resultado del escaneo al controlador
     * principal.
     */
    public interface EscanerListener {

        void onIsbnsScanned(List<String> isbns);
    }

    /**
     * Establece el listener que recibirá los eventos de finalización del
     * escaneo de libros. Este listener permite al controlador principal ser
     * notificado cuando el proceso de escaneo finaliza, ya sea con éxito o con
     * error.
     *
     * @param listener Objeto que implementa {@code EscanerListener} para
     * recibir los eventos del escaneo.
     */
    public void setListener(EscanerListener listener) {
        this.listener = listener;
    }

    /**
     * Inicializa la cámara y comienza el bucle de captura.
     */
    public void init() {
        codigosDetectados = FXCollections.observableArrayList();
        if (listaCodigos != null) {
            listaCodigos.setItems(codigosDetectados);
        }
        startWebcam();
    }

    /**
     * Inicia la captura de vídeo desde la webcam en un hilo separado para
     * evitar bloquear la interfaz de usuario. Este método:
     *
     * Si la cámara no se puede abrir o ocurre una excepción, muestra un diálogo
     * de error y cierra la ventana de escaneo.
     */
    private void startWebcam() {
        Task<Void> webCamTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 1. Cargar librerías nativas de OpenCV
                System.out.println("[EscanerController] Iniciando tarea de cámara...");

                try {
                    // Log de información de OpenCV para depuración
                    System.out.println("[EscanerController] OpenCV Build Info: " + Core.getBuildInformation());

                    // 2. Abrir cámara (índice 0 suele ser la cámara por defecto)
                    System.out.println("[EscanerController] Intentando abrir VideoCapture(0)...");

                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("mac")) {
                        System.out.println("[EscanerController] Detectado macOS. Usando CAP_AVFOUNDATION...");
                        capture = new VideoCapture(0, Videoio.CAP_AVFOUNDATION);
                    } else {
                        System.out.println("[EscanerController] Sistema estándar. Usando CAP_ANY (0)...");
                        capture = new VideoCapture(0);
                    }

                    if (capture.isOpened()) {
                        System.out.println("[EscanerController] Cámara abierta correctamente.");
                        startScanning();
                    } else {
                        System.err.println(
                                "[EscanerController] capture.isOpened() devolvió false. No se detectó cámara.");
                        Platform.runLater(() -> {
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("No se detectó cámara");
                            alert.setContentText(
                                    "No se pudo iniciar la captura de vídeo. Verifica permisos y conexión.");
                            alert.showAndWait();
                            cerrarVentana();
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[EscanerController] Excepción al abrir cámara: " + e.getMessage());
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error Crítico");
                        alert.setHeaderText("Fallo al iniciar cámara");
                        alert.setContentText("Ocurrió un error inesperado: " + e.getMessage());
                        alert.showAndWait();
                        cerrarVentana();
                    });
                }
                return null;
            }
        };

        Thread thread = new Thread(webCamTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Inicia el bucle de escaneo en un hilo de fondo. Captura frames, los
     * muestra en la UI y busca códigos de barras.
     */
    private void startScanning() {
        stopCamera.set(false);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Mat matrix = new Mat();

                while (!stopCamera.get()) {
                    if (capture != null && capture.isOpened()) {
                        // Leer frame
                        if (capture.read(matrix)) {

                            // Convertir Mat a BufferedImage para ZXing y JavaFX
                            BufferedImage image = matToBufferedImage(matrix);

                            if (image != null) {
                                // 1. Mostrar imagen en la UI (JavaFX thread)
                                Platform.runLater(() -> {
                                    Image fxImage = SwingFXUtils.toFXImage(image, null);
                                    imgWebcam.setImage(fxImage);
                                });

                                // 2. Intentar leer código de barras (ZXing)
                                try {
                                    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
                                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                                    Result result = new MultiFormatReader().decode(bitmap);

                                    if (result != null) {
                                        String text = result.getText();
                                        System.out.println("Código detectado: " + text);

                                        // Si parece un ISBN (10 o 13 dígitos)
                                        if (esPosibleISBN(text)) {
                                            if (!codigosDetectados.contains(text)) {
                                                Toolkit.getDefaultToolkit().beep();
                                                Platform.runLater(() -> {
                                                    codigosDetectados.add(text);
                                                    listaCodigos.scrollTo(codigosDetectados.size() - 1);
                                                });

                                                // Pausa para evitar lecturas múltiples seguidas
                                                Thread.sleep(2000);
                                            }
                                        }
                                    }
                                } catch (NotFoundException e) {

                                }
                            }
                        }
                    }
                    // Pequeña pausa
                    Thread.sleep(30);
                }
                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Convierte una Mat de OpenCV a BufferedImage para poder usarla con ZXing y
     * JavaFX.
     *
     * @param original La Mat de OpenCV a convertir.
     * @return La imagen convertida a BufferedImage.
     */
    private BufferedImage matToBufferedImage(Mat original) {
        // Asegurarse de tener 3 canales (BGR) o 1 (Grayscale)
        // Por defecto Webcam suele dar BGR.

        int width = original.width();
        int height = original.height();
        int channels = original.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        BufferedImage image;

        if (channels > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }

        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    /**
     * Valida si un texto podría ser un código ISBN.
     *
     * @param text Texto a validar. Puede ser {@code null}.
     * @return {@code true} si el texto podría ser un ISBN (longitud 10 o 13),
     * {@code false} en caso contrario.
     */
    private boolean esPosibleISBN(String text) {
        if (text == null) {
            return false;
        }
        String clean = text.replaceAll("-", "").trim();
        return clean.length() == 10 || clean.length() == 13;
    }

    /**
     * Procesa el lote de códigos escaneados y los envía al listener.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void procesarLote(ActionEvent event) {
        if (listener != null) {
            listener.onIsbnsScanned(new ArrayList<>(codigosDetectados));
        }
        cerrarVentana();
    }

    /**
     * Manejador del evento "cancelar" en la interfaz gráfica.
     *
     * @param event Evento de acción generado por el botón "Cancelar".
     */
    @FXML
    private void cancelar(ActionEvent event) {
        cerrarVentana();
    }

    /**
     * Cierra la ventana actual y libera los recursos de la cámara.
     */
    private void cerrarVentana() {
        stopCamera.set(true);
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        Stage stage = (Stage) imgWebcam.getScene().getWindow();
        stage.close();
    }

    /**
     * Libera los recursos de la cámara y detiene la captura de video.
     */
    public void shutdown() {
        stopCamera.set(true);
        if (capture != null) {
            capture.release();
        }
    }
}
