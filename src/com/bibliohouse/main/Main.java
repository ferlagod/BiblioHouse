/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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
package com.bibliohouse.main;

import com.bibliohouse.ui.VentanaPrincipal;
import javax.swing.SwingUtilities;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

/**
 * Clase principal que inicia la aplicación BiblioHouse. Su único propósito es
 * lanzar la interfaz gráfica de forma segura.
 *
 * @author ferlagod
 * @version 0.6
 */
public class Main {

    /**
     * Método principal que inicia la aplicación.
     *
     * @param args Argumentos de la línea de comandos (no se utilizan).
     */
    public static void main(String[] args) {

        Preferences prefs = Preferences.userNodeForPackage(VentanaPrincipal.class);
        String themeClassName = prefs.get("appTheme", "com.formdev.flatlaf.themes.FlatMacLightLaf"); // Tema claro por defecto

        try {
            UIManager.setLookAndFeel(themeClassName);
        } catch (Exception e) {
            System.err.println("No se pudo inicializar el Look and Feel de FlatLaf.");
        }

        // Inicia la interfaz gráfica de usuario en el Event Dispatch Thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaPrincipal ventana = new VentanaPrincipal();
                ventana.setVisible(true);
            }
        });
    }
}
