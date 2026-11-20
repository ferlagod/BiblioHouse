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
import com.bibliohouse.logic.LibroService;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.controlsfx.control.NotificationPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la eliminación de libros con opción de deshacer.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.3
 */
class TestDeshacerEliminar {

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

    private static void enHiloFx(ThrowingRunnable r) throws Exception {
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

    interface ThrowingRunnable {

        void run() throws Exception;
    }

    @Test
    void eliminarYDeshacerRestauraElLibroEnSuPosicion() throws Exception {
        Libro a = new Libro();
        a.setId("a");
        a.setTitulo("A");
        Libro b = new Libro();
        b.setId("b");
        b.setTitulo("B");
        Libro c = new Libro();
        c.setId("c");
        c.setTitulo("C");
        ObservableList<Libro> libros = FXCollections.observableArrayList(List.of(a, b, c));
        JsonManager json = new JsonManager(tempDir.getAbsolutePath());

        enHiloFx(() -> {
            PrimaryController pc = new PrimaryController();
            NotificationPane pane = new NotificationPane();
            poner(pc, "notificationPane", pane);
            poner(pc, "listaLibrosCompleta", libros);
            poner(pc, "jsonManager", json);
            poner(pc, "libroService", new LibroService(json, libros));

            pc.eliminarLibro(b);
            assertEquals(List.of(a, c), libros, "Se elimina sin diálogo de confirmación");
            assertEquals(1, pane.getActions().size(), "La notificación ofrece «Deshacer»");

            pane.getActions().get(0).handle(new ActionEvent());
            assertEquals(List.of(a, b, c), libros, "Deshacer devuelve el libro a su posición");
            assertTrue(pane.getActions().isEmpty());

            // La acción de deshacer solo se ejecuta una vez aunque se pulse dos
            java.util.concurrent.atomic.AtomicInteger veces = new java.util.concurrent.atomic.AtomicInteger();
            pc.ofrecerDeshacer("x", veces::incrementAndGet);
            org.controlsfx.control.action.Action accion = pane.getActions().get(0);
            accion.handle(new ActionEvent());
            accion.handle(new ActionEvent());
            assertEquals(1, veces.get());
        });
    }
}
