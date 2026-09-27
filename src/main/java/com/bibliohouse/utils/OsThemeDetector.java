/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.bibliohouse.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Detector multiplataforma de tema del sistema operativo (Modo Oscuro / Claro).
 * Compatible con macOS (AppleInterfaceStyle), Windows 10/11 (AppsUseLightTheme)
 * y Linux (GNOME/Freedesktop color-scheme).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class OsThemeDetector {

    private static final Logger LOGGER = Logger.getLogger(OsThemeDetector.class.getName());
    private static volatile Boolean cachedDarkMode = null;
    private static long lastCheckTime = 0;
    private static final long CACHE_TTL_MS = 5000;
    private static ScheduledExecutorService scheduler;

    /**
     * Comprueba si el sistema operativo se encuentra actualmente en modo oscuro.
     *
     * @return true si el sistema está en modo oscuro, false si es claro.
     */
    public static boolean isDarkMode() {
        long now = System.currentTimeMillis();
        if (cachedDarkMode != null && (now - lastCheckTime) < CACHE_TTL_MS) {
            return cachedDarkMode;
        }

        boolean dark = detectDarkModeNative();
        cachedDarkMode = dark;
        lastCheckTime = now;
        return dark;
    }

    private static boolean detectDarkModeNative() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Process process = null;
        try {
            if (os.contains("mac")) {
                process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    boolean dark = line != null && line.trim().equalsIgnoreCase("Dark");
                    process.waitFor(300, TimeUnit.MILLISECONDS);
                    return dark;
                }
            } else if (os.contains("win")) {
                process = new ProcessBuilder("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme")
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    boolean isDark = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("AppsUseLightTheme")) {
                            isDark = line.contains("0x0");
                            break;
                        }
                    }
                    process.waitFor(300, TimeUnit.MILLISECONDS);
                    return isDark;
                }
            } else if (os.contains("linux")) {
                process = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    boolean dark = line != null && line.contains("prefer-dark");
                    process.waitFor(300, TimeUnit.MILLISECONDS);
                    return dark;
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "No se pudo detectar el modo del sistema: {0}", t.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return false;
    }

    /**
     * Inicia una tarea programada periódica para sincronizar dinámicamente el tema
     * cuando el usuario cambia el tema del sistema operativo (por ejemplo, al anochecer/amanecer).
     *
     * @param onThemeChanged Callback ejecutado en el hilo de JavaFX con el nuevo estado (true = oscuro).
     */
    public static synchronized void startAutoSync(Consumer<Boolean> onThemeChanged) {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OsThemeDetector-AutoSync");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean current = detectDarkModeNative();
                if (cachedDarkMode == null || current != cachedDarkMode) {
                    cachedDarkMode = current;
                    lastCheckTime = System.currentTimeMillis();
                    Platform.runLater(() -> onThemeChanged.accept(current));
                }
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Error en auto-sync de tema: {0}", t.getMessage());
            }
        }, 10, 15, TimeUnit.SECONDS);
    }

    /**
     * Detiene el programador de sincronización de tema.
     */
    public static synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
