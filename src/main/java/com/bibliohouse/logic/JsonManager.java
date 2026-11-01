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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Clase que gestiona la lectura y escritura de datos de la aplicación en
 * archivos JSON. Usa la librería Gson para manejar la serialización y
 * deserialización de objetos.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 *
 */
public class JsonManager {

    private static final Logger LOGGER = Logger.getLogger(JsonManager.class.getName());
    // Ruta base donde se guardan todos los datos de la aplicación.
    private static final String APP_BASE_DIRECTORY_PATH = System.getProperty("user.home") + File.separator
            + "BiblioHouse";

    // Ruta específica para los datos del usuario.
    private final String rutaDatosUsuario;
    // Declaración de preferencias
    private final String preferencesFilePath;

    private final String databaseFilePath;
    private final String prestamosDatabasePath;
    private final String sociosDatabasePath;
    private final String estanteriasDatabasePath;
    private final String deseosDatabasePath;
    private final String progresoLecturaFilePath;
    private final String eliminadosFilePath;

    /**
     * Nombre del archivo de marcas de borrado (tombstones) que se sincroniza
     * con NextCloud para que los elementos eliminados no reaparezcan.
     */
    public static final String ARCHIVO_ELIMINADOS = "eliminados.json";

    /**
     * Colecciones con soporte de marcas de borrado.
     */
    public static final String COL_LIBROS = "libros";
    public static final String COL_DESEOS = "deseos";
    public static final String COL_SOCIOS = "socios";
    public static final String COL_PRESTAMOS = "prestamos";

    /**
     * Archivos de datos que se fusionan elemento a elemento durante la
     * sincronización (en vez de sobrescribirse completos).
     */
    public static final java.util.Set<String> ARCHIVOS_FUSIONABLES = java.util.Set.of(
            ARCHIVO_ELIMINADOS, "biblioteca.json", "deseos.json", "socios.json", "prestamos.json");

    /**
     * Antigüedad máxima de una marca de borrado antes de purgarse (1 año).
     */
    private static final long CADUCIDAD_ELIMINADOS_MS = 365L * 24 * 60 * 60 * 1000;

    private final Map<String, ProgresoLectura> cacheProgresos = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Ebooks de los que ya se intentó extraer la portada sin éxito en esta
     * sesión.
     */
    private final java.util.Set<String> intentosPortadaFallidos = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Claves de cada colección tal y como las conoce la interfaz (último
     * cargado o guardado). Permite detectar qué elementos se han borrado al
     * guardar sin tener que instrumentar cada punto de borrado.
     */
    private final Map<String, java.util.Set<String>> clavesConocidas = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Instancias vivas, para poder volcar a disco lo pendiente al cerrar la
     * aplicación.
     */
    private static final java.util.Set<JsonManager> INSTANCIAS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Gson gson;

    /**
     * Tarea de sincronización automática con NextCloud. Puede ser {@code null}
     * si la sincronización está desactivada.
     */
    private Runnable autoSyncTask;

    /**
     * Ejecutor programado para el debounce del auto-sync. Un único hilo daemon
     * compartido para toda la vida del gestor.
     */
    private final ScheduledExecutorService syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nextcloud-autosync");
        t.setDaemon(true);
        return t;
    });

    // Red de seguridad: si la JVM termina sin pasar por App.stop(), se vuelca
    // igualmente lo pendiente de todas las instancias vivas.
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(JsonManager::cerrarTodas, "bibliohousefx-shutdown"));
    }

    /**
     * Referencia al sync pendiente (para cancelarlo si llega otro antes).
     */
    private ScheduledFuture<?> pendingSyncFuture;

    /**
     * Referencia al guardado de libros pendiente (debounce).
     */
    private ScheduledFuture<?> pendingSaveFuture;
    private Runnable pendingSaveTask;

    /**
     * Referencia al guardado de progreso de lectura pendiente (debounce).
     */
    private ScheduledFuture<?> pendingProgresoSaveFuture;

    /**
     * Establece la tarea de sincronización automática. Pasar {@code null}
     * desactiva la sincronización.
     *
     * @param task La tarea a ejecutar, o {@code null} para desactivar.
     */
    public void setAutoSyncTask(Runnable task) {
        this.autoSyncTask = task;
    }

    /**
     * Escribe inmediatamente en disco cualquier guardado de libros o de
     * progreso de lectura que esté esperando en el debounce.
     */
    public void flushPendientes() {
        if (pendingSaveFuture != null) {
            pendingSaveFuture.cancel(false);
        }
        ejecutarGuardadoPendiente();
        flushProgresoLectura();
    }

    /**
     * Ejecuta (una sola vez) el guardado de libros pendiente, venga del
     * temporizador del debounce o de un volcado forzado.
     */
    private void ejecutarGuardadoPendiente() {
        Runnable tarea;
        synchronized (this) {
            tarea = pendingSaveTask;
            pendingSaveTask = null;
        }
        if (tarea != null) {
            tarea.run();
        }
    }

    /**
     * Vuelca lo pendiente a disco y cierra el ejecutor de forma ordenada. Debe
     * llamarse al salir de la aplicación o al descartar esta instancia.
     */
    public void shutdown() {
        INSTANCIAS.remove(this);
        flushPendientes();
        if (pendingSyncFuture != null) {
            pendingSyncFuture.cancel(false);
        }
        syncScheduler.shutdown();
        try {
            if (!syncScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                syncScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cancela los guardados pendientes sin escribirlos y cierra el ejecutor.
     * Solo para cuando los datos se van a borrar (restaurar de fábrica).
     */
    public void descartarSinGuardar() {
        INSTANCIAS.remove(this);
        synchronized (this) {
            pendingSaveTask = null;
        }
        if (pendingSaveFuture != null) {
            pendingSaveFuture.cancel(false);
        }
        if (pendingProgresoSaveFuture != null) {
            pendingProgresoSaveFuture.cancel(false);
        }
        cacheProgresos.clear();
        syncScheduler.shutdownNow();
    }

    /**
     * Cierra todas las instancias vivas volcando antes sus datos pendientes.
     */
    public static void cerrarTodas() {
        for (JsonManager jm : new ArrayList<>(INSTANCIAS)) {
            try {
                jm.shutdown();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar el gestor de datos", e);
            }
        }
    }

    /**
     * Clase interna es para que Gson sepa cómo manejar las fechas (LocalDate).
     */
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        // Formato de la fecha
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        /**
         * Convierte un String de JSON a una fecha LocalDate.
         *
         * @param json El JSON con la fecha en formato String.
         * @param typeOfT Tipo del objeto.
         * @param context Contexto de la serialización.
         * @return La fecha parseada desde el String.
         * @throws JsonParseException Si la fecha no tiene el formato correcto.
         */
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        /**
         * Convierte un JsonPrimitive (String) a un objeto LocalDate.
         *
         * @param json JSON con la fecha en formato String.
         * @param typeOfT Tipo del objeto.
         * @param context Contexto de deserialización.
         * @return Objeto LocalDate parseado desde el String.
         * @throws JsonParseException Si la fecha no tiene el formato correcto.
         */
        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }

    /**
     * Constructor de JsonManager.Recibe la ruta de datos del usuario.
     *
     * @param rutaDatosUsuario La ruta completa a la carpeta de datos del
     * usuario actual.
     */
    public JsonManager(String rutaDatosUsuario) {
        if (rutaDatosUsuario == null || rutaDatosUsuario.isEmpty()) {
            throw new IllegalArgumentException("La ruta de datos del usuario no puede ser nula o vacía.");
        }

        this.rutaDatosUsuario = rutaDatosUsuario;

        // Se define las rutas de los archivos dentro de la carpeta del usuario
        this.databaseFilePath = rutaDatosUsuario + File.separator + "biblioteca.json";
        this.prestamosDatabasePath = rutaDatosUsuario + File.separator + "prestamos.json";
        this.sociosDatabasePath = rutaDatosUsuario + File.separator + "socios.json";
        this.estanteriasDatabasePath = rutaDatosUsuario + File.separator + "estanterias.json";
        this.preferencesFilePath = rutaDatosUsuario + File.separator + "preferences.json"; // <-- Inicialización
        this.deseosDatabasePath = rutaDatosUsuario + File.separator + "deseos.json";
        this.progresoLecturaFilePath = rutaDatosUsuario + File.separator + "progreso_lectura.json";
        this.eliminadosFilePath = rutaDatosUsuario + File.separator + ARCHIVO_ELIMINADOS;

        // Se configura el Gson para que use el adaptador de fechas
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        // Se crea las carpetas necesarias si no existen
        crearDirectorioBaseSiNoExiste();

        crearDirectorioUsuarioSiNoExiste();

        INSTANCIAS.add(this);
    }

    /**
     * Crea la carpeta base de la aplicación si no existe. Esto es por si es la
     * primera vez que se ejecuta el programa.
     */
    private void crearDirectorioBaseSiNoExiste() {
        File appBaseDir = new File(APP_BASE_DIRECTORY_PATH);
        if (!appBaseDir.exists()) {
            LOGGER.info("Creando directorio base de la aplicación en: " + APP_BASE_DIRECTORY_PATH);
            if (!appBaseDir.mkdirs()) {
                LOGGER.severe("Error: no se pudo crear el directorio base.");
            }
        }
        // Aseguramos que la carpeta esté oculta
        ocultarDirectorio(appBaseDir);
    }

    /**
     * Intenta ocultar el directorio para que el usuario no lo borre
     * accidentalmente. En macOS usa 'chflags hidden', en Windows usa el
     * atributo 'dos:hidden'.
     *
     * @param dir El directorio a ocultar.
     */
    private void ocultarDirectorio(File dir) {
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        try {
            if (os.contains("mac")) {
                // En macOS usamos el comando chflags
                new ProcessBuilder("chflags", "hidden", dir.getAbsolutePath()).start();
            } else if (os.contains("win")) {
                // En Windows usamos la API de NIO
                java.nio.file.Files.setAttribute(dir.toPath(), "dos:hidden", true);
            }
        } catch (IOException e) {
            // No es crítico si falla, solo mostramos logging
            LOGGER.log(Level.WARNING, "No se pudo ocultar la carpeta de datos: {0}", e.getMessage());
        }
    }

    /**
     * Crea la carpeta del usuario si no existe. Esto es por si es la primera
     * vez que este usuario usa el programa.
     */
    private void crearDirectorioUsuarioSiNoExiste() {
        File userDir = new File(this.rutaDatosUsuario);
        if (!userDir.exists()) {
            LOGGER.info("Creando directorio para el usuario en: " + this.rutaDatosUsuario);
            if (!userDir.mkdirs()) {
                LOGGER.log(Level.SEVERE, "Error: no se pudo crear el directorio del usuario en: {0}",
                        this.rutaDatosUsuario);
            }
        }
    }

    /**
     * Método genérico para guardar cualquier objeto (lista, mapa, etc.) en un
     * archivo JSON de forma atómica y segura con backup.
     *
     * @param objeto El objeto a serializar.
     * @param path La ruta del archivo donde se guardará.
     * @param tipoDato Descripción del dato para el logging.
     */
    private void guardarObjeto(Object objeto, String path, String tipoDato) {
        guardarObjeto(objeto, path, tipoDato, true);
    }

    /**
     * Guarda un objeto en JSON de forma atómica y con backup.
     *
     * @param objeto El objeto a serializar.
     * @param path La ruta del archivo donde se guardará.
     * @param tipoDato Descripción del dato para el logging.
     * @param dispararSync {@code false} para no programar una sincronización
     * (p. ej. cuando el guardado lo provoca la propia sincronización).
     */
    private synchronized void guardarObjeto(Object objeto, String path, String tipoDato, boolean dispararSync) {
        crearDirectorioUsuarioSiNoExiste();

        File archivoFinal = new File(path);
        File archivoTemporal = new File(path + ".tmp");
        File archivoBackup = new File(path + ".bak");

        // 1. Escribir en el archivo TEMPORAL primero
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(archivoTemporal);
                Writer writer = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(objeto, writer);
            writer.flush();
            // Forzamos el volcado al disco físico antes del renombrado
            fos.getFD().sync();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error crítico: No se pudo escribir el archivo temporal para " + tipoDato, e);
            return; // Si falla el temporal, no tocamos el original
        }

        // 2. Si la escritura fue bien, gestionamos el reemplazo atómico
        try {
            // Creamos un backup del original antes de borrarlo (seguridad extra)
            if (archivoFinal.exists()) {
                java.nio.file.Files.copy(archivoFinal.toPath(), archivoBackup.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Movemos el temporal al final (esto es una operación atómica en el SO)
            java.nio.file.Files.move(archivoTemporal.toPath(), archivoFinal.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            LOGGER.log(Level.FINE, "Guardado atómico completado: {0}", path);
        } catch (java.nio.file.AtomicMoveNotSupportedException amns) {
            // Fallback: mover sin atomicidad en filesystems que no lo soportan (ej. NFS,
            // red)
            try {
                java.nio.file.Files.move(archivoTemporal.toPath(), archivoFinal.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.log(Level.WARNING, "ATOMIC_MOVE no soportado para {0}, usando fallback.", path);
            } catch (IOException fallbackEx) {
                LOGGER.log(Level.SEVERE, "Error en fallback al renombrar archivo para " + tipoDato, fallbackEx);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al renombrar archivo temporal a final para " + tipoDato, e);
        }

        // 3. Lanzar sincronización si existe
        if (dispararSync && autoSyncTask != null && !syncScheduler.isShutdown()) {
            if (pendingSyncFuture != null && !pendingSyncFuture.isDone()) {
                pendingSyncFuture.cancel(false);
            }
            pendingSyncFuture = syncScheduler.schedule(autoSyncTask, 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Método genérico para guardar cualquier lista de objetos en un archivo
     * JSON.
     *
     * @param lista La lista de objetos que queremos guardar.
     * @param path La ruta del archivo donde se guardará.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     * guardando.
     */
    private synchronized <T> void guardarDatos(List<T> lista, String path, String tipoDato) {
        guardarObjeto(lista, path, tipoDato);
    }

    /**
     * Método genérico para cargar un objeto o mapa desde un archivo JSON.
     *
     * @param path La ruta del archivo.
     * @param tipoObjeto El tipo del objeto esperado.
     * @param tipoDato Descripción del dato para el logging.
     * @return El objeto deserializado, o {@code null} si falla o no existe.
     */
    private <T> T cargarObjeto(String path, Type tipoObjeto, String tipoDato) {
        File file = new File(path);
        File backupFile = new File(path + ".bak");

        // Intentar cargar el archivo principal
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                T obj = gson.fromJson(reader, tipoObjeto);
                if (obj != null) {
                    LOGGER.log(Level.FINE, "Cargado {0} desde {1}", new Object[]{tipoDato, path});
                    return obj;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar " + path + ". Intentando cargar backup...", e);
            }
        }

        // Si llegamos aquí, o no existe el principal o falló. Intentamos backup.
        if (backupFile.exists()) {
            try (FileReader reader = new FileReader(backupFile, java.nio.charset.StandardCharsets.UTF_8)) {
                T obj = gson.fromJson(reader, tipoObjeto);
                if (obj != null) {
                    LOGGER.log(Level.WARNING, "RECUPERADO: Cargado {0} desde BACKUP {1}",
                            new Object[]{tipoDato, backupFile.getPath()});
                    return obj;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar backup " + backupFile.getPath(), e);
            }
        }

        return null;
    }

    /**
     * Método genérico para cargar datos desde un archivo JSON en forma de
     * lista.
     *
     * @param path La ruta del archivo que queremos cargar.
     * @param tipoLista El tipo de la lista que esperamos.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     * cargando.
     * @return La lista de objetos cargados desde el archivo. Si hay error,
     * devuelve una lista vacía.
     */
    private <T> List<T> cargarDatos(String path, Type tipoLista, String tipoDato) {
        List<T> resultado = cargarObjeto(path, tipoLista, tipoDato);
        if (resultado == null) {
            LOGGER.log(Level.INFO,
                    "No se encontraron datos válidos para {0} (ni original ni backup). Se devuelve lista vacía.", tipoDato);
            return new ArrayList<>();
        }
        return resultado;
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS LIBROS ---
    /**
     * Convierte una ruta absoluta de archivo o ruta local en una ruta relativa
     * canónica (ej. "covers/id.jpg" o "ebooks/id.epub") para persistir en JSON
     * de forma portable entre diferentes sistemas operativos (Windows, macOS,
     * Linux, Android).
     *
     * @param urlOrPath Ruta local o URL remota.
     * @param subcarpeta Subcarpeta base ("covers" o "ebooks").
     * @return Ruta relativa portable o la URL remota original.
     */
    public static String convertirARutaRelativa(String urlOrPath, String subcarpeta) {
        if (urlOrPath == null || urlOrPath.isBlank() || urlOrPath.contains("default_cover")) {
            return "";
        }
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            return urlOrPath;
        }
        String nombre = com.bibliohouse.utils.ImageLoader.extraerNombreArchivo(urlOrPath);
        if (nombre.isBlank()) {
            return "";
        }
        return subcarpeta + "/" + nombre;
    }

    /**
     * Prepara una lista de libros para persistencia en JSON, convirtiendo rutas
     * absolutas locales de portadas y ebooks en rutas relativas universales
     * ("covers/..." y "ebooks/..."). Genera directamente el árbol JSON (una
     * única pasada de serialización), de modo que los objetos vivos en memoria
     * no se mutan y no hace falta clonar cada libro.
     *
     * @param libros Lista de libros a preparar.
     * @return Árbol JSON con rutas relativas listo para escribir.
     */
    private com.google.gson.JsonArray prepararLibrosParaGuardar(List<Libro> libros) {
        com.google.gson.JsonArray arbol = new com.google.gson.JsonArray();
        if (libros == null) {
            return arbol;
        }
        for (Libro original : libros) {
            if (original == null) {
                continue;
            }
            com.google.gson.JsonObject obj = gson.toJsonTree(original).getAsJsonObject();
            obj.addProperty("portadaURL", convertirARutaRelativa(original.getPortadaURL(), "covers"));
            if (original.getRutaArchivoDigital() != null && !original.getRutaArchivoDigital().isBlank()) {
                obj.addProperty("rutaArchivoDigital", convertirARutaRelativa(original.getRutaArchivoDigital(), "ebooks"));
            }
            arbol.add(obj);
        }
        return arbol;
    }

    // --- MARCAS DE BORRADO (TOMBSTONES) ---
    /**
     * Calcula las claves únicas de una lista de libros.
     */
    private static java.util.Set<String> clavesLibros(List<Libro> libros) {
        java.util.Set<String> claves = new java.util.HashSet<>();
        if (libros != null) {
            for (Libro l : libros) {
                if (l != null) {
                    claves.add(obtenerClaveUnicaLibro(l));
                }
            }
        }
        return claves;
    }

    /**
     * Clave única de un socio (su número de socio).
     *
     * @param socio Socio.
     * @return Clave canónica.
     */
    public static String obtenerClaveSocio(Socio socio) {
        return socio == null ? "" : String.valueOf(socio.getNumeroSocio());
    }

    /**
     * Clave única de un préstamo (su ID, o libro + fecha si no tiene ID).
     *
     * @param p Préstamo.
     * @return Clave canónica.
     */
    public static String obtenerClavePrestamo(Prestamo p) {
        if (p == null) {
            return "";
        }
        if (p.getId() != null && !p.getId().isBlank()) {
            return p.getId().trim();
        }
        return "libro:" + p.getLibroId() + "|" + p.getFechaPrestamo() + "|" + p.getNumeroSocio();
    }

    private static <T> java.util.Set<String> claves(List<T> lista, java.util.function.Function<T, String> clave) {
        java.util.Set<String> res = new java.util.HashSet<>();
        if (lista != null) {
            for (T t : lista) {
                if (t != null) {
                    res.add(clave.apply(t));
                }
            }
        }
        return res;
    }

    /**
     * Registra las claves que la interfaz conoce para una colección (tras una
     * carga), sin generar marcas de borrado.
     */
    private void recordarClaves(String coleccion, java.util.Set<String> claves) {
        clavesConocidas.put(coleccion, new java.util.HashSet<>(claves));
    }

    /**
     * Compara las claves que se van a guardar con las últimas conocidas y
     * anota como borradas las que han desaparecido. Si una clave borrada
     * vuelve a aparecer (elemento recreado), se retira su marca.
     *
     * <p>
     * Como protección, si en un único guardado desaparece más de la mitad de
     * una colección de 10 o más elementos no se generan marcas: es más
     * probable un error que un borrado intencionado, y propagarlo a la nube
     * borraría los datos en todos los dispositivos.</p>
     *
     * @param coleccion Nombre de la colección.
     * @param clavesNuevas Claves de la lista que se va a guardar.
     */
    private synchronized void actualizarEliminados(String coleccion, java.util.Set<String> clavesNuevas) {
        java.util.Set<String> previas = clavesConocidas.put(coleccion, new java.util.HashSet<>(clavesNuevas));
        if (previas == null) {
            return; // Nunca se cargó esta colección: no sabemos qué se ha borrado
        }

        Map<String, Map<String, Long>> eliminados = cargarEliminados();
        Map<String, Long> mapa = eliminados.computeIfAbsent(coleccion, k -> new HashMap<>());
        boolean cambio = false;

        List<String> borradas = new ArrayList<>();
        for (String k : previas) {
            if (!clavesNuevas.contains(k)) {
                borradas.add(k);
            }
        }
        if (!borradas.isEmpty()) {
            if (previas.size() >= 10 && borradas.size() * 2 > previas.size()) {
                LOGGER.log(Level.WARNING,
                        "Borrado masivo en {0} ({1} de {2}): no se generan marcas de borrado por seguridad.",
                        new Object[]{coleccion, borradas.size(), previas.size()});
            } else {
                long ahora = System.currentTimeMillis();
                for (String k : borradas) {
                    mapa.put(k, ahora);
                }
                cambio = true;
            }
        }

        for (String k : clavesNuevas) {
            if (!previas.contains(k) && mapa.remove(k) != null) {
                cambio = true;
            }
        }

        if (cambio) {
            guardarObjeto(eliminados, eliminadosFilePath, "marcas de borrado", false);
        }
    }

    /**
     * Carga las marcas de borrado: colección → (clave → instante del borrado).
     * Purga las que superan la caducidad.
     *
     * @return Mapa mutable de marcas de borrado.
     */
    public synchronized Map<String, Map<String, Long>> cargarEliminados() {
        Type tipo = new TypeToken<HashMap<String, HashMap<String, Long>>>() {
        }.getType();
        Map<String, Map<String, Long>> datos = cargarObjeto(eliminadosFilePath, tipo, "marcas de borrado");
        if (datos == null) {
            datos = new HashMap<>();
        }
        long limite = System.currentTimeMillis() - CADUCIDAD_ELIMINADOS_MS;
        for (Map<String, Long> m : datos.values()) {
            if (m != null) {
                m.values().removeIf(t -> t == null || t < limite);
            }
        }
        return datos;
    }

    /**
     * Une dos conjuntos de marcas de borrado quedándose con el instante más
     * reciente de cada clave.
     *
     * @param a Marcas locales.
     * @param b Marcas remotas.
     * @return Unión de ambas.
     */
    public static Map<String, Map<String, Long>> fusionarEliminados(Map<String, Map<String, Long>> a,
            Map<String, Map<String, Long>> b) {
        Map<String, Map<String, Long>> res = new HashMap<>();
        for (Map<String, Map<String, Long>> origen : java.util.Arrays.asList(a, b)) {
            if (origen == null) {
                continue;
            }
            origen.forEach((col, mapa) -> {
                if (mapa != null) {
                    Map<String, Long> destino = res.computeIfAbsent(col, k -> new HashMap<>());
                    mapa.forEach((k, t) -> {
                        if (t != null) {
                            destino.merge(k, t, Math::max);
                        }
                    });
                }
            });
        }
        return res;
    }

    /**
     * Fusiona dos colecciones de libros (por ejemplo, local y remota de
     * NextCloud) resolviendo conflictos por identificador único (ID) y
     * timestamp de última modificación. Si un libro existe en ambas
     * colecciones, se conserva la versión con el timestamp más reciente. Si un
     * libro solo existe en una de las colecciones, se incluye en la colección
     * unificada.
     *
     * @param locales Lista de libros de la biblioteca local.
     * @param remotas Lista de libros provenientes del almacenamiento remoto o
     * móvil.
     * @return Lista unificada y sin duplicados con los libros más actualizados
     * de ambos lados.
     */
    public static List<Libro> fusionarColecciones(List<Libro> locales, List<Libro> remotas) {
        if (locales == null && remotas == null) {
            return new ArrayList<>();
        }
        if (locales == null || locales.isEmpty()) {
            return remotas != null ? new ArrayList<>(remotas) : new ArrayList<>();
        }
        if (remotas == null || remotas.isEmpty()) {
            return new ArrayList<>(locales);
        }

        Map<String, Libro> mapaFusion = new LinkedHashMap<>();

        // 1. Indexar libros locales
        for (Libro local : locales) {
            if (local == null) {
                continue;
            }
            String clave = obtenerClaveUnicaLibro(local);
            mapaFusion.put(clave, local);
        }

        // 2. Fusionar libros remotos
        for (Libro remoto : remotas) {
            if (remoto == null) {
                continue;
            }
            String clave = obtenerClaveUnicaLibro(remoto);
            Libro localExistente = mapaFusion.get(clave);

            if (localExistente == null) {
                // Libro nuevo que solo existe en remoto: se agrega a la colección
                mapaFusion.put(clave, remoto);
            } else {
                // Existe en ambos lados: gana la versión con el timestamp de última modificación más reciente
                long tiempoLocal = localExistente.getUltimaModificacion();
                long tiempoRemoto = remoto.getUltimaModificacion();

                if (tiempoRemoto > tiempoLocal) {
                    mapaFusion.put(clave, remoto);
                }
            }
        }

        return new ArrayList<>(mapaFusion.values());
    }

    /**
     * Fusiona dos colecciones de libros como
     * {@link #fusionarColecciones(List, List)} y después descarta los libros
     * con una marca de borrado igual o posterior a su última modificación (un
     * libro editado en otro dispositivo después de borrarse aquí se conserva).
     *
     * @param locales Libros locales.
     * @param remotas Libros remotos.
     * @param eliminados Marcas de borrado (clave → instante), puede ser null.
     * @return Colección unificada sin los libros borrados.
     */
    public static List<Libro> fusionarColecciones(List<Libro> locales, List<Libro> remotas,
            Map<String, Long> eliminados) {
        List<Libro> fusion = fusionarColecciones(locales, remotas);
        if (eliminados != null && !eliminados.isEmpty()) {
            fusion.removeIf(l -> {
                Long borrado = eliminados.get(obtenerClaveUnicaLibro(l));
                return borrado != null && borrado >= l.getUltimaModificacion();
            });
        }
        return fusion;
    }

    /**
     * Fusión genérica por clave para colecciones sin marca de tiempo (socios,
     * préstamos). Ante un conflicto decide {@code resolver(local, remoto)}; las
     * claves con marca de borrado se descartan.
     */
    static <T> List<T> fusionarPorClave(List<T> locales, List<T> remotas,
            java.util.function.Function<T, String> clave,
            java.util.function.BinaryOperator<T> resolver, Map<String, Long> eliminados) {
        Map<String, T> mapa = new LinkedHashMap<>();
        if (locales != null) {
            for (T t : locales) {
                if (t != null) {
                    mapa.put(clave.apply(t), t);
                }
            }
        }
        if (remotas != null) {
            for (T t : remotas) {
                if (t != null) {
                    mapa.merge(clave.apply(t), t, resolver);
                }
            }
        }
        if (eliminados != null && !eliminados.isEmpty()) {
            mapa.keySet().removeIf(eliminados::containsKey);
        }
        return new ArrayList<>(mapa.values());
    }

    /**
     * Fusiona el contenido remoto de un archivo de datos con su versión local
     * y guarda el resultado en disco (sin disparar otra sincronización ni
     * alterar las claves conocidas por la interfaz).
     *
     * @param nombreArchivo Uno de {@link #ARCHIVOS_FUSIONABLES}.
     * @param remoto Contenido JSON remoto.
     * @return {@code true} si el archivo local ha cambiado.
     * @throws IOException Si no se puede leer el archivo local.
     * @throws JsonParseException Si el contenido remoto no es JSON válido.
     */
    public synchronized boolean fusionarConRemoto(String nombreArchivo, Reader remoto) throws IOException {
        File local = new File(rutaDatosUsuario, nombreArchivo);
        byte[] antes = local.exists() ? java.nio.file.Files.readAllBytes(local.toPath()) : new byte[0];

        Map<String, Map<String, Long>> eliminados = cargarEliminados();
        switch (nombreArchivo) {
            case ARCHIVO_ELIMINADOS -> {
                Type tipo = new TypeToken<HashMap<String, HashMap<String, Long>>>() {
                }.getType();
                Map<String, Map<String, Long>> remotos = gson.fromJson(remoto, tipo);
                guardarObjeto(fusionarEliminados(eliminados, remotos), eliminadosFilePath, "marcas de borrado", false);
            }
            case "biblioteca.json", "deseos.json" -> {
                boolean esBiblioteca = nombreArchivo.equals("biblioteca.json");
                String path = esBiblioteca ? databaseFilePath : deseosDatabasePath;
                Type tipo = new TypeToken<ArrayList<Libro>>() {
                }.getType();
                List<Libro> locales = cargarDatos(path, tipo, nombreArchivo);
                List<Libro> fusion = fusionarColecciones(locales, parsearLibros(remoto),
                        eliminados.get(esBiblioteca ? COL_LIBROS : COL_DESEOS));
                guardarObjeto(prepararLibrosParaGuardar(fusion), path, nombreArchivo, false);
            }
            case "socios.json" -> {
                Type tipo = new TypeToken<ArrayList<Socio>>() {
                }.getType();
                List<Socio> remotos = gson.fromJson(remoto, tipo);
                List<Socio> fusion = fusionarPorClave(cargarDatos(sociosDatabasePath, tipo, "socios"), remotos,
                        JsonManager::obtenerClaveSocio, (l, r) -> l, eliminados.get(COL_SOCIOS));
                guardarObjeto(fusion, sociosDatabasePath, "socios", false);
            }
            case "prestamos.json" -> {
                Type tipo = new TypeToken<ArrayList<Prestamo>>() {
                }.getType();
                List<Prestamo> remotos = gson.fromJson(remoto, tipo);
                // Un préstamo devuelto es un estado final: gana sobre uno aún abierto
                List<Prestamo> fusion = fusionarPorClave(cargarDatos(prestamosDatabasePath, tipo, "préstamos"), remotos,
                        JsonManager::obtenerClavePrestamo,
                        (l, r) -> l.getFechaDevolucion() == null && r.getFechaDevolucion() != null ? r : l,
                        eliminados.get(COL_PRESTAMOS));
                guardarObjeto(fusion, prestamosDatabasePath, "préstamos", false);
            }
            default ->
                throw new IllegalArgumentException("Archivo no fusionable: " + nombreArchivo);
        }

        byte[] despues = local.exists() ? java.nio.file.Files.readAllBytes(local.toPath()) : new byte[0];
        return !java.util.Arrays.equals(antes, despues);
    }

    /**
     * Obtiene la clave de identificación única canónica para un libro durante
     * el merge. Prioriza ID (UUID), con fallback a ISBN o Título+Autor.
     *
     * @param libro El libro a indexar.
     * @return Clave única representativa.
     */
    public static String obtenerClaveUnicaLibro(Libro libro) {
        if (libro == null) {
            return "";
        }
        if (libro.getId() != null && !libro.getId().isBlank()) {
            return libro.getId().trim();
        }
        if (libro.getIsbn() != null && !libro.getIsbn().isBlank()) {
            return "isbn:" + libro.getIsbn().replaceAll("[^0-9Xx]", "");
        }
        return "title:" + (libro.getTitulo() != null ? libro.getTitulo().toLowerCase(java.util.Locale.ROOT).trim() : "")
                + "|" + (libro.getAutor() != null ? libro.getAutor().toLowerCase(java.util.Locale.ROOT).trim() : "");
    }

    /**
     * Fusiona la lista actual de libros con una lista remota, guardando en
     * disco la versión combinada (resolviendo por ID y timestamp de última
     * modificación).
     *
     * @param librosRemotos Lista de libros obtenidos de la nube o dispositivo
     * remoto.
     * @return Lista unificada final resultante de la fusión.
     */
    public synchronized List<Libro> fusionarYGuardarLibros(List<Libro> librosRemotos) {
        List<Libro> locales = cargarLibros();
        List<Libro> fusionados = fusionarColecciones(locales, librosRemotos, cargarEliminados().get(COL_LIBROS));
        guardarLibros(fusionados);
        return fusionados;
    }

    /**
     * Fusiona la lista actual de deseos con una lista remota, guardando en
     * disco la versión combinada (resolviendo por ID y timestamp de última
     * modificación).
     *
     * @param deseosRemotos Lista de deseos obtenidos de la nube o dispositivo
     * remoto.
     * @return Lista unificada final resultante de la fusión.
     */
    public synchronized List<Libro> fusionarYGuardarDeseos(List<Libro> deseosRemotos) {
        List<Libro> locales = cargarDeseos();
        List<Libro> fusionados = fusionarColecciones(locales, deseosRemotos, cargarEliminados().get(COL_DESEOS));
        guardarDeseos(fusionados);
        return fusionados;
    }

    /**
     * Parsea una lista de libros desde una cadena JSON.
     *
     * @param json Cadena JSON con la lista de libros.
     * @return Lista de libros parseados.
     */
    public List<Libro> parsearLibros(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();
        List<Libro> lista = gson.fromJson(json, tipoLista);
        return lista != null ? lista : new ArrayList<>();
    }

    /**
     * Parsea una lista de libros desde un Reader de caracteres.
     *
     * @param reader Lector con el contenido JSON.
     * @return Lista de libros parseados.
     */
    public List<Libro> parsearLibros(Reader reader) {
        if (reader == null) {
            return new ArrayList<>();
        }
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();
        List<Libro> lista = gson.fromJson(reader, tipoLista);
        return lista != null ? lista : new ArrayList<>();
    }

    /**
     * Serializa una lista de libros a formato JSON con rutas relativas
     * portables.
     *
     * @param libros Lista de libros a serializar.
     * @return Cadena JSON formateada.
     */
    public String serializarLibros(List<Libro> libros) {
        return gson.toJson(prepararLibrosParaGuardar(libros));
    }

    /**
     * Resuelve las rutas relativas ("covers/..." y "ebooks/...") contenidas en
     * los JSON contra el directorio de datos local de la aplicación en la
     * máquina actual.
     *
     * @param libros Lista de libros cargados.
     * @param carpetaCovers Directorio local de portadas.
     * @param carpetaEbooks Directorio local de ebooks.
     */
    private void resolverRutasLocales(List<Libro> libros, String carpetaCovers, String carpetaEbooks) {
        if (libros == null) {
            return;
        }
        for (Libro libro : libros) {
            if (libro.getEstanterias() == null) {
                libro.setEstanterias(new ArrayList<>());
            }

            // 1. Resolver portada
            String url = libro.getPortadaURL();
            if (url != null && !url.isEmpty() && !url.startsWith("http") && !url.contains("default_cover")) {
                String nombreArchivo = com.bibliohouse.utils.ImageLoader.extraerNombreArchivo(url);
                File localEsperado = new File(carpetaCovers, nombreArchivo);
                if (localEsperado.exists() && localEsperado.isFile()) {
                    libro.setPortadaURL(localEsperado.getAbsolutePath());
                } else {
                    boolean encontrado = false;
                    if (libro.getId() != null && !libro.getId().isBlank()) {
                        for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp"}) {
                            File porId = new File(carpetaCovers, libro.getId() + ext);
                            if (porId.exists() && porId.isFile()) {
                                libro.setPortadaURL(porId.getAbsolutePath());
                                encontrado = true;
                                break;
                            }
                        }
                    }
                    if (!encontrado && libro.getIsbn() != null && !libro.getIsbn().isBlank()) {
                        String isbnLimpio = libro.getIsbn().replaceAll("[^0-9Xx]", "");
                        for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp"}) {
                            File porIsbn = new File(carpetaCovers, isbnLimpio + ext);
                            if (porIsbn.exists() && porIsbn.isFile()) {
                                libro.setPortadaURL(porIsbn.getAbsolutePath());
                                encontrado = true;
                                break;
                            }
                        }
                    }
                    if (!encontrado) {
                        libro.setPortadaURL(localEsperado.getAbsolutePath());
                    }
                }
            } else if ((url == null || url.isEmpty() || url.contains("default_cover")) && libro.getId() != null) {
                for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp"}) {
                    File porId = new File(carpetaCovers, libro.getId() + ext);
                    if (porId.exists() && porId.isFile()) {
                        libro.setPortadaURL(porId.getAbsolutePath());
                        break;
                    }
                }
            }

            // 2. Resolver archivo digital (ebook)
            String digital = libro.getRutaArchivoDigital();
            if (digital != null && !digital.isBlank() && !digital.startsWith("http")) {
                String nombreEbook = com.bibliohouse.utils.ImageLoader.extraerNombreArchivo(digital);
                File ebookLocal = new File(carpetaEbooks, nombreEbook);
                if (ebookLocal.exists() && ebookLocal.isFile()) {
                    libro.setRutaArchivoDigital(ebookLocal.getAbsolutePath());
                } else if (libro.getId() != null) {
                    for (String ext : new String[]{".epub", ".pdf", ".mobi", ".azw3", ".cbz"}) {
                        File porId = new File(carpetaEbooks, libro.getId() + ext);
                        if (porId.exists() && porId.isFile()) {
                            libro.setRutaArchivoDigital(porId.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
        }
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS LIBROS ---
    /**
     * Guarda la lista de libros en el archivo JSON correspondiente con rutas
     * relativas portables.
     *
     * @param libros Lista de libros a guardar.
     */
    public void guardarLibros(List<Libro> libros) {
        // Este guardado contiene la lista más reciente: cualquier guardado
        // diferido pendiente quedaría obsoleto y podría pisarlo.
        synchronized (this) {
            pendingSaveTask = null;
            if (pendingSaveFuture != null) {
                pendingSaveFuture.cancel(false);
            }
        }
        com.google.gson.JsonArray paraGuardar = prepararLibrosParaGuardar(libros);
        java.util.Set<String> claves = clavesLibros(libros);
        synchronized (this) {
            actualizarEliminados(COL_LIBROS, claves);
            guardarObjeto(paraGuardar, databaseFilePath, "libros");
        }
    }

    /**
     * Guarda la lista de libros con debounce y rutas relativas portables.
     *
     * @param libros Lista de libros a guardar.
     */
    public void guardarLibrosDebounced(List<Libro> libros) {
        // La instantánea se toma en el hilo llamante (un único árbol JSON, sin
        // clonar libros); la escritura a disco va en segundo plano.
        com.google.gson.JsonArray copia = prepararLibrosParaGuardar(libros);
        java.util.Set<String> claves = clavesLibros(libros);
        synchronized (this) {
            pendingSaveTask = () -> {
                synchronized (JsonManager.this) {
                    actualizarEliminados(COL_LIBROS, claves);
                    guardarObjeto(copia, databaseFilePath, "libros");
                }
            };
            if (pendingSaveFuture != null && !pendingSaveFuture.isDone()) {
                pendingSaveFuture.cancel(false);
            }
            if (syncScheduler.isShutdown()) {
                ejecutarGuardadoPendiente();
                return;
            }
            // Programar guardado real en 500ms
            pendingSaveFuture = syncScheduler.schedule(this::ejecutarGuardadoPendiente, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Carga la lista de libros desde el archivo JSON. Resuelve las rutas
     * relativas contra el directorio local de la aplicación.
     *
     * @return Lista de libros cargados.
     */
    public List<Libro> cargarLibros() {
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();

        // 1. Cargamos los libros del archivo JSON
        List<Libro> libros = cargarDatos(databaseFilePath, tipoLista, "libros");

        // 2. Definimos rutas
        String carpetaCovers = rutaDatosUsuario + File.separator + "covers";
        String carpetaPortadasAntigua = rutaDatosUsuario + File.separator + "portadas";
        String carpetaEbooks = rutaDatosUsuario + File.separator + "ebooks";

        // --- INICIO RUTINA DE MIGRACIÓN ---
        File oldDir = new File(carpetaPortadasAntigua);
        File newDir = new File(carpetaCovers);

        if (oldDir.exists() && oldDir.isDirectory()) {
            if (!newDir.exists()) {
                newDir.mkdirs();
            }

            File[] files = oldDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    File dest = new File(newDir, f.getName());
                    // Movemos el archivo a la nueva carpeta
                    f.renameTo(dest);
                }
            }
            // Una vez vacía, intentamos borrar la carpeta antigua para limpiar
            oldDir.delete();
            LOGGER.info("Migración de portadas completada: de 'portadas' a 'covers'.");
        }
        // --- FIN RUTINA DE MIGRACIÓN ---

        // 3. Procesamos la lista cargada para resolver rutas relativas a absolutas locales
        resolverRutasLocales(libros, carpetaCovers, carpetaEbooks);

        boolean migracionPaginas = false;
        for (Libro libro : libros) {

            // Retrocompatibilidad: Si es un PDF digital pero no tiene número de páginas
            if (libro.isEsDigital() && libro.getPaginasTotales() == 0 && libro.getRutaArchivoDigital() != null) {
                if (libro.getRutaArchivoDigital().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                    File pdfFile = new File(libro.getRutaArchivoDigital());
                    if (pdfFile.exists()) {
                        try (org.apache.pdfbox.pdmodel.PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
                            int pags = pdf.getNumberOfPages();
                            if (pags > 0) {
                                libro.setPaginasTotales(pags);
                                migracionPaginas = true;
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.FINE,
                                    "No se pudo extraer el numero de paginas del PDF antiguo: " + libro.getTitulo());
                        }
                    }
                }
            }

            // Extracción local automática de portada de e-book (EPUB / PDF) si carece de ella
            if (libro.isEsDigital() && libro.getRutaArchivoDigital() != null && !libro.getRutaArchivoDigital().isBlank()) {
                String portada = libro.getPortadaURL();
                boolean sinPortada = (portada == null || portada.isBlank() || portada.contains("default_cover") || !new File(portada).exists());
                // Si ya se intentó sin éxito en esta sesión no se repite (abrir y
                // recorrer el ebook en cada recarga ralentiza la carga)
                if (sinPortada && intentosPortadaFallidos.add(libro.getRutaArchivoDigital())) {
                    boolean portadaExtraida = EbookMetadataService.asegurarPortadaEbook(libro, rutaDatosUsuario);
                    if (portadaExtraida) {
                        migracionPaginas = true;
                        intentosPortadaFallidos.remove(libro.getRutaArchivoDigital());
                    }
                }
            }
        }

        // Si hemos extraído páginas de PDFs antiguos, guardamos el JSON para persistir
        // los cambios
        if (migracionPaginas) {
            guardarLibros(libros);
        }

        // 4. Fusionar datos de tracking de lectura desde progreso_lectura.json
        Map<String, ProgresoLectura> progresos = cargarProgresosLectura();
        for (Libro libro : libros) {
            if (libro.getId() != null && progresos.containsKey(libro.getId())) {
                ProgresoLectura prog = progresos.get(libro.getId());
                libro.setPaginaActual(prog.getPaginaActual());
                if (prog.getPaginasTotales() > 0 && libro.getPaginasTotales() <= 0) {
                    libro.setPaginasTotales(prog.getPaginasTotales());
                }
            } else if (libro.getId() != null && libro.getPaginaActual() > 0) {
                cacheProgresos.put(libro.getId(), new ProgresoLectura(libro.getPaginaActual(), libro.getPaginasTotales()));
            }
        }

        recordarClaves(COL_LIBROS, clavesLibros(libros));
        return libros;
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS PRÉSTAMOS ---
    /**
     * Guarda la lista de préstamos en el archivo JSON correspondiente.
     *
     * @param prestamos Lista de préstamos a guardar.
     */
    public synchronized void guardarPrestamos(List<Prestamo> prestamos) {
        actualizarEliminados(COL_PRESTAMOS, claves(prestamos, JsonManager::obtenerClavePrestamo));
        guardarDatos(prestamos, prestamosDatabasePath, "préstamos");
    }

    /**
     * Carga la lista de préstamos desde el archivo JSON.
     *
     * @return Lista de préstamos cargados.
     */
    public List<Prestamo> cargarPrestamos() {
        Type tipoLista = new TypeToken<ArrayList<Prestamo>>() {
        }.getType();
        List<Prestamo> prestamos = cargarDatos(prestamosDatabasePath, tipoLista, "préstamos");
        recordarClaves(COL_PRESTAMOS, claves(prestamos, JsonManager::obtenerClavePrestamo));
        return prestamos;
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS SOCIOS ---
    /**
     * Guarda la lista de socios en el archivo JSON correspondiente.
     *
     * @param socios Lista de socios a guardar.
     */
    public synchronized void guardarSocios(List<Socio> socios) {
        actualizarEliminados(COL_SOCIOS, claves(socios, JsonManager::obtenerClaveSocio));
        guardarDatos(socios, sociosDatabasePath, "socios");
    }

    /**
     * Carga la lista de socios desde el archivo JSON.
     *
     * @return Lista de socios cargados.
     */
    public List<Socio> cargarSocios() {
        Type tipoLista = new TypeToken<ArrayList<Socio>>() {
        }.getType();
        List<Socio> socios = cargarDatos(sociosDatabasePath, tipoLista, "socios");
        recordarClaves(COL_SOCIOS, claves(socios, JsonManager::obtenerClaveSocio));
        return socios;
    }

    // --- MÉTODOS ESPECÍFICOS PARA LAS ESTANTERIAS ---
    /**
     * Guarda la lista maestra de nombres de estanterías en un archivo JSON.
     *
     * @param estanterias La lista de estanterías a guardar.
     */
    public void guardarEstanterias(List<String> estanterias) {
        guardarDatos(estanterias, estanteriasDatabasePath, "estanterías");
    }

    /**
     * Carga la lista de nombres de estanterías desde un archivo JSON.
     *
     * @return Una lista de Strings con las estanterías. Devuelve una lista
     * vacía si no se encuentra el archivo.
     */
    public List<String> cargarEstanterias() {
        Type tipoLista = new TypeToken<ArrayList<String>>() {
        }.getType();
        return cargarDatos(estanteriasDatabasePath, tipoLista, "estanterías");
    }

    /**
     * Devuelve la ruta del archivo JSON de la biblioteca.
     *
     * @return Ruta del archivo biblioteca.json.
     */
    public String getDatabasePath() {
        return databaseFilePath; // Devuelve la ruta específica del usuario
    }

    /**
     * Devuelve la ruta del directorio de datos del usuario actual.
     *
     * @return Ruta de la carpeta de datos del usuario.
     */
    public String getRutaDatosUsuario() {
        return rutaDatosUsuario;
    }

    /**
     * Devuelve la ruta base de la aplicación (donde está la carpeta 'users').
     *
     * @return Ruta base de la aplicación.
     */
    public static String getAppBaseDirectoryPath() {
        return APP_BASE_DIRECTORY_PATH;
    }

    /**
     * Carga una lista de libros desde un archivo específico seleccionado por el
     * usuario.
     *
     * @param archivo El archivo .json seleccionado.
     * @return La lista de libros contenida en ese archivo.
     */
    public List<Libro> importarLibrosDesdeArchivo(File archivo) {
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();

        try (Reader reader = new FileReader(archivo, java.nio.charset.StandardCharsets.UTF_8)) {
            List<Libro> librosImportados = gson.fromJson(reader, tipoLista);

            if (librosImportados == null) {
                return new ArrayList<>();
            }
            return librosImportados;

        } catch (IOException | JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error al importar libros desde archivo: " + archivo.getName(), e);
            return new ArrayList<>(); // Lista vacía para indicar error, nunca null
        }
    }

    /**
     * Guarda la lista de libros en un archivo específico seleccionado por el
     * usuario (Exportar).
     *
     * @param archivo El archivo destino.
     * @param libros La lista de libros a guardar.
     * @return true si se guardó correctamente, false si falló.
     */
    public boolean exportarLibros(File archivo, List<Libro> libros) {
        try (Writer writer = new FileWriter(archivo, java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(prepararLibrosParaGuardar(libros), writer);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al exportar libros al archivo: " + archivo.getName(), e);
            return false;
        }
    }

    /**
     * Guarda las preferencias de la aplicación (tema, maximizado, etc.) en
     * JSON.
     *
     * @param preferences Mapa de claves-valor con las preferencias.
     */
    public void guardarPreferencias(Map<String, String> preferences) {
        // Guardado atómico: un corte de luz a mitad no deja el archivo vacío
        guardarObjeto(preferences, preferencesFilePath, "preferencias", false);
    }

    /**
     * Carga las preferencias de la aplicación.
     *
     * @return Mapa con las preferencias guardadas, o valores por defecto.
     */
    public Map<String, String> cargarPreferencias() {
        File file = new File(preferencesFilePath);
        if (!file.exists()) {
            Map<String, String> defaultPrefs = new HashMap<>();
            defaultPrefs.put("tema", "claro");
            defaultPrefs.put("maximized", "false"); // Valor por defecto: no maximizado
            return defaultPrefs;
        }

        try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            Type tipoMapa = new TypeToken<HashMap<String, String>>() {
            }.getType();
            Map<String, String> preferences = gson.fromJson(reader, tipoMapa);
            // Si el archivo existe pero está vacío o corrupto, devolvemos valores por
            // defecto
            if (preferences == null) {
                Map<String, String> defaultPrefs = new HashMap<>();
                defaultPrefs.put("tema", "claro");
                defaultPrefs.put("maximized", "false");
                return defaultPrefs;
            }
            return preferences;
        } catch (IOException | JsonParseException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar preferencias. Usando por defecto.", e);
            Map<String, String> defaultPrefs = new HashMap<>();
            defaultPrefs.put("tema", "claro");
            defaultPrefs.put("maximized", "false");
            return defaultPrefs;
        }
    }

    /**
     * Guarda la lista de deseos en el archivo de base de datos.
     *
     * @param deseos lista de libros que se guardarán en deseos
     */
    public synchronized void guardarDeseos(List<Libro> deseos) {
        actualizarEliminados(COL_DESEOS, clavesLibros(deseos));
        guardarObjeto(prepararLibrosParaGuardar(deseos), deseosDatabasePath, "lista de deseos");
    }

    /**
     * Carga la lista de deseos desde el archivo de base de datos. Resuelve las
     * rutas relativas contra el directorio de datos local.
     *
     * @return Lista de libros en la lista de deseos.
     */
    public List<Libro> cargarDeseos() {
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();
        List<Libro> deseos = cargarDatos(deseosDatabasePath, tipoLista, "lista de deseos");
        if (deseos != null) {
            String carpetaCovers = rutaDatosUsuario + File.separator + "covers";
            String carpetaEbooks = rutaDatosUsuario + File.separator + "ebooks";
            resolverRutasLocales(deseos, carpetaCovers, carpetaEbooks);
            recordarClaves(COL_DESEOS, clavesLibros(deseos));
            return deseos;
        }
        return new ArrayList<>();
    }

    // --- MÉTODOS MODULARES PARA EL TRACKING DE LECTURA (PERSISTENCIA LIGERA) ---
    /**
     * Carga el mapa de progreso de lectura desde el archivo JSON secundario
     * {@code progreso_lectura.json}. Almacena los resultados en caché en
     * memoria.
     *
     * @return Mapa asociativo de ID de libro a su {@link ProgresoLectura}.
     */
    public Map<String, ProgresoLectura> cargarProgresosLectura() {
        Type tipoMapa = new TypeToken<HashMap<String, ProgresoLectura>>() {
        }.getType();
        Map<String, ProgresoLectura> datos = cargarObjeto(progresoLecturaFilePath, tipoMapa, "progresos de lectura");
        if (datos != null) {
            cacheProgresos.clear();
            cacheProgresos.putAll(datos);
        }
        return new HashMap<>(cacheProgresos);
    }

    /**
     * Guarda el progreso de lectura de un libro de forma atómica en el archivo
     * ultraligero {@code progreso_lectura.json}, sin reescribir ni tocar el
     * archivo principal {@code biblioteca.json}.
     *
     * @param libroId Identificador único (UUID) del libro.
     * @param paginaActual Página alcanzada por el usuario.
     * @param paginasTotales Páginas totales del libro.
     */
    public void guardarProgresoLectura(String libroId, int paginaActual, int paginasTotales) {
        if (libroId == null || libroId.isBlank()) {
            return;
        }
        cacheProgresos.put(libroId, new ProgresoLectura(paginaActual, paginasTotales));
        guardarObjeto(new HashMap<>(cacheProgresos), progresoLecturaFilePath, "progreso de lectura");
    }

    /**
     * Guarda el progreso de lectura con debounce de 400ms en el archivo
     * ultraligero {@code progreso_lectura.json}.
     * <p>
     * Es la opción óptima para el visor EPUB/lector digital, ya que un usuario
     * que pasa páginas frecuentemente nunca bloquea la aplicación ni provoca
     * escrituras masivas de megabytes en disco.
     * </p>
     *
     * @param libroId Identificador único (UUID) del libro.
     * @param paginaActual Página actual alcanzada.
     * @param paginasTotales Páginas totales del libro.
     */
    public void guardarProgresoLecturaDebounced(String libroId, int paginaActual, int paginasTotales) {
        if (libroId == null || libroId.isBlank()) {
            return;
        }
        cacheProgresos.put(libroId, new ProgresoLectura(paginaActual, paginasTotales));

        if (pendingProgresoSaveFuture != null && !pendingProgresoSaveFuture.isDone()) {
            pendingProgresoSaveFuture.cancel(false);
        }

        Map<String, ProgresoLectura> copia = new HashMap<>(cacheProgresos);
        pendingProgresoSaveFuture = syncScheduler.schedule(
                () -> guardarObjeto(copia, progresoLecturaFilePath, "progreso de lectura"),
                400, TimeUnit.MILLISECONDS);
    }

    /**
     * Fuerza la escritura en disco de cualquier progreso de lectura pendiente
     * en el temporizador debounced. Útil al cerrar ventanas de lectura o salir.
     */
    public void flushProgresoLectura() {
        if (pendingProgresoSaveFuture != null && !pendingProgresoSaveFuture.isDone()) {
            pendingProgresoSaveFuture.cancel(false);
        }
        if (!cacheProgresos.isEmpty()) {
            guardarObjeto(new HashMap<>(cacheProgresos), progresoLecturaFilePath, "progreso de lectura");
        }
    }

    /**
     * Devuelve la ruta del archivo secundario progreso_lectura.json.
     *
     * @return Ruta completa al archivo de progreso.
     */
    public String getProgresoLecturaPath() {
        return progresoLecturaFilePath;
    }
}
