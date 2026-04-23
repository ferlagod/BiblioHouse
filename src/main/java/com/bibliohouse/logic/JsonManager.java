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
 * @author Fernando Lago
 * @version 1.6
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

    // Rutas de los archivos JSON que usamos para guardar los datos.
    private final String databaseFilePath;
    private final String prestamosDatabasePath;
    private final String sociosDatabasePath;
    private final String estanteriasDatabasePath;
    private final String deseosDatabasePath;

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
    private final ScheduledExecutorService syncScheduler
            = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "nextcloud-autosync");
                t.setDaemon(true);
                return t;
            });

    /**
     * Referencia al sync pendiente (para cancelarlo si llega otro antes).
     */
    private ScheduledFuture<?> pendingSyncFuture;

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

        // Se configura el Gson para que use el adaptador de fechas
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        // Se crea las carpetas necesarias si no existen
        crearDirectorioBaseSiNoExiste();

        // Registrar shutdown hook para cerrar el ejecutor del auto-sync al salir de la JVM
        Runtime.getRuntime().addShutdownHook(new Thread(syncScheduler::shutdownNow,
                "nextcloud-sync-shutdown"));
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
     * @param lista La lista de objetos que queremos guardar.
     * @param path La ruta del archivo donde se guardará.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     * guardando.
     */
    private <T> void guardarDatos(List<T> lista, String path, String tipoDato) {
        crearDirectorioUsuarioSiNoExiste();

        File archivoFinal = new File(path);
        File archivoTemporal = new File(path + ".tmp");
        File archivoBackup = new File(path + ".bak");

        // 1. Escribir en el archivo TEMPORAL primero
        try (FileWriter writer = new FileWriter(archivoTemporal)) {
            gson.toJson(lista, writer);
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
     * Método genérico para cargar datos desde un archivo JSON.
     *
     * @param path La ruta del archivo que queremos cargar.
     * @param tipoLista El tipo de la lista que esperamos.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     * cargando.
     * @return La lista de objetos cargados desde el archivo. Si hay error,
     * devuelve una lista vacía.
     */
    private <T> List<T> cargarDatos(String path, Type tipoLista, String tipoDato) {
        File file = new File(path);
        File backupFile = new File(path + ".bak");

        // Intentar cargar el archivo principal
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                List<T> lista = gson.fromJson(reader, tipoLista);
                if (lista != null) {
                    LOGGER.log(Level.INFO, "Cargados {0} {1} desde {2}", new Object[]{lista.size(), tipoDato, path});
                    return lista;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar " + path + ". Intentando cargar backup...", e);
            }
        }

        // Si llegamos aquí, o no existe el principal o falló. Intentamos backup.
        if (backupFile.exists()) {
            try (FileReader reader = new FileReader(backupFile)) {
                List<T> lista = gson.fromJson(reader, tipoLista);
                if (lista != null) {
                    LOGGER.log(Level.WARNING, "RECUPERADO: Cargados {0} {1} desde BACKUP {2}",
                            new Object[]{lista.size(), tipoDato, backupFile.getPath()});
                    return lista;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al cargar backup " + backupFile.getPath(), e);
            }
        }

        LOGGER.log(Level.INFO,
                "No se encontraron datos válidos para {0} (ni original ni backup). Se devuelve lista vacía.", tipoDato);
        return new ArrayList<>();
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
     * Carga la lista de libros desde el archivo JSON. Si un libro no tiene
     * estanterías asignadas, se inicializa como una lista vacía.
     *
     * @return Lista de libros cargados.
     */
    /**
     * Carga la lista de libros y realiza una migración automática de portadas
     * de la carpeta antigua 'portadas' a la nueva 'covers' si es necesario.
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

        try (Reader reader = new FileReader(archivo)) {
            List<Libro> librosImportados = gson.fromJson(reader, tipoLista);

            if (librosImportados == null) {
                return new ArrayList<>();
            }
            return librosImportados;

        } catch (IOException | JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error al importar libros desde archivo: " + archivo.getName(), e);
            return null; // Retornamos null para indicar error
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
}
