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

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controlador de la ventana de Ayuda (Manual de Usuario). Muestra un WebView
 * con el contenido HTML correspondiente al idioma seleccionado.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class HelpController implements Initializable {

    @FXML
    private WebView webView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (webView != null) {
            loadHelpContent();
        }
    }

    /**
     * Carga el archivo HTML correspondiente al idioma actual de la aplicación.
     * Si no existe el archivo específico, carga el de español o inglés por
     * defecto.
     */
    private void loadHelpContent() {
        Locale current = App.getCurrentLocale();
        String lang = current.getLanguage(); // es, en, ca, gl, eu, pt...

        String htmlFile = "/com/ferlagod/bibliohousefx/help/manual_" + lang + ".html";
        URL resource = getClass().getResource(htmlFile);

        // Fallback a español si no existe el idioma específico
        if (resource == null) {
            htmlFile = "/com/ferlagod/bibliohousefx/help/manual_es.html";
            resource = getClass().getResource(htmlFile);
        }

        // Fallback a inglés si tampoco existe español (caso raro)
        if (resource == null) {
            htmlFile = "/com/ferlagod/bibliohousefx/help/manual_en.html";
            resource = getClass().getResource(htmlFile);
        }

        if (resource != null) {
            WebEngine webEngine = webView.getEngine();
            webEngine.load(resource.toExternalForm());
        }
    }
}
