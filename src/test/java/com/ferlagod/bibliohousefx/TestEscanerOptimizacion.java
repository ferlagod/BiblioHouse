/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.ferlagod.bibliohousefx;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la optimización del escáner:
 * - Reutilización de búferes de bytes y BufferedImage ante fotogramas consecutivos.
 * - Reasignación controlada únicamente cuando cambian las dimensiones.
 * - Liberación de matrices nativas de OpenCV.
 */
public class TestEscanerOptimizacion {

    @BeforeAll
    public static void setupOpenCV() {
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (Throwable t) {
            System.err.println("OpenCV native load: " + t.getMessage());
        }
    }

    @Test
    @DisplayName("Debe reutilizar la misma instancia de BufferedImage y byte[] en fotogramas sucesivos con igual resolución")
    public void testReutilizacionBuffersMismasDimensiones() {
        EscanerController controller = new EscanerController();

        Mat mat1 = new Mat(480, 640, CvType.CV_8UC3, new Scalar(255, 0, 0));
        BufferedImage img1 = controller.matToBufferedImage(mat1);

        assertNotNull(img1, "La primera imagen convertida no debe ser nula");
        assertEquals(640, img1.getWidth());
        assertEquals(480, img1.getHeight());

        byte[] buf1 = controller.getReusableBuffer();
        assertNotNull(buf1, "El búfer de bytes reutilizable debe estar inicializado");
        assertEquals(640 * 480 * 3, buf1.length);

        // Segundo frame con idéntica resolución (simula el siguiente frame a 30 FPS)
        Mat mat2 = new Mat(480, 640, CvType.CV_8UC3, new Scalar(0, 255, 0));
        BufferedImage img2 = controller.matToBufferedImage(mat2);

        // Verificación de reutilización de memoria (cero asignaciones nuevas)
        assertSame(img1, img2, "Debe reutilizarse la misma instancia de BufferedImage");
        assertSame(buf1, controller.getReusableBuffer(), "Debe reutilizarse el mismo array de bytes");

        // Liberar matrices nativas
        mat1.release();
        mat2.release();
    }

    @Test
    @DisplayName("Debe reasignar buffers cuando cambian las dimensiones del frame")
    public void testCambioDimensionesReasignaBuffers() {
        EscanerController controller = new EscanerController();

        Mat matGrande = new Mat(720, 1280, CvType.CV_8UC3);
        BufferedImage imgGrande = controller.matToBufferedImage(matGrande);
        byte[] bufGrande = controller.getReusableBuffer();

        assertEquals(1280, imgGrande.getWidth());
        assertEquals(720, imgGrande.getHeight());
        assertEquals(1280 * 720 * 3, bufGrande.length);

        // Cambiar resolución (ej. cambio a 640x480)
        Mat matPequena = new Mat(480, 640, CvType.CV_8UC3);
        BufferedImage imgPequena = controller.matToBufferedImage(matPequena);
        byte[] bufPequeno = controller.getReusableBuffer();

        assertNotSame(imgGrande, imgPequena, "Debe asignar una nueva imagen para la nueva resolución");
        assertNotSame(bufGrande, bufPequeno, "Debe asignar un nuevo array acorde al nuevo tamaño");
        assertEquals(640, imgPequena.getWidth());
        assertEquals(480, imgPequena.getHeight());
        assertEquals(640 * 480 * 3, bufPequeno.length);

        matGrande.release();
        matPequena.release();
    }

    @Test
    @DisplayName("Shutdown debe liberar las referencias de los buffers")
    public void testShutdownLimpiaBuffers() {
        EscanerController controller = new EscanerController();

        Mat mat = new Mat(100, 100, CvType.CV_8UC3);
        controller.matToBufferedImage(mat);
        assertNotNull(controller.getReusableBuffer());
        assertNotNull(controller.getReusableImage());

        controller.shutdown();
        assertNull(controller.getReusableBuffer(), "Shutdown debe limpiar reusableBuffer");
        assertNull(controller.getReusableImage(), "Shutdown debe limpiar reusableImage");

        mat.release();
    }

    @Test
    @DisplayName("Mat nula o vacía debe retornar null sin lanzar excepción")
    public void testMatInvalida() {
        EscanerController controller = new EscanerController();

        assertNull(controller.matToBufferedImage(null));

        Mat empty = new Mat();
        assertNull(controller.matToBufferedImage(empty));
        empty.release();
    }
}
