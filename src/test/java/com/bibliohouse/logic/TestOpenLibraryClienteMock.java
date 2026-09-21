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

import java.lang.reflect.Proxy;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para OpenLibraryCliente sin conexión a internet (100% deterministas)
 * utilizando inyección de HttpSender para aislar por completo las llamadas HTTP de red.
 */
public class TestOpenLibraryClienteMock {

    @AfterEach
    public void tearDown() {
        OpenLibraryCliente.setHttpSender(null);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> crearRespuestaHttp(int statusCode, String body) {
        return (HttpResponse<String>) Proxy.newProxyInstance(
                HttpResponse.class.getClassLoader(),
                new Class<?>[]{HttpResponse.class},
                (proxy, method, args) -> {
                    if ("statusCode".equals(method.getName())) {
                        return statusCode;
                    }
                    if ("body".equals(method.getName())) {
                        return body;
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Debe parsear correctamente una respuesta JSON 200 de OpenLibrary con todos los metadatos")
    public void testBusquedaExitosaConDatosCompletos() {
        String jsonExitoso = "{"
                + "\"docs\": [{"
                + "  \"title\": \"The Hobbit\","
                + "  \"author_name\": [\"J.R.R. Tolkien\"],"
                + "  \"first_publish_year\": 1937,"
                + "  \"publisher\": [\"George Allen & Unwin\"],"
                + "  \"subject\": [\"Fantasy\", \"Adventure\"],"
                + "  \"isbn\": [\"9780261103344\", \"0261103342\"],"
                + "  \"cover_i\": 8407492,"
                + "  \"number_of_pages\": 310"
                + "}]"
                + "}";

        HttpResponse<String> respuesta = crearRespuestaHttp(200, jsonExitoso);
        OpenLibraryCliente.setHttpSender(req -> respuesta);

        List<Libro> resultados = OpenLibraryCliente.buscarLibros("The Hobbit");

        assertNotNull(resultados);
        assertEquals(1, resultados.size());

        Libro libro = resultados.get(0);
        assertEquals("The Hobbit", libro.getTitulo());
        assertEquals("J.R.R. Tolkien", libro.getAutor());
        assertEquals("1937", libro.getAño());
        assertEquals("George Allen & Unwin", libro.getEditorial());
        assertEquals("Fantasy, Adventure", libro.getGenero());
        assertEquals("9780261103344", libro.getIsbn());
        assertEquals("https://covers.openlibrary.org/b/id/8407492-L.jpg", libro.getPortadaURL());
        assertEquals(310, libro.getPaginasTotales());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay documentos en la respuesta")
    public void testRespuestaSinDocs() {
        HttpResponse<String> respuesta = crearRespuestaHttp(200, "{\"docs\": []}");
        OpenLibraryCliente.setHttpSender(req -> respuesta);

        List<Libro> resultados = OpenLibraryCliente.buscarLibros("TerminoInexistente999");

        assertNotNull(resultados);
        assertTrue(resultados.isEmpty());
    }

    @Test
    @DisplayName("Debe retornar lista vacía y no fallar cuando el servidor responde HTTP 500")
    public void testRespuestaErrorHttp500() {
        HttpResponse<String> respuesta = crearRespuestaHttp(500, "Internal Server Error");
        OpenLibraryCliente.setHttpSender(req -> respuesta);

        List<Libro> resultados = OpenLibraryCliente.buscarLibros("CualquierLibro");

        assertNotNull(resultados);
        assertTrue(resultados.isEmpty(), "Ante error 500 debe retornar lista vacía");
    }

    @Test
    @DisplayName("Debe capturar HttpTimeoutException y retornar lista vacía sin arrojar excepción")
    public void testExcepcionDeRedTimeout() {
        OpenLibraryCliente.setHttpSender((OpenLibraryCliente.HttpSender) req -> {
            throw new HttpTimeoutException("Request timed out");
        });

        List<Libro> resultados = OpenLibraryCliente.buscarLibros("LibroConTimeout");

        assertNotNull(resultados);
        assertTrue(resultados.isEmpty(), "Ante timeout de red debe devolver lista vacía");
    }

    @Test
    @DisplayName("Debe seleccionar el primer ISBN con longitud válida (10 o 13 dígitos)")
    public void testPriorizaIsbnValido() {
        // El primer ISBN es inválido (longitud 7), el segundo es válido (13 dígitos)
        String jsonIsbn = "{"
                + "\"docs\": [{"
                + "  \"title\": \"Libro ISBN Test\","
                + "  \"isbn\": [\"1234567\", \"9788445071403\", \"8445071408\"]"
                + "}]"
                + "}";

        HttpResponse<String> respuesta = crearRespuestaHttp(200, jsonIsbn);
        OpenLibraryCliente.setHttpSender(req -> respuesta);

        List<Libro> resultados = OpenLibraryCliente.buscarLibros("ISBN Test");

        assertEquals(1, resultados.size());
        assertEquals("9788445071403", resultados.get(0).getIsbn(), "Debe seleccionar el primer ISBN válido de 13 o 10 dígitos");
    }
}
