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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la persistencia modular y de alto rendimiento
 * del seguimiento de lectura en BiblioHouse.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
class TestPersistenciaProgresoLectura {

    @TempDir
    File tempDir;

    private JsonManager jsonManager;
    private File userDir;

    @BeforeEach
    void setUp() {
        userDir = new File(tempDir, "user_data");
        jsonManager = new JsonManager(userDir.getAbsolutePath());
    }

    @Test
    void testGuardarYCargarProgresoModular() {
        Libro libro = new Libro("Cien Años de Soledad", "Gabriel García Márquez", "Sudamericana", "1967", "Novela", "9780307474728", "");
        libro.setPaginasTotales(450);
        libro.setPaginaActual(0);
        jsonManager.guardarLibros(Collections.singletonList(libro));

        File bibliotecaFile = new File(jsonManager.getDatabasePath());
        assertTrue(bibliotecaFile.exists(), "biblioteca.json debe existir");
        long bibliotecaMtimeAntes = bibliotecaFile.lastModified();

        // Actualizamos el progreso de lectura con el método modular ligero
        jsonManager.guardarProgresoLectura(libro.getId(), 145, 450);

        File progresoFile = new File(jsonManager.getProgresoLecturaPath());
        assertTrue(progresoFile.exists(), "progreso_lectura.json debe haberse creado");

        // biblioteca.json NO debe haberse modificado
        assertEquals(bibliotecaMtimeAntes, bibliotecaFile.lastModified(),
                "biblioteca.json no debe reescribirse al actualizar solo el progreso de lectura");

        // Al recargar la biblioteca completa, el progreso debe fusionarse automáticamente
        List<Libro> librosCargados = jsonManager.cargarLibros();
        assertEquals(1, librosCargados.size());
        assertEquals(145, librosCargados.get(0).getPaginaActual(),
                "La página actual debe haberse recuperado desde progreso_lectura.json");
    }

    @Test
    void testGuardarProgresoDebouncedYFlush() {
        Libro libro = new Libro("El Quijote", "Miguel de Cervantes", "Francisco de Robles", "1605", "Clásico", "9788424116286", "");
        libro.setPaginasTotales(800);
        jsonManager.guardarLibros(Collections.singletonList(libro));

        // Múltiples escrituras debounced simulando pasar páginas rápidamente
        for (int pag = 1; pag <= 25; pag++) {
            jsonManager.guardarProgresoLecturaDebounced(libro.getId(), pag, 800);
        }

        // Forzar volcado (flush)
        jsonManager.flushProgresoLectura();

        Map<String, ProgresoLectura> mapa = jsonManager.cargarProgresosLectura();
        assertTrue(mapa.containsKey(libro.getId()));
        assertEquals(25, mapa.get(libro.getId()).getPaginaActual());
        assertEquals(800, mapa.get(libro.getId()).getPaginasTotales());
    }

    @Test
    void testRendimientoSimuladoGranEscala() {
        // Simular una biblioteca de 500 libros
        List<Libro> granCatalogo = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Libro l = new Libro("Libro Volumen #" + i, "Autor " + i, "Editorial", "2020", "Tema", "ISBN-" + i, "");
            l.setReseña("Reseña extensa con descripción detallada para simular el tamaño real del catálogo en disco " + i);
            granCatalogo.add(l);
        }

        // Guardado inicial del catálogo completo
        jsonManager.guardarLibros(granCatalogo);

        Libro libroLeido = granCatalogo.get(10);

        // Medir tiempo de 50 actualizaciones de página con guardado modular ligero
        long startModular = System.nanoTime();
        for (int p = 1; p <= 50; p++) {
            jsonManager.guardarProgresoLectura(libroLeido.getId(), p, 300);
        }
        long durationModularMs = (System.nanoTime() - startModular) / 1_000_000;

        // Verificar consistencia final
        List<Libro> recargados = jsonManager.cargarLibros();
        Libro lEncontrado = recargados.stream().filter(l -> l.getId().equals(libroLeido.getId())).findFirst().orElseThrow();
        assertEquals(50, lEncontrado.getPaginaActual());

        // La persistencia modular de 50 escrituras debe completarse en tiempo despreciable
        assertTrue(durationModularMs < 5000, "50 escrituras modulares deben ser extremadamente rápidas");
    }

    @Test
    void testResilienciaProgresoLecturaCorrupto() throws Exception {
        Libro libro = new Libro("Rayuela", "Julio Cortázar", "Sudamericana", "1963", "Boom", "9788437604947", "");
        libro.setPaginasTotales(600);
        libro.setPaginaActual(88);
        jsonManager.guardarLibros(Collections.singletonList(libro));

        // Corromper intencionadamente el archivo de progreso
        File progresoFile = new File(jsonManager.getProgresoLecturaPath());
        Files.writeString(progresoFile.toPath(), "{ json_totalmente_invalido !!! ");

        // Cargar libros no debe lanzar excepción y debe mantener los datos base de biblioteca.json
        assertDoesNotThrow(() -> {
            List<Libro> libros = jsonManager.cargarLibros();
            assertEquals(1, libros.size());
            assertEquals(88, libros.get(0).getPaginaActual());
        });
    }
}
