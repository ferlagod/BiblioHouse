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

import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba que las vistas principales se cargan desde FXML (incluidos los
 * iconos Ikonli declarados en ellas y las claves de traducción).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.3
 */
class TestCargaVistas {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    @Test
    void seQuitaElEmojiInicialDeLosTextos() {
        assertEquals("Leyendo", com.bibliohouse.utils.Iconos.sinSimboloInicial("📖 Leyendo"));
        assertEquals("Leído", com.bibliohouse.utils.Iconos.sinSimboloInicial("✓ Leído"));
        assertEquals("E-book", com.bibliohouse.utils.Iconos.sinSimboloInicial("📱 E-book"));
        assertEquals("Todos", com.bibliohouse.utils.Iconos.sinSimboloInicial("Todos"));
        assertEquals("3 libros", com.bibliohouse.utils.Iconos.sinSimboloInicial("3 libros"), "Los números no son símbolos");
        assertEquals("⭐", com.bibliohouse.utils.Iconos.sinSimboloInicial("⭐"), "Un texto que es solo un símbolo no se vacía");
    }

    @Test
    void ventanaPrincipal() throws Exception {
        cargar("primary.fxml");
    }

    @Test
    void barraLateral() throws Exception {
        cargar("sidebar.fxml");
    }

    @Test
    void catalogo() throws Exception {
        cargar("tab_mis_libros.fxml");
    }

    @Test
    void panelDeFicha() throws Exception {
        cargar("panel_detalle.fxml");
    }

    private static void cargar(String fxml) throws Exception {
        CountDownLatch fin = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
                loader.setResources(ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages"));
                assertNotNull(loader.load());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                fin.countDown();
            }
        });
        assertTrue(fin.await(20, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError("No se pudo cargar " + fxml, error.get());
        }
    }
}
