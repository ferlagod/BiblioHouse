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
package com.bibliohouse.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;

/**
 * Clase para comprobar actualizaciones de la aplicación BiblioHouse. Consulta
 * la API de Forgejo para verificar si existe una versión más reciente que la
 * actual y notifica el resultado mediante un callback.
 */
public class UpdateChecker {

    // API de Forgejo para tu repositorio
    private static final String API_URL = "https://forjalibre.eu/api/v1/repos/ferlagod/BiblioHouse/releases/latest";
    // Versión actual de la aplicación. 
    private static final String VERSION_ACTUAL = "1.6";

    /**
     * Comprueba si hay una versión más reciente de la aplicación.
     *
     * @param alEncontrarNueva Callback que se ejecuta si se encuentra una
     * versión más reciente. Recibe como parámetro la versión más reciente
     * encontrada.
     */
    public static void comprobarActualizaciones(java.util.function.Consumer<String> alEncontrarNueva) {
        Thread hilo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(5))
                            .build();
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Accept", "application/json")
                            .GET()
                            .build();
                    
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        // Forgejo también devuelve "tag_name": "1.6" o "v1.6"
                        Matcher matcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([0-9.]+)\"").matcher(response.body());
                        
                        if (matcher.find()) {
                            String versionMasReciente = matcher.group(1);
                            
                            if (esVersionMasReciente(VERSION_ACTUAL, versionMasReciente)) {
                                Platform.runLater(() -> alEncontrarNueva.accept(versionMasReciente));
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    // Si falla la conexión, ignorar silenciosamente
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Compara dos versiones para determinar si la versión en línea es más
     * reciente.
     *
     * @param actual Versión actual de la aplicación.
     * @param online Versión obtenida de la API.
     * @return true si la versión en línea es más reciente, false en caso
     * contrario.
     */
    private static boolean esVersionMasReciente(String actual, String online) {
        return online.compareTo(actual) > 0;
    }
}
