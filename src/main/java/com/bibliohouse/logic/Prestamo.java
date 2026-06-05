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
 * @author Fernando Lago Dávila
 * @version 1.8
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
     * Constructor vacío. Lo necesita Gson para deserializar desde JSON.
     */
    public Prestamo() {
    }

    /**
     * Crea un nuevo préstamo con todos los datos. Esto es para cuando ya tienes
     * toda la información y quieres crear el préstamo de golpe.
     *
     * @param isbnLibro El ISBN del libro que se presta.
     * @param tituloLibro El título del libro.
     * @param numeroSocio El número de socio de quien se lleva el libro.
     * @param nombreSocio El nombre de la persona.
     * @param fechaPrestamo Cuándo se lo lleva.
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
     * Constructor que crea un préstamo recibiendo directamente los
     * objetos.Extrae los datos automáticamente y pone la fecha de hoy.
     *
     * * @param libro El objeto Libro a prestar.
     *
     * @param libro El objeto libro que se presta.
     * @param socio El objeto Socio que lo recibe.
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
     * Obtiene el identificador único del libro.
     *
     * @return Cadena de texto que representa el ID del libro.
     */
    public String getLibroId() {
        return libroId;
    }

    /**
     * Establece el identificador único del libro.
     *
     * @param libroId Nueva cadena de texto que será asignada como ID del libro.
     */
    public void setLibroId(String libroId) {
        this.libroId = libroId;
    }

    /**
     * Obtiene el ISBN del libro prestado.
     *
     * @return El ISBN del libro.
     */
    public String getIsbnLibro() {
        return isbnLibro;
    }

    /**
     * Obtiene el título del libro prestado.
     *
     * @return El título del libro.
     */
    public String getTituloLibro() {
        return tituloLibro;
    }

    /**
     * Obtiene el numero de socio de la persona que realiza el préstamo.
     *
     * @return El numero de socio de la persona.
     */
    public int getNumeroSocio() {
        return numeroSocio;
    }

    /**
     * Obtiene el nombre de la persona que realiza el préstamo.
     *
     * @return El nombre de la persona.
     */
    public String getNombreSocio() {
        return nombreSocio;
    }

    /**
     * Obtiene la fecha en que se realizó el préstamo.
     *
     * @return La fecha de préstamo.
     */
    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    /**
     * Obtiene la fecha de préstamo formateada como una cadena.
     *
     * @return La fecha de préstamo formateada como "dd/MM/yyyy", o una cadena
     * vacía si la fecha es null.
     */
    public String getFechaPrestamoFormateada() {
        if (fechaPrestamo == null) {
            return "";
        }
        return fechaPrestamo.format(DATE_FORMATTER);
    }

    /**
     * Obtiene la fecha de devolución del libro.
     *
     * @return La fecha de devolución, o null si el libro no ha sido devuelto.
     */
    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    /**
     * Obtiene la fecha de devolución formateada como una cadena.
     *
     * @return La fecha de devolución formateada como "dd/MM/yyyy", o
     * "Pendiente" si la fecha es null.
     */
    public String getFechaDevolucionFormateada() {
        if (fechaDevolucion == null) {
            return "Pendiente";
        }
        return fechaDevolucion.format(DATE_FORMATTER);
    }

    /**
     * Establece la fecha de devolución del libro.
     *
     * @param fechaDevolucion La fecha de devolución a establecer.
     */
    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    /**
     * Verifica si el libro ha sido devuelto.
     *
     * @return true si el libro ha sido devuelto, false de lo contrario.
     */
    public boolean isDevuelto() {
        return fechaDevolucion != null;
    }
}
