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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool compartido para tareas en segundo plano de la interfaz (búsquedas,
 * exportaciones, sincronizaciones manuales...). Reutiliza hilos en vez de
 * crear uno nuevo por tarea, y todos son daemon para no impedir el cierre.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public final class Hilos {

    private static final AtomicInteger CONTADOR = new AtomicInteger();

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "bibliohouse-bg-" + CONTADOR.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private Hilos() {
    }

    /**
     * Ejecuta una tarea en segundo plano.
     *
     * @param tarea Tarea (p. ej. un {@code javafx.concurrent.Task}).
     */
    public static void ejecutar(Runnable tarea) {
        POOL.execute(tarea);
    }

    /**
     * Detiene el pool al cerrar la aplicación.
     */
    public static void shutdown() {
        POOL.shutdownNow();
    }
}
