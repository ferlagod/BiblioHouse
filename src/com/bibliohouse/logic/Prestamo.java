/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.bibliohouse.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Representa un préstamo de un libro a una persona. Esta clase contiene
 * información sobre el préstamo de un libro, incluyendo el ISBN y título del
 * libro, el nombre de la persona que realiza el préstamo, y las fechas de
 * préstamo y devolución.
 *
 * @author ferlagod
 * @version 0.3
 */
public class Prestamo {

    private String isbnLibro; // Usamos el ISBN para identificar el libro de forma única
    private String tituloLibro; // Guardamos el título para mostrarlo fácilmente
    private String nombrePersona;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion; // Será null si el libro no ha sido devuelto

    /**
     * Constructor vacío para la librería XStream. Este constructor es necesario
     * para la deserialización XML.
     */
    public Prestamo() {
    }

    /**
     * Constructor para crear un nuevo préstamo.
     *
     * @param isbnLibro El ISBN del libro prestado.
     * @param tituloLibro El título del libro prestado.
     * @param nombrePersona El nombre de la persona que realiza el préstamo.
     * @param fechaPrestamo La fecha en que se realiza el préstamo.
     */
    public Prestamo(String isbnLibro, String tituloLibro, String nombrePersona, LocalDate fechaPrestamo) {
        this.isbnLibro = isbnLibro;
        this.tituloLibro = tituloLibro;
        this.nombrePersona = nombrePersona;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = null; // Un nuevo préstamo no está devuelto
    }

    // --- Getters y Setters ---
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
     * Obtiene el nombre de la persona que realiza el préstamo.
     *
     * @return El nombre de la persona.
     */
    public String getNombrePersona() {
        return nombrePersona;
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
     * vacía si la fecha es nula.
     */
    public String getFechaPrestamoFormateada() {
        if (fechaPrestamo == null) {
            return "";
        }
        return fechaPrestamo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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
     * "Pendiente" si la fecha es nula.
     */
    public String getFechaDevolucionFormateada() {
        if (fechaDevolucion == null) {
            return "Pendiente";
        }
        return fechaDevolucion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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
