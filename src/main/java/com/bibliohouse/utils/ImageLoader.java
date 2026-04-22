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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Clase para cargar imágenes sin que se trabe la app. Guarda las fotos en una
 * carpeta para no descargarlas dos veces.
 *
 * @author Fernando Lago
 * @version 1.6
 */
public class ImageLoader {

    private static final Logger LOGGER = Logger.getLogger(ImageLoader.class.getName());

    // Directorio donde se guardarán las imágenes descargadas.
    private static String cacheDir = null;
    // Executor para descargas en segundo plano.
    private static final ExecutorService executor = Executors.newFixedThreadPool(16);
    // Ruta de la imagen por defecto
    private static final String DEFAULT_IMAGE_PATH = "/resources/default_cover.jpg";
    private static final int MAX_CACHE_SIZE = 100; // Mantendrá las últimas 100 portadas usadas en memoria

    // Caché en memoria (RAM) para acceso ultrarrápido durante la sesión.
    private static final Map<String, Image> memoryCache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /**
     * Esta función carga la imagen por defecto (esa gris con el logo) cuando un
     * libro no tiene portada o cuando la url está mal. Intento cargarla de la
     * memoria para que vaya rápido.
     */
    private static void loadDefault(ImageView target, double w, double h) {
        try {
            // Intentar cargar desde recursos
            URL defaultUrl = ImageLoader.class.getResource(DEFAULT_IMAGE_PATH);
            if (defaultUrl != null) {
                String uri = defaultUrl.toExternalForm();
                // Usar caché de memoria para la imagen por defecto también
                String memoryKey = "DEFAULT_" + w + "x" + h;
                if (memoryCache.containsKey(memoryKey)) {
                    target.setImage(memoryCache.get(memoryKey));
                    return;
                }

                double loadW = (w > 0) ? w : 0;
                double loadH = (h > 0) ? h : 0;
                Image img = new Image(uri, loadW, loadH, true, true, false);
                memoryCache.put(memoryKey, img);
                target.setImage(img);
            } else {
                LOGGER.warning("No se encontró la imagen por defecto en: " + DEFAULT_IMAGE_PATH);
                target.setImage(null);
            }
        } catch (Exception e) {
            LOGGER.severe("Error cargando imagen por defecto: " + e.getMessage());
            target.setImage(null);
        }
    }

    /**
     * Configura el directorio de caché. Debe llamarse al iniciar sesión.
     *
     * @param path Ruta absoluta a la carpeta de caché.
     */
    public static void setCacheDir(String path) {
        cacheDir = path;
        File dir = new File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Esta función pone la imagen en el cuadro (ImageView). Si ya la tenemos en
     * memoria, la usa. Si no, la busca.
     *
     * @param url La dirección de la foto o la ruta del archivo.
     * @param target El cuadro donde va la foto.
     * @param w Ancho que queremos.
     * @param h Alto que queremos.
     */
    public static void load(String urlOrPath, ImageView target, double w, double h) {
        if (target == null) {
            return;
        }

        // 1. Detener animaciones de carga previas
        stopSkeleton(target);

        // 2. Si no hay ruta, cargar imagen por defecto inmediatamente
        if (urlOrPath == null || urlOrPath.isEmpty()) {
            loadDefault(target, w, h);
            return;
        }

        // 3. Comprobar Caché de memoria para respuesta instantánea
        // Forzamos siempre el tamaño de miniatura (110x160) para ahorrar RAM
        // Aunque el componente pida más, cargamos poco para ganar fluidez
        double thumbW = 110;
        double thumbH = 160;
        String memoryKey = urlOrPath + "_" + thumbW + "x" + thumbH;

        if (memoryCache.containsKey(memoryKey)) {
            target.setImage(memoryCache.get(memoryKey));
            return;
        }

        // 4. CARGA ASÍNCRONA REAL (Punto 1: Fluidez)
        // Usamos el constructor de Image con backgroundLoading = true
        // Parámetros: url, ancho, alto, preservar ratio, suavizado, CARGA EN SEGUNDO PLANO
        String finalUrl = isValidUrl(urlOrPath) ? urlOrPath : new File(urlOrPath).toURI().toString();

        // El tercer parámetro 'true' activa el suavizado y el último 'true' la carga en segundo plano
        Image image = new Image(finalUrl, thumbW, thumbH, true, true, true);

        // Mostramos un estado vacío o placeholder mientras descarga/lee del disco
        target.setImage(null);

        // Cuando la imagen esté lista en segundo plano, se asigna al ImageView
        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() == 1.0 && !image.isError()) {
                javafx.application.Platform.runLater(() -> {
                    target.setImage(image);
                    memoryCache.put(memoryKey, image); // Guardar en caché para la próxima vez
                });
            }
        });

        // Manejo de errores silencioso para no bloquear la app
        image.errorProperty().addListener((obs, oldErr, isError) -> {
            if (isError) {
                javafx.application.Platform.runLater(() -> loadDefault(target, w, h));
            }
        });
    }

    /**
     * Sobrecarga para cargar tamaño original.
     */
    public static void load(String urlOrPath, ImageView target) {
        load(urlOrPath, target, -1, -1);
    }

    /**
     * Si la imagen es de internet: 1. Mira si ya la bajamos antes. 2. Si está,
     * la carga del disco. 3. Si no, la descarga y la guarda.
     */
    private static void handleWebImage(String url, ImageView target, double w, double h, String memoryKey) {
        if (cacheDir == null) {
            LOGGER.warning("ImageLoader: cacheDir no configurado. Usando carga directa.");
            startSkeleton(target, w, h);
            Image image = new Image(url, w > 0 ? w : 0, h > 0 ? h : 0, true, true, true);
            // Mostrar cuando cargue
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                    stopSkeleton(target);
                    target.setImage(image);
                }
            });
            return;
        }

        String filename = hashUrl(url);
        File cacheFile = new File(cacheDir, filename);

        if (cacheFile.exists()) {
            handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
        } else {
            // Empezar el esqueleto ANTES de descargar de internet
            startSkeleton(target, w, h);
            downloadAndLoad(url, cacheFile, target, w, h, memoryKey);
        }
    }

    /**
     * Descarga la imagen en un hilo separado, la guarda y luego la carga en el
     * UI.
     */
    private static void downloadAndLoad(String url, File destination, ImageView target, double w, double h, String memoryKey) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (InputStream in = new URL(url).openStream()) {
                    Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                // Éxito: Cargar localmente (ahí se detendrá el skeleton al terminar de leer el disco)
                handleLocalImage(destination.getAbsolutePath(), target, w, h, memoryKey);
            }

            @Override
            protected void failed() {
                stopSkeleton(target);
                LOGGER.warning("Fallo al descargar imagen para caché: " + url);
                loadDefault(target, w, h);
            }
        };
        executor.submit(task);
    }

    /**
     * Carga una imagen local.
     */
    private static void handleLocalImage(String path, ImageView target, double w, double h, String memoryKey) {
        File file = new File(path);
        if (!file.exists()) {
            stopSkeleton(target);
            loadDefault(target, w, h);
            return;
        }

        String uri = file.toURI().toString();
        double loadW = (w > 0) ? w : 0;
        double loadH = (h > 0) ? h : 0;

        Image image = new Image(uri, loadW, loadH, true, true, true);

        // Si por casualidad se cargó al instante, la ponemos y listo
        if (image.getProgress() >= 1.0) {
            if (!image.isError()) {
                memoryCache.put(memoryKey, image);
            }
            stopSkeleton(target);
            target.setImage(image);
        } else {
            // Activar skeleton mientras se lee del disco
            startSkeleton(target, w, h);

            // Esperar pacientemente a que llegue al 100% (1.0)
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    stopSkeleton(target);
                    if (!image.isError()) {
                        memoryCache.put(memoryKey, image);
                        target.setImage(image); // Dar el cambiazo
                    }
                }
            });

            image.errorProperty().addListener((obs, oldVal, isError) -> {
                if (isError) {
                    stopSkeleton(target);
                    memoryCache.remove(memoryKey);
                    loadDefault(target, w, h);
                }
            });
        }
    }

    /**
     * Comprueba si una cadena de texto es una URL válida. Solo se considera
     * válida si la URL comienza con "http://" o "https://".
     *
     * @param url Cadena de texto a validar como URL.
     * @return true si la URL no es nula y comienza con "http://" o "https://".
     * false en caso contrario, incluyendo si la URL es nula.
     */
    private static boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * Genera un hash SHA-256 de la URL para usar como nombre de archivo seguro.
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

            // Ponemos .png por defecto para que el SO lo reconozca como imagen si se
            // explora.
            return hexString.toString() + ".png";
        } catch (NoSuchAlgorithmException e) {
            // Fallback muy simple (no ideal por caracteres especiales)
            return String.valueOf(url.hashCode()) + ".png";
        }
    }

    /**
     * Limpia la caché de memoria.
     */
    public static void clearMemoryCache() {
        memoryCache.clear();
    }

    /**
     * Detiene el executor service para permitir que la aplicación se cierre
     * correctamente.
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    /**
     * Descarga (o copia) la portada a la carpeta local 'portadas' del usuario
     * para que sea 100% offline e independiente de internet.
     *
     * * @param urlOrPath URL de internet o ruta de un archivo local.
     * @param idLibro ID del libro (para nombrar el archivo de forma única).
     * @param carpetaUsuario Ruta absoluta de la carpeta base del usuario.
     * @return La nueva ruta local del archivo, o la original si falla.
     */
    public static String hacerPortadaLocalOffline(String urlOrPath, String idLibro, String carpetaUsuario) {
        // Si no hay portada o es la por defecto, no hacemos nada
        if (urlOrPath == null || urlOrPath.isEmpty() || urlOrPath.equals(DEFAULT_IMAGE_PATH)) {
            return urlOrPath;
        }

        //Usamos 'covers' para que coincida con la sincronización de NextCloud
        File dirCovers = new File(carpetaUsuario, "covers");
        if (!dirCovers.exists()) {
            dirCovers.mkdirs();
        }

        // Determinamos la extensión (por defecto .jpg)
        String extension = ".jpg";
        if (urlOrPath.toLowerCase().endsWith(".png")) {
            extension = ".png";
        }

        // El archivo final se llamará como el ID del libro
        File archivoDestino = new File(dirCovers, idLibro + extension);

        try {
            if (isValidUrl(urlOrPath)) {
                // Es de internet: la descargamos
                try (InputStream in = new URL(urlOrPath).openStream()) {
                    Files.copy(in, archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // Es un archivo local, lo copiamos
                File archivoOrigen = new File(urlOrPath);

                // IMPORTANTE: Si el archivo ya está en la carpeta covers (aunque sea con otra ruta absoluta)
                // solo necesitamos devolver la ruta, no volver a copiarlo sobre sí mismo.
                if (archivoOrigen.exists()) {
                    if (!archivoOrigen.getCanonicalPath().equals(archivoDestino.getCanonicalPath())) {
                        Files.copy(archivoOrigen.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    // Si nos pasan una ruta que no existe (ej. de otro PC), devolvemos null
                    // para que el sistema intente buscarla o cargar la por defecto.
                    return urlOrPath;
                }
            }

            // Devolvemos la ruta local absoluta de este PC
            return archivoDestino.getAbsolutePath();

        } catch (IOException e) {
            LOGGER.severe("Error al hacer la portada offline: " + e.getMessage());
            return urlOrPath;
        }
    }

    /**
     * Inicia la animación de "Skeleton Loader" (pulso gris) en el ImageView.
     */
    private static void startSkeleton(ImageView target, double w, double h) {
        // Evitar dobles animaciones en la misma celda
        if (target.getProperties().containsKey("skeleton_anim")) {
            return;
        }

        // Crear una imagen gris vacía del tamaño deseado
        int width = (w > 0) ? (int) w : 130;
        int height = (h > 0) ? (int) h : 180;
        WritableImage placeholder = new WritableImage(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                placeholder.getPixelWriter().setColor(x, y, Color.rgb(225, 225, 225));
            }
        }
        target.setImage(placeholder);

        // Crear la animación de pulso (Fade)
        FadeTransition ft = new FadeTransition(Duration.millis(700), target);
        ft.setFromValue(0.4);
        ft.setToValue(0.9);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        // Guardar la animación en las propiedades del nodo para detenerla luego
        target.getProperties().put("skeleton_anim", ft);
    }

    /**
     * Detiene la animación de "Skeleton Loader" y restaura la opacidad.
     */
    private static void stopSkeleton(ImageView target) {
        if (target.getProperties().containsKey("skeleton_anim")) {
            FadeTransition ft = (FadeTransition) target.getProperties().get("skeleton_anim");
            ft.stop();
            target.getProperties().remove("skeleton_anim");
        }
        target.setOpacity(1.0);
    }
}
