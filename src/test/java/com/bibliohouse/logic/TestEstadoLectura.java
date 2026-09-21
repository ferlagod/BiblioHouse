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

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para EstadoLectura y su integración con Libro y LanguageManager.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
class TestEstadoLectura {

    private Locale localeOriginal;

    @BeforeEach
    void setUp() {
        localeOriginal = LanguageManager.getLocale();
    }

    @AfterEach
    void tearDown() {
        LanguageManager.setLocale(localeOriginal);
    }

    @Test
    @DisplayName("Parseo de estados en español con y sin tildes")
    void testParseoEspanol() {
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("Leído"));
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("leido"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("Leyendo"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("leyendo"));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString("Pendiente"));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString("pendiente"));
        assertEquals(EstadoLectura.ABANDONADO, EstadoLectura.fromString("Abandonado"));
    }

    @Test
    @DisplayName("Parseo multilingüe tolerante (inglés, catalán, gallego, euskera, portugués)")
    void testParseoMultilingue() {
        // Inglés
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("Read"));
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("read"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("Reading"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("reading"));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString("Pending"));
        assertEquals(EstadoLectura.ABANDONADO, EstadoLectura.fromString("Abandoned"));

        // Catalán
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("Llegit"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("Llegint"));

        // Gallego / Portugués
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("Lido"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("Lendo"));

        // Euskera
        assertEquals(EstadoLectura.LEIDO, EstadoLectura.fromString("Irakurrita"));
        assertEquals(EstadoLectura.LEYENDO, EstadoLectura.fromString("Irakurtzen"));
    }

    @Test
    @DisplayName("Parseo defensivo con nulos, espacios y valores desconocidos")
    void testParseoDefensivo() {
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString(null));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString(""));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString("   "));
        assertEquals(EstadoLectura.PENDIENTE, EstadoLectura.fromString("CualquierTextoDesconocido123"));
    }

    @Test
    @DisplayName("Integración con Libro: normalización automática y sincronización de leido")
    void testIntegracionConLibro() {
        Libro libro = new Libro();

        // Asignar en inglés
        libro.setEstadoLectura("Reading");
        assertEquals(EstadoLectura.LEYENDO, libro.getEstadoLecturaEnum());
        assertEquals("Leyendo", libro.getEstadoLectura());
        assertFalse(libro.isLeido());

        // Asignar enum LEIDO
        libro.setEstadoLecturaEnum(EstadoLectura.LEIDO);
        assertEquals(EstadoLectura.LEIDO, libro.getEstadoLecturaEnum());
        assertEquals("Leído", libro.getEstadoLectura());
        assertTrue(libro.isLeido());

        // Asignar texto en minúsculas "leido"
        libro.setEstadoLectura("leido");
        assertEquals(EstadoLectura.LEIDO, libro.getEstadoLecturaEnum());
        assertTrue(libro.isLeido());

        // Asignar "Pending"
        libro.setEstadoLectura("Pending");
        assertEquals(EstadoLectura.PENDIENTE, libro.getEstadoLecturaEnum());
        assertFalse(libro.isLeido());
    }

    @Test
    @DisplayName("Etiquetas traducidas según el idioma configurado en LanguageManager")
    void testEtiquetasTraducidas() {
        LanguageManager.setLocale("en");
        assertEquals("Read", EstadoLectura.LEIDO.getEtiqueta());
        assertEquals("Reading", EstadoLectura.LEYENDO.getEtiqueta());
        assertEquals("Pending", EstadoLectura.PENDIENTE.getEtiqueta());

        LanguageManager.setLocale("gl");
        assertEquals("Lido", EstadoLectura.LEIDO.getEtiqueta());
        assertEquals("Lendo", EstadoLectura.LEYENDO.getEtiqueta());
        assertEquals("Pendente", EstadoLectura.PENDIENTE.getEtiqueta());

        LanguageManager.setLocale("ca");
        assertEquals("Llegit", EstadoLectura.LEIDO.getEtiqueta());
        assertEquals("Llegint", EstadoLectura.LEYENDO.getEtiqueta());

        LanguageManager.setLocale("es");
        assertEquals("Leído", EstadoLectura.LEIDO.getEtiqueta());
        assertEquals("Leyendo", EstadoLectura.LEYENDO.getEtiqueta());
        assertEquals("Pendiente", EstadoLectura.PENDIENTE.getEtiqueta());
    }
}
