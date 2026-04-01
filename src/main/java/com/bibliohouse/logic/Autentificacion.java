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

/**
 * Esta clase tiene métodos para manejar contraseñas de forma segura.
 *
 * @author Fernando Lago
 * @version 1.5
 */
public class Autentificacion {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Este método convierte una contraseña normal en un hash para que no se vea
     * la contraseña real.
     *
     * @param password La contraseña que escribe el usuario.
     * @return Un código largo en Base64 que representa la contraseña, o null si
     * algo sale mal.
     */
    public static String hashPassword(String password) {
        // Compruebo que la contraseña no esté vacía o sea null
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            // Convierto la contraseña a bytes y luego se hashea, paraluego guardarlo como
            // texto en la base de datos
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            // Si no encuentra el algoritmo, se imprime el error y devuelve null
            System.err.println("Error grave: Algoritmo de hash no encontrado: " + HASH_ALGORITHM);
            return null;
        }
    }

    /**
     * Comprueba si la contraseña que escribe el usuario coincide con el hash
     * guardado.
     *
     * @param inputPassword La contraseña que escribe el usuario al iniciar
     * sesión.
     * @param hashGuardado El hash que tenemos guardado en la base de datos.
     * @return true si coinciden, false si no coinciden o si algo va mal.
     */
    public static boolean checkPassword(String inputPassword, String hashGuardado) {
        // Se comprueba que ni la contraseña ni el hash estén vacíos
        if (inputPassword == null || hashGuardado == null || hashGuardado.isEmpty()) {
            return false;
        }
        // Calcula el hash de la contraseña introducida
        String inputHash = hashPassword(inputPassword);

        // Compara los hashes (ambos deben estar codificados en Base64)
        return hashGuardado.equals(inputHash);
    }
}
