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

    /**
     * Obtiene la instancia única (singleton) del bus de eventos de la aplicación.
     *
     * @return Instancia singleton de {@link AppEventBus}.
     */
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
     * Evento emitido cuando un libro ha sido añadido o modificado en la biblioteca.
     */
    public static class LibroModificadoEvent {
        private final Libro libro;
        private final boolean esNuevo;

        /**
         * Crea una nueva instancia del evento de modificación o creación de libro.
         *
         * @param libro   El libro modificado o añadido.
         * @param esNuevo Indica si el libro acaba de registrarse como nuevo (true) o si fue editado (false).
         */
        public LibroModificadoEvent(Libro libro, boolean esNuevo) {
            this.libro = libro;
            this.esNuevo = esNuevo;
        }

        /**
         * Obtiene el libro asociado al evento.
         *
         * @return Objeto {@link Libro} modificado o añadido.
         */
        public Libro getLibro() {
            return libro;
        }

        /**
         * Indica si el libro fue recién creado o modificado.
         *
         * @return {@code true} si es un libro recién añadido; {@code false} si fue actualizado.
         */
        public boolean isEsNuevo() {
            return esNuevo;
        }
    }

    /**
     * Evento emitido cuando un libro ha sido eliminado de la biblioteca.
     */
    public static class LibroEliminadoEvent {
        private final Libro libro;

        /**
         * Crea una nueva instancia del evento de eliminación de libro.
         *
         * @param libro El libro que ha sido eliminado.
         */
        public LibroEliminadoEvent(Libro libro) {
            this.libro = libro;
        }

        /**
         * Obtiene el libro que fue eliminado.
         *
         * @return Objeto {@link Libro} retirado.
         */
        public Libro getLibro() {
            return libro;
        }
    }

    /**
     * Evento emitido cuando el usuario selecciona una estantería o categoría en la barra lateral.
     */
    public static class FiltroEstanteriaEvent {
        private final String estanteria;

        /**
         * Crea un nuevo evento de cambio de filtro de estantería.
         *
         * @param estanteria Nombre de la estantería o categoría seleccionada (ej. "Todos los libros", "Ciencia Ficción").
         */
        public FiltroEstanteriaEvent(String estanteria) {
            this.estanteria = estanteria;
        }

        /**
         * Obtiene el nombre de la estantería seleccionada.
         *
         * @return Nombre de la estantería de filtrado.
         */
        public String getEstanteria() {
            return estanteria;
        }
    }

    /**
     * Evento emitido cuando cambia la lista de estanterías del usuario (creación, edición o borrado).
     */
    public static class EstanteriasActualizadasEvent {
        private final List<String> estanterias;

        /**
         * Crea un evento de estanterías actualizadas con la nueva lista.
         *
         * @param estanterias Lista completa y actualizada de nombres de estanterías.
         */
        public EstanteriasActualizadasEvent(List<String> estanterias) {
            this.estanterias = estanterias;
        }

        /**
         * Obtiene la lista actualizada de estanterías.
         *
         * @return Lista con los nombres de las estanterías disponibles.
         */
        public List<String> getEstanterias() {
            return estanterias;
        }
    }

    /**
     * Evento emitido cuando se crea, edita o devuelve un préstamo de un libro.
     */
    public static class PrestamoModificadoEvent {
        /**
         * Constructor predeterminado para el evento de modificación de préstamos.
         */
        public PrestamoModificadoEvent() {}
    }

    /**
     * Evento emitido para actualizar el mensaje de la barra de estado inferior.
     */
    public static class StatusMessageEvent {
        private final String mensaje;

        /**
         * Crea un evento para mostrar un mensaje en la barra de estado.
         *
         * @param mensaje Texto descriptivo de la acción o estado actual.
         */
        public StatusMessageEvent(String mensaje) {
            this.mensaje = mensaje;
        }

        /**
         * Obtiene el mensaje a mostrar en la barra de estado.
         *
         * @return Mensaje descriptivo.
         */
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

        /**
         * Crea un evento de cambio de idioma.
         *
         * @param nuevoLocale  Nuevo {@link java.util.Locale} establecido.
         * @param nuevoBundle  Nuevo {@link java.util.ResourceBundle} cargado para traducciones.
         */
        public IdiomaCambiadoEvent(java.util.Locale nuevoLocale, java.util.ResourceBundle nuevoBundle) {
            this.nuevoLocale = nuevoLocale;
            this.nuevoBundle = nuevoBundle;
        }

        /**
         * Obtiene el nuevo Locale seleccionado.
         *
         * @return Instancia de {@link java.util.Locale}.
         */
        public java.util.Locale getNuevoLocale() {
            return nuevoLocale;
        }

        /**
         * Obtiene el paquete de recursos con los textos en el nuevo idioma.
         *
         * @return Instancia de {@link java.util.ResourceBundle}.
         */
        public java.util.ResourceBundle getNuevoBundle() {
            return nuevoBundle;
        }
    }
}

