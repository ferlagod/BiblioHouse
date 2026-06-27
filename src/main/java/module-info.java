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
module com.ferlagod.bibliohousefx {
    // 1. Módulos básicos de JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.web;
    requires atlantafx.base;
    requires org.controlsfx.controls;

    // 2. Módulos estándar de Java
    requires java.net.http;
    requires java.logging;
    requires java.prefs;
    requires java.desktop;
    requires java.xml;

    // 3. Librerías externas
    requires org.json;
    requires com.google.gson;
    requires java.base;
    requires org.apache.pdfbox;

    // Webcam & Barcode
    requires opencv;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires javafx.swing;

    // NextCloud WebDAV sync
    requires com.github.sardine;

    // BCrypt para hashing de contraseñas (SEC-01)
    requires jbcrypt;

    // 4. Configuración de permisos (EXPORTS y OPENS)
    opens com.ferlagod.bibliohousefx to javafx.fxml;

    exports com.ferlagod.bibliohousefx;

    // Permite a GSON meter las narices en la carpeta logic para guardar/leer tus
    // datos
    opens com.bibliohouse.logic to com.google.gson;

    exports com.bibliohouse.logic;
    exports com.bibliohouse.utils;
    
}
