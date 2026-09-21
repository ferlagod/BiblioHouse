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
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * Utilidad criptográfica moderna para encriptar y desencriptar cadenas de texto sensibles
 * (como credenciales de NextCloud) usando AES-256-GCM con autenticación de integridad (AEAD),
 * vector de inicialización (IV) criptográfico aleatorio por cada operación y clave maestra
 * persistida en el perfil del usuario.
 *
 * Mantiene compatibilidad transparente hacia atrás con datos cifrados en versiones previas
 * mediante el modo legado AES-ECB.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class SeguridadUtil {

    private static final String ALGORITMO_GCM = "AES/GCM/NoPadding";
    private static final String ALGORITMO_LEGACY = "AES";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recomendado por NIST SP 800-38D
    private static final int GCM_TAG_LENGTH = 128; // 128 bits de tag de autenticación
    private static final String PREFIX_V2 = "v2:";
    private static final String PREF_KEY_MASTER = "vault_master_key";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static SecretKeySpec masterKey;

    /**
     * Obtiene o genera la clave maestra de 256 bits protegida en el almacén de preferencias
     * del usuario del sistema operativo.
     *
     * @return Clave de 256 bits para AES.
     */
    private static synchronized SecretKeySpec getMasterKey() {
        if (masterKey != null) {
            return masterKey;
        }

        try {
            Preferences prefs = Preferences.userNodeForPackage(SeguridadUtil.class);
            String storedKey = prefs.get(PREF_KEY_MASTER, null);

            if (storedKey != null && !storedKey.trim().isEmpty()) {
                byte[] keyBytes = Base64.getDecoder().decode(storedKey.trim());
                if (keyBytes.length == 32) { // 256 bits
                    masterKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
                    return masterKey;
                }
            }

            // Generar nueva clave criptográfica de 256 bits
            byte[] newKeyBytes = new byte[32];
            SECURE_RANDOM.nextBytes(newKeyBytes);
            prefs.put(PREF_KEY_MASTER, Base64.getEncoder().encodeToString(newKeyBytes));
            prefs.flush();

            masterKey = new SecretKeySpec(newKeyBytes, KEY_ALGORITHM);
            return masterKey;
        } catch (Exception e) {
            // Fallback determinista seguro en caso de error de acceso a Preferences
            return getFallbackKey();
        }
    }

    private static SecretKeySpec getFallbackKey() {
        try {
            String seed = System.getProperty("user.home", "") + "BiblioHouseSecureSalt2026";
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (Exception ex) {
            throw new RuntimeException("Error inicializando clave de seguridad", ex);
        }
    }

    /**
     * Genera la clave de 128 bits derivada de propiedades del sistema para permitir
     * descifrar contraseñas previamente almacenadas con el formato legado.
     *
     * @return Clave secreta para AES legado.
     * @throws Exception Si falla la generación del hash SHA-256.
     */
    private static SecretKeySpec generarClaveLegada() throws Exception {
        String infoHardware = System.getProperty("os.name")
                + System.getProperty("os.arch")
                + System.getProperty("user.name");

        byte[] claveBytes = infoHardware.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        claveBytes = sha.digest(claveBytes);
        claveBytes = Arrays.copyOf(claveBytes, 16);

        return new SecretKeySpec(claveBytes, ALGORITMO_LEGACY);
    }

    /**
     * Encripta una cadena de texto usando AES-256-GCM con un vector de inicialización (IV)
     * criptográficamente aleatorio generado para cada cifrado.
     *
     * @param texto Texto a encriptar.
     * @return Texto encriptado con prefijo "v2:" y contenido Base64, o el texto original si ocurre un error.
     */
    public static String encriptar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getMasterKey(), spec);

            byte[] cipherBytes = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);

            return PREFIX_V2 + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            return texto; // Fallback: devuelve el texto original
        }
    }

    /**
     * Desencripta una cadena de texto. Detecta automáticamente si el contenido fue
     * cifrado con el formato moderno AES-256-GCM (prefijo "v2:") o con el formato legado
     * AES-ECB, proporcionando compatibilidad hacia atrás completa y transparente.
     *
     * @param textoEncriptado Texto encriptado en Base64.
     * @return Texto desencriptado, o el texto original si falla la autenticación o descifrado.
     */
    public static String desencriptar(String textoEncriptado) {
        if (textoEncriptado == null || textoEncriptado.isEmpty()) {
            return "";
        }

        if (textoEncriptado.startsWith(PREFIX_V2)) {
            // Formato moderno AES-256-GCM
            try {
                String payload = textoEncriptado.substring(PREFIX_V2.length());
                byte[] decoded = Base64.getDecoder().decode(payload);

                if (decoded.length < GCM_IV_LENGTH) {
                    return textoEncriptado;
                }

                byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
                byte[] cipherBytes = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

                Cipher cipher = Cipher.getInstance(ALGORITMO_GCM);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec);

                byte[] plainBytes = cipher.doFinal(cipherBytes);
                return new String(plainBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return textoEncriptado; // Fallback: devuelve texto original ante manipulación o error
            }
        } else {
            // Formato legado AES-ECB para retrocompatibilidad
            try {
                Cipher cipher = Cipher.getInstance(ALGORITMO_LEGACY);
                cipher.init(Cipher.DECRYPT_MODE, generarClaveLegada());
                byte[] original = cipher.doFinal(Base64.getDecoder().decode(textoEncriptado));
                return new String(original, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return textoEncriptado; // Fallback: devuelve el texto original
            }
        }
    }

    /**
     * Cifra una cadena usando el algoritmo legado AES-ECB derivado del hardware/usuario.
     * Método auxiliar de paquete destinado a pruebas unitarias de retrocompatibilidad.
     *
     * @param texto Cadena en texto plano a cifrar.
     * @return Texto cifrado codificado en Base64.
     * @throws Exception Si ocurre un fallo durante la inicialización del cifrado.
     */
    static String encriptarLegado(String texto) throws Exception {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        Cipher cipher = Cipher.getInstance(ALGORITMO_LEGACY);
        cipher.init(Cipher.ENCRYPT_MODE, generarClaveLegada());
        byte[] encriptado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encriptado);
    }
}
