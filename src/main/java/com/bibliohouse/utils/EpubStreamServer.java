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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor HTTP local ultra ligero en loopback (127.0.0.1) basado exclusivamente
 * en las clases estándar de sockets de {@code java.base}.
 * <p>
 * Transmite archivos EPUB al visor WebView por bloques de 64 KB (streaming),
 * eliminando la necesidad de leer archivos de decenas de megabytes en memoria
 * o convertirlos a Base64 en el hilo de interfaz gráfica de JavaFX.
 * </p>
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class EpubStreamServer {

    private static final Logger LOGGER = Logger.getLogger(EpubStreamServer.class.getName());
    private static final int BUFFER_SIZE = 65536;

    private final File archivo;
    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean running = false;
    private int port;

    /**
     * Construye una nueva instancia del servidor de streaming para el archivo EPUB indicado.
     *
     * @param archivo Archivo EPUB existente en el sistema de archivos local.
     * @throws IllegalArgumentException Si el archivo es nulo o no existe.
     */
    public EpubStreamServer(File archivo) {
        if (archivo == null || !archivo.exists()) {
            throw new IllegalArgumentException("El archivo EPUB no existe o es nulo");
        }
        this.archivo = archivo;
    }

    /**
     * Inicia el servidor HTTP local asignando automáticamente un puerto libre en 127.0.0.1.
     *
     * @return La URL HTTP local para acceder al recurso (ej.: http://127.0.0.1:54321/book.epub).
     * @throws IOException Si ocurre un fallo al abrir el ServerSocket.
     */
    public synchronized String start() throws IOException {
        if (running && serverSocket != null && !serverSocket.isClosed()) {
            return getUrl();
        }

        serverSocket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        this.port = serverSocket.getLocalPort();
        this.running = true;

        listenerThread = new Thread(this::listenLoop, "epub-stream-server");
        listenerThread.setDaemon(true);
        listenerThread.start();

        LOGGER.log(Level.FINE, "Servidor de streaming EPUB iniciado en {0}", getUrl());
        return getUrl();
    }

    private void listenLoop() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                Thread worker = new Thread(() -> handleClient(client), "epub-stream-client");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException e) {
                // Socket cerrado intencionadamente al detener el servidor
                break;
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            // Consumir cabeceras restantes
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                // Consumir hasta la línea en blanco
            }

            long length = archivo.length();
            String responseHeaders = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/epub+zip\r\n"
                    + "Content-Length: " + length + "\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Accept-Ranges: bytes\r\n"
                    + "Connection: close\r\n\r\n";

            out.write(responseHeaders.getBytes(StandardCharsets.UTF_8));

            if (!requestLine.startsWith("HEAD")) {
                try (InputStream fis = new BufferedInputStream(new FileInputStream(archivo))) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            }
        } catch (Exception ignored) {
            // Desconexión normal del navegador o cierre prematuro
        }
    }

    /**
     * Detiene el servidor y cierra el socket local.
     */
    public synchronized void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Devuelve la URL local bajo la cual se está transmitiendo el archivo.
     *
     * @return URL HTTP con puerto dinámico.
     */
    public String getUrl() {
        return "http://127.0.0.1:" + port + "/book.epub";
    }

    /**
     * Obtiene el puerto TCP local efímero asignado por el sistema operativo.
     *
     * @return Número de puerto local.
     */
     public int getPort() {
        return port;
    }

    /**
     * Comprueba si el servidor HTTP local se encuentra actualmente en ejecución y aceptando conexiones.
     *
     * @return {@code true} si el socket del servidor está abierto y activo; {@code false} en caso contrario.
     */
    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }
}
