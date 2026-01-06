package com.ferlagod.bibliohousefx;

import javafx.application.Application;

/**
 * Clase lanzadora para evitar problemas con módulos en JavaFX.
 * Simplemente llama al main de la clase App.
 *
 * @author Fernando Lago
 * @version 1.0
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
