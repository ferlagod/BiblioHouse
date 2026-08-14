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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Esto representa un préstamo de libro a alguien. Aquí guardamos quién se llevó
 * el libro, cuándo se lo llevó y cuándo lo devolvió (si es que ya lo devolvió).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class Prestamo {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Usamos el ID interno (UUID) para identificar inequívocamente el libro
    private String libroId;
    // Mantenemos ISBN y Título como caché para mostrar datos si el libro se borra
    private String isbnLibro;
    // El título, para mostrarlo más fácil
    private String tituloLibro;
    // A quién se lo hemos prestado
    private int numeroSocio;
    // El nombre completo del socio
    private String nombreSocio;
    // Cuándo se lo llevó
    private LocalDate fechaPrestamo;
    // Cuándo lo devolvió (null si todavía no lo ha devuelto)
    private LocalDate fechaDevolucion;

    /**
     * Constructor vacío. Lo usamos principalmente para que el programa pueda
     * cargar los datos guardados sin volverse loco (Gson lo necesita).
     */
    public Prestamo() {
    }

    /**
     * Crea un nuevo préstamo de golpe cuando ya tienes todos los datos sueltos.
     *
     * @param isbnLibro El ISBN del libro que prestamos.
     * @param tituloLibro El título del libro.
     * @param numeroSocio El número de carné de la persona.
     * @param nombreSocio El nombre del que se lo lleva.
     * @param fechaPrestamo El día en que se lo lleva.
     */
    public Prestamo(String isbnLibro, String tituloLibro, int numeroSocio, String nombreSocio,
            LocalDate fechaPrestamo) {
        this.libroId = null; // En constructores legacy o manuales sin objeto Libro, esto puede ser null
        this.isbnLibro = isbnLibro;
        this.tituloLibro = tituloLibro;
        this.numeroSocio = numeroSocio;
        this.nombreSocio = nombreSocio;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = null;
    }

    /**
     * Constructor súper útil que crea un préstamo pasándole directamente el 
     * Libro y el Socio. Él solito saca los datos de ambos y le pone la fecha de hoy.
     *
     * @param libro El libro que le estamos prestando.
     * @param socio La persona que se lo lleva a casa.
     */
    public Prestamo(Libro libro, Socio socio) {
        // Extraemos datos del Libro
        this.libroId = libro.getId();
        this.isbnLibro = libro.getIsbn();
        this.tituloLibro = libro.getTitulo();

        // Extraemos datos del Socio
        this.numeroSocio = socio.getNumeroSocio();
        // Unimos nombre y apellidos para guardar el nombre completo
        this.nombreSocio = socio.getNombre() + " " + socio.getApellidos();

        // Asignamos la fecha de hoy automáticamente
        this.fechaPrestamo = LocalDate.now();
        this.fechaDevolucion = null;
    }

    // --- Getters y Setters ---
    /**
     * Nos devuelve el identificador interno del libro que se prestó.
     *
     * @return El ID del libro en texto.
     */
    public String getLibroId() {
        return libroId;
    }

    /**
     * Nos deja cambiar el identificador interno del libro.
     *
     * @param libroId El nuevo ID del libro.
     */
    public void setLibroId(String libroId) {
        this.libroId = libroId;
    }

    /**
     * Nos dice el ISBN del libro que hemos prestado.
     *
     * @return El ISBN del libro.
     */
    public String getIsbnLibro() {
        return isbnLibro;
    }

    /**
     * Nos dice cómo se llama el libro que hemos prestado.
     *
     * @return El título del libro.
     */
    public String getTituloLibro() {
        return tituloLibro;
    }

    /**
     * Nos dice el número del socio que tiene el libro ahora mismo.
     *
     * @return El número de socio.
     */
    public int getNumeroSocio() {
        return numeroSocio;
    }

    /**
     * Nos da el nombre de la persona que se llevó el libro a su casa.
     *
     * @return El nombre de la persona.
     */
    public String getNombreSocio() {
        return nombreSocio;
    }

    /**
     * Nos dice qué día exacto se llevó el libro.
     *
     * @return La fecha en la que empezó el préstamo.
     */
    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    /**
     * Nos da la fecha del préstamo ya puesta bonita en texto (ej: 25/12/2026),
     * para no tener que pelearnos con formatos nosotros.
     *
     * @return La fecha en texto lista para enseñar en pantalla.
     */
    public String getFechaPrestamoFormateada() {
        if (fechaPrestamo == null) {
            return "";
        }
        return fechaPrestamo.format(DATE_FORMATTER);
    }

    /**
     * Nos dice qué día nos devolvió el libro (si es que lo ha devuelto, si no, da null).
     *
     * @return La fecha de devolución, o null si aún lo tiene secuestrado.
     */
    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    /**
     * Nos da la fecha de devolución en un texto bonito para enseñar en la pantalla.
     * Si no lo ha devuelto todavía, nos devuelve la palabra "Pendiente".
     *
     * @return La fecha bonita o el texto "Pendiente".
     */
    public String getFechaDevolucionFormateada() {
        if (fechaDevolucion == null) {
            return "Pendiente";
        }
        return fechaDevolucion.format(DATE_FORMATTER);
    }

    /**
     * Sirve para apuntar qué día nos devolvió por fin el libro.
     *
     * @param fechaDevolucion El día que nos lo dio.
     */
    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    /**
     * Nos chiva con un verdadero o falso si el libro ya está devuelto o no.
     *
     * @return true si ya lo tenemos nosotros, false si sigue por ahí prestado.
     */
    public boolean isDevuelto() {
        return fechaDevolucion != null;
    }
}
