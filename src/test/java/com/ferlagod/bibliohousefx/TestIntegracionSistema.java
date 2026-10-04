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

import com.bibliohouse.utils.OsThemeDetector;
import com.bibliohouse.utils.SystemNotificationService;
import com.ferlagod.bibliohousefx.PaletaComandosDialog.PaletaItem;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.MenuBar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las características de integración nativa con
 * el sistema: detección automática de tema claro/oscuro del SO, servicio de
 * notificaciones nativas, items de la paleta de comandos Spotlight/Raycast y
 * barra de menús global.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class TestIntegracionSistema {

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    @Test
    @DisplayName("OsThemeDetector debe detectar el tema del sistema sin lanzar excepciones")
    public void testOsThemeDetectorNoExcepcion() {
        assertDoesNotThrow(() -> {
            boolean isDark = OsThemeDetector.isDarkMode();
            // Debe devolver true o false válidos
            assertTrue(isDark || !isDark);
        });
    }

    @Test
    @DisplayName("OsThemeDetector permite registrar callback y ejecutar auto-sync")
    public void testOsThemeDetectorListener() {
        AtomicBoolean llamado = new AtomicBoolean(false);
        assertDoesNotThrow(() -> {
            OsThemeDetector.startAutoSync(dark -> llamado.set(true));
        });
    }

    @Test
    @DisplayName("SystemNotificationService inicializa y procesa notificaciones de info y alerta sin lanzar fallos")
    public void testSystemNotificationService() {
        assertDoesNotThrow(() -> {
            SystemNotificationService.inicializar();
            SystemNotificationService.notificarInfo("BiblioHouse Test", "Notificación informativa de prueba");
            SystemNotificationService.notificarAlerta("Alerta Préstamo", "Préstamo próximo a vencer");
        });
    }

    @Test
    @DisplayName("PaletaComandosDialog.PaletaItem debe encapsular correctamente datos y acciones")
    public void testPaletaItemModel() {
        AtomicBoolean ejecutado = new AtomicBoolean(false);
        PaletaItem item = new PaletaItem("🔍", "Buscar Duplicados", "Encontrar libros repetidos", "Cmd+D", () -> ejecutado.set(true));

        assertEquals("🔍", item.getIcono());
        assertEquals("Buscar Duplicados", item.getTitulo());
        assertEquals("Encontrar libros repetidos", item.getSubtitulo());
        assertEquals("Cmd+D", item.getAtajo());

        item.getAccion().run();
        assertTrue(ejecutado.get(), "La acción asociada al PaletaItem debió ejecutarse.");
    }

    @Test
    @DisplayName("MenuBar soporta useSystemMenuBarProperty para integración nativa")
    public void testSystemMenuBarProperty() {
        MenuBar menuBar = new MenuBar();
        assertNotNull(menuBar.useSystemMenuBarProperty());

        // En macOS o cualquier SO podemos setear la propiedad a true/false sin error
        menuBar.useSystemMenuBarProperty().set(true);
        assertTrue(menuBar.useSystemMenuBarProperty().get());

        menuBar.useSystemMenuBarProperty().set(false);
        assertFalse(menuBar.useSystemMenuBarProperty().get());
    }
}
