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
package com.bibliohouse.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;

/**
 * Utilidad para encriptar y desencriptar cadenas de texto usando AES con una
 * clave única basada en información del hardware del equipo. Esto permite que
 * los datos encriptados solo puedan ser leídos en el mismo equipo donde se
 * encriptaron.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class SeguridadUtil {

    /**
     * Algoritmo de encriptación utilizado (AES).
     */
    private static final String ALGORITMO = "AES";

    /**
     * Genera una clave única de 128 bits (AES) basada en información del
     * hardware del equipo. La clave se deriva de propiedades del sistema como
     * el nombre del OS, arquitectura y usuario.
     *
     * @return Clave secreta para AES.
     * @throws Exception Si falla la generación del hash SHA-256.
     */
    private static SecretKeySpec generarClaveUnica() throws Exception {
        // Obtenemos información del hardware
        String infoHardware = System.getProperty("os.name")
                + System.getProperty("os.arch")
                + System.getProperty("user.name");

        byte[] claveBytes = infoHardware.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        claveBytes = sha.digest(claveBytes);
        claveBytes = Arrays.copyOf(claveBytes, 16); // 128 bits para máxima compatibilidad

        return new SecretKeySpec(claveBytes, ALGORITMO);
    }

    /**
     * Encripta una cadena de texto usando AES con la clave única del equipo. Si
     * falla la encriptación, devuelve el texto original.
     *
     * @param texto Texto a encriptar.
     * @return Texto encriptado en Base64, o el texto original si falla.
     */
    public static String encriptar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, generarClaveUnica());
            byte[] encriptado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encriptado);
        } catch (Exception e) {
            return texto; // Fallback: devuelve el texto original
        }
    }

    /**
     * Desencripta una cadena de texto encriptada con AES usando la clave única
     * del equipo. Si falla la desencriptación, devuelve el texto original.
     *
     * @param textoEncriptado Texto encriptado en Base64.
     * @return Texto desencriptado, o el texto original si falla.
     */
    public static String desencriptar(String textoEncriptado) {
        if (textoEncriptado == null || textoEncriptado.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, generarClaveUnica());
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(textoEncriptado));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return textoEncriptado; // Fallback: devuelve el texto original
        }
    }
}
