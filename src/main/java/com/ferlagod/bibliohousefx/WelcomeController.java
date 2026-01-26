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
package com.ferlagod.bibliohousefx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Controlador para la pantalla de bienvenida. Esta clase gestiona la primera
 * interacción del usuario con la aplicación, ofreciendo la opción de iniciar
 * sesión/registrarse o usar la aplicación en modo invitado (sin registro).
 *
 * @author Ferlagod
 * @version 1.0
 */
public class WelcomeController {

    /**
     * Inicializa el controlador. Se llama automáticamente después de cargar el
     * archivo FXML. Aquí se pueden realizar configuraciones iniciales si fueran
     * necesarias.
     */
    @FXML
    public void initialize() {

    }

    /**
     * Maneja la acción del botón "Iniciar con Registro". Carga y muestra la
     * pantalla de inicio de sesión (Login/Registro). Reutiliza la ventana
     * (Stage) actual.
     *
     * @param event El evento de acción provocado por el botón.
     */
    @FXML
    private void handleWithRegistration(ActionEvent event) {
        try {
            // Obtenemos el bundle de idioma actual desde App
            ResourceBundle bundle = ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages",
                    App.getCurrentLocale());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            loader.setResources(bundle);
            Parent root = loader.load();

            // Obtenemos el Stage actual (desde el botón que disparó el evento, por ejemplo)
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            // Cambiamos el contenido de la escena a Login
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Error fatal al cargar login.fxml");
        }
    }

    /**
     * Maneja la acción del botón "Iniciar sin Registro" (Modo Invitado).
     * Muestra una advertencia de privacidad y seguridad antes de proceder. Si
     * el usuario acepta, crea un perfil local de "Invitado" y entra a la
     * aplicación.
     *
     * @param event El evento de acción provocado por el botón.
     */
    @FXML
    private void handleWithoutRegistration(ActionEvent event) {
        ResourceBundle bundle = ResourceBundle.getBundle("com.ferlagod.bibliohousefx.messages", App.getCurrentLocale());

        // 1. Mostrar aviso de privacidad
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(bundle.getString("privacy.title"));
        alert.setHeaderText(bundle.getString("privacy.header"));
        alert.setContentText(bundle.getString("privacy.content"));

        // Esperar respuesta
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // 2. Definir ruta para invitado
            String guestPath = System.getProperty("user.home") + File.separator + "BiblioHouse" + File.separator
                    + "guest";
            File guestDir = new File(guestPath);
            if (!guestDir.exists()) {
                guestDir.mkdirs();
            }

            // 2.5 Verificar si existe biblioteca.json, si no, crearla desde template
            /**
             * Aquí miro si el archivo de la biblioteca del invitado existe.
             * Si no existe, copio uno que tengo guardado en el programa
             * con libros de ejemplo para que la estantería no se vea triste y vacía.
             */
            File guestLibrary = new File(guestDir, "biblioteca.json");
            if (!guestLibrary.exists()) {
                try (InputStream is = getClass().getResourceAsStream("default_library.json")) {
                    if (is != null) {
                        Files.copy(is, guestLibrary.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Biblioteca de invitado creada desde plantilla por defecto.");
                    } else {
                        System.err.println("No se encontró default_library.json en recursos.");
                    }
                } catch (IOException e) {
                    System.err.println("Error al copiar la biblioteca por defecto: " + e.getMessage());
                }
            }

            // 3. Cargar la app principal como "Invitado"
            try {
                // Cerramos ventana actual
                Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                currentStage.close();

                // Abrimos la principal
                App.loadMain("Invitado", guestPath);

            } catch (IOException e) {
                System.err.println("Error al cargar la aplicación en modo invitado.");
            }
        }
    }
}
