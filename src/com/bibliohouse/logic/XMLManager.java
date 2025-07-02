/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestiona la lectura y escritura de la colección de libros en un archivo XML
 * utilizando la librería XStream.
 *
 * @author ferlagod
 */
public class XMLManager {

    private static final Logger LOGGER = Logger.getLogger(XMLManager.class.getName());
    // Se define una ruta de guardado en la carpeta de usuario para ser multiplataforma
    private static final String APP_DIRECTORY_PATH = System.getProperty("user.home") + File.separator + "BiblioHouse";
    private static final String DATABASE_FILE_PATH = APP_DIRECTORY_PATH + File.separator + "biblioteca.xml";
    private static final String PRESTAMOS_DATABASE_PATH = APP_DIRECTORY_PATH + File.separator + "prestamos.xml";
    private final XStream xstream;

    /**
     * Constructor para XMLManager. Inicializa XStream y configura los permisos
     * de seguridad necesarios.
     */
    public XMLManager() {
        xstream = new XStream(new StaxDriver());

// Configuración de seguridad de XStream
        xstream.addPermission(NoTypePermission.NONE);
        xstream.addPermission(NullPermission.NULL);
        xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xstream.allowTypeHierarchy(Collection.class);
        xstream.allowTypes(new Class[]{com.bibliohouse.logic.Libro.class});

        // Permitimos las clases Libro, Prestamo y LocalDate 
        xstream.allowTypes(new Class[]{
            com.bibliohouse.logic.Libro.class,
            com.bibliohouse.logic.Prestamo.class,
            java.time.LocalDate.class
        });

        xstream.alias("biblioteca", List.class);
        xstream.alias("libro", com.bibliohouse.logic.Libro.class);

        xstream.alias("prestamos", List.class);
        xstream.alias("prestamo", com.bibliohouse.logic.Prestamo.class);

        // Asegurarse de que el directorio de la aplicación existe
        crearDirectorioSiNoExiste();
    }

    /**
     * Crea el directorio de la aplicación si no existe.
     */
    private void crearDirectorioSiNoExiste() {
        File appDir = new File(APP_DIRECTORY_PATH);
        if (!appDir.exists()) {
            LOGGER.log(Level.INFO, "El directorio de la aplicación no existe, creándolo en: {0}", APP_DIRECTORY_PATH);
            boolean created = appDir.mkdirs();
            if (!created) {
                LOGGER.log(Level.SEVERE, "No se pudo crear el directorio de la aplicación.");
            }
        }
    }

    /**
     * Guarda una lista de préstamos en un archivo XML especificado por la ruta
     * PRESTAMOS_DATABASE_PATH.
     *
     * Registra información sobre la operación, incluyendo el número de
     * préstamos guardados y la ruta del archivo.
     *
     *
     * @param prestamos La lista de préstamos a guardar en el archivo XML. No
     * debe ser nula.
     */
    public void guardarPrestamos(List<Prestamo> prestamos) {
        try (FileOutputStream fos = new FileOutputStream(PRESTAMOS_DATABASE_PATH)) {
            xstream.toXML(prestamos, fos);
            LOGGER.log(Level.INFO, "Se han guardado {0} préstamos en {1}", new Object[]{prestamos.size(), PRESTAMOS_DATABASE_PATH});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar los préstamos en el archivo XML.", e);
        }
    }

    /**
     * Carga una lista de préstamos desde un archivo XML especificado por la
     * ruta PRESTAMOS_DATABASE_PATH. Utiliza la biblioteca XStream para
     * deserializar el contenido del archivo XML a una lista de préstamos.
     * Registra el número de préstamos cargados.
     *
     * @return Una lista de préstamos cargados desde el archivo XML. Si el
     * archivo no existe o hay un error, se devuelve una lista vacía.
     */
    public List<Prestamo> cargarPrestamos() {
        File file = new File(PRESTAMOS_DATABASE_PATH);
        if (!file.exists()) {
            LOGGER.info("El archivo de préstamos no existe. Se creará uno nuevo.");
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(PRESTAMOS_DATABASE_PATH)) {
            @SuppressWarnings("unchecked")
            List<Prestamo> prestamos = (List<Prestamo>) xstream.fromXML(fis);

            LOGGER.log(Level.INFO, "Se han cargado {0} préstamos desde {1}", new Object[]{prestamos != null ? prestamos.size() : 0, PRESTAMOS_DATABASE_PATH});
            return prestamos != null ? prestamos : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el archivo de préstamos.", e);
            return new ArrayList<>();
        }
    }

    /**
     * Guarda una lista de libros en un archivo XML.
     *
     * @param libros La lista de libros a guardar.
     */
    public void guardarLibros(List<Libro> libros) {
        try (FileOutputStream fos = new FileOutputStream(DATABASE_FILE_PATH)) {
            xstream.toXML(libros, fos);
            LOGGER.log(Level.INFO, "Se han guardado {0} libros correctamente en {1}", new Object[]{libros.size(), DATABASE_FILE_PATH});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error irrecuperable al guardar los libros en el archivo XML.", e);
        }
    }

    /**
     * Carga una lista de libros desde un archivo XML.
     *
     * @return La lista de libros cargada desde el archivo XML.
     */
    public List<Libro> cargarLibros() {
        File file = new File(DATABASE_FILE_PATH);
        if (!file.exists()) {
            LOGGER.log(Level.INFO, "El archivo {0} no existe. Se creará uno nuevo en el primer guardado.", DATABASE_FILE_PATH);
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(DATABASE_FILE_PATH)) {
            @SuppressWarnings("unchecked")
            List<Libro> libros = (List<Libro>) xstream.fromXML(fis);

            LOGGER.log(Level.INFO, "Se han cargado {0} libros desde {1}", new Object[]{libros != null ? libros.size() : 0, DATABASE_FILE_PATH});
            return libros != null ? libros : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar/leer el archivo XML. Puede estar corrupto. Se devuelve una lista vacía.", e);
            return new ArrayList<>();
        }
    }

    /**
     * Devuelve la ruta completa del archivo de la base de datos.
     *
     * @return Ruta del archivo de la base de datos.
     */
    public String getDatabasePath() {
        return DATABASE_FILE_PATH;
    }
}
