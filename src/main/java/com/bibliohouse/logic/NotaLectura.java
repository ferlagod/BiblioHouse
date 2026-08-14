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
 * SIN NINGUNA GARANTÍA; sin incluso la garantía implícita de
 * COMERCIABILIDAD o APTITUD PARA UN PROPÓSITO PARTICULAR. Vea la
 * Licencia Pública General de GNU para más detalles.
 *
 * Usted debería haber recibido una copia de la Licencia Pública General de GNU
 * junto con este programa. Si no es así, vea <https://www.gnu.org/licenses/>.
 */
package com.bibliohouse.logic;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Esta clase sirve para guardar una nota o cita que tomamos mientras leemos un libro.
 * Aquí guardamos lo que escribimos, de qué página lo sacamos y la fecha en la que
 * lo hicimos. ¡Como tener un diario de lectura!
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.0
 */
public class NotaLectura {

    /**
     * Enumeración para diferenciar el tipo de nota que estamos haciendo.
     */
    public enum TipoNota {
        /**
         * Cuando copiamos tal cual algo que dice el libro (una cita literal).
         */
        CITA,
        /**
         * Cuando escribimos nosotros mismos un comentario o pensamiento.
         */
        NOTA
    }

    private String id;
    private String fechaHora;
    private TipoNota tipo;
    private String texto;
    private int paginaReferencia;

    /**
     * Constructor vacío.
     * Nos sirve para crear una nota desde cero. Ya le pone un número
     * de identificación (ID) único para que no se pierda y la hora actual.
     */
    public NotaLectura() {
        this.id = UUID.randomUUID().toString();
        this.fechaHora = LocalDateTime.now().toString();
    }

    /**
     * Constructor para crear una nota de golpe cuando ya sabemos todos los datos.
     * También le pone él solito la fecha de ahora mismo y su ID.
     *
     * @param tipo Si es una CITA o una NOTA tuya.
     * @param texto Lo que quieres dejar escrito.
     * @param paginaReferencia En qué número de página del libro va esto.
     */
    public NotaLectura(TipoNota tipo, String texto, int paginaReferencia) {
        this.id = UUID.randomUUID().toString();
        this.fechaHora = LocalDateTime.now().toString();
        this.tipo = tipo;
        this.texto = texto;
        this.paginaReferencia = paginaReferencia;
    }

    /**
     * Nos devuelve el identificador (el ID raro único) que tiene esta nota.
     *
     * @return El ID de la nota en forma de texto.
     */
    public String getId() {
        return id;
    }

    /**
     * Nos dice en qué momento (fecha y hora) hicimos la nota.
     *
     * @return Un texto con la fecha y la hora.
     */
    public String getFechaHora() {
        return fechaHora;
    }

    /**
     * Nos sirve para cambiarle a mano la fecha o la hora a la nota.
     *
     * @param fechaHora La nueva fecha y hora que le queremos poner.
     */
    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    /**
     * Nos chiva qué tipo de contenido es, si es una CITA literal o una NOTA nuestra.
     *
     * @return El tipo de nota (CITA o NOTA).
     */
    public TipoNota getTipo() {
        return tipo;
    }

    /**
     * Sirve para marcar esta nota como que es una CITA o que es una NOTA.
     *
     * @param tipo El tipo que le queremos poner.
     */
    public void setTipo(TipoNota tipo) {
        this.tipo = tipo;
    }

    /**
     * Nos da el texto largo que escribimos en la nota.
     *
     * @return El texto que guardamos.
     */
    public String getTexto() {
        return texto;
    }

    /**
     * Nos permite guardar o modificar el texto de lo que escribimos.
     *
     * @param texto El texto nuevo a guardar.
     */
    public void setTexto(String texto) {
        this.texto = texto;
    }

    /**
     * Nos dice a qué página del libro pertenece esta nota o cita.
     *
     * @return El número de la página.
     */
    public int getPaginaReferencia() {
        return paginaReferencia;
    }

    /**
     * Sirve para guardar el número de página donde encontramos esto.
     *
     * @param paginaReferencia El numerito de la página.
     */
    public void setPaginaReferencia(int paginaReferencia) {
        this.paginaReferencia = paginaReferencia;
    }
}