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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Importador de CSV de Bookwyrm y Goodreads.
 *
 * Funciona con CSVs de Goodreads, Bookwyrm y otros formatos genéricos.
 *
 * @author Fernando Lago
 * @version 1.5
 */
public class ImportadorCSV {

    /**
     * Importa un archivo CSV y lo convierte en una lista de objetos Libro.
     * Funciona con CSVs de Goodreads, Bookwyrm y otros formatos genéricos.
     *
     * @param archivo El archivo CSV a importar.
     * @return Lista de Libros importados, o null si hay un error.
     */
    public static List<Libro> importar(File archivo) {
        List<Libro> librosImportados = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea = br.readLine();
            if (linea == null) {
                return librosImportados; // Archivo vacío
            }
            // Detectar si usa comas o puntos y comas
            String separador = linea.contains(";") ? ";" : ",";

            // Expresión regular mágica que separa por comas (o ;) pero IGNORA las que están dentro de comillas
            String regexSplit = separador + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

            // Analizar cabeceras para saber dónde está cada cosa
            String[] cabeceras = linea.split(regexSplit);
            int idxTitulo = -1, idxAutor = -1, idxIsbn = -1, idxIsbn10 = -1, idxAnio = -1, idxEditorial = -1;
            int idxRating = -1, idxShelf = -1;

            for (int i = 0; i < cabeceras.length; i++) {
                String c = cabeceras[i].replace("\"", "").trim().toLowerCase();

                // Mapeo múltiple (Genérico + Goodreads + Bookwyrm)
                switch (c) {
                    case "title":
                    case "título":
                    case "titulo":
                        idxTitulo = i;
                        break;
                    case "author":
                    case "autor":
                    case "author_text":
                        idxAutor = i;
                        break;
                    case "isbn13":
                    case "isbn":
                    case "isbn_13":
                        idxIsbn = i;
                        break;
                    case "isbn10":
                    case "isbn_10":
                        idxIsbn10 = i;
                        break;
                    case "original publication year":
                    case "year published":
                    case "año":
                    case "year":
                        idxAnio = i;
                        break;
                    case "publisher":
                    case "editorial":
                        idxEditorial = i;
                        break;
                    case "my rating":
                    case "rating":
                    case "calificacion":
                        idxRating = i;
                        break;
                    case "exclusive shelf":
                    case "shelf":
                    case "estado":
                        idxShelf = i;
                        break;
                    default:
                        break;
                }
            }

            // Si no encontramos ni título ni autor, el CSV no nos vale
            if (idxTitulo == -1 || idxAutor == -1) {
                throw new Exception("El archivo CSV debe contener al menos columnas de Título y Autor (o title y author_text).");
            }

            // Leer los libros línea por línea
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                String[] campos = linea.split(regexSplit, -1);

                String titulo = limpiar(obtenerCampo(campos, idxTitulo));
                String autor = limpiar(obtenerCampo(campos, idxAutor));

                // Si el título o el autor están en blanco, nos saltamos la línea
                if (titulo.isEmpty() || autor.isEmpty()) {
                    continue;
                }

                // Preferencia al ISBN13, si no está intentamos coger el ISBN10
                String isbn = limpiar(obtenerCampo(campos, idxIsbn)).replace("=\"", "").replace("\"", "");
                if (isbn.isEmpty() && idxIsbn10 != -1) {
                    isbn = limpiar(obtenerCampo(campos, idxIsbn10)).replace("=\"", "").replace("\"", "");
                }

                String anio = limpiar(obtenerCampo(campos, idxAnio)).replace(".0", ""); // Quitar decimales
                String editorial = limpiar(obtenerCampo(campos, idxEditorial));

                Libro nuevoLibro = new Libro(titulo, autor, editorial, anio, "", isbn, null);
                nuevoLibro.setCantidad(1); // Por defecto añadimos 1 en stock
                nuevoLibro.setPoseido(true);

                // --- INTEGRACIÓN ESPECIAL: Bookwyrm / Goodreads ---
                // 1. Estado de Lectura
                if (idxShelf != -1) {
                    String shelf = limpiar(obtenerCampo(campos, idxShelf)).toLowerCase();
                    if (shelf.contains("read") && !shelf.contains("to-read") && !shelf.contains("currently-reading") && !shelf.equals("reading")) {
                        nuevoLibro.setEstadoLectura("Leído");
                    } else if (shelf.contains("to-read") || shelf.contains("pendiente")) {
                        nuevoLibro.setEstadoLectura("Pendiente");
                    } else if (shelf.contains("reading") || shelf.contains("leyendo")) {
                        nuevoLibro.setEstadoLectura("Leyendo");
                    }
                }

                // 2. Calificación (Estrellas)
                if (idxRating != -1) {
                    String ratingStr = limpiar(obtenerCampo(campos, idxRating));
                    if (!ratingStr.isEmpty() && !ratingStr.equals("0") && !ratingStr.equals("0.00")) {
                        try {
                            // Bookwyrm devuelve "5.00", así que lo pasamos a double y luego redondeamos a int
                            double califDouble = Double.parseDouble(ratingStr);
                            int califInt = (int) Math.round(califDouble);
                            if (califInt >= 0 && califInt <= 5) {
                                nuevoLibro.setCalificacion(califInt);
                            }
                        } catch (NumberFormatException e) {
                            // Ignoramos si viene basura en esa columna
                        }
                    }
                }

                librosImportados.add(nuevoLibro);
            }

        } catch (Exception e) {
            System.err.println("Error al importar CSV: " + e.getMessage());
            return null; // Devolvemos null para que el controlador sepa que hubo un error
        }

        return librosImportados;
    }

    /**
     * Devuelve el valor de un campo del array, evitando errores si el índice no
     * existe.
     *
     * @param campos El array de donde extraer el campo.
     * @param index La posición del campo que se quiere obtener.
     * @return El valor del campo, o una cadena vacía si el índice no es válido.
     */
    private static String obtenerCampo(String[] campos, int index) {
        if (index >= 0 && index < campos.length) {
            return campos[index];
        }
        return "";
    }

    /**
     * Limpia un texto quitando espacios sobrantes y comillas dobles al inicio y
     * final.
     *
     * @param texto El texto a limpiar.
     * @return El texto limpio, o cadena vacía si el texto es null.
     */
    private static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        texto = texto.trim();
        // Quitar comillas del principio y final que ponen los CSV
        if (texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1);
        }
        return texto.trim();
    }
}
