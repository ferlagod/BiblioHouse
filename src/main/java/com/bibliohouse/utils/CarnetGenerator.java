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

import com.bibliohouse.logic.Socio;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Utilidad para generar carnets de socios en formato PDF. Incluye código de
 * barras para facilitar el préstamo.
 *
 * @author Fernando Lago
 * @version 1.2
 */
public class CarnetGenerator {

    // Tamaño de tarjeta estándar tipo visita (85x55 mm aprox)
    // En puntos PDF (1 pulgada = 72 puntos).
    // 85mm ~ 3.35 in * 72 ~ 241 pts
    // 55mm ~ 2.16 in * 72 ~ 155 pts
    private static final float CARD_WIDTH = 241;
    private static final float CARD_HEIGHT = 153; // Ajustado para que quepan 4 filas
    private static final float MARGIN_X = 40;
    private static final float MARGIN_Y = 30;
    private static final float SPACING_X = 20;
    private static final float SPACING_Y = 20;

    // Colores corporativos basados en el CSS
    private static final Color COLOR_PRIMARIO = new Color(0, 120, 215); // Azul primary
    private static final Color COLOR_TEXTO_OSCURO = new Color(51, 51, 51);

    /**
     * Genera un archivo PDF con los carnets de los socios de la biblioteca.
     * Cada carnet incluye el nombre del socio, su número de socio y un código
     * de barras. Si falla la generación del código de barras, se muestra un
     * texto alternativo en el carnet.
     *
     * @param socios Lista de objetos {@link Socio} cuyos carnets se generarán.
     * @param archivoSalida Archivo de destino donde se guardará el PDF
     * generado.
     * @throws IOException Si ocurre un error al crear o escribir en el archivo
     * PDF.
     */
    public void generarCarnetsPDF(List<Socio> socios, File archivoSalida) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Cargar el logo usando el InputStream para evitar fallos cuando compiles el .jar
            PDImageXObject logoBiblio = null;
            try (InputStream is = getClass().getResourceAsStream("/resources/LogoBiblioHouse100.png")) {
                if (is != null) {
                    byte[] imageBytes = is.readAllBytes();
                    logoBiblio = PDImageXObject.createFromByteArray(document, imageBytes, "logo");
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar el logo para el carnet: " + e.getMessage());
            }

            float currentX = MARGIN_X;
            float currentY = PDRectangle.A4.getHeight() - MARGIN_Y - CARD_HEIGHT;

            int col = 0;
            int row = 0;

            for (Socio socio : socios) {
                // 1. Dibujar borde exterior de la tarjeta
                contentStream.setStrokingColor(Color.LIGHT_GRAY);
                contentStream.setLineWidth(1);
                contentStream.addRect(currentX, currentY, CARD_WIDTH, CARD_HEIGHT);
                contentStream.stroke();

                // 2. Dibujar franja superior de cabecera (Azul)
                float headerHeight = 35;
                contentStream.setNonStrokingColor(COLOR_PRIMARIO);
                contentStream.addRect(currentX, currentY + CARD_HEIGHT - headerHeight, CARD_WIDTH, headerHeight);
                contentStream.fill();

                // 3. Dibujar Logo en la cabecera (Esquina superior derecha)
                if (logoBiblio != null) {
                    float logoSize = 25;
                    contentStream.drawImage(logoBiblio, currentX + CARD_WIDTH - logoSize - 10, currentY + CARD_HEIGHT - headerHeight + 5, logoSize, logoSize);
                }

                // 4. Textos de la cabecera (Blanco)
                contentStream.setNonStrokingColor(Color.WHITE);
                contentStream.beginText();
                contentStream.setFont(fontBold, 12);
                contentStream.newLineAtOffset(currentX + 10, currentY + CARD_HEIGHT - 18);
                contentStream.showText("BIBLIOHOUSE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(fontRegular, 7);
                contentStream.newLineAtOffset(currentX + 10, currentY + CARD_HEIGHT - 28);
                contentStream.showText("CARNET DE SOCIO");
                contentStream.endText();

                // 5. Datos del Socio (Gris oscuro/Negro)
                contentStream.setNonStrokingColor(COLOR_TEXTO_OSCURO);

                contentStream.beginText();
                contentStream.setFont(fontBold, 11);
                contentStream.newLineAtOffset(currentX + 10, currentY + 80);
                String nombreCompleto = socio.getNombreCompleto();
                if (nombreCompleto.length() > 26) {
                    nombreCompleto = nombreCompleto.substring(0, 23) + "...";
                }
                contentStream.showText(nombreCompleto.toUpperCase());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.newLineAtOffset(currentX + 10, currentY + 65);
                contentStream.showText("Socio Nº: " + socio.getNumeroSocio());
                contentStream.endText();

                // 6. Generar y centrar el código de barras
                try {
                    BufferedImage barcodeImage = generateBarcodeImage(String.valueOf(socio.getNumeroSocio()));
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, barcodeImage);

                    float barcodeWidth = 140;
                    float barcodeHeight = 30;
                    // Centrar horizontalmente
                    float barcodeX = currentX + ((CARD_WIDTH - barcodeWidth) / 2);

                    contentStream.drawImage(pdImage, barcodeX, currentY + 15, barcodeWidth, barcodeHeight);

                } catch (Exception e) {
                    contentStream.beginText();
                    contentStream.setFont(fontRegular, 8);
                    contentStream.newLineAtOffset(currentX + 10, currentY + 20);
                    contentStream.showText("Error generando código de barras");
                    contentStream.endText();
                }

                // Control de filas y columnas
                col++;
                currentX += CARD_WIDTH + SPACING_X;

                if (col >= 2) {
                    col = 0;
                    currentX = MARGIN_X;
                    row++;
                    currentY -= (CARD_HEIGHT + SPACING_Y);
                }

                if (row >= 4) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    currentX = MARGIN_X;
                    currentY = PDRectangle.A4.getHeight() - MARGIN_Y - CARD_HEIGHT;
                    col = 0;
                    row = 0;
                }
            }

            contentStream.close();
            document.save(archivoSalida);
        }
    }

    /**
     * Genera una imagen de código de barras en formato CODE 128 a partir de un
     * texto.
     *
     * @param text Texto que se codificará en el código de barras.
     * @return Imagen {@link BufferedImage} del código de barras generado.
     * @throws Exception Si ocurre un error durante la codificación del texto o
     * la generación de la imagen.
     */
    private BufferedImage generateBarcodeImage(String text) throws Exception {
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, 300, 60);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
