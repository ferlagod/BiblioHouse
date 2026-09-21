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
package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.AppEventBus;
import com.bibliohouse.logic.EbookMetadataService;
import com.bibliohouse.logic.EstadoLectura;
import com.bibliohouse.logic.JsonManager;
import com.bibliohouse.logic.Libro;
import com.bibliohouse.utils.ImageLoader;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controlador para la pestaña "Mis Libros". Gestiona la cuadrícula visual de
 * portadas (tarjetas interactivas), filtrado rápido, ordenación, drag & drop
 * de e-books y acciones de menú contextual sobre cada libro.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class CatalogoGridController {

    @FXML
    private MenuButton btnOrdenar;
    @FXML
    private TextField txtBuscarMisLibros;
    @FXML
    private FlowPane panelMisLibros;
    @FXML
    private ScrollPane scrollMisLibros;
    @FXML
    private HBox boxPaginacion;
    @FXML
    private Button btnPrimeraPagina;
    @FXML
    private Button btnAnterior;
    @FXML
    private Label lblInfoPaginacion;
    @FXML
    private Button btnSiguiente;
    @FXML
    private Button btnUltima;
    @FXML
    private ComboBox<String> comboLibrosPorPagina;

    // Estado de paginación para virtualización fluida
    private int paginaActual = 1;
    private int librosPorPagina = 48;
    private int totalPaginas = 1;
    private int totalLibrosFiltrados = 0;

    private PrimaryController mainController;
    private JsonManager jsonManager;
    private ObservableList<Libro> listaLibrosCompleta;
    private List<Libro> listaDeseos;
    private String rutaUsuario;
    private ResourceBundle resources;

    private String categoriaActual = SidebarController.VISTA_TODOS;
    private ContextMenu contextMenuLibros;

    private Comparator<Libro> currentComparator = Comparator.comparing(
            l -> l.getTitulo() != null ? l.getTitulo() : "",
            String::compareToIgnoreCase);

    @FXML
    public void initialize() {
        if (txtBuscarMisLibros != null) {
            txtBuscarMisLibros.textProperty().addListener((obs, oldVal, newVal) -> {
                paginaActual = 1;
                refrescarCuadricula();
            });
        }

        if (comboLibrosPorPagina != null) {
            comboLibrosPorPagina.getItems().setAll("24", "48", "96", "Todos");
            comboLibrosPorPagina.setValue(String.valueOf(librosPorPagina));
            comboLibrosPorPagina.valueProperty().addListener((obs, oldVal, newVal) -> {
                if ("Todos".equalsIgnoreCase(newVal)) {
                    librosPorPagina = Integer.MAX_VALUE;
                } else if (newVal != null) {
                    try {
                        librosPorPagina = Integer.parseInt(newVal.trim());
                    } catch (NumberFormatException ignored) {
                        librosPorPagina = 48;
                    }
                }
                paginaActual = 1;
                refrescarCuadricula();
            });
        }

        // Suscripción al bus de eventos para reaccionar a cambios
        AppEventBus.getInstance().subscribe(AppEventBus.FiltroEstanteriaEvent.class, e -> {
            this.categoriaActual = e.getEstanteria();
            this.paginaActual = 1;
            refrescarCuadricula();
        });

        AppEventBus.getInstance().subscribe(AppEventBus.LibroModificadoEvent.class, e -> refrescarCuadricula());
        AppEventBus.getInstance().subscribe(AppEventBus.LibroEliminadoEvent.class, e -> refrescarCuadricula());
    }

    /**
     * Inicializa las dependencias, listas observables, rutas y recursos localizados
     * requeridos por la vista de cuadrícula del catálogo de libros.
     *
     * @param mainController Controlador principal de la ventana (PrimaryController).
     * @param jsonManager    Gestor de persistencia en disco de datos JSON.
     * @param listaLibros    Lista observable que contiene la totalidad de libros del usuario.
     * @param listaDeseos    Lista de libros deseados pero no poseídos.
     * @param rutaUsuario    Directorio raíz del perfil del usuario en disco.
     * @param resources      Paquete de recursos para textos internacionalizados.
     */
    public void initData(PrimaryController mainController, JsonManager jsonManager,
                         ObservableList<Libro> listaLibros, List<Libro> listaDeseos,
                         String rutaUsuario, ResourceBundle resources) {
        this.mainController = mainController;
        this.jsonManager = jsonManager;
        this.listaLibrosCompleta = listaLibros;
        this.listaDeseos = listaDeseos;
        this.rutaUsuario = rutaUsuario;
        this.resources = resources;

        configurarContextMenu();
        configurarDragAndDrop();
        refrescarCuadricula();
    }

    // =========================================================================
    // ORDENACIÓN
    // =========================================================================

    /**
     * Ordena los libros alfabéticamente por título de forma ascendente (A - Z)
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorTituloAZ() {
        currentComparator = Comparator.comparing(l -> l.getTitulo() != null ? l.getTitulo() : "", String::compareToIgnoreCase);
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros alfabéticamente por título de forma descendente (Z - A)
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorTituloZA() {
        currentComparator = Comparator.comparing((Libro l) -> l.getTitulo() != null ? l.getTitulo() : "", String::compareToIgnoreCase).reversed();
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros alfabéticamente por autor de la A a la Z
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorAutor() {
        currentComparator = Comparator.comparing((Libro l) -> l.getAutor() != null ? l.getAutor() : "", String::compareToIgnoreCase);
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros por año de publicación de más reciente a más antiguo
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorAnio() {
        currentComparator = (l1, l2) -> {
            String a1 = l1.getAño() != null ? l1.getAño() : "";
            String a2 = l2.getAño() != null ? l2.getAño() : "";
            return a2.compareTo(a1); // Descendente
        };
        paginaActual = 1;
        refrescarCuadricula();
    }

    /**
     * Ordena los libros por orden de inserción o modificación más reciente
     * y reinicia la paginación a la primera página.
     */
    @FXML
    public void ordenarPorReciente() {
        currentComparator = (l1, l2) -> {
            if (listaLibrosCompleta == null) return 0;
            int i1 = listaLibrosCompleta.indexOf(l1);
            int i2 = listaLibrosCompleta.indexOf(l2);
            return Integer.compare(i2, i1); // Descendente
        };
        paginaActual = 1;
        refrescarCuadricula();
    }

    // =========================================================================
    // NAVEGACIÓN Y PAGINACIÓN
    // =========================================================================

    /**
     * Salta a la primera página de la cuadrícula de libros y desplaza la vista hacia arriba.
     */
    @FXML
    public void irPrimeraPagina() {
        if (paginaActual > 1) {
            paginaActual = 1;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Retrocede a la página anterior de la cuadrícula de libros si no se está en la primera.
     */
    @FXML
    public void irPaginaAnterior() {
        if (paginaActual > 1) {
            paginaActual--;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Avanza a la página siguiente de la cuadrícula de libros si hay páginas posteriores disponibles.
     */
    @FXML
    public void irPaginaSiguiente() {
        if (paginaActual < totalPaginas) {
            paginaActual++;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Salta directamente a la última página de la cuadrícula de libros.
     */
    @FXML
    public void irUltimaPagina() {
        if (paginaActual < totalPaginas) {
            paginaActual = totalPaginas;
            refrescarCuadricula();
            scrollearArriba();
        }
    }

    /**
     * Desplaza suavemente el ScrollPane de libros a la posición superior inicial.
     */
    private void scrollearArriba() {
        if (scrollMisLibros != null) {
            scrollMisLibros.setVvalue(0.0);
        }
    }

    /**
     * Actualiza las etiquetas de información de paginación y el estado de habilitación
     * de los botones de navegación anterior/siguiente/primera/última página.
     *
     * @param inicio Índice del primer libro mostrado en la página actual.
     * @param fin    Índice del último libro mostrado en la página actual.
     * @param total  Número total de libros tras aplicar los filtros de búsqueda y estantería.
     */
    private void actualizarBarraPaginacion(int inicio, int fin, int total) {
        if (boxPaginacion == null) {
            return;
        }

        if (total == 0) {
            if (lblInfoPaginacion != null) {
                lblInfoPaginacion.setText("0 libros");
            }
            if (btnPrimeraPagina != null) btnPrimeraPagina.setDisable(true);
            if (btnAnterior != null) btnAnterior.setDisable(true);
            if (btnSiguiente != null) btnSiguiente.setDisable(true);
            if (btnUltima != null) btnUltima.setDisable(true);
            return;
        }

        if (lblInfoPaginacion != null) {
            lblInfoPaginacion.setText(String.format("Mostrando %d-%d de %d (Pág. %d/%d)",
                    inicio, fin, total, paginaActual, totalPaginas));
        }

        boolean puedeRetroceder = paginaActual > 1;
        boolean puedeAvanzar = paginaActual < totalPaginas;

        if (btnPrimeraPagina != null) btnPrimeraPagina.setDisable(!puedeRetroceder);
        if (btnAnterior != null) btnAnterior.setDisable(!puedeRetroceder);
        if (btnSiguiente != null) btnSiguiente.setDisable(!puedeAvanzar);
        if (btnUltima != null) btnUltima.setDisable(!puedeAvanzar);
    }

    // =========================================================================
    // RENDERIZADO DE LA CUADRÍCULA
    // =========================================================================

    /**
     * Dibuja las tarjetas de libros respetando el filtro lateral de estantería,
     * el texto de búsqueda rápida y la paginación activa.
     */
    public void refrescarCuadricula() {
        if (panelMisLibros == null || listaLibrosCompleta == null) {
            return;
        }

        String busquedaRapida = txtBuscarMisLibros != null ? txtBuscarMisLibros.getText().toLowerCase().trim() : "";

        // Filtrar según estantería seleccionada
        List<Libro> filtrados = listaLibrosCompleta.stream()
                .filter(l -> {
                    // Filtro de categoría
                    if (SidebarController.VISTA_DESEOS.equals(categoriaActual)) {
                        return !l.isPoseido();
                    }
                    if (SidebarController.VISTA_DIGITAL.equals(categoriaActual)) {
                        return l.isEsDigital() && l.isPoseido();
                    }
                    if (SidebarController.VISTA_TODOS.equals(categoriaActual) || categoriaActual == null) {
                        return l.isPoseido();
                    }
                    return l.isPoseido() && l.getEstanterias() != null && l.getEstanterias().contains(categoriaActual);
                })
                .filter(l -> {
                    // Filtro de texto
                    if (busquedaRapida.isEmpty()) {
                        return true;
                    }
                    boolean tituloCoincide = l.getTitulo() != null && l.getTitulo().toLowerCase().contains(busquedaRapida);
                    boolean autorCoincide = l.getAutor() != null && l.getAutor().toLowerCase().contains(busquedaRapida);
                    return tituloCoincide || autorCoincide;
                })
                .sorted(currentComparator)
                .collect(Collectors.toList());

        totalLibrosFiltrados = filtrados.size();

        // Estado vacío
        if (filtrados.isEmpty()) {
            totalPaginas = 1;
            paginaActual = 1;
            actualizarBarraPaginacion(0, 0, 0);

            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 60 0 60 0;");

            Label lblIcon = new Label("📚");
            lblIcon.setStyle("-fx-font-size: 48px;");

            Label lblVacio = new Label("Tu biblioteca está vacía aquí");
            lblVacio.setStyle("-fx-text-fill: -color-fg-default; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label lblSub = new Label("Prueba a cambiar el filtro de estantería\no añade libros nuevos desde «Gestionar Libros»");
            lblSub.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px; -fx-text-alignment: center;");
            lblSub.setWrapText(true);
            lblSub.setMaxWidth(400);
            lblSub.setAlignment(Pos.CENTER);

            emptyState.getChildren().addAll(lblIcon, lblVacio, lblSub);
            panelMisLibros.getChildren().setAll(emptyState);
            return;
        }

        // Calcular paginación para virtualización
        int pageSize = librosPorPagina > 0 ? librosPorPagina : Integer.MAX_VALUE;
        totalPaginas = (int) Math.ceil((double) filtrados.size() / pageSize);
        if (totalPaginas < 1) {
            totalPaginas = 1;
        }
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }
        if (paginaActual < 1) {
            paginaActual = 1;
        }

        int desde = (paginaActual - 1) * pageSize;
        if (desde >= filtrados.size()) {
            desde = 0;
            paginaActual = 1;
        }
        int hasta = Math.min(desde + pageSize, filtrados.size());
        List<Libro> paginaLibros = filtrados.subList(desde, hasta);

        actualizarBarraPaginacion(desde + 1, hasta, filtrados.size());

        // Construir tarjetas exclusivamente para la sublista de la página actual
        List<Node> tarjetas = paginaLibros.stream()
                .map(this::crearTarjetaMisLibros)
                .collect(Collectors.toList());
        panelMisLibros.getChildren().setAll(tarjetas);
    }

    /**
     * Obtiene el número de la página actual en la cuadrícula (1-indexed).
     *
     * @return Página actual.
     */
    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Establece el número de página actual en la cuadrícula.
     *
     * @param paginaActual Nueva página a mostrar.
     */
    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    /**
     * Obtiene la cantidad máxima de libros que se renderizan por página.
     *
     * @return Límite de libros por página.
     */
    public int getLibrosPorPagina() {
        return librosPorPagina;
    }

    /**
     * Establece la cantidad de libros que se renderizarán por página.
     *
     * @param librosPorPagina Cantidad de libros por página.
     */
    public void setLibrosPorPagina(int librosPorPagina) {
        this.librosPorPagina = librosPorPagina;
    }

    /**
     * Obtiene el total de páginas calculadas para la vista actual.
     *
     * @return Número total de páginas.
     */
    public int getTotalPaginas() {
        return totalPaginas;
    }

    /**
     * Obtiene el total de libros que coinciden con los filtros actuales.
     *
     * @return Cantidad de libros filtrados.
     */
    public int getTotalLibrosFiltrados() {
        return totalLibrosFiltrados;
    }

    /**
     * Establece la lista completa observable de libros de la biblioteca.
     *
     * @param listaLibrosCompleta Lista de libros.
     */
    public void setListaLibrosCompleta(ObservableList<Libro> listaLibrosCompleta) {
        this.listaLibrosCompleta = listaLibrosCompleta;
    }

    /**
     * Establece la categoría o estantería activa para el filtrado de libros.
     *
     * @param categoriaActual Nombre de la estantería o categoría seleccionada.
     */
    public void setCategoriaActual(String categoriaActual) {
        this.categoriaActual = categoriaActual;
    }

    /**
     * Obtiene el FlowPane contenedor de la cuadrícula de libros.
     *
     * @return Contenedor FlowPane.
     */
    public FlowPane getPanelMisLibros() {
        return panelMisLibros;
    }

    /**
     * Establece el FlowPane contenedor de la cuadrícula de libros.
     *
     * @param panelMisLibros Contenedor FlowPane.
     */
    public void setPanelMisLibros(FlowPane panelMisLibros) {
        this.panelMisLibros = panelMisLibros;
    }

    /**
     * Construye la tarjeta gráfica de un libro individual con portada, títulos,
     * insignias de lectura y soporte para eventos de clic y menú contextual.
     *
     * @param libro Libro para el que se genera la tarjeta.
     * @return Nodo {@link VBox} con la tarjeta interactiva renderizada.
     */
    private VBox crearTarjetaMisLibros(Libro libro) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.setPrefWidth(140);
        tarjeta.setStyle("-fx-padding: 10; -fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-cursor: hand;");

        // Eventos de ratón
        tarjeta.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                if (e.getClickCount() == 2) {
                    abrirDetalle(libro);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (mainController != null) {
                    mainController.seleccionarLibro(libro);
                }
                mostrarContextMenu(tarjeta, libro, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        tarjeta.setOnContextMenuRequested(e -> {
            if (mainController != null) {
                mainController.seleccionarLibro(libro);
            }
            mostrarContextMenu(tarjeta, libro, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        ImageView img = new ImageView();
        img.setMouseTransparent(true);
        ImageLoader.load(libro.getPortadaURL(), img, 110, 160);

        StackPane contenedorPortada = new StackPane(img);
        contenedorPortada.setMouseTransparent(true);

        // Badge de estado de lectura desacoplado
        EstadoLectura estado = libro.getEstadoLecturaEnum();
        Label badge = null;
        if (estado == EstadoLectura.LEIDO) {
            badge = new Label("✓");
            badge.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 12; -fx-font-size: 11px;");
        } else if (estado == EstadoLectura.LEYENDO) {
            badge = new Label("•••");
            badge.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 1 6 3 6; -fx-background-radius: 12; -fx-font-size: 11px;");
        } else if (estado == EstadoLectura.ABANDONADO) {
            badge = new Label("✕");
            badge.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 12; -fx-font-size: 11px;");
        }

        if (badge != null) {
            badge.setStyle(badge.getStyle() + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(5, 5, 0, 0));
            contenedorPortada.getChildren().add(badge);
        }

        Label lblTitulo = new Label(libro.getTitulo());
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(130);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-default;");
        lblTitulo.setMouseTransparent(true);

        // Badge Digital
        if (libro.isEsDigital()) {
            Label badgeDigital = new Label("📱");
            badgeDigital.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 6 3 6; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeDigital.setStyle(badgeDigital.getStyle() + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);");
            StackPane.setAlignment(badgeDigital, Pos.BOTTOM_LEFT);
            StackPane.setMargin(badgeDigital, new Insets(0, 0, 5, 5));
            contenedorPortada.getChildren().add(badgeDigital);
        }

        tarjeta.getChildren().addAll(contenedorPortada, lblTitulo);

        // Reading Tracker barra de progreso
        if (estado == EstadoLectura.LEYENDO && libro.getPaginasTotales() > 0) {
            double progreso = (double) libro.getPaginaActual() / libro.getPaginasTotales();
            progreso = Math.max(0.0, Math.min(1.0, progreso));

            ProgressBar pBar = new ProgressBar(progreso);
            pBar.setPrefWidth(110);
            pBar.setPrefHeight(6);
            pBar.setStyle("-fx-accent: #ff9800;");
            pBar.setMouseTransparent(true);

            Label lblProgreso = new Label(libro.getPaginaActual() + " / " + libro.getPaginasTotales() + " pág.");
            lblProgreso.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-fg-muted;");
            lblProgreso.setMouseTransparent(true);

            tarjeta.getChildren().addAll(pBar, lblProgreso);
        }

        return tarjeta;
    }

    // =========================================================================
    // MENÚ CONTEXTUAL DE LIBROS
    // =========================================================================

    /**
     * Inicializa la instancia compartida del menú contextual de libros.
     */
    private void configurarContextMenu() {
        this.contextMenuLibros = new ContextMenu();
    }

    /**
     * Construye dinámicamente y despliega el menú contextual sobre la tarjeta del libro,
     * adaptando las opciones disponibles según si el libro es digital o físico (leer,
     * prestar, editar, cambiar portada, cambiar estado de lectura, imprimir etiqueta, eliminar).
     *
     * @param owner   Nodo gráfico propietario sobre el que se despliega el menú.
     * @param libro   Libro sobre el que se ejecutarán las acciones.
     * @param screenX Coordenada X absoluta de la pantalla donde se produjo el evento.
     * @param screenY Coordenada Y absoluta de la pantalla donde se produjo el evento.
     */
    private void mostrarContextMenu(Node owner, Libro libro, double screenX, double screenY) {
        if (contextMenuLibros == null) {
            contextMenuLibros = new ContextMenu();
        }
        contextMenuLibros.getItems().clear();

        // 1. Leer (si es ebook)
        boolean isEbook = libro.isEsDigital() && libro.getRutaArchivoDigital() != null && !libro.getRutaArchivoDigital().isEmpty();
        if (isEbook) {
            MenuItem itemLeer = new MenuItem(resources != null && resources.containsKey("ctx.read") ? resources.getString("ctx.read") : "Leer");
            itemLeer.setOnAction(e -> {
                if (mainController != null) {
                    mainController.abrirLectorDigital(libro);
                }
            });
            contextMenuLibros.getItems().addAll(itemLeer, new SeparatorMenuItem());
        }

        // 2. Prestar
        MenuItem itemPrestar = new MenuItem(resources != null && resources.containsKey("ctx.loan") ? resources.getString("ctx.loan") : "Prestar");
        itemPrestar.setOnAction(e -> {
            if (mainController != null) {
                mainController.prepararPrestamoLibro(libro);
            }
        });

        // 3. Editar
        MenuItem itemEditar = new MenuItem(resources != null && resources.containsKey("ctx.edit") ? resources.getString("ctx.edit") : "Editar");
        itemEditar.setOnAction(e -> {
            if (mainController != null) {
                mainController.editarLibro(libro);
            }
        });

        // 4. Cambiar portada
        MenuItem itemPortada = new MenuItem(resources != null && resources.containsKey("ctx.cover") ? resources.getString("ctx.cover") : "Cambiar portada");
        itemPortada.setOnAction(e -> {
            if (mainController != null) {
                mainController.cambiarPortada(libro);
            }
        });

        // 5. Estado de lectura
        Menu menuEstado = new Menu(resources != null && resources.containsKey("ctx.mark_as") ? resources.getString("ctx.mark_as") : "Marcar como...");
        for (EstadoLectura est : EstadoLectura.values()) {
            MenuItem item = new MenuItem(est.getEtiqueta());
            item.setOnAction(e -> cambiarEstadoLectura(libro, est));
            menuEstado.getItems().add(item);
        }

        // 6. Etiqueta física (si es libro físico)
        MenuItem itemEtiquetas = new MenuItem(resources != null && resources.containsKey("ctx.label") ? resources.getString("ctx.label") : "Imprimir etiqueta física");
        itemEtiquetas.setOnAction(e -> {
            if (mainController != null) {
                mainController.generarEtiquetaFisica(libro);
            }
        });

        // 7. Eliminar
        MenuItem itemEliminar = new MenuItem(resources != null && resources.containsKey("ctx.delete") ? resources.getString("ctx.delete") : "Eliminar");
        itemEliminar.setStyle("-fx-text-fill: red;");
        itemEliminar.setOnAction(e -> {
            if (mainController != null) {
                mainController.eliminarLibro(libro);
            }
        });

        contextMenuLibros.getItems().addAll(itemPrestar, new SeparatorMenuItem(), itemEditar, itemPortada,
                menuEstado, new SeparatorMenuItem());

        if (!libro.isEsDigital()) {
            contextMenuLibros.getItems().addAll(itemEtiquetas, new SeparatorMenuItem());
        }

        contextMenuLibros.getItems().add(itemEliminar);
        contextMenuLibros.show(owner, screenX, screenY);
    }

    /**
     * Actualiza el estado de lectura de un libro (Pendiente, Leyendo, Leído, Abandonado),
     * persistiendo el cambio con debounce y notificando al bus de eventos de la aplicación.
     *
     * @param libro       Libro cuyo estado será modificado.
     * @param nuevoEstado Nuevo {@link EstadoLectura} a aplicar.
     */
    private void cambiarEstadoLectura(Libro libro, EstadoLectura nuevoEstado) {
        libro.setEstadoLecturaEnum(nuevoEstado);
        if (jsonManager != null && listaLibrosCompleta != null) {
            jsonManager.guardarLibrosDebounced(listaLibrosCompleta);
        }
        refrescarCuadricula();
        AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(libro, false));
        AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Estado actualizado a " + nuevoEstado.getEtiqueta() + ": " + libro.getTitulo()));
    }

    /**
     * Solicita al controlador principal abrir la ventana con la ficha de detalle del libro.
     *
     * @param libro Libro seleccionado por el usuario.
     */
    private void abrirDetalle(Libro libro) {
        if (mainController != null) {
            mainController.mostrarDetalleLibro(libro);
        }
    }

    // =========================================================================
    // DRAG & DROP DE E-BOOKS
    // =========================================================================

    /**
     * Configura el comportamiento de arrastrar y soltar (Drag & Drop) sobre el panel
     * del catálogo para permitir la importación directa y automática de archivos e-book (EPUB, PDF, MOBI).
     */
    private void configurarDragAndDrop() {
        if (panelMisLibros == null) {
            return;
        }

        panelMisLibros.setOnDragOver(event -> {
            if (event.getGestureSource() != panelMisLibros && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                panelMisLibros.setStyle("-fx-background-color: -color-accent-subtle; -fx-border-color: -color-accent-emphasis; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8;");
            }
            event.consume();
        });

        panelMisLibros.setOnDragExited(event -> {
            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            event.consume();
        });

        panelMisLibros.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            panelMisLibros.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (EbookMetadataService.esArchivoEbook(file)) {
                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("Importando e-book..."));

                    Thread importThread = new Thread(() -> {
                        Libro nuevoLibro = EbookMetadataService.crearLibroDesdeArchivo(file, this.rutaUsuario);
                        Platform.runLater(() -> {
                            if (nuevoLibro != null) {
                                boolean existe = listaLibrosCompleta.stream()
                                        .anyMatch(l -> l.getTitulo() != null && l.getTitulo().equalsIgnoreCase(nuevoLibro.getTitulo()));

                                if (existe) {
                                    if (mainController != null) {
                                        mainController.mostrarAlertaPublic("Libro duplicado", "Ya existe un libro con el título: " + nuevoLibro.getTitulo());
                                    }
                                } else {
                                    nuevoLibro.setId(UUID.randomUUID().toString());
                                    listaLibrosCompleta.add(nuevoLibro);
                                    if (jsonManager != null) {
                                        jsonManager.guardarLibros(listaLibrosCompleta);
                                    }
                                    refrescarCuadricula();
                                    AppEventBus.getInstance().publish(new AppEventBus.LibroModificadoEvent(nuevoLibro, true));
                                    AppEventBus.getInstance().publish(new AppEventBus.StatusMessageEvent("E-book importado: " + nuevoLibro.getTitulo()));
                                }
                            }
                        });
                    });
                    importThread.setDaemon(true);
                    importThread.start();
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }
}
