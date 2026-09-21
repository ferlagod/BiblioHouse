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

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar el servidor HTTP embebido de streaming
 * de archivos EPUB en LectorDigitalController.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
class TestLectorDigitalStreaming {

    @TempDir
    File tempDir;

    private LectorDigitalController controller;
    private File archivoEpub;

    @BeforeEach
    void setUp() throws Exception {
        controller = new LectorDigitalController();
        archivoEpub = new File(tempDir, "libro_prueba.epub");
        byte[] contenidoEpub = "PK\u0003\u0004mimetypeapplication/epub+zipContenidoSimuladoDeLibroIlustrado".getBytes();
        Files.write(archivoEpub.toPath(), contenidoEpub);
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.detenerServidor();
        }
    }

    @Test
    void testIniciarYDetenerServidorLocalStreaming() throws Exception {
        String urlString = controller.iniciarServidorLocal(archivoEpub);
        assertNotNull(urlString);
        assertTrue(urlString.startsWith("http://127.0.0.1:"), "La URL debe pertenecer al loopback local");
        assertTrue(urlString.endsWith("/book.epub"), "La URL debe terminar en /book.epub");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), "El código HTTP debe ser 200 OK");
        assertEquals("application/epub+zip", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

        byte[] contenidoEsperado = Files.readAllBytes(archivoEpub.toPath());
        assertArrayEquals(contenidoEsperado, response.body(), "El contenido transmitido debe ser exactamente idéntico al archivo en disco");

        // Detener el servidor
        controller.detenerServidor();

        // Verificar que tras detener el servidor no responde peticiones
        assertThrows(Exception.class, () -> {
            client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }, "Una vez detenido el servidor, las peticiones deben ser rechazadas");
    }

    @Test
    void testReutilizacionYCambioDeArchivo() throws Exception {
        File segundoArchivo = new File(tempDir, "segundo_libro.epub");
        Files.write(segundoArchivo.toPath(), "PK\u0003\u0004SegundoLibroContenido".getBytes());

        String url1 = controller.iniciarServidorLocal(archivoEpub);
        assertNotNull(url1);

        // Iniciar de nuevo con otro archivo debe cerrar el servidor anterior limpiamente
        String url2 = controller.iniciarServidorLocal(segundoArchivo);
        assertNotNull(url2);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(url2))
                .GET()
                .build();

        HttpResponse<byte[]> response2 = client.send(request2, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response2.statusCode());
        assertArrayEquals(Files.readAllBytes(segundoArchivo.toPath()), response2.body());
    }
}
