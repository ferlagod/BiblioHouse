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

import com.bibliohouse.logic.Libro;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @autor Fernando Lago
 * @version 1.6
 */
public class ProcesadorSagas {

    /**
     * Normaliza el nombre de la saga: quita tildes, espacios extra y pasa a
     * minúsculas. Evita que "Harry Potter" y "harry potter" se separen.
     */
    public static String normalizar(String saga) {
        if (saga == null || saga.trim().isEmpty()) {
            return "Sin Saga";
        }
        String limpia = Normalizer.normalize(saga, Normalizer.Form.NFD);
        limpia = limpia.replaceAll("\\p{M}", ""); // Elimina marcas diacríticas (tildes)
        return limpia.toLowerCase().trim();
    }

    /**
     * Revisa el título. Si detecta el formato "Titulo (Saga, #1)", recorta la
     * cadena, asigna la serie, el orden y deja el título limpio.
     */
    public static void extraerSagaDeTitulo(Libro libro) {
        if (libro.getTitulo() == null) {
            return;
        }

        // Regex para cazar: CualquierCosa (CualquierCosa #Numero) o (CualquierCosa, #Numero)
        Pattern patron = Pattern.compile("^(.*?)\\s*\\((.*?)[,\\s]*#(\\d+(?:\\.\\d+)?)\\)$");
        Matcher matcher = patron.matcher(libro.getTitulo());

        if (matcher.find()) {
            libro.setTitulo(matcher.group(1).trim());
            libro.setSerie(matcher.group(2).trim());
            libro.setOrdenEnSerie(Double.parseDouble(matcher.group(3)));
        }
    }
}
