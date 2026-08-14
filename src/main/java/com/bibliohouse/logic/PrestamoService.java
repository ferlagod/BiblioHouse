package com.bibliohouse.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio encargado de gestionar las operaciones de préstamos, devoluciones y
 * comprobación de vencimientos.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class PrestamoService {

    private static final Logger LOGGER = Logger.getLogger(PrestamoService.class.getName());

    private final JsonManager jsonManager;
    private final List<Prestamo> listaPrestamosCompleta;
    private final List<Libro> listaLibrosCompleta;

    /**
     * Crea una nueva instancia de PrestamoService con las dependencias
     * necesarias para gestionar préstamos de libros.
     *
     * @param jsonManager Gestor de persistencia para guardar y cargar datos de
     * préstamos y libros.
     * @param listaPrestamosCompleta Lista completa de préstamos (activos e
     * históricos) de la biblioteca.
     * @param listaLibrosCompleta Lista completa de libros disponibles en la
     * biblioteca.
     */
    public PrestamoService(JsonManager jsonManager, List<Prestamo> listaPrestamosCompleta, List<Libro> listaLibrosCompleta) {
        this.jsonManager = jsonManager;
        this.listaPrestamosCompleta = listaPrestamosCompleta;
        this.listaLibrosCompleta = listaLibrosCompleta;
    }

    /**
     * Valida y realiza un préstamo, restando el stock correspondiente.
     *
     * @param libroOriginal El libro a prestar.
     * @param socio El socio que toma prestado el libro.
     * @return El préstamo registrado si tiene éxito.
     * @throws IllegalArgumentException si no hay stock o si el socio ya tiene
     * el libro.
     */
    public Prestamo realizarPrestamo(Libro libroOriginal, Socio socio) throws IllegalArgumentException {
        if (libroOriginal == null || socio == null) {
            throw new IllegalArgumentException("El libro y el socio no pueden ser nulos.");
        }

        if (libroOriginal.getCantidad() <= 0) {
            throw new IllegalArgumentException("No quedan ejemplares disponibles de este libro.");
        }

        // Evitar duplicados
        boolean yaLoTiene = listaPrestamosCompleta.stream()
                .anyMatch(p -> {
                    if (p.getNumeroSocio() == socio.getNumeroSocio() && p.getFechaDevolucion() == null) {
                        if (p.getLibroId() != null) {
                            return p.getLibroId().equals(libroOriginal.getId());
                        } else {
                            return p.getTituloLibro() != null && p.getTituloLibro().equals(libroOriginal.getTitulo());
                        }
                    }
                    return false;
                });

        if (yaLoTiene) {
            throw new IllegalArgumentException(socio.getNombre() + " ya tiene una copia activa de este libro.");
        }

        Prestamo nuevoPrestamo = new Prestamo(libroOriginal, socio);
        listaPrestamosCompleta.add(nuevoPrestamo);
        libroOriginal.setCantidad(libroOriginal.getCantidad() - 1);

        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));

        return nuevoPrestamo;
    }

    /**
     * Marca un préstamo como devuelto y repone el stock.
     *
     * @param p El préstamo a devolver.
     * @throws IllegalArgumentException si ya estaba devuelto o si no es válido.
     */
    public void marcarDevuelto(Prestamo p) throws IllegalArgumentException {
        if (p == null) {
            throw new IllegalArgumentException("El préstamo proporcionado es nulo.");
        }
        if (p.getFechaDevolucion() != null) {
            throw new IllegalArgumentException("Este préstamo ya figura como devuelto el " + p.getFechaDevolucionFormateada());
        }

        p.setFechaDevolucion(LocalDate.now());

        listaLibrosCompleta.stream()
                .filter(l -> l.getId().equals(p.getLibroId()))
                .findFirst()
                .ifPresentOrElse(
                        libro -> {
                            libro.setCantidad(libro.getCantidad() + 1);
                            LOGGER.log(Level.INFO, "Stock devuelto para el libro: {0}", libro.getTitulo());
                        },
                        () -> LOGGER.log(Level.WARNING, "No se encontró el libro con ID {0} para devolver stock.", p.getLibroId())
                );

        jsonManager.guardarPrestamos(new ArrayList<>(listaPrestamosCompleta));
        jsonManager.guardarLibros(new ArrayList<>(listaLibrosCompleta));
    }

    /**
     * Obtiene una lista de préstamos que han sobrepasado su fecha límite.
     *
     * @param dueDaysLimit Límite de días para considerar un préstamo vencido.
     * @return Lista de préstamos vencidos.
     */
    public List<Prestamo> obtenerPrestamosVencidos(int dueDaysLimit) {
        return listaPrestamosCompleta.stream()
                .filter(p -> p.getFechaDevolucion() == null)
                .filter(p -> p.getFechaPrestamo() != null)
                .filter(p -> p.getFechaPrestamo().isBefore(LocalDate.now().minusDays(dueDaysLimit)))
                .collect(Collectors.toList());
    }

    /**
     * Calcula los días de retraso para un préstamo vencido.
     *
     * @param p El préstamo vencido.
     * @param dueDaysLimit Límite de días configurado.
     * @return Días de retraso.
     */
    public long calcularDiasRetraso(Prestamo p, int dueDaysLimit) {
        if (p.getFechaPrestamo() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(p.getFechaPrestamo().plusDays(dueDaysLimit), LocalDate.now());
    }
}
