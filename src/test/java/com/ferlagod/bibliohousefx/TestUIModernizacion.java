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

import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.Libro;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las nuevas funcionalidades visuales y de UX en
 * CatalogoGridController: chips de filtrado rápido, alternancia entre vista
 * cuadrícula y lista compacta, y riqueza de información en las tarjetas de
 * libros.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class TestUIModernizacion {

    @BeforeAll
    public static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    private ObservableList<Libro> crearBibliotecaVariada() {
        List<Libro> lista = new ArrayList<>();

        // 1. Libro leído con calificación
        Libro l1 = new Libro();
        l1.setTitulo("El Quijote");
        l1.setAutor("Miguel de Cervantes");
        l1.setEstadoLecturaEnum(EstadoLectura.LEIDO);
        l1.setCalificacion(5);
        l1.setPoseido(true);
        lista.add(l1);

        // 2. Libro leyendo con páginas
        Libro l2 = new Libro();
        l2.setTitulo("Dune");
        l2.setAutor("Frank Herbert");
        l2.setEstadoLecturaEnum(EstadoLectura.LEYENDO);
        l2.setPaginaActual(250);
        l2.setPaginasTotales(600);
        l2.setCalificacion(4);
        l2.setPoseido(true);
        lista.add(l2);

        // 3. Libro pendiente digital (E-book)
        Libro l3 = new Libro();
        l3.setTitulo("Fundación");
        l3.setAutor("Isaac Asimov");
        l3.setEstadoLecturaEnum(EstadoLectura.PENDIENTE);
        l3.setEsDigital(true);
        l3.setPoseido(true);
        lista.add(l3);

        // 4. Libro abandonado
        Libro l4 = new Libro();
        l4.setTitulo("Libro Difícil");
        l4.setAutor("Autor Anónimo");
        l4.setEstadoLecturaEnum(EstadoLectura.ABANDONADO);
        l4.setPoseido(true);
        lista.add(l4);

        return FXCollections.observableArrayList(lista);
    }

    @Test
    @DisplayName("Los chips de filtrado rápido deben filtrar correctamente según el estado de lectura y digital")
    public void testFiltrosRapidosChips() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearBibliotecaVariada());
        controller.setLibrosPorPagina(10);

        // Por defecto: todos los poseídos (4 libros)
        controller.refrescarCuadricula();
        assertEquals(4, controller.getTotalLibrosFiltrados());

        // Filtrar chip Leyendo
        controller.filtrarChipLeyendo();
        assertEquals(1, controller.getTotalLibrosFiltrados());

        // Filtrar chip Leídos
        controller.filtrarChipLeidos();
        assertEquals(1, controller.getTotalLibrosFiltrados());

        // Filtrar chip Pendientes
        controller.filtrarChipPendientes();
        assertEquals(1, controller.getTotalLibrosFiltrados());

        // Filtrar chip Digitales
        controller.filtrarChipDigitales();
        assertEquals(1, controller.getTotalLibrosFiltrados());

        // Volver a Todos
        controller.filtrarChipTodos();
        assertEquals(4, controller.getTotalLibrosFiltrados());
    }

    @Test
    @DisplayName("Las tarjetas de libros deben incluir autor, calificación y descripciones accesibles")
    public void testRiquezaTarjetaLibro() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearBibliotecaVariada());
        controller.setLibrosPorPagina(10);
        controller.refrescarCuadricula();

        FlowPane panel = controller.getPanelMisLibros();
        assertNotNull(panel);
        assertEquals(4, panel.getChildren().size());

        Node primeraTarjeta = panel.getChildren().get(0);
        assertTrue(primeraTarjeta instanceof VBox);
        VBox card = (VBox) primeraTarjeta;

        // Verificar que la tarjeta tiene clase CSS moderna
        assertTrue(card.getStyleClass().contains("book-card"));

        // Verificar que es enfocable por teclado (a11y)
        assertTrue(card.isFocusTraversable());

        // Al estar ordenado alfabéticamente por título (A-Z), el primero es "Dune"
        String accessibleTextDune = card.getAccessibleText();
        assertNotNull(accessibleTextDune);
        assertTrue(accessibleTextDune.contains("Dune"));
        assertTrue(accessibleTextDune.contains("Frank Herbert"));
        assertTrue(accessibleTextDune.contains("4 estrellas"));

        // El segundo es "El Quijote"
        Node segundaTarjeta = panel.getChildren().get(1);
        assertTrue(segundaTarjeta instanceof VBox);
        String accessibleTextQuijote = ((VBox) segundaTarjeta).getAccessibleText();
        assertNotNull(accessibleTextQuijote);
        assertTrue(accessibleTextQuijote.contains("El Quijote"));
        assertTrue(accessibleTextQuijote.contains("Miguel de Cervantes"));
        assertTrue(accessibleTextQuijote.contains("5 estrellas"));
    }

    @Test
    @DisplayName("La alternancia entre vista de cuadrícula y lista compacta debe cambiar el modo de vista")
    public void testAlternarVistas() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearBibliotecaVariada());
        controller.setLibrosPorPagina(10);

        // Inicialmente modo cuadrícula
        controller.cambiarAVistaCuadricula();
        controller.refrescarCuadricula();
        assertEquals(4, controller.getPanelMisLibros().getChildren().size());

        // Cambiar a modo lista
        controller.cambiarAVistaLista();
        // Verificar que la paginación y libros siguen disponibles
        assertEquals(4, controller.getTotalLibrosFiltrados());
    }

    @Test
    @DisplayName("El Sidebar debe inicializarse y actualizar el reto de lectura y los contadores")
    public void testSidebarRetoYContadores() {
        SidebarController sidebar = new SidebarController();
        java.util.Map<String, String> prefs = new java.util.HashMap<>();
        prefs.put("reto_anual", "10");

        sidebar.initData(null, crearBibliotecaVariada(), prefs, null);
        assertEquals(SidebarController.VISTA_TODOS, sidebar.getEstanteriaSeleccionada());
    }

    @Test
    @DisplayName("El botón de sincronización rápida debe cambiar su estado visual y animación")
    public void testBotonSincronizacionRapida() {
        PrimaryController controller = new PrimaryController();
        Button btn = new Button();
        btn.getStyleClass().add("sync-btn-top");

        try {
            java.lang.reflect.Field field = PrimaryController.class.getDeclaredField("btnSyncRapido");
            field.setAccessible(true);
            field.set(controller, btn);

            java.lang.reflect.Field iconField = PrimaryController.class.getDeclaredField("lblSyncIcon");
            iconField.setAccessible(true);
            Label icon = new Label("🔄");
            iconField.set(controller, icon);
            btn.setGraphic(icon);

            // Iniciar sincronización
            controller.iniciarAnimacionSincronizacion();
            assertTrue(btn.isDisabled(), "El botón debe deshabilitarse durante la sincronización");
            assertTrue(btn.getStyleClass().contains("syncing"), "Debe tener clase CSS 'syncing'");
            assertTrue(btn.getText().contains("Sincronizando"), "Debe indicar que está sincronizando");

            // Finalizar con éxito
            controller.detenerAnimacionSincronizacion(true);
            assertTrue(btn.getStyleClass().contains("sync-success"), "Debe tener clase CSS 'sync-success'");
            assertTrue(btn.getText().contains("Al día"), "Debe indicar que está al día");
            assertEquals("✓", icon.getText());
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            fail("No debería fallar la prueba del botón de sincronización: " + ex.getMessage());
        }
    }
}
