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
package com.bibliohouse.logic;

import com.ferlagod.bibliohousefx.App;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para LanguageManager y su sincronización con App y AppEventBus.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
class TestLanguageManager {

    private Locale localeOriginal;

    @BeforeEach
    void setUp() {
        localeOriginal = LanguageManager.getLocale();
    }

    @AfterEach
    void tearDown() {
        // Restaurar estado original
        LanguageManager.setLocale(localeOriginal);
    }

    @Test
    @DisplayName("Cambio de idioma a inglés y sincronización con App")
    void testCambioAIngles() {
        LanguageManager.setLocale("en");

        assertEquals("en", LanguageManager.getLocale().getLanguage());
        assertEquals("en", App.getCurrentLocale().getLanguage());
        assertEquals("File", LanguageManager.getString("menu.file"));
        assertEquals("File", App.getBundle().getString("menu.file"));
    }

    @Test
    @DisplayName("Cambio de idioma a galego y verificación de claves")
    void testCambioAGalego() {
        LanguageManager.setLocale("gl");

        assertEquals("gl", LanguageManager.getLocale().getLanguage());
        assertEquals("gl", App.getCurrentLocale().getLanguage());
        assertEquals("Ficheiro", LanguageManager.getString("menu.file"));
    }

    @Test
    @DisplayName("Cambio de idioma a español y verificación de claves")
    void testCambioAEspanol() {
        LanguageManager.setLocale("es");

        assertEquals("es", LanguageManager.getLocale().getLanguage());
        assertEquals("Archivo", LanguageManager.getString("menu.file"));
    }

    @Test
    @DisplayName("Notificación reactiva de IdiomaCambiadoEvent a través de AppEventBus")
    void testNotificacionEventBus() throws InterruptedException {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        AtomicBoolean eventoRecibido = new AtomicBoolean(false);
        AtomicReference<Locale> localeEnEvento = new AtomicReference<>();

        java.util.function.Consumer<AppEventBus.IdiomaCambiadoEvent> listener = event -> {
            eventoRecibido.set(true);
            localeEnEvento.set(event.getNuevoLocale());
            latch.countDown();
        };

        AppEventBus.getInstance().subscribe(AppEventBus.IdiomaCambiadoEvent.class, listener);

        try {
            LanguageManager.setLocale("en");
            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(eventoRecibido.get(), "El evento IdiomaCambiadoEvent debe haberse emitido");
            assertNotNull(localeEnEvento.get(), "El evento debe contener el nuevo Locale");
            assertEquals("en", localeEnEvento.get().getLanguage());
        } finally {
            AppEventBus.getInstance().unsubscribe(AppEventBus.IdiomaCambiadoEvent.class, listener);
        }
    }

    @Test
    @DisplayName("Retorno de clave por defecto cuando la propiedad no existe")
    void testValorPorDefectoParaClaveInexistente() {
        String resultado = LanguageManager.getString("clave.completamente.inexistente.123", "ValorPorDefecto");
        assertEquals("ValorPorDefecto", resultado);
    }
}
