# BiblioHouse    
![Portada](https://imgur.com/CJOQvUS.png)


**BiblioHouse** es una aplicación de escritorio de código abierto para catalogar y gestionar tu biblioteca personal. Creada con Java y Swing, te permite mantener un inventario ordenado de todos tus libros de forma sencilla y eficiente.

### Capturas de Pantalla

**Pantalla Principal - Añadir Libro**

![Imagen de la pestaña para añadir un libro](https://imgur.com/zCZ0YfJ.png)


**Búsqueda en OpenLibrary**

![Búsqueda en OpenLibrary](https://i.imgur.com/cQc6Jfl.png)


**Gestión de la Biblioteca**

![Imagen de la pestaña para buscar y editar tus libros](https://imgur.com/cQ2Buau.png)

**Ventana de editar**

![Ventana de editar](https://imgur.com/sivEUK5.png)


### ✨ Características Principales

* **Catalogación de Libros**: Guarda toda la información importante de tus libros: título, autor, editorial, año de publicación, género, ISBN y la imagen de la portada.
* **Doble Vía de Entrada**:
    * **Entrada Manual**: Añade libros rellenando los campos tú mismo.
    * **Búsqueda en OpenLibrary**: Busca un libro por título o ISBN en la enorme base de datos de OpenLibrary y añádelo a tu colección con un solo clic. La información principal (incluida la portada) se descarga y guarda automáticamente.
* **Gestión Completa**: Busca en tu biblioteca por cualquier campo, edita la información de un libro existente o elimínalo si ya no lo tienes.
* **Gestión de Préstamos**:
      * Mantén un control total sobre los libros que prestas gracias a una pestaña dedicada.
      * Anota fácilmente a quién prestas un libro, la fecha del préstamo y márcalo como devuelto con un solo clic.
      * Encuentra rápidamente qué libros están prestados o a quién le has prestado un libro con su potente buscador.
  * **Calificaciones y Reseñas Personales**:
       * Califica tus libros con un sistema de 1 a 5 estrellas.
       * Añade tus propias notas y reseñas para no olvidar qué te pareció cada libro.
* **Prevención de Duplicados**: La aplicación te avisa si intentas añadir un libro que ya existe en tu biblioteca.
* **Base de Datos Local**: Toda tu información se guarda en un archivo `biblioteca.xml` en una carpeta dedicada en tu ordenador, dándote control total sobre tus datos.
* **Vista de Detalle**: Haz doble clic en un libro de tu biblioteca para abrir una ventana con toda su información detallada y de solo lectura.
* **Importar y Exportar**:
    * **Exporta** tu base de datos para tener una copia de seguridad.
    * **Importa** una base de datos para restaurar una copia o mover tu colección a otro ordenador.
* **Herramientas Útiles**:
    * Encuentra posibles duplicados en tu colección.
    * Configura una carpeta por defecto para tus exportaciones.
    * Manual de usuario integrado.
* **Interfaz Moderna**: Utiliza el look and feel de FlatLaf para una apariencia limpia y moderna, con un estilo inspirado en macOS.

### 🛠️ Tecnologías Utilizadas

* **Lenguaje**: Java 21
* **Interfaz Gráfica**: Java Swing
* **Base de Datos**: Archivo XML gestionado con la librería **XStream**.
* **API Externa**: [OpenLibrary Search API](https://openlibrary.org/dev/docs/api/search) para la búsqueda de libros.
* **Look and Feel**: [FlatLaf](https://www.formdev.com/flatlaf/) para el diseño de la interfaz.
* **Entorno de Desarrollo**: Apache NetBeans IDE

### 📄 Licencia

Este proyecto está licenciado bajo la **Licencia Pública General de GNU v3.0**. Consulta el archivo `LICENSE` para más detalles.

---

Desarrollado con ❤️ por **ferlagod**.
