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
 * Utilidad para generar carnets de socios en formato PDF.
 * Incluye código de barras para facilitar el préstamo.
 * 
 * @author Fernando Lago
 * @version 1.0
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

    public void generarCarnetsPDF(List<Socio> socios, File archivoSalida) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Fuente estándar
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float currentX = MARGIN_X;
            // Empezamos desde arriba (A4 height es aprox 842)
            float currentY = PDRectangle.A4.getHeight() - MARGIN_Y - CARD_HEIGHT;

            int col = 0;
            int row = 0;

            for (Socio socio : socios) {
                // Dibujar borde de la tarjeta
                contentStream.setStrokingColor(Color.LIGHT_GRAY);
                contentStream.setLineWidth(1);
                contentStream.addRect(currentX, currentY, CARD_WIDTH, CARD_HEIGHT);
                contentStream.stroke();

                // Cabecera "BIBLIOHOUSE"
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                contentStream.newLineAtOffset(currentX + 15, currentY + CARD_HEIGHT - 25);
                contentStream.showText("BIBLIOHOUSE");
                contentStream.endText();

                // Etiqueta "Socio"
                contentStream.beginText();
                contentStream.setFont(fontRegular, 8);
                contentStream.newLineAtOffset(currentX + 15, currentY + CARD_HEIGHT - 38);
                contentStream.showText("CARNET DE SOCIO");
                contentStream.endText();

                // Nombre del Socio
                contentStream.beginText();
                contentStream.setFont(fontBold, 12);
                contentStream.newLineAtOffset(currentX + 15, currentY + 70);
                // Truncar si es muy largo
                String nombreCompleto = socio.getNombreCompleto();
                if (nombreCompleto.length() > 25) {
                    nombreCompleto = nombreCompleto.substring(0, 22) + "...";
                }
                contentStream.showText(nombreCompleto.toUpperCase());
                contentStream.endText();

                // Nº Socio
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(currentX + 15, currentY + 55);
                contentStream.showText("Nº: " + socio.getNumeroSocio());
                contentStream.endText();

                // Generar código de barras
                try {
                    // El código será el número de socio.
                    BufferedImage barcodeImage = generateBarcodeImage(String.valueOf(socio.getNumeroSocio()));
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, barcodeImage);

                    // Posicionar abajo centrado o a la izquierda
                    contentStream.drawImage(pdImage, currentX + 15, currentY + 10, 150, 30);

                } catch (Exception e) {
                    e.printStackTrace();
                    contentStream.beginText();
                    contentStream.setFont(fontRegular, 8);
                    contentStream.newLineAtOffset(currentX + 15, currentY + 15);
                    contentStream.showText("Error generando código");
                    contentStream.endText();
                }

                // Control de filas y columnas
                col++;
                currentX += CARD_WIDTH + SPACING_X;

                if (col >= 2) { // 2 columnas por página
                    col = 0;
                    currentX = MARGIN_X;
                    row++;
                    currentY -= (CARD_HEIGHT + SPACING_Y);
                }

                // Si llenamos la página (8 tarjetas: 2 col x 4 filas)
                if (row >= 4) {
                    contentStream.close();

                    // Crear nueva página
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
     * Genera una imagen BufferedImage con el código de barras (Code 128).
     */
    private BufferedImage generateBarcodeImage(String text) throws Exception {
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, 300, 60); // Ancho x Alto nativo
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
