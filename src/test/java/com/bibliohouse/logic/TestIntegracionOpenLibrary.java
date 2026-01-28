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

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Clase TestIntegracionOpenLibrary.
 * @author Fernando Lago Dávila
 * @version 1.1
 */
class TestIntegracionOpenLibrary {

    @Test
    @Tag("integration")
    void testBuscarLibroConocido() {
        // Only run if there is internet connection (assumed for integration text
        // context)
        // We can check connection roughly, or just let it fail if no network.
        // For robustness, we could check specific system properties, but let's assume
        // environment is capable.

        String query = "The Hobbit";
        List<Libro> resultados = OpenLibraryCliente.buscarLibros(query);

        // OpenLibrary should definitely return something for "The Hobbit"
        assertNotNull(resultados, "La lista de resultados no debería ser nula");

        // If the API is down or network fails, the client returns empty list (and logs
        // error).
        // So an empty list isn't necessarily a code bug, but an environment failure.
        // However, for an Integration Test, we expect it to work.
        // Warning: This test makes a real HTTP request.

        if (resultados.isEmpty()) {
            // Check if it was due to network
            // Since we can't easily introspect the client logs here without complex setup,
            // we'll fail with a hint.
            fail("No se encontraron resultados para '" + query + "'. Verifica la conexión a internet.");
        }

        // Verify that the results contain relevant data
        boolean foundHash = resultados.stream()
                .anyMatch(libro -> libro.getTitulo().toLowerCase().contains("hobbit"));

        assertTrue(foundHash, "Debería haber al menos un libro con 'Hobbit' en el título");
    }
}