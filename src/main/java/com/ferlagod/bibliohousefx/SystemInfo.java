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

/**
 * Clase de utilidad para obtener información del sistema. Proporciona versiones
 * de Java y JavaFX en uso.
 *
 * @author Fernando Lago
 * @version 1.1
 */
public class SystemInfo {

    /**
     * Obtiene la versión de Java que se está ejecutando.
     *
     * @return String con la versión de Java.
     */
    public static String javaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Obtiene la versión de JavaFX que se está ejecutando.
     *
     * @return String con la versión de JavaFX.
     */
    public static String javafxVersion() {
        return System.getProperty("javafx.version");
    }

}
