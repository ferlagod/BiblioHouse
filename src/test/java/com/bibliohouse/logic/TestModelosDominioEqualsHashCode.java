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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar los contratos equals() y hashCode()
 * en los modelos de dominio (Libro, Socio, Prestamo, NotaLectura).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
class TestModelosDominioEqualsHashCode {

    // =========================================================================
    // PRUEBAS DE LIBRO
    // =========================================================================

    @Test
    @DisplayName("Libro: igualdad y hashCode por UUID (id)")
    void testLibroEqualsPorId() {
        Libro libro1 = new Libro("El Quijote", "Cervantes", "Espasa", "1605", "Clásico", "9781234567890", "");
        String idComun = libro1.getId();

        Libro libroClon = new Libro("Don Quijote de la Mancha", "Miguel de Cervantes", "Otra Editorial", "1615", "Novela", "9780000000000", "");
        libroClon.setId(idComun);

        assertEquals(libro1, libroClon);
        assertEquals(libro1.hashCode(), libroClon.hashCode());

        Libro libroDiferente = new Libro("El Hobbit", "Tolkien", "Minotauro", "1937", "Fantasía", "9789876543210", "");
        assertNotEquals(libro1, libroDiferente);
    }

    @Test
    @DisplayName("Libro: operaciones en List y Set con clones o instancias deserializadas")
    void testLibroColecciones() {
        Libro libro = new Libro("Cien Años de Soledad", "García Márquez", "Sudamericana", "1967", "Realismo", "9788437604947", "");

        List<Libro> lista = new ArrayList<>();
        lista.add(libro);

        Set<Libro> conjunto = new HashSet<>();
        conjunto.add(libro);

        Libro copiaDeserializada = new Libro();
        copiaDeserializada.setId(libro.getId());
        copiaDeserializada.setTitulo(libro.getTitulo());

        assertTrue(lista.contains(copiaDeserializada), "List.contains debe encontrar el libro por equals()");
        assertTrue(conjunto.contains(copiaDeserializada), "Set.contains debe encontrar el libro por hashCode() y equals()");

        boolean eliminado = lista.remove(copiaDeserializada);
        assertTrue(eliminado, "List.remove debe eliminar el elemento usando equals()");
        assertEquals(0, lista.size());
    }

    @Test
    @DisplayName("Libro: fallback por ISBN cuando no hay ID")
    void testLibroFallbackIsbn() {
        Libro libro1 = new Libro();
        libro1.setId(null);
        libro1.setIsbn("978-84-376-0494-7");
        libro1.setTitulo("Título A");

        Libro libro2 = new Libro();
        libro2.setId(null);
        libro2.setIsbn("978-84-376-0494-7");
        libro2.setTitulo("Título B");

        assertEquals(libro1, libro2);
        assertEquals(libro1.hashCode(), libro2.hashCode());
    }

    // =========================================================================
    // PRUEBAS DE SOCIO
    // =========================================================================

    @Test
    @DisplayName("Socio: igualdad y hashCode por numeroSocio")
    void testSocioEqualsPorNumeroSocio() {
        Socio socio1 = new Socio("Ana", "García", "12345678Z", "Calle Mayor 1", 42);
        Socio socioClon = new Socio("Ana María", "García López", "12345678Z", "Calle Nueva 2", 42);
        Socio socioOtro = new Socio("Carlos", "Pérez", "87654321A", "Calle Sol 5", 99);

        assertEquals(socio1, socioClon);
        assertEquals(socio1.hashCode(), socioClon.hashCode());
        assertNotEquals(socio1, socioOtro);
    }

    @Test
    @DisplayName("Socio: operaciones en List y Set con clones")
    void testSocioColecciones() {
        Socio socio = new Socio("Pedro", "Gómez", "11223344B", "Avda Galicia 10", 7);

        List<Socio> lista = new ArrayList<>();
        lista.add(socio);

        Set<Socio> set = new HashSet<>();
        set.add(socio);

        Socio copia = new Socio();
        copia.setNumeroSocio(7);

        assertTrue(lista.contains(copia));
        assertTrue(set.contains(copia));

        assertTrue(lista.remove(copia));
        assertEquals(0, lista.size());
    }

    @Test
    @DisplayName("Socio: fallback por DNI si el numeroSocio es 0")
    void testSocioFallbackDni() {
        Socio s1 = new Socio("Laura", "Vázquez", "44556677C", "Dirección 1", 0);
        Socio s2 = new Socio("Laura", "Vázquez", "44556677C", "Dirección 2", 0);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    // =========================================================================
    // PRUEBAS DE PRESTAMO
    // =========================================================================

    @Test
    @DisplayName("Prestamo: igualdad por UUID (id)")
    void testPrestamoEqualsPorId() {
        Prestamo p1 = new Prestamo("ISBN1", "Libro 1", 10, "Socio 1", LocalDate.of(2026, 1, 15));
        Prestamo p2 = new Prestamo("ISBN2", "Libro 2", 20, "Socio 2", LocalDate.of(2026, 2, 20));
        p2.setId(p1.getId());

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("Prestamo: igualdad por tupla (libroId, numeroSocio, fechaPrestamo) sin ID")
    void testPrestamoEqualsPorTupla() {
        Prestamo p1 = new Prestamo("ISBN-X", "Libro X", 5, "Socio X", LocalDate.of(2026, 3, 1));
        p1.setId(null);
        p1.setLibroId("libro-uuid-123");

        Prestamo p2 = new Prestamo("ISBN-X", "Libro X", 5, "Socio X", LocalDate.of(2026, 3, 1));
        p2.setId(null);
        p2.setLibroId("libro-uuid-123");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("Prestamo: operaciones en colecciones")
    void testPrestamoColecciones() {
        Prestamo p = new Prestamo("ISBN-Y", "Libro Y", 12, "Socio Y", LocalDate.now());
        List<Prestamo> lista = new ArrayList<>();
        lista.add(p);

        Prestamo clon = new Prestamo();
        clon.setId(p.getId());

        assertTrue(lista.contains(clon));
        assertTrue(lista.remove(clon));
        assertTrue(lista.isEmpty());
    }

    // =========================================================================
    // PRUEBAS DE NOTA LECTURA
    // =========================================================================

    @Test
    @DisplayName("NotaLectura: igualdad por ID")
    void testNotaLecturaEquals() {
        NotaLectura n1 = new NotaLectura(NotaLectura.TipoNota.CITA, "Gran frase", 150);
        NotaLectura n2 = new NotaLectura(NotaLectura.TipoNota.NOTA, "Mi opinión", 150);
        n2.setId(n1.getId());

        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
    }
}
