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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Clase para cargar imágenes sin que se trabe la app. Guarda las fotos en una
 * carpeta para no descargarlas dos veces.
 *
 * @author Fernando Lago
 * @version 1.0 (Versión simple)
 */
public class ImageLoader {

    private static final Logger LOGGER = Logger.getLogger(ImageLoader.class.getName());

    /**
     * Caché en memoria (RAM) para acceso ultrarrápido durante la sesión.
     */
    private static final Map<String, Image> memoryCache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Directorio donde se guardarán las imágenes descargadas.
     */
    private static String cacheDir = null;

    /**
     * Executor para descargas en segundo plano.
     */
    private static final ExecutorService executor = Executors.newFixedThreadPool(16);

    /**
     * Ruta de la imagen por defecto
     */
    private static final String DEFAULT_IMAGE_PATH = "/resources/default_cover.jpg";

    /**
     * Esta función carga la imagen por defecto (esa gris con el logo)
     * cuando un libro no tiene portada o cuando la url está mal.
     * Intento cargarla de la memoria para que vaya rápido.
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

        // Limpiar imagen previa mientras carga la nueva
        if (urlOrPath == null || urlOrPath.isEmpty()) {
            loadDefault(target, w, h);
            return;
        }

        // 1. CLAVE DE CACHÉ (Incluye dimensiones para el mapa en memoria)
        String memoryKey = urlOrPath + "_" + w + "x" + h;

        // 2. CHECK MEMORY CACHE
        if (memoryCache.containsKey(memoryKey)) {
            target.setImage(memoryCache.get(memoryKey));
            return;
        }

        // 3. DECIDIR FUENTE
        if (isValidUrl(urlOrPath)) {
            handleWebImage(urlOrPath, target, w, h, memoryKey);
        } else {
            handleLocalImage(urlOrPath, target, w, h, memoryKey);
        }
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
        // Si no se ha configurado caché, no guardamos en disco (o usamos temp)
        if (cacheDir == null) {
            LOGGER.warning("ImageLoader: cacheDir no configurado. Usando carga directa.");
            Image image = new Image(url, w > 0 ? w : 0, h > 0 ? h : 0, true, true, true);
            target.setImage(image);
            return;
        }

        // Generar nombre de archivo basado en hash de la URL
        String filename = hashUrl(url);
        File cacheFile = new File(cacheDir, filename);

        if (cacheFile.exists()) {
            // HIT EN DISCO: Cargar desde archivo local
            handleLocalImage(cacheFile.getAbsolutePath(), target, w, h, memoryKey);
        } else {
            // MISS EN DISCO: Descargar en background
            downloadAndLoad(url, cacheFile, target, w, h, memoryKey);
        }
    }

    /**
     * Descarga la imagen en un hilo separado, la guarda y luego la carga en el
     * UI.
     */
    private static void downloadAndLoad(String url, File destination, ImageView target, double w, double h,
            String memoryKey) {
        // Usamos un placeholder o spinner si se desea. Por ahora nada.

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Descargar bytes y guardar en disco
                try (InputStream in = new URL(url).openStream()) {
                    Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                // Éxito: Ahora cargamos desde el archivo local recién creado
                handleLocalImage(destination.getAbsolutePath(), target, w, h, memoryKey);
            }

            @Override
            protected void failed() {
                // Fallo: Cargar imagen por defecto
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
            loadDefault(target, w, h);
            return;
        }

        // Construir imagen
        // Nota: JavaFX carga asíncronamente si backgroundLoading=true.
        // Si w y h son > 0, JavaFX hace el resize nativo eficiente.
        String uri = file.toURI().toString();

        double loadW = (w > 0) ? w : 0; // 0 significa tamaño original en constructor de Image
        double loadH = (h > 0) ? h : 0;

        Image image = new Image(uri, loadW, loadH, true, true, true); // backgroundLoading=true

        // Guardar en caché de memoria inmediatamente (aunque se esté cargando)
        memoryCache.put(memoryKey, image);

        // Si falla la carga, quitar de cache y mostrar default
        image.errorProperty().addListener((obs, oldVal, isError) -> {
            if (isError) {
                memoryCache.remove(memoryKey);
                loadDefault(target, w, h);
            }
        });

        target.setImage(image);
    }

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
            // Añadir una extensión genérica o intentar adivinarla sería mejor,
            // pero JavaFX suele detectar el formato por contenido.
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
}
