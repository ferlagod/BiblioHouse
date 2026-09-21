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

import java.text.Normalizer;

/**
 * Enumeración que define los estados de lectura posibles de un libro
 * en el dominio de BiblioHouse.
 *
 * Desacopla la lógica interna y de persistencia de las cadenas traducidas
 * mostradas en la interfaz de usuario.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public enum EstadoLectura {

    PENDIENTE("export.status.pending", "Pendiente"),
    LEYENDO("export.status.reading", "Leyendo"),
    LEIDO("export.status.read", "Leído"),
    ABANDONADO("export.status.abandoned", "Abandonado");

    private final String i18nKey;
    private final String etiquetaEspanol;

    EstadoLectura(String i18nKey, String etiquetaEspanol) {
        this.i18nKey = i18nKey;
        this.etiquetaEspanol = etiquetaEspanol;
    }

    /**
     * Obtiene la etiqueta traducida para mostrar en la interfaz de usuario
     * según el idioma activo en {@link LanguageManager}.
     *
     * @return El texto traducido correspondiente al estado.
     */
    public String getEtiqueta() {
        return LanguageManager.getString(i18nKey, etiquetaEspanol);
    }

    /**
     * Devuelve el nombre canónico en español (útil para serialización y compatibilidad).
     *
     * @return El nombre en español ("Pendiente", "Leyendo", "Leído", "Abandonado").
     */
    public String getEtiquetaEspanol() {
        return etiquetaEspanol;
    }

    /**
     * Parsea de forma segura y tolerante cualquier texto o representación previa
     * en cualquier idioma soportado (español, inglés, gallego, catalán, euskera, portugués),
     * ignorando mayúsculas, minúsculas y tildes.
     *
     * @param texto Cadena a analizar.
     * @return El {@link EstadoLectura} correspondiente (por defecto {@link #PENDIENTE}).
     */
    public static EstadoLectura fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            return PENDIENTE;
        }

        String trimmed = texto.trim();

        // 1. Coincidencia exacta con nombre de constante enum
        try {
            return EstadoLectura.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // 2. Normalización eliminando acentos y convirtiendo a minúsculas
        String norm = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();

        // 3. Mapeo multilingüe tolerante
        return switch (norm) {
            case "leido", "read", "llegit", "irakurrita", "lido" -> LEIDO;
            case "leyendo", "reading", "llegint", "irakurtzen", "lendo" -> LEYENDO;
            case "abandonado", "abandoned", "abandonat", "utzia" -> ABANDONADO;
            default -> PENDIENTE; // "pendiente", "pending", "pendent", "zain", etc.
        };
    }

    @Override
    public String toString() {
        return getEtiqueta();
    }
}
