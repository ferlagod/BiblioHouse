package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TestVelocidad {

    @TempDir
    File tempDir;

    private JsonManager jsonManager;
    private File userDir;

    @BeforeEach
    void setUp() {
        userDir = new File(tempDir, "user_perf_data");
        jsonManager = new JsonManager(userDir.getAbsolutePath());
    }

    @Test
    @Tag("performance")
    void benchmarkGuardarCargarMuchosLibros() {
        // Generate 10,000 books
        int cantidad = 10000;
        List<Libro> libros = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            libros.add(new Libro("Titulo " + i, "Autor " + i, "Editorial " + i, "2023", "Gen", "123", ""));
        }

        // Measure write time
        long startWrite = System.currentTimeMillis();
        jsonManager.guardarLibros(libros);
        long endWrite = System.currentTimeMillis();
        long writeDuration = endWrite - startWrite;

        System.out.println("Time to write " + cantidad + " books: " + writeDuration + "ms");

        // Measure read time
        long startRead = System.currentTimeMillis();
        List<Libro> cargados = jsonManager.cargarLibros();
        long endRead = System.currentTimeMillis();
        long readDuration = endRead - startRead;

        System.out.println("Time to read " + cantidad + " books: " + readDuration + "ms");

        assertEquals(cantidad, cargados.size());

        // Assert reasonable performance thresholds (adjust as needed for target
        // hardware)
        // e.g., Writing 10k items shouldn't take more than 2 seconds (2000ms) on modern
        // SSD
        // Reading is usually faster.
        assertTrue(writeDuration < 5000, "Writing 10k books took too long (" + writeDuration + "ms)");
        assertTrue(readDuration < 3000, "Reading 10k books took too long (" + readDuration + "ms)");
    }
}
