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

import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Esta clase es como una ficha para cada libro. Aquí guardamos el título, autor
 * y todo eso.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class Libro {

    // Atributos de la clase Libro
    private String id;
    private String titulo;
    private String autor;
    private String editorial;
    @SerializedName("año")
    private String anio;
    private String genero;
    private String isbn;
    private String portadaURL;
    private int calificacion;
    @SerializedName("reseña")
    private String resena;
    private List<String> estanterias;
    private int cantidad;
    /**
     * @deprecated Usar {@link #estadoLectura} directamente. Este campo
     * permanece para compatibilidad al leer JSONs antiguos.
     */
    @Deprecated
    private boolean leido;
    private String estadoLectura = "Pendiente";
    // Fecha en que se marca como "Leído" (para la tasa)
    private LocalDate fechaFinalizacion;
    // Si es falso, es un libro deseado (wishlist)
    private boolean poseido = true;

    // CAMPOS PARA UBICACIÓN FÍSICA
    private String ubicacionFisica;

    // CAMPOS PARA SERIES/SAGAS
    private String serie;
    // double para permitir 1.5, 0.5, etc.
    private double ordenEnSerie;

    // CAMPOS PARA E-BOOKS / ARCHIVOS DIGITALES
    /**
     * Ruta absoluta al archivo digital enlazado (EPUB, PDF, MOBI). Null si no
     * tiene.
     */
    private String rutaArchivoDigital;
    /**
     * True si el libro se considera "digital" (tiene archivo enlazado).
     */
    private boolean esDigital = false;

    // CAMPOS PARA READING TRACKER Y DIARIO
    private int paginaActual = 0;
    private int paginasTotales = 0;
    private List<NotaLectura> diario = new ArrayList<>();

    /**
     * Constructor vacío. Nos sirve para crear un libro sin datos y que el 
     * programa no se enfade al cargar cosas. Ya le pone un ID aleatorio.
     */
    public Libro() {
        // Si no tiene ID, le inventamos uno aleatorio
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * Constructor para crear el libro con todos los datos de golpe, ideal 
     * cuando ya sabemos todo sobre él.
     *
     * @param titulo El nombre del libro.
     * @param autor Quien lo ha escrito.
     * @param editorial La empresa que lo publica.
     * @param año El año en el que salió.
     * @param genero De qué va (ciencia ficción, novela...).
     * @param isbn El código de barras o identificador internacional.
     * @param portadaURL La ruta donde guardamos la imagen de la portada.
     * @param calificacion Las estrellitas que le damos.
     * @param reseña Lo que pensamos nosotros del libro.
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn,
            String portadaURL, int calificacion, String reseña) {
        // Generación del ID único, asegurando que siempre tenga un valor
        this.id = UUID.randomUUID().toString();
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.anio = año;
        this.genero = genero;
        this.isbn = isbn;
        this.portadaURL = portadaURL;
        this.calificacion = calificacion;
        this.resena = reseña;

        // Valores por defecto
        this.estanterias = new ArrayList<>();
        this.cantidad = 1;
        this.leido = false;
        this.poseido = true; // Por defecto es poseído
    }

    /**
     * Constructor más cortito. Lo usamos cuando guardamos el libro pero 
     * todavía no lo hemos leído ni puntuado. Nos pone las estrellas a 0.
     *
     * @param titulo El nombre del libro.
     * @param autor Quien lo ha escrito.
     * @param editorial La empresa que lo publica.
     * @param año Año en el que salió.
     * @param genero De qué va (ciencia ficción, novela...).
     * @param isbn El código de barras.
     * @param portadaURL Dónde está la imagen de la portada.
     */
    public Libro(String titulo, String autor, String editorial, String año, String genero, String isbn,
            String portadaURL) {
        // Por defecto, 0 estrellas y sin reseña
        this(titulo, autor, editorial, año, genero, isbn, portadaURL, 0, "");
        this.estanterias = new ArrayList<>();
    }

    // --- Métodos Getter y Setter ---
    /**
     * Nos devuelve el ID único interno del libro. Ese código largo y raro.
     *
     * @return El UUID (ID) en texto.
     */
    public String getId() {
        return id;
    }

    /**
     * Sirve para cambiar el ID del libro. NOTA: ¡Cuidado! Normalmente 
     * solo lo usa GSON al cargar, no lo toques a mano a menos que sepas qué haces.
     *
     * @param id El nuevo ID a guardar.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Nos chiva el título del libro.
     *
     * @return El nombre del libro.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Nos permite cambiar el título del libro por si nos equivocamos.
     *
     * @param titulo El nuevo título.
     */
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    /**
     * Nos devuelve el nombre de la persona que escribió el libro.
     *
     * @return El autor del libro.
     */
    public String getAutor() {
        return autor;
    }

    /**
     * Nos permite guardar o corregir el nombre de quien lo ha escrito.
     *
     * @param autor El nuevo autor del libro.
     */
    public void setAutor(String autor) {
        this.autor = autor;
    }

    /**
     * Nos da la empresa o editorial que lo publicó.
     *
     * @return La editorial del libro.
     */
    public String getEditorial() {
        return editorial;
    }

    /**
     * Nos deja cambiar el nombre de la empresa o editorial.
     *
     * @param editorial La nueva editorial del libro.
     */
    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    /**
     * Nos chiva en qué año se publicó el libro.
     *
     * @return El año de publicación.
     */
    public String getAño() {
        return anio;
    }

    /**
     * Nos sirve para actualizar el año en el que salió el libro.
     *
     * @param año El nuevo año.
     */
    public void setAño(String año) {
        this.anio = año;
    }

    /**
     * Nos devuelve de qué género es el libro (ej. Terror, Novela histórica).
     *
     * @return El género del libro.
     */
    public String getGenero() {
        return genero;
    }

    /**
     * Sirve para guardar el género literario.
     *
     * @param genero El nuevo género del libro.
     */
    public void setGenero(String genero) {
        this.genero = genero;
    }

    /**
     * Nos da el código de barras o ISBN del libro. ¡Es como su DNI!
     *
     * @return El ISBN del libro.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Nos deja cambiar el ISBN si resulta que lo habíamos metido mal.
     *
     * @param isbn El nuevo ISBN.
     */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Nos devuelve la ruta (URL o archivo) donde está guardada la foto de portada.
     *
     * @return La URL o ruta de la portada.
     */
    public String getPortadaURL() {
        return portadaURL;
    }

    /**
     * Sirve para guardar dónde está la foto de la portada.
     *
     * @param portadaURL La nueva URL o ruta de la portada.
     */
    public void setPortadaURL(String portadaURL) {
        this.portadaURL = portadaURL;
    }

    /**
     * Nos dice cuántas estrellitas le hemos dado al libro.
     *
     * @return La nota que le hemos puesto (calificación).
     */
    public int getCalificacion() {
        return calificacion;
    }

    /**
     * Nos permite puntuar el libro.
     *
     * @param calificacion Las estrellitas (ej. 1 al 5).
     */
    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    /**
     * Nos devuelve todo el texto de la reseña que hayamos escrito sobre el libro.
     *
     * @return Lo que pensamos del libro.
     */
    public String getReseña() {
        return resena;
    }

    /**
     * Sirve para que podamos escribir nuestra propia opinión o reseña.
     *
     * @param reseña Todo lo que queramos decir sobre él.
     */
    public void setReseña(String reseña) {
        this.resena = reseña;
    }

    /**
     * Nos dice en qué estanterías virtuales hemos metido este libro.
     * Un libro puede estar en varias.
     *
     * @return Una lista con las estanterías.
     */
    public List<String> getEstanterias() {
        return estanterias;
    }

    /**
     * Nos permite decirle al libro en qué estanterías debe guardarse.
     *
     * @param estanterias Las nuevas estanterías.
     */
    public void setEstanterias(List<String> estanterias) {
        this.estanterias = estanterias;
    }

    /**
     * Nos chiva cuántas copias iguales de este libro tenemos.
     *
     * @return El número de copias.
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Nos deja guardar si tenemos más de una copia de este mismo libro.
     *
     * @param cantidad Las copias que tenemos.
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Nos dice con un 'verdadero' o 'falso' si ya hemos leído este libro.
     * En realidad, mira si el "estadoLectura" es igual a "Leído".
     *
     * @return true si ya nos lo hemos acabado.
     */
    public boolean isLeido() {
        return "Leído".equals(this.estadoLectura);
    }

    /**
     * Nos permite marcar a mano si ya nos hemos leído el libro o no.
     * Si lo marcamos como leído, el programa lo sincroniza para que cuadre todo.
     *
     * @param leido true si ya lo hemos terminado.
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

    /**
     * Nos dice en qué estado se encuentra nuestra lectura como enum fuertemente tipado.
     *
     * @return El {@link EstadoLectura} del libro.
     */
    public EstadoLectura getEstadoLecturaEnum() {
        return EstadoLectura.fromString(estadoLectura);
    }

    /**
     * Asigna el estado de lectura utilizando el enum fuertemente tipado.
     * Sincroniza la etiqueta canónica en español y la bandera booleana leido.
     *
     * @param estado El nuevo {@link EstadoLectura}.
     */
    public void setEstadoLecturaEnum(EstadoLectura estado) {
        EstadoLectura resolved = (estado != null) ? estado : EstadoLectura.PENDIENTE;
        this.estadoLectura = resolved.getEtiquetaEspanol();
        this.leido = (resolved == EstadoLectura.LEIDO);
    }

    /**
     * Nos dice en qué estado se encuentra nuestra lectura (ej: 'Leído', 'Leyendo', 'Pendiente').
     *
     * @return El estado de lectura canónico en español.
     */
    public String getEstadoLectura() {
        if (estadoLectura == null || estadoLectura.isBlank()) {
            return EstadoLectura.PENDIENTE.getEtiquetaEspanol();
        }
        return getEstadoLecturaEnum().getEtiquetaEspanol();
    }

    /**
     * Sirve para decirle al programa cómo va nuestra lectura y actualizar el estado.
     * Parsea de forma tolerante cadenas en cualquier idioma soportado y las normaliza.
     *
     * @param estadoLectura El nuevo estado (ej: 'Leído', 'Read', 'Leyendo', 'Reading').
     */
    public void setEstadoLectura(String estadoLectura) {
        setEstadoLecturaEnum(EstadoLectura.fromString(estadoLectura));
    }

    /**
     * Nos da el día exacto en el que por fin nos terminamos el libro.
     *
     * @return La fecha en la que lo acabamos.
     */
    public LocalDate getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    /**
     * Nos sirve para apuntar qué día nos hemos acabado el libro.
     *
     * @param fechaFinalizacion La fecha final.
     */
    public void setFechaFinalizacion(LocalDate fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    /**
     * Nos chiva si este libro lo tenemos de verdad o si solo está en la lista de deseos.
     *
     * @return true si lo tenemos, false si es solo un deseo.
     */
    public boolean isPoseido() {
        return poseido;
    }

    /**
     * Nos permite marcar que ya hemos comprado el libro, o pasarlo a lista de deseos.
     *
     * @param poseido true si ya lo tenemos nosotros.
     */
    public void setPoseido(boolean poseido) {
        this.poseido = poseido;
    }

    /**
     * Nos dice el nombre de la saga o serie de libros a la que pertenece este.
     *
     * @return El nombre de la saga o null si es un libro suelto.
     */
    public String getSerie() {
        return serie;
    }

    /**
     * Sirve para meter el libro dentro de una saga concreta.
     *
     * @param serie El nombre de la serie o saga.
     */
    public void setSerie(String serie) {
        this.serie = serie;
    }

    /**
     * Nos dice qué número de libro es dentro de su saga (por si es el 1, el 2 o el 2.5).
     *
     * @return El número en la serie.
     */
    public double getOrdenEnSerie() {
        return ordenEnSerie;
    }

    /**
     * Nos deja guardar qué número de orden le toca dentro de la saga.
     *
     * @param ordenEnSerie El número decimal para el orden.
     */
    public void setOrdenEnSerie(double ordenEnSerie) {
        this.ordenEnSerie = ordenEnSerie;
    }

    // --- E-BOOK / ARCHIVO DIGITAL ---
    /**
     * Nos devuelve la ruta en nuestro ordenador de dónde está guardado el archivo digital.
     *
     * @return La ruta del PDF, EPUB, etc., o null si no hay archivo.
     */
    public String getRutaArchivoDigital() {
        return rutaArchivoDigital;
    }

    /**
     * Sirve para enlazar el libro con su archivo de ordenador (EPUB, PDF...).
     *
     * @param rutaArchivoDigital La ruta donde está el archivo guardado.
     */
    public void setRutaArchivoDigital(String rutaArchivoDigital) {
        this.rutaArchivoDigital = rutaArchivoDigital;
    }

    /**
     * Nos chiva si el libro es un e-book (tiene archivo) o es de papel normal.
     *
     * @return true si es digital.
     */
    public boolean isEsDigital() {
        return esDigital;
    }

    /**
     * Nos permite marcar que este libro lo tenemos en formato digital.
     *
     * @param esDigital true si es un e-book.
     */
    public void setEsDigital(boolean esDigital) {
        this.esDigital = esDigital;
    }

    /**
     * Nos dice por qué página vamos leyendo ahora mismo.
     *
     * @return La página en la que estamos.
     */
    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Sirve para apuntar o guardar por qué página del libro vamos.
     *
     * @param paginaActual El número de página actual.
     */
    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    /**
     * Nos dice cuántas páginas tiene en total el libro para calcular cuánto falta.
     *
     * @return El número total de páginas.
     */
    public int getPaginasTotales() {
        return paginasTotales;
    }

    /**
     * Nos permite guardar cuántas páginas en total tiene el libro.
     *
     * @param paginasTotales Las páginas completas del libro.
     */
    public void setPaginasTotales(int paginasTotales) {
        this.paginasTotales = paginasTotales;
    }

    /**
     * Nos devuelve todas las notas o citas que hayamos ido guardando mientras leíamos.
     *
     * @return Una lista con nuestras notas del diario.
     */
    public List<NotaLectura> getDiario() {
        if (diario == null) {
            diario = new ArrayList<>();
        }
        return diario;
    }

    /**
     * Sirve para cambiar o guardar la lista entera de notas y citas del libro.
     *
     * @param diario La nueva lista de notas.
     */
    public void setDiario(List<NotaLectura> diario) {
        this.diario = diario;
    }

    /**
     * Nos da solo el nombre suelto del archivo digital (ej: "harrypotter.epub"),
     * quitándole toda la ruta larga e incómoda del disco duro.
     *
     * @return El nombre del archivo cortito o null si no hay.
     */
    public String getNombreArchivoDigital() {
        if (rutaArchivoDigital == null || rutaArchivoDigital.isEmpty()) {
            return null;
        }
        return new File(rutaArchivoDigital).getName();
    }

    /**
     * Nos dice qué tipo de archivo es el libro (por ejemplo EPUB, PDF) pero 
     * puesto en letras mayúsculas para que quede bonito.
     *
     * @return El formato del archivo o null si no existe.
     */
    public String getFormatoDigital() {
        String nombre = getNombreArchivoDigital();
        if (nombre == null) {
            return null;
        }
        int punto = nombre.lastIndexOf('.');
        if (punto >= 0 && punto < nombre.length() - 1) {
            return nombre.substring(punto + 1).toUpperCase();
        }
        return null;
    }

    /**
     * Nos devuelve un texto súper simple para mostrar el libro en la pantalla
     * (por ejemplo, en los desplegables). Como no queremos que salga algo feo,
     * le decimos que devuelva solo el título del libro.
     *
     * @return El título del libro.
     */
    @Override
    public String toString() {
        // Devuelve solo el título para mostrarlo en el JComboBox
        return this.titulo;
    }

    /**
     * Nos saca solo el nombre del archivo de la portada que hemos puesto, 
     * quitándole la ruta larga que tiene delante.
     *
     * @return El nombre del archivo de imagen (ej: "foto.jpg").
     */
    public String getNombreArchivoPortada() {
        if (portadaURL == null || portadaURL.isEmpty()) {
            return null;
        }
        File f = new File(portadaURL);
        return f.getName();
    }

    /**
     * Nos dice dónde está guardado exactamente este libro en papel (ej: "Salón, Estante 3").
     *
     * @return La ubicación física o null.
     */
    public String getUbicacionFisica() {
        return ubicacionFisica;
    }

    /**
     * Sirve para guardar la posición exacta física de nuestro libro impreso.
     *
     * @param ubicacionFisica El lugar de la casa o caja.
     */
    public void setUbicacionFisica(String ubicacionFisica) {
        this.ubicacionFisica = ubicacionFisica;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Libro libro = (Libro) o;
        if (id != null && !id.isBlank() && libro.id != null && !libro.id.isBlank()) {
            return id.equals(libro.id);
        }
        if (isbn != null && !isbn.isBlank() && libro.isbn != null && !libro.isbn.isBlank()) {
            return isbn.trim().equalsIgnoreCase(libro.isbn.trim());
        }
        return Objects.equals(titulo, libro.titulo) && Objects.equals(autor, libro.autor);
    }

    @Override
    public int hashCode() {
        if (id != null && !id.isBlank()) {
            return id.hashCode();
        }
        if (isbn != null && !isbn.isBlank()) {
            return isbn.trim().toLowerCase().hashCode();
        }
        return Objects.hash(titulo, autor);
    }
}

