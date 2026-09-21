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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Bus de eventos ligero y desacoplado para la comunicación entre componentes
 * y controladores en BiblioHouse. Permite la suscripción tipada y asegura que
 * los manejadores que actualicen la interfaz se invoquen en el hilo de JavaFX.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class AppEventBus {

    private static final AppEventBus INSTANCE = new AppEventBus();
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    private AppEventBus() {
    }

    public static AppEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Suscribe un consumidor a los eventos del tipo especificado.
     *
     * @param <T> Tipo de evento.
     * @param eventType Clase del evento a escuchar.
     * @param listener Manejador del evento.
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Desuscribe un consumidor de los eventos del tipo especificado.
     *
     * @param <T> Tipo de evento.
     * @param eventType Clase del evento.
     * @param listener Manejador a remover.
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publica un evento a todos los suscriptores registrados. Si se está en
     * otro hilo y es relevante para la UI, se asegura el despacho en el hilo FX.
     *
     * @param <T> Tipo de evento.
     * @param event Instancia del evento a publicar.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (event == null) {
            return;
        }
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null && !list.isEmpty()) {
            Runnable dispatch = () -> {
                for (Consumer<?> listener : list) {
                    try {
                        ((Consumer<T>) listener).accept(event);
                    } catch (Exception e) {
                        java.util.logging.Logger.getLogger(AppEventBus.class.getName())
                                .log(java.util.logging.Level.SEVERE, "Error despachando evento " + event.getClass().getSimpleName(), e);
                    }
                }
            };

            try {
                if (Platform.isFxApplicationThread()) {
                    dispatch.run();
                } else {
                    Platform.runLater(dispatch);
                }
            } catch (IllegalStateException e) {
                // Si el Toolkit de JavaFX no está inicializado (ej. en pruebas unitarias), ejecutar directamente
                dispatch.run();
            }
        }
    }

    /**
     * Limpia todos los suscriptores (útil en pruebas o recargas de UI).
     */
    public void clear() {
        listeners.clear();
    }

    // =========================================================================
    // EVENTOS CONCRETOS
    // =========================================================================

    /**
     * Evento emitido cuando un libro ha sido añadido o modificado.
     */
    public static class LibroModificadoEvent {
        private final Libro libro;
        private final boolean esNuevo;

        public LibroModificadoEvent(Libro libro, boolean esNuevo) {
            this.libro = libro;
            this.esNuevo = esNuevo;
        }

        public Libro getLibro() {
            return libro;
        }

        public boolean isEsNuevo() {
            return esNuevo;
        }
    }

    /**
     * Evento emitido cuando un libro ha sido eliminado.
     */
    public static class LibroEliminadoEvent {
        private final Libro libro;

        public LibroEliminadoEvent(Libro libro) {
            this.libro = libro;
        }

        public Libro getLibro() {
            return libro;
        }
    }

    /**
     * Evento emitido cuando el usuario selecciona una estantería en la barra lateral.
     */
    public static class FiltroEstanteriaEvent {
        private final String estanteria;

        public FiltroEstanteriaEvent(String estanteria) {
            this.estanteria = estanteria;
        }

        public String getEstanteria() {
            return estanteria;
        }
    }

    /**
     * Evento emitido cuando cambia la lista de estanterías del usuario.
     */
    public static class EstanteriasActualizadasEvent {
        private final List<String> estanterias;

        public EstanteriasActualizadasEvent(List<String> estanterias) {
            this.estanterias = estanterias;
        }

        public List<String> getEstanterias() {
            return estanterias;
        }
    }

    /**
     * Evento emitido cuando se crea o devuelve un préstamo.
     */
    public static class PrestamoModificadoEvent {
        public PrestamoModificadoEvent() {}
    }

    /**
     * Evento emitido para actualizar el mensaje de la barra de estado inferior.
     */
    public static class StatusMessageEvent {
        private final String mensaje;

        public StatusMessageEvent(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    /**
     * Evento emitido cuando cambia el idioma o locale de la aplicación.
     */
    public static class IdiomaCambiadoEvent {
        private final java.util.Locale nuevoLocale;
        private final java.util.ResourceBundle nuevoBundle;

        public IdiomaCambiadoEvent(java.util.Locale nuevoLocale, java.util.ResourceBundle nuevoBundle) {
            this.nuevoLocale = nuevoLocale;
            this.nuevoBundle = nuevoBundle;
        }

        public java.util.Locale getNuevoLocale() {
            return nuevoLocale;
        }

        public java.util.ResourceBundle getNuevoBundle() {
            return nuevoBundle;
        }
    }
}

