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

import com.bibliohouse.logic.Socio;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador para crear o editar socios. Aquí metemos los datos de la gente a
 * la que vamos a prestar libros (y perseguir si no los devuelven a tiempo).
 *
 * @author Fernando Lago
 * @version 1.3
 */
public class GestionSociosController {

    @FXML
    private TextField txtNumeroSocio;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtApellidos;
    @FXML
    private TextField txtDni;
    @FXML
    private TextField txtDomicilio;
    @FXML
    private Label lblTitulo; // Etiqueta para el título de la ventana (Crear/Editar)
    private Socio socioEnEdicion = null; // Objeto que estamos creando o editando
    private boolean guardado = false;

    // --- METODOS DE CONFIGURACION ---
    /**
     * Recibe la lista actual de socios para calcular el siguiente ID
     * automáticamente (Modo Creación).
     *
     * @param listaExistente La lista de socios que ya existen en el sistema.
     */
    public void setListaSocios(List<Socio> listaExistente) {
        int maxId = 0;

        if (listaExistente != null && !listaExistente.isEmpty()) {
            for (Socio s : listaExistente) {
                if (s.getNumeroSocio() > maxId) {
                    maxId = s.getNumeroSocio();
                }
            }
        }

        // El siguiente ID será el máximo encontrado + 1
        int siguienteId = maxId + 1;
        txtNumeroSocio.setText(String.valueOf(siguienteId));
        txtNumeroSocio.setEditable(false); // No se debe editar el ID en creación

        if (lblTitulo != null) {
            lblTitulo.setText("Nuevo Socio");
        }
    }

    /**
     * Carga los datos de un socio para ser editado (Modo Edición).
     *
     * @param socio El socio que queremos editar.
     */
    public void setSocioToEdit(Socio socio) {
        this.socioEnEdicion = socio;

        txtNumeroSocio.setText(String.valueOf(socio.getNumeroSocio()));
        txtNumeroSocio.setEditable(false); // El ID nunca se edita
        txtNombre.setText(socio.getNombre());
        txtApellidos.setText(socio.getApellidos());
        txtDni.setText(socio.getDni());
        txtDomicilio.setText(socio.getDomicilio());

        if (lblTitulo != null) {
            lblTitulo.setText("Editar Socio: " + socio.getNumeroSocio());
        }
    }

    // --- METODOS DE ACCION ---
    /**
     * Obtiene el socio que ha sido creado o modificado.
     *
     * @return El objeto Socio con los datos del formulario.
     */
    public Socio getSocioCreado() {
        return socioEnEdicion;
    }

    /**
     * Indica si la operación de guardar se ha realizado con éxito.
     *
     * @return true si se ha pulsado Guardar, false si se ha cancelado.
     */
    public boolean isGuardado() {
        return guardado;
    }

    /**
     * Valida los datos y guarda la información del socio. Si es un nuevo socio,
     * lo crea. Si es edición, actualiza los datos.
     *
     * @param event El evento de acción del botón.
     */
    @FXML
    private void guardar(ActionEvent event) {
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();

        if (nombre.isEmpty() || apellidos.isEmpty()) {
            mostrarAlerta("Datos incompletos", "El Nombre y los Apellidos son obligatorios.");
            return;
        }

        // Si socioEnEdicion es null, estamos en modo CREACIÓN.
        if (socioEnEdicion == null) {
            socioEnEdicion = new Socio(); // Creamos el objeto si es nuevo
            socioEnEdicion.setNumeroSocio(Integer.parseInt(txtNumeroSocio.getText()));
        }

        // Actualizamos los datos (esto funciona tanto para CREACIÓN como para EDICIÓN)
        socioEnEdicion.setNombre(nombre);
        socioEnEdicion.setApellidos(apellidos);
        socioEnEdicion.setDni(txtDni.getText().trim());
        socioEnEdicion.setDomicilio(txtDomicilio.getText().trim());

        guardado = true;
        cerrarVentana();
    }

    /**
     * Cancela la operación y cierra la ventana sin guardar cambios.
     *
     * @param event El evento de acción.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        guardado = false;
        cerrarVentana();
    }

    /**
     * Cierra la ventana actual de la aplicación.
     */
    private void cerrarVentana() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    /**
     * Muestra una alerta de tipo advertencia al usuario.
     *
     * @param titulo Título de la alerta (ej: "Error" o "Advertencia").
     * @param contenido Mensaje detallado que se mostrará en la alerta.
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
