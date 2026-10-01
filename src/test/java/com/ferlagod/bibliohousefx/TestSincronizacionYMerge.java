/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.logic.NextCloudSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la sincronización bidireccional y la fusión
 * por ID y timestamp de última modificación (Merge no destructivo).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class TestSincronizacionYMerge {

    @Test
    @DisplayName("Libro debe inicializar y actualizar su timestamp de última modificación")
    public void testTimestampUltimaModificacion() throws InterruptedException {
        long antes = System.currentTimeMillis();
        Libro libro = new Libro("Cien años de soledad", "Gabriel García Márquez", "Sudamericana", "1967", "Novela", "9780307474728", "");
        long despues = System.currentTimeMillis();

        assertTrue(libro.getUltimaModificacion() >= antes && libro.getUltimaModificacion() <= despues,
                "El libro debe tener un timestamp inicial válido.");

        Thread.sleep(10);
        libro.marcarModificado();
        assertTrue(libro.getUltimaModificacion() > antes,
                "marcarModificado() debe incrementar el timestamp.");
    }

    @Test
    @DisplayName("obtenerClaveUnicaLibro debe priorizar ID, luego ISBN y finalmente Título/Autor")
    public void testClaveUnicaLibro() {
        Libro libroConId = new Libro();
        libroConId.setId("uuid-12345");
        libroConId.setIsbn("978-84-376-0494-7");
        libroConId.setTitulo("Don Quijote");
        libroConId.setAutor("Cervantes");
        assertEquals("uuid-12345", JsonManager.obtenerClaveUnicaLibro(libroConId));

        Libro libroSinId = new Libro();
        libroSinId.setId(null);
        libroSinId.setIsbn("978-84-376-0494-7");
        libroSinId.setTitulo("Don Quijote");
        libroSinId.setAutor("Cervantes");
        assertEquals("isbn:9788437604947", JsonManager.obtenerClaveUnicaLibro(libroSinId));

        Libro libroSinIdNiIsbn = new Libro();
        libroSinIdNiIsbn.setId("");
        libroSinIdNiIsbn.setIsbn("");
        libroSinIdNiIsbn.setTitulo("Ficciones");
        libroSinIdNiIsbn.setAutor("Jorge Luis Borges");
        assertEquals("title:ficciones|jorge luis borges", JsonManager.obtenerClaveUnicaLibro(libroSinIdNiIsbn));
    }

    @Test
    @DisplayName("Fusión de colecciones: conserva libros exclusivos de cada dispositivo (móvil y PC)")
    public void testFusionLibrosExclusivos() {
        // Libro A: Creado en móvil sin conexión
        Libro libroMovil = new Libro("Libro Móvil A", "Autor Móvil", "Editorial", "2024", "Ficción", "111111", "");
        libroMovil.setId("id-libro-a");

        // Libro B: Creado en PC sin conexión
        Libro libroPc = new Libro("Libro PC B", "Autor PC", "Editorial", "2024", "Ficción", "222222", "");
        libroPc.setId("id-libro-b");

        List<Libro> locales = List.of(libroPc);
        List<Libro> remotos = List.of(libroMovil);

        List<Libro> fusionados = JsonManager.fusionarColecciones(locales, remotos);

        assertEquals(2, fusionados.size(), "La lista fusionada debe contener ambos libros.");
        assertTrue(fusionados.stream().anyMatch(l -> "id-libro-a".equals(l.getId())));
        assertTrue(fusionados.stream().anyMatch(l -> "id-libro-b".equals(l.getId())));
    }

    @Test
    @DisplayName("Fusión de colecciones: en caso de conflicto por ID, gana la versión más reciente")
    public void testFusionConflictoPorIdGanaMasReciente() {
        String idComun = "id-libro-comun-100";

        // Versión local antigua (leída hasta página 50 a las 10:00)
        Libro localAntiguo = new Libro("El Aleph", "Jorge Luis Borges", "Losada", "1949", "Cuentos", "333333", "");
        localAntiguo.setId(idComun);
        localAntiguo.setPaginaActual(50);
        localAntiguo.setUltimaModificacion(1000L);

        // Versión remota más reciente (avanzó a la página 120 en el móvil a las 12:00)
        Libro remotoNuevo = new Libro("El Aleph", "Jorge Luis Borges", "Losada", "1949", "Cuentos", "333333", "");
        remotoNuevo.setId(idComun);
        remotoNuevo.setPaginaActual(120);
        remotoNuevo.setUltimaModificacion(2000L);

        // Prueba 1: Remoto es más nuevo -> Gana remoto
        List<Libro> fusionados1 = JsonManager.fusionarColecciones(List.of(localAntiguo), List.of(remotoNuevo));
        assertEquals(1, fusionados1.size());
        assertEquals(120, fusionados1.get(0).getPaginaActual(), "Debe prevalecer la versión remota por ser más reciente.");

        // Prueba 2: Local es más nuevo -> Gana local
        localAntiguo.setUltimaModificacion(3000L);
        localAntiguo.setPaginaActual(150);
        List<Libro> fusionados2 = JsonManager.fusionarColecciones(List.of(localAntiguo), List.of(remotoNuevo));
        assertEquals(1, fusionados2.size());
        assertEquals(150, fusionados2.get(0).getPaginaActual(), "Debe prevalecer la versión local por ser más reciente.");
    }

    @Test
    @DisplayName("coleccionTieneCambiosNuevos detecta adiciones o modificaciones frente a la base")
    public void testColeccionTieneCambiosNuevos() {
        Libro libro1 = new Libro("Libro 1", "Autor", "Ed", "2020", "Gen", "123", "");
        libro1.setId("id-1");
        libro1.setUltimaModificacion(1000L);

        Libro libro2 = new Libro("Libro 2", "Autor", "Ed", "2020", "Gen", "456", "");
        libro2.setId("id-2");
        libro2.setUltimaModificacion(1000L);

        List<Libro> base = List.of(libro1);
        List<Libro> conNuevoLibro = List.of(libro1, libro2);

        // Si se agregó libro2, debe reportar cambios nuevos
        assertTrue(NextCloudSyncService.coleccionTieneCambiosNuevos(conNuevoLibro, base));

        // Si la lista es idéntica, no hay cambios nuevos
        assertFalse(NextCloudSyncService.coleccionTieneCambiosNuevos(base, base));

        // Si se modificó libro1 con un timestamp mayor
        Libro libro1Modificado = new Libro("Libro 1", "Autor", "Ed", "2020", "Gen", "123", "");
        libro1Modificado.setId("id-1");
        libro1Modificado.setUltimaModificacion(2000L);

        assertTrue(NextCloudSyncService.coleccionTieneCambiosNuevos(List.of(libro1Modificado), base));
    }

    @Test
    @DisplayName("esRecursoRemotoMasReciente valida correctamente diferencias de tiempo")
    public void testEsRecursoRemotoMasReciente(@TempDir Path tempDir) throws IOException {
        File localFile = Files.createFile(tempDir.resolve("test_sync.json")).toFile();
        long ahora = System.currentTimeMillis();
        localFile.setLastModified(ahora);

        // Remoto 5 segundos en el futuro -> Más reciente
        Date remotoFuturo = new Date(ahora + 5000);
        assertTrue(NextCloudSyncService.esRecursoRemotoMasReciente(remotoFuturo, localFile));

        // Remoto dentro de la tolerancia de 2 segundos -> No es considerado más reciente
        Date remotoCercano = new Date(ahora + 1000);
        assertFalse(NextCloudSyncService.esRecursoRemotoMasReciente(remotoCercano, localFile));

        // Remoto más antiguo -> No es más reciente
        Date remotoPasado = new Date(ahora - 5000);
        assertFalse(NextCloudSyncService.esRecursoRemotoMasReciente(remotoPasado, localFile));

        // Archivo local inexistente -> Remoto siempre es más reciente
        File localInexistente = tempDir.resolve("no_existe.json").toFile();
        assertTrue(NextCloudSyncService.esRecursoRemotoMasReciente(new Date(), localInexistente));
    }

    @Test
    @DisplayName("fusionarYGuardarLibros persiste en disco de forma segura sin pérdida de datos")
    public void testFusionYGuardadoEnDisco(@TempDir Path tempDir) {
        String dataDir = tempDir.toAbsolutePath().toString();
        JsonManager jsonManager = new JsonManager(dataDir);

        Libro libroLocal = new Libro("Local", "Autor", "Ed", "2021", "Ficción", "777", "");
        libroLocal.setId("uuid-local");
        jsonManager.guardarLibros(List.of(libroLocal));

        Libro libroRemoto = new Libro("Remoto", "Autor", "Ed", "2021", "Ficción", "888", "");
        libroRemoto.setId("uuid-remoto");

        List<Libro> unificados = jsonManager.fusionarYGuardarLibros(List.of(libroRemoto));

        assertEquals(2, unificados.size());
        List<Libro> recargados = jsonManager.cargarLibros();
        assertEquals(2, recargados.size(), "Los datos guardados en disco deben contener ambos libros.");
        assertTrue(recargados.stream().anyMatch(l -> "uuid-local".equals(l.getId())));
        assertTrue(recargados.stream().anyMatch(l -> "uuid-remoto".equals(l.getId())));
    }
}
