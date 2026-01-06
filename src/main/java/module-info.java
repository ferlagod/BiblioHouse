module com.ferlagod.bibliohousefx {
    // 1. Módulos básicos de JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.web;

    // 2. Módulos estándar de Java
    requires java.net.http;
    requires java.logging;
    requires java.prefs;
    requires java.desktop;

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

    // 4. Configuración de permisos (EXPORTS y OPENS)
    // Permite a JavaFX pintar tu ventana principal (si no, no se ve nada)
    opens com.ferlagod.bibliohousefx to javafx.fxml;

    exports com.ferlagod.bibliohousefx;

    // Permite a GSON meter las narices en la carpeta logic para guardar/leer tus
    // datos
    opens com.bibliohouse.logic to com.google.gson;

    exports com.bibliohouse.logic;
}