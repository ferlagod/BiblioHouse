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

    private int calificacion; // Calificación de 0 a 5 estrellas
    private String reseña;    // Notas o reseña personal

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
     * @param calificacion La puntuación dada por el usuario
     * @param reseña La reseña escrita por el usuario
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn, String portadaURL, int calificacion, String reseña) {
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.año = año;
        this.genero = genero;
        this.isbn = isbn;
        this.portadaURL = portadaURL;
        this.calificacion = calificacion;
        this.reseña = reseña;
    }

    /**
     * Constructor simplificado para crear una instancia de Libro. Este
     * constructor se utiliza cuando no se dispone de todos los datos del libro,
     * como en el caso de la integración con OpenLibrary. Establece valores por
     * defecto para las estrellas y la reseña del libro.
     *
     * @param titulo El título del libro.
     * @param autor El autor del libro.
     * @param editorial La editorial del libro.
     * @param año El año de publicación del libro.
     * @param genero El género del libro.
     * @param isbn El ISBN del libro, que se utiliza como identificador único.
     * @param portadaURL La URL de la portada del libro.
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn, String portadaURL) {
        this(titulo, autor, editorial, año, genero, isbn, portadaURL, 0, ""); // Por defecto, 0 estrellas y sin reseña
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
     * Obtiene la calificación del libro. La calificación es un valor numérico
     * que representa la valoración del libro.
     *
     * @return La calificación del libro.
     */
    public int getCalificacion() {
        return calificacion;
    }

    /**
     * Establece la calificación del libro. La calificación es un valor numérico
     * que representa la valoración del libro.
     *
     * @param calificacion La calificación a establecer para el libro.
     */
    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    /**
     * Obtiene la reseña del libro. La reseña es un texto que describe la
     * opinión o crítica sobre el libro.
     *
     * @return La reseña del libro.
     */
    public String getReseña() {
        return reseña;
    }

    /**
     * Establece la reseña del libro. La reseña es un texto que describe la
     * opinión o crítica sobre el libro.
     *
     * @param reseña La reseña a establecer para el libro.
     */
    public void setReseña(String reseña) {
        this.reseña = reseña;
    }

    /**
     * Devuelve una representación en cadena del libro.
     *
     * @return Una cadena que representa el libro.
     */
    @Override
    public String toString() {
        return this.titulo; // Devuelve solo el título para mostrarlo en el JComboBox
    }
}
