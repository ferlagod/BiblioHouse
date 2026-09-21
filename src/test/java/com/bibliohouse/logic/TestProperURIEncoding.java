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
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestProperURIEncoding {

    @Test
    public void testProperUriEncoding() throws Exception {
        String username = "fernando.lago@oniros.eu";
        String encoded = new java.net.URI(null, null, username, null).getRawPath();
        assertNotNull(encoded);
        assertEquals("fernando.lago@oniros.eu", encoded);
    }

    public static void main(String[] args) throws Exception {
        String username = "fernando.lago@oniros.eu";
        String encoded = new java.net.URI(null, null, username, null).getRawPath();
        System.out.println("Properly encoded username for path: " + encoded);
    }
}
