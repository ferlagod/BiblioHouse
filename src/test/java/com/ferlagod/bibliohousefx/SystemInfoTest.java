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
 * Unit tests for SystemInfo.
 * @author Fernando Lago Dávila
 * @version 1.1
 */
public class SystemInfoTest {

    @Test
    void testJavaVersion() {
        String javaVersion = SystemInfo.javaVersion();
        assertNotNull(javaVersion, "Java version should not be null");
        assertFalse(javaVersion.isEmpty(), "Java version should not be empty");
    }

    @Test
    void testJavaFxVersion() {
        // JavaFX version might be null strings logic...
        // We just want to ensure calling it doesn't throw an exception.
        assertDoesNotThrow(() -> SystemInfo.javafxVersion());
    }
}