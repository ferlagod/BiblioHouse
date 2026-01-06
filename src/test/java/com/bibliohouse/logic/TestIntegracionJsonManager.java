package com.bibliohouse.logic;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

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
