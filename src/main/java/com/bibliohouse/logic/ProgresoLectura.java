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
 * @version 2.0
 */
public class ProgresoLectura {

    private int paginaActual;
    private int paginasTotales;
    private double porcentaje;
    private String fechaUltimaLectura;

    public ProgresoLectura() {
        this(0, 0);
    }

    public ProgresoLectura(int paginaActual, int paginasTotales) {
        this.paginaActual = paginaActual;
        this.paginasTotales = paginasTotales;
        this.porcentaje = (paginasTotales > 0) ? ((double) paginaActual / paginasTotales) * 100.0 : 0.0;
        this.fechaUltimaLectura = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
        actualizarPorcentajeYFecha();
    }

    public int getPaginasTotales() {
        return paginasTotales;
    }

    public void setPaginasTotales(int paginasTotales) {
        this.paginasTotales = paginasTotales;
        actualizarPorcentajeYFecha();
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getFechaUltimaLectura() {
        return fechaUltimaLectura;
    }

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
