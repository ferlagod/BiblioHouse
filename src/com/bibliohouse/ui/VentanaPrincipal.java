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
import com.bibliohouse.logic.OpenLibraryClient;
import com.bibliohouse.logic.Prestamo;
import com.bibliohouse.logic.XMLManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Clase principal de la interfaz gráfica de la aplicación BiblioHouse. Esta
 * clase gestiona la ventana principal de la aplicación y sus funcionalidades.
 *
 * @author ferlagod
 * @version 0.4
 */
public class VentanaPrincipal extends javax.swing.JFrame {

    private final Preferences prefs;
    private static final String EXPORT_PATH_KEY = "exportPath";

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName());

    private final XMLManager xmlManager;
    private List<Libro> listaDeLibros;
    private List<Libro> resultadosBusquedaActual; // Para guardar los resultados de OpenLibrary
    private String rutaImagenSeleccionada;
    private final OpenLibraryClient openLibraryClient;
    private final List<Prestamo> listaDePrestamos;

    /**
     * Crea una nueva instancia de VentanaPrincipal. Inicializa los componentes
     * de la interfaz gráfica y configura la ventana principal.
     */
    public VentanaPrincipal() {
        initComponents();
        setTitle("BiblioHouse");
        establecerIcono();

        // Formatear el JSpinner para que no use separador de miles
        javax.swing.JSpinner.NumberEditor editor = new javax.swing.JSpinner.NumberEditor(spinnerAnio, "#");
        spinnerAnio.setEditor(editor);

        // --- Inicialización de los componentes de lógica ---
        xmlManager = new XMLManager();
        openLibraryClient = new OpenLibraryClient();
        listaDeLibros = xmlManager.cargarLibros();
        resultadosBusquedaActual = new ArrayList<>();

        // --- Preparación de las tablas y componentes ---
        prepararTablaResultados();
        prepararComponentesBusquedaLocal();
        actualizarTablaMiBiblioteca(listaDeLibros); // Carga inicial de libros

        this.listaDePrestamos = xmlManager.cargarPrestamos();
        prepararComponentesPrestamos();

        // Inicializa las preferencias
        prefs = Preferences.userNodeForPackage(VentanaPrincipal.class);

        setLocationRelativeTo(null);
        actualizarTodaLaUI();
    }

    /**
     * Establece el icono de la aplicación cargándolo desde un archivo de
     * recursos. Este método intenta cargar el icono de la aplicación desde la
     * ruta especificada "/com/bibliohouse/resources/icono.png".
     */
    private void establecerIcono() {
        try {
            java.net.URL iconURL = getClass().getResource("/com/bibliohouse/resources/icono.png");
            if (iconURL != null) {
                setIconImage(new javax.swing.ImageIcon(iconURL).getImage());
            } else {
                LOGGER.warning("No se pudo encontrar el archivo del icono.");
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Error al cargar el icono de la aplicación.", e);
        }
    }

    /**
     * Actualiza todos los componentes de la interfaz de usuario. Este método se
     * utiliza para sincronizar la interfaz de usuario con el estado actual de
     * los datos.
     */
    private void actualizarTodaLaUI() {
        LOGGER.info("Actualizando todos los componentes de la interfaz...");
        // 1. Actualiza la tabla de la biblioteca principal
        actualizarTablaMiBiblioteca(this.listaDeLibros);
        // 2. Actualiza toda la pestaña de préstamos (tabla y combo box)
        actualizarPestanaPrestamos();
    }

    /**
     * Inicializa y configura los componentes de la pestaña de préstamos.
     * Configura la tabla de préstamos, el combo box de criterios de búsqueda y
     * carga los datos iniciales.
     */
    private void prepararComponentesPrestamos() {
        // Configurar la tabla de préstamos
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Libro", "Prestado a", "Fecha Préstamo", "Fecha Devolución"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblPrestamos.setModel(model);

        // Configurar el JComboBox de búsqueda de préstamos 
        cmbCriterioBusquedaPrestamo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
            "Nombre del Libro", "Nombre de Persona"
        }));

        // Cargar los datos iniciales
        actualizarPestanaPrestamos();
    }

    /**
     * Actualiza los componentes de la pestaña de préstamos en la interfaz de
     * usuario. Este método se encarga de refrescar la tabla de préstamos con la
     * lista actual de préstamos y actualizar el ComboBox con los libros
     * disponibles para préstamo.
     */
    private void actualizarPestanaPrestamos() {
        // 1. Actualizar la tabla de préstamos
        actualizarTablaPrestamos(this.listaDePrestamos);

        // 2. Actualizar el ComboBox con los libros disponibles para prestar
        actualizarLibrosDisponibles();
    }

    /**
     * Actualiza la tabla de préstamos en la interfaz de usuario con la lista
     * proporcionada de préstamos. Este método limpia la tabla existente y añade
     * cada préstamo de la lista como una nueva fila en la tabla.
     *
     * @param prestamos La lista de préstamos a mostrar en la tabla.
     */
    private void actualizarTablaPrestamos(List<Prestamo> prestamos) {
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblPrestamos.getModel();
        model.setRowCount(0); // Limpiar tabla

        for (Prestamo p : prestamos) {
            model.addRow(new Object[]{
                p.getTituloLibro(),
                p.getNombrePersona(),
                p.getFechaPrestamoFormateada(),
                p.getFechaDevolucionFormateada()
            });
        }
    }

    /**
     * Actualiza el ComboBox de libros disponibles para mostrar solo los libros
     * que no están actualmente prestados. Este método primero limpia todos los
     * elementos actuales del ComboBox. Luego, filtra la lista de préstamos para
     * obtener los ISBNs de los libros que están prestados y no han sido
     * devueltos. Finalmente, añade al ComboBox solo aquellos libros cuya ISBN
     * no está en la lista de libros prestados.
     */
    private void actualizarLibrosDisponibles() {
        Libro seleccionActual = (Libro) cmbLibrosDisponibles.getSelectedItem();
        cmbLibrosDisponibles.removeAllItems(); // Limpiar ComboBox

        // Obtener la lista de ISBN de libros que ya están prestados y no devueltos
        List<String> isbnsPrestados = listaDePrestamos.stream()
                .filter(p -> !p.isDevuelto())
                .map(Prestamo::getIsbnLibro)
                .collect(Collectors.toList());

        // Añadir solo los libros que NO están en la lista de prestados
        for (Libro libro : listaDeLibros) {
            if (!isbnsPrestados.contains(libro.getIsbn())) {
                cmbLibrosDisponibles.addItem(libro); // Guardamos el objeto entero
            }
        }
        cmbLibrosDisponibles.setSelectedItem(seleccionActual);
    }

    /**
     * Comprueba si un libro ya existe en la biblioteca.
     *
     * @param libro El libro a verificar.
     * @return true si el libro ya existe, false en caso contrario.
     */
    private boolean existeLibro(Libro libro) {
        for (Libro libroExistente : listaDeLibros) {
            if (libroExistente.getTitulo().equalsIgnoreCase(libro.getTitulo())
                    && libroExistente.getAutor().equalsIgnoreCase(libro.getAutor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        addBookPane = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        lblAutor = new javax.swing.JLabel();
        lblEditorial = new javax.swing.JLabel();
        lblAnio = new javax.swing.JLabel();
        lblGenero = new javax.swing.JLabel();
        lblIsbn = new javax.swing.JLabel();
        btnGuardarManual = new javax.swing.JButton();
        txtTitulo = new javax.swing.JTextField();
        txtAutor = new javax.swing.JTextField();
        txtEditorial = new javax.swing.JTextField();
        txtGenero = new javax.swing.JTextField();
        lblPortadaPreview = new javax.swing.JLabel();
        txtIsbn = new javax.swing.JTextField();
        spinnerAnio = new javax.swing.JSpinner();
        btnSeleccionarImagen = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        lblBusquedaOpenLibrary = new javax.swing.JLabel();
        txtBusquedaOpenLibrary = new javax.swing.JTextField();
        btnBuscarOpenLibrary = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblResultadosBusqueda = new javax.swing.JTable();
        btnAnadirSeleccionado = new javax.swing.JButton();
        btnClosed = new javax.swing.JButton();
        searchBookPane = new javax.swing.JPanel();
        lblCriterioBusqueda = new javax.swing.JLabel();
        cmbCriterioBusqueda = new javax.swing.JComboBox<>();
        txtBusquedaLocal = new javax.swing.JTextField();
        btnBuscarLocal = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblMiBiblioteca = new javax.swing.JTable();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnClosed1 = new javax.swing.JButton();
        Prestamos = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cmbLibrosDisponibles = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        txtNombrePersona = new javax.swing.JTextField();
        btnPrestar = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblPrestamos = new javax.swing.JTable();
        btnDevolver = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        cmbCriterioBusquedaPrestamo = new javax.swing.JComboBox<>();
        txtBusquedaPrestamo = new javax.swing.JTextField();
        btnClosedPrestamo = new javax.swing.JButton();
        btnBuscarPrestamo = new javax.swing.JButton();
        barraMenu = new javax.swing.JMenuBar();
        menuArchivo = new javax.swing.JMenu();
        menuItemImportar = new javax.swing.JMenuItem();
        menuItemExportar = new javax.swing.JMenuItem();
        menuItemSalir = new javax.swing.JMenuItem();
        menuHerramuentas = new javax.swing.JMenu();
        menuItemBuscarDuplicados = new javax.swing.JMenuItem();
        menuItemConfiguracion = new javax.swing.JMenuItem();
        menuAyuda = new javax.swing.JMenu();
        menuItemAcercaDe = new javax.swing.JMenuItem();
        menuItemManual = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lblTitulo.setText("Título:");

        lblAutor.setText("Autor:");

        lblEditorial.setText("Editorial:");

        lblAnio.setText("Año:");

        lblGenero.setText("Género:");

        lblIsbn.setText("ISBN:");

        btnGuardarManual.setText("Guardar libro");
        btnGuardarManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarManualActionPerformed(evt);
            }
        });

        lblPortadaPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lblPortadaPreview.setPreferredSize(new java.awt.Dimension(150, 200));

        btnSeleccionarImagen.setText("Seleccionar Imagen...");
        btnSeleccionarImagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSeleccionarImagenActionPerformed(evt);
            }
        });

        lblBusquedaOpenLibrary.setText("Buscar en OpenLibrary (por título o ISBN):");

        txtBusquedaOpenLibrary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBusquedaOpenLibraryActionPerformed(evt);
            }
        });

        btnBuscarOpenLibrary.setText("Buscar");
        btnBuscarOpenLibrary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarOpenLibraryActionPerformed(evt);
            }
        });

        tblResultadosBusqueda.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Titulo", "Autor", "Año"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblResultadosBusqueda.getTableHeader().setResizingAllowed(false);
        jScrollPane1.setViewportView(tblResultadosBusqueda);
        if (tblResultadosBusqueda.getColumnModel().getColumnCount() > 0) {
            tblResultadosBusqueda.getColumnModel().getColumn(0).setResizable(false);
            tblResultadosBusqueda.getColumnModel().getColumn(1).setResizable(false);
            tblResultadosBusqueda.getColumnModel().getColumn(2).setResizable(false);
        }

        btnAnadirSeleccionado.setText("Añadir Libro Seleccionado");
        btnAnadirSeleccionado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnadirSeleccionadoActionPerformed(evt);
            }
        });

        btnClosed.setText("Cerrar");
        btnClosed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClosedActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addBookPaneLayout = new javax.swing.GroupLayout(addBookPane);
        addBookPane.setLayout(addBookPaneLayout);
        addBookPaneLayout.setHorizontalGroup(
            addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(addBookPaneLayout.createSequentialGroup()
                .addGap(52, 52, 52)
                .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(addBookPaneLayout.createSequentialGroup()
                        .addComponent(btnClosed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAnadirSeleccionado))
                    .addComponent(jScrollPane1)
                    .addGroup(addBookPaneLayout.createSequentialGroup()
                        .addComponent(lblBusquedaOpenLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtBusquedaOpenLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, 335, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41)
                        .addComponent(btnBuscarOpenLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(addBookPaneLayout.createSequentialGroup()
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblAnio)
                            .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(lblGenero, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblEditorial, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblAutor, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblTitulo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(33, 33, 33)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, addBookPaneLayout.createSequentialGroup()
                                .addComponent(spinnerAnio, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblIsbn)
                                .addGap(18, 18, 18)
                                .addComponent(txtIsbn))
                            .addComponent(txtGenero, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtEditorial, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtAutor, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtTitulo, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(130, 130, 130)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnSeleccionarImagen)
                            .addComponent(lblPortadaPreview, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGuardarManual))
                        .addGap(4, 4, 4)))
                .addGap(50, 50, 50))
        );
        addBookPaneLayout.setVerticalGroup(
            addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addBookPaneLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(addBookPaneLayout.createSequentialGroup()
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblTitulo)
                            .addComponent(txtTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAutor)
                            .addComponent(txtAutor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblEditorial)
                            .addComponent(txtEditorial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblGenero)
                            .addComponent(txtGenero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAnio)
                            .addComponent(spinnerAnio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblIsbn)
                            .addComponent(txtIsbn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(addBookPaneLayout.createSequentialGroup()
                        .addComponent(lblPortadaPreview, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSeleccionarImagen)
                        .addGap(18, 18, 18)
                        .addComponent(btnGuardarManual)))
                .addGap(44, 44, 44)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBusquedaOpenLibrary)
                    .addComponent(txtBusquedaOpenLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscarOpenLibrary))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(addBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAnadirSeleccionado)
                    .addComponent(btnClosed))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Añadir Libro", addBookPane);

        lblCriterioBusqueda.setText("Buscar por:");

        cmbCriterioBusqueda.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        txtBusquedaLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBusquedaLocalActionPerformed(evt);
            }
        });

        btnBuscarLocal.setText("Buscar");
        btnBuscarLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarLocalActionPerformed(evt);
            }
        });

        tblMiBiblioteca.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(tblMiBiblioteca);

        btnEditar.setText("Editar Libro Seleccionado");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btnEliminar.setText("Eliminar Libro Seleccionado");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnClosed1.setText("Cerrar");
        btnClosed1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClosed1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout searchBookPaneLayout = new javax.swing.GroupLayout(searchBookPane);
        searchBookPane.setLayout(searchBookPaneLayout);
        searchBookPaneLayout.setHorizontalGroup(
            searchBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchBookPaneLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(searchBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchBookPaneLayout.createSequentialGroup()
                        .addComponent(btnEditar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEliminar))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchBookPaneLayout.createSequentialGroup()
                        .addComponent(lblCriterioBusqueda)
                        .addGap(46, 46, 46)
                        .addComponent(cmbCriterioBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48)
                        .addComponent(txtBusquedaLocal, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnBuscarLocal)))
                .addGap(30, 30, 30))
            .addGroup(searchBookPaneLayout.createSequentialGroup()
                .addGap(385, 385, 385)
                .addComponent(btnClosed1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        searchBookPaneLayout.setVerticalGroup(
            searchBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchBookPaneLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(searchBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCriterioBusqueda)
                    .addComponent(cmbCriterioBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBusquedaLocal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscarLocal))
                .addGap(30, 30, 30)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addGroup(searchBookPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEditar)
                    .addComponent(btnEliminar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 101, Short.MAX_VALUE)
                .addComponent(btnClosed1)
                .addGap(52, 52, 52))
        );

        jTabbedPane1.addTab("Buscar en mi Biblioteca", searchBookPane);

        jLabel1.setText("Prestar libro:");

        jLabel2.setText("a:");

        btnPrestar.setText("Prestar");
        btnPrestar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrestarActionPerformed(evt);
            }
        });

        tblPrestamos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(tblPrestamos);

        btnDevolver.setText("Marcar como Devuelto");
        btnDevolver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDevolverActionPerformed(evt);
            }
        });

        jLabel3.setText("Buscar por:");

        cmbCriterioBusquedaPrestamo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbCriterioBusquedaPrestamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbCriterioBusquedaPrestamoActionPerformed(evt);
            }
        });

        txtBusquedaPrestamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBusquedaPrestamoActionPerformed(evt);
            }
        });

        btnClosedPrestamo.setText("Cerrar");
        btnClosedPrestamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClosedPrestamoActionPerformed(evt);
            }
        });

        btnBuscarPrestamo.setText("Buscar préstamo");
        btnBuscarPrestamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarPrestamoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PrestamosLayout = new javax.swing.GroupLayout(Prestamos);
        Prestamos.setLayout(PrestamosLayout);
        PrestamosLayout.setHorizontalGroup(
            PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PrestamosLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnClosedPrestamo)
                    .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(btnBuscarPrestamo)
                        .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDevolver)
                            .addComponent(btnPrestar, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PrestamosLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addComponent(cmbLibrosDisponibles, 0, 289, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(txtNombrePersona, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE))
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator3)
                            .addGroup(PrestamosLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(cmbCriterioBusquedaPrestamo, 0, 296, Short.MAX_VALUE)
                                .addGap(56, 56, 56)
                                .addComponent(txtBusquedaPrestamo, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)))))
                .addGap(69, 69, 69))
        );
        PrestamosLayout.setVerticalGroup(
            PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PrestamosLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cmbLibrosDisponibles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(txtNombrePersona, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnPrestar)
                .addGap(46, 46, 46)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnDevolver)
                .addGap(18, 18, 18)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(PrestamosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cmbCriterioBusquedaPrestamo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBusquedaPrestamo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(30, 30, 30)
                .addComponent(btnBuscarPrestamo)
                .addGap(34, 34, 34)
                .addComponent(btnClosedPrestamo)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Préstamos", Prestamos);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        menuArchivo.setText("Archivo");

        menuItemImportar.setText("Importar base de datos...");
        menuItemImportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemImportarActionPerformed(evt);
            }
        });
        menuArchivo.add(menuItemImportar);

        menuItemExportar.setText("Exportar");
        menuItemExportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemExportarActionPerformed(evt);
            }
        });
        menuArchivo.add(menuItemExportar);

        menuItemSalir.setText("Salir");
        menuItemSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemSalirActionPerformed(evt);
            }
        });
        menuArchivo.add(menuItemSalir);

        barraMenu.add(menuArchivo);

        menuHerramuentas.setText("Herramientas");

        menuItemBuscarDuplicados.setText("Buscar duplicados...");
        menuItemBuscarDuplicados.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemBuscarDuplicadosActionPerformed(evt);
            }
        });
        menuHerramuentas.add(menuItemBuscarDuplicados);

        menuItemConfiguracion.setText("Configuración...");
        menuItemConfiguracion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemConfiguracionActionPerformed(evt);
            }
        });
        menuHerramuentas.add(menuItemConfiguracion);

        barraMenu.add(menuHerramuentas);

        menuAyuda.setText("Ayuda");
        menuAyuda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAyudaActionPerformed(evt);
            }
        });

        menuItemAcercaDe.setText("Acerca de BiblioHouse...");
        menuItemAcercaDe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAcercaDeActionPerformed(evt);
            }
        });
        menuAyuda.add(menuItemAcercaDe);

        menuItemManual.setText("Manual de Usuario...");
        menuItemManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemManualActionPerformed(evt);
            }
        });
        menuAyuda.add(menuItemManual);

        barraMenu.add(menuAyuda);

        setJMenuBar(barraMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Maneja la acción de seleccionar una imagen para la portada del libro.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnSeleccionarImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSeleccionarImagenActionPerformed
        // 1. Crear un selector de archivos
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();

        // 2. Filtrar para que solo muestre archivos de imagen
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Imágenes (jpg, png, gif)", "jpg", "png", "gif");
        fileChooser.setFileFilter(filter);

        // 3. Mostrar el diálogo y esperar a que el usuario seleccione un archivo
        int resultado = fileChooser.showOpenDialog(this);

        // 4. Comprobar si el usuario ha seleccionado un archivo y ha pulsado "Aceptar"
        if (resultado == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                // 5. Obtener el archivo seleccionado y guardar su ruta absoluta
                java.io.File archivoSeleccionado = fileChooser.getSelectedFile();
                rutaImagenSeleccionada = archivoSeleccionado.getAbsolutePath();

                // 6. Cargar la imagen en un ImageIcon
                javax.swing.ImageIcon iconoOriginal = new javax.swing.ImageIcon(rutaImagenSeleccionada);

                // 7. Redimensionar la imagen para que se ajuste al tamaño del JLabel
                java.awt.Image imagenOriginal = iconoOriginal.getImage();
                java.awt.Image imagenRedimensionada = imagenOriginal.getScaledInstance(lblPortadaPreview.getWidth(), lblPortadaPreview.getHeight(), java.awt.Image.SCALE_SMOOTH);

                // 8. Crear un nuevo ImageIcon con la imagen redimensionada y ponerlo en el JLabel
                lblPortadaPreview.setIcon(new javax.swing.ImageIcon(imagenRedimensionada));

            } catch (Exception e) {
                // En caso de error, mostrar un mensaje al usuario
                javax.swing.JOptionPane.showMessageDialog(this, "Error al cargar la imagen.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }

    }//GEN-LAST:event_btnSeleccionarImagenActionPerformed

    /**
     * Maneja la acción de guardar un libro manualmente.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnGuardarManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarManualActionPerformed
        // 1. Validar que el título no esté vacío
        String titulo = txtTitulo.getText().trim();
        if (titulo.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "El campo 'Título' es obligatorio.", "Error de validación", javax.swing.JOptionPane.ERROR_MESSAGE);
            return; // Detiene la ejecución si la validación falla
        }

        // 2. Recoger el resto de datos del formulario
        String autor = txtAutor.getText().trim();
        String editorial = txtEditorial.getText().trim();
        String genero = txtGenero.getText().trim();
        String isbn = txtIsbn.getText().trim();

        // Obtener el valor del JSpinner como String
        String anio = spinnerAnio.getValue().toString();

        // 3. Crear el nuevo objeto Libro. La ruta de la imagen ya está guardada en 'rutaImagenSeleccionada'.
        com.bibliohouse.logic.Libro nuevoLibro = new com.bibliohouse.logic.Libro(titulo, autor, editorial, anio, genero, isbn, rutaImagenSeleccionada);

        // 4. Añadir el nuevo libro a nuestra lista en memoria
        if (existeLibro(nuevoLibro)) {
            JOptionPane.showMessageDialog(this, "Este libro ya existe en tu biblioteca.", "Libro Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        listaDeLibros.add(nuevoLibro);

        // 5. Usar el XMLManager para guardar la lista completa y actualizada en el archivo
        xmlManager.guardarLibros(listaDeLibros);

        // 6. Mostrar un mensaje de confirmación al usuario
        JOptionPane.showMessageDialog(this, "Libro guardado y sincronizado con éxito.", "Guardado", JOptionPane.INFORMATION_MESSAGE);

        actualizarTodaLaUI();
        // 7. Limpiar el formulario para poder añadir otro libro
        limpiarFormulario();
    }

    /**
     * Limpia todos los campos del formulario.
     */
    private void limpiarFormulario() {
        txtTitulo.setText("");
        txtAutor.setText("");
        txtEditorial.setText("");
        txtGenero.setText("");
        txtIsbn.setText("");
        spinnerAnio.setValue(java.time.LocalDate.now().getYear()); // Pone el año actual
        lblPortadaPreview.setIcon(null); // Quita la imagen de la vista previa
        rutaImagenSeleccionada = null; // Resetea la ruta de la imagen

    }//GEN-LAST:event_btnGuardarManualActionPerformed

    /**
     * Maneja la acción de buscar libros en OpenLibrary.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnBuscarOpenLibraryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarOpenLibraryActionPerformed
        String termino = txtBusquedaOpenLibrary.getText().trim();
        if (termino.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, introduce un término de búsqueda.", "Aviso", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Para evitar que la interfaz se congele mientras busca en internet,
        // usamos un SwingWorker para realizar la búsqueda en un hilo separado.
        new javax.swing.SwingWorker<java.util.List<com.bibliohouse.logic.Libro>, Void>() {
            @Override
            protected java.util.List<com.bibliohouse.logic.Libro> doInBackground() throws Exception {
                // Esta es la única parte que se ejecuta fuera del hilo principal.
                return openLibraryClient.buscarLibros(termino);
            }

            @Override
            protected void done() {
                try {
                    // Una vez que la búsqueda termina, obtenemos el resultado y actualizamos la tabla.
                    java.util.List<com.bibliohouse.logic.Libro> resultados = get();
                    actualizarTablaResultados(resultados);
                } catch (Exception e) {
                    e.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(null, "Error al realizar la búsqueda.", "Error de Red", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }

        }.execute();
    }//GEN-LAST:event_btnBuscarOpenLibraryActionPerformed

    /**
     * Descarga y guarda la portada de un libro desde una URL.
     *
     * @param urlString La URL de la portada.
     * @param isbn El ISBN del libro.
     * @return La ruta local de la portada guardada.
     */
    private String descargarYGuardarPortada(String urlString, String isbn) {
        if (urlString == null || urlString.isEmpty() || isbn == null || isbn.isEmpty()) {
            return "";
        }

        try {
            File coversDir = new File(System.getProperty("user.home") + File.separator + "BiblioHouse" + File.separator + "covers");
            if (!coversDir.exists()) {
                coversDir.mkdirs();
            }

            String nombreArchivo = isbn.replaceAll("[^a-zA-Z0-9.-]", "_") + ".jpg";
            File archivoDestino = new File(coversDir, nombreArchivo);

            try (InputStream in = new URL(urlString).openStream()) {
                Files.copy(in, archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.log(java.util.logging.Level.INFO, "Portada descargada: {0}", archivoDestino.getAbsolutePath());
                return archivoDestino.getAbsolutePath();
            }
        } catch (IOException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "No se pudo descargar la portada desde " + urlString, e);
            return "";
        }
    }

    /**
     * Maneja la acción de añadir un libro seleccionado desde los resultados de
     * búsqueda.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnAnadirSeleccionadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnadirSeleccionadoActionPerformed
        int filaSeleccionada = tblResultadosBusqueda.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un libro de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtenemos el objeto Libro COMPLETO desde la lista que guardamos, no desde la tabla.
        Libro libroSeleccionado = resultadosBusquedaActual.get(filaSeleccionada);

        if (existeLibro(libroSeleccionado)) {
            JOptionPane.showMessageDialog(this, "Este libro ya existe en tu biblioteca.", "Libro Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String portadaUrlRemota = libroSeleccionado.getPortadaURL();
        String rutaLocalPortada = descargarYGuardarPortada(portadaUrlRemota, libroSeleccionado.getIsbn());
        libroSeleccionado.setPortadaURL(rutaLocalPortada);

        listaDeLibros.add(libroSeleccionado);
        xmlManager.guardarLibros(listaDeLibros);
        actualizarTablaMiBiblioteca(listaDeLibros);

        JOptionPane.showMessageDialog(this, "Libro '" + libroSeleccionado.getTitulo() + "' añadido a tu biblioteca.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        actualizarTodaLaUI();
    }//GEN-LAST:event_btnAnadirSeleccionadoActionPerformed

    /**
     * Configura los componentes de búsqueda local.
     */
    private void prepararComponentesBusquedaLocal() {
        // Rellenamos el JComboBox con las opciones
        cmbCriterioBusqueda.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{
            "Título", "Autor", "Género", "Editorial", "Año", "ISBN"
        }));

        // Preparamos el modelo de la tabla que mostrará nuestros libros
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Título", "Autor", "Editorial", "Año", "Género", "ISBN"}
        ) {
            // Hacemos que las celdas no sean editables
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblMiBiblioteca.setModel(model);
    }

    /**
     * Recibe una lista de libros y la muestra en la tabla de la biblioteca.
     *
     * @param librosAMostrar La lista de libros que se debe mostrar.
     */
    private void actualizarTablaMiBiblioteca(java.util.List<com.bibliohouse.logic.Libro> librosAMostrar) {
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblMiBiblioteca.getModel();
        model.setRowCount(0); // Limpiamos la tabla antes de llenarla

        for (com.bibliohouse.logic.Libro libro : librosAMostrar) {
            model.addRow(new Object[]{
                libro.getTitulo(),
                libro.getAutor(),
                libro.getEditorial(),
                libro.getAño(),
                libro.getGenero(),
                libro.getIsbn()
            });
        }
    }

    /**
     * Maneja la acción de buscar libros en la biblioteca local.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnBuscarLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarLocalActionPerformed
        String terminoBusqueda = txtBusquedaLocal.getText().trim().toLowerCase();

        // Si la búsqueda está vacía, mostramos todos los libros de nuevo
        if (terminoBusqueda.isEmpty()) {
            actualizarTablaMiBiblioteca(listaDeLibros);
            return;
        }

        String criterio = (String) cmbCriterioBusqueda.getSelectedItem();
        java.util.List<com.bibliohouse.logic.Libro> resultados = new java.util.ArrayList<>();

        // Recorremos la lista completa de libros que tenemos en memoria
        for (com.bibliohouse.logic.Libro libro : listaDeLibros) {
            String valorDelCampo = "";

            // Obtenemos el valor del campo correspondiente según el criterio seleccionado
            switch (criterio) {
                case "Título":
                    valorDelCampo = libro.getTitulo();
                    break;
                case "Autor":
                    valorDelCampo = libro.getAutor();
                    break;
                case "Género":
                    valorDelCampo = libro.getGenero();
                    break;
                case "Editorial":
                    valorDelCampo = libro.getEditorial();
                    break;
                case "Año":
                    valorDelCampo = libro.getAño();
                    break;
                case "ISBN":
                    valorDelCampo = libro.getIsbn();
                    break;
            }

            // Si el valor (en minúsculas) contiene el término de búsqueda, lo añadimos a los resultados
            if (valorDelCampo != null && valorDelCampo.toLowerCase().contains(terminoBusqueda)) {
                resultados.add(libro);
            }
        }

        // Actualizamos la tabla para mostrar solo los resultados encontrados
        actualizarTablaMiBiblioteca(resultados);

    }//GEN-LAST:event_btnBuscarLocalActionPerformed

    /**
     * Maneja la acción de eliminar un libro seleccionado.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        // 1. Obtener la fila seleccionada en la tabla
        int filaSeleccionada = tblMiBiblioteca.getSelectedRow();

        // 2. Validar si se ha seleccionado una fila
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, selecciona un libro de la tabla para eliminar.", "Ningún libro seleccionado", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Pedir confirmación al usuario antes de borrar
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(this, "¿Estás seguro de que quieres eliminar este libro de forma permanente?", "Confirmar eliminación", javax.swing.JOptionPane.YES_NO_OPTION);

        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            // 4. Obtener los datos clave de la fila para identificar el libro
            String titulo = (String) tblMiBiblioteca.getValueAt(filaSeleccionada, 0);
            String autor = (String) tblMiBiblioteca.getValueAt(filaSeleccionada, 1);

            // 5. Buscar el libro en la lista principal (listaDeLibros) y eliminarlo
            // Usamos un iterador para evitar problemas al modificar la lista mientras la recorremos.
            java.util.Iterator<com.bibliohouse.logic.Libro> iter = listaDeLibros.iterator();
            while (iter.hasNext()) {
                com.bibliohouse.logic.Libro libro = iter.next();
                if (libro.getTitulo().equals(titulo) && libro.getAutor().equals(autor)) {
                    iter.remove(); // Elimina el libro de la lista principal
                    break; // Salimos del bucle una vez encontrado y eliminado
                }
            }

            // 6. Guardar la lista actualizada en el archivo XML
            xmlManager.guardarLibros(listaDeLibros);

            // 7. Actualizar la tabla para que refleje la eliminación
            btnBuscarLocal.doClick(); // Simulamos un clic en el botón de búsqueda para refrescar la vista actual

            JOptionPane.showMessageDialog(this, "Libro eliminado y sincronizado.", "Eliminado", JOptionPane.INFORMATION_MESSAGE);

            actualizarTodaLaUI();
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    /**
     * Maneja la acción de editar un libro seleccionado.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        // 1. Obtener la fila seleccionada
        int filaSeleccionada = tblMiBiblioteca.getSelectedRow();
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Por favor, selecciona un libro de la tabla para editar.", "Ningún libro seleccionado", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Encontrar el objeto Libro real en nuestra lista principal
        String titulo = (String) tblMiBiblioteca.getValueAt(filaSeleccionada, 0);
        String autor = (String) tblMiBiblioteca.getValueAt(filaSeleccionada, 1);
        Libro libroAEditar = null;
        for (Libro libro : listaDeLibros) {
            if (libro.getTitulo().equals(titulo) && libro.getAutor().equals(autor)) {
                libroAEditar = libro;
                break;
            }
        }

        if (libroAEditar == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudo encontrar el libro seleccionado en la base de datos.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Crear y mostrar la ventana de diálogo de edición
        VentanaEditarLibro dialogoEditar = new VentanaEditarLibro(this, true, libroAEditar);
        dialogoEditar.setVisible(true);

        // 4. Después de que el diálogo se cierra, comprobar si el usuario guardó los cambios
        if (dialogoEditar.fueGuardado()) {
            // El libro ya fue modificado dentro del diálogo, así que solo necesitamos guardar y refrescar
            xmlManager.guardarLibros(listaDeLibros);
            dialogoEditar.getLibroEditado();
            btnBuscarLocal.doClick();
            JOptionPane.showMessageDialog(this, "Libro actualizado y sincronizado.", "Actualizado", JOptionPane.INFORMATION_MESSAGE);
            actualizarTodaLaUI();
        }
    }//GEN-LAST:event_btnEditarActionPerformed

    /**
     * Maneja la acción de cerrar la ventana actual cuando se hace clic en el
     * botón "Cerrar". Este método se activa al hacer clic en el botón
     * correspondiente y cierra la ventana actual.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnClosed1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClosed1ActionPerformed
        dispose();
    }//GEN-LAST:event_btnClosed1ActionPerformed

    /**
     * Maneja la acción de cerrar la ventana actual cuando se hace clic en el
     * botón "Cerrar". Este método se activa al hacer clic en el botón
     * correspondiente y cierra la ventana actual.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void btnClosedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClosedActionPerformed
        dispose();
    }//GEN-LAST:event_btnClosedActionPerformed

    /**
     * Maneja la acción de importar una base de datos.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void menuItemImportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemImportarActionPerformed

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Importar base de datos");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos XML", "xml"));

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File archivoAImportar = fileChooser.getSelectedFile();

            // Advertencia de seguridad para evitar sobreescribir datos por error
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que quieres reemplazar tu biblioteca actual con el contenido de este archivo?\n"
                    + "Esta acción no se puede deshacer.",
                    "Confirmar Importación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirmacion == JOptionPane.YES_OPTION) {
                File archivoDestino = new File(xmlManager.getDatabasePath());
                try {
                    // Copiamos el archivo seleccionado a la ubicación de nuestra base de datos
                    Files.copy(archivoAImportar.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Recargamos la lista de libros desde el archivo recién importado
                    this.listaDeLibros = xmlManager.cargarLibros();

                    // Actualizamos la tabla para mostrar la nueva biblioteca
                    actualizarTablaMiBiblioteca(this.listaDeLibros);

                    JOptionPane.showMessageDialog(this, "Base de datos importada con éxito.", "Importación Completa", JOptionPane.INFORMATION_MESSAGE);
                    actualizarTodaLaUI();
                } catch (IOException e) {
                    LOGGER.log(java.util.logging.Level.SEVERE, "Error al importar la base de datos.", e);
                    JOptionPane.showMessageDialog(this, "No se pudo importar el archivo.", "Error de Importación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_menuItemImportarActionPerformed


    private void menuAyudaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAyudaActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_menuAyudaActionPerformed

    /**
     * Este método es llamado cuando se realiza una acción sobre el ítem de menú
     * "Acerca de". Muestra una ventana de diálogo "Acerca de" en modalidad de
     * diálogo, lo que impide que el usuario interactúe con otras ventanas hasta
     * que cierre la ventana de "Acerca de".
     *
     * @param evt El evento que se genera cuando el ítem de menú es
     * seleccionado.
     */
    private void menuItemAcercaDeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAcercaDeActionPerformed
        VentanaAcercaDe dialogoAcerca = new VentanaAcercaDe(this, true);
        dialogoAcerca.setVisible(true);
    }//GEN-LAST:event_menuItemAcercaDeActionPerformed

    /**
     * Este método es llamado cuando se realiza una acción sobre el ítem de menú
     * "Salir". Cierra la ventana actual y termina la ejecución de la
     * aplicación.
     *
     * @param evt El evento que se genera cuando el ítem de menú es
     * seleccionado.
     */
    private void menuItemSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemSalirActionPerformed
        dispose();
    }//GEN-LAST:event_menuItemSalirActionPerformed

    /**
     * Maneja la acción de buscar libros duplicados.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void menuItemBuscarDuplicadosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemBuscarDuplicadosActionPerformed
        // Usamos un mapa para agrupar libros por una clave única (título+autor en minúsculas)
        Map<String, List<Libro>> mapaDeLibros = new HashMap<>();

        for (Libro libro : listaDeLibros) {
            String clave = (libro.getTitulo() + libro.getAutor()).toLowerCase();
            mapaDeLibros.computeIfAbsent(clave, k -> new ArrayList<>()).add(libro);
        }

        // Construimos un texto con los resultados
        StringBuilder sb = new StringBuilder();
        boolean duplicadosEncontrados = false;

        for (Map.Entry<String, List<Libro>> entry : mapaDeLibros.entrySet()) {
            if (entry.getValue().size() > 1) { // Si hay más de un libro en el grupo, es un duplicado
                duplicadosEncontrados = true;
                sb.append("----------------------------------------------------------\n");
                sb.append("Libro: ").append(entry.getValue().get(0).getTitulo()).append("\n");
                sb.append("Autor: ").append(entry.getValue().get(0).getAutor()).append("\n");
                sb.append("Número de copias: ").append(entry.getValue().size()).append("\n");
                sb.append("----------------------------------------------------------\n\n");
            }
        }

        if (duplicadosEncontrados) {
            VentanaDuplicados dialogo = new VentanaDuplicados(this, true, sb.toString());
            dialogo.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "No se encontraron libros duplicados en tu biblioteca.", "Búsqueda Finalizada", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_menuItemBuscarDuplicadosActionPerformed

    /**
     * Maneja la acción de abrir la ventana de configuración.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void menuItemConfiguracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemConfiguracionActionPerformed
        // TODO add your handling code here:
        String rutaActual = prefs.get(EXPORT_PATH_KEY, "");
        VentanaConfiguracion dialogo = new VentanaConfiguracion(this, true, rutaActual);
        dialogo.setVisible(true);

        // Después de que se cierra el diálogo, comprobamos si se guardó una nueva ruta
        String nuevaRuta = dialogo.getRutaSeleccionada();
        if (nuevaRuta != null) { // Si el usuario no pulsó Cancelar
            prefs.put(EXPORT_PATH_KEY, nuevaRuta);
            LOGGER.log(java.util.logging.Level.INFO, "Nueva ruta de exportación guardada: {0}", nuevaRuta);
        }
    }//GEN-LAST:event_menuItemConfiguracionActionPerformed

    /**
     * Este método es llamado cuando se realiza una acción sobre un ítem de
     * menú. Muestra una ventana de ayuda (VentanaAyuda) en modalidad de
     * diálogo, lo que impide que el usuario interactúe con otras ventanas hasta
     * que cierre la ventana de ayuda.
     *
     * @param evt El evento que se genera cuando el ítem de menú es
     * seleccionado.
     */
    private void menuItemManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemManualActionPerformed
        VentanaAyuda dialogoAyuda = new VentanaAyuda(this, true);
        dialogoAyuda.setVisible(true);
    }//GEN-LAST:event_menuItemManualActionPerformed

    /**
     * Activa la acción de buscar libros en la biblioteca de OpenLibrary.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void txtBusquedaOpenLibraryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBusquedaOpenLibraryActionPerformed

        btnBuscarOpenLibrary.doClick();
    }//GEN-LAST:event_txtBusquedaOpenLibraryActionPerformed

    /**
     * Ativa la acción de buscar libros en la biblioteca local.
     *
     * @param evt El evento de acción que desencadena este método.
     */
    private void txtBusquedaLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBusquedaLocalActionPerformed

        btnBuscarLocal.doClick();
    }//GEN-LAST:event_txtBusquedaLocalActionPerformed

    /**
     * Maneja la acción de exportar la base de datos a un archivo XML. Muestra
     * un cuadro de diálogo para que el usuario seleccione la ubicación y el
     * nombre del archivo de exportación. Utiliza las preferencias del usuario
     * para recordar el último directorio utilizado. Si la operación de
     * exportación es exitosa, muestra un mensaje de confirmación. En caso de
     * error durante la exportación, muestra un mensaje de error.
     *
     * @param evt El evento de acción que desencadena este método, generalmente
     * generado por la selección de un elemento de menú.
     */
    private void menuItemExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemExportarActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar base de datos");

        // Obtenemos la ruta guardada y la usamos como directorio por defecto
        String defaultPath = prefs.get(EXPORT_PATH_KEY, null);
        if (defaultPath != null && !defaultPath.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(defaultPath));
        }

        fileChooser.setSelectedFile(new File("biblioteca_exportada.xml"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos XML", "xml"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File archivoAExportar = fileChooser.getSelectedFile();
            File archivoFuente = new File(xmlManager.getDatabasePath());
            try {
                Files.copy(archivoFuente.toPath(), archivoAExportar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "Base de datos exportada con éxito a:\n" + archivoAExportar.getAbsolutePath(), "Exportación Completa", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "Error al exportar la base de datos.", e);
                JOptionPane.showMessageDialog(this, "No se pudo exportar el archivo.", "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        }

    }//GEN-LAST:event_menuItemExportarActionPerformed

    /**
     * Este método cierra la ventana actual y termina la ejecución de la
     * aplicación.
     *
     * @param evt El evento que se genera cuando el botón cerrar es
     * seleccionado.
     */
    private void btnClosedPrestamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClosedPrestamoActionPerformed

        this.dispose();
    }//GEN-LAST:event_btnClosedPrestamoActionPerformed

    /**
     * Maneja la acción de prestar un libro cuando se hace clic en el botón
     * "Prestar". Este método valida que se haya seleccionado un libro y que se
     * haya proporcionado un nombre de persona. Crea un nuevo préstamo con la
     * información proporcionada, lo añade a la lista de préstamos, guarda la
     * lista actualizada y actualiza la interfaz de usuario.
     *
     * @param evt El evento de acción que desencadena este método, generado por
     * un clic en el botón "Prestar".
     */
    private void btnPrestarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrestarActionPerformed
        Libro libroSeleccionado = (Libro) cmbLibrosDisponibles.getSelectedItem();
        String nombrePersona = txtNombrePersona.getText().trim();

        if (libroSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "No hay ningún libro disponible para prestar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nombrePersona.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce el nombre de la persona.", "Campo Vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Creamos el nuevo préstamo
        Prestamo nuevoPrestamo = new Prestamo(
                libroSeleccionado.getIsbn(),
                libroSeleccionado.getTitulo(),
                nombrePersona,
                LocalDate.now()
        );

        listaDePrestamos.add(nuevoPrestamo);
        xmlManager.guardarPrestamos(listaDePrestamos);

        JOptionPane.showMessageDialog(this, "Préstamo registrado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        txtNombrePersona.setText(""); // Limpiamos el campo de texto
        actualizarPestanaPrestamos(); // Actualizamos toda la pestaña

    }//GEN-LAST:event_btnPrestarActionPerformed

    /**
     * Maneja la acción de marcar un libro como devuelto cuando se hace clic en
     * el botón "Devolver". Este método verifica que se haya seleccionado una
     * fila en la tabla de préstamos. Actualiza la fecha de devolución del
     * préstamo seleccionado a la fecha actual, guarda la lista de préstamos
     * actualizada y actualiza la interfaz de usuario.
     *
     * @param evt El evento de acción que desencadena este método, generado por
     * un clic en el botón "Devolver".
     */
    private void btnDevolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDevolverActionPerformed
        int filaSeleccionada = tblPrestamos.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un préstamo de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tenemos que encontrar el objeto Prestamo real en nuestra lista
        Prestamo prestamoSeleccionado = listaDePrestamos.get(filaSeleccionada);

        if (prestamoSeleccionado.isDevuelto()) {
            JOptionPane.showMessageDialog(this, "Este libro ya ha sido devuelto.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        prestamoSeleccionado.setFechaDevolucion(LocalDate.now());
        xmlManager.guardarPrestamos(listaDePrestamos);

        JOptionPane.showMessageDialog(this, "Libro marcado como devuelto.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        actualizarPestanaPrestamos();

        actualizarTodaLaUI();

    }//GEN-LAST:event_btnDevolverActionPerformed

    /**
     * Maneja la acción de búsqueda de préstamos cuando se hace clic en el botón
     * "Buscar". Este método recupera el término de búsqueda ingresado por el
     * usuario y el criterio de búsqueda seleccionado. Realiza una búsqueda en
     * la lista de préstamos según el criterio seleccionado y actualiza la tabla
     * de préstamos con los resultados.
     *
     * @param evt El evento de acción que desencadena este método, generado por
     * un clic en el botón "Buscar".
     */
    private void btnBuscarPrestamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarPrestamoActionPerformed
        String terminoBusqueda = txtBusquedaPrestamo.getText().trim().toLowerCase();

        if (terminoBusqueda.isEmpty()) {
            actualizarTablaPrestamos(this.listaDePrestamos); // Muestra todos si está vacío
            return;
        }

        String criterio = (String) cmbCriterioBusquedaPrestamo.getSelectedItem();
        List<Prestamo> resultados = new ArrayList<>();

        for (Prestamo prestamo : this.listaDePrestamos) {
            String valorAComparar = "";
            if ("Nombre del Libro".equals(criterio)) {
                valorAComparar = prestamo.getTituloLibro().toLowerCase();
            } else if ("Nombre de Persona".equals(criterio)) {
                valorAComparar = prestamo.getNombrePersona().toLowerCase();
            }

            if (valorAComparar.contains(terminoBusqueda)) {
                resultados.add(prestamo);
            }
        }

        actualizarTablaPrestamos(resultados);
    }//GEN-LAST:event_btnBuscarPrestamoActionPerformed

    /**
     * Maneja la acción de búsqueda de préstamos cuando se activa el campo de
     * texto. Este método se activa típicamente al presionar "Enter" en el campo
     * de texto de búsqueda.
     *
     * @param evt El evento de acción que desencadena este método, generado por
     * una acción en el campo de texto.
     */
    private void txtBusquedaPrestamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBusquedaPrestamoActionPerformed
        btnBuscarPrestamo.doClick();
    }//GEN-LAST:event_txtBusquedaPrestamoActionPerformed
    /**
     * Maneja el evento de cambio de selección en el combo box de criterio de
     * búsqueda de préstamos. Este método se ejecuta automáticamente cuando el
     * usuario selecciona un nuevo criterio en el combo box de búsqueda de
     * préstamos.
     *
     * @param evt el evento de acción generado por el cambio de selección en el
     * combo box.
     */
    private void cmbCriterioBusquedaPrestamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbCriterioBusquedaPrestamoActionPerformed
        btnBuscarPrestamo.doClick();
    }//GEN-LAST:event_cmbCriterioBusquedaPrestamoActionPerformed

    /**
     * Actualiza la tabla de resultados con una lista de libros. Este método se
     * utiliza para mostrar los resultados de una búsqueda en una tabla.
     *
     * @param libros La lista de libros que se mostrarán en la tabla.
     */
    private void actualizarTablaResultados(java.util.List<com.bibliohouse.logic.Libro> libros) {
        this.resultadosBusquedaActual = libros;

        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblResultadosBusqueda.getModel();
        model.setRowCount(0);

        if (libros.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron resultados.", "Búsqueda", JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (Libro libro : libros) {
                model.addRow(new Object[]{
                    libro.getTitulo(),
                    libro.getAutor(),
                    libro.getAño(),
                    libro.getEditorial(),
                    libro.getIsbn()
                });
            }
        }
    }

    /**
     * Configura el modelo y las cabeceras de la tabla de resultados.
     */
    private void prepararTablaResultados() {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Título", "Autor", "Año", "Editorial", "ISBN"} // <-- Más columnas visibles
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblResultadosBusqueda.setModel(model);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Prestamos;
    private javax.swing.JPanel addBookPane;
    private javax.swing.JMenuBar barraMenu;
    private javax.swing.JButton btnAnadirSeleccionado;
    private javax.swing.JButton btnBuscarLocal;
    private javax.swing.JButton btnBuscarOpenLibrary;
    private javax.swing.JButton btnBuscarPrestamo;
    private javax.swing.JButton btnClosed;
    private javax.swing.JButton btnClosed1;
    private javax.swing.JButton btnClosedPrestamo;
    private javax.swing.JButton btnDevolver;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardarManual;
    private javax.swing.JButton btnPrestar;
    private javax.swing.JButton btnSeleccionarImagen;
    private javax.swing.JComboBox<String> cmbCriterioBusqueda;
    private javax.swing.JComboBox<String> cmbCriterioBusquedaPrestamo;
    private javax.swing.JComboBox<Libro> cmbLibrosDisponibles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblAnio;
    private javax.swing.JLabel lblAutor;
    private javax.swing.JLabel lblBusquedaOpenLibrary;
    private javax.swing.JLabel lblCriterioBusqueda;
    private javax.swing.JLabel lblEditorial;
    private javax.swing.JLabel lblGenero;
    private javax.swing.JLabel lblIsbn;
    private javax.swing.JLabel lblPortadaPreview;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JMenu menuArchivo;
    private javax.swing.JMenu menuAyuda;
    private javax.swing.JMenu menuHerramuentas;
    private javax.swing.JMenuItem menuItemAcercaDe;
    private javax.swing.JMenuItem menuItemBuscarDuplicados;
    private javax.swing.JMenuItem menuItemConfiguracion;
    private javax.swing.JMenuItem menuItemExportar;
    private javax.swing.JMenuItem menuItemImportar;
    private javax.swing.JMenuItem menuItemManual;
    private javax.swing.JMenuItem menuItemSalir;
    private javax.swing.JPanel searchBookPane;
    private javax.swing.JSpinner spinnerAnio;
    private javax.swing.JTable tblMiBiblioteca;
    private javax.swing.JTable tblPrestamos;
    private javax.swing.JTable tblResultadosBusqueda;
    private javax.swing.JTextField txtAutor;
    private javax.swing.JTextField txtBusquedaLocal;
    private javax.swing.JTextField txtBusquedaOpenLibrary;
    private javax.swing.JTextField txtBusquedaPrestamo;
    private javax.swing.JTextField txtEditorial;
    private javax.swing.JTextField txtGenero;
    private javax.swing.JTextField txtIsbn;
    private javax.swing.JTextField txtNombrePersona;
    private javax.swing.JTextField txtTitulo;
    // End of variables declaration//GEN-END:variables

}
