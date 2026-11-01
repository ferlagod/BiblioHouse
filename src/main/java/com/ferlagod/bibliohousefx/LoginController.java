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


import com.bibliohouse.logic.LanguageManager;
import com.bibliohouse.logic.Autentificacion;
import com.bibliohouse.logic.JsonManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

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
            setError(lblLoginError, LanguageManager.getString("ui.por_favor_rellena_todos_los_campos", "Por favor, rellena todos los campos."));
            return;
        }
        // Mismas reglas que al registrarse (descarta ".", "..", rutas, etc.)
        if (!user.matches("^[a-zA-Z0-9_]+$")) {
            setError(lblLoginError, LanguageManager.getString("ui.nombre_de_usuario_no_valido", "Nombre de usuario no válido."));
            return;
        }

        // SEC-03: Canonicalización para prevenir Path Traversal
        File userDir;
        try {
            File base = new File(USERS_BASE_PATH).getCanonicalFile();
            userDir = new File(base, user).getCanonicalFile();
            if (!userDir.getPath().startsWith(base.getPath() + File.separator)
                    && !userDir.equals(base)) {
                setError(lblLoginError, LanguageManager.getString("ui.nombre_de_usuario_no_valido", "Nombre de usuario no válido."));
                return;
            }
        } catch (IOException e) {
            setError(lblLoginError, LanguageManager.getString("ui.error_al_verificar_el_usuario", "Error al verificar el usuario."));
            return;
        }

        File authFile = new File(userDir, AUTH_FILE_NAME);

        if (!userDir.exists() || !authFile.exists()) {
            setError(lblLoginError, LanguageManager.getString("ui.el_usuario_no_existe_crea_una", "El usuario no existe. Crea una cuenta primero."));
            return;
        }

        String storedHash;
        try (FileReader reader = new FileReader(authFile)) {
            Properties props = new Properties();
            props.load(reader);
            storedHash = props.getProperty("passwordHash");
        } catch (IOException e) {
            setError(lblLoginError, LanguageManager.getString("ui.error_al_leer_datos_del_usuario", "Error al leer datos del usuario."));
            return;
        }

        // Verificar contraseña en segundo plano: BCrypt tarda ~300 ms a
        // propósito y congelaría la ventana si se hiciera en el hilo de la UI
        final String rutaUsuario = userDir.getAbsolutePath();
        javafx.concurrent.Task<Boolean> verificacion = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                boolean ok = Autentificacion.checkPassword(pass, storedHash);
                // SEC-01: Migración silenciosa SHA-256 → BCrypt en el primer login
                if (ok && Autentificacion.isLegacyHash(storedHash)) {
                    migrateHashIfLegacy(authFile, pass);
                }
                return ok;
            }
        };
        cardLogin.setDisable(true);
        lblLoginError.setVisible(false);
        verificacion.setOnSucceeded(e -> {
            cardLogin.setDisable(false);
            if (Boolean.TRUE.equals(verificacion.getValue())) {
                entrarALaApp(user, rutaUsuario);
            } else {
                setError(lblLoginError, LanguageManager.getString("ui.contrasena_incorrecta", "Contraseña incorrecta."));
                txtLoginPass.clear();
                txtLoginPass.requestFocus();
            }
        });
        verificacion.setOnFailed(e -> {
            cardLogin.setDisable(false);
            setError(lblLoginError, LanguageManager.getString("ui.error_al_verificar_la_contrasena", "Error al verificar la contraseña."));
        });
        com.bibliohouse.utils.Hilos.ejecutar(verificacion);
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
            setError(lblRegError, LanguageManager.getString("ui.rellena_todos_los_campos", "Rellena todos los campos."));
            return;
        }
        if (!user.matches("^[a-zA-Z0-9_]+$")) {
            setError(lblRegError, LanguageManager.getString("ui.el_usuario_solo_puede_tener_letras", "El usuario solo puede tener letras, números y _"));
            return;
        }
        // SEC-04: Mínimo 8 caracteres para nuevas cuentas
        if (pass.length() < 8) {
            setError(lblRegError, LanguageManager.getString("ui.la_contrasena_debe_tener_al_menos", "La contraseña debe tener al menos 8 caracteres."));
            return;
        }
        if (!pass.equals(confirm)) {
            setError(lblRegError, LanguageManager.getString("ui.las_contrasenas_no_coinciden", "Las contraseñas no coinciden."));
            return;
        }

        File userDir = new File(USERS_BASE_PATH, user);
        if (userDir.exists()) {
            setError(lblRegError, LanguageManager.getString("ui.este_usuario_ya_existe", "Este usuario ya existe."));
            return;
        }

        // El hash BCrypt se calcula en segundo plano para no congelar la ventana
        cardRegister.setDisable(true);
        javafx.concurrent.Task<String> hashTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return Autentificacion.hashPassword(pass);
            }
        };
        hashTask.setOnSucceeded(e -> {
            cardRegister.setDisable(false);
            crearUsuario(user, userDir, hashTask.getValue());
        });
        hashTask.setOnFailed(e -> {
            cardRegister.setDisable(false);
            setError(lblRegError, LanguageManager.getString("ui.no_se_pudo_crear_el_usuario", "No se pudo crear el usuario."));
        });
        com.bibliohouse.utils.Hilos.ejecutar(hashTask);
    }

    /**
     * Crea en disco la carpeta y el archivo de autenticación del usuario.
     *
     * @param user Nombre de usuario.
     * @param userDir Carpeta del usuario.
     * @param hash Hash BCrypt de su contraseña.
     */
    private void crearUsuario(String user, File userDir, String hash) {
        if (userDir.exists()) {
            setError(lblRegError, LanguageManager.getString("ui.este_usuario_ya_existe", "Este usuario ya existe."));
            return;
        }
        if (userDir.mkdirs()) {
            File authFile = new File(userDir, AUTH_FILE_NAME);

            Properties props = new Properties();
            props.setProperty("passwordHash", hash);

            try (FileWriter writer = new FileWriter(authFile)) {
                props.store(writer, "BiblioHouse User Auth");
            } catch (IOException e) {
                setError(lblRegError, LanguageManager.getString("ui.error_al_guardar_el_usuario_en", "Error al guardar el usuario en disco."));
                userDir.delete(); // Seguro porque aún no hay subdirectorios
                return;
            }

            // Inicializar DESPUÉS de que el auth file esté confirmado en disco
            inicializarDatosUsuario(userDir.getAbsolutePath());

            // Mensaje de éxito
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(LanguageManager.getString("ui.cuenta_creada", "Cuenta creada"));
            alert.setHeaderText(null);
            alert.setContentText(LanguageManager.getString("ui.usuario_creado_con_exito_ahora_puedes", "¡Usuario creado con éxito! Ahora puedes iniciar sesión."));
            alert.showAndWait();

            // Volver al login automáticamente
            mostrarLogin(null);
            txtLoginUser.setText(user); // Rellenar usuario por comodidad
        } else {
            setError(lblRegError, LanguageManager.getString("ui.no_se_pudo_crear_la_carpeta", "No se pudo crear la carpeta del usuario."));
        }
    }

    /**
     * Migra silenciosamente un hash SHA-256 legado a BCrypt tras un login
     * exitoso. Si la migración falla, el usuario puede seguir usando el hash
     * antiguo.
     *
     * @param authFile Archivo user.auth donde actualizar el hash.
     * @param plainPassword Contraseña en texto plano (disponible justo después
     * del login).
     */
    private void migrateHashIfLegacy(File authFile, String plainPassword) {
        try {
            Properties props = new Properties();
            try (FileReader r = new FileReader(authFile)) {
                props.load(r);
            }
            String newHash = Autentificacion.hashPassword(plainPassword);
            props.setProperty("passwordHash", newHash);
            try (FileWriter w = new FileWriter(authFile)) {
                props.store(w, "BiblioHouse User Auth");
            }
            LOGGER.log(Level.INFO, "Hash de contraseña migrado a BCrypt correctamente.");
        } catch (IOException e) {
            // No crítico: el usuario puede seguir usando el hash SHA-256 hasta la próxima vez
            LOGGER.log(Level.WARNING, "No se pudo migrar el hash de contraseña a BCrypt.", e);
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
            newMgr.shutdown();
            LOGGER.log(Level.INFO, "Estanterías por defecto creadas en: {0}", path);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudieron crear las estanterías por defecto", e);
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
            alert.setTitle(LanguageManager.getString("ui.error_critico", "Error Crítico"));
            alert.setHeaderText(LanguageManager.getString("ui.no_se_pudo_iniciar_la_aplicacion", "No se pudo iniciar la aplicación"));
            alert.setContentText(LanguageManager.getString("ui.detalles_del_error", "Detalles del error: ") + e.getMessage());
            alert.showAndWait();
            setError(lblLoginError, LanguageManager.getString("ui.error_critico_2", "Error crítico: ") + e.getMessage());
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
