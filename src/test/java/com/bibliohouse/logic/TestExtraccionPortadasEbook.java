/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.bibliohouse.logic;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la extracción local y optimizada de portadas
 * incrustadas en archivos de e-books (EPUB mediante manifiesto OPF y PDF vía PDFBox).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class TestExtraccionPortadasEbook {

    @TempDir
    Path tempDir;

    private File userDir;
    private File coversDir;

    @BeforeEach
    void setUp() {
        userDir = tempDir.resolve("user_data").toFile();
        coversDir = new File(userDir, "covers");
        coversDir.mkdirs();
    }

    /**
     * Crea un archivo EPUB sintético válido con container.xml, content.opf y una portada de alta resolución.
     */
    private File crearEpubDePrueba(String nombreArchivo, int width, int height) throws IOException {
        File epubFile = tempDir.resolve(nombreArchivo).toFile();

        // 1. Generar imagen de portada simulada
        BufferedImage coverImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = coverImg.createGraphics();
        g.setColor(new Color(20, 60, 120));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("PORTADA EPUB", width / 4, height / 2);
        g.dispose();

        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        ImageIO.write(coverImg, "jpg", imgBaos);
        byte[] coverBytes = imgBaos.toByteArray();

        // 2. Empaquetar ZIP con estructura EPUB estándar
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(epubFile))) {
            // mimetype
            zos.putNextEntry(new ZipEntry("mimetype"));
            zos.write("application/epub+zip".getBytes(StandardCharsets.US_ASCII));
            zos.closeEntry();

            // META-INF/container.xml
            zos.putNextEntry(new ZipEntry("META-INF/container.xml"));
            String containerXml = "<?xml version=\"1.0\"?>\n"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
                    + "  <rootfiles>\n"
                    + "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n"
                    + "  </rootfiles>\n"
                    + "</container>";
            zos.write(containerXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // OEBPS/content.opf con metadato de portada
            zos.putNextEntry(new ZipEntry("OEBPS/content.opf"));
            String opfXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"BookId\">\n"
                    + "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n"
                    + "    <dc:title>Libro Digital de Prueba</dc:title>\n"
                    + "    <dc:creator>Autor Digital</dc:creator>\n"
                    + "    <meta name=\"cover\" content=\"cover-image-id\"/>\n"
                    + "  </metadata>\n"
                    + "  <manifest>\n"
                    + "    <item id=\"cover-image-id\" href=\"images/cover.jpg\" media-type=\"image/jpeg\"/>\n"
                    + "  </manifest>\n"
                    + "  <spine>\n"
                    + "  </spine>\n"
                    + "</package>";
            zos.write(opfXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // OEBPS/images/cover.jpg
            zos.putNextEntry(new ZipEntry("OEBPS/images/cover.jpg"));
            zos.write(coverBytes);
            zos.closeEntry();
        }

        return epubFile;
    }

    /**
     * Crea un archivo PDF sintético de 1 página utilizando Apache PDFBox.
     */
    private File crearPdfDePrueba(String nombreArchivo) throws IOException {
        File pdfFile = tempDir.resolve(nombreArchivo).toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            doc.getDocumentInformation().setTitle("PDF de Prueba Automatizada");
            doc.getDocumentInformation().setAuthor("Autor PDF");

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
                content.newLineAtOffset(100, 700);
                content.showText("Primera Página PDF");
                content.endText();
            }
            doc.save(pdfFile);
        }
        return pdfFile;
    }

    @Test
    @DisplayName("Extracción de portada EPUB leyendo OPF y optimizando a JPEG en tiempo récord")
    public void testExtraccionPortadaEpub() throws IOException {
        File epub = crearEpubDePrueba("novela.epub", 1600, 2400);

        long start = System.currentTimeMillis();
        String rutaPortada = EbookMetadataService.extraerPortadaEbook(epub, "id-epub-test", userDir.getAbsolutePath());
        long duration = System.currentTimeMillis() - start;

        assertNotNull(rutaPortada, "Debe retornar la ruta de la portada extraída.");
        File archivoPortada = new File(rutaPortada);
        assertTrue(archivoPortada.exists(), "El archivo de portada debe existir en disco.");
        assertTrue(rutaPortada.endsWith(".jpg"), "La portada debe guardarse en formato .jpg optimizado.");

        // Validar dimensiones y compresión
        BufferedImage img = ImageIO.read(archivoPortada);
        assertNotNull(img);
        assertEquals(600, img.getWidth(), "El ancho debe haberse redimensionado a 600 px.");
        assertEquals(900, img.getHeight(), "El alto debe haberse redimensionado a 900 px (ratio 2:3).");
        assertTrue(archivoPortada.length() > 5000 && archivoPortada.length() < 120_000,
                "El peso debe estar entre 5 KB y 120 KB (comprimido JPEG 85%).");

        // Validar rapidez (usualmente ~20-30 ms, permitimos holgura de prueba)
        assertTrue(duration < 1000, "La extracción de portada del EPUB debe ser inmediata (tiempo: " + duration + "ms).");
    }

    @Test
    @DisplayName("Extracción de primera página de PDF con PDFBox a 72 DPI en ~30 ms")
    public void testExtraccionPortadaPdf() throws IOException {
        File pdf = crearPdfDePrueba("documento.pdf");

        long start = System.currentTimeMillis();
        String rutaPortada = EbookMetadataService.extraerPortadaEbook(pdf, "id-pdf-test", userDir.getAbsolutePath());
        long duration = System.currentTimeMillis() - start;

        assertNotNull(rutaPortada);
        File archivoPortada = new File(rutaPortada);
        assertTrue(archivoPortada.exists());
        assertTrue(rutaPortada.endsWith(".jpg"));

        BufferedImage img = ImageIO.read(archivoPortada);
        assertNotNull(img);
        // A4 a 72 DPI tiene ancho estándar de ~595 px (<= 600 px)
        assertTrue(img.getWidth() <= 600, "El ancho debe ser menor o igual a 600 px: " + img.getWidth());
        assertTrue(img.getHeight() > 0);

        assertTrue(duration < 1000, "La extracción de portada de PDF debe completarse en ~30 ms (tiempo: " + duration + "ms).");
    }

    @Test
    @DisplayName("asegurarPortadaEbook auto-asigna portada a libros digitales que carecen de ella")
    public void testAsegurarPortadaEbook() throws IOException {
        File epub = crearEpubDePrueba("clasico.epub", 1200, 1800);

        Libro libro = new Libro();
        libro.setId("libro-digital-1");
        libro.setTitulo("Clásico");
        libro.setEsDigital(true);
        libro.setRutaArchivoDigital(epub.getAbsolutePath());
        libro.setPortadaURL(null); // Sin portada

        // 1. Debe extraer y asignar
        boolean asignada = EbookMetadataService.asegurarPortadaEbook(libro, userDir.getAbsolutePath());
        assertTrue(asignada, "Debe reportar que se extrajo y asignó una portada.");
        assertNotNull(libro.getPortadaURL());
        assertTrue(new File(libro.getPortadaURL()).exists());

        // 2. Si se vuelve a invocar, detecta que ya tiene portada y no hace trabajo redundante
        boolean reintento = EbookMetadataService.asegurarPortadaEbook(libro, userDir.getAbsolutePath());
        assertFalse(reintento, "No debe re-extraer si el libro ya cuenta con portada válida.");
    }

    @Test
    @DisplayName("JsonManager auto-extrae y persiste portadas de e-books locales al cargar la biblioteca")
    public void testJsonManagerAutoExtraePortadasAlCargar() throws IOException {
        File pdf = crearPdfDePrueba("manual.pdf");
        String rutaEbook = EbookMetadataService.hacerEbookLocalOffline(pdf.getAbsolutePath(), "libro-pdf-auto", userDir.getAbsolutePath());

        JsonManager jsonManager = new JsonManager(userDir.getAbsolutePath());

        Libro libro = new Libro();
        libro.setId("libro-pdf-auto");
        libro.setTitulo("Manual");
        libro.setEsDigital(true);
        libro.setRutaArchivoDigital(rutaEbook);
        libro.setPortadaURL(""); // Vacía

        jsonManager.guardarLibros(List.of(libro));

        // Cargar libros: JsonManager detecta e-book sin portada y la extrae automáticamente
        List<Libro> cargados = jsonManager.cargarLibros();
        assertEquals(1, cargados.size());
        Libro cargado = cargados.get(0);

        assertNotNull(cargado.getPortadaURL());
        assertFalse(cargado.getPortadaURL().isBlank());
        assertTrue(new File(cargado.getPortadaURL()).exists(),
                "La portada extraída automáticamente debe existir físicamente en disco.");
    }
}
