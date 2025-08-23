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

        // Muestra el scroll arriba del todo **/
        setLocationRelativeTo(parent);
    }

    /**
     * Convierte texto con formato Markdown (negritas y cursivas) a HTML.
     */
    private String markdownToHtml(String markdownText) {
        // Reemplazar **negritas** por <b>negritas</b>
        markdownText = markdownText.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        // Reemplazar *cursivas* por <i>cursivas</i>
        markdownText = markdownToHtmlCursivas(markdownText);
        // Reemplazar saltos de línea por <br>
        markdownText = markdownText.replaceAll("\n", "<br>");
        // Añadir estilo CSS básico para mejorar la legibilidad
        return "<html><head><style>body { font-family: Arial, sans-serif; margin: 10px; }</style></head><body>" + markdownText + "</body></html>";
    }

    /**
     * Método auxiliar para reemplazar *cursivas* por <i>cursivas</i>
     */
    private String markdownToHtmlCursivas(String markdownText) {
        return markdownText.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
    }

        /**
     * Aplica los textos del idioma actual a todos los componentes de la UI.
     */
    private void applyTranslations() {
        setTitle(LanguageManager.getString("help.title"));
        btnClosedAyuda.setText(LanguageManager.getString("button.close"));

        // Construye el texto del manual
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
                .append(LanguageManager.getString("help.manual.section5.language")).append("\n")
                .toString();

        // Convierte el texto Markdown a HTML
        String htmlText = markdownToHtml(textoAyuda);

        // Asigna el texto HTML al JEditorPane
        txtAreaAyuda.setContentType("text/html");
        txtAreaAyuda.setText(htmlText);
        txtAreaAyuda.setCaretPosition(0);
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
        txtAreaAyuda = new javax.swing.JEditorPane();
        btnClosedAyuda = new javax.swing.JButton();
        lblGpl = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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
    private javax.swing.JEditorPane txtAreaAyuda;
    // End of variables declaration//GEN-END:variables
}
