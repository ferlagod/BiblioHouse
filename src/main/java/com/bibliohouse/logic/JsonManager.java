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
import java.util.logging.Logger;

/**
 * Clase que gestiona la lectura y escritura de datos de la aplicación en
 * archivos JSON. Usa la librería Gson para manejar la serialización y
 * deserialización de objetos.
 *
 * @author Fernando Lago
 * @version 1.0
 *
 */
public class JsonManager {

    private static final Logger LOGGER = Logger.getLogger(JsonManager.class.getName());
    // Ruta base donde se guardan todos los datos de la aplicación.
    private static final String APP_BASE_DIRECTORY_PATH = System.getProperty("user.home") + File.separator
            + "BiblioHouse";

    // Ruta específica para los datos del usuario.
    private final String rutaDatosUsuario;
    private final String preferencesFilePath; // <-- Declaración de preferencias

    // Rutas de los archivos JSON que usamos para guardar los datos.
    private final String databaseFilePath;
    private final String prestamosDatabasePath;
    private final String sociosDatabasePath;
    private final String estanteriasDatabasePath;

    private final Gson gson;

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
     * @param lista La lista de objetos que queremos guardar.
     * @param path La ruta del archivo donde se guardará.
     * @param tipoDato Un String que describe qué tipo de datos estamos
     * guardando.
     */
    private <T> void guardarDatos(List<T> lista, String path, String tipoDato) {
        // Asegurarse de que el directorio del usuario exista antes de intentar escribir
        crearDirectorioUsuarioSiNoExiste();

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(lista, writer);
            LOGGER.log(Level.FINE, "Guardados {0} {1} en {2}", new Object[]{lista.size(), tipoDato, path});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar " + tipoDato + " en " + path, e);

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
        if (!file.exists()) {
            LOGGER.log(Level.INFO, "El archivo de {0} no existe para este usuario en {1}. Se devuelve lista vacía.",
                    new Object[]{tipoDato, path});
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(file)) {
            List<T> lista = gson.fromJson(reader, tipoLista);
            if (lista == null) {
                LOGGER.log(Level.WARNING,
                        "El archivo JSON {0} en {1} parece estar vacío o corrupto. Devolviendo lista vacía.",
                        new Object[]{tipoDato, path});
                return new ArrayList<>();
            }
            LOGGER.log(Level.INFO, "Cargados {0} {1} desde {2}", new Object[]{lista.size(), tipoDato, path});
            return lista;
        } catch (JsonParseException e) { // Captura específica para errores de formato JSON
            LOGGER.log(Level.SEVERE,
                    "Error de formato al cargar " + tipoDato + " desde " + path + ". El archivo podría estar corrupto.",
                    e);
            return new ArrayList<>(); // Devuelve lista vacía para evitar fallos mayores
        } catch (IOException e) { // Otros errores de lectura
            LOGGER.log(Level.SEVERE, "Error de E/S al cargar " + tipoDato + " desde " + path, e);
            return new ArrayList<>();
        } catch (Exception e) { // Captura genérica por si acaso
            LOGGER.log(Level.SEVERE, "Error inesperado al cargar " + tipoDato + " desde " + path, e);
            return new ArrayList<>();
        }
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
    public List<Libro> cargarLibros() {
        Type tipoLista = new TypeToken<ArrayList<Libro>>() {
        }.getType();
        List<Libro> libros = cargarDatos(databaseFilePath, tipoLista, "libros");
        // Se comprueba que los libros tengan todos los campos necesarios
        for (Libro libro : libros) {
            if (libro.getEstanterias() == null) {
                libro.setEstanterias(new ArrayList<>());
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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Libro> librosImportados = gson.fromJson(reader, tipoLista);

            if (librosImportados == null) {
                return new ArrayList<>();
            }
            return librosImportados;

        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error al importar libros: " + e.getMessage());
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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(libros, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Error al exportar libros: " + e.getMessage());
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
}
