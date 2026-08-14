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
 * Clase de prueba para comparar el análisis de URIs con caracteres especiales,
 * como el '@' en el nombre de usuario. Muestra cómo el codificado (%40) y el
 * carácter literal '@' afectan al resultado de {@link java.net.URI#getPath()}.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class TestSardineURL {

    /**
     * Método principal que demuestra la diferencia en el análisis de URIs
     * cuando el carácter '@' está codificado (%40) o no.
     *
     * @param args Argumentos de la línea de comandos (no se utilizan).
     * @throws Exception Si ocurre un error al crear las URIs.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("No passwords available, but we can check URI parsing.");

        // URI con el '@' codificado como %40 en el nombre de usuario.
        java.net.URI uri = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago%40oniros.eu/");
        System.out.println("URI path: " + uri.getPath());

        // URI con el '@' sin codificar en el nombre de usuario.
        java.net.URI uri2 = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago@oniros.eu/");
        System.out.println("URI2 path: " + uri2.getPath());
    }
}
