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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Cliente para buscar libros en la API de OpenLibrary. Hace peticiones HTTP y
 * devuelve los resultados como objetos Libro.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class OpenLibraryCliente {

    //Logger para registrar eventos y errores.
    private static final Logger LOGGER = Logger.getLogger(OpenLibraryCliente.class.getName());
    // URL base de la API de OpenLibrary.
    private static final String API_BASE_URL = "https://openlibrary.org/search.json";
    // Campos que solicitamos a la API para no traer datos innecesarios.
    private static final String FIELDS_TO_GET = "title,author_name,first_publish_year,publisher,subject,isbn,cover_i";

    /**
     * Busca libros en OpenLibrary. Si no encuentra nada, devuelve una lista
     * vacía.
     *
     * @param busqueda Texto a buscar.
     * @return Lista de libros.
     */
    public static List<Libro> buscarLibros(String busqueda) {
        // Solo buscamos en OpenLibrary.
        // Si no hay resultados, devolvemos lista vacía y punto.
        return buscarEnOpenLibrary(busqueda);
    }

    /**
     * Busca libros en OpenLibrary específicamente.
     *
     * @param terminoDeBusqueda Término a buscar (título, autor, ISBN, etc.)
     * @return Lista de libros encontrados. Lista vacía si no hay resultados o
     * hay error.
     */
    private static List<Libro> buscarEnOpenLibrary(String terminoDeBusqueda) {
        List<Libro> librosEncontrados = new ArrayList<>();

        try {
            // Codificar el término para URL (espacios se convierten en %20, etc.)
            String terminoCodificado = URLEncoder.encode(terminoDeBusqueda, StandardCharsets.UTF_8);
            String urlCompleta = String.format("%s?q=%s&fields=%s&limit=20", API_BASE_URL, terminoCodificado,
                    FIELDS_TO_GET);

            LOGGER.log(Level.INFO, "Realizando búsqueda en OpenLibrary: {0}", urlCompleta);

            // Crear cliente HTTP con timeout de 30 segundos
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // Crear petición HTTP con headers necesarios
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "BiblioHouse/1.0 (ferlagod@example.com)")
                    .header("Accept", "application/json")
                    .build();

            // Enviar petición y obtener respuesta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Comprobar que la respuesta es OK (código 200)
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING,
                        "La API de OpenLibrary devolvió un código de estado no exitoso: {0}. Cuerpo: {1}",
                        new Object[]{response.statusCode(), response.body()});
                return librosEncontrados;
            }

            // Parsear JSON de respuesta
            JSONObject jsonResponse = new JSONObject(response.body());

            // Verificar que hay resultados
            if (!jsonResponse.has("docs")) {
                LOGGER.log(Level.INFO, "La respuesta de OpenLibrary no contiene documentos.");
                return librosEncontrados;
            }

            JSONArray docs = jsonResponse.getJSONArray("docs");

            // Procesar cada libro encontrado
            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);

                // Obtener título
                String titulo = doc.optString("title", "Sin título");

                // Obtener autor (toma el primero si hay varios)
                String autor = "Desconocido";
                if (doc.has("author_name")) {
                    JSONArray autores = doc.optJSONArray("author_name");
                    if (autores != null && autores.length() > 0) {
                        autor = autores.getString(0);
                    }
                }

                // Obtener año de publicación
                String anio = String.valueOf(doc.optInt("first_publish_year", 0));

                // Obtener editorial (toma la primera si hay varias)
                String editorial = "";
                if (doc.has("publisher")) {
                    JSONArray publishers = doc.optJSONArray("publisher");
                    if (publishers != null && publishers.length() > 0) {
                        editorial = publishers.getString(0);
                    }
                }

                // Obtener géneros (máximo 5, separados por comas)
                String genero = "";
                if (doc.has("subject")) {
                    JSONArray subjects = doc.optJSONArray("subject");
                    if (subjects != null) {
                        genero = IntStream.range(0, Math.min(subjects.length(), 5))
                                .mapToObj(subjects::optString)
                                .collect(Collectors.joining(", "));
                    }
                }

                // Obtener ISBN válido (13 o 10 dígitos)
                String isbn = "";
                if (doc.has("isbn")) {
                    JSONArray isbns = doc.optJSONArray("isbn");
                    if (isbns != null) {
                        for (int j = 0; j < isbns.length(); j++) {
                            String currentIsbn = isbns.optString(j);
                            if (currentIsbn.length() == 13 || currentIsbn.length() == 10) {
                                isbn = currentIsbn;
                                break; // Usar el primer ISBN válido
                            }
                        }
                    }
                }

                // Obtener URL de portada
                String portadaUrl = "";
                if (doc.has("cover_i")) {
                    int coverId = doc.optInt("cover_i");
                    if (coverId > 0) {
                        portadaUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                    }
                }

                // Crear objeto Libro
                Libro nuevoLibro = new Libro(titulo, autor, editorial, anio, genero, isbn, portadaUrl);

                // Pasamos el limpiador automático de sagas
                com.bibliohouse.utils.ProcesadorSagas.extraerSagaDeTitulo(nuevoLibro);

                // Añadirlo a la lista
                librosEncontrados.add(nuevoLibro);
            }

        } catch (ConnectException | UnknownHostException e) {
            // Error de conexión: no hay internet, firewall, o servidor caído
            LOGGER.log(Level.SEVERE,
                    "No se pudo conectar a OpenLibrary. Verifica tu conexión a internet, firewall o VPN.", e);
        } catch (SocketTimeoutException | HttpTimeoutException e) {
            // Timeout: el servidor tardó más de 30 segundos en responder
            LOGGER.log(Level.WARNING,
                    "La conexión con OpenLibrary agotó el tiempo de espera (timeout). El servidor puede estar lento o inaccesible.",
                    e);
        } catch (IOException e) {
            // Otro error de red
            LOGGER.log(Level.SEVERE, "Error de red al conectar con OpenLibrary API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            // La búsqueda fue interrumpida (usuario cerró la app, etc.)
            Thread.currentThread().interrupt(); // Restaurar flag de interrupción
            LOGGER.log(Level.WARNING, "Búsqueda en OpenLibrary interrumpida", e);
        } catch (org.json.JSONException e) {
            // Error al parsear el JSON de respuesta
            LOGGER.log(Level.SEVERE, "Error al procesar el JSON recibido de OpenLibrary API", e);
        }

        return librosEncontrados;
    }
}
