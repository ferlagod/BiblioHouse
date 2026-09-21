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
package com.bibliohouse.utils;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Utilidad centralizada para la carga de fuentes tipográficas y sanitización de
 * texto en documentos PDF generados con Apache PDFBox.
 * <p>
 * Permite cargar tipografías TrueType (como Roboto) mediante {@link PDType0Font}
 * con soporte Unicode nativo, evitando mutilar textos con acentos, tildes, eñes
 * y caracteres internacionales, y proporcionando un fallback robusto en caso de
 * ausencia de recursos.
 * </p>
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public final class PdfFontHelper {

    private static final Logger LOGGER = Logger.getLogger(PdfFontHelper.class.getName());

    public static final String FONT_REGULAR_PATH = "/fonts/Roboto-Regular.ttf";
    public static final String FONT_BOLD_PATH = "/fonts/Roboto-Bold.ttf";

    private PdfFontHelper() {
        // Clase de utilidad no instanciable
    }

    /**
     * Carga la fuente estándar regular en formato TrueType (Unicode) para el documento PDF.
     * Si no se encuentra el recurso o falla la carga, recurre a HELVETICA.
     *
     * @param document El documento {@link PDDocument} en el que se incrustará la fuente.
     * @return La fuente {@link PDFont} lista para su uso.
     */
    public static PDFont loadRegularFont(PDDocument document) {
        try (InputStream is = PdfFontHelper.class.getResourceAsStream(FONT_REGULAR_PATH)) {
            if (is != null) {
                return PDType0Font.load(document, is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar la fuente TrueType regular (" + FONT_REGULAR_PATH + "), usando Helvetica como fallback.", e);
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    /**
     * Carga la fuente estándar en negrita en formato TrueType (Unicode) para el documento PDF.
     * Si no se encuentra el recurso o falla la carga, recurre a HELVETICA_BOLD.
     *
     * @param document El documento {@link PDDocument} en el que se incrustará la fuente.
     * @return La fuente {@link PDFont} en negrita lista para su uso.
     */
    public static PDFont loadBoldFont(PDDocument document) {
        try (InputStream is = PdfFontHelper.class.getResourceAsStream(FONT_BOLD_PATH)) {
            if (is != null) {
                return PDType0Font.load(document, is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar la fuente TrueType negrita (" + FONT_BOLD_PATH + "), usando Helvetica Bold como fallback.", e);
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    /**
     * Sanitiza el texto eliminando caracteres de control incompatibles con PDF
     * y garantizando que cualquier carácter sin glifo en la fuente dada no
     * provoque una excepción {@link IllegalArgumentException}.
     *
     * @param texto El texto original que se desea renderizar.
     * @param font  La fuente tipográfica utilizada para el renderizado.
     * @return Texto seguro para invocar en {@code contentStream.showText(texto)}.
     */
    public static String sanitizarTexto(String texto, PDFont font) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        // 1. Eliminar caracteres de control excepto saltos de línea y tabulaciones
        String limpio = texto.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        if (font == null) {
            return limpio;
        }

        // 2. Verificar compatibilidad de glifos en la fuente para evitar IllegalArgumentException
        StringBuilder sb = new StringBuilder(limpio.length());
        for (int i = 0; i < limpio.length(); i++) {
            int codePoint = limpio.codePointAt(i);
            String charStr = new String(Character.toChars(codePoint));
            try {
                font.encode(charStr);
                sb.append(charStr);
            } catch (Exception ex) {
                // Carácter no soportado en la fuente (ej. emoji exótico): sustituir por espacio
                sb.append(' ');
            }
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }
        }

        return sb.toString();
    }
}
