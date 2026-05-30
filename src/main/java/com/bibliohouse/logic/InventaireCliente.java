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
import org.json.JSONObject;

/**
 * Cliente para buscar en Inventaire.io. Es otra página para buscar libros.
 *
 * @author Fernando Lago Dávila
 * @version 1.7
 */
public class InventaireCliente {

    /**
     * Logger para registrar eventos y errores.
     */
    private static final Logger LOGGER = Logger.getLogger(InventaireCliente.class.getName());

    /**
     * URL base de la API de Inventaire.io.
     */
    private static final String API_BASE_URL = "https://inventaire.io/api/search";

    /**
     * Cliente HTTP compartido: reutiliza conexiones TCP (HTTP/2 multiplexing).
     */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Busca libros en Inventaire. Llama a la URL y parsea el JSON.
     *
     * @param terminoDeBusqueda El texto.
     * @return Lista de libros.
     */
    public static List<Libro> buscarLibros(String terminoDeBusqueda) {
        List<Libro> librosEncontrados = new ArrayList<>();

        try {
            // Codificar el término para URL
            String terminoCodificado = URLEncoder.encode(terminoDeBusqueda, StandardCharsets.UTF_8);
            String urlCompleta = String.format("%s?search=%s&types=works&limit=20", API_BASE_URL,
                    terminoCodificado);

            LOGGER.log(Level.INFO, "Realizando búsqueda en Inventaire.io: {0}", urlCompleta);

            // Crear petición HTTP con headers necesarios
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "BiblioHouse/1.0 (ferlagod@example.com)")
                    .header("Accept", "application/json")
                    .build();

            // Enviar petición y obtener respuesta
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            // Comprobar que la respuesta es OK (código 200)
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING,
                        "La API de Inventaire.io devolvió un código de estado no exitoso: {0}. Cuerpo: {1}",
                        new Object[]{response.statusCode(), response.body()});
                return librosEncontrados;
            }

            // Parsear JSON de respuesta
            JSONObject jsonResponse = new JSONObject(response.body());

            // Verificar que hay resultados
            if (!jsonResponse.has("results")) {
                LOGGER.log(Level.INFO, "La respuesta de Inventaire.io no contiene resultados.");
                return librosEncontrados;
            }

            JSONArray results = jsonResponse.getJSONArray("results");

            // Procesar cada resultado encontrado
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                // Obtener título (label es el campo principal en Inventaire)
                String titulo = result.optString("label", "Sin título");

                // Obtener descripción como autor (Inventaire tiene estructura diferente)
                String autor = result.optString("description", "Desconocido");

                // Inicializar otros campos vacíos
                String anio = "";
                String editorial = "";
                String genero = "";
                String isbn = "";
                String portadaUrl = "";

                // Si hay un ID de entidad, intentar obtener más datos
                String uri = result.optString("uri", "");
                if (!uri.isEmpty()) {
                    // Inventaire puede tener imágenes en formato específico
                    if (result.has("image")) {
                        JSONArray images = result.optJSONArray("image");
                        if (images != null && images.length() > 0) {
                            portadaUrl = images.getString(0);
                            // Asegurar que es URL completa
                            if (portadaUrl.startsWith("/")) {
                                portadaUrl = "https://inventaire.io" + portadaUrl;
                            }
                        }
                    }

                    // Intentar obtener año de la descripción o campos adicionales
                    if (result.has("originalLang")) {
                        // Este campo a veces contiene información del año
                        String originalLang = result.optString("originalLang", "");
                        // Buscar un patrón de 4 dígitos que podría ser un año
                        if (originalLang.matches(".*\\b(19|20)\\d{2}\\b.*")) {
                            anio = originalLang.replaceAll(".*\\b((19|20)\\d{2})\\b.*", "$1");
                        }
                    }
                }

                // Si el autor viene en formato "por Nombre Apellido", limpiar
                if (autor.toLowerCase().startsWith("by ") || autor.toLowerCase().startsWith("por ")) {
                    autor = autor.substring(autor.indexOf(" ") + 1);
                }

                // Crear objeto Libro y añadirlo a la lista
                librosEncontrados.add(new Libro(titulo, autor, editorial, anio, genero, isbn, portadaUrl));
            }

            LOGGER.log(Level.INFO, "Inventaire.io devolvió {0} resultados", librosEncontrados.size());

        } catch (ConnectException | UnknownHostException e) {
            // Error de conexión: no hay internet, firewall, o servidor caído
            LOGGER.log(Level.SEVERE,
                    "No se pudo conectar a Inventaire.io. Verifica tu conexión a internet, firewall o VPN.", e);
        } catch (SocketTimeoutException | HttpTimeoutException e) {
            // Timeout: el servidor tardó más de 30 segundos en responder
            LOGGER.log(Level.WARNING,
                    "La conexión con Inventaire.io agotó el tiempo de espera (timeout). El servidor puede estar lento o inaccesible.",
                    e);
        } catch (IOException e) {
            // Otro error de red
            LOGGER.log(Level.SEVERE, "Error de red al conectar con Inventaire.io API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            // La búsqueda fue interrumpida
            Thread.currentThread().interrupt(); // Restaurar flag de interrupción
            LOGGER.log(Level.WARNING, "Búsqueda en Inventaire.io interrumpida", e);
        } catch (org.json.JSONException e) {
            // Error al parsear el JSON de respuesta
            LOGGER.log(Level.SEVERE, "Error al procesar el JSON recibido de Inventaire.io API", e);
        }

        return librosEncontrados;
    }
}
