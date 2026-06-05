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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio encargado de gestionar las operaciones relacionadas con los libros,
 * como la fusión de duplicados, la eliminación de libros y la actualización de
 * stock. Todas las operaciones modifican la lista de libros y persisten los
 * cambios en disco.
 *
 * @author Fernando Lago Dávila
 * @version 1.8
 */
public class LibroService {

    private final JsonManager jsonManager;
    private final List<Libro> listaLibrosCompleta;

    /**
     * Crea una nueva instancia de LibroService.
     *
     * @param jsonManager Gestor de persistencia para guardar los cambios.
     * @param listaLibrosCompleta Lista completa de libros de la biblioteca.
     */
    public LibroService(JsonManager jsonManager, List<Libro> listaLibrosCompleta) {
        this.jsonManager = jsonManager;
        this.listaLibrosCompleta = listaLibrosCompleta;
    }

    /**
     * Busca libros duplicados en la biblioteca, fusiona su stock y elimina las
     * copias. Los duplicados se detectan por ISBN (si está disponible) o por
     * título y autor. El stock de los duplicados se suma al libro principal, y
     * los duplicados se eliminan.
     *
     * @return Un informe con los libros fusionados, o null si no se encontraron
     * duplicados.
     */
    public String buscarYFusionarDuplicados() {
        if (listaLibrosCompleta == null || listaLibrosCompleta.isEmpty()) {
            return null;
        }

        // Agrupar libros por ISBN (si existe) o por título+autor
        Map<String, List<Libro>> grupos = listaLibrosCompleta.stream().collect(Collectors.groupingBy(l -> {
            if (l.getIsbn() != null && !l.getIsbn().isBlank()) {
                return l.getIsbn().replaceAll("[^0-9X]", "");
            }
            return (l.getTitulo() + "|" + l.getAutor()).toLowerCase().trim();
        }));

        List<Libro> librosParaBorrar = new ArrayList<>();
        StringBuilder reporte = new StringBuilder("Análisis de duplicados:\n\n");
        int contadorFusionados = 0;

        // Procesar cada grupo de libros
        for (List<Libro> grupo : grupos.values()) {
            if (grupo.size() > 1) {
                Libro principal = grupo.get(0);
                for (int i = 1; i < grupo.size(); i++) {
                    Libro duplicado = grupo.get(i);
                    principal.setCantidad(principal.getCantidad() + duplicado.getCantidad());
                    librosParaBorrar.add(duplicado);
                    contadorFusionados++;
                    reporte.append("✔️ ").append(principal.getTitulo()).append(" (Fusionado)\n");
                }
            }
        }

        if (contadorFusionados == 0) {
            return null;
        }

        // Eliminación eficiente O(n) usando identidad de objeto en vez de removeAll(List) que sería O(n²).
        java.util.IdentityHashMap<Libro, Boolean> aEliminar = new java.util.IdentityHashMap<>();
        for (Libro l : librosParaBorrar) {
            aEliminar.put(l, Boolean.TRUE);
        }
        listaLibrosCompleta.removeIf(aEliminar::containsKey);
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        return reporte.toString();
    }

    /**
     * Elimina permanentemente un libro de la biblioteca y guarda los cambios en
     * disco.
     *
     * @param libro Libro a eliminar.
     */
    public void borrarTotalmente(Libro libro) {
        if (libro != null) {
            listaLibrosCompleta.remove(libro);
            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
        }
    }

    /**
     * Actualiza el stock de un libro, sumando o restando la cantidad
     * especificada. Asegura que el stock no sea negativo y guarda los cambios
     * en disco.
     *
     * @param libro Libro cuyo stock se actualizará.
     * @param variacion Cantidad a sumar (positiva) o restar (negativa).
     */
    public void actualizarStock(Libro libro, int variacion) {
        if (libro != null) {
            int nuevoStock = libro.getCantidad() + variacion;
            libro.setCantidad(Math.max(nuevoStock, 0)); // Evitar stock negativo
            jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
        }
    }
}
