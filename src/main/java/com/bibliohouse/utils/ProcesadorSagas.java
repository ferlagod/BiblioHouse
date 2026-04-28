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
    /**
     * Limpia el nombre de una saga para evitar que "Saga X Vol. 1" y "Saga X
     * Vol. 2" se consideren dos sagas distintas.
     */
    public static String normalizar(String serieOriginal) {
        if (serieOriginal == null || serieOriginal.isBlank()) {
            return "";
        }

        String limpia = serieOriginal.trim();

        // 1. Eliminar indicadores de volumen seguidos de números al final de la cadena
        // Ej: "El Archivo de las Tormentas, Libro 1" -> "El Archivo de las Tormentas"
        limpia = limpia.replaceAll("(?i)[,\\s-]*\\b(vol\\.?|volumen|tomo|libro|book|parte|part)\\s*\\d+.*$", "");

        // 2. Eliminar el símbolo '#' seguido de números al final
        // Ej: "Nacidos de la Bruma #3" -> "Nacidos de la Bruma"
        limpia = limpia.replaceAll("(?i)[,\\s-]*#\\s*\\d+.*$", "");

        // 3. Eliminar números romanos al final si están precedidos por un espacio
        // Ej: "Fundación III" -> "Fundación" (Cuidado de no romper palabras como "Carlos III")
        limpia = limpia.replaceAll("(?i)\\s+(I{1,3}|IV|V|VI{1,3}|IX|X|XI{1,3})$", "");

        // 4. Eliminar la palabra "La serie de", "Saga" o "The series" para normalizar
        // Ej: "La saga de Harry Potter" -> "Harry Potter"
        limpia = limpia.replaceAll("(?i)^(la\\s+)?(saga|serie|series)\\s+(de|of)?\\s*", "");

        // 5. Limpiar espacios extra dobles que hayan podido quedar
        limpia = limpia.replaceAll("\\s{2,}", " ").trim();

        // 6. Convertir la primera letra a mayúscula para homogeneizar visualmente
        if (!limpia.isEmpty()) {
            limpia = limpia.substring(0, 1).toUpperCase() + limpia.substring(1);
        }

        return limpia;
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
        Pattern patron = Pattern.compile("^(.*?)\\s*\\((.*?)[,\\s]*(?:#|vol\\.?|libro|book|tomo)?\\s*(\\d+(?:\\.\\d+)?)\\)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patron.matcher(libro.getTitulo());

        if (matcher.find()) {
            libro.setTitulo(matcher.group(1).trim());
            libro.setSerie(matcher.group(2).trim());
            libro.setOrdenEnSerie(Double.parseDouble(matcher.group(3)));
        }
    }
}
