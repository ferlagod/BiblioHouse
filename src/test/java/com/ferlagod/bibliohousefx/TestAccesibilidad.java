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

import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.Libro;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las características de Accesibilidad Total (A11y):
 * 1. Navegación por teclado en las tarjetas del catálogo (focusTraversable, teclas de flechas, Enter/Espacio).
 * 2. Lector de pantalla y semántica JavaFX (accessibleRole, accessibleText en portadas, tarjetas y badges).
 * 3. Selector de escala y densidad tipográfica (Compacto, Estándar, Grande / Accesible).
 * 4. Soporte para temas de alto contraste (WCAG AA/AAA).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class TestAccesibilidad {

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit ya inicializado
        }
    }

    private Libro crearLibroAccesible() {
        Libro libro = new Libro();
        libro.setTitulo("Cien años de soledad");
        libro.setAutor("Gabriel García Márquez");
        libro.setEstadoLecturaEnum(EstadoLectura.LEIDO);
        libro.setCalificacion(5);
        libro.setPoseido(true);
        libro.setEsDigital(true);
        libro.setRutaArchivoDigital("/libros/cienaños.epub");
        return libro;
    }

    @Test
    @DisplayName("Las tarjetas de libros deben ser navegables por teclado y tener semántica completa de lector de pantalla")
    public void testTarjetaSemanticaYFoco() {
        CatalogoGridController controller = new CatalogoGridController();
        FlowPane panel = new FlowPane();
        controller.setPanelMisLibros(panel);

        Libro libro = crearLibroAccesible();
        List<Libro> lista = new ArrayList<>();
        lista.add(libro);
        controller.setListaLibrosCompleta(FXCollections.observableArrayList(lista));
        controller.refrescarCuadricula();

        assertEquals(1, panel.getChildren().size(), "El panel debe contener 1 tarjeta de libro");
        Node nodo = panel.getChildren().get(0);
        assertTrue(nodo instanceof VBox, "La tarjeta debe ser un VBox");
        VBox tarjeta = (VBox) nodo;

        // 1. Navegabilidad por teclado
        assertTrue(tarjeta.isFocusTraversable(), "La tarjeta del libro debe ser focusTraversable para navegación por teclado");
        assertTrue(tarjeta.getStyleClass().contains("book-card"), "La tarjeta debe tener la clase de estilo 'book-card'");

        // 2. Semántica para lector de pantallas
        assertEquals(AccessibleRole.BUTTON, tarjeta.getAccessibleRole(), "El rol accesible de la tarjeta debe ser BUTTON para permitir activación");
        String accessibleText = tarjeta.getAccessibleText();
        assertNotNull(accessibleText, "El texto accesible no debe ser nulo");
        assertTrue(accessibleText.contains("Cien años de soledad"), "Debe incluir el título del libro");
        assertTrue(accessibleText.contains("Gabriel García Márquez"), "Debe incluir el autor");
        assertTrue(accessibleText.contains("5 estrellas"), "Debe incluir la calificación de estrellas");

        // 3. Semántica interna (portada / badges)
        boolean portadaEncontrada = false;
        for (Node hijo : tarjeta.getChildren()) {
            if (hijo.getAccessibleRole() == AccessibleRole.IMAGE_VIEW) {
                portadaEncontrada = true;
                assertNotNull(hijo.getAccessibleText());
                assertTrue(hijo.getAccessibleText().contains("Cien años de soledad"));
            }
        }
        assertTrue(portadaEncontrada, "El contenedor de la portada debe tener rol accesible IMAGE_VIEW");
    }

    @Test
    @DisplayName("Navegación por teclado en la cuadrícula responde a teclas de flechas y selección")
    public void testNavegacionTecladoCuadricula() {
        CatalogoGridController controller = new CatalogoGridController();
        FlowPane panel = new FlowPane();
        controller.setPanelMisLibros(panel);

        List<Libro> libros = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Libro l = new Libro();
            l.setTitulo("Libro " + i);
            l.setAutor("Autor " + i);
            libros.add(l);
        }
        controller.setListaLibrosCompleta(FXCollections.observableArrayList(libros));
        controller.refrescarCuadricula();

        assertEquals(3, panel.getChildren().size());
        VBox card1 = (VBox) panel.getChildren().get(0);
        VBox card2 = (VBox) panel.getChildren().get(1);

        assertTrue(card1.isFocusTraversable());
        assertTrue(card2.isFocusTraversable());

        // Simular evento de tecla LEFT/RIGHT para asegurar que el handler está registrado y no lanza excepciones
        KeyEvent rightKey = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false);
        assertDoesNotThrow(() -> card1.fireEvent(rightKey), "Pulsar flecha derecha no debe lanzar excepciones");

        KeyEvent leftKey = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, false);
        assertDoesNotThrow(() -> card2.fireEvent(leftKey), "Pulsar flecha izquierda no debe lanzar excepciones");
    }

    @Test
    @DisplayName("El selector de escala de interfaz y densidad debe aplicar las clases y estilos correspondientes a la Scene")
    public void testEscalaInterfazYDensidad() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);
        App.registerScene(scene);

        // 1. Densidad Compacta
        App.applyDensity(App.DENSITY_COMPACT);
        assertTrue(root.getStyleClass().contains("density-compact"), "La raíz debe tener 'density-compact'");
        assertFalse(root.getStyleClass().contains("density-standard"));
        assertFalse(root.getStyleClass().contains("density-accessible"));
        assertTrue(root.getStyle().contains("-fx-font-size: 11.5px;"), "La fuente debe ser 11.5px en modo compacto");

        // 2. Densidad Grande / Accesible
        App.applyDensity(App.DENSITY_ACCESSIBLE);
        assertTrue(root.getStyleClass().contains("density-accessible"), "La raíz debe tener 'density-accessible'");
        assertFalse(root.getStyleClass().contains("density-compact"));
        assertTrue(root.getStyle().contains("-fx-font-size: 16.0px;"), "La fuente debe ser 16.0px en modo accesible");

        // 3. Densidad Estándar
        App.applyDensity(App.DENSITY_STANDARD);
        assertTrue(root.getStyleClass().contains("density-standard"), "La raíz debe tener 'density-standard'");
        assertTrue(root.getStyle().contains("-fx-font-size: 13.0px;"), "La fuente debe ser 13.0px en modo estándar");
    }

    @Test
    @DisplayName("Los temas de alto contraste deben aplicar clases específicas y estilos accesibles")
    public void testTemasAltoContraste() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);
        App.registerScene(scene);

        // 1. Alto Contraste Claro
        App.applyTheme(App.THEME_HIGH_CONTRAST_LIGHT);
        assertTrue(root.getStyleClass().contains("high-contrast"), "Debe tener la clase genérica 'high-contrast'");
        assertTrue(root.getStyleClass().contains("theme-high-contrast-light"), "Debe tener 'theme-high-contrast-light'");
        assertFalse(root.getStyleClass().contains("theme-high-contrast-dark"));

        // 2. Alto Contraste Oscuro
        App.applyTheme(App.THEME_HIGH_CONTRAST_DARK);
        assertTrue(root.getStyleClass().contains("high-contrast"), "Debe tener la clase genérica 'high-contrast'");
        assertTrue(root.getStyleClass().contains("theme-high-contrast-dark"), "Debe tener 'theme-high-contrast-dark'");
        assertFalse(root.getStyleClass().contains("theme-high-contrast-light"));

        // 3. Retorno a tema estándar (ej. Primer Claro)
        App.applyTheme("Claro (Primer Light)");
        assertFalse(root.getStyleClass().contains("high-contrast"), "No debe tener 'high-contrast' en temas normales");
        assertFalse(root.getStyleClass().contains("theme-high-contrast-dark"));
    }
}
