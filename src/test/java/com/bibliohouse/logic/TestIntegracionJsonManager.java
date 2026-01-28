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
 * @author Fernando Lago Dávila
 * @version 1.1
 */
class TestIntegracionJsonManager {

    @TempDir
    File tempDir;

    private JsonManager jsonManager;
    private File userDir;

    @BeforeEach
    void setUp() {
        // Use a temporary directory for the user's data to simulate integration with
        // the file system
        // without affecting the actual user home directory.
        userDir = new File(tempDir, "user_data");
        jsonManager = new JsonManager(userDir.getAbsolutePath());
    }

    @Test
    void testGuardarYCargarLibros() {
        // Create a test book
        Libro libro = new Libro("Titulo Test", "Autor Test", "Editorial Test", "2023", "Ficción", "1234567890", "");

        // Save the book
        jsonManager.guardarLibros(Collections.singletonList(libro));

        // Load the books back
        List<Libro> librosCargados = jsonManager.cargarLibros();

        // Verify persistence
        assertNotNull(librosCargados);
        assertEquals(1, librosCargados.size());

        Libro loadedLibro = librosCargados.get(0);
        assertEquals("Titulo Test", loadedLibro.getTitulo());
        assertEquals("Autor Test", loadedLibro.getAutor());
    }

    @Test
    void testGuardarYCargarPreferencias() {
        // Save some preferences
        jsonManager.guardarPreferencias(Collections.singletonMap("tema", "oscuro"));

        // Load them back
        var prefs = jsonManager.cargarPreferencias();

        // Verify
        assertEquals("oscuro", prefs.get("tema"));
    }
}