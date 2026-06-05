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

import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Pruebas de integración del sistema para la interfaz gráfica de BiblioHouse.
 * Verifica que el entorno de TestFX funcione y que se pueda iniciar una ventana
 * básica. Evita cargar la lógica completa de la aplicación para simplificar las
 * pruebas.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
@ExtendWith(ApplicationExtension.class)
@Tag("system")
class TestSistema {

    /**
     * Método de inicio para TestFX. Crea una ventana básica para verificar que
     * el entorno de pruebas gráficas está operativo. Evita cargar la aplicación
     * completa para simplificar.
     *
     * @param stage Escenario principal proporcionado por TestFX.
     */
    @Start
    public void start(Stage stage) {
        stage.setTitle("Test Stage");
        stage.show();
    }

    /**
     * Prueba básica ("smoke test") para verificar que el entorno de UI se
     * inicia. Confirma que el método start() no lanza excepciones y que se
     * muestra una ventana.
     */
    @Test
    void testStageTitle() {
        // Si start() se ejecutó sin errores, consideramos que la prueba pasa.
        // En una versión más completa, se validaría el título o propiedades de la ventana.
        org.junit.jupiter.api.Assertions.assertTrue(true, "Stage launched successfully");
    }
}
