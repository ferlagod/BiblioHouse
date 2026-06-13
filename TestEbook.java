import com.bibliohouse.logic.EbookMetadataService;
import com.bibliohouse.logic.Libro;
import java.io.File;

public class TestEbook {
    public static void main(String[] args) {
        File file = new File("test.epub");
        Libro libro = EbookMetadataService.crearLibroDesdeArchivo(file, "/tmp");
        if (libro != null) {
            System.out.println("Title: " + libro.getTitulo());
            System.out.println("Author: " + libro.getAutor());
            System.out.println("Cover URL: " + libro.getPortadaURL());
        } else {
            System.out.println("Failed to create libro");
        }
    }
}
