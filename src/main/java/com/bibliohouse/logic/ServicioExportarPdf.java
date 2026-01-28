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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Esto es el servicio para exportar libros a PDF. Crea informes en PDF que
 * quedan bastante profesionales, con estadísticas, agrupación por estanterías y
 * todo bien formateado. Útil para tener un backup en papel o para compartir.
 *
 * @author Fernando Lago
 * @version 1.1
 */
public class ServicioExportarPdf {

    private static final Logger LOGGER = Logger.getLogger(ServicioExportarPdf.class.getName());

    // Aquí configuramos los márgenes y el espaciado del PDF para que quede bien
    private static final float MARGIN = 50;
    private static final float LEADING = 14; // Espaciado entre líneas
    private static final float SECTION_SPACING = 20; // Separación entre secciones

    // Los tamaños de letra que usamos en el PDF
    private static final float FONT_SIZE_TITLE = 18; // Para el título principal
    private static final float FONT_SIZE_HEADING = 14; // Para encabezados
    private static final float FONT_SIZE_SUBHEADING = 12; // Para subtítulos
    private static final float FONT_SIZE_NORMAL = 10; // Texto normal
    private static final float FONT_SIZE_SMALL = 8; // Texto pequeño (detalles)

    /**
     * Exporta una lista de libros filtrados a un archivo PDF.
     *
     * @param libros Lista de libros a exportar.
     * @param librosPorEstanteria Mapa de libros organizados por estantería.
     * @param destino Archivo de destino para el PDF.
     * @param criteriosFiltro Descripción de los filtros aplicados.
     * @return true si la exportación fue exitosa, false en caso contrario.
     */
    public boolean exportarLibrosPDF(List<Libro> libros, Map<String, List<Libro>> librosPorEstanteria,
            File destino, String criteriosFiltro) {
        try (PDDocument document = new PDDocument()) {

            // Crear primera página con estadísticas
            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, firstPage)) {
                float yPosition = firstPage.getMediaBox().getHeight() - MARGIN;

                // Título principal
                yPosition = agregarTexto(contentStream, "INFORME DE BIBLIOTECA", MARGIN, yPosition,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
                yPosition -= LEADING;

                // Fecha de generación
                String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                yPosition = agregarTexto(contentStream, "Fecha de generación: " + fecha, MARGIN, yPosition,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
                yPosition -= SECTION_SPACING;

                // Criterios de filtro si existen
                if (criteriosFiltro != null && !criteriosFiltro.isEmpty()) {
                    yPosition = agregarTexto(contentStream, "Filtros aplicados:", MARGIN, yPosition,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
                    yPosition -= LEADING / 2;
                    yPosition = agregarTextoMultilinea(contentStream, criteriosFiltro, MARGIN + 10, yPosition,
                            new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL,
                            firstPage.getMediaBox().getWidth() - 2 * MARGIN - 10);
                    yPosition -= SECTION_SPACING;
                }

                // Estadísticas generales
                yPosition = agregarEstadisticas(contentStream, libros, yPosition, firstPage.getMediaBox().getWidth());
            }

            // Agregar páginas con libros agrupados por estantería
            agregarLibrosPorEstanteria(document, librosPorEstanteria);

            // Guardar el documento
            document.save(destino);
            LOGGER.log(Level.INFO, "PDF exportado exitosamente a: {0}", destino.getAbsolutePath());
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al exportar PDF", e);
            return false;
        }
    }

    /**
     * Agrega estadísticas generales de la biblioteca al PDF.
     *
     * @param contentStream Stream de contenido de la página.
     * @param libros Lista de libros.
     * @param yPosition Posición Y inicial.
     * @param pageWidth Ancho de la página.
     * @return Nueva posición Y después de agregar las estadísticas.
     * @throws IOException Si hay error al escribir en el PDF.
     */
    private float agregarEstadisticas(PDPageContentStream contentStream, List<Libro> libros,
            float yPosition, float pageWidth) throws IOException {
        // Título de sección
        yPosition = agregarTexto(contentStream, "ESTADÍSTICAS GENERALES", MARGIN, yPosition,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
        yPosition -= LEADING;

        // Total de libros
        yPosition = agregarTexto(contentStream, "Total de libros: " + libros.size(), MARGIN + 10, yPosition,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        yPosition -= LEADING / 2;

        // Estadísticas por estado de lectura
        Map<String, Long> porEstado = new HashMap<>();
        for (Libro libro : libros) {
            String estado = libro.getEstadoLectura() != null ? libro.getEstadoLectura() : "Pendiente";
            porEstado.put(estado, porEstado.getOrDefault(estado, 0L) + 1);
        }

        yPosition = agregarTexto(contentStream, "Por estado de lectura:", MARGIN + 10, yPosition,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        yPosition -= LEADING / 2;

        for (Map.Entry<String, Long> entry : porEstado.entrySet()) {
            yPosition = agregarTexto(contentStream, "  - " + entry.getKey() + ": " + entry.getValue(),
                    MARGIN + 20, yPosition, new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        yPosition -= LEADING / 2;

        // Estadísticas por género
        Map<String, Long> porGenero = new HashMap<>();
        for (Libro libro : libros) {
            String genero = libro.getGenero() != null && !libro.getGenero().isEmpty()
                    ? libro.getGenero()
                    : "Sin género";
            porGenero.put(genero, porGenero.getOrDefault(genero, 0L) + 1);
        }

        yPosition = agregarTexto(contentStream, "Por género (top 5):", MARGIN + 10, yPosition,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        yPosition -= LEADING / 2;

        // Mostrar solo los top 5 géneros
        List<Map.Entry<String, Long>> topGeneros = porGenero.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : topGeneros) {
            yPosition = agregarTexto(contentStream, "  - " + entry.getKey() + ": " + entry.getValue(),
                    MARGIN + 20, yPosition, new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        return yPosition - SECTION_SPACING;
    }

    /**
     * Agrega los libros organizados por estantería al documento PDF.
     *
     * @param document Documento PDF.
     * @param librosPorEstanteria Mapa de libros organizados por estantería.
     * @throws IOException Si hay error al escribir en el PDF.
     */
    private void agregarLibrosPorEstanteria(PDDocument document, Map<String, List<Libro>> librosPorEstanteria)
            throws IOException {

        for (Map.Entry<String, List<Libro>> entry : librosPorEstanteria.entrySet()) {
            String estanteria = entry.getKey();
            List<Libro> libros = entry.getValue();

            if (libros.isEmpty()) {
                continue;
            }

            // Nueva página para cada estantería
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = page.getMediaBox().getHeight() - MARGIN;

                // Título de la estantería
                String tituloEstanteria = estanteria.isEmpty() ? "SIN ESTANTERÍA" : estanteria.toUpperCase();
                yPosition = agregarTexto(contentStream, tituloEstanteria, MARGIN, yPosition,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
                yPosition -= LEADING;

                yPosition = agregarTexto(contentStream, libros.size() + " libro(s)", MARGIN, yPosition,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
                yPosition -= SECTION_SPACING;

                // Agregar cada libro
                for (Libro libro : libros) {
                    // Verificar si necesitamos una nueva página
                    if (yPosition < MARGIN + 100) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        try (PDPageContentStream newStream = new PDPageContentStream(document, page)) {
                            yPosition = page.getMediaBox().getHeight() - MARGIN;

                            // Continuar con el nuevo stream
                            yPosition = agregarLibro(newStream, libro, yPosition, page.getMediaBox().getWidth());
                        }

                        // Crear nuevo stream para el siguiente libro
                        if (libros.indexOf(libro) < libros.size() - 1) {
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            yPosition = page.getMediaBox().getHeight() - MARGIN;
                        }
                        break;
                    } else {
                        yPosition = agregarLibro(contentStream, libro, yPosition, page.getMediaBox().getWidth());
                    }
                }
            }
        }
    }

    /**
     * Agrega la información de un libro al PDF.
     *
     * @param contentStream Stream de contenido de la página.
     * @param libro Libro a agregar.
     * @param yPosition Posición Y inicial.
     * @param pageWidth Ancho de la página.
     * @return Nueva posición Y después de agregar el libro.
     * @throws IOException Si hay error al escribir en el PDF.
     */
    private float agregarLibro(PDPageContentStream contentStream, Libro libro, float yPosition, float pageWidth)
            throws IOException {

        PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // Título del libro
        String titulo = libro.getTitulo() != null ? libro.getTitulo() : "Sin título";
        yPosition = agregarTexto(contentStream, "• " + titulo, MARGIN + 10, yPosition, boldFont, FONT_SIZE_SUBHEADING);
        yPosition -= LEADING / 2;

        // Autor
        if (libro.getAutor() != null && !libro.getAutor().isEmpty()) {
            yPosition = agregarTexto(contentStream, "  Autor: " + libro.getAutor(), MARGIN + 15, yPosition,
                    normalFont, FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        // Editorial y año
        String editorialAnio = "";
        if (libro.getEditorial() != null && !libro.getEditorial().isEmpty()) {
            editorialAnio = "Editorial: " + libro.getEditorial();
        }
        if (libro.getAño() != null && !libro.getAño().isEmpty()) {
            editorialAnio += (editorialAnio.isEmpty() ? "" : " | ") + "Año: " + libro.getAño();
        }
        if (!editorialAnio.isEmpty()) {
            yPosition = agregarTexto(contentStream, "  " + editorialAnio, MARGIN + 15, yPosition,
                    normalFont, FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        // Género
        if (libro.getGenero() != null && !libro.getGenero().isEmpty()) {
            yPosition = agregarTexto(contentStream, "  Género: " + libro.getGenero(), MARGIN + 15, yPosition,
                    normalFont, FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        // ISBN
        if (libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
            yPosition = agregarTexto(contentStream, "  ISBN: " + libro.getIsbn(), MARGIN + 15, yPosition,
                    normalFont, FONT_SIZE_SMALL);
            yPosition -= LEADING / 2;
        }

        // Cantidad y estado
        String cantidadEstado = "Cantidad: " + libro.getCantidad();
        String estado = libro.getEstadoLectura() != null ? libro.getEstadoLectura() : "Pendiente";
        cantidadEstado += " | Estado: " + estado;
        yPosition = agregarTexto(contentStream, "  " + cantidadEstado, MARGIN + 15, yPosition,
                normalFont, FONT_SIZE_SMALL);
        yPosition -= LEADING / 2;

        // Espaciado entre libros
        yPosition -= LEADING;

        return yPosition;
    }

    /**
     * Agrega texto en una posición específica del PDF.
     *
     * @param contentStream Stream de contenido de la página.
     * @param texto Texto a agregar.
     * @param x Posición X.
     * @param y Posición Y.
     * @param font Fuente a utilizar.
     * @param fontSize Tamaño de la fuente.
     * @return Nueva posición Y después del texto.
     * @throws IOException Si hay error al escribir en el PDF.
     */
    private float agregarTexto(PDPageContentStream contentStream, String texto, float x, float y,
            PDFont font, float fontSize) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        // Reemplazar caracteres especiales que pueden causar problemas
        String textoLimpio = limpiarTexto(texto);
        contentStream.showText(textoLimpio);
        contentStream.endText();
        return y - LEADING;
    }

    /**
     * Agrega texto multilínea en el PDF.
     *
     * @param contentStream Stream de contenido de la página.
     * @param texto Texto a agregar.
     * @param x Posición X.
     * @param y Posición Y inicial.
     * @param font Fuente a utilizar.
     * @param fontSize Tamaño de la fuente.
     * @param maxWidth Ancho máximo del texto.
     * @return Nueva posición Y después del texto.
     * @throws IOException Si hay error al escribir en el PDF.
     */
    private float agregarTextoMultilinea(PDPageContentStream contentStream, String texto, float x, float y,
            PDFont font, float fontSize, float maxWidth) throws IOException {
        String[] lineas = texto.split("\n");
        float yPosition = y;

        for (String linea : lineas) {
            yPosition = agregarTexto(contentStream, linea, x, yPosition, font, fontSize);
        }

        return yPosition;
    }

    /**
     * Limpia el texto para evitar problemas con caracteres especiales en PDF.
     * PDFBox Standard14Fonts no soporta todos los caracteres Unicode.
     *
     * @param texto Texto original.
     * @return Texto limpio compatible con PDF.
     */
    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        // Reemplazar caracteres españoles por equivalentes ASCII cuando sea necesario
        // o mantenerlos si PDFBox los soporta en la versión 3.0
        return texto
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U")
                .replace("ñ", "n").replace("Ñ", "N")
                .replace("ü", "u").replace("Ü", "U");
    }
}
