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
import javafx.scene.Node;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la pantallita "Acerca de". Aquí es donde hago un poco de
 * autobombo y pongo mi contacto.
 *
 * @author Fernando Lago
 * @version 1.6
 */
public class AcercaDeController {

    private static final Logger LOGGER = Logger.getLogger(AcercaDeController.class.getName());

    /**
     * Cierra esta ventana. Botón de pánico o de "ya he visto suficiente".
     *
     * @param event El clic del botón.
     */
    @FXML
    private void cerrarVentana(ActionEvent event) {
        // Obtiene la ventana actual a través del botón que lanzó el evento y la cierra
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Intenta abrir tu programa de correo para que me escribas. Si no tienes
     * uno configurado, pues mala suerte, no hace nada (o da error en consola).
     */
    @FXML
    private void abrirEmail() {
        try {
            Desktop.getDesktop().mail(new URI("mailto:info@bibliohouse.org"));
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el cliente de correo.", e);
        }
    }

    /**
     * Abre la página de Liberapay del proyecto en el navegador por defecto del
     * sistema.
     *
     *
     */
    @FXML
    private void abrirLiberapay() {
        try {
            Desktop.getDesktop().browse(new URI("https://liberapay.com/ferlagod./"));
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el navegador.", e);
        }
    }
}
