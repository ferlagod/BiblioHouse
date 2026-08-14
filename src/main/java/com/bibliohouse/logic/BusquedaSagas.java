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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;

/**
 * Clase encargada de buscar y asignar sagas a libros que no tienen serie
 * asignada. Utiliza APIs externas como Google Books y OpenLibrary para obtener
 * información sobre las sagas de los libros, y permite actualizar la interfaz
 * de usuario mediante callbacks al finalizar el proceso.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class BusquedaSagas {

    /**
     * Busca y asigna sagas a los libros que no tienen serie asignada.Muestra un
     * diálogo de confirmación y una barra de progreso durante la búsqueda.
     *
     * @param listaLibros Lista completa de libros a analizar.
     * @param jsonManager Gestor de persistencia para guardar los cambios.
     * @param onComplete Callback opcional que se ejecuta al finalizar el
     * proceso. Se usa para actualizar la interfaz de usuario.
     */
    public void buscarSagasFaltantes(List<Libro> listaLibros, JsonManager jsonManager, Runnable onComplete) {
        List<Libro> librosSinSaga = listaLibros.stream()
                .filter(l -> l.getSerie() == null || l.getSerie().trim().isEmpty())
                .collect(Collectors.toList());

        if (librosSinSaga.isEmpty()) {
            mostrarAlerta("Búsqueda de Sagas", "Búsqueda finalizada", "Todos tus libros ya tienen una saga asignada.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Búsqueda de Sagas Online");
        confirmacion.setHeaderText("Analizando " + librosSinSaga.size() + " libros sin saga.");
        confirmacion.setContentText("El programa consultará bases de datos externas para intentar deducir las series. ¿Deseas continuar?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        Alert dialogo = new Alert(Alert.AlertType.INFORMATION);
        dialogo.setTitle("Buscando sagas...");
        dialogo.setHeaderText("Procesando colección, por favor espera.");
        dialogo.getDialogPane().getButtonTypes().clear();
        ProgressBar progressBar = new ProgressBar(-1);
        dialogo.getDialogPane().setContent(progressBar);
        dialogo.show();

        Thread hilo = new Thread(() -> {
            int actualizados = 0;
            for (Libro libro : librosSinSaga) {
                // Intentamos por Título + Autor
                String query = libro.getTitulo() + (libro.getAutor() != null ? " " + libro.getAutor() : "");
                Libro apiLibro = buscarSagaEnApisSilencioso(query);

                // Fallback por ISBN
                if (apiLibro == null && libro.getIsbn() != null && !libro.getIsbn().isEmpty()) {
                    apiLibro = buscarSagaEnApisSilencioso(libro.getIsbn());
                }

                if (apiLibro != null && apiLibro.getSerie() != null && !apiLibro.getSerie().isEmpty()) {
                    libro.setSerie(apiLibro.getSerie());
                    libro.setOrdenEnSerie(apiLibro.getOrdenEnSerie());
                    actualizados++;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }

            final int totalAct = actualizados;
            Platform.runLater(() -> {
                // 1. GUARDADO FORZOSO: Guardamos la lista completa en el archivo JSON
                jsonManager.guardarLibros(listaLibros);

                dialogo.getDialogPane().getButtonTypes().add(ButtonType.OK);
                dialogo.close();

                // 2. CALLBACK: Ejecutamos el refresco de la UI definido en el controlador
                if (onComplete != null) {
                    onComplete.run();
                }

                mostrarAlerta("Búsqueda de Sagas", "Búsqueda finalizada", "Se han asignado " + totalAct + " sagas nuevas.");
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Busca información de saga para un libro en Google Books y OpenLibrary.
     *
     * @param query El ISBN o título del libro a buscar.
     * @return El primer libro encontrado con información de saga, o null si no
     * se encuentra.
     */
    private Libro buscarSagaEnApisSilencioso(String query) {
        try {
            var f1 = CompletableFuture.supplyAsync(() -> GoogleBooksCliente.buscarLibros(query));
            var f2 = CompletableFuture.supplyAsync(() -> OpenLibraryCliente.buscarLibros(query));
            CompletableFuture.allOf(f1, f2).join();

            List<Libro> resultados = f1.get();
            resultados.addAll(f2.get());

            return resultados.stream()
                    .filter(l -> l.getSerie() != null && !l.getSerie().isEmpty())
                    .findFirst().orElse(null);
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    /**
     * Muestra una alerta informativa al usuario.
     *
     * @param titulo Título de la alerta.
     * @param cabecera Cabecera de la alerta.
     * @param mensaje Contenido del mensaje.
     */
    private void mostrarAlerta(String titulo, String cabecera, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(cabecera);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}
