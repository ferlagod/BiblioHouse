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

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clase TestIntegracionJsonManager.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
class TestIntegracionJsonManager {

    @TempDir
    File tempDir;

    private JsonManager jsonManager;
    private File userDir;

    /**
     * Configuración inicial para las pruebas. Crea un directorio temporal para
     * almacenar los datos del usuario, simulando la integración con el sistema
     * de archivos sin modificar el directorio home real del usuario. Inicializa
     * el gestor JSON con la ruta del directorio temporal.
     */
    @BeforeEach
    void setUp() {
        // Usa un directorio temporal para los datos del usuario y simular la integración con
        // el sistema de archivos sin afectar al directorio home real del usuario.
        userDir = new File(tempDir, "user_data");
        jsonManager = new JsonManager(userDir.getAbsolutePath());
    }

    /**
     * Prueba el guardado y carga de libros en formato JSON. Crea un libro de
     * prueba, lo guarda mediante el gestor JSON, luego lo carga y verifica que
     * los datos se hayan persistido correctamente.
     */
    @Test
    void testGuardarYCargarLibros() {
        // Crea un libro de prueba
        Libro libro = new Libro("Titulo Test", "Autor Test", "Editorial Test", "2023", "Ficción", "1234567890", "");

        // Guarda el libro
        jsonManager.guardarLibros(Collections.singletonList(libro));

        // Carga los libros de vuelta
        List<Libro> librosCargados = jsonManager.cargarLibros();

        // Verifica la persistencia de los datos
        assertNotNull(librosCargados);
        assertEquals(1, librosCargados.size());

        Libro libroCargado = librosCargados.get(0);
        assertEquals("Titulo Test", libroCargado.getTitulo());
        assertEquals("Autor Test", libroCargado.getAutor());
    }

    /**
     * Prueba el guardado y carga de preferencias de usuario. Almacena un
     * conjunto de preferencias (ej. tema de interfaz) y verifica que se
     * recuperen correctamente.
     */
    @Test
    void testGuardarYCargarPreferencias() {
        // Guarda preferencias de prueba
        jsonManager.guardarPreferencias(Collections.singletonMap("tema", "oscuro"));

        // Carga las preferencias guardadas
        var preferencias = jsonManager.cargarPreferencias();

        // Verifica que el valor se haya persistido
        assertEquals("oscuro", preferencias.get("tema"));
    }

}
