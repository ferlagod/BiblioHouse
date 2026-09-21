package com.bibliohouse.logic;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import com.bibliohouse.utils.PdfFontHelper;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para generar PDFs con códigos QR de los libros físicos.
 * Permite imprimir etiquetas con información detallada de cada libro.
 */
public class ServicioEtiquetasFisicas {

    /**
     * Genera un archivo PDF que contiene una cuadrícula de etiquetas.
     * Cada etiqueta incluye un código QR con los metadatos del libro,
     * así como el título, autor y ubicación en texto claro.
     *
     * @param libros Lista de libros físicos a incluir en las etiquetas.
     * @param archivoDestino Archivo PDF de destino donde se guardará el resultado.
     * @throws Exception Si ocurre un error al procesar el código QR o el PDF.
     */
    public static void generarEtiquetasPDF(List<Libro> libros, File archivoDestino) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = PdfFontHelper.loadBoldFont(document);
            PDFont fontRegular = PdfFontHelper.loadRegularFont(document);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float startX = 50;
            float startY = 750;
            float currentX = startX;
            float currentY = startY;

            float rectWidth = 150;
            float rectHeight = 200;
            float spacingX = 20;
            float spacingY = 20;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            for (Libro libro : libros) {
                // Generate QR Code image
                BufferedImage qrImage = generarQR(libro, 120, 120);
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, qrImage);

                // Draw bounding box (optional, for cutting)
                contentStream.setStrokingColor(java.awt.Color.LIGHT_GRAY);
                contentStream.addRect(currentX, currentY - rectHeight, rectWidth, rectHeight);
                contentStream.stroke();
                
                contentStream.setNonStrokingColor(java.awt.Color.BLACK);

                // Draw QR Code
                contentStream.drawImage(pdImage, currentX + 15, currentY - 135, 120, 120);

                // Draw Text
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(currentX + 10, currentY - 15);
                
                String titulo = libro.getTitulo();
                if (titulo.length() > 22) titulo = titulo.substring(0, 19) + "...";
                contentStream.showText(PdfFontHelper.sanitizarTexto(titulo, fontBold));
                
                contentStream.setFont(fontRegular, 8);
                contentStream.newLineAtOffset(0, -12);
                String autor = libro.getAutor() != null ? libro.getAutor() : "";
                if (autor.length() > 25) autor = autor.substring(0, 22) + "...";
                contentStream.showText(PdfFontHelper.sanitizarTexto(autor, fontRegular));

                contentStream.newLineAtOffset(0, -12);
                String ubic = libro.getUbicacionFisica() != null ? libro.getUbicacionFisica() : "";
                if (ubic.length() > 25) ubic = ubic.substring(0, 22) + "...";
                contentStream.showText(PdfFontHelper.sanitizarTexto(ubic, fontRegular));
                
                contentStream.endText();

                // Move to next label position
                currentX += rectWidth + spacingX;
                if (currentX + rectWidth > page.getMediaBox().getWidth() - 50) {
                    currentX = startX;
                    currentY -= rectHeight + spacingY;
                    
                    if (currentY - rectHeight < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        currentX = startX;
                        currentY = startY;
                    }
                }
            }

            contentStream.close();
            document.save(archivoDestino);
        }
    }

    /**
     * Limpia un texto de caracteres especiales para evitar errores en PDFBox.
     * PDFBox utilizando fuentes estándar como Helvetica (WinAnsiEncoding) no
     * soporta ciertos caracteres Unicode, por lo que los eliminamos.
     *
     * @param texto El texto original.
     * @return El texto limpio de caracteres incompatibles.
     */
    private static String limpiarTextoParaPDF(String texto) {
        // WinAnsiEncoding limitation in PDFBox for standard fonts.
        // We replace weird characters with spaces to avoid crash.
        return texto.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "");
    }

    /**
     * Genera un código QR en formato de imagen con los datos de un libro.
     * Los metadatos se agrupan en un texto plano codificado en UTF-8.
     *
     * @param libro El libro físico del cual generar el código QR.
     * @param width El ancho de la imagen a generar.
     * @param height El alto de la imagen a generar.
     * @return Una imagen (BufferedImage) con el código QR renderizado.
     * @throws Exception Si hay algún problema durante la generación o codificación.
     */
    private static BufferedImage generarQR(Libro libro, int width, int height) throws Exception {
        StringBuilder qrData = new StringBuilder();
        qrData.append("Título: ").append(libro.getTitulo()).append("\n");
        qrData.append("Autor: ").append(libro.getAutor() != null ? libro.getAutor() : "-").append("\n");
        qrData.append("Estanterías: ").append(libro.getEstanterias() != null ? String.join(", ", libro.getEstanterias()) : "-").append("\n");
        qrData.append("Saga: ").append(libro.getSerie() != null && !libro.getSerie().isEmpty() ? libro.getSerie() : "-").append("\n");
        qrData.append("Editorial: ").append(libro.getEditorial() != null && !libro.getEditorial().isEmpty() ? libro.getEditorial() : "-").append("\n");
        qrData.append("Ubicación: ").append(libro.getUbicacionFisica() != null && !libro.getUbicacionFisica().isEmpty() ? libro.getUbicacionFisica() : "-").append("\n");
        qrData.append("Tipo/Género: ").append(libro.getGenero() != null && !libro.getGenero().isEmpty() ? libro.getGenero() : "-").append("\n");

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // Force UTF-8

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                qrData.toString(),
                BarcodeFormat.QR_CODE, width, height, hints);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
