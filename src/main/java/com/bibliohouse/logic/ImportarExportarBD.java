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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Clase encargada de gestionar la importación y exportación de la base de datos
 * de libros en formato JSON. Proporciona diálogos para seleccionar archivos y
 * opciones para añadir o reemplazar los datos existentes.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class ImportarExportarBD {

    /**
     * Exporta la lista completa de libros a un archivo JSON seleccionado por el
     * usuario. Muestra un diálogo para guardar el archivo y notifica el
     * resultado de la operación.
     *
     * @param ownerWindow         Ventana propietaria para el diálogo de guardado.
     * @param jsonManager         Gestor de persistencia para realizar la
     *                            exportación.
     * @param listaLibrosCompleta Lista completa de libros a exportar.
     */
    public void exportarBaseDatos(Window ownerWindow, JsonManager jsonManager, List<Libro> listaLibrosCompleta) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Backup");
        fileChooser.setInitialFileName("biblioteca_backup.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = fileChooser.showSaveDialog(ownerWindow);

        if (archivo != null) {
            boolean ok = jsonManager.exportarLibros(archivo, new ArrayList<>(listaLibrosCompleta));
            if (ok) {
                mostrarAlerta("Éxito", "Copia guardada correctamente.");
            } else {
                mostrarAlerta("Error", "Fallo al exportar.");
            }
        }
    }

    /**
     * Importa libros desde un archivo JSON seleccionado por el usuario. Muestra
     * un diálogo para abrir el archivo y otro para elegir entre añadir los
     * libros a los existentes o reemplazarlos por completo. Finalmente, guarda
     * los cambios y actualiza la interfaz de usuario si se proporciona un
     * callback.
     *
     * @param ownerWindow         Ventana propietaria para los diálogos.
     * @param jsonManager         Gestor de persistencia para realizar la
     *                            importación.
     * @param listaLibrosCompleta Lista observable de libros que se actualizará.
     * @param onUpdateUI          Callback opcional para actualizar la interfaz de
     *                            usuario después de la importación.
     */
    public void importarBaseDatos(Window ownerWindow, JsonManager jsonManager,
            ObservableList<Libro> listaLibrosCompleta, Runnable onUpdateUI) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar Base de Datos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File archivo = fileChooser.showOpenDialog(ownerWindow);

        if (archivo != null) {
            List<Libro> importados = jsonManager.importarLibrosDesdeArchivo(archivo);
            if (importados != null && !importados.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Importar");
                alert.setContentText("¿Añadir a los existentes o Reemplazar todo?");

                ButtonType btnAdd = new ButtonType("Añadir");
                ButtonType btnReplace = new ButtonType("Reemplazar");
                ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(btnAdd, btnReplace, btnCancel);

                Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent()) {
                    if (res.get() == btnAdd) {
                        listaLibrosCompleta.addAll(importados);
                    } else if (res.get() == btnReplace) {
                        listaLibrosCompleta.setAll(importados);
                    }

                    if (res.get() != btnCancel) {
                        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
                        // Ejecutar callback para actualizar la interfaz
                        if (onUpdateUI != null) {
                            onUpdateUI.run();
                        }
                    }
                }
            }
        }
    }

    /**
     * Muestra una alerta informativa al usuario. Se ejecuta en el hilo de
     * JavaFX para garantizar que se muestre correctamente incluso si se llama
     * desde otro hilo.
     *
     * @param titulo  Título de la alerta.
     * @param mensaje Contenido del mensaje a mostrar.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}
