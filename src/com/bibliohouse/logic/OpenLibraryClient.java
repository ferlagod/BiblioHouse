/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Cliente para interactuar con la API de OpenLibrary. Esta clase permite buscar
 * libros utilizando la API de OpenLibrary.
 *
 * @author ferlagod
 */
public class OpenLibraryClient {

    private static final Logger LOGGER = Logger.getLogger(OpenLibraryClient.class.getName());
    private static final String API_BASE_URL = "https://openlibrary.org/search.json";    
    private static final String FIELDS_TO_GET = "title,author_name,first_publish_year,publisher,subject,isbn,cover_i";

    /**
     * Busca libros en la API de OpenLibrary basado en un término de búsqueda.
     *
     * @param terminoDeBusqueda El término a buscar en OpenLibrary.
     * @return Una lista de objetos Libro encontrados.
     */
    public List<Libro> buscarLibros(String terminoDeBusqueda) {
        List<Libro> librosEncontrados = new ArrayList<>();

        try {
            String terminoCodificado = URLEncoder.encode(terminoDeBusqueda, StandardCharsets.UTF_8);
            String urlCompleta = String.format("%s?q=%s&fields=%s&limit=20", API_BASE_URL, terminoCodificado, FIELDS_TO_GET);
            
            LOGGER.log(Level.INFO, "Realizando búsqueda en OpenLibrary: {0}", urlCompleta);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlCompleta)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "La API de OpenLibrary devolvió un código de estado no exitoso: {0}", response.statusCode());
                return librosEncontrados;
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray docs = jsonResponse.getJSONArray("docs");

            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);

                String titulo = doc.optString("title", "Sin título");
                String autor = doc.optJSONArray("author_name") != null ? doc.optJSONArray("author_name").optString(0, "") : "";
                String anio = String.valueOf(doc.optInt("first_publish_year"));
                String editorial = doc.optJSONArray("publisher") != null ? doc.optJSONArray("publisher").optString(0, "") : "";

                String genero = "";
                if (doc.has("subject")) {
                    JSONArray subjects = doc.getJSONArray("subject");
                    // Tomamos los primeros 5 géneros y los unimos con comas
                    genero = IntStream.range(0, Math.min(subjects.length(), 5))
                                      .mapToObj(subjects::getString)
                                      .collect(Collectors.joining(", "));
                }

                String isbn = "";
                if (doc.has("isbn")) {
                    JSONArray isbns = doc.getJSONArray("isbn");
                    for (int j = 0; j < isbns.length(); j++) {
                        String currentIsbn = isbns.getString(j);
                        if (currentIsbn.length() == 13 || currentIsbn.length() == 10) {
                            isbn = currentIsbn;
                            break;
                        }
                    }
                }

                String portadaUrl = doc.has("cover_i") ? "https://covers.openlibrary.org/b/id/" + doc.getInt("cover_i") + "-L.jpg" : "";

                librosEncontrados.add(new Libro(titulo, autor, editorial, anio, genero, isbn, portadaUrl));
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error de red al conectar con OpenLibrary API", e);
        } catch (org.json.JSONException e) {
            LOGGER.log(Level.SEVERE, "Error al procesar el JSON recibido de OpenLibrary API", e);
        }

        return librosEncontrados;
    }
}
