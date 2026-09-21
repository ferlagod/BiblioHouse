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
import java.util.HashMap;
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
 * @version 2.1
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

    private final Map<String, ProgresoLectura> cacheProgresos = new java.util.concurrent.ConcurrentHashMap<>();

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

    // El shutdown hook se registra una sola vez a nivel de clase, no por instancia,
    // para evitar acumular hooks huérfanos cuando la UI se recarga (p.ej. al
    // cambiar idioma).
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Noop: cada instancia es un daemon thread; la JVM los mata al salir.
            // Este hook existe solo para loguear si fuera necesario en el futuro.
        }, "bibliohousefx-shutdown"));
    }

    /**
     * Referencia al sync pendiente (para cancelarlo si llega otro antes).
     */
    private ScheduledFuture<?> pendingSyncFuture;

    /**
     * Referencia al guardado de libros pendiente (debounce).
     */
    private ScheduledFuture<?> pendingSaveFuture;

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
     * Cierra el ejecutor del auto-sync de forma ordenada. Debe llamarse al
     * salir de la aplicación para no dejar hilos huérfanos.
     */
    public void shutdown() {
        syncScheduler.shutdownNow();
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
         * @param json    El JSON con la fecha en formato String.
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
         * @param json    JSON con la fecha en formato String.
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
     *                         usuario actual.
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

        // Se configura el Gson para que use el adaptador de fechas
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        // Se crea las carpetas necesarias si no existen
        crearDirectorioBaseSiNoExiste();

        crearDirectorioUsuarioSiNoExiste();
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
        String os = System.getProperty("os.name").toLowerCase();
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
     * Método genérico para guardar cualquier lista de objetos en un archivo
     * JSON.
     *
     * @param lista    La lista de objetos que queremos guardar.
     * @param path     La ruta del archivo donde se guardará.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     *                 guardando.
     */
    /**
     * Método genérico para guardar cualquier objeto (lista, mapa, etc.) en un
     * archivo JSON de forma atómica y segura con backup.
     *
     * @param objeto   El objeto a serializar.
     * @param path     La ruta del archivo donde se guardará.
     * @param tipoDato Descripción del dato para el logging.
     */
    private synchronized void guardarObjeto(Object objeto, String path, String tipoDato) {
        crearDirectorioUsuarioSiNoExiste();

        File archivoFinal = new File(path);
        File archivoTemporal = new File(path + ".tmp");
        File archivoBackup = new File(path + ".bak");

        // 1. Escribir en el archivo TEMPORAL primero
        try (FileWriter writer = new FileWriter(archivoTemporal)) {
            gson.toJson(objeto, writer);
            // Forzamos el volcado al disco físico
            writer.flush();
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
        if (autoSyncTask != null) {
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
     * @param lista    La lista de objetos que queremos guardar.
     * @param path     La ruta del archivo donde se guardará.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     *                 guardando.
     */
    private synchronized <T> void guardarDatos(List<T> lista, String path, String tipoDato) {
        guardarObjeto(lista, path, tipoDato);
    }

    /**
     * Método genérico para cargar un objeto o mapa desde un archivo JSON.
     *
     * @param path       La ruta del archivo.
     * @param tipoObjeto El tipo del objeto esperado.
     * @param tipoDato   Descripción del dato para el logging.
     * @return El objeto deserializado, o {@code null} si falla o no existe.
     */
    private <T> T cargarObjeto(String path, Type tipoObjeto, String tipoDato) {
        File file = new File(path);
        File backupFile = new File(path + ".bak");

        // Intentar cargar el archivo principal
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                T obj = gson.fromJson(reader, tipoObjeto);
                if (obj != null) {
                    LOGGER.log(Level.FINE, "Cargado {0} desde {1}", new Object[] { tipoDato, path });
                    return obj;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar " + path + ". Intentando cargar backup...", e);
            }
        }

        // Si llegamos aquí, o no existe el principal o falló. Intentamos backup.
        if (backupFile.exists()) {
            try (FileReader reader = new FileReader(backupFile)) {
                T obj = gson.fromJson(reader, tipoObjeto);
                if (obj != null) {
                    LOGGER.log(Level.WARNING, "RECUPERADO: Cargado {0} desde BACKUP {1}",
                            new Object[] { tipoDato, backupFile.getPath() });
                    return obj;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar backup " + backupFile.getPath(), e);
            }
        }

        return null;
    }

    /**
     * Método genérico para cargar datos desde un archivo JSON en forma de lista.
     *
     * @param path      La ruta del archivo que queremos cargar.
     * @param tipoLista El tipo de la lista que esperamos.
     * @param tipoDato  Un String que describe qué tipo de datos estamos
     *                  cargando.
     * @return La lista de objetos cargados desde el archivo. Si hay error,
     *         devuelve una lista vacía.
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
     * Guarda la lista de libros en el archivo JSON correspondiente.
     *
     * @param libros Lista de libros a guardar.
     */
    public void guardarLibros(List<Libro> libros) {
        guardarDatos(libros, databaseFilePath, "libros");
    }

    /**
     * Guarda la lista de libros con debounce: toma una copia defensiva
     * inmediatamente pero retrasa la escritura a disco 500ms. Si se llama de
     * nuevo antes de que expire el plazo, el guardado anterior se cancela.
     * Ideal para operaciones rápidas y repetitivas (cambio de estado, etc.).
     *
     * @param libros Lista de libros a guardar.
     */
    public void guardarLibrosDebounced(List<Libro> libros) {
        // Copia defensiva inmediata (barata, O(n) punteros)
        List<Libro> copia = new ArrayList<>(libros);
        // Cancelar guardado pendiente si existe
        if (pendingSaveFuture != null && !pendingSaveFuture.isDone()) {
            pendingSaveFuture.cancel(false);
        }
        // Programar guardado real en 500ms
        pendingSaveFuture = syncScheduler.schedule(
                () -> guardarDatos(copia, databaseFilePath, "libros"),
                500, TimeUnit.MILLISECONDS);
    }

    /**
     * Carga la lista de libros desde el archivo JSON. Si un libro no tiene
     * estanterías asignadas, se inicializa como una lista vacía.
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

        // 3. Procesamos la lista cargada para reparar rutas dinámicas
        boolean migracionPaginas = false;
        for (Libro libro : libros) {
            if (libro.getEstanterias() == null) {
                libro.setEstanterias(new ArrayList<>());
            }

            String url = libro.getPortadaURL();
            if (url != null && !url.isEmpty() && !url.startsWith("http") && !url.contains("default_cover")) {
                // Reparamos la ruta para que apunte SIEMPRE a la carpeta 'covers' del PC actual
                File archivo = new File(url);
                libro.setPortadaURL(carpetaCovers + File.separator + archivo.getName());
            }

            // Retrocompatibilidad: Si es un PDF digital pero no tiene número de páginas
            if (libro.isEsDigital() && libro.getPaginasTotales() == 0 && libro.getRutaArchivoDigital() != null) {
                if (libro.getRutaArchivoDigital().toLowerCase().endsWith(".pdf")) {
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

        return libros;
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS PRÉSTAMOS ---
    /**
     * Guarda la lista de préstamos en el archivo JSON correspondiente.
     *
     * @param prestamos Lista de préstamos a guardar.
     */
    public void guardarPrestamos(List<Prestamo> prestamos) {
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
        return cargarDatos(prestamosDatabasePath, tipoLista, "préstamos");
    }

    // --- MÉTODOS ESPECÍFICOS PARA LOS SOCIOS ---
    /**
     * Guarda la lista de socios en el archivo JSON correspondiente.
     *
     * @param socios Lista de socios a guardar.
     */
    public void guardarSocios(List<Socio> socios) {
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
        return cargarDatos(sociosDatabasePath, tipoLista, "socios");
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
     *         vacía si no se encuentra el archivo.
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

        try (Reader reader = new FileReader(archivo)) {
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
     * @param libros  La lista de libros a guardar.
     * @return true si se guardó correctamente, false si falló.
     */
    public boolean exportarLibros(File archivo, List<Libro> libros) {
        try (Writer writer = new FileWriter(archivo)) {
            gson.toJson(libros, writer);
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
        crearDirectorioUsuarioSiNoExiste();

        try (FileWriter writer = new FileWriter(preferencesFilePath)) {
            gson.toJson(preferences, writer);
            LOGGER.log(Level.INFO, "Preferencias guardadas.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar preferencias.", e);
        }
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

        try (FileReader reader = new FileReader(file)) {
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
    public void guardarDeseos(List<Libro> deseos) {
        guardarDatos(deseos, deseosDatabasePath, "lista de deseos");
    }

    /**
     * Carga la lista de deseos desde el archivo de base de datos.
     *
     * @return Lista de libros en la lista de deseos.
     */
    public List<Libro> cargarDeseos() {
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();
        return cargarDatos(deseosDatabasePath, tipoLista, "lista de deseos");
    }

    // --- MÉTODOS MODULARES PARA EL TRACKING DE LECTURA (PERSISTENCIA LIGERA) ---

    /**
     * Carga el mapa de progreso de lectura desde el archivo JSON secundario
     * {@code progreso_lectura.json}. Almacena los resultados en caché en memoria.
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
     * @param libroId        Identificador único (UUID) del libro.
     * @param paginaActual   Página alcanzada por el usuario.
     * @param paginasTotales Páginas totales del libro.
     */
    public void guardarProgresoLectura(String libroId, int paginaActual, int paginasTotales) {
        if (libroId == null || libroId.isBlank()) return;
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
     * @param libroId        Identificador único (UUID) del libro.
     * @param paginaActual   Página actual alcanzada.
     * @param paginasTotales Páginas totales del libro.
     */
    public void guardarProgresoLecturaDebounced(String libroId, int paginaActual, int paginasTotales) {
        if (libroId == null || libroId.isBlank()) return;
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
