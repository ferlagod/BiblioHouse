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

import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del panel lateral de ficha de libro en la ventana principal.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.3
 */
class TestPanelDetalle {

    @TempDir
    File tempDir;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    private static void poner(Object destino, String campo, Object valor) throws Exception {
        Field f = PrimaryController.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(destino, valor);
    }

    private static void enHiloFx(Runnable r) throws Exception {
        CountDownLatch fin = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                fin.countDown();
            }
        });
        assertTrue(fin.await(10, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }

    /** Texto del título en la ficha abierta (el contenido del ScrollPane no es buscable sin escena). */
    private static String tituloMostrado(PrimaryController pc) {
        try {
            Field fc = PrimaryController.class.getDeclaredField("panelDetalleController");
            fc.setAccessible(true);
            Object detalle = fc.get(pc);
            Field ft = DetalleLibroController.class.getDeclaredField("lblTitulo");
            ft.setAccessible(true);
            return ((Label) ft.get(detalle)).getText();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Libro libro(String id, String titulo) {
        Libro l = new Libro();
        l.setId(id);
        l.setTitulo(titulo);
        l.setAutor("Autor de " + titulo);
        return l;
    }

    @Test
    void elPanelSeAbreCambiaDeLibroYSeCierra() throws Exception {
        BorderPane contenedor = new BorderPane();
        PrimaryController pc = new PrimaryController();
        poner(pc, "mainContainer", contenedor);
        poner(pc, "jsonManager", new JsonManager(tempDir.getAbsolutePath()));
        poner(pc, "resources", ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages"));
        Libro dune = libro("1", "Dune");
        Libro hobbit = libro("2", "El hobbit");

        enHiloFx(() -> {
            pc.mostrarDetalleLibro(dune);
            assertTrue(pc.isPanelDetalleAbierto());
            Node panel = contenedor.getRight();
            assertNotNull(panel, "La ficha se muestra a la derecha del catálogo, no en otra ventana");
            assertEquals("Dune", tituloMostrado(pc));

            // Seleccionar otro libro con el panel abierto lo actualiza
            pc.seleccionarLibro(hobbit);
            assertEquals("El hobbit", tituloMostrado(pc));
            assertNotSame(panel, contenedor.getRight());

            // El botón ✕ cierra el panel
            ((Button) contenedor.getRight().lookup("#btnCerrarPanel")).fire();
            assertFalse(pc.isPanelDetalleAbierto());
        });

        // Tras la animación de salida el panel desaparece del layout
        Thread.sleep(500);
        enHiloFx(() -> assertNull(contenedor.getRight()));
    }
}
