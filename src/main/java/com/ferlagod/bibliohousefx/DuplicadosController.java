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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de resultados de duplicados. Muestra el texto con
 * el análisis de libros duplicados.
 *
 * @author Fernando Lago
 * @version 1.3
 */
public class DuplicadosController {

    @FXML
    private TextArea txtResultados;

    /**
     * Muestra el texto proporcionado en el área de texto de la ventana.
     *
     * @param texto El texto con los resultados del análisis.
     */
    public void setTextoResultados(String texto) {
        txtResultados.setText(texto);
    }

    /**
     * Cierra la ventana de duplicados.
     *
     * @param event El evento del botón Cerrar.
     */
    @FXML
    private void cerrar(ActionEvent event) {
        Stage stage = (Stage) txtResultados.getScene().getWindow();
        stage.close();
    }
}
