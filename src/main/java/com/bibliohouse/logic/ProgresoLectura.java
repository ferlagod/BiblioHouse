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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modelo ligero para persistir el avance y seguimiento de lectura de un libro
 * sin necesidad de re-serializar la totalidad de los metadatos de la biblioteca.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class ProgresoLectura {

    private int paginaActual;
    private int paginasTotales;
    private double porcentaje;
    private String fechaUltimaLectura;

    /**
     * Constructor por defecto requerido para serialización y deserialización JSON.
     * Inicializa la página actual y totales en cero.
     */
    public ProgresoLectura() {
        this(0, 0);
    }

    /**
     * Crea una nueva instancia de progreso de lectura con las páginas especificadas.
     * Calcula automáticamente el porcentaje y registra la marca de tiempo actual.
     *
     * @param paginaActual   Página actual alcanzada por el lector.
     * @param paginasTotales Número total de páginas del libro.
     */
    public ProgresoLectura(int paginaActual, int paginasTotales) {
        this.paginaActual = paginaActual;
        this.paginasTotales = paginasTotales;
        this.porcentaje = (paginasTotales > 0) ? ((double) paginaActual / paginasTotales) * 100.0 : 0.0;
        this.fechaUltimaLectura = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Obtiene la página actual leída.
     *
     * @return Número de página actual.
     */
    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Establece la página actual leída y recalcula el porcentaje y fecha de lectura.
     *
     * @param paginaActual Nueva página alcanzada.
     */
    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
        actualizarPorcentajeYFecha();
    }

    /**
     * Obtiene el número total de páginas del libro.
     *
     * @return Total de páginas.
     */
    public int getPaginasTotales() {
        return paginasTotales;
    }

    /**
     * Establece el número total de páginas y recalcula el porcentaje de avance.
     *
     * @param paginasTotales Total de páginas del libro.
     */
    public void setPaginasTotales(int paginasTotales) {
        this.paginasTotales = paginasTotales;
        actualizarPorcentajeYFecha();
    }

    /**
     * Obtiene el porcentaje de lectura completado (de 0.0 a 100.0).
     *
     * @return Porcentaje de avance de lectura.
     */
    public double getPorcentaje() {
        return porcentaje;
    }

    /**
     * Establece manualmente el porcentaje de lectura completado.
     *
     * @param porcentaje Porcentaje numérico (0.0 a 100.0).
     */
    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    /**
     * Obtiene la fecha y hora de la última sesión de lectura en formato ISO-8601.
     *
     * @return Cadena con la fecha y hora en formato ISO.
     */
    public String getFechaUltimaLectura() {
        return fechaUltimaLectura;
    }

    /**
     * Establece la fecha y hora de la última sesión de lectura.
     *
     * @param fechaUltimaLectura Cadena con la fecha y hora.
     */
    public void setFechaUltimaLectura(String fechaUltimaLectura) {
        this.fechaUltimaLectura = fechaUltimaLectura;
    }

    private void actualizarPorcentajeYFecha() {
        this.porcentaje = (paginasTotales > 0) ? ((double) paginaActual / paginasTotales) * 100.0 : 0.0;
        this.fechaUltimaLectura = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgresoLectura that = (ProgresoLectura) o;
        return paginaActual == that.paginaActual && paginasTotales == that.paginasTotales;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paginaActual, paginasTotales);
    }

    @Override
    public String toString() {
        return "ProgresoLectura{" +
                "paginaActual=" + paginaActual +
                ", paginasTotales=" + paginasTotales +
                ", porcentaje=" + porcentaje +
                ", fechaUltimaLectura='" + fechaUltimaLectura + '\'' +
                '}';
    }
}
