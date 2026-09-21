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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase SystemInfo. Verifica la obtención de
 * información del sistema sin efectos secundarios.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class SystemInfoTest {

    /**
     * Prueba que la versión de Java se obtiene correctamente.
     */
    @Test
    void testJavaVersion() {
        String javaVersion = SystemInfo.javaVersion();
        assertNotNull(javaVersion, "La versión de Java no debería ser nula");
        assertFalse(javaVersion.isEmpty(), "La versión de Java no debería estar vacía");
    }

    /**
     * Prueba que la obtención de la versión de JavaFX no lanza excepciones. El
     * valor puede ser nulo o vacío, pero el método debe ejecutarse sin errores.
     */
    @Test
    void testJavaFxVersion() {
        assertDoesNotThrow(() -> SystemInfo.javafxVersion(),
                "La llamada a javafxVersion() no debería lanzar excepciones");
    }
}
