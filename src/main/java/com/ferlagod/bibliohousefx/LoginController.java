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

import com.bibliohouse.logic.Autentificacion;
import com.bibliohouse.logic.JsonManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Aquí controlamos quien entra y quien no. Gestiona el login y el registro de
 * nuevos usuarios. Si no tienes llave, no pasas.
 *
 * @author Fernando Lago
 * @version 1.4
 */
public class LoginController {

    // --- ELEMENTOS DE LA UI ---
    @FXML
    private VBox cardLogin;
    @FXML
    private VBox cardRegister;
    // Login
    @FXML
    private TextField txtLoginUser;
    @FXML
    private PasswordField txtLoginPass;
    @FXML
    private Label lblLoginError;
    // Registro
    @FXML
    private TextField txtRegUser;
    @FXML
    private PasswordField txtRegPass;
    @FXML
    private PasswordField txtRegPassConfirm;
    @FXML
    private Label lblRegError;
    // Rutas
    private static final String USERS_BASE_PATH = System.getProperty("user.home") + File.separator + "BiblioHouse"
            + File.separator + "users";
    private static final String AUTH_FILE_NAME = "user.auth";

    /**
     * Arrancamos motores. Nos aseguramos de que la carpeta de usuarios exista
     * (si no, dónde guardamos cosas?) y mostramos la pantalla de login por
     * defecto.
     */
    @FXML
    public void initialize() {
        // Asegurar que la carpeta base existe
        new File(USERS_BASE_PATH).mkdirs();
        mostrarLogin(null); // Empezar siempre en login
    }

    // --- CAMBIO DE PANTALLAS ---
    /**
     * Cambia la vista a la tarjeta de Registro.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void mostrarRegistro(ActionEvent event) {
        cardLogin.setVisible(false);
        cardRegister.setVisible(true);
        limpiarCampos();
    }

    /**
     * Cambia la vista a la tarjeta de Login.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void mostrarLogin(ActionEvent event) {
        cardRegister.setVisible(false);
        cardLogin.setVisible(true);
        limpiarCampos();
    }

    /**
     * Limpia todos los campos de texto de las pantallas de login y registro.
     */
    private void limpiarCampos() {
        txtLoginUser.clear();
        txtLoginPass.clear();
        txtRegUser.clear();
        txtRegPass.clear();
        txtRegPassConfirm.clear();
        lblLoginError.setVisible(false);
        lblRegError.setVisible(false);
    }

    // --- LÓGICA DE INICIO DE SESIÓN ---
    /**
     * Intenta entrar. Si el usuario existe y la contraseña cuadra, para
     * adentro. Si no, mostramos error y a intentarlo de nuevo.
     *
     * @param event Botón Entrar.
     */
    @FXML
    private void actionLogin(ActionEvent event) {
        String user = txtLoginUser.getText().trim();
        String pass = txtLoginPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            setError(lblLoginError, "Por favor, rellena todos los campos.");
            return;
        }

        File userDir = new File(USERS_BASE_PATH, user);
        File authFile = new File(userDir, AUTH_FILE_NAME);

        if (!userDir.exists() || !authFile.exists()) {
            setError(lblLoginError, "El usuario no existe. Crea una cuenta primero.");
            return;
        }

        // Verificar contraseña
        try (FileReader reader = new FileReader(authFile)) {
            Properties props = new Properties();
            props.load(reader);
            String storedHash = props.getProperty("passwordHash");

            if (Autentificacion.checkPassword(pass, storedHash)) {
                entrarALaApp(user, userDir.getAbsolutePath());
            } else {
                setError(lblLoginError, "Contraseña incorrecta.");
            }
        } catch (IOException e) {
            setError(lblLoginError, "Error al leer datos del usuario.");
        }
    }

    // --- LÓGICA DE REGISTRO ---
    /**
     * Crea un usuario nuevo. Le hace su carpeta, guarda su contraseña
     * (hasheada, por seguridad) y le regala unas cuantas estanterías por
     * defecto para que no empiece vacío.
     *
     * @param event Botón Registrarse.
     */
    @FXML
    private void actionRegister(ActionEvent event) {
        String user = txtRegUser.getText().trim();
        String pass = txtRegPass.getText();
        String confirm = txtRegPassConfirm.getText();

        // Validaciones
        if (user.isEmpty() || pass.isEmpty()) {
            setError(lblRegError, "Rellena todos los campos.");
            return;
        }
        if (!user.matches("^[a-zA-Z0-9_]+$")) {
            setError(lblRegError, "El usuario solo puede tener letras, números y _");
            return;
        }
        if (pass.length() < 4) {
            setError(lblRegError, "La contraseña es muy corta.");
            return;
        }
        if (!pass.equals(confirm)) {
            setError(lblRegError, "Las contraseñas no coinciden.");
            return;
        }

        File userDir = new File(USERS_BASE_PATH, user);
        if (userDir.exists()) {
            setError(lblRegError, "Este usuario ya existe.");
            return;
        }

        // Crear usuario
        if (userDir.mkdirs()) {
            String hash = Autentificacion.hashPassword(pass);
            File authFile = new File(userDir, AUTH_FILE_NAME);

            Properties props = new Properties();
            props.setProperty("passwordHash", hash);

            try (FileWriter writer = new FileWriter(authFile)) {
                props.store(writer, "BiblioHouse User Auth");

                // Inicializar estanterías básicas
                inicializarDatosUsuario(userDir.getAbsolutePath());

                // Mensaje de éxito
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cuenta creada");
                alert.setHeaderText(null);
                alert.setContentText("¡Usuario creado con éxito! Ahora puedes iniciar sesión.");
                alert.showAndWait();

                // Volver al login automáticamente
                mostrarLogin(null);
                txtLoginUser.setText(user); // Rellenar usuario por comodidad

            } catch (IOException e) {
                setError(lblRegError, "Error al guardar el usuario en disco.");
                userDir.delete();
            }
        } else {
            setError(lblRegError, "No se pudo crear la carpeta del usuario.");
        }
    }

    /**
     * Crea las estanterías por defecto para un usuario nuevo.
     *
     * @param path La ruta absoluta de la carpeta del usuario.
     */
    private void inicializarDatosUsuario(String path) {
        try {
            // 1. Creamos un gestor temporal apuntando a la carpeta del nuevo usuario
            JsonManager newMgr = new JsonManager(path);

            // 2. Definimos la lista de estanterías "básicas"
            List<String> defaultShelves = Arrays.asList(
                    "Aventura",
                    "Biografía",
                    "Ciencia Ficción",
                    "Cómic / Manga",
                    "Ensayo",
                    "Fantasía",
                    "Histórica",
                    "Infantil",
                    "Misterio",
                    "Novela Negra",
                    "Poesía",
                    "Romance",
                    "Teatro",
                    "Terror",
                    "Thriller",
                    "Viajes");

            // 3. Guardamos la lista en el archivo .json del usuario
            newMgr.guardarEstanterias(defaultShelves);

            System.out.println("✅ Estanterías por defecto creadas en: " + path);

        } catch (Exception e) {
            System.err.println("⚠️ Advertencia: No se pudieron crear las estanterías por defecto.");
        }
    }

    /**
     * Carga la aplicación principal una vez autenticado el usuario.
     *
     * @param username Nombre del usuario.
     * @param path Ruta a los datos del usuario.
     */
    private void entrarALaApp(String username, String path) {
        lblLoginError.setVisible(false);
        try {
            // 1. Abrimos la grande
            App.loadMain(username, path);

            // 2. Cerramos la pequeña (Login)
            Stage loginStage = (Stage) cardLogin.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            // Mostrar alerta real en lugar de solo texto
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Crítico");
            alert.setHeaderText("No se pudo iniciar la aplicación");
            alert.setContentText("Detalles del error: " + e.getMessage());
            alert.showAndWait();
            setError(lblLoginError, "Error crítico: " + e.getMessage());
        }
    }

    /**
     * Muestra un mensaje de error en la etiqueta correspondiente.
     *
     * @param label Etiqueta donde mostrar el error.
     * @param msg Mensaje de error.
     */
    private void setError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    /**
     * Cierra la aplicación completamente al pulsar el botón Salir o Cerrar.
     *
     * @param event El evento del botón.
     */
    @FXML
    private void cerrarApp(ActionEvent event) {
        Stage stage = (Stage) cardLogin.getScene().getWindow();
        stage.close();
    }
}
