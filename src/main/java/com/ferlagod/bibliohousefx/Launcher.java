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

import javafx.application.Application;

/**
 * Clase lanzadora para evitar problemas con módulos en JavaFX. Simplemente
 * llama al main de la clase App.
 *
 * @author Fernando Lago
 * @version 1.4
 */
public class Launcher {

    /**
     * Punto de entrada principal de la aplicación. Inicia la aplicación JavaFX
     * llamando al método launch de la clase {@link Application}, pasando la
     * clase principal {@link App} y los argumentos de línea de comandos.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
