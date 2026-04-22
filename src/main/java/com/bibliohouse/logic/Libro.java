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

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Esta clase es como una ficha para cada libro. Aquí guardamos el título, autor
 * y todo eso.
 *
 * @author Fernando Lago
 * @version 1.6
 */
public class Libro {

    // Atributos de la clase Libro
    private String id;
    private String titulo;
    private String autor;
    private String editorial;
    private String año;
    private String genero;
    private String isbn;
    private String portadaURL;
    private int calificacion;
    private String reseña;
    private List<String> estanterias;
    private int cantidad;
    private boolean leido;
    private String estadoLectura = "Pendiente";
    // Fecha en que se marca como "Leído" (para la tasa)
    private LocalDate fechaFinalizacion;
    // Si es falso, es un libro deseado (wishlist)
    private boolean poseido = true;

    // CAMPOS PARA SERIES/SAGAS
    private String serie;
    // double para permitir 1.5, 0.5, etc.
    private double ordenEnSerie;

    /**
     * Constructor vacío.
     */
    public Libro() {
        // Si no tiene ID, le inventamos uno aleatorio
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * Constructor para crear el libro con todos los datos de golpe.
     *
     * @param titulo titulo del libro
     * @param autor autor del libro
     * @param editorial editorial al que pertenece el libro
     * @param año año de publicación del libro
     * @param genero género del libro
     * @param isbn isbn del libro
     * @param portadaURL portada del libro
     * @param calificacion calificación dada al libro
     * @param reseña reseña escrita por el usuario
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn,
            String portadaURL, int calificacion, String reseña) {
        // Generación del ID único, asegurando que siempre tenga un valor
        this.id = UUID.randomUUID().toString();
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.año = año;
        this.genero = genero;
        this.isbn = isbn;
        this.portadaURL = portadaURL;
        this.calificacion = calificacion;
        this.reseña = reseña;

        // Valores por defecto
        this.estanterias = new ArrayList<>();
        this.cantidad = 1;
        this.leido = false;
        this.poseido = true; // Por defecto es poseído
    }

    /**
     * Constructor más corto. Se utiliza cuando creamos el libro pero aún no lo
     * hemos puntuado ni reseñado. Pone las estrellas a 0 por defecto.
     *
     * @param titulo Nombre del libro.
     * @param autor Nombre del escritor.
     * @param editorial Nombre de la editorial.
     * @param año Año de publicación.
     * @param genero Género literario.
     * @param isbn Código ISBN.
     * @param portadaURL Link a la imagen.
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn,
            String portadaURL) {
        // Por defecto, 0 estrellas y sin reseña
        this(titulo, autor, editorial, año, genero, isbn, portadaURL, 0, "");
        this.estanterias = new ArrayList<>();
    }

    // --- Métodos Getter y Setter ---
    /**
     * Obtiene el ID único interno del libro.
     *
     * @return El UUID como cadena de texto.
     */
    public String getId() {
        return id;
    }

    /**
     * Establece el ID único interno del libro. NOTA: Este setter solo debe ser
     * usado por GSON al cargar datos, no debe cambiarse manualmente.
     *
     * @param id El ID a establecer.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Devuelve el título del libro.
     *
     * @return El título.
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
     * Obtiene la calificación del libro.
     *
     * @return La calificación del libro.
     */
    public int getCalificacion() {
        return calificacion;
    }

    /**
     * Establece la calificación del libro.
     *
     * @param calificacion La calificación a establecer para el libro.
     */
    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    /**
     * Obtiene la reseña del libro.
     *
     * @return La reseña del libro.
     */
    public String getReseña() {
        return reseña;
    }

    /**
     * Establece la reseña del libro.
     *
     * @param reseña La reseña a establecer para el libro.
     */
    public void setReseña(String reseña) {
        this.reseña = reseña;
    }

    /**
     * Obtiene la estanteria donde se encuentra el libro.
     *
     * @return La estanteria donde se encuentra el libro.
     */
    public List<String> getEstanterias() {
        return estanterias;
    }

    /**
     * Establece la estanterias disponibles.
     *
     * @param estanterias Las estanterías disponibles para los libros.
     */
    public void setEstanterias(List<String> estanterias) {
        this.estanterias = estanterias;
    }

    /**
     * Obtiene la cantidad de unidades del libro.
     *
     * @return La caantidad de unidades de un libro.
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Establece la cantidad de libros disponibles.
     *
     * @param cantidad La cantidad disponibles de los libros.
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Comprueba si el libro ha sido marcado como leído.
     *
     * @return true si está marcado como leído, false en caso contrario.
     */
    public boolean isLeido() {
        return leido;
    }

    /**
     * Marca el libro como leído o no leído. También imprime un mensaje por
     * consola para comprobar que funciona (DEBUG).
     *
     * @param leido true para marcar como leído.
     */
    public void setLeido(boolean leido) {
        this.leido = leido;
        // Sincronizar con el estado de lectura (Backward compatibility)
        if (leido) {
            this.estadoLectura = "Leído";
        } else if ("Leído".equals(this.estadoLectura)) {
            // Si estaba marcado como leído y ahora se desmarca, volver a pendiente
            this.estadoLectura = "Pendiente";
        }
    }

    public String getEstadoLectura() {
        return estadoLectura;
    }

    public void setEstadoLectura(String estadoLectura) {
        this.estadoLectura = estadoLectura;
        // Sincronizar con el booleano leido
        this.leido = "Leído".equals(estadoLectura);
    }

    public LocalDate getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDate fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    /**
     * Comprueba si el libro es propiedad del usuario o si es un deseo.
     *
     * @return true si el libro es poseído, false si es un deseo (wishlist).
     */
    public boolean isPoseido() {
        return poseido;
    }

    /**
     * Establece si el libro es poseído o un deseo.
     *
     * @param poseido true para marcar como poseído.
     */
    public void setPoseido(boolean poseido) {
        this.poseido = poseido;
    }

    /**
     * Convierte el objeto Libro a texto. Solo devuelve el título porque es lo
     * que se ve en el JComboBox de la interfaz.
     *
     * @return El título del libro.
     */
    public String getSerie() {
        return serie;
    }

    /**
     * Establece la serie a la que pertenece el libro.
     *
     * @param serie Nombre de la saga.
     */
    public void setSerie(String serie) {
        this.serie = serie;
    }

    /**
     * Obtiene el orden de lectura dentro de la serie.
     *
     * @return Número de orden (ej: 1.0, 2.5).
     */
    public double getOrdenEnSerie() {
        return ordenEnSerie;
    }

    /**
     * Establece el orden dentro de la serie.
     *
     * @param ordenEnSerie Número decimal de orden.
     */
    public void setOrdenEnSerie(double ordenEnSerie) {
        this.ordenEnSerie = ordenEnSerie;
    }

    /**
     * Devuelve una representación en cadena de este objeto, mostrando
     * únicamente el título. Este método está sobrescrito para adaptar la
     * visualización en componentes como JComboBox, donde solo se requiere
     * mostrar el título del objeto en lugar de la representación completa.
     *
     * @return el título del objeto como cadena de texto.
     */
    @Override
    public String toString() {
        // Devuelve solo el título para mostrarlo en el JComboBox
        return this.titulo;
    }

    /**
     * Obtiene el nombre del archivo de la portada a partir de su URL o ruta
     * local.
     *
     * @return El nombre del archivo (ej: "portada.jpg") o null si la URL está
     * vacía o es nula.
     */
    public String getNombreArchivoPortada() {
        if (portadaURL == null || portadaURL.isEmpty()) {
            return null;
        }
        File f = new File(portadaURL);
        return f.getName();
    }
}
