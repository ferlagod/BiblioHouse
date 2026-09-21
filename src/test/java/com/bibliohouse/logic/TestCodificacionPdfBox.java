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

import com.bibliohouse.utils.CarnetGenerator;
import com.bibliohouse.utils.PdfFontHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la solución a las caídas por codificación de
 * caracteres en Apache PDFBox, asegurando compatibilidad completa con acentos,
 * eñes, diéresis y caracteres internacionales sin lanzar IllegalArgumentException.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
class TestCodificacionPdfBox {

    @TempDir
    File tempDir;

    @Test
    void testPdfFontHelperCargaFuentesYSanitiza() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDFont regular = PdfFontHelper.loadRegularFont(doc);
            PDFont bold = PdfFontHelper.loadBoldFont(doc);

            assertNotNull(regular, "La fuente regular no debe ser nula");
            assertNotNull(bold, "La fuente bold no debe ser nula");

            // Validar sanitización con cadenas nulas y vacías
            assertEquals("", PdfFontHelper.sanitizarTexto(null, regular));
            assertEquals("", PdfFontHelper.sanitizarTexto("", regular));

            // Validar que se preservan los caracteres en español e internacionales
            String original = "Íñigo Peña Àngel Ça João Müller 100% «Cita»";
            String sanitizado = PdfFontHelper.sanitizarTexto(original, regular);
            assertTrue(sanitizado.contains("Í"), "Debe preservar 'Í'");
            assertTrue(sanitizado.contains("ñ"), "Debe preservar 'ñ'");
            assertTrue(sanitizado.contains("Peña"), "Debe preservar 'Peña'");
            assertTrue(sanitizado.contains("Ça"), "Debe preservar cedilla 'Ça'");

            // Validar eliminación de caracteres de control ilegales en PDF
            String conControl = "Texto\u0000con\u0007control";
            String sinControl = PdfFontHelper.sanitizarTexto(conControl, regular);
            assertFalse(sinControl.contains("\u0000"));
            assertFalse(sinControl.contains("\u0007"));
        }
    }

    @Test
    void testCarnetConAcentosYEñesNoLanzaExcepcion() {
        File carnetPdf = new File(tempDir, "carnets_test.pdf");

        List<Socio> socios = new ArrayList<>();
        socios.add(new Socio("Íñigo", "López Peña", "12345678A", "Calle Mayor 1", 101));
        socios.add(new Socio("María", "Muñoz Núñez", "87654321B", "Avenida Sol 2", 102));
        socios.add(new Socio("Àngel", "Ça Pereira", "11223344C", "Rúa Nova 3", 103));

        CarnetGenerator generator = new CarnetGenerator();

        // En la versión anterior con Standard14Fonts.HELVETICA esto lanzaba IllegalArgumentException
        assertDoesNotThrow(() -> {
            generator.generarCarnetsPDF(socios, carnetPdf);
        }, "Generar carnets con acentos y eñes no debe lanzar ninguna excepción");

        assertTrue(carnetPdf.exists(), "El archivo PDF de carnets debe haberse generado");
        assertTrue(carnetPdf.length() > 1000, "El archivo PDF generado debe tener un tamaño válido");
    }

    @Test
    void testExportarPdfConCaracteresCompletosSinMutilar() {
        File reportePdf = new File(tempDir, "reporte_libros.pdf");

        List<Libro> libros = new ArrayList<>();
        Libro libro1 = new Libro("Cien Años de Soledad", "Gabriel García Márquez", "Editorial Sudamericana", "1967", "Ficción", "9780307474728", "");
        libro1.setAño("1967");
        libro1.setCantidad(2);
        libro1.setEstadoLectura("Leído");
        libros.add(libro1);

        Libro libro2 = new Libro("El Señor de los Anillos: La Compañía del Anillo", "J.R.R. Tolkien", "Minotauro", "1954", "Fantasía Épica", "9788445071403", "");
        libro2.setAño("1954");
        libro2.setCantidad(1);
        libro2.setEstadoLectura("Leyendo");
        libros.add(libro2);

        Map<String, List<Libro>> porEstanteria = new HashMap<>();
        porEstanteria.put("Obras Maestras & Ficción", libros);

        ServicioExportarPdf exportador = new ServicioExportarPdf();

        assertDoesNotThrow(() -> {
            boolean ok = exportador.exportarLibrosPDF(libros, porEstanteria, reportePdf, "Filtro: Ficción & Fantasía (Año >= 1950)");
            assertTrue(ok, "La exportación debe retornar true");
        });

        assertTrue(reportePdf.exists(), "El reporte PDF debe haberse generado");
        assertTrue(reportePdf.length() > 2000, "El archivo reporte PDF debe tener un tamaño válido");
    }

    @Test
    void testServicioEtiquetasFisicasConCaracteresEspeciales() {
        File etiquetasPdf = new File(tempDir, "etiquetas_test.pdf");

        Libro libro = new Libro("Crònica d'un assassinat", "Vicente Cañas & Peña", "Edición Especial", "2021", "Misterio", "9781234567890", "");
        libro.setUbicacionFisica("Pasillo A - Sección 3ª");
        libro.setEstanterias(Collections.singletonList("Novela Negra & Policiaca"));

        assertDoesNotThrow(() -> {
            ServicioEtiquetasFisicas.generarEtiquetasPDF(Collections.singletonList(libro), etiquetasPdf);
        });

        assertTrue(etiquetasPdf.exists(), "El PDF de etiquetas físicas debe haberse generado");
        assertTrue(etiquetasPdf.length() > 1000, "El archivo de etiquetas debe tener un tamaño válido");
    }
}
