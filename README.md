# ![BiblioHouse Logo](https://imgur.com/ibLRiqI.png) BiblioHouse

![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue.svg?style=flat-square)
![License: GPL v3](https://img.shields.io/badge/License-GPL_v3-green.svg?style=flat-square)
![Open Source](https://img.shields.io/badge/Open_Source-%E2%9D%A4-red.svg?style=flat-square)

**BiblioHouse** es un gestor de biblioteca personal moderno, rápido y de código abierto desarrollado en Java. Diseñado para amantes de la lectura y coleccionistas que exigen un control absoluto sobre sus datos: **todo se procesa y almacena en local, garantizando tu privacidad y la soberanía digital de tu colección.**

---

## ✨ Características Principales

### 📚 Gestión y Organización Avanzada
* **Gestor de Sagas y Colecciones:** Agrupa tus libros por serie y detecta visualmente las "brechas" (tomos que te faltan) para completar tus colecciones épicas.
* **Lista de Deseos (Wishlist):** Guarda tus futuras lecturas en un panel visual e independiente para no alterar las estadísticas de tu biblioteca. Muévelos a tu estantería con un solo clic cuando los consigas.
* **Importación CSV Inteligente:** ¿Vienes de otra red social? Importa tu biblioteca de golpe desde **Goodreads** o **Bookwyrm**. El sistema leerá automáticamente tu calificación por estrellas y tu estado de lectura (Leído, Pendiente, Leyendo).
* **Control Total:** CRUD de libros, detección de duplicados y asignación a estanterías físicas.

### ☁️ Privacidad, Nube y Modo Offline
* **Soberanía de Datos:** Toda tu información se guarda en archivos `.json` locales. Nadie rastrea lo que lees.
* **Sincronización Nextcloud:** Sube, descarga y mantén tu base de datos sincronizada automáticamente en tu propia nube privada a través de WebDAV.
* **Portadas 100% Offline:** Las imágenes de los libros se descargan y "secuestran" en tu equipo. Tu biblioteca lucirá perfecta incluso sin conexión a internet.

### 🛠️ Herramientas para el Lector Empedernido
* **Buscador Multi-proveedor:** Integración simultánea y concurrente con **OpenLibrary, Google Books e Inventaire** para autocompletar los metadatos y la portada del libro buscando por título o ISBN.
* **Escáner en Ráfaga:** Importa decenas de libros seguidos usando tu cámara web para leer los códigos de barras.
* **Gestión de Préstamos:** Controla a qué amigos (socios) les prestas tus ejemplares y genera carnets de biblioteca en PDF.
* **Usabilidad Avanzada:** Soporte para arrastrar y soltar (*Drag & Drop*) de portadas, navegación rápida por teclado y alertas de interfaz integradas.
* **Multi-idioma y Multiplataforma:** Traducido a Español, Inglés, Català, Galego, Euskara y Português. Optimizado para Windows, macOS y Linux (Ubuntu).

## 📷 Capturas de Pantalla

| Inicio | Pantalla Principal|
|:---:|:---:|
| ![Imgur](https://i.postimg.cc/KYXHF8sD/Captura-de-pantalla-2026-05-02-a-las-14-09-32.png) | ![Imgur](https://i.postimg.cc/TP84G3Hz/Captura-de-pantalla-2026-05-02-a-las-14-09-47.png) |

| Visor de Libros | Edición |
|:---:|:---:|
| ![Imgur](https://i.postimg.cc/ZqkMZ5DX/Captura-de-pantalla-2026-05-02-a-las-14-10-19.png) | ![Imgur](https://i.postimg.cc/QMLysdnP/Captura-de-pantalla-2026-05-02-a-las-14-10-41.png) |

| Configuración | Manual de Usuario|
|:---:|:---:|
| ![Imgur](https://i.postimg.cc/BvWVsnwd/Captura-de-pantalla-2026-05-06-a-las-15-19-44.png) | ![Imgur](https://i.postimg.cc/d0MxvVxt/Captura-de-pantalla-2026-05-06-a-las-15-45-09.png)|

*(Nota: Capturas de las nuevas vistas de Lista de Deseos y Gestor de Sagas en camino)*

## 🚀 Instalación y Arranque Rápido

### Requisitos previos
* [JDK 21](https://adoptium.net/) o superior.
* [Maven 3.8+](https://maven.apache.org/) instalado en el sistema.

### Compilar y Ejecutar

1. Clona este repositorio en tu máquina local:
   ```bash
   git clone https://forjalibre.eu/ferlagod/BiblioHouse.git
   cd BiblioHouse
   ```
2. Descarga las dependencias y compila el proyecto usando Maven:
   ```bash
   mvn clean install -DskipTests
   ```
3. Ejecuta la aplicación:
   ```bash
   mvn javafx:run
   ```

## 🛠️ Tecnologías Utilizadas

* **Lógica Core:** Java 21
* **Interfaz Gráfica:** JavaFX 21 + FXML
* **Gestor de Dependencias:** Maven
* **Persistencia de Datos:** Gson (Google)
* **Generación de Documentos:** Apache PDFBox

## 📄 Licencia

Este proyecto es Software Libre y se distribuye bajo la Licencia **GNU General Public License v3.0**. Eres libre de usarlo, estudiarlo, modificarlo y compartirlo. Consulta el archivo `LICENSE.txt` para más detalles.

---
🌐 **Web Oficial:** [bibliohouse.org](https://bibliohouse.org)  

[![Apóyame en Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/ferlagod./)

*Desarrollado con ❤️ y mucho café por [ferlagod](https://github.com/ferlagod).*

