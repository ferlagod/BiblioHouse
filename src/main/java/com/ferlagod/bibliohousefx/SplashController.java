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
 * SIN NINGUNA GARANTÍA; sin incluso la garantía implícita de
 * COMERCIABILIDAD o APTITUD PARA UN PROPÓSITO PARTICULAR. Vea la
 * Licencia Pública General de GNU para más detalles.
 *
 * Usted debería haber recibido una copia de la Licencia Pública General de GNU
 * junto con este programa. Si no es así, vea <https://www.gnu.org/licenses/>.
 */
package com.ferlagod.bibliohousefx;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador para la pantalla de splash de la aplicación. Gestiona las
 * animaciones de entrada y la simulación de carga inicial.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class SplashController implements Initializable {

    @FXML
    private VBox logoContainer;
    @FXML
    private VBox textContainer;
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar progressBar;

    /**
     * Inicializa el controlador y prepara las animaciones de entrada.
     *
     * @param url Ubicación del FXML (no utilizado directamente).
     * @param rb Recursos de internacionalización (no utilizado directamente).
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuración inicial: logo oculto y desplazado
        logoContainer.setOpacity(0.0);
        logoContainer.setTranslateY(40.0);

        // Animación de rebote y aparición del logo
        TranslateTransition translateLogo = new TranslateTransition(Duration.millis(1200), logoContainer);
        translateLogo.setFromY(40.0);
        translateLogo.setToY(0.0);
        translateLogo.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        FadeTransition fadeLogo = new FadeTransition(Duration.millis(800), logoContainer);
        fadeLogo.setFromValue(0.0);
        fadeLogo.setToValue(1.0);

        ParallelTransition logoAnim = new ParallelTransition(translateLogo, fadeLogo);

        // Animación de aparición de los textos
        FadeTransition fadeText = new FadeTransition(Duration.millis(800), textContainer);
        fadeText.setFromValue(0.0);
        fadeText.setToValue(1.0);
        fadeText.setDelay(Duration.millis(400));

        SequentialTransition masterAnim = new SequentialTransition(logoAnim, fadeText);
        masterAnim.setOnFinished(e -> startLoadingTask());

        // Iniciar animaciones con retraso inicial (PauseTransition es FX-safe y no deja hilos huérfanos)
        javafx.animation.PauseTransition startDelay = new javafx.animation.PauseTransition(Duration.millis(200));
        startDelay.setOnFinished(ev -> masterAnim.play());
        startDelay.play();
    }

    /**
     * Inicia la tarea de carga simulada con actualización de progreso y
     * mensajes.
     */
    private void startLoadingTask() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                updateMessage("Comprobando integridad de base de datos...");
                // Trabajo real: verificar que la carpeta de datos existe
                java.io.File dataDir = new java.io.File(
                        System.getProperty("user.home") + java.io.File.separator + "BiblioHouse");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                updateProgress(40, 100);
                updateMessage("Preparando recursos...");
                // Trabajo real: pre-calentar el ResourceBundle para que el login cargue más rápido
                try {
                    java.util.ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages",
                            App.getCurrentLocale());
                } catch (Exception ignored) {
                    // No crítico si falla
                }

                updateProgress(80, 100);
                updateMessage("Cargando interfaz principal...");
                // Trabajo real: pre-cargar la imagen por defecto en la caché de ImageLoader
                try {
                    com.bibliohouse.utils.ImageLoader.preloadDefaultCover();
                } catch (Exception ignored) {
                    // No crítico si falla
                }

                updateProgress(100, 100);
                updateMessage("Listo");
                // Pequeña pausa para que el usuario vea "Listo" antes de la transición
                Thread.sleep(150);
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            // Animación de salida antes de cambiar a la ventana principal
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), logoContainer.getParent());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> App.getApp().loadWelcome());
            fadeOut.play();
        });

        Thread splashThread = new Thread(task);
        splashThread.setDaemon(true);
        splashThread.start();
    }
}
