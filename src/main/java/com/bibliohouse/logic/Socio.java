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
 * @author Fernando Lago Dávila
 * @version 1.9
 */
public class Socio {

    private String nombre;
    private String apellidos;
    private String dni; // El DNI o documento de identidad
    private String domicilio; // Dónde vive
    private int numeroSocio; // Un número único para identificarlo 

    /**
     * Constructor vacío. Nos sirve para crear un socio sin datos iniciales
     * y poder ir llenándolos después poco a poco.
     */
    public Socio() {
    }

    /**
     * Constructor que nos permite crear un socio dándole todos sus datos de una sola vez.
     *
     * @param nombre El nombre de la persona.
     * @param apellidos Los apellidos de la persona.
     * @param dni El carné de identidad o documento del socio.
     * @param domicilio La dirección donde vive.
     * @param numeroSocio El número identificador único en nuestra biblioteca.
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
     * Nos devuelve el nombre que tiene guardado este socio.
     *
     * @return El nombre en formato texto.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Nos da el nombre completo del socio, juntando su nombre y sus apellidos
     * con un espacio en el medio para que quede bonito.
     *
     * @return El texto con el nombre y los apellidos.
     */
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    /**
     * Nos permite cambiar o guardar un nombre nuevo para el socio.
     *
     * @param nombre El nombre que le queremos poner.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Nos devuelve los apellidos que tiene guardados el socio.
     *
     * @return Los apellidos en formato texto.
     */
    public String getApellidos() {
        return apellidos;
    }

    /**
     * Nos permite cambiar o guardar los apellidos del socio.
     *
     * @param apellidos Los apellidos que le queremos poner.
     */
    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    /**
     * Nos devuelve el DNI o documento del socio.
     *
     * @return El DNI guardado.
     */
    public String getDni() {
        return dni;
    }

    /**
     * Sirve para cambiar o asignar el DNI del socio.
     *
     * @param dni El nuevo documento de identidad.
     */
    public void setDni(String dni) {
        this.dni = dni;
    }

    /**
     * Nos dice dónde vive el socio (su dirección o domicilio).
     *
     * @return La dirección donde vive.
     */
    public String getDomicilio() {
        return domicilio;
    }

    /**
     * Nos permite actualizar la dirección o domicilio del socio.
     *
     * @param domicilio La nueva dirección donde va a vivir.
     */
    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    /**
     * Nos devuelve el número único que identifica a este socio en la biblioteca.
     *
     * @return El número de socio.
     */
    public int getNumeroSocio() {
        return numeroSocio;
    }

    /**
     * Sirve para asignarle un número único a este socio.
     *
     * @param numeroSocio El numerito que le toca.
     */
    public void setNumeroSocio(int numeroSocio) {
        this.numeroSocio = numeroSocio;
    }

    /**
     * Nos devuelve una cadena de texto lista para mostrar en pantalla, por ejemplo
     * en los desplegables (JComboBox). Queda algo así como "1 - Juan Pérez".
     *
     * @return Un texto combinando el número, el nombre y los apellidos.
     */
    @Override
    public String toString() {
        return numeroSocio + " - " + nombre + " " + apellidos;
    }

}
