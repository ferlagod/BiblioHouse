/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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

/**
 * Esta clase representa el modelo de datos para un Libro. Contiene todos los
 * atributos que definen a un libro en nuestra biblioteca.
 *
 * @author ferlagod
 * @version 0.2
 */
public class Libro {

    // Atributos de la clase Libro
    private String titulo;
    private String autor;
    private String editorial;
    private String año; // Usamos String para simplificar
    private String genero;
    private String isbn;
    private String portadaURL; // URL a la imagen de la portada

    /**
     * Constructor vacío para la clase Libro.
     */
    public Libro() {
    }

    /**
     * Constructor para crear un objeto Libro con todos sus atributos.
     *
     * @param titulo El título del libro.
     * @param autor El autor del libro.
     * @param editorial La editorial del libro.
     * @param año El año de publicación del libro.
     * @param genero El género del libro.
     * @param isbn El ISBN del libro.
     * @param portadaURL La URL de la imagen de la portada del libro.
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn, String portadaURL) {
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.año = año;
        this.genero = genero;
        this.isbn = isbn;
        this.portadaURL = portadaURL;
    }

    // --- Métodos Getter y Setter ---
    /**
     * Obtiene el título del libro.
     *
     * @return El título del libro.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Establece el título del libro.
     *
     * @param titulo El nuevo título del libro.
     */
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    /**
     * Obtiene el autor del libro.
     *
     * @return El autor del libro.
     */
    public String getAutor() {
        return autor;
    }

    /**
     * Establece el autor del libro.
     *
     * @param autor El nuevo autor del libro.
     */
    public void setAutor(String autor) {
        this.autor = autor;
    }

    /**
     * Obtiene la editorial del libro.
     *
     * @return La editorial del libro.
     */
    public String getEditorial() {
        return editorial;
    }

    /**
     * Establece la editorial del libro.
     *
     * @param editorial La nueva editorial del libro.
     */
    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    /**
     * Obtiene el año de publicación del libro.
     *
     * @return El año de publicación del libro.
     */
    public String getAño() {
        return año;
    }

    /**
     * Establece el año de publicación del libro.
     *
     * @param año El nuevo año de publicación del libro.
     */
    public void setAño(String año) {
        this.año = año;
    }

    /**
     * Obtiene el género del libro.
     *
     * @return El género del libro.
     */
    public String getGenero() {
        return genero;
    }

    /**
     * Establece el género del libro.
     *
     * @param genero El nuevo género del libro.
     */
    public void setGenero(String genero) {
        this.genero = genero;
    }

    /**
     * Obtiene el ISBN del libro.
     *
     * @return El ISBN del libro.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Establece el ISBN del libro.
     *
     * @param isbn El nuevo ISBN del libro.
     */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Obtiene la URL de la portada del libro.
     *
     * @return La URL de la portada del libro.
     */
    public String getPortadaURL() {
        return portadaURL;
    }

    /**
     * Establece la URL de la portada del libro.
     *
     * @param portadaURL La nueva URL de la portada del libro.
     */
    public void setPortadaURL(String portadaURL) {
        this.portadaURL = portadaURL;
    }

    /**
     * Devuelve una representación en cadena del libro.
     *
     * @return Una cadena que representa el libro.
     */
    @Override
    public String toString() {
        return "Libro{"
                + "titulo='" + titulo + '\''
                + ", autor='" + autor + '\''
                + ", año='" + año + '\''
                + '}';
    }
}
