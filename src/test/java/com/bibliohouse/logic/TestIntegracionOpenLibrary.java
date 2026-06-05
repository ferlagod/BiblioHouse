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
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
class TestIntegracionOpenLibrary {

    /**
     * Prueba de integración para buscar un libro conocido en OpenLibrary.
     * Realiza una búsqueda real a la API y verifica que devuelva resultados
     * válidos. Requiere conexión a internet. Si falla, puede deberse a
     * problemas de red o API.
     */
    @Test
    @Tag("integration")
    void testBuscarLibroConocido() {
        // Solo se ejecuta si hay conexión a internet (se asume en contexto de integración)
        String query = "The Hobbit";
        List<Libro> resultados = OpenLibraryCliente.buscarLibros(query);

        // OpenLibrary debería devolver resultados para "The Hobbit"
        assertNotNull(resultados, "La lista de resultados no debería ser nula");

        // Si la API falla o no hay red, el cliente devuelve una lista vacía (y registra el error)
        if (resultados.isEmpty()) {
            fail("No se encontraron resultados para '" + query + "'. Verifica la conexión a internet o el estado de la API.");
        }

        // Verifica que los resultados contengan datos relevantes
        boolean encontrado = resultados.stream()
                .anyMatch(libro -> libro.getTitulo().toLowerCase().contains("hobbit"));

        assertTrue(encontrado, "Debería haber al menos un libro con 'Hobbit' en el título");
    }

}
