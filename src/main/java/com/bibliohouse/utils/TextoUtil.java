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

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidades de texto para búsquedas tolerantes a mayúsculas y tildes.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.2
 */
public final class TextoUtil {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");

    private TextoUtil() {
    }

    /**
     * Normaliza un texto para comparaciones de búsqueda: minúsculas
     * independientes del idioma del sistema y sin tildes ni diacríticos
     * («García» → «garcia», «Ñandú» → «nandu»).
     *
     * @param texto Texto original (puede ser {@code null}).
     * @return Texto normalizado, o cadena vacía si es {@code null}.
     */
    public static String normalizarBusqueda(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String sinTildes = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Indica si {@code texto} contiene {@code consultaNormalizada}, ignorando
     * mayúsculas y tildes.
     *
     * @param texto Texto donde buscar.
     * @param consultaNormalizada Consulta ya pasada por
     * {@link #normalizarBusqueda(String)}.
     * @return {@code true} si hay coincidencia.
     */
    public static boolean contiene(String texto, String consultaNormalizada) {
        if (consultaNormalizada == null || consultaNormalizada.isEmpty()) {
            return true;
        }
        return texto != null && normalizarBusqueda(texto).contains(consultaNormalizada);
    }
}
