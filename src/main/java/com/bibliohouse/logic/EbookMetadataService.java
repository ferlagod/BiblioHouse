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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Servicio para extraer metadatos y portadas de archivos de e-books (EPUB, PDF,
 * MOBI). Utiliza APIs estándar de Java (ZIP + XML) para EPUB y PDFBox para PDF.
 * No requiere dependencias externas adicionales.
 *
 * @author Fernando Lago Dávila
 * @version 1.9
 */
public class EbookMetadataService {

    private static final Logger LOGGER = Logger.getLogger(EbookMetadataService.class.getName());

    /**
     * Extensiones de archivo aceptadas como e-books.
     */
    private static final String[] EXTENSIONES_EBOOK = {".epub", ".pdf", ".mobi"};

    /**
     * Copia el archivo del e-book a la carpeta local "ebooks" dentro del
     * directorio de la base de datos del usuario, de forma similar a las portadas.
     *
     * @param rutaOriginal Ruta original del archivo.
     * @param idLibro ID único del libro (usado para el nombre del archivo).
     * @param carpetaUsuario Carpeta base de datos del usuario.
     * @return La nueva ruta absoluta, o la original si falla.
     */
    public static String hacerEbookLocalOffline(String rutaOriginal, String idLibro, String carpetaUsuario) {
        if (rutaOriginal == null || rutaOriginal.isEmpty() || idLibro == null) {
            return rutaOriginal;
        }

        File dirEbooks = new File(carpetaUsuario, "ebooks");
        if (!dirEbooks.exists()) {
            dirEbooks.mkdirs();
        }

        File archivoOrigen = new File(rutaOriginal);
        if (!archivoOrigen.exists()) {
            return rutaOriginal;
        }

        // Extraer extensión original
        String name = archivoOrigen.getName().toLowerCase();
        String extension = "";
        if (name.endsWith(".epub")) extension = ".epub";
        else if (name.endsWith(".pdf")) extension = ".pdf";
        else if (name.endsWith(".mobi")) extension = ".mobi";
        else {
            int lastDot = name.lastIndexOf(".");
            if (lastDot > 0) extension = name.substring(lastDot);
        }

        File archivoDestino = new File(dirEbooks, idLibro + extension);

        try {
            if (!archivoOrigen.getCanonicalPath().equals(archivoDestino.getCanonicalPath())) {
                Files.copy(archivoOrigen.toPath(), archivoDestino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return archivoDestino.getAbsolutePath();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo copiar el ebook a local: " + e.getMessage());
            return rutaOriginal;
        }
    }

    /**
     * Comprueba si un archivo es un e-book soportado por su extensión.
     *
     * @param archivo El archivo a comprobar.
     * @return true si la extensión es .epub, .pdf o .mobi.
     */
    public static boolean esArchivoEbook(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return false;
        }
        String nombre = archivo.getName().toLowerCase();
        for (String ext : EXTENSIONES_EBOOK) {
            if (nombre.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Crea un objeto Libro a partir de un archivo de e-book. Extrae metadatos
     * y portada según el formato del archivo.
     *
     * @param archivo     El archivo EPUB, PDF o MOBI.
     * @param rutaUsuario Ruta del directorio del usuario para guardar portadas.
     * @return Un nuevo Libro con los datos extraídos y marcado como digital.
     */
    public static Libro crearLibroDesdeArchivo(File archivo, String rutaUsuario) {
        if (archivo == null || !archivo.exists()) {
            return null;
        }

        String nombre = archivo.getName().toLowerCase();
        Libro libro;

        if (nombre.endsWith(".epub")) {
            libro = extraerDesdeEpub(archivo, rutaUsuario);
        } else if (nombre.endsWith(".pdf")) {
            libro = extraerDesdePdf(archivo, rutaUsuario);
        } else if (nombre.endsWith(".mobi")) {
            libro = extraerDesdeMobi(archivo);
        } else {
            return null;
        }

        if (libro != null) {
            libro.setEsDigital(true);
            String rutaCopiada = hacerEbookLocalOffline(archivo.getAbsolutePath(), libro.getId(), rutaUsuario);
            libro.setRutaArchivoDigital(rutaCopiada);
            libro.setPoseido(true);
        }

        return libro;
    }

    // ==================== EPUB ====================

    /**
     * Extrae metadatos y portada de un archivo EPUB. Un EPUB es un ZIP que
     * contiene un archivo OPF con metadatos Dublin Core y referencias a la
     * portada.
     */
    private static Libro extraerDesdeEpub(File archivo, String rutaUsuario) {
        Libro libro = new Libro();

        try (ZipFile zip = new ZipFile(archivo)) {
            // 1. Localizar el archivo OPF (content.opf) desde container.xml
            String opfPath = encontrarOpfPath(zip);
            if (opfPath == null) {
                // Fallback: intentar content.opf directamente
                opfPath = "content.opf";
            }

            ZipEntry opfEntry = zip.getEntry(opfPath);
            if (opfEntry == null) {
                // Segundo fallback: buscar cualquier .opf en el ZIP
                opfPath = buscarEntradaConExtension(zip, ".opf");
                if (opfPath != null) {
                    opfEntry = zip.getEntry(opfPath);
                }
            }

            if (opfEntry != null) {
                // El directorio base del OPF para resolver rutas relativas
                String opfDir = "";
                int lastSlash = opfPath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    opfDir = opfPath.substring(0, lastSlash + 1);
                }

                Document doc = parsearXml(zip.getInputStream(opfEntry));
                if (doc != null) {
                    extraerMetadatosDublinCore(doc, libro);
                    extraerPortadaEpub(zip, doc, opfDir, libro, rutaUsuario);
                }
            }

            // Si no se pudo extraer el título, usar el nombre del archivo
            if (libro.getTitulo() == null || libro.getTitulo().isEmpty()) {
                libro.setTitulo(nombreSinExtension(archivo.getName()));
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al leer EPUB: " + archivo.getName(), e);
            libro.setTitulo(nombreSinExtension(archivo.getName()));
        }

        return libro;
    }

    /**
     * Busca la ruta del archivo OPF dentro del container.xml estándar.
     */
    private static String encontrarOpfPath(ZipFile zip) {
        ZipEntry container = zip.getEntry("META-INF/container.xml");
        if (container == null) {
            return null;
        }

        try {
            Document doc = parsearXml(zip.getInputStream(container));
            if (doc == null) {
                return null;
            }

            NodeList rootfiles = doc.getElementsByTagName("rootfile");
            for (int i = 0; i < rootfiles.getLength(); i++) {
                Element el = (Element) rootfiles.item(i);
                String fullPath = el.getAttribute("full-path");
                if (fullPath != null && !fullPath.isEmpty()) {
                    return fullPath;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error leyendo container.xml", e);
        }

        return null;
    }

    /**
     * Busca la primera entrada en el ZIP con la extensión dada.
     */
    private static String buscarEntradaConExtension(ZipFile zip, String extension) {
        return zip.stream()
                .filter(e -> e.getName().toLowerCase().endsWith(extension))
                .map(ZipEntry::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Extrae metadatos Dublin Core del documento OPF.
     */
    private static void extraerMetadatosDublinCore(Document doc, Libro libro) {
        // Título
        String titulo = obtenerTextoElemento(doc, "dc:title");
        if (titulo == null) {
            titulo = obtenerTextoElemento(doc, "title");
        }
        if (titulo != null) {
            libro.setTitulo(titulo.trim());
        }

        // Autor
        String autor = obtenerTextoElemento(doc, "dc:creator");
        if (autor == null) {
            autor = obtenerTextoElemento(doc, "creator");
        }
        if (autor != null) {
            libro.setAutor(autor.trim());
        }

        // Editorial
        String editorial = obtenerTextoElemento(doc, "dc:publisher");
        if (editorial == null) {
            editorial = obtenerTextoElemento(doc, "publisher");
        }
        if (editorial != null) {
            libro.setEditorial(editorial.trim());
        }

        // Fecha (año)
        String fecha = obtenerTextoElemento(doc, "dc:date");
        if (fecha == null) {
            fecha = obtenerTextoElemento(doc, "date");
        }
        if (fecha != null) {
            // Puede ser "2020-01-15" o solo "2020"
            String anio = fecha.trim();
            if (anio.length() >= 4) {
                libro.setAño(anio.substring(0, 4));
            }
        }

        // Género/Categoría
        String genero = obtenerTextoElemento(doc, "dc:subject");
        if (genero == null) {
            genero = obtenerTextoElemento(doc, "subject");
        }
        if (genero != null) {
            libro.setGenero(genero.trim());
        }

        // ISBN (buscar en dc:identifier con esquema ISBN)
        NodeList identifiers = doc.getElementsByTagName("dc:identifier");
        if (identifiers.getLength() == 0) {
            identifiers = doc.getElementsByTagName("identifier");
        }
        for (int i = 0; i < identifiers.getLength(); i++) {
            Element el = (Element) identifiers.item(i);
            String scheme = el.getAttribute("opf:scheme");
            String text = el.getTextContent();
            if ("ISBN".equalsIgnoreCase(scheme)
                    || (text != null && text.replaceAll("[^0-9X]", "").length() >= 10)) {
                String isbn = text.trim().replaceAll("^(urn:isbn:|ISBN:?\\s*)", "");
                libro.setIsbn(isbn);
                break;
            }
        }
    }

    /**
     * Intenta extraer la portada embebida del EPUB.
     */
    private static void extraerPortadaEpub(ZipFile zip, Document opfDoc, String opfDir,
            Libro libro, String rutaUsuario) {
        try {
            // 1. Buscar en <meta name="cover" content="xxx"/>
            String coverId = null;
            NodeList metaNodes = opfDoc.getElementsByTagName("meta");
            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element meta = (Element) metaNodes.item(i);
                if ("cover".equals(meta.getAttribute("name"))) {
                    coverId = meta.getAttribute("content");
                    break;
                }
            }

            // 2. Buscar el item con ese ID en el manifest
            String coverHref = null;
            NodeList items = opfDoc.getElementsByTagName("item");

            if (coverId != null) {
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    if (coverId.equals(item.getAttribute("id"))) {
                        coverHref = item.getAttribute("href");
                        break;
                    }
                }
            }

            // 3. Fallback: buscar item con properties="cover-image" (EPUB 3)
            if (coverHref == null) {
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    String props = item.getAttribute("properties");
                    if (props != null && props.contains("cover-image")) {
                        coverHref = item.getAttribute("href");
                        break;
                    }
                }
            }

            // 4. Fallback: buscar item con media-type de imagen y "cover" en el id o href
            if (coverHref == null) {
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    String mediaType = item.getAttribute("media-type");
                    String id = item.getAttribute("id");
                    String href = item.getAttribute("href");
                    if (mediaType != null && mediaType.startsWith("image/")) {
                        if ((id != null && id.toLowerCase().contains("cover"))
                                || (href != null && href.toLowerCase().contains("cover"))) {
                            coverHref = href;
                            break;
                        }
                    }
                }
            }

            // 5. Extraer la imagen si la encontramos
            if (coverHref != null) {
                // Resolver la ruta relativa al OPF
                String fullCoverPath = opfDir + coverHref;
                // Intentar sin la ruta del OPF si falla
                ZipEntry coverEntry = zip.getEntry(fullCoverPath);
                if (coverEntry == null) {
                    coverEntry = zip.getEntry(coverHref);
                }

                if (coverEntry != null) {
                    guardarPortadaDesdeStream(zip.getInputStream(coverEntry),
                            libro, rutaUsuario);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo extraer portada del EPUB", e);
        }
    }

    // ==================== PDF ====================

    /**
     * Extrae la primera página de un PDF como portada y usa el nombre del
     * archivo como título.
     */
    private static Libro extraerDesdePdf(File archivo, String rutaUsuario) {
        Libro libro = new Libro();
        libro.setTitulo(nombreSinExtension(archivo.getName()));

        try (PDDocument pdf = Loader.loadPDF(archivo)) {
            // Intentar extraer título del metadato PDF
            if (pdf.getDocumentInformation() != null) {
                String titulo = pdf.getDocumentInformation().getTitle();
                if (titulo != null && !titulo.isBlank()) {
                    libro.setTitulo(titulo.trim());
                }
                String autor = pdf.getDocumentInformation().getAuthor();
                if (autor != null && !autor.isBlank()) {
                    libro.setAutor(autor.trim());
                }
            }

            // Extraer cantidad de páginas
            int paginasTotales = pdf.getNumberOfPages();
            if (paginasTotales > 0) {
                libro.setPaginasTotales(paginasTotales);
            }

            // Renderizar primera página como portada
            if (paginasTotales > 0) {
                PDFRenderer renderer = new PDFRenderer(pdf);
                BufferedImage firstPage = renderer.renderImageWithDPI(0, 150);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(firstPage, "jpg", baos);

                guardarPortadaDesdeBytes(baos.toByteArray(), libro, rutaUsuario);
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al leer PDF: " + archivo.getName(), e);
        }

        return libro;
    }

    // ==================== MOBI ====================

    /**
     * Para MOBI solo usamos el nombre del archivo como título (no hay librería
     * Java ligera estándar para parsear MOBI).
     */
    private static Libro extraerDesdeMobi(File archivo) {
        Libro libro = new Libro();
        libro.setTitulo(nombreSinExtension(archivo.getName()));
        return libro;
    }

    // ==================== UTILIDADES ====================

    /**
     * Parsea un InputStream XML a un Document DOM.
     */
    private static Document parsearXml(InputStream is) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // Seguridad: desactivar DTDs externas
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error parseando XML", e);
            return null;
        }
    }

    /**
     * Obtiene el texto del primer elemento con el tag dado.
     */
    private static String obtenerTextoElemento(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    /**
     * Guarda la portada desde un InputStream en la carpeta de covers del
     * usuario.
     */
    private static void guardarPortadaDesdeStream(InputStream is, Libro libro,
            String rutaUsuario) {
        try {
            byte[] bytes = is.readAllBytes();
            guardarPortadaDesdeBytes(bytes, libro, rutaUsuario);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error guardando portada desde stream", e);
        }
    }

    /**
     * Guarda la portada desde bytes en la carpeta covers/ del usuario.
     */
    private static void guardarPortadaDesdeBytes(byte[] bytes, Libro libro,
            String rutaUsuario) {
        try {
            String coversDir = rutaUsuario + File.separator + "covers";
            File coversDirFile = new File(coversDir);
            if (!coversDirFile.exists()) {
                coversDirFile.mkdirs();
            }

            // Detectar extensión por los primeros bytes
            String ext = detectarExtensionImagen(bytes);

            String nombreArchivo = libro.getId() + ext;
            File archivoPortada = new File(coversDir, nombreArchivo);

            Files.write(archivoPortada.toPath(), bytes);
            libro.setPortadaURL(archivoPortada.getAbsolutePath());

        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error guardando portada", e);
        }
    }

    /**
     * Detecta la extensión de imagen a partir de los magic bytes.
     */
    private static String detectarExtensionImagen(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return ".jpg";
        } else if (bytes.length >= 4 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return ".png";
        } else if (bytes.length >= 4 && bytes[0] == 0x47 && bytes[1] == 0x49
                && bytes[2] == 0x46) {
            return ".gif";
        }
        return ".jpg"; // Default
    }

    /**
     * Obtiene el nombre del archivo sin extensión.
     */
    private static String nombreSinExtension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto > 0) {
            return nombreArchivo.substring(0, punto);
        }
        return nombreArchivo;
    }
}
