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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abre archivos, carpetas y enlaces con la aplicación predeterminada del
 * sistema. Las llamadas a {@link Desktop} se hacen fuera del hilo de JavaFX:
 * invocarlas desde él puede bloquear la interfaz (especialmente en macOS).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public final class AbrirExterno {

    private static final Logger LOGGER = Logger.getLogger(AbrirExterno.class.getName());

    private AbrirExterno() {
    }

    /**
     * Abre un archivo o carpeta con la aplicación predeterminada.
     *
     * @param archivo Archivo o carpeta.
     * @throws IOException Se mantiene por compatibilidad con los llamantes; los
     * errores reales se producen en segundo plano y se registran en el log.
     */
    public static void abrir(File archivo) throws IOException {
        ejecutar("abrir " + archivo, () -> Desktop.getDesktop().open(archivo));
    }

    /**
     * Abre el cliente de correo con la dirección indicada.
     *
     * @param mailto URI {@code mailto:}.
     * @throws IOException Se mantiene por compatibilidad con los llamantes.
     */
    public static void correo(URI mailto) throws IOException {
        ejecutar("correo " + mailto, () -> Desktop.getDesktop().mail(mailto));
    }

    private interface Accion {

        void run() throws IOException;
    }

    private static void ejecutar(String descripcion, Accion accion) {
        Thread hilo = new Thread(() -> {
            try {
                if (!Desktop.isDesktopSupported()) {
                    LOGGER.log(Level.WARNING, "Desktop no soportado: no se puede {0}", descripcion);
                    return;
                }
                accion.run();
            } catch (IOException | RuntimeException e) {
                LOGGER.log(Level.WARNING, "No se pudo " + descripcion, e);
            }
        }, "abrir-externo");
        hilo.setDaemon(true);
        hilo.start();
    }
}
