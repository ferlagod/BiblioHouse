package com.bibliohouse.logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio encargado de gestionar las búsquedas unificadas en APIs externas
 * (Google Books, OpenLibrary, Inventaire).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class BusquedaService {

    private static final Logger LOGGER = Logger.getLogger(BusquedaService.class.getName());

    /**
     * Busca un libro en varios proveedores de forma asíncrona con un timeout de
     * 5 segundos.
     *
     * @param query El texto a buscar (título, autor o ISBN).
     * @return Un CompletableFuture que resolverá con la lista de libros
     * encontrados.
     */
    public CompletableFuture<List<Libro>> ejecutarBusquedaGlobalAsync(String query) {
        var f1 = CompletableFuture.supplyAsync(() -> OpenLibraryCliente.buscarLibros(query))
                .completeOnTimeout(new ArrayList<>(), 10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Error en OpenLibrary", ex);
                    return new ArrayList<>();
                });
        var f2 = CompletableFuture.supplyAsync(() -> GoogleBooksCliente.buscarLibros(query))
                .completeOnTimeout(new ArrayList<>(), 10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Error en Google Books", ex);
                    return new ArrayList<>();
                });
        var f3 = CompletableFuture.supplyAsync(() -> InventaireCliente.buscarLibros(query))
                .completeOnTimeout(new ArrayList<>(), 10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Error en Inventaire", ex);
                    return new ArrayList<>();
                });

        return CompletableFuture.allOf(f1, f2, f3).thenApply(v -> {
            List<Libro> unidos = new ArrayList<>();
            unidos.addAll(f1.join());
            unidos.addAll(f2.join());
            unidos.addAll(f3.join());
            return unidos;
        });
    }

    private static final java.net.http.HttpClient HTTP_CLIENT = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    /**
     * Comprueba de forma instantánea si OpenLibrary dispone de una portada
     * directa para un ISBN. Es significativamente más rápido (< 400ms) que
     * realizar una búsqueda semántica completa.
     *
     * @param isbn ISBN-10 o ISBN-13 del libro.
     * @return URL de la portada si existe, o cadena vacía si no está
     * disponible.
     */
    public String buscarImagenPorIsbnDirecto(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return "";
        }
        String limpio = isbn.replaceAll("[^0-9Xx]", "");
        if (limpio.length() != 10 && limpio.length() != 13) {
            return "";
        }

        try {
            String url = "https://covers.openlibrary.org/b/isbn/" + limpio + "-L.jpg?default=false";
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 BiblioHouse/2.1")
                    .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            java.net.http.HttpResponse<Void> resp = HTTP_CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                // Verificar que no sea una respuesta de 0 bytes o de error
                long len = resp.headers().firstValueAsLong("content-length").orElse(-1L);
                if (len < 0 || len > 200) {
                    return url;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.FINE, "No se pudo consultar portada directa por ISBN: {0}", e.getMessage());
        }
        return "";
    }

    /**
     * Motor de búsqueda masiva silencioso para recuperar portadas faltantes.
     * Rastrea primero por ISBN directo si aplica, y luego las 3 APIs en
     * paralelo.
     *
     * @param query El título o ISBN a buscar.
     * @return La URL de la portada encontrada, o cadena vacía si no encuentra
     * ninguna.
     */
    public String buscarImagenEnApisMasivo(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        String cleanQuery = query.trim();

        // 1. Si parece un ISBN, probar primero la API directa de OpenLibrary
        String limpio = cleanQuery.replaceAll("[^0-9Xx]", "");
        if (limpio.length() == 10 || limpio.length() == 13) {
            String directa = buscarImagenPorIsbnDirecto(limpio);
            if (!directa.isEmpty()) {
                return directa;
            }
        }

        try {
            final String q = cleanQuery;
            CompletableFuture<List<Libro>> futureGoogle = CompletableFuture
                    .supplyAsync(() -> GoogleBooksCliente.buscarLibros(q))
                    .completeOnTimeout(new ArrayList<>(), 7, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en GoogleBooks buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture<List<Libro>> futureOpenLib = CompletableFuture
                    .supplyAsync(() -> OpenLibraryCliente.buscarLibros(q))
                    .completeOnTimeout(new ArrayList<>(), 7, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en OpenLibrary buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture<List<Libro>> futureInventaire = CompletableFuture
                    .supplyAsync(() -> InventaireCliente.buscarLibros(q))
                    .completeOnTimeout(new ArrayList<>(), 7, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en Inventaire buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture.allOf(futureGoogle, futureOpenLib, futureInventaire).join();

            if (futureOpenLib.get() != null) {
                for (Libro lib : futureOpenLib.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover") && !img.contains("-S.jpg")) {
                        return img.replace("-M.jpg", "-L.jpg");
                    }
                }
            }
            if (futureGoogle.get() != null) {
                for (Libro lib : futureGoogle.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
            if (futureInventaire.get() != null) {
                for (Libro lib : futureInventaire.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.WARNING, "Error al buscar imagen masiva", e);
        }
        return "";
    }
}
