# ![BiblioHouse Logo](https://imgur.com/ibLRiqI.png) BiblioHouse

[![Version: 2.1.0](https://img.shields.io/badge/Version-2.1.0-blue.svg?style=flat-square)](https://forjalibre.eu/ferlagod/BiblioHouse)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)](https://adoptium.net/)
[![JavaFX 21.0.5](https://img.shields.io/badge/JavaFX-21.0.5-blue.svg?style=flat-square)](https://openjfx.io/)
[![Tests](https://img.shields.io/badge/Tests-58%2F58%20Passing-brightgreen.svg?style=flat-square)](https://forjalibre.eu/ferlagod/BiblioHouse)
[![License: GPL v3](https://img.shields.io/badge/License-GPL_v3-green.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Open Source](https://img.shields.io/badge/Open_Source-%E2%9D%A4-red.svg?style=flat-square)](https://forjalibre.eu/ferlagod/BiblioHouse)

**BiblioHouse** es un gestor de biblioteca personal moderno, ultrarrápido y de código abierto desarrollado en **Java 21** y **JavaFX**. Diseñado específicamente para amantes de la lectura, investigadores y coleccionistas que exigen control absoluto sobre sus datos: **todo se almacena y procesa localmente en tu equipo, garantizando la privacidad y soberanía digital de tu biblioteca.**

---

## ✨ Características Principales

### 📚 Catálogo, Organización y Lectura
* **Cuadrícula Visual Interactiva y Paginación Fluida:** Explora tus libros con portadas de alta calidad, insignias de estado y un sistema de **virtualización/paginación configurable** (24, 48, 96 o vista completa) para manejar colecciones de miles de ejemplares sin caídas de rendimiento.
* **Lector Digital Integrado con Streaming Local:** Lee tus archivos **EPUB** directamente dentro de la aplicación mediante un mini servidor HTTP loopback por bloques (*streaming* de 64 KB), eliminando bloqueos de pantalla y reduciendo el consumo de memoria en libros pesados o ilustrados.
* **Seguimiento de Lectura (Reading Tracker):** Visualiza barras de avance de páginas en cada tarjeta, actualiza tu progreso de lectura en tiempo real y guárdalo atómicamente con persistencia ligera sin reescribir toda la biblioteca.
* **Reto Anual de Lectura:** Configura tu objetivo de libros leídos para el año en curso y sigue tu porcentaje de avance con el widget reactivo de la barra lateral.
* **Gestor de Sagas y Series:** Agrupa tus libros por series literarias y detecta visualmente las entregas faltantes para completar tus colecciones.
* **Lista de Deseos (Wishlist):** Gestiona tus futuras adquisiciones en un panel independiente que no altera las estadísticas de tu colección. Muévelos a tu estantería con un solo clic al comprarlos.
* **Importación CSV Inteligente:** Migra fácilmente tu biblioteca desde **Goodreads** o **Bookwyrm**, conservando calificaciones, fechas y estados de lectura.

### 🔍 Detección, Escáner y Metadatos
* **Búsqueda Multifuente Concurrente:** Consulta simultáneamente en **OpenLibrary**, **Google Books** e **Inventaire** para autocompletar portadas, sinopsis, géneros, autores y número de páginas.
* **Escáner de Código de Barras a 30 FPS:** Captura códigos ISBN en ráfaga usando tu cámara web con **OpenCV** y **ZXing**, optimizado para reutilizar búferes nativos y minimizar la presión sobre el recolector de basura.
* **Drag & Drop de E-books y Portadas:** Arrastra archivos EPUB, PDF o imágenes directamente sobre la ventana para importar libros o actualizar carátulas al instante.

### 🔒 Privacidad, Sincronización y Modo Offline
* **Cifrado AES-256-GCM:** Protección criptográfica con clave maestra de 256 bits, vector de inicialización (IV) aleatorio por operación y autenticación de integridad (AEAD) para tus credenciales de sincronización.
* **Sincronización con NextCloud:** Copia de seguridad y sincronización bidireccional automática mediante WebDAV con tu nube privada.
* **Portadas 100% Offline:** Descarga y almacenamiento local de portadas para consultar tu catálogo con total normalidad sin conexión a internet.

### 🏷️ Préstamos, Socios y Exportación
* **Gestión de Préstamos y Control de Devoluciones:** Administra préstamos a socios con avisos automáticos de libros vencidos y registro histórico completo.
* **Generación de Carnets y Etiquetas PDF:** Imprime credenciales de socios con código de barras y genera etiquetas en PDF con Apache PDFBox y tipografías TrueType Unicode (Roboto).
* **Exportación a Web y PDF:** Exporta tu biblioteca completa como un catálogo interactivo HTML/CSS listo para publicar en la web o como informe PDF maquetado.

### 🌍 Idiomas y Compatibilidad
* **Multi-idioma:** Interfaz completa traducida a **Español, Galego, Català, Euskara, English y Português**.
* **Multiplataforma:** Totalmente compatible y optimizado para **macOS** (Apple Silicon e Intel), **Windows 10/11** y **Linux** (Ubuntu, Debian, Fedora, Arch).

---

## 📷 Capturas de Pantalla

| Inicio | Pantalla Principal / Catálogo |
|:---:|:---:|
| ![Inicio](https://i.postimg.cc/KYXHF8sD/Captura-de-pantalla-2026-05-02-a-las-14-09-32.png) | ![Catálogo](https://i.postimg.cc/TP84G3Hz/Captura-de-pantalla-2026-05-02-a-las-14-09-47.png) |

| Lector Digital EPUB | Edición y Ficha de Libro |
|:---:|:---:|
| ![Visor](https://i.postimg.cc/ZqkMZ5DX/Captura-de-pantalla-2026-05-02-a-las-14-10-19.png) | ![Edición](https://i.postimg.cc/QMLysdnP/Captura-de-pantalla-2026-05-02-a-las-14-10-41.png) |

| Configuración y Nube | Manual de Usuario Integrado |
|:---:|:---:|
| ![Configuración](https://i.postimg.cc/BvWVsnwd/Captura-de-pantalla-2026-05-06-a-las-15-19-44.png) | ![Manual](https://i.postimg.cc/d0MxvVxt/Captura-de-pantalla-2026-05-06-a-las-15-45-09.png) |

---

## 🚀 Instalación y Arranque

### Requisitos Previos
* **Java Development Kit (JDK) 21** o superior ([Eclipse Temurin](https://adoptium.net/) recomendado).
* **Apache Maven 3.8+** instalado.

### Compilar y Ejecutar en Modo Desarrollo

1. Clona el repositorio:
   ```bash
   git clone https://forjalibre.eu/ferlagod/BiblioHouse.git
   cd BiblioHouse
   ```

2. Compila el proyecto y ejecuta la suite de pruebas:
   ```bash
   mvn clean test
   ```

3. Inicia la aplicación:
   ```bash
   mvn javafx:run
   ```

---

## 📦 Generación de Instaladores Nativos (`jpackage`)

BiblioHouse incluye configuración lista para generar instaladores independientes con JRE embebido mediante `jpackage`:

### En macOS (genera archivo `.dmg`):
```bash
mvn clean package
mvn jpackage:jpackage
```
El instalador `.dmg` generado se encontrará en la carpeta `target/dist/`.

### En Windows (genera instalador `.exe` o `.msi`):
```cmd
mvn clean package
mvn jpackage:jpackage
```

---

## 🛠️ Tecnologías Utilizadas

| Componente | Tecnología |
| :--- | :--- |
| **Lenguaje Core** | Java 21 (LTS) |
| **Interfaz Gráfica** | JavaFX 21.0.5 + FXML + CSS |
| **Captura y Escáner** | OpenCV 4.9.0 + ZXing 3.5.3 |
| **Persistencia** | Gson 2.13.2 (JSON atómico debounced) |
| **Documentos y Etiquetas** | Apache PDFBox 3.0.4 + Roboto TrueType Fonts |
| **Sincronización WebDAV** | Sardine 5.10 |
| **Pruebas Unitarias** | JUnit 5 + Mockito 5.12 |
| **Empaquetado Nativo** | Maven Compiler Plugin 3.13.0 + JPackage Plugin |

---

## 📄 Licencia

Este proyecto es Software Libre publicado bajo los términos de la **Licencia Pública General de GNU versión 3.0 (GNU GPL v3)**. Tienes la libertad de ejecutarlo, estudiar cómo funciona, adaptarlo a tus necesidades y redistribuirlo. Consulta el archivo [LICENSE.txt](file:///Users/ferlagod/Desktop/BiblioHouse/BiblioHouse/BiblioHouse/LICENSE.txt) para más información.

---

🌐 **Web Oficial:** [ferlagod.eu](https://ferlagod.eu)  
☕ **Donaciones y Apoyo:** [![Apóyame en Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/ferlagod./)

*Desarrollado con ❤️ y pasión por la lectura por [Fernando Lago Dávila (ferlagod)](https://github.com/ferlagod).*
