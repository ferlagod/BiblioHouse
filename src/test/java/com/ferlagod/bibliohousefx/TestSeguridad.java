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

import com.bibliohouse.logic.JsonManager;
import java.io.File;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de seguridad para validar el manejo de rutas en JsonManager. Verifica
 * que se rechacen entradas inválidas y documenta el comportamiento actual
 * frente a intentos de path traversal.
 *
 * @author Fernando Lago Dávila
 * @version 1.4
 */
class TestSeguridad {

    @TempDir
    File tempDir;

    /**
     * Prueba la validación básica de rutas en el constructor de JsonManager.
     * Actualmente, solo verifica que se lancen excepciones para rutas nulas o
     * vacías. Nota: No bloquea explícitamente intentos de path traversal (ej:
     * "../../").
     */
    @Test
    @Tag("security")
    void testPathTraversalInConstructor() {
        // El código actual no bloquea path traversal, pero debería validar entradas nulas/vacías
        assertThrows(IllegalArgumentException.class,
                () -> new JsonManager(null),
                "Debería lanzar excepción si la ruta es nula");

        assertThrows(IllegalArgumentException.class,
                () -> new JsonManager(""),
                "Debería lanzar excepción si la ruta está vacía");

    }
}
