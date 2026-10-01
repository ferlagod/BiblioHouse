/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.bibliohouse.logic;

import com.bibliohouse.utils.ImageLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la resolución inteligente de portadas,
 * compatibilidad de rutas multiplataforma y sincronización de portadas.
 */
class TestSincronizacionYPortadas {

    @TempDir
    File tempDir;

    private File userDir;
    private File coversDir;
    private JsonManager jsonManager;

    @BeforeEach
    void setUp() {
        userDir = new File(tempDir, "user_data");
        coversDir = new File(userDir, "covers");
        coversDir.mkdirs();
        jsonManager = new JsonManager(userDir.getAbsolutePath());
        ImageLoader.setCacheDir(coversDir.getAbsolutePath());
    }

    @Test
    void testExtraerNombreArchivoMultiplataforma() {
        // Windows
        assertEquals("portada123.jpg", ImageLoader.extraerNombreArchivo("C:\\Users\\Pepe\\BiblioHouse\\covers\\portada123.jpg"));
        assertEquals("libro.png", ImageLoader.extraerNombreArchivo("D:\\covers\\libro.png"));

        // Unix / Mac
        assertEquals("portada123.jpg", ImageLoader.extraerNombreArchivo("/Users/ferlagod/BiblioHouse/covers/portada123.jpg"));
        assertEquals("libro.png", ImageLoader.extraerNombreArchivo("/home/usuario/.bibliohouse/covers/libro.png"));

        // Relativo
        assertEquals("quijote.jpg", ImageLoader.extraerNombreArchivo("covers/quijote.jpg"));
        assertEquals("quijote.jpg", ImageLoader.extraerNombreArchivo("quijote.jpg"));
        assertEquals("", ImageLoader.extraerNombreArchivo(null));
        assertEquals("", ImageLoader.extraerNombreArchivo("   "));
    }

    @Test
    void testResolverArchivoLocalEnCovers() throws IOException {
        // Creamos una portada real en la carpeta covers local
        File realCover = new File(coversDir, "libro-uuid-1.jpg");
        Files.writeString(realCover.toPath(), "fake image data 12345");

        // Caso 1: Ruta exacta existente
        File res1 = ImageLoader.resolverArchivoLocal(realCover.getAbsolutePath());
        assertNotNull(res1);
        assertEquals(realCover.getAbsolutePath(), res1.getAbsolutePath());

        // Caso 2: Ruta absoluta proveniente de otro PC (Windows)
        String rutaWindows = "C:\\Users\\OtroPC\\BiblioHouse\\covers\\libro-uuid-1.jpg";
        File res2 = ImageLoader.resolverArchivoLocal(rutaWindows);
        assertNotNull(res2, "Debería encontrar la imagen en la carpeta covers local a pesar de la ruta de Windows");
        assertEquals(realCover.getAbsolutePath(), res2.getAbsolutePath());

        // Caso 3: Ruta con extensión diferente (.png en vez de .jpg)
        String rutaDistintaExt = "/tmp/libro-uuid-1.png";
        File res3 = ImageLoader.resolverArchivoLocal(rutaDistintaExt);
        assertNotNull(res3, "Debería resolver la portada existente con extensión alternativa .jpg");
        assertEquals(realCover.getAbsolutePath(), res3.getAbsolutePath());
    }

    @Test
    void testJsonManagerReparaRutasMultiplataforma() throws IOException {
        // Creamos una portada en el directorio covers del usuario actual
        File realCover = new File(coversDir, "9788420412146.jpg");
        Files.writeString(realCover.toPath(), "fake content");

        // Creamos un libro con ruta absoluta de Windows guardada en otro sistema
        Libro libro = new Libro();
        libro.setId("libro-quijote");
        libro.setTitulo("Don Quijote");
        libro.setIsbn("9788420412146");
        libro.setPortadaURL("C:\\Users\\Antiguo\\BiblioHouse\\covers\\9788420412146.jpg");

        List<Libro> lista = new ArrayList<>();
        lista.add(libro);
        jsonManager.guardarLibros(lista);

        // Cargamos los libros: JsonManager debe reparar la ruta automáticamente
        List<Libro> cargados = jsonManager.cargarLibros();
        assertNotNull(cargados);
        assertEquals(1, cargados.size());

        Libro cargado = cargados.get(0);
        assertEquals(realCover.getAbsolutePath(), cargado.getPortadaURL(),
                "La ruta de portada debe apuntar al archivo físico existente en el equipo actual");
    }

    @Test
    void testJsonGuardaRutaRelativaPortable() throws IOException {
        Libro libro = new Libro();
        libro.setId("libro-test-relativo");
        libro.setTitulo("Libro Portable");
        // Ruta absoluta local en memoria
        libro.setPortadaURL("/Users/ferlagod/BiblioHouse/covers/libro-test-relativo.jpg");
        libro.setRutaArchivoDigital("/Users/ferlagod/BiblioHouse/ebooks/libro-test-relativo.epub");

        List<Libro> lista = new ArrayList<>();
        lista.add(libro);
        jsonManager.guardarLibros(lista);

        // Verificamos el contenido en crudo del archivo biblioteca.json
        File jsonFile = new File(userDir, "biblioteca.json");
        assertTrue(jsonFile.exists());
        String contenidoJson = Files.readString(jsonFile.toPath());

        // Debe contener rutas relativas "covers/..." y "ebooks/..."
        assertTrue(contenidoJson.contains("\"covers/libro-test-relativo.jpg\""),
                "El JSON debe almacenar la ruta relativa de la portada: " + contenidoJson);
        assertTrue(contenidoJson.contains("\"ebooks/libro-test-relativo.epub\""),
                "El JSON debe almacenar la ruta relativa del ebook: " + contenidoJson);
        // NO debe contener rutas absolutas del sistema de archivos local
        assertFalse(contenidoJson.contains("/Users/ferlagod"),
                "El JSON no debe contener rutas absolutas de una máquina específica");
    }

    @Test
    void testBusquedaDirectaIsbnValidaFormato() {
        BusquedaService service = new BusquedaService();
        // Si el ISBN es nulo o vacío o inválido, debe devolver vacío sin lanzar excepciones
        assertEquals("", service.buscarImagenPorIsbnDirecto(null));
        assertEquals("", service.buscarImagenPorIsbnDirecto(""));
        assertEquals("", service.buscarImagenPorIsbnDirecto("12345")); // Menor a 10 dígitos
    }

    @Test
    void testNextCloudDeteccionModificacionRemota() throws IOException {
        File tempFile = new File(userDir, "test_check.json");
        Files.writeString(tempFile.toPath(), "{\"version\":1}");
        long localTime = System.currentTimeMillis();
        tempFile.setLastModified(localTime);

        // 1. Archivo local inexistente -> Debe considerarse que remoto debe descargarse
        File noExiste = new File(userDir, "no_existe.json");
        assertTrue(NextCloudSyncService.esRecursoRemotoMasReciente(new Date(localTime), noExiste),
                "Si el archivo local no existe, debe requerir sincronización.");

        // 2. Fecha remota nula -> No se puede determinar si es más reciente
        assertFalse(NextCloudSyncService.esRecursoRemotoMasReciente(null, tempFile),
                "Si la fecha remota es null, no debe considerarse más reciente.");

        // 3. Remoto más reciente por 10 segundos -> Debe detectar modificación
        Date remotoNuevo = new Date(localTime + 10000);
        assertTrue(NextCloudSyncService.esRecursoRemotoMasReciente(remotoNuevo, tempFile),
                "Si el remoto es 10s más nuevo, debe requerir sincronización.");

        // 4. Remoto más antiguo por 10 segundos -> No debe requerir sincronización
        Date remotoViejo = new Date(localTime - 10000);
        assertFalse(NextCloudSyncService.esRecursoRemotoMasReciente(remotoViejo, tempFile),
                "Si el remoto es más antiguo que local, no debe requerir sincronización.");

        // 5. Diferencia dentro del margen de tolerancia (ej: 1 segundo por redondeo FAT/ext4)
        Date remotoJitter = new Date(localTime + 1000);
        assertFalse(NextCloudSyncService.esRecursoRemotoMasReciente(remotoJitter, tempFile),
                "Una diferencia menor o igual a 2000 ms debe considerarse tolerancia y no falso positivo.");
    }

    @Test
    void testAppEventBusCatalogoSincronizado() {
        java.util.concurrent.atomic.AtomicBoolean recibido = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger librosRecibidos = new java.util.concurrent.atomic.AtomicInteger(-1);

        AppEventBus.getInstance().subscribe(AppEventBus.CatalogoSincronizadoEvent.class, e -> {
            recibido.set(true);
            librosRecibidos.set(e.getTotalLibros());
        });

        AppEventBus.getInstance().publish(new AppEventBus.CatalogoSincronizadoEvent(42));

        assertTrue(recibido.get(), "El suscriptor debe recibir CatalogoSincronizadoEvent");
        assertEquals(42, librosRecibidos.get(), "Debe transportar el número total de libros sincronizados");
    }
}
