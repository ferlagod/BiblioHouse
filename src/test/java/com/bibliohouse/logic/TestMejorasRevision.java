package com.bibliohouse.logic;

import com.bibliohouse.utils.EpubStreamServer;
import com.bibliohouse.utils.TextoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las correcciones de la revisión: marcas de borrado en la
 * sincronización, fusión de socios y préstamos, validación de nombres remotos,
 * protección XXE, búsqueda sin tildes, token del servidor EPUB y volcado de
 * guardados pendientes al cerrar.
 */
class TestMejorasRevision {

    @TempDir
    File tempDir;

    private File userDir;

    @BeforeEach
    void setUp() {
        userDir = new File(tempDir, "usuario");
        userDir.mkdirs();
    }

    private static Libro libro(String id, String titulo, long modificado) {
        Libro l = new Libro();
        l.setId(id);
        l.setTitulo(titulo);
        l.setUltimaModificacion(modificado);
        return l;
    }

    @Test
    @DisplayName("Borrar un libro genera una marca de borrado y no reaparece al fusionar con la nube")
    void testBorradoNoResucitaTrasFusion() throws IOException {
        JsonManager jm = new JsonManager(userDir.getAbsolutePath());
        List<Libro> iniciales = new ArrayList<>(List.of(libro("a", "A", 1000), libro("b", "B", 1000)));
        jm.guardarLibros(iniciales);
        List<Libro> cargados = new ArrayList<>(jm.cargarLibros());

        cargados.removeIf(l -> l.getId().equals("b"));
        jm.guardarLibros(cargados);

        Map<String, Long> marcas = jm.cargarEliminados().get(JsonManager.COL_LIBROS);
        assertNotNull(marcas);
        assertTrue(marcas.containsKey("b"), "El libro borrado debe quedar marcado");

        // La nube todavía tiene el libro "b" (versión anterior al borrado)
        String remoto = jm.serializarLibros(List.of(libro("a", "A", 1000), libro("b", "B", 1000)));
        jm.fusionarConRemoto("biblioteca.json", new StringReader(remoto));

        List<Libro> resultado = jm.cargarLibros();
        assertEquals(1, resultado.size(), "El libro borrado no debe volver desde la nube");
        assertEquals("a", resultado.get(0).getId());
        jm.shutdown();
    }

    @Test
    @DisplayName("Un libro editado en otro dispositivo después de borrarse aquí se conserva")
    void testEdicionPosteriorAlBorradoGana() {
        Map<String, Long> marcas = Map.of("b", 5000L);
        List<Libro> fusion = JsonManager.fusionarColecciones(
                List.of(libro("a", "A", 1000)),
                List.of(libro("b", "B editado", 9000)),
                marcas);
        assertEquals(2, fusion.size());
    }

    @Test
    @DisplayName("Un borrado masivo accidental no genera marcas que se propagarían a la nube")
    void testBorradoMasivoNoGeneraMarcas() {
        JsonManager jm = new JsonManager(userDir.getAbsolutePath());
        List<Libro> muchos = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            muchos.add(libro("id" + i, "T" + i, 1000));
        }
        jm.guardarLibros(muchos);
        jm.cargarLibros();
        jm.guardarLibros(new ArrayList<>());

        Map<String, Long> marcas = jm.cargarEliminados().get(JsonManager.COL_LIBROS);
        assertTrue(marcas == null || marcas.isEmpty());
        jm.shutdown();
    }

    @Test
    @DisplayName("Las marcas de borrado se unen quedándose con el instante más reciente")
    void testFusionarEliminados() {
        Map<String, Map<String, Long>> a = Map.of("libros", Map.of("x", 10L, "y", 50L));
        Map<String, Map<String, Long>> b = Map.of("libros", Map.of("x", 30L), "socios", Map.of("7", 5L));
        Map<String, Map<String, Long>> r = JsonManager.fusionarEliminados(a, b);
        assertEquals(30L, r.get("libros").get("x"));
        assertEquals(50L, r.get("libros").get("y"));
        assertEquals(5L, r.get("socios").get("7"));
    }

    @Test
    @DisplayName("Préstamos: la fusión une ambos lados y un préstamo devuelto gana a uno abierto")
    void testFusionPrestamos() {
        Prestamo abierto = new Prestamo();
        abierto.setId("p1");
        Prestamo devuelto = new Prestamo();
        devuelto.setId("p1");
        devuelto.setFechaDevolucion(LocalDate.of(2026, 1, 2));
        Prestamo otro = new Prestamo();
        otro.setId("p2");

        List<Prestamo> r = JsonManager.fusionarPorClave(List.of(abierto), List.of(devuelto, otro),
                JsonManager::obtenerClavePrestamo,
                (l, rem) -> l.getFechaDevolucion() == null && rem.getFechaDevolucion() != null ? rem : l,
                null);
        assertEquals(2, r.size());
        assertNotNull(r.get(0).getFechaDevolucion(), "El préstamo devuelto debe prevalecer");
    }

    @Test
    @DisplayName("Socios: un socio borrado no vuelve desde la nube")
    void testSocioBorradoNoVuelve() throws IOException {
        JsonManager jm = new JsonManager(userDir.getAbsolutePath());
        jm.guardarSocios(new ArrayList<>(List.of(new Socio("Ana", "A", "1", "C", 1), new Socio("Luis", "L", "2", "C", 2))));
        List<Socio> socios = new ArrayList<>(jm.cargarSocios());
        socios.removeIf(s -> s.getNumeroSocio() == 2);
        jm.guardarSocios(socios);

        String remoto = "[{\"nombre\":\"Ana\",\"numeroSocio\":1},{\"nombre\":\"Luis\",\"numeroSocio\":2},{\"nombre\":\"Eva\",\"numeroSocio\":3}]";
        jm.fusionarConRemoto("socios.json", new StringReader(remoto));

        List<Socio> resultado = jm.cargarSocios();
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().noneMatch(s -> s.getNumeroSocio() == 2));
        assertTrue(resultado.stream().anyMatch(s -> s.getNumeroSocio() == 3), "Los socios nuevos remotos se añaden");
        jm.shutdown();
    }

    @Test
    @DisplayName("Los nombres remotos con rutas o '..' se rechazan")
    void testNombresRemotosPeligrosos() {
        File covers = new File(userDir, "covers");
        covers.mkdirs();
        assertNull(NextCloudSyncService.resolverDentroDe(covers, "../../.zshrc"));
        assertNull(NextCloudSyncService.resolverDentroDe(covers, "..\\evil.exe"));
        assertNull(NextCloudSyncService.resolverDentroDe(covers, ".."));
        assertNull(NextCloudSyncService.resolverDentroDe(covers, "C:evil"));
        assertNull(NextCloudSyncService.resolverDentroDe(covers, ".oculto"));
        assertNotNull(NextCloudSyncService.resolverDentroDe(covers, "1234.jpg"));
    }

    @Test
    @DisplayName("Se rechaza HTTP hacia servidores que solo parecen locales")
    void testHttpSoloEnLocalhost() {
        assertThrows(IllegalArgumentException.class,
                () -> new NextCloudSyncService("http://localhost.malicioso.com", "u", "p"));
        assertDoesNotThrow(() -> new NextCloudSyncService("http://localhost:8080", "u", "p"));
        assertDoesNotThrow(() -> new NextCloudSyncService("https://nube.ejemplo.com", "u", "p"));
    }

    @Test
    @DisplayName("Un EPUB con entidades externas (XXE) no puede leer archivos locales")
    void testEpubConXxeNoFiltraArchivos() throws IOException {
        File secreto = new File(tempDir, "secreto.txt");
        Files.writeString(secreto.toPath(), "CONTENIDO_SECRETO", StandardCharsets.UTF_8);

        File epub = new File(tempDir, "xxe.epub");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(epub))) {
            zos.putNextEntry(new ZipEntry("mimetype"));
            zos.write("application/epub+zip".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zos.write(("<?xml version=\"1.0\"?><container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                    + "<rootfiles><rootfile full-path=\"content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("content.opf"));
            zos.write(("<?xml version=\"1.0\"?><!DOCTYPE p [<!ENTITY xxe SYSTEM \"" + secreto.toURI() + "\">]>"
                    + "<package xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><metadata><dc:title>&xxe;</dc:title></metadata></package>")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        Libro libro = EbookMetadataService.crearLibroDesdeArchivo(epub, userDir.getAbsolutePath());
        String titulo = libro != null && libro.getTitulo() != null ? libro.getTitulo() : "";
        assertFalse(titulo.contains("CONTENIDO_SECRETO"), "El contenido de archivos locales no debe filtrarse");
    }

    @Test
    @DisplayName("La búsqueda ignora mayúsculas y tildes")
    void testBusquedaSinTildes() {
        String q = TextoUtil.normalizarBusqueda("garcia");
        assertTrue(TextoUtil.contiene("Gabriel García Márquez", q));
        assertTrue(TextoUtil.contiene("ÑANDÚ", TextoUtil.normalizarBusqueda("nandu")));
        assertFalse(TextoUtil.contiene(null, q));
        assertTrue(TextoUtil.contiene("lo que sea", ""));
    }

    @Test
    @DisplayName("El servidor EPUB solo responde en su ruta secreta")
    void testServidorEpubRequiereToken() throws Exception {
        File epub = new File(tempDir, "libro.epub");
        Files.write(epub.toPath(), "PK\u0003\u0004contenido".getBytes(StandardCharsets.UTF_8));
        EpubStreamServer server = new EpubStreamServer(epub);
        try {
            String url = server.start();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<Void> sinToken = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/book.epub")).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(404, sinToken.statusCode());
            HttpResponse<Void> conToken = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(200, conToken.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Cerrar el gestor escribe a disco el guardado diferido pendiente")
    void testShutdownVuelcaGuardadoPendiente() {
        JsonManager jm = new JsonManager(userDir.getAbsolutePath());
        jm.guardarLibrosDebounced(List.of(libro("z", "Pendiente", 1000)));
        jm.shutdown(); // inmediatamente, antes de los 500 ms del debounce

        JsonManager otro = new JsonManager(userDir.getAbsolutePath());
        List<Libro> cargados = otro.cargarLibros();
        assertEquals(1, cargados.size(), "El cambio pendiente no debe perderse al cerrar");
        assertEquals("z", cargados.get(0).getId());
        otro.shutdown();
    }
}
