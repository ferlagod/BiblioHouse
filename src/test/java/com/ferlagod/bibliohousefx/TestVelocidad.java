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

/**
 * Pruebas de rendimiento para operaciones masivas de guardado y carga de
 * libros. Mide el tiempo requerido para manejar grandes volúmenes de datos en
 * JsonManager.
 *
 * @author Fernando Lago Dávila
 * @version 1.5
 */
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

    /**
     * Prueba de rendimiento: guarda y carga 10,000 libros. Establece umbrales
     * máximos aceptables para operaciones en milisegundos.
     */
    @Test
    @Tag("performance")
    void benchmarkGuardarCargarMuchosLibros() {
        int cantidad = 10000;
        List<Libro> libros = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            libros.add(new Libro("Titulo " + i, "Autor " + i, "Editorial " + i, "2023", "Gen", "123", ""));
        }

        // Mide tiempo de escritura
        long startWrite = System.currentTimeMillis();
        jsonManager.guardarLibros(libros);
        long endWrite = System.currentTimeMillis();
        long writeDuration = endWrite - startWrite;

        System.out.println("Time to write " + cantidad + " books: " + writeDuration + "ms");

        long startRead = System.currentTimeMillis();
        List<Libro> cargados = jsonManager.cargarLibros();
        long endRead = System.currentTimeMillis();
        long readDuration = endRead - startRead;

        System.out.println("Time to read " + cantidad + " books: " + readDuration + "ms");

        assertEquals(cantidad, cargados.size());

        // Mide tiempo de lectura
        assertTrue(writeDuration < 5000, "Writing 10k books took too long (" + writeDuration + "ms)");
        assertTrue(readDuration < 3000, "Reading 10k books took too long (" + readDuration + "ms)");
    }
}
