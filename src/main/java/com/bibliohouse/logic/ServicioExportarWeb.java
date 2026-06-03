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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Servicio para generar un catálogo web HTML estático autocontenido con un
 * diseño tipo galería Netflix. Las portadas de los libros se incrustan como
 * Base64 data URIs para máxima portabilidad (un solo archivo sin dependencias).
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class ServicioExportarWeb {

    private static final Logger LOGGER = Logger.getLogger(ServicioExportarWeb.class.getName());

    /** Ancho objetivo de las miniaturas incrustadas (px). */
    private static final int THUMBNAIL_WIDTH = 200;

    /** Calidad JPEG para las miniaturas (0.0 - 1.0). */
    private static final float JPEG_QUALITY = 0.70f;

    /**
     * Modos de agrupación para organizar los libros en el catálogo.
     */
    public enum ModoAgrupacion {
        GENERO,
        ESTANTERIA
    }

    /**
     * Genera un catálogo HTML autocontenido y lo escribe en el archivo
     * especificado.
     *
     * @param libros Lista de libros a incluir en el catálogo.
     * @param archivoDestino Archivo .html donde se guardará el catálogo.
     * @param nombreBiblioteca Nombre personalizado de la biblioteca.
     * @param modo Modo de agrupación (por género o por estantería).
     * @return true si la generación fue exitosa, false en caso de error.
     */
    public boolean exportarCatalogoWeb(List<Libro> libros, File archivoDestino,
            String nombreBiblioteca, ModoAgrupacion modo) {
        try {
            String html = generarHTML(libros, nombreBiblioteca, modo);
            try (Writer writer = new FileWriter(archivoDestino)) {
                writer.write(html);
                writer.flush();
            }
            LOGGER.log(Level.INFO, "Catálogo web generado: {0}", archivoDestino.getAbsolutePath());
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al generar el catálogo web", e);
            return false;
        }
    }

    /**
     * Genera el HTML completo del catálogo como un String.
     */
    private String generarHTML(List<Libro> libros, String nombreBiblioteca, ModoAgrupacion modo) {
        StringBuilder html = new StringBuilder();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Estadísticas
        long totalLibros = libros.size();
        long autoresUnicos = libros.stream()
                .map(Libro::getAutor)
                .filter(a -> a != null && !a.isEmpty())
                .distinct().count();
        long generosUnicos = libros.stream()
                .map(Libro::getGenero)
                .filter(g -> g != null && !g.isEmpty())
                .distinct().count();

        // Agrupar libros
        Map<String, List<Libro>> grupos = agruparLibros(libros, modo);

        // === HTML ===
        html.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<meta name=\"description\" content=\"Catálogo de biblioteca personal generado con BiblioHouse\">\n");
        html.append("<title>").append(escapeHtml(nombreBiblioteca)).append(" — BiblioHouse</title>\n");
        html.append("<style>\n");
        html.append(generarCSS());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // Header
        html.append("<header class=\"hero\">\n");
        html.append("  <div class=\"hero-content\">\n");
        html.append("    <h1 id=\"library-title\">📚 ").append(escapeHtml(nombreBiblioteca)).append("</h1>\n");
        html.append("    <p class=\"hero-subtitle\">Generado el ").append(fecha).append("</p>\n");
        html.append("    <div class=\"stats\">\n");
        html.append("      <div class=\"stat\"><span class=\"stat-num\">").append(totalLibros).append("</span><span class=\"stat-label\">Libros</span></div>\n");
        html.append("      <div class=\"stat\"><span class=\"stat-num\">").append(autoresUnicos).append("</span><span class=\"stat-label\">Autores</span></div>\n");
        html.append("      <div class=\"stat\"><span class=\"stat-num\">").append(generosUnicos).append("</span><span class=\"stat-label\">Géneros</span></div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</header>\n");

        // Search bar
        html.append("<div class=\"search-container\">\n");
        html.append("  <input type=\"text\" id=\"searchInput\" placeholder=\"🔍 Buscar por título, autor o género...\" autocomplete=\"off\">\n");
        html.append("  <div id=\"searchCount\" class=\"search-count\"></div>\n");
        html.append("</div>\n");

        // Secciones por categoría
        html.append("<main id=\"catalog\">\n");
        for (Map.Entry<String, List<Libro>> entry : grupos.entrySet()) {
            String categoria = entry.getKey().isEmpty() ? "Sin clasificar" : entry.getKey();
            List<Libro> librosCategoria = entry.getValue();

            html.append("<section class=\"category\" data-category=\"")
                    .append(escapeHtml(categoria.toLowerCase())).append("\">\n");
            html.append("  <h2 class=\"category-title\">").append(escapeHtml(categoria))
                    .append(" <span class=\"category-count\">(").append(librosCategoria.size()).append(")</span></h2>\n");
            html.append("  <div class=\"row-wrapper\">\n");
            html.append("    <div class=\"books-row\">\n");

            for (Libro libro : librosCategoria) {
                html.append(generarTarjetaLibro(libro));
            }

            html.append("    </div>\n");
            html.append("  </div>\n");
            html.append("</section>\n");
        }
        html.append("</main>\n");

        // No-results message
        html.append("<div id=\"noResults\" class=\"no-results\" style=\"display:none;\">\n");
        html.append("  <p>😕 No se encontraron libros con ese criterio.</p>\n");
        html.append("</div>\n");

        // Footer
        html.append("<footer>\n");
        html.append("  <p>Generado con <a href=\"https://bibliohouse.org\" target=\"_blank\" rel=\"noopener\">BiblioHouse</a> · Software Libre bajo licencia GPL v3</p>\n");
        html.append("</footer>\n");

        // JavaScript
        html.append("<script>\n");
        html.append(generarJavaScript());
        html.append("</script>\n");

        html.append("</body>\n</html>\n");
        return html.toString();
    }

    /**
     * Genera la tarjeta HTML de un libro individual.
     */
    private String generarTarjetaLibro(Libro libro) {
        StringBuilder card = new StringBuilder();
        String titulo = libro.getTitulo() != null ? libro.getTitulo() : "Sin título";
        String autor = libro.getAutor() != null ? libro.getAutor() : "Autor desconocido";
        String genero = libro.getGenero() != null ? libro.getGenero() : "";
        String editorial = libro.getEditorial() != null ? libro.getEditorial() : "";
        String anio = libro.getAño() != null ? libro.getAño() : "";
        String serie = libro.getSerie() != null ? libro.getSerie() : "";
        String estado = libro.getEstadoLectura() != null ? libro.getEstadoLectura() : "Pendiente";
        int calificacion = libro.getCalificacion();

        // Portada Base64
        String imgSrc = obtenerPortadaBase64(libro);

        card.append("      <div class=\"book-card\" ");
        card.append("data-title=\"").append(escapeHtml(titulo.toLowerCase())).append("\" ");
        card.append("data-author=\"").append(escapeHtml(autor.toLowerCase())).append("\" ");
        card.append("data-genre=\"").append(escapeHtml(genero.toLowerCase())).append("\">\n");

        card.append("        <div class=\"card-inner\">\n");
        card.append("          <img src=\"").append(imgSrc).append("\" alt=\"")
                .append(escapeHtml(titulo)).append("\" loading=\"lazy\">\n");

        // Overlay con título
        card.append("          <div class=\"card-overlay\">\n");
        card.append("            <span class=\"card-title\">").append(escapeHtml(titulo)).append("</span>\n");
        card.append("          </div>\n");

        // Badge de estado
        String badgeClass = "Leído".equals(estado) ? "badge-read" : "Leyendo".equals(estado) ? "badge-reading" : "badge-pending";
        card.append("          <span class=\"badge ").append(badgeClass).append("\">").append(escapeHtml(estado)).append("</span>\n");

        // Info panel (visible on hover)
        card.append("          <div class=\"card-info\">\n");
        card.append("            <h3>").append(escapeHtml(titulo)).append("</h3>\n");
        card.append("            <p class=\"info-author\">").append(escapeHtml(autor)).append("</p>\n");
        if (!editorial.isEmpty()) {
            card.append("            <p class=\"info-detail\">📖 ").append(escapeHtml(editorial)).append("</p>\n");
        }
        if (!anio.isEmpty()) {
            card.append("            <p class=\"info-detail\">📅 ").append(escapeHtml(anio)).append("</p>\n");
        }
        if (!genero.isEmpty()) {
            card.append("            <p class=\"info-detail\">🏷️ ").append(escapeHtml(genero)).append("</p>\n");
        }
        if (!serie.isEmpty()) {
            String ordenStr = libro.getOrdenEnSerie() > 0 ? " #" + formatOrden(libro.getOrdenEnSerie()) : "";
            card.append("            <p class=\"info-detail\">📚 ").append(escapeHtml(serie)).append(escapeHtml(ordenStr)).append("</p>\n");
        }
        if (calificacion > 0) {
            card.append("            <p class=\"info-stars\">");
            for (int i = 0; i < 5; i++) {
                card.append(i < calificacion ? "★" : "☆");
            }
            card.append("</p>\n");
        }
        card.append("          </div>\n");

        card.append("        </div>\n");
        card.append("      </div>\n");
        return card.toString();
    }

    /**
     * Formatea el número de orden en serie (1.0 → "1", 1.5 → "1.5").
     */
    private String formatOrden(double orden) {
        if (orden == Math.floor(orden)) {
            return String.valueOf((int) orden);
        }
        return String.valueOf(orden);
    }

    /**
     * Obtiene la portada del libro como una cadena Base64 data URI.
     * Si no se puede leer la portada, devuelve un placeholder SVG.
     */
    private String obtenerPortadaBase64(Libro libro) {
        String url = libro.getPortadaURL();
        if (url == null || url.isEmpty() || url.contains("default_cover")) {
            return generarPlaceholderSVG(libro.getTitulo(), libro.getAutor());
        }

        File archivo = new File(url);
        if (!archivo.exists() || !archivo.isFile()) {
            return generarPlaceholderSVG(libro.getTitulo(), libro.getAutor());
        }

        try {
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                return generarPlaceholderSVG(libro.getTitulo(), libro.getAutor());
            }

            // Redimensionar manteniendo aspecto
            int newWidth = THUMBNAIL_WIDTH;
            int newHeight = (int) ((double) original.getHeight() / original.getWidth() * newWidth);

            BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            // Comprimir a JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = jpegWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            jpegWriter.setOutput(new MemoryCacheImageOutputStream(baos));
            jpegWriter.write(null, new IIOImage(thumbnail, null, null), param);
            jpegWriter.dispose();

            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/jpeg;base64," + base64;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar la portada: {0}", archivo.getName());
            return generarPlaceholderSVG(libro.getTitulo(), libro.getAutor());
        }
    }

    /**
     * Genera un placeholder SVG elegante incrustado como data URI para libros
     * sin portada.
     */
    private String generarPlaceholderSVG(String titulo, String autor) {
        String initial = (titulo != null && !titulo.isEmpty())
                ? titulo.substring(0, 1).toUpperCase() : "?";
        String tituloCorto = (titulo != null && titulo.length() > 20)
                ? titulo.substring(0, 20) + "…" : (titulo != null ? titulo : "");
        String autorCorto = (autor != null && autor.length() > 18)
                ? autor.substring(0, 18) + "…" : (autor != null ? autor : "");

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='300' viewBox='0 0 200 300'>"
                + "<defs><linearGradient id='g' x1='0%' y1='0%' x2='100%' y2='100%'>"
                + "<stop offset='0%' style='stop-color:#667eea'/>"
                + "<stop offset='100%' style='stop-color:#764ba2'/>"
                + "</linearGradient></defs>"
                + "<rect width='200' height='300' fill='url(#g)' rx='4'/>"
                + "<text x='100' y='120' font-family='system-ui,sans-serif' font-size='72' fill='rgba(255,255,255,0.3)' text-anchor='middle'>"
                + escapeHtml(initial) + "</text>"
                + "<text x='100' y='200' font-family='system-ui,sans-serif' font-size='13' fill='white' text-anchor='middle' font-weight='bold'>"
                + escapeXml(tituloCorto) + "</text>"
                + "<text x='100' y='225' font-family='system-ui,sans-serif' font-size='11' fill='rgba(255,255,255,0.7)' text-anchor='middle'>"
                + escapeXml(autorCorto) + "</text>"
                + "<text x='100' y='270' font-family='system-ui,sans-serif' font-size='24' fill='rgba(255,255,255,0.2)' text-anchor='middle'>📖</text>"
                + "</svg>";

        String base64 = Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    /**
     * Agrupa los libros según el modo seleccionado.
     */
    private Map<String, List<Libro>> agruparLibros(List<Libro> libros, ModoAgrupacion modo) {
        Map<String, List<Libro>> mapa = new LinkedHashMap<>();

        for (Libro libro : libros) {
            List<String> claves = new ArrayList<>();

            if (modo == ModoAgrupacion.ESTANTERIA) {
                List<String> estanterias = libro.getEstanterias();
                if (estanterias == null || estanterias.isEmpty()) {
                    claves.add("");
                } else {
                    claves.addAll(estanterias);
                }
            } else {
                String genero = libro.getGenero();
                claves.add(genero != null && !genero.isEmpty() ? genero : "");
            }

            for (String clave : claves) {
                mapa.computeIfAbsent(clave, k -> new ArrayList<>()).add(libro);
            }
        }

        // Ordenar por nombre de categoría, poniendo "Sin clasificar" al final
        return mapa.entrySet().stream()
                .sorted((a, b) -> {
                    if (a.getKey().isEmpty()) return 1;
                    if (b.getKey().isEmpty()) return -1;
                    return a.getKey().compareToIgnoreCase(b.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    /**
     * Genera el CSS del catálogo con diseño oscuro tipo Netflix.
     */
    private String generarCSS() {
        return """
            :root {
              --bg-primary: #0a0a0f;
              --bg-secondary: #12121a;
              --bg-card: #1a1a2e;
              --text-primary: #e8e8f0;
              --text-secondary: #9898b0;
              --accent: #e50914;
              --accent-glow: rgba(229, 9, 20, 0.4);
              --gold: #f5c518;
              --gradient-start: #667eea;
              --gradient-end: #764ba2;
              --card-radius: 8px;
              --transition: 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
            }

            * { margin: 0; padding: 0; box-sizing: border-box; }

            body {
              font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
              background: var(--bg-primary);
              color: var(--text-primary);
              line-height: 1.6;
              overflow-x: hidden;
            }

            /* === HEADER / HERO === */
            .hero {
              background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
              padding: 60px 30px 50px;
              text-align: center;
              position: relative;
              overflow: hidden;
            }
            .hero::before {
              content: '';
              position: absolute;
              top: -50%;
              left: -50%;
              width: 200%;
              height: 200%;
              background: radial-gradient(circle, rgba(229,9,20,0.06) 0%, transparent 60%);
              animation: pulseGlow 8s ease-in-out infinite;
            }
            @keyframes pulseGlow {
              0%, 100% { opacity: 0.3; transform: scale(1); }
              50% { opacity: 0.7; transform: scale(1.1); }
            }
            .hero-content { position: relative; z-index: 1; }
            .hero h1 {
              font-size: clamp(1.8rem, 4vw, 3rem);
              font-weight: 800;
              margin-bottom: 8px;
              letter-spacing: -0.5px;
              background: linear-gradient(135deg, #fff 0%, #c8c8e0 100%);
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
              background-clip: text;
            }
            .hero-subtitle {
              color: var(--text-secondary);
              font-size: 0.95rem;
              margin-bottom: 30px;
            }
            .stats {
              display: flex;
              justify-content: center;
              gap: 40px;
              flex-wrap: wrap;
            }
            .stat {
              display: flex;
              flex-direction: column;
              align-items: center;
            }
            .stat-num {
              font-size: 2rem;
              font-weight: 700;
              background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
              background-clip: text;
            }
            .stat-label {
              font-size: 0.8rem;
              color: var(--text-secondary);
              text-transform: uppercase;
              letter-spacing: 1.5px;
              margin-top: 2px;
            }

            /* === SEARCH === */
            .search-container {
              position: sticky;
              top: 0;
              z-index: 100;
              background: rgba(10, 10, 15, 0.92);
              backdrop-filter: blur(20px);
              -webkit-backdrop-filter: blur(20px);
              padding: 16px 30px;
              border-bottom: 1px solid rgba(255,255,255,0.06);
            }
            #searchInput {
              width: 100%;
              max-width: 600px;
              display: block;
              margin: 0 auto;
              padding: 14px 24px;
              font-size: 1rem;
              background: var(--bg-secondary);
              border: 1px solid rgba(255,255,255,0.08);
              border-radius: 50px;
              color: var(--text-primary);
              outline: none;
              transition: var(--transition);
            }
            #searchInput:focus {
              border-color: var(--gradient-start);
              box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
            }
            #searchInput::placeholder { color: var(--text-secondary); }
            .search-count {
              text-align: center;
              color: var(--text-secondary);
              font-size: 0.82rem;
              margin-top: 8px;
              min-height: 1.2em;
            }

            /* === CATEGORIES === */
            main { padding: 20px 0 40px; }
            .category {
              padding: 10px 30px 30px;
              transition: opacity var(--transition);
            }
            .category-title {
              font-size: 1.4rem;
              font-weight: 700;
              margin-bottom: 16px;
              padding-left: 4px;
              border-left: 4px solid var(--accent);
              padding-left: 14px;
            }
            .category-count {
              font-weight: 400;
              color: var(--text-secondary);
              font-size: 0.9rem;
            }

            /* === HORIZONTAL ROW === */
            .row-wrapper {
              position: relative;
            }
            .books-row {
              display: flex;
              gap: 18px;
              overflow-x: auto;
              padding: 10px 4px 20px;
              scroll-behavior: smooth;
              scrollbar-width: thin;
              scrollbar-color: rgba(255,255,255,0.15) transparent;
            }
            .books-row::-webkit-scrollbar { height: 6px; }
            .books-row::-webkit-scrollbar-track { background: transparent; }
            .books-row::-webkit-scrollbar-thumb {
              background: rgba(255,255,255,0.15);
              border-radius: 3px;
            }

            /* === BOOK CARDS === */
            .book-card {
              flex: 0 0 auto;
              width: 160px;
              transition: var(--transition);
            }
            .card-inner {
              position: relative;
              border-radius: var(--card-radius);
              overflow: hidden;
              background: var(--bg-card);
              box-shadow: 0 4px 15px rgba(0,0,0,0.4);
              transition: var(--transition);
              cursor: pointer;
            }
            .card-inner:hover {
              transform: scale(1.08);
              box-shadow: 0 12px 40px rgba(0,0,0,0.6), 0 0 20px var(--accent-glow);
              z-index: 10;
            }
            .card-inner img {
              width: 100%;
              height: 240px;
              object-fit: cover;
              display: block;
              transition: var(--transition);
            }

            /* Card overlay (title at bottom) */
            .card-overlay {
              position: absolute;
              bottom: 0;
              left: 0;
              right: 0;
              padding: 30px 10px 10px;
              background: linear-gradient(transparent, rgba(0,0,0,0.85));
              pointer-events: none;
            }
            .card-title {
              font-size: 0.8rem;
              font-weight: 600;
              line-height: 1.3;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
              overflow: hidden;
            }

            /* Badge */
            .badge {
              position: absolute;
              top: 8px;
              right: 8px;
              padding: 2px 8px;
              border-radius: 4px;
              font-size: 0.65rem;
              font-weight: 700;
              text-transform: uppercase;
              letter-spacing: 0.5px;
            }
            .badge-read { background: #27ae60; color: white; }
            .badge-reading { background: var(--gradient-start); color: white; }
            .badge-pending { background: rgba(255,255,255,0.15); color: var(--text-secondary); }

            /* Info panel (hover) */
            .card-info {
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              bottom: 0;
              background: rgba(10, 10, 15, 0.93);
              padding: 16px 12px;
              display: flex;
              flex-direction: column;
              justify-content: center;
              opacity: 0;
              transition: opacity 0.25s ease;
              pointer-events: none;
            }
            .card-inner:hover .card-info {
              opacity: 1;
            }
            .card-info h3 {
              font-size: 0.85rem;
              font-weight: 700;
              margin-bottom: 6px;
              line-height: 1.3;
              display: -webkit-box;
              -webkit-line-clamp: 3;
              -webkit-box-orient: vertical;
              overflow: hidden;
            }
            .info-author {
              font-size: 0.78rem;
              color: var(--gradient-start);
              margin-bottom: 10px;
              font-weight: 500;
            }
            .info-detail {
              font-size: 0.72rem;
              color: var(--text-secondary);
              margin-bottom: 3px;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
            .info-stars {
              color: var(--gold);
              font-size: 0.9rem;
              margin-top: 8px;
              letter-spacing: 2px;
            }

            /* Hide card when filtered out */
            .book-card.hidden { display: none; }
            .category.hidden { display: none; }

            /* === NO RESULTS === */
            .no-results {
              text-align: center;
              padding: 60px 30px;
              color: var(--text-secondary);
              font-size: 1.2rem;
            }

            /* === FOOTER === */
            footer {
              text-align: center;
              padding: 40px 30px;
              color: var(--text-secondary);
              font-size: 0.82rem;
              border-top: 1px solid rgba(255,255,255,0.06);
            }
            footer a {
              color: var(--gradient-start);
              text-decoration: none;
              font-weight: 600;
            }
            footer a:hover { text-decoration: underline; }

            /* === RESPONSIVE === */
            @media (max-width: 768px) {
              .hero { padding: 40px 20px 35px; }
              .stats { gap: 24px; }
              .stat-num { font-size: 1.5rem; }
              .category { padding: 10px 16px 24px; }
              .book-card { width: 130px; }
              .card-inner img { height: 195px; }
              .search-container { padding: 12px 16px; }
              #searchInput { padding: 12px 20px; font-size: 0.9rem; }
            }
            @media (max-width: 480px) {
              .hero h1 { font-size: 1.5rem; }
              .book-card { width: 110px; }
              .card-inner img { height: 165px; }
              .card-title { font-size: 0.7rem; }
              .books-row { gap: 10px; }
            }
            """;
    }

    /**
     * Genera el JavaScript del buscador/filtro.
     */
    private String generarJavaScript() {
        return """
            (function() {
              const input = document.getElementById('searchInput');
              const catalog = document.getElementById('catalog');
              const noResults = document.getElementById('noResults');
              const countEl = document.getElementById('searchCount');
              const cards = catalog.querySelectorAll('.book-card');
              const sections = catalog.querySelectorAll('.category');

              let debounceTimer;

              input.addEventListener('input', function() {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(filterBooks, 150);
              });

              function filterBooks() {
                const query = input.value.trim().toLowerCase();
                let visibleCount = 0;

                cards.forEach(function(card) {
                  if (!query) {
                    card.classList.remove('hidden');
                    visibleCount++;
                    return;
                  }
                  const title = card.getAttribute('data-title') || '';
                  const author = card.getAttribute('data-author') || '';
                  const genre = card.getAttribute('data-genre') || '';
                  const match = title.includes(query) || author.includes(query) || genre.includes(query);
                  card.classList.toggle('hidden', !match);
                  if (match) visibleCount++;
                });

                // Hide empty sections
                sections.forEach(function(section) {
                  const visibleCards = section.querySelectorAll('.book-card:not(.hidden)');
                  section.classList.toggle('hidden', visibleCards.length === 0);
                });

                noResults.style.display = visibleCount === 0 ? 'block' : 'none';

                if (query) {
                  countEl.textContent = visibleCount + ' libro' + (visibleCount !== 1 ? 's' : '') + ' encontrado' + (visibleCount !== 1 ? 's' : '');
                } else {
                  countEl.textContent = '';
                }
              }
            })();
            """;
    }

    /**
     * Escapa caracteres especiales para HTML.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Escapa caracteres especiales para XML/SVG (incluye apóstrofe).
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
