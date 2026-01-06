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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

// OpenCV Imports
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

/**
 * Controlador para la ventana de escaneo de códigos de barras.
 * Utiliza OpenCV (vía OpenPnP) y ZXing para leer ISBNs.
 * VERSIÓN ACTUALIZADA PARA COMPATIBILIDAD CON APPLE SILICON (M1/M2/M3).
 *
 * @author Fernando Lago
 * @version 2.0 (OpenCV)
 */
public class EscanerController {

    @FXML
    private ImageView imgWebcam;

    private VideoCapture capture;
    private AtomicBoolean stopCamera = new AtomicBoolean(false);
    private EscanerListener listener;

    /**
     * Interfaz para comunicar el resultado del escaneo al controlador principal.
     */
    public interface EscanerListener {
        void onIsbnScanned(String isbn);
    }

    public void setListener(EscanerListener listener) {
        this.listener = listener;
    }

    /**
     * Inicializa la cámara y comienza el bucle de captura.
     */
    public void init() {
        startWebcam();
    }

    private void startWebcam() {
        Task<Void> webCamTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 1. Cargar librerías nativas de OpenCV
                try {
                    // Carga la librería nativa adecuada para el SO (soporta M1)
                    nu.pattern.OpenCV.loadLocally();
                    // O si usamos org.openpnp.opencv.OpenCV:
                    // En algunas versiones con shading funciona directo, pero por si acaso:
                    // System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
                } catch (Throwable e) {
                    // Fallback o error logging
                    System.err.println("Error cargando OpenCV: " + e.getMessage());
                }

                // 2. Abrir cámara (índice 0 suele ser la default)
                capture = new VideoCapture(0);

                if (capture.isOpened()) {
                    startScanning();
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("No se detectó cámara");
                        alert.setContentText("No se pudo iniciar la captura de vídeo. Verifica permisos y conexión.");
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
                                            Platform.runLater(() -> {
                                                if (listener != null) {
                                                    listener.onIsbnScanned(text);
                                                }
                                                java.awt.Toolkit.getDefaultToolkit().beep();
                                                cerrarVentana();
                                            });
                                            stopCamera.set(true);
                                        }
                                    }
                                } catch (NotFoundException e) {
                                    // No code found
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
     * Convierte una Mat de OpenCV a BufferedImage.
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

    private boolean esPosibleISBN(String text) {
        if (text == null)
            return false;
        String clean = text.replaceAll("-", "").trim();
        return clean.length() == 10 || clean.length() == 13;
    }

    @FXML
    private void cancelar(ActionEvent event) {
        cerrarVentana();
    }

    private void cerrarVentana() {
        stopCamera.set(true);
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        Stage stage = (Stage) imgWebcam.getScene().getWindow();
        stage.close();
    }

    public void shutdown() {
        stopCamera.set(true);
        if (capture != null) {
            capture.release();
        }
    }
}
