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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Clase para cargar imágenes de forma asíncrona con caché multinivel.
 *
 * * @author Fernando Lago
 * @version 1.7
 */
public class ImageLoader {

    private static String cacheDir = null;
    private static final ExecutorService executor = Executors.newFixedThreadPool(8); // Pool reducido para no saturar IO
    private static final String DEFAULT_IMAGE_PATH = "/resources/default_cover.jpg";
    private static final int MAX_CACHE_SIZE = 60; // Optimizado para fluidez sin devorar RAM

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

        if (cacheFile.exists()) {
            handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
        } else {
            // Descargar en segundo plano y luego cargar
            Task<Void> downloadTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    java.net.URLConnection conn = new URL(url).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
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
     * existe, detiene el skeleton loading y carga la imagen por defecto.
     *
     * @param path Ruta local del archivo de imagen.
     * @param target ImageView donde se mostrará la imagen.
     * @param w Ancho deseado para la imagen.
     * @param h Alto deseado para la imagen.
     * @param memoryKey Clave única para identificar la imagen en la caché en
     * memoria.
     */
    private static void handleLocalImage(String path, ImageView target, double w, double h, String memoryKey) {
        File file = new File(path);
        if (!file.exists()) {
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
        // backgroundLoading (último parámetro) = true
        Image image = new Image(uri, w, h, true, true, true);

        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                javafx.application.Platform.runLater(() -> {
                    if (!image.isError()) {
                        stopSkeleton(target);
                        memoryCache.put(memoryKey, image);
                        target.setImage(image);
                    } else {
                        stopSkeleton(target);
                        loadDefault(target, w, h);
                    }
                });
            }
        });

        // Caso especial: la imagen ya estaba lista al crearse
        if (image.getProgress() >= 1.0 && !image.isError()) {
            stopSkeleton(target);
            memoryCache.put(memoryKey, image);
            target.setImage(image);
        }
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
    public static String hacerPortadaLocalOffline(String urlOrPath, String idLibro, String carpetaUsuario) {
        if (urlOrPath == null || urlOrPath.isEmpty() || urlOrPath.equals(DEFAULT_IMAGE_PATH)) {
            return urlOrPath;
        }

        File dirCovers = new File(carpetaUsuario, "covers");
        if (!dirCovers.exists()) {
            dirCovers.mkdirs();
        }

        String extension = urlOrPath.toLowerCase().endsWith(".png") ? ".png" : ".jpg";
        File archivoDestino = new File(dirCovers, idLibro + extension);

        try {
            if (isValidUrl(urlOrPath)) {
                java.net.URLConnection conn = new URL(urlOrPath).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                File archivoOrigen = new File(urlOrPath);
                if (archivoOrigen.exists()) {
                    // OWASP A01: Validación de extensión para evitar Arbitrary File Read/Copy
                    String name = archivoOrigen.getName().toLowerCase();
                    if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".webp")) {
                        return urlOrPath; // Rechazar archivos que no sean imágenes
                    }
                    if (!archivoOrigen.getCanonicalPath().equals(archivoDestino.getCanonicalPath())) {
                        Files.copy(archivoOrigen.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    return urlOrPath;
                }
            }
            return archivoDestino.getAbsolutePath();
        } catch (IOException e) {
            return urlOrPath;
        }
    }

    /**
     * Muestra un efecto de skeleton loading (animación de carga) en el
     * ImageView especificado.
     *
     * @param target ImageView donde se mostrará la animación de skeleton.
     * @param w Ancho deseado para el marcador de posición (si es <= 0, usa
     * 110px). @param h Alto deseado para el marcador de posición (si es <= 0,
     * usa 160px).
     */
    private static void startSkeleton(ImageView target, double w, double h) {
        if (target.getProperties().containsKey("skeleton_anim")) {
            return;
        }

        int width = (w > 0) ? (int) w : 110;
        int height = (h > 0) ? (int) h : 160;

        WritableImage placeholder = new WritableImage(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                placeholder.getPixelWriter().setColor(x, y, Color.rgb(235, 235, 235));
            }
        }
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
