package com.bibliohouse.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio encargado de gestionar las búsquedas unificadas en APIs externas
 * (Google Books, OpenLibrary, Inventaire).
 */
public class BusquedaService {

    private static final Logger LOGGER = Logger.getLogger(BusquedaService.class.getName());

    /**
     * Busca un libro en varios proveedores de forma asíncrona con un timeout de 5 segundos.
     *
     * @param query El texto a buscar (título, autor o ISBN).
     * @return Un CompletableFuture que resolverá con la lista de libros encontrados.
     */
    public CompletableFuture<List<Libro>> ejecutarBusquedaGlobalAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            var f1 = CompletableFuture.supplyAsync(() -> OpenLibraryCliente.buscarLibros(query));
            var f2 = CompletableFuture.supplyAsync(() -> GoogleBooksCliente.buscarLibros(query));
            var f3 = CompletableFuture.supplyAsync(() -> InventaireCliente.buscarLibros(query));

            try {
                CompletableFuture.allOf(f1, f2, f3).get(5, TimeUnit.SECONDS);
                List<Libro> unidos = new ArrayList<>();
                if (!f1.isCompletedExceptionally()) unidos.addAll(f1.get());
                if (!f2.isCompletedExceptionally()) unidos.addAll(f2.get());
                if (!f3.isCompletedExceptionally()) unidos.addAll(f3.get());
                return unidos;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.log(Level.WARNING, "Timeout o error en la búsqueda global de libros para la query: " + query, e);
                return new ArrayList<Libro>();
            }
        });
    }

    /**
     * Motor de búsqueda masiva silencioso para recuperar portadas faltantes.
     * Rastrea las 3 APIs en paralelo y devuelve la primera URL válida que encuentre.
     * 
     * @param query El título o ISBN a buscar.
     * @return La URL de la portada encontrada, o cadena vacía si no encuentra ninguna.
     */
    public String buscarImagenEnApisMasivo(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        try {
            CompletableFuture<List<Libro>> futureGoogle = CompletableFuture
                    .supplyAsync(() -> GoogleBooksCliente.buscarLibros(query))
                    .completeOnTimeout(new ArrayList<>(), 3, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en GoogleBooks buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture<List<Libro>> futureOpenLib = CompletableFuture
                    .supplyAsync(() -> OpenLibraryCliente.buscarLibros(query))
                    .completeOnTimeout(new ArrayList<>(), 3, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en OpenLibrary buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture<List<Libro>> futureInventaire = CompletableFuture
                    .supplyAsync(() -> InventaireCliente.buscarLibros(query))
                    .completeOnTimeout(new ArrayList<>(), 3, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Fallo en Inventaire buscando portada", ex);
                        return new ArrayList<>();
                    });

            CompletableFuture.allOf(futureGoogle, futureOpenLib, futureInventaire).join();

            if (futureGoogle.get() != null) {
                for (Libro lib : futureGoogle.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover")) {
                        return img;
                    }
                }
            }
            if (futureOpenLib.get() != null) {
                for (Libro lib : futureOpenLib.get()) {
                    String img = lib.getPortadaURL();
                    if (img != null && !img.trim().isEmpty() && !img.contains("default_cover") && !img.contains("-S.jpg")) {
                        return img.replace("-M.jpg", "-L.jpg");
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
