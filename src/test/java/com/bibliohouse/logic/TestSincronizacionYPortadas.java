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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
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

    @Test
    void testRedimensionamientoYCompresionPortadaGrande() throws IOException {
        // Crear imagen de alta resolución (2000 x 3000 px, típica de escáner o cámara de 6 megapíxeles)
        int anchoOriginal = 2000;
        int altoOriginal = 3000;
        BufferedImage imgGrande = new BufferedImage(anchoOriginal, altoOriginal, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imgGrande.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, anchoOriginal, altoOriginal);
        g.setColor(Color.YELLOW);
        g.fillOval(200, 300, 1600, 2400);
        g.dispose();

        File destino = new File(coversDir, "portada_optimizada.jpg");
        boolean exito = ImageLoader.redimensionarYComprimirPortada(imgGrande, destino);

        assertTrue(exito, "La optimización de la portada debió ejecutarse con éxito.");
        assertTrue(destino.exists(), "El archivo optimizado debe existir en disco.");

        // Validar dimensiones resultantes (máximo 600 px de ancho, conservando ratio 2:3 -> 600 x 900)
        BufferedImage cargada = ImageIO.read(destino);
        assertNotNull(cargada, "La imagen guardada debe ser un JPEG válido legible por ImageIO.");
        assertEquals(ImageLoader.MAX_COVER_WIDTH, cargada.getWidth(), "El ancho debe ser exactamente 600 px.");
        assertEquals(ImageLoader.MAX_COVER_HEIGHT, cargada.getHeight(), "El alto debe ser exactamente 900 px (ratio 2:3).");

        // Validar tamaño en disco: entre 20 KB y 120 KB (típicamente 40-70 KB), nunca los 18 MB crudos
        long bytesDisco = destino.length();
        assertTrue(bytesDisco > 10_000 && bytesDisco < 150_000,
                "El peso del archivo optimizado debe estar en el rango de 20-100 KB, pero fue: " + bytesDisco + " bytes");
    }

    @Test
    void testNoEscalarHaciaArribaPortadaPequena() throws IOException {
        // Imagen ya pequeña (300 x 450 px): no debe escalarse a 600 px
        BufferedImage imgPequena = new BufferedImage(300, 450, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imgPequena.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 300, 450);
        g.dispose();

        File destino = new File(coversDir, "portada_pequena.jpg");
        boolean exito = ImageLoader.redimensionarYComprimirPortada(imgPequena, destino);

        assertTrue(exito);
        BufferedImage cargada = ImageIO.read(destino);
        assertNotNull(cargada);
        assertEquals(300, cargada.getWidth(), "No debe escalarse hacia arriba si es menor a 600 px.");
        assertEquals(450, cargada.getHeight(), "No debe escalarse hacia arriba si es menor a 900 px.");
    }

    @Test
    void testHacerPortadaLocalOfflineOptimizaYRemueveObsoletos() throws IOException {
        // 1. Simular portada original pesada en PNG (1200 x 1800 px)
        File pngOriginal = new File(tempDir, "portada_pesada.png");
        BufferedImage imgPng = new BufferedImage(1200, 1800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imgPng.createGraphics();
        g.setColor(new Color(50, 100, 150, 200)); // Semitransparente
        g.fillRect(0, 0, 1200, 1800);
        g.dispose();
        ImageIO.write(imgPng, "png", pngOriginal);

        // 2. Simular un archivo legacy obsoleto con el mismo ID pero extensión .png en covers
        String bookId = "uuid-libro-1234";
        File coverObsoletoPng = new File(coversDir, bookId + ".png");
        Files.writeString(coverObsoletoPng.toPath(), "datos obsoletos png");
        assertTrue(coverObsoletoPng.exists());

        // 3. Ejecutar hacerPortadaLocalOffline
        String rutaLocal = ImageLoader.hacerPortadaLocalOffline(pngOriginal.getAbsolutePath(), bookId, userDir.getAbsolutePath());

        assertNotNull(rutaLocal);
        assertTrue(rutaLocal.endsWith(bookId + ".jpg"), "La portada debe guardarse con extensión .jpg optimizada.");

        File archivoFinal = new File(rutaLocal);
        assertTrue(archivoFinal.exists(), "El archivo optimizado .jpg debe existir.");

        // Debe haber limpiado el antiguo .png obsoleto con el mismo ID
        assertFalse(coverObsoletoPng.exists(), "El antiguo archivo .png con el mismo ID debió eliminarse.");

        // Comprobar dimensiones del archivo resultante
        BufferedImage bFinal = ImageIO.read(archivoFinal);
        assertNotNull(bFinal);
        assertEquals(600, bFinal.getWidth(), "El ancho debe ser 600 px.");
        assertEquals(900, bFinal.getHeight(), "El alto debe ser 900 px.");
    }
}
