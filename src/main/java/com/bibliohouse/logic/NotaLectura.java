package com.bibliohouse.logic;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una nota o cita vinculada a un libro en el Diario de Lectura.
 */
public class NotaLectura {

    public enum TipoNota {
        CITA,
        NOTA
    }

    private String id;
    private String fechaHora;
    private TipoNota tipo;
    private String texto;
    private int paginaReferencia;

    public NotaLectura() {
        this.id = UUID.randomUUID().toString();
        this.fechaHora = LocalDateTime.now().toString();
    }

    public NotaLectura(TipoNota tipo, String texto, int paginaReferencia) {
        this.id = UUID.randomUUID().toString();
        this.fechaHora = LocalDateTime.now().toString();
        this.tipo = tipo;
        this.texto = texto;
        this.paginaReferencia = paginaReferencia;
    }

    public String getId() {
        return id;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public TipoNota getTipo() {
        return tipo;
    }

    public void setTipo(TipoNota tipo) {
        this.tipo = tipo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public int getPaginaReferencia() {
        return paginaReferencia;
    }

    public void setPaginaReferencia(int paginaReferencia) {
        this.paginaReferencia = paginaReferencia;
    }
}
