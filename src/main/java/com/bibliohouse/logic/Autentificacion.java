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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Gestiona el hashing y verificación de contraseñas de forma segura.
 *
 * Compatibilidad con versiones anteriores: los hashes generados con la versión
 * antigua (SHA-256 en Base64 puro, sin prefijo "$2a$") siguen siendo
 * reconocidos. Al primer login exitoso con un hash antiguo, el sistema lo migra
 * automáticamente a BCrypt sin que el usuario lo note.
 *
 * <p>
 * Los nuevos hashes siempre usan BCrypt (factor de coste 12).</p>
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class Autentificacion {

    private static final Logger LOGGER = Logger.getLogger(Autentificacion.class.getName());

    /**
     * Factor de coste BCrypt. 12 rounds ≈ ~300 ms en hardware moderno,
     * suficientemente lento para desalentar ataques de fuerza bruta.
     */
    private static final int BCRYPT_ROUNDS = 12;

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Genera un hash BCrypt seguro (con salt integrado) para una contraseña.
     * Todos los usuarios nuevos o actualizados usarán este método.
     *
     * @param password La contraseña en texto plano.
     * @return El hash BCrypt listo para almacenar, o {@code null} si la
     * contraseña es nula o vacía.
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifica si una contraseña coincide con su hash almacenado. Detecta
     * automáticamente si el hash es del formato antiguo (SHA-256 Base64) o del
     * nuevo formato (BCrypt).
     *
     * @param inputPassword La contraseña introducida por el usuario.
     * @param storedHash El hash almacenado en disco.
     * @return {@code true} si la contraseña es correcta, {@code false} en caso
     * contrario o si algún parámetro es nulo.
     */
    public static boolean checkPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isLegacyHash(storedHash)) {
            return checkLegacy(inputPassword, storedHash);
        }
        return checkBcrypt(inputPassword, storedHash);
    }

    /**
     * Indica si el hash guardado en disco es del formato antiguo (SHA-256). Los
     * hashes BCrypt siempre comienzan por {@code $2} (p.ej. {@code $2a$},
     * {@code $2b$}).
     *
     * @param storedHash El hash a examinar.
     * @return {@code true} si es un hash SHA-256 legado.
     */
    public static boolean isLegacyHash(String storedHash) {
        return storedHash != null && !storedHash.startsWith("$2");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos internos
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Verifica una contraseña contra un hash BCrypt.
     *
     * @param password Contraseña en texto plano a verificar.
     * @param bcryptHash Hash BCrypt almacenado.
     * @return {@code true} si la contraseña coincide con el hash, {@code false}
     * si no coincide o si el hash está malformado.
     */
    private static boolean checkBcrypt(String password, String bcryptHash) {
        try {
            return BCrypt.checkpw(password, bcryptHash);
        } catch (IllegalArgumentException e) {
            // Hash malformado
            LOGGER.log(Level.WARNING, "Hash BCrypt malformado al verificar contraseña.", e);
            return false;
        }
    }

    /**
     * Verifica una contraseña contra un hash SHA-256 (formato legado). Solo se
     * usa durante la migración transparente.
     *
     * @param password Contraseña en texto plano a verificar.
     * @param legacyHash Hash SHA-256 almacenado.
     * @return {@code true} si la contraseña coincide con el hash, {@code false}
     * en caso contrario.
     */
    private static boolean checkLegacy(String password, String legacyHash) {
        String inputHash = hashSha256(password);
        return legacyHash.equals(inputHash);
    }

    /**
     * Calcula el hash SHA-256 en Base64, tal como lo hacía la versión anterior.
     * Solo se conserva para la migración de hashes legados.
     *
     * @param password La contraseña en texto plano.
     * @return El hash SHA-256 en Base64, o {@code null} si falla el algoritmo.
     */
    static String hashSha256(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Algoritmo SHA-256 no disponible.", e);
            return null;
        }
    }
}
