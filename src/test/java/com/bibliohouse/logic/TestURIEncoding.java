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
 * Clase de prueba para comparar el comportamiento de los métodos
 * {@link java.net.URI#toASCIIString()} y {@link java.net.URI#toString()} en
 * URIs con caracteres especiales como '@'. También muestra cómo afecta el
 * codificado (%40) al resultado de {@link java.net.URI#getPath()}.
 *
 * @author Fernando lago Dávila
 * @version 2.1
 */
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestURIEncoding {

    @Test
    public void testUriEncodingDifferences() throws Exception {
        java.net.URI uri = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago%40oniros.eu/");
        assertNotNull(uri.toASCIIString());
        assertEquals("/remote.php/dav/files/fernando.lago@oniros.eu/", uri.getPath());
        assertEquals("/remote.php/dav/files/fernando.lago%40oniros.eu/", uri.getRawPath());

        java.net.URI uri2 = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago@oniros.eu/");
        assertNotNull(uri2.toASCIIString());
        assertEquals("/remote.php/dav/files/fernando.lago@oniros.eu/", uri2.getPath());
        assertEquals("/remote.php/dav/files/fernando.lago@oniros.eu/", uri2.getRawPath());
    }

    public static void main(String[] args) throws Exception {
        // URI con '@' codificado como %40.
        java.net.URI uri = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago%40oniros.eu/");
        System.out.println("URI toASCIIString: " + uri.toASCIIString());
        System.out.println("URI toString: " + uri.toString());

        // URI con '@' sin codificar.
        java.net.URI uri2 = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago@oniros.eu/");
        System.out.println("URI2 toASCIIString: " + uri2.toASCIIString());
        System.out.println("URI2 toString: " + uri2.toString());

        // Comparación de los paths extraídos.
        System.out.println("URI 1 Path: " + uri.getPath());
        System.out.println("URI 2 Path: " + uri2.getPath());
    }
}
