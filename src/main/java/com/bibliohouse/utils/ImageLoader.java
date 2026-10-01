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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

/**
 * Clase para cargar imágenes de forma asíncrona con caché multinivel.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class ImageLoader {

    private static String cacheDir = null;
    private static final ExecutorService executor = Executors.newFixedThreadPool(8); // Pool reducido para no saturar IO
    private static final String DEFAULT_IMAGE_PATH = "/resources/default_cover.jpg";
    private static final int MAX_CACHE_SIZE = 150; // Aumentado para evitar evictions al scrollear bibliotecas grandes

    public static final int MAX_COVER_WIDTH = 600;
    public static final int MAX_COVER_HEIGHT = 900;
    public static final float JPEG_COMPRESSION_QUALITY = 0.85f;

    // Caché en memoria (LRU) — envuelta en synchronizedMap para acceso seguro
    // desde el hilo FX y el ExecutorService simultáneamente.
    private static final Map<String, Image> memoryCache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    }
    );

    /**
     * Carga la imagen por defecto en el ImageView especificado, escalándola al
     * tamaño indicado. Utiliza una caché en memoria para evitar recargar la
     * misma imagen con las mismas dimensiones.
     *
     * @param target ImageView donde se cargará la imagen.
     * @param w Ancho deseado para la imagen
     */
    private static void loadDefault(ImageView target, double w, double h) {
        try {
            URL defaultUrl = ImageLoader.class.getResource(DEFAULT_IMAGE_PATH);
            if (defaultUrl != null) {
                String uri = defaultUrl.toExternalForm();
                String memoryKey = "DEFAULT_" + w + "x" + h;
                if (memoryCache.containsKey(memoryKey)) {
                    target.setImage(memoryCache.get(memoryKey));
                    return;
                }
                Image img = new Image(uri, w > 0 ? w : 0, h > 0 ? h : 0, true, true, false);
                memoryCache.put(memoryKey, img);
                target.setImage(img);
            } else {
                target.setImage(null);
            }
        } catch (Exception e) {
            target.setImage(null);
        }
    }

    /**
     * Establece el directorio donde se almacenarán las imágenes en caché. Si el
     * directorio no existe, lo crea automáticamente.
     *
     * @param path Ruta del directorio donde se guardará la caché.
     */
    public static void setCacheDir(String path) {
        cacheDir = path;
        File dir = new File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Pre-carga la imagen por defecto en la caché de memoria. Llamado durante
     * el splash screen para que la primera vista de libros no tenga que cargar
     * la imagen desde disco.
     */
    public static void preloadDefaultCover() {
        try {
            URL defaultUrl = ImageLoader.class.getResource(DEFAULT_IMAGE_PATH);
            if (defaultUrl != null) {
                String uri = defaultUrl.toExternalForm();
                // Pre-cargar en los tamaños más comunes (tarjetas y edición)
                for (String key : new String[]{"DEFAULT_150.0x220.0", "DEFAULT_300.0x450.0"}) {
                    if (!memoryCache.containsKey(key)) {
                        double w = key.contains("150") ? 150 : 300;
                        double h = key.contains("220") ? 220 : 450;
                        Image img = new Image(uri, w, h, true, true, false);
                        memoryCache.put(key, img);
                    }
                }
            }
        } catch (Exception ignored) {
            // No crítico: se cargará bajo demanda la primera vez que se necesite
        }
    }

    private static final java.net.http.HttpClient HTTP_CLIENT = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    /**
     * Extrae el nombre del archivo de forma agnóstica al sistema operativo,
     * soportando tanto separadores Unix (/) como Windows (\).
     *
     * @param ruta Ruta completa o parcial del archivo.
     * @return Nombre del archivo sin directorios, o cadena vacía si es nulo.
     */
    public static String extraerNombreArchivo(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "";
        }
        int lastSlash = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        return (lastSlash >= 0 && lastSlash < ruta.length() - 1) ? ruta.substring(lastSlash + 1) : ruta;
    }

    /**
     * Resuelve un archivo local buscando tanto en la ruta proporcionada como en
     * el directorio de caché de portadas (covers) con múltiples extensiones.
     *
     * @param path Ruta del archivo a buscar.
     * @return El archivo File existente o null si no se encuentra.
     */
    public static File resolverArchivoLocal(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            return f;
        }
        if (cacheDir != null) {
            String cleanName = extraerNombreArchivo(path);
            if (!cleanName.isBlank()) {
                File enCache = new File(cacheDir, cleanName);
                if (enCache.exists() && enCache.isFile()) {
                    return enCache;
                }
                String baseName = cleanName.contains(".") ? cleanName.substring(0, cleanName.lastIndexOf('.')) : cleanName;
                for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp"}) {
                    File alternativa = new File(cacheDir, baseName + ext);
                    if (alternativa.exists() && alternativa.isFile()) {
                        return alternativa;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Descarga de forma segura y fiable una imagen web a un archivo destino
     * utilizando HttpClient con redirección automática y User-Agent de navegador.
     *
     * @param url URL de la imagen.
     * @param archivoDestino Archivo local donde se guardará.
     * @return true si la descarga fue exitosa y el archivo es válido.
     */
    public static boolean descargarImagenWeb(String url, File archivoDestino) {
        if (!isValidUrl(url) || archivoDestino == null) {
            return false;
        }
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 BiblioHouse/2.1")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .GET()
                    .build();

            java.net.http.HttpResponse<byte[]> response = HTTP_CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] bytes = response.body();
                // Validar que no sea un píxel transparente vacío (< 200 bytes) ni cuerpo corrupto
                if (bytes != null && bytes.length > 200) {
                    File parent = archivoDestino.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    File tmp = new File(archivoDestino.getAbsolutePath() + ".tmp");
                    Files.write(tmp.toPath(), bytes);
                    try {
                        Files.move(tmp.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        Files.move(tmp.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Carga una imagen de forma asíncrona en un ImageView, aplicando skeleton
     * loading mientras se procesa. Prioriza el uso de la caché en memoria para
     * evitar recargas innecesarias.
     *
     * @param urlOrPath Ruta local o URL de la imagen a cargar.
     * @param target ImageView donde se mostrará la imagen.
     * @param w Ancho deseado para la imagen
     */
    public static void load(String urlOrPath, ImageView target, double w, double h) {
        if (target == null) {
            return;
        }

        stopSkeleton(target);

        if (urlOrPath == null || urlOrPath.isEmpty() || urlOrPath.contains("default_cover")) {
            loadDefault(target, w, h);
            return;
        }

        // Tamaño de miniatura para la caché si no se especifica
        double loadW = (w > 0) ? w : 110;
        double loadH = (h > 0) ? h : 160;
        String memoryKey = urlOrPath + "_" + loadW + "x" + loadH;

        // 1. Respuesta instantánea desde caché en memoria
        if (memoryCache.containsKey(memoryKey)) {
            target.setImage(memoryCache.get(memoryKey));
            return;
        }

        // 2. Iniciar skeleton loading mientras se procesa
        startSkeleton(target, loadW, loadH);

        // 3. Determinar si es una imagen web o local
        if (isValidUrl(urlOrPath)) {
            handleWebImage(urlOrPath, target, loadW, loadH, memoryKey);
        } else {
            handleLocalImage(urlOrPath, target, loadW, loadH, memoryKey);
        }
    }

    /**
     * Gestiona la carga de una imagen desde una URL remota. Primero verifica si
     * la imagen está en caché local (disco).
     *
     * @param url URL de la imagen remota.
     * @param target ImageView donde se mostrará la imagen.
     * @param w Ancho deseado para la imagen.
     * @param h Alto deseado para la imagen.
     * @param memoryKey Clave única para identificar la imagen en la caché en
     * memoria.
     */
    private static void handleWebImage(String url, ImageView target, double w, double h, String memoryKey) {
        if (cacheDir == null) {
            loadImageAsync(url, target, w, h, memoryKey);
            return;
        }

        String filename = hashUrl(url);
        File cacheFile = new File(cacheDir, filename);

        if (cacheFile.exists() && cacheFile.length() > 200) {
            handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
        } else {
            // Descargar en segundo plano con cliente HTTP robusto
            target.getProperties().put("target_image_key", memoryKey);
            Task<Boolean> downloadTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return descargarImagenWeb(url, cacheFile);
                }

                @Override
                protected void succeeded() {
                    if (Boolean.TRUE.equals(getValue()) && cacheFile.exists()) {
                        handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
                    } else {
                        stopSkeleton(target);
                        loadDefault(target, w, h);
                    }
                }

                @Override
                protected void failed() {
                    stopSkeleton(target);
                    loadDefault(target, w, h);
                }
            };
            executor.submit(downloadTask);
        }
    }

    /**
     * Gestiona la carga de una imagen desde una ruta local. Si el archivo no
     * existe en la ruta dada ni en la carpeta de portadas, carga la imagen por defecto.
     *
     * @param path Ruta local del archivo de imagen.
     * @param target ImageView donde se mostrará la imagen.
     * @param w Ancho deseado para la imagen.
     * @param h Alto deseado para la imagen.
     * @param memoryKey Clave única para identificar la imagen en la caché en
     * memoria.
     */
    private static void handleLocalImage(String path, ImageView target, double w, double h, String memoryKey) {
        File file = resolverArchivoLocal(path);
        if (file == null || !file.exists() || !file.isFile()) {
            stopSkeleton(target);
            loadDefault(target, w, h);
            return;
        }
        loadImageAsync(file.toURI().toString(), target, w, h, memoryKey);
    }

    /**
     * Carga una imagen de forma asíncrona usando el motor nativo de JavaFX, sin
     * bloquear el hilo principal de la aplicación.
     *
     * @param uri URI o ruta de la imagen a cargar.
     * @param target ImageView donde se mostrará la imagen.
     * @param w Ancho deseado para la imagen.
     * @param h Alto deseado para la imagen.
     * @param memoryKey Clave única para almacenar la imagen en la caché en
     * memoria.
     */
    private static void loadImageAsync(String uri, ImageView target, double w, double h, String memoryKey) {
        target.getProperties().put("target_image_key", memoryKey);
        Image image = new Image(uri, w, h, true, true, true);

        // Caso rápido: la imagen ya estaba lista al crearse (caché del SO)
        if (image.getProgress() >= 1.0 && !image.isError()) {
            stopSkeleton(target);
            memoryCache.put(memoryKey, image);
            target.setImage(image);
            return;
        }
        if (image.isError()) {
            stopSkeleton(target);
            loadDefault(target, w, h);
            return;
        }

        // Caso asíncrono: registrar listener solo si aún no está completa
        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                javafx.application.Platform.runLater(() -> {
                    if (memoryKey.equals(target.getProperties().get("target_image_key"))) {
                        if (!image.isError()) {
                            stopSkeleton(target);
                            memoryCache.put(memoryKey, image);
                            target.setImage(image);
                        } else {
                            stopSkeleton(target);
                            loadDefault(target, w, h);
                        }
                    }
                });
            }
        });
    }

    /**
     * Verifica si una cadena es una URL válida (http o https).
     *
     * @param url Cadena a verificar.
     * @return true si la cadena no es nula y comienza con "http://" o
     * "https://", false en caso contrario.
     */
    private static boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * Genera un hash SHA-256 de una URL y lo devuelve como una cadena
     * hexadecimal, añadiendo la extensión ".png" al final.
     *
     * @param url La URL de la que se generará el hash.
     * @return Cadena hexadecimal del hash SHA-256 de la URL, con extensión
     * ".png".
     */
    private static String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString() + ".png";
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(url.hashCode()) + ".png";
        }
    }

    /**
     * Guarda una portada de libro localmente en la carpeta de usuario, ya sea
     * descargándola desde una URL o copiándola desde una ruta local.
     *
     * @param urlOrPath URL o ruta local de la portada original.
     * @param idLibro Identificador único del libro, usado como nombre de
     * archivo.
     * @param carpetaUsuario Ruta de la carpeta del usuario donde se guardará la
     * portada.
     * @return Ruta absoluta del archivo de portada guardado localmente, o la
     * ruta original si no se pudo procesar.
     */
    /**
     * Guarda una portada de libro localmente en la carpeta de usuario, ya sea
     * descargándola desde una URL o copiándola desde una ruta local.
     * Redimensiona automáticamente la imagen a un estándar de biblioteca
     * (máximo 600 px de ancho y 900 px de alto, manteniendo el ratio de aspecto)
     * y la guarda comprimida en JPEG con calidad al 85%.
     *
     * @param urlOrPath URL o ruta local de la portada original.
     * @param idLibro Identificador único del libro, usado como nombre de archivo.
     * @param carpetaUsuario Ruta de la carpeta del usuario donde se guardará la portada.
     * @return Ruta absoluta del archivo de portada guardado localmente, o la ruta original si no se pudo procesar.
     */
    public static String hacerPortadaLocalOffline(String urlOrPath, String idLibro, String carpetaUsuario) {
        if (urlOrPath == null || urlOrPath.isEmpty() || urlOrPath.equals(DEFAULT_IMAGE_PATH) || urlOrPath.contains("default_cover")) {
            return urlOrPath;
        }

        if (carpetaUsuario == null || carpetaUsuario.isBlank()) {
            return urlOrPath;
        }

        File dirCovers = new File(carpetaUsuario, "covers");
        if (!dirCovers.exists()) {
            dirCovers.mkdirs();
        }

        String safeId = (idLibro != null && !idLibro.isBlank()) ? idLibro : java.util.UUID.randomUUID().toString();
        File archivoDestino = new File(dirCovers, safeId + ".jpg");

        try {
            if (isValidUrl(urlOrPath)) {
                File tmpDescarga = new File(dirCovers, safeId + "_download.tmp");
                boolean descargado = descargarImagenWeb(urlOrPath, tmpDescarga);
                if (descargado && tmpDescarga.exists()) {
                    boolean optimizado = redimensionarYComprimirPortada(tmpDescarga, archivoDestino);
                    try {
                        Files.deleteIfExists(tmpDescarga.toPath());
                    } catch (Exception ignored) {}

                    if (optimizado && archivoDestino.exists() && archivoDestino.length() > 200) {
                        limpiarPortadasObsoletasConMismoId(dirCovers, safeId, ".jpg");
                        return archivoDestino.getAbsolutePath();
                    }
                }
                // Si la descarga u optimización falló pero ya existía un archivo válido con ese ID
                if (archivoDestino.exists() && archivoDestino.length() > 200) {
                    return archivoDestino.getAbsolutePath();
                }
                return urlOrPath;
            } else {
                File archivoOrigen = resolverArchivoLocal(urlOrPath);
                if (archivoOrigen != null && archivoOrigen.exists() && archivoOrigen.isFile()) {
                    // Si el archivo origen ya es exactamente el destino y ya está optimizado (< 120 KB), retornarlo directamente
                    try {
                        if (archivoOrigen.getCanonicalPath().equals(archivoDestino.getCanonicalPath()) && archivoOrigen.length() < 120_000) {
                            return archivoDestino.getAbsolutePath();
                        }
                    } catch (IOException ignored) {}

                    // OWASP A01: Validación de extensión para evitar Arbitrary File Read/Copy
                    String name = archivoOrigen.getName().toLowerCase();
                    if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".webp")) {
                        return urlOrPath;
                    }

                    boolean optimizado = redimensionarYComprimirPortada(archivoOrigen, archivoDestino);
                    if (!optimizado) {
                        // Fallback defensivo si ImageIO no pudo decodificar el formato específico
                        if (!archivoOrigen.getCanonicalPath().equals(archivoDestino.getCanonicalPath())) {
                            Files.copy(archivoOrigen.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                    if (archivoDestino.exists() && archivoDestino.length() > 200) {
                        limpiarPortadasObsoletasConMismoId(dirCovers, safeId, ".jpg");
                        return archivoDestino.getAbsolutePath();
                    }
                    return urlOrPath;
                } else {
                    // Verificar si ya existe en destino con cualquier extensión
                    for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp"}) {
                        File testFile = new File(dirCovers, safeId + ext);
                        if (testFile.exists() && testFile.isFile()) {
                            return testFile.getAbsolutePath();
                        }
                    }
                    return urlOrPath;
                }
            }
        } catch (IOException e) {
            return urlOrPath;
        }
    }

    /**
     * Redimensiona una imagen al tamaño estándar de portada (máximo 600px de ancho y 900px de alto,
     * conservando la relación de aspecto) y la guarda comprimida en formato JPEG al 85%.
     * Si la imagen original es menor a estas dimensiones, no se escala hacia arriba.
     *
     * @param imagenOriginal Imagen en memoria a redimensionar y comprimir.
     * @param archivoDestino Archivo de destino donde se guardará.
     * @return true si la imagen fue procesada y escrita exitosamente.
     */
    public static boolean redimensionarYComprimirPortada(BufferedImage imagenOriginal, File archivoDestino) {
        if (imagenOriginal == null || archivoDestino == null) {
            return false;
        }

        int origWidth = imagenOriginal.getWidth();
        int origHeight = imagenOriginal.getHeight();
        if (origWidth <= 0 || origHeight <= 0) {
            return false;
        }

        int targetWidth = origWidth;
        int targetHeight = origHeight;

        // Escalar manteniendo aspect ratio únicamente si excede las dimensiones máximas
        if (origWidth > MAX_COVER_WIDTH || origHeight > MAX_COVER_HEIGHT) {
            double escala = Math.min((double) MAX_COVER_WIDTH / origWidth, (double) MAX_COVER_HEIGHT / origHeight);
            targetWidth = Math.max(1, (int) Math.round(origWidth * escala));
            targetHeight = Math.max(1, (int) Math.round(origHeight * escala));
        }

        // Crear imagen RGB para evitar colores erróneos al convertir canales alfa de PNG a JPEG
        BufferedImage imagenOptimizada = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagenOptimizada.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo blanco para portadas con transparencia
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);

            g2d.drawImage(imagenOriginal, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        File parent = archivoDestino.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tmpFile = new File(archivoDestino.getAbsolutePath() + ".tmp");
        try {
            guardarJpegComprimido(imagenOptimizada, tmpFile, JPEG_COMPRESSION_QUALITY);

            try {
                Files.move(tmpFile.toPath(), archivoDestino.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tmpFile.toPath(), archivoDestino.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tmpFile.toPath());
            } catch (Exception ignored) {}
            return false;
        }
    }

    /**
     * Carga un archivo de imagen desde disco, lo redimensiona y lo comprime en formato JPEG al 85%.
     *
     * @param archivoOrigen Archivo de imagen original (JPEG, PNG, WebP, etc.).
     * @param archivoDestino Archivo final donde se guardará la portada optimizada.
     * @return true si la operación se completó exitosamente.
     */
    public static boolean redimensionarYComprimirPortada(File archivoOrigen, File archivoDestino) {
        if (archivoOrigen == null || !archivoOrigen.exists() || archivoDestino == null) {
            return false;
        }
        try {
            BufferedImage img = ImageIO.read(archivoOrigen);
            if (img != null) {
                return redimensionarYComprimirPortada(img, archivoDestino);
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Lee una imagen desde un flujo de entrada (InputStream), la redimensiona y la comprime en formato JPEG al 85%.
     *
     * @param inputStream Flujo con los bytes de la imagen.
     * @param archivoDestino Archivo final donde se guardará la portada optimizada.
     * @return true si la operación se completó exitosamente.
     */
    public static boolean redimensionarYComprimirPortada(InputStream inputStream, File archivoDestino) {
        if (inputStream == null || archivoDestino == null) {
            return false;
        }
        try {
            BufferedImage img = ImageIO.read(inputStream);
            if (img != null) {
                return redimensionarYComprimirPortada(img, archivoDestino);
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Escribe un BufferedImage en disco en formato JPEG con el factor de calidad indicado.
     *
     * @param imagen Imagen a persistir.
     * @param destino Archivo destino.
     * @param calidad Calidad entre 0.0f y 1.0f (ej. 0.85f).
     * @throws IOException Si ocurre un fallo de escritura en disco.
     */
    public static void guardarJpegComprimido(BufferedImage imagen, File destino, float calidad) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(imagen, "jpg", destino);
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(destino)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(calidad);
            }
            writer.write(null, new IIOImage(imagen, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Elimina archivos de portada anteriores con el mismo identificador pero diferente extensión
     * (por ejemplo, eliminando una versión previa .png de 8 MB al generar el nuevo .jpg de 50 KB).
     */
    private static void limpiarPortadasObsoletasConMismoId(File dirCovers, String safeId, String extensionActual) {
        for (String ext : new String[]{".png", ".jpeg", ".webp"}) {
            if (!ext.equalsIgnoreCase(extensionActual)) {
                File oldFile = new File(dirCovers, safeId + ext);
                if (oldFile.exists() && oldFile.isFile()) {
                    try {
                        Files.deleteIfExists(oldFile.toPath());
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Muestra un efecto de skeleton loading (animación de carga) en el
     * ImageView especificado.
     *
     * @param target ImageView donde se mostrará la animación de skeleton.
     * @param w Ancho deseado para el marcador de posición
     * @param h Alto deseado para el marcador de posición
     */
    private static void startSkeleton(ImageView target, double w, double h) {
        if (target.getProperties().containsKey("skeleton_anim")) {
            return;
        }

        int width = (w > 0) ? (int) w : 110;
        int height = (h > 0) ? (int) h : 160;

        // Crear placeholder gris con buffer (O(1) vs antiguo O(w*h) por pixel)
        WritableImage placeholder = new WritableImage(width, height);
        int[] buffer = new int[width * height];
        int gray = 0xFFEBEBEB; // ARGB for rgb(235,235,235)
        java.util.Arrays.fill(buffer, gray);
        placeholder.getPixelWriter().setPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), buffer, 0, width);
        target.setImage(placeholder);

        FadeTransition ft = new FadeTransition(Duration.millis(800), target);
        ft.setFromValue(0.3);
        ft.setToValue(0.8);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
        target.getProperties().put("skeleton_anim", ft);
    }

    /**
     * Detiene y elimina la animación de skeleton loading del ImageView
     * especificado, restaurando su opacidad al valor original (1.0). Si no hay
     * animación activa, no realiza ninguna acción.
     *
     * @param target ImageView del que se detendrá la animación de skeleton.
     */
    private static void stopSkeleton(ImageView target) {
        if (target.getProperties().containsKey("skeleton_anim")) {
            FadeTransition ft = (FadeTransition) target.getProperties().get("skeleton_anim");
            ft.stop();
            target.getProperties().remove("skeleton_anim");
        }
        target.setOpacity(1.0);
    }

    /**
     * Apaga el ejecutor de hilos utilizado para las tareas asíncronas de carga
     * de imágenes. Si el ejecutor ya está apagado, no realiza ninguna acción.
     * Este método debe llamarse al cerrar la aplicación para liberar recursos.
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
