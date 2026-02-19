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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Cliente para conectar con Google Books. Si OpenLib no va, usamos esto.
 *
 * @author Fernando Lago
 * @version 1.3
 */
public class GoogleBooksCliente {

    private static final Logger LOGGER = Logger.getLogger(GoogleBooksCliente.class.getName());
    private static final String API_BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    /**
     * Busca en Google. Devuelve una lista de libros.
     *
     * @param terminoDeBusqueda Lo que queremos buscar.
     * @return Lista de libros que encontró.
     */
    public static List<Libro> buscarLibros(String terminoDeBusqueda) {
        List<Libro> librosEncontrados = new ArrayList<>();

        String apiKey = ConfigLoader.getProperty("google.books.api.key");
        if (apiKey == null || apiKey.isEmpty() || "TU_API_KEY_AQUI".equals(apiKey)) {
            LOGGER.log(Level.WARNING, "No se ha configurado la API key de Google Books en config.properties");
            return librosEncontrados;
        }

        try {
            // Codificar el término para URL
            String terminoCodificado = URLEncoder.encode(terminoDeBusqueda, StandardCharsets.UTF_8);
            String urlCompleta = String.format("%s?q=%s&maxResults=20&key=%s", API_BASE_URL, terminoCodificado, apiKey);

            LOGGER.log(Level.INFO, "Realizando búsqueda en Google Books");

            // Crear cliente HTTP
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // Crear petición HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "BiblioHouse/1.0")
                    .header("Accept", "application/json")
                    .build();

            // Enviar petición y obtener respuesta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "La API de Google Books devolvió error: {0}", response.statusCode());
                return librosEncontrados;
            }

            // Parsear JSON
            JSONObject jsonResponse = new JSONObject(response.body());

            if (!jsonResponse.has("items")) {
                LOGGER.log(Level.INFO, "Google Books no devolvió resultados.");
                return librosEncontrados;
            }

            JSONArray items = jsonResponse.getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                if (!item.has("volumeInfo")) {
                    continue;
                }
                JSONObject volumeInfo = item.getJSONObject("volumeInfo");

                // Título
                String titulo = volumeInfo.optString("title", "Sin título");

                // Autor
                String autor = "Desconocido";
                if (volumeInfo.has("authors")) {
                    JSONArray authors = volumeInfo.getJSONArray("authors");
                    if (authors.length() > 0) {
                        autor = authors.getString(0);
                    }
                }

                // Editorial
                String editorial = volumeInfo.optString("publisher", "");

                // Año
                String anio = "";
                String publishedDate = volumeInfo.optString("publishedDate", "");
                if (publishedDate.length() >= 4) {
                    anio = publishedDate.substring(0, 4);
                }

                // Género
                String genero = "";
                if (volumeInfo.has("categories")) {
                    JSONArray categories = volumeInfo.getJSONArray("categories");
                    if (categories.length() > 0) {
                        genero = categories.getString(0);
                    }
                }

                // ISBN
                String isbn = "";
                if (volumeInfo.has("industryIdentifiers")) {
                    JSONArray identifiers = volumeInfo.getJSONArray("industryIdentifiers");
                    for (int j = 0; j < identifiers.length(); j++) {
                        JSONObject id = identifiers.getJSONObject(j);
                        String type = id.optString("type");
                        if ("ISBN_13".equals(type)) {
                            isbn = id.optString("identifier");
                            break;
                        } else if ("ISBN_10".equals(type) && isbn.isEmpty()) {
                            isbn = id.optString("identifier");
                        }
                    }
                }

                // Portada
                String portadaUrl = "";
                if (volumeInfo.has("imageLinks")) {
                    JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
                    portadaUrl = imageLinks.optString("thumbnail", "");
                    if (portadaUrl.startsWith("http:")) {
                        portadaUrl = portadaUrl.replace("http:", "https:");
                    }
                }

                librosEncontrados.add(new Libro(titulo, autor, editorial, anio, genero, isbn, portadaUrl));
            }

        } catch (ConnectException | UnknownHostException e) {
            LOGGER.log(Level.SEVERE, "Error de conexión con Google Books", e);
        } catch (SocketTimeoutException | HttpTimeoutException e) {
            LOGGER.log(Level.WARNING, "Timeout en conexión con Google Books", e);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error en búsqueda de Google Books", e);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error inesperado en Google Books", e);
        }

        return librosEncontrados;
    }
}
