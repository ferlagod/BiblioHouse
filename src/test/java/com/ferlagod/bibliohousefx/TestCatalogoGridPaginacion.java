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

import com.bibliohouse.logic.Libro;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la paginación y virtualización del catálogo en
 * CatalogoGridController.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class TestCatalogoGridPaginacion {

    @BeforeAll
    public static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    private ObservableList<Libro> crearListaLibros(int cantidad) {
        List<Libro> lista = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            Libro libro = new Libro();
            libro.setTitulo("Libro de Prueba #" + i);
            libro.setAutor("Autor #" + i);
            libro.setPoseido(true);
            lista.add(libro);
        }
        return FXCollections.observableArrayList(lista);
    }

    @Test
    @DisplayName("Debe calcular correctamente el total de páginas según el tamaño de página")
    public void testCalculoTotalPaginas() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearListaLibros(100));
        controller.setLibrosPorPagina(48);

        controller.refrescarCuadricula();

        assertEquals(100, controller.getTotalLibrosFiltrados());
        assertEquals(3, controller.getTotalPaginas(), "100 libros con página de 48 deben generar 3 páginas");
        assertEquals(1, controller.getPaginaActual());
        assertEquals(48, controller.getPanelMisLibros().getChildren().size(), "La página 1 debe contener 48 tarjetas");
    }

    @Test
    @DisplayName("Debe navegar secuencialmente entre páginas y ajustar el número de tarjetas en la última página")
    public void testNavegacionSecuencial() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearListaLibros(100));
        controller.setLibrosPorPagina(48);
        controller.refrescarCuadricula();

        // Navegar a la página 2
        controller.irPaginaSiguiente();
        assertEquals(2, controller.getPaginaActual());
        assertEquals(48, controller.getPanelMisLibros().getChildren().size(), "La página 2 debe tener 48 tarjetas");

        // Navegar a la página 3 (última página: 100 - 96 = 4 tarjetas)
        controller.irPaginaSiguiente();
        assertEquals(3, controller.getPaginaActual());
        assertEquals(4, controller.getPanelMisLibros().getChildren().size(), "La última página debe tener exactamente 4 tarjetas");

        // Intentar avanzar más allá de la última página no debe cambiar de página
        controller.irPaginaSiguiente();
        assertEquals(3, controller.getPaginaActual());

        // Retroceder a página 2
        controller.irPaginaAnterior();
        assertEquals(2, controller.getPaginaActual());

        // Saltar a la primera página
        controller.irPrimeraPagina();
        assertEquals(1, controller.getPaginaActual());

        // Saltar a la última página
        controller.irUltimaPagina();
        assertEquals(3, controller.getPaginaActual());
    }

    @Test
    @DisplayName("Cambiar el tamaño de página debe recalcular el total de páginas y clampar la página actual")
    public void testCambioTamanoPagina() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearListaLibros(100));

        // Con 24 libros por página -> 5 páginas
        controller.setLibrosPorPagina(24);
        controller.refrescarCuadricula();
        assertEquals(5, controller.getTotalPaginas());

        // Ir a la página 5
        controller.irUltimaPagina();
        assertEquals(5, controller.getPaginaActual());

        // Cambiar a 96 libros por página -> 2 páginas (página actual debe clampar a 2)
        controller.setLibrosPorPagina(96);
        controller.refrescarCuadricula();
        assertEquals(2, controller.getTotalPaginas());
        assertEquals(2, controller.getPaginaActual(), "La página actual debe clampar al nuevo total de páginas");
    }

    @Test
    @DisplayName("Modo 'Todos' (librosPorPagina = Integer.MAX_VALUE) debe mostrar todos los libros en 1 sola página")
    public void testModoTodos() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearListaLibros(50));
        controller.setLibrosPorPagina(Integer.MAX_VALUE);

        controller.refrescarCuadricula();

        assertEquals(1, controller.getTotalPaginas());
        assertEquals(1, controller.getPaginaActual());
        assertEquals(50, controller.getPanelMisLibros().getChildren().size());
    }

    @Test
    @DisplayName("Ordenar por recientes muestra primero el último libro añadido")
    public void testOrdenPorRecientes() {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        controller.setListaLibrosCompleta(crearListaLibros(5));
        controller.setLibrosPorPagina(48);

        controller.ordenarPorReciente();

        List<String> titulos = controller.getPanelMisLibros().getChildren().stream()
                .map(n -> n.getAccessibleText())
                .toList();
        assertEquals(5, titulos.size());
        assertTrue(titulos.get(0).startsWith("Libro: Libro de Prueba #5"), titulos.get(0));
        assertTrue(titulos.get(4).startsWith("Libro: Libro de Prueba #1"), titulos.get(4));
    }

    @Test
    @DisplayName("La cuadrícula y la tabla usan cada una su propio nodo de estado vacío")
    public void testEstadoVacioNoCompartido() throws Exception {
        CatalogoGridController controller = new CatalogoGridController();
        controller.setPanelMisLibros(new FlowPane());
        javafx.scene.control.TableView<Libro> tabla = new javafx.scene.control.TableView<>();
        java.lang.reflect.Field campo = CatalogoGridController.class.getDeclaredField("tablaMisLibros");
        campo.setAccessible(true);
        campo.set(controller, tabla);
        controller.setListaLibrosCompleta(FXCollections.observableArrayList());

        controller.refrescarCuadricula();

        assertEquals(1, controller.getPanelMisLibros().getChildren().size());
        javafx.scene.Node vacioCuadricula = controller.getPanelMisLibros().getChildren().get(0);
        assertTrue(vacioCuadricula.getStyleClass().contains("catalog-empty-state"));
        assertNotNull(tabla.getPlaceholder());
        assertNotSame(vacioCuadricula, tabla.getPlaceholder(),
                "Si se comparte el nodo, la tabla lo saca de la cuadrícula y esta queda en blanco");
        assertSame(controller.getPanelMisLibros(), vacioCuadricula.getParent());
    }
}
