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

import com.bibliohouse.logic.Libro;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 * Muestra una vista detallada y de solo lectura de un libro.
 *
 * @author ferlagod
 * @version 1.0
 */
public class VentanaDetalleLibro extends JDialog {

    public VentanaDetalleLibro(java.awt.Frame parent, boolean modal, Libro libro) {
        super(parent, modal);
        initComponents(libro);
        if (parent != null && parent.getIconImage() != null) {
            setIconImage(parent.getIconImage());
        }
        setTitle("Detalle del Libro - BiblioHouse");
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents(Libro libro) {
        // --- Panel Principal con Bordes ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Panel Izquierdo: Portada ---
        JPanel panelPortada = new JPanel(new BorderLayout());
        JLabel lblPortada = new JLabel();
        lblPortada.setPreferredSize(new Dimension(200, 300));
        lblPortada.setHorizontalAlignment(JLabel.CENTER);
        lblPortada.setBorder(BorderFactory.createEtchedBorder());

        if (libro.getPortadaURL() != null && !libro.getPortadaURL().isEmpty() && new File(libro.getPortadaURL()).exists()) {
            ImageIcon icono = new ImageIcon(libro.getPortadaURL());
            Image img = icono.getImage().getScaledInstance(200, 300, Image.SCALE_SMOOTH);
            lblPortada.setIcon(new ImageIcon(img));
        } else {
            lblPortada.setText("Sin Portada");
        }
        panelPortada.add(lblPortada, BorderLayout.CENTER);

        // --- Panel Derecho: Detalles del Libro ---
        JPanel panelDetalles = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel lblTitulo = new JLabel(libro.getTitulo());
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panelDetalles.add(lblTitulo, gbc);

        // Autor
        gbc.gridy++;
        JLabel lblAutor = new JLabel(libro.getAutor());
        lblAutor.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        panelDetalles.add(lblAutor, gbc);

        // Separador
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelDetalles.add(new JSeparator(), gbc);

        // Datos en pares (Etiqueta, Valor)
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        addDetalle(panelDetalles, gbc, "Año:", libro.getAño());
        addDetalle(panelDetalles, gbc, "Editorial:", libro.getEditorial());
        addDetalle(panelDetalles, gbc, "Género:", libro.getGenero());
        addDetalle(panelDetalles, gbc, "ISBN:", libro.getIsbn());

        // Calificación con Estrellas
        gbc.gridy++;
        addDetalle(panelDetalles, gbc, "Calificación:", getRatingAsStars(libro.getCalificacion()));

        // Reseña
        gbc.gridy++;
        JLabel lblHeaderResena = new JLabel("Reseña Personal:");
        lblHeaderResena.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelDetalles.add(lblHeaderResena, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JTextArea txtResena = new JTextArea(libro.getReseña());
        txtResena.setEditable(false);
        txtResena.setLineWrap(true);
        txtResena.setWrapStyleWord(true);
        txtResena.setBackground(getBackground()); // Para que parezca una etiqueta
        panelDetalles.add(new JScrollPane(txtResena), gbc);

        // --- Panel Inferior: Botón de Cerrar ---
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        panelBoton.add(btnCerrar);

        // --- Ensamblar todo ---
        panelPrincipal.add(panelPortada, BorderLayout.WEST);
        panelPrincipal.add(panelDetalles, BorderLayout.CENTER);
        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
    }

    private void addDetalle(JPanel panel, GridBagConstraints gbc, String etiqueta, String valor) {
        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(lblEtiqueta, gbc);

        JLabel lblValor = new JLabel(valor);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(lblValor, gbc);

        gbc.gridy++;
    }

    private String getRatingAsStars(int rating) {
        if (rating <= 0) {
            return "Sin calificar";
        }
        String stars = "★".repeat(rating);
        String emptyStars = "☆".repeat(5 - rating);
        return stars + emptyStars;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
