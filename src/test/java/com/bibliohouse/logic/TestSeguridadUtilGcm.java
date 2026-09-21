/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.bibliohouse.logic;

import com.bibliohouse.utils.SeguridadUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la modernización criptográfica en SeguridadUtil:
 * - Cifrado y descifrado seguro con AES-256-GCM.
 * - Variación de texto cifrado por IV criptográfico aleatorio.
 * - Formato con prefijo v2:.
 * - Compatibilidad hacia atrás con el algoritmo legado AES-ECB.
 * - Resiliencia ante manipulación de datos (tampering).
 */
public class TestSeguridadUtilGcm {

    @Test
    @DisplayName("Debe cifrar y descifrar correctamente usando AES-256-GCM")
    public void testEncriptarYDesencriptarGcm() {
        String original = "MiContraseñaSúperSecreta_NextCloud_2026!#@áéíóúñ";
        String cifrado = SeguridadUtil.encriptar(original);

        assertNotNull(cifrado);
        assertTrue(cifrado.startsWith("v2:"), "El texto cifrado con el nuevo formato debe comenzar con 'v2:'");
        assertNotEquals(original, cifrado);

        String descifrado = SeguridadUtil.desencriptar(cifrado);
        assertEquals(original, descifrado, "El texto descifrado debe coincidir exactamente con el original");
    }

    @Test
    @DisplayName("Cifrar el mismo texto dos veces debe generar textos cifrados distintos gracias al IV aleatorio")
    public void testVectorInicializacionAleatorio() {
        String texto = "contraseña_repetida_123";

        String cifrado1 = SeguridadUtil.encriptar(texto);
        String cifrado2 = SeguridadUtil.encriptar(texto);

        assertNotEquals(cifrado1, cifrado2, "Dos llamadas de cifrado con el mismo texto deben generar cadenas diferentes por el IV");
        assertEquals(texto, SeguridadUtil.desencriptar(cifrado1));
        assertEquals(texto, SeguridadUtil.desencriptar(cifrado2));
    }

    @Test
    @DisplayName("Debe descifrar correctamente credenciales cifradas con el modo legado AES-ECB (retrocompatibilidad)")
    public void testRetrocompatibilidadLegada() throws Exception {
        String textoOriginal = "password_creada_en_version_previa";

        // Simular una contraseña cifrada con la versión anterior (modo ECB)
        java.lang.reflect.Method metodoLegado = SeguridadUtil.class.getDeclaredMethod("encriptarLegado", String.class);
        metodoLegado.setAccessible(true);
        String cifradoLegado = (String) metodoLegado.invoke(null, textoOriginal);

        assertFalse(cifradoLegado.startsWith("v2:"), "El formato legado no debe tener prefijo v2:");

        // Desencriptar con la versión actual debe reconocer el formato y descifrarlo correctamente
        String descifrado = SeguridadUtil.desencriptar(cifradoLegado);
        assertEquals(textoOriginal, descifrado, "Debe descifrar correctamente credenciales del formato legado");
    }

    @Test
    @DisplayName("Debe manejar cadenas nulas y vacías sin excepciones")
    public void testCadenasVaciasYNulas() {
        assertEquals("", SeguridadUtil.encriptar(null));
        assertEquals("", SeguridadUtil.encriptar(""));
        assertEquals("", SeguridadUtil.desencriptar(null));
        assertEquals("", SeguridadUtil.desencriptar(""));
    }

    @Test
    @DisplayName("La alteración de un solo byte en el texto cifrado debe fallar la verificación de integridad GCM")
    public void testDeteccionManipulacionTampering() {
        String original = "clave_de_acceso_critica";
        String cifrado = SeguridadUtil.encriptar(original);

        // Modificar un carácter del payload Base64 para simular corrupción o manipulación maliciosa
        char ultimoChar = cifrado.charAt(cifrado.length() - 1);
        char charModificado = (ultimoChar == 'A') ? 'B' : 'A';
        String cifradoManipulado = cifrado.substring(0, cifrado.length() - 1) + charModificado;

        // La autenticación GCM (Tag) debe detectar la manipulación y devolver el fallback sin lanzar excepción
        String resultado = SeguridadUtil.desencriptar(cifradoManipulado);
        assertNotEquals(original, resultado, "Un texto manipulado no debe descifrarse al contenido original");
        assertEquals(cifradoManipulado, resultado, "Ante fallo de autenticación GCM debe devolver el texto manipulado de fallback");
    }
}
