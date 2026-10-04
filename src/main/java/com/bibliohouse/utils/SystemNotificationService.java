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

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio unificado de notificaciones nativas para el sistema operativo (macOS
 * Notification Center vía osascript, Linux libnotify vía notify-send, y Windows
 * Action Center vía SystemTray).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class SystemNotificationService {

    private static final Logger LOGGER = Logger.getLogger(SystemNotificationService.class.getName());
    private static TrayIcon trayIcon;
    private static boolean initialized = false;

    /**
     * Inicializa el servicio de notificaciones nativas de forma segura y sin
     * bloquear hilos.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        String os = System.getProperty("os.name", "").toLowerCase();
        // En macOS y Linux usamos notificaciones directas del SO sin cargar AWT
        if (os.contains("mac") || os.contains("linux")) {
            LOGGER.info("[SystemNotificationService] Notificaciones nativas activas para " + os);
            return;
        }

        // En Windows usamos SystemTray si está disponible
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                java.net.URL iconUrl = SystemNotificationService.class.getResource("/resources/LogoBiblioHouse.png");
                Image image = iconUrl != null ? Toolkit.getDefaultToolkit().getImage(iconUrl) : null;
                if (image == null) {
                    image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                }
                trayIcon = new TrayIcon(image, "BiblioHouse");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                LOGGER.info("[SystemNotificationService] Bandeja del sistema inicializada correctamente.");
            } catch (AWTException | SecurityException e) {
                LOGGER.log(Level.FINE, "No se pudo registrar icono en la bandeja: {0}", e.getMessage());
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "SystemTray no disponible: {0}", t.getMessage());
            }
        }
    }

    /**
     * Alias en español para initialize().
     */
    public static synchronized void inicializar() {
        initialize();
    }

    /**
     * Envía una notificación nativa al sistema operativo.
     *
     * @param titulo Título de la notificación.
     * @param mensaje Cuerpo de la notificación.
     * @param tipo Tipo de mensaje (INFO, WARNING, ERROR).
     */
    public static void notificar(String titulo, String mensaje, MessageType tipo) {
        if (!initialized) {
            initialize();
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        String safeTitulo = (titulo != null ? titulo : "BiblioHouse").replace("\"", "'");
        String safeMensaje = (mensaje != null ? mensaje : "").replace("\"", "'");

        if (os.contains("mac")) {
            Thread t = new Thread(() -> {
                try {
                    String script = String.format("display notification \"%s\" with title \"%s\"", safeMensaje, safeTitulo);
                    Process p = new ProcessBuilder("osascript", "-e", script).start();
                    p.waitFor(2, TimeUnit.SECONDS);
                    if (p.isAlive()) {
                        p.destroyForcibly();
                    }
                } catch (Throwable ignored) {
                }
            }, "NativeNotification-Mac");
            t.setDaemon(true);
            t.start();
            return;
        }

        if (os.contains("linux")) {
            Thread t = new Thread(() -> {
                try {
                    Process p = new ProcessBuilder("notify-send", safeTitulo, safeMensaje).start();
                    p.waitFor(2, TimeUnit.SECONDS);
                    if (p.isAlive()) {
                        p.destroyForcibly();
                    }
                } catch (Throwable ignored) {
                }
            }, "NativeNotification-Linux");
            t.setDaemon(true);
            t.start();
            return;
        }

        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(titulo != null ? titulo : "BiblioHouse", mensaje, tipo);
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Error emitiendo notificación nativa: {0}", t.getMessage());
            }
        }
    }

    /**
     * Envía una notificación informativa nativa con el título "BiblioHouse".
     *
     * @param mensaje Mensaje a mostrar.
     */
    public static void notificarInfo(String mensaje) {
        notificar("BiblioHouse", mensaje, MessageType.INFO);
    }

    /**
     * Envía una notificación informativa nativa con título personalizado.
     *
     * @param titulo Título de la notificación.
     * @param mensaje Mensaje a mostrar.
     */
    public static void notificarInfo(String titulo, String mensaje) {
        notificar(titulo, mensaje, MessageType.INFO);
    }

    /**
     * Envía una notificación de advertencia nativa al sistema operativo.
     *
     * @param titulo Título de la alerta.
     * @param mensaje Detalle del aviso.
     */
    public static void notificarAlerta(String titulo, String mensaje) {
        notificar(titulo, mensaje, MessageType.WARNING);
    }

    /**
     * Libera el icono de la bandeja del sistema y sus recursos al cerrar la
     * aplicación.
     */
    public static synchronized void shutdown() {
        if (trayIcon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Throwable ignored) {
            }
            trayIcon = null;
        }
        initialized = false;
    }
}
