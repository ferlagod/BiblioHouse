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

/**
 * Esta clase representa a un socio de la biblioteca. Un socio es alguien que
 * puede llevarse libros prestados. Aquí guardamos sus datos personales: nombre,
 * apellidos, DNI, etc.
 *
 * @author Fernando Lago
 * @version 1.4
 */
public class Socio {

    private String nombre;
    private String apellidos;
    private String dni; // El DNI o documento de identidad
    private String domicilio; // Dónde vive
    private int numeroSocio; // Un número único para identificarlo 

    /**
     * Constructor por defecto de la clase Socio.
     */
    public Socio() {
    }

    /**
     * Constructor para crear una nueva instancia de Socio con información
     * personal y un número de socio.
     *
     * @param nombre El nombre del socio.
     * @param apellidos Los apellidos del socio.
     * @param dni El DNI del socio.
     * @param domicilio El domicilio del socio.
     * @param numeroSocio El número de socio único.
     */
    public Socio(String nombre, String apellidos, String dni, String domicilio, int numeroSocio) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.dni = dni;
        this.domicilio = domicilio;
        this.numeroSocio = numeroSocio;
    }

    // ---GETTER Y SETTER---
    /**
     * Obtiene el nombre del socio.
     *
     * @return El nombre del socio.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el nombre completo del socio juntando nombre y apellidos.
     *
     * @return El nombre + un espacio + apellidos.
     */
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    /**
     * Establece el nombre del socio.
     *
     * @param nombre El nombre a establecer para el socio.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene los apellidos del socio.
     *
     * @return Los apellidos del socio.
     */
    public String getApellidos() {
        return apellidos;
    }

    /**
     * Establece los apellidos del socio.
     *
     * @param apellidos Los apellidos a establecer para el socio.
     */
    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    /**
     * Obtiene el DNI del socio.
     *
     * @return El DNI del socio.
     */
    public String getDni() {
        return dni;
    }

    /**
     * Establece el DNI del socio.
     *
     * @param dni El DNI a establecer para el socio.
     */
    public void setDni(String dni) {
        this.dni = dni;
    }

    /**
     * Obtiene el domicilio del socio.
     *
     * @return El domicilio del socio.
     */
    public String getDomicilio() {
        return domicilio;
    }

    /**
     * Establece el domicilio del socio.
     *
     * @param domicilio El domicilio a establecer para el socio.
     */
    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    /**
     * Obtiene el número de socio.
     *
     * @return El número de socio.
     */
    public int getNumeroSocio() {
        return numeroSocio;
    }

    /**
     * Establece el número de socio.
     *
     * @param numeroSocio El número de socio a establecer.
     */
    public void setNumeroSocio(int numeroSocio) {
        this.numeroSocio = numeroSocio;
    }

    /**
     * Devuelve una representación en texto del socio con el formato "Nº -
     * Nombre Apellidos".Este método es utilizado por los JComboBox para mostrar
     * los socios.
     *
     * @return el numero de socio con nombre y apellidos
     */
    @Override
    public String toString() {
        return numeroSocio + " - " + nombre + " " + apellidos;
    }

}
