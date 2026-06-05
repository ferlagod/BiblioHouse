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
package com.bibliohouse.logic;

/**
 * Clase de prueba para demostrar el codificado correcto de un nombre de usuario
 * en una URI, especialmente cuando contiene caracteres especiales como '@'.
 * Este ejemplo muestra cómo usar la clase {@link java.net.URI} para codificar
 * correctamente el componente de ruta de una URI.
 * 
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class TestProperURIEncoding {

    /**
     * Método principal que ejecuta la prueba de codificación de URI.
     * Toma un nombre de usuario con '@' y lo codifica para que sea válido en una URI.
     *
     * @param args Argumentos de la línea de comandos (no se utilizan en este ejemplo).
     * @throws Exception Si ocurre un error al crear la URI.
     */
    public static void main(String[] args) throws Exception {
        String username = "fernando.lago@oniros.eu";
        // Crea una URI con el nombre de usuario como componente de ruta.
        // El constructor de URI codifica automáticamente los caracteres especiales.
        String encoded = new java.net.URI(null, null, username, null).getRawPath();
        System.out.println("Properly encoded username for path: " + encoded);
    }
}