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
package com.bibliohouse.utils;

import java.util.regex.Pattern;
import javafx.scene.control.Labeled;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Iconos vectoriales de la interfaz (conjunto Feather de Ikonli). Sustituyen a
 * los emojis, que cada sistema operativo dibuja con un estilo y tamaño
 * distintos (y algunos ni siquiera muestra). El color lo toman del tema
 * (AtlantaFX) o de la regla CSS {@code .ikonli-font-icon} del contenedor.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.3
 */
public final class Iconos {

    /** Tamaño por defecto, acorde con el texto de 13 px de la interfaz. */
    public static final int TAMANO = 14;

    /**
     * Símbolos o emojis al principio de un texto (p. ej. «📖 Leyendo»,
     * «✓ Leído»), seguidos de espacios.
     */
    private static final Pattern SIMBOLO_INICIAL = Pattern.compile("^[^\\p{L}\\p{N}]+\\s*");

    private Iconos() {
    }

    /**
     * Crea un icono del tamaño por defecto.
     *
     * @param ikon Icono (p. ej. {@code Feather.BOOK}).
     * @return Nodo del icono.
     */
    public static FontIcon de(Ikon ikon) {
        return de(ikon, TAMANO);
    }

    /**
     * Crea un icono del tamaño indicado.
     *
     * @param ikon Icono.
     * @param tamano Tamaño en píxeles.
     * @return Nodo del icono.
     */
    public static FontIcon de(Ikon ikon, int tamano) {
        FontIcon icono = new FontIcon(ikon);
        icono.setIconSize(tamano);
        return icono;
    }

    /**
     * Quita el emoji o símbolo con el que empiezan algunos textos traducidos,
     * para mostrar en su lugar un icono vectorial. Si el texto no empieza por
     * un símbolo se devuelve igual.
     *
     * @param texto Texto original.
     * @return Texto sin el símbolo inicial.
     */
    public static String sinSimboloInicial(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = SIMBOLO_INICIAL.matcher(texto).replaceFirst("");
        return limpio.isEmpty() ? texto : limpio;
    }

    /**
     * Pone un icono a un control con texto, quitando el emoji inicial que
     * tuviera el texto.
     *
     * @param control Botón, etiqueta…
     * @param ikon Icono a mostrar a la izquierda del texto.
     */
    public static void aplicar(Labeled control, Ikon ikon) {
        if (control == null) {
            return;
        }
        control.setText(sinSimboloInicial(control.getText()));
        control.setGraphic(de(ikon));
    }
}
