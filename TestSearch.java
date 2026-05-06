package com.bibliohouse;

import com.bibliohouse.logic.BusquedaService;
import com.bibliohouse.logic.Libro;
import java.util.List;

public class TestSearch {
    public static void main(String[] args) throws Exception {
        BusquedaService service = new BusquedaService();
        System.out.println("Starting search...");
        List<Libro> result = service.ejecutarBusquedaGlobalAsync("Harry Potter").get();
        System.out.println("Search finished. Found: " + result.size());
        for (Libro l : result) {
            System.out.println(l.getTitulo() + " - " + l.getAutor());
        }
        System.exit(0);
    }
}
