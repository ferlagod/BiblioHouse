/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2025 ferlagod
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
package com.bibliohouse.ui;

import com.bibliohouse.logic.LanguageManager;

/**
 * JDialog que muestra un manual de usuario básico para la aplicación
 * BiblioHouse.
 *
 * @author ferlagod
 */
public class VentanaAyuda extends javax.swing.JDialog {

    /**
     * Crea una nueva instancia de VentanaAyuda.
     *
     * @param parent El JFrame padre desde el cual se lanza este diálogo.
     * @param modal Indica si el diálogo debe ser modal.
     */
    public VentanaAyuda(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        applyTranslations();

        setResizable(false);
        setTitle("Ayuda - BiblioHouse");

        // Texto del Manual de Usuario
        String textoAyuda = """
   Bienvenido a BiblioHouse - Manual de Usuario
        ============================================
        
        1. AÑADIR UN LIBRO
        --------------------
        - **Manualmente**: En la primera pestaña, rellena los campos del formulario y pulsa "Guardar Libro". Puedes añadir una imagen de portada con el botón "Seleccionar Imagen".
        - **Desde OpenLibrary**: Escribe un título o ISBN en el campo de búsqueda de OpenLibrary y pulsa "Buscar" (o la tecla Intro). Selecciona un libro de la tabla de resultados y pulsa "Añadir Libro Seleccionado". La información principal, incluida la portada, se descargará y guardará automáticamente.
        
        
        2. GESTIONAR MI BIBLIOTECA
        --------------------------
        - **Ver tu colección**: La pestaña "Mi Biblioteca" muestra todos los libros que has añadido.
        - **Buscar en tu colección**: Para filtrar tu biblioteca, selecciona un criterio (Título, Autor, etc.), escribe un término en el campo de texto y pulsa "Buscar". Para ver de nuevo toda la biblioteca, borra el campo de búsqueda y pulsa "Buscar".
        - **Ver detalles del libro**: Haz **doble clic** en cualquier libro de la tabla para abrir una ventana con toda su información detallada, incluyendo la portada en grande y tu reseña personal.
        - **Editar un libro**: Selecciona un libro de la tabla y pulsa "Editar Libro Seleccionado". Se abrirá una ventana donde podrás modificar todos sus datos, incluyendo:
            - **Calificación**: Puntúa el libro usando el menú desplegable de estrellas.
            - **Reseña**: Escribe tus notas y opiniones personales sobre el libro.
        - **Eliminar un libro**: Selecciona un libro y pulsa "Eliminar Libro Seleccionado" para borrarlo permanentemente (se pedirá confirmación).
        
        
        3. GESTIONAR PRÉSTAMOS
        ----------------------
        - **Prestar un libro**: En la pestaña "Préstamos", selecciona un libro disponible en el menú desplegable, escribe el nombre de la persona a quien se lo prestas y pulsa "Prestar".
        - **Marcar como devuelto**: Selecciona un préstamo de la tabla y pulsa "Marcar como Devuelto". La fecha de devolución se registrará automáticamente.
        - **Buscar préstamos**: Usa la barra de búsqueda de esta pestaña para filtrar los préstamos por el título del libro o por el nombre de la persona.
        
        
        4. MENÚ ARCHIVO
        ---------------
        - **Importar base de datos**: Te permite reemplazar tu biblioteca actual con un archivo `biblioteca.xml` guardado previamente.
        - **Exportar base de datos**: Guarda una copia de seguridad de tu `biblioteca.xml` en la ubicación que elijas.
        - **Salir**: Cierra la aplicación.
        
        
        5. MENÚ HERRAMIENTAS
        --------------------
        - **Buscar duplicados**: Analiza tu biblioteca y te muestra una lista de libros que podrían estar duplicados.
        - **Configuración**: Te permite:
            - Elegir una carpeta por defecto para tus exportaciones.
            - Cambiar el tema visual de la aplicación entre un **modo claro** y un **modo oscuro**. El cambio se aplica al instante.
        """;

        txtAreaAyuda.setText(textoAyuda);
        txtAreaAyuda.setCaretPosition(0); // Muestra el scroll arriba del todo

        setLocationRelativeTo(parent);
    }

    /**
     * Aplica los textos del idioma actual a todos los componentes de la UI.
     */
    private void applyTranslations() {
        setTitle(LanguageManager.getString("help.title"));
        btnClosedAyuda.setText(LanguageManager.getString("button.close"));

        // Construimos el texto del manual a partir de las claves de idioma
        String textoAyuda = new StringBuilder()
                .append(LanguageManager.getString("help.manual.welcome")).append("\n")
                .append(LanguageManager.getString("help.manual.header")).append("\n")
                .append(LanguageManager.getString("help.manual.section1.title")).append("\n")
                .append(LanguageManager.getString("help.manual.section1.subtitle")).append("\n")
                .append(LanguageManager.getString("help.manual.section1.manual")).append("\n")
                .append(LanguageManager.getString("help.manual.section1.openlibrary")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.title")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.subtitle")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.view")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.search")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.details")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.edit")).append("\n")
                .append(LanguageManager.getString("help.manual.section2.delete")).append("\n")
                .append(LanguageManager.getString("help.manual.section3.title")).append("\n")
                .append(LanguageManager.getString("help.manual.section3.subtitle")).append("\n")
                .append(LanguageManager.getString("help.manual.section3.loan")).append("\n")
                .append(LanguageManager.getString("help.manual.section3.return")).append("\n")
                .append(LanguageManager.getString("help.manual.section3.search")).append("\n")
                .append(LanguageManager.getString("help.manual.section4.title")).append("\n")
                .append(LanguageManager.getString("help.manual.section4.subtitle")).append("\n")
                .append(LanguageManager.getString("help.manual.section4.import")).append("\n")
                .append(LanguageManager.getString("help.manual.section4.export")).append("\n")
                .append(LanguageManager.getString("help.manual.section4.exit")).append("\n")
                .append(LanguageManager.getString("help.manual.section5.title")).append("\n")
                .append(LanguageManager.getString("help.manual.section5.subtitle")).append("\n")
                .append(LanguageManager.getString("help.manual.section5.duplicates")).append("\n")
                .append(LanguageManager.getString("help.manual.section5.settings")).append("\n")
                .toString();

        txtAreaAyuda.setText(textoAyuda);
        txtAreaAyuda.setCaretPosition(0); // Muestra el scroll arriba del todo
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaAyuda = new javax.swing.JTextArea();
        btnClosedAyuda = new javax.swing.JButton();
        lblGpl = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        txtAreaAyuda.setEditable(false);
        txtAreaAyuda.setColumns(20);
        txtAreaAyuda.setRows(5);
        jScrollPane1.setViewportView(txtAreaAyuda);

        btnClosedAyuda.setText("Cerrar");
        btnClosedAyuda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClosedAyudaActionPerformed(evt);
            }
        });

        lblGpl.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/gplv3.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 841, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblGpl, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClosedAyuda)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 365, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnClosedAyuda)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblGpl, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)))
                .addGap(0, 12, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Cierra el diálogo cuando se hace clic en el botón "Cerrar".
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnClosedAyudaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClosedAyudaActionPerformed
        dispose();
    }//GEN-LAST:event_btnClosedAyudaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClosedAyuda;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblGpl;
    private javax.swing.JTextArea txtAreaAyuda;
    // End of variables declaration//GEN-END:variables
}
