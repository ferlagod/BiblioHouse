/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 */
package com.ferlagod.bibliohousefx;

import com.bibliohouse.logic.Libro;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Paleta de comandos flotante y búsqueda global rápida (estilo Spotlight / Raycast).
 * Se activa mediante el atajo universal Shortcut+K (Cmd+K en macOS / Ctrl+K en Windows/Linux).
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.1
 */
public class PaletaComandosDialog {

    public static class PaletaItem {
        private final String icono;
        private final String titulo;
        private final String subtitulo;
        private final String atajo;
        private final Runnable accion;

        public PaletaItem(String icono, String titulo, String subtitulo, String atajo, Runnable accion) {
            this.icono = icono;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.atajo = atajo;
            this.accion = accion;
        }

        public String getIcono() { return icono; }
        public String getTitulo() { return titulo; }
        public String getSubtitulo() { return subtitulo; }
        public String getAtajo() { return atajo; }
        public Runnable getAccion() { return accion; }
    }

    public static void mostrar(PrimaryController mainController) {
        if (mainController == null) {
            return;
        }
        Window owner = mainController.getWindow();

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox root = new VBox(10);
        root.setPrefWidth(580);
        root.setMaxWidth(580);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 14; -fx-border-color: -color-border-default; -fx-border-radius: 14; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 24, 0, 0, 8); -fx-padding: 16;");

        // Fila de búsqueda
        HBox boxBusqueda = new HBox(10);
        boxBusqueda.setAlignment(Pos.CENTER_LEFT);
        boxBusqueda.setStyle("-fx-padding: 4 6;");

        Label iconLupa = new Label("🔍");
        iconLupa.setStyle("-fx-font-size: 16px;");

        TextField txtBusqueda = new TextField();
        txtBusqueda.setPromptText("Buscar libro, autor o comando (ej. 'Quijote', 'Añadir', 'PDF', 'Configuración')...");
        txtBusqueda.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-padding: 4; -fx-border-width: 0;");
        HBox.setHgrow(txtBusqueda, Priority.ALWAYS);

        Label badgeK = new Label("ESC para cerrar");
        badgeK.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted; -fx-background-color: -color-bg-subtle; -fx-padding: 2 6; -fx-background-radius: 6;");

        boxBusqueda.getChildren().addAll(iconLupa, txtBusqueda, badgeK);

        Separator sep = new Separator();

        // Lista de resultados
        ListView<PaletaItem> listaResultados = new ListView<>();
        listaResultados.setPrefHeight(320);
        listaResultados.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        listaResultados.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PaletaItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox fila = new HBox(12);
                    fila.setAlignment(Pos.CENTER_LEFT);
                    fila.setPadding(new Insets(6, 8, 6, 8));

                    Label lblIcon = new Label(item.getIcono());
                    lblIcon.setStyle("-fx-font-size: 16px;");

                    VBox textos = new VBox(2);
                    Label lblTitulo = new Label(item.getTitulo());
                    lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-fg-default;");

                    Label lblSub = new Label(item.getSubtitulo());
                    lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
                    textos.getChildren().addAll(lblTitulo, lblSub);
                    HBox.setHgrow(textos, Priority.ALWAYS);

                    fila.getChildren().addAll(lblIcon, textos);

                    if (item.getAtajo() != null && !item.getAtajo().isBlank()) {
                        Label lblAtajo = new Label(item.getAtajo());
                        lblAtajo.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-muted; -fx-background-color: -color-bg-subtle; -fx-padding: 2 6; -fx-background-radius: 6; -fx-border-color: -color-border-subtle; -fx-border-radius: 6;");
                        fila.getChildren().add(lblAtajo);
                    }

                    setGraphic(fila);
                    setText(null);
                }
            }
        });

        // Generar lista fija de comandos de la aplicación
        List<PaletaItem> comandosBase = new ArrayList<>();
        comandosBase.add(new PaletaItem("➕", "Añadir Nuevo Libro", "Buscar en OpenLibrary o registrar manualmente", "Cmd+N", () -> {
            mainController.seleccionarPestanaDirecta(1);
        }));
        comandosBase.add(new PaletaItem("📷", "Escanear Código de Barras", "Usar cámara web para capturar ISBN automáticamente", "", () -> {
            mainController.abrirEscaner();
        }));
        comandosBase.add(new PaletaItem("⚙️", "Configuración y Preferencias", "Ajustar tema visual, idioma, NextCloud y opciones", "Cmd+,", () -> {
            mainController.abrirConfiguracion();
        }));
        comandosBase.add(new PaletaItem("📊", "Estadísticas de Biblioteca", "Ver métricas, gráficos de géneros y estados de lectura", "", () -> {
            mainController.mostrarEstadisticas();
        }));
        comandosBase.add(new PaletaItem("📄", "Exportar a PDF", "Generar informe maquetado para imprimir o compartir", "", () -> {
            mainController.exportarPDF();
        }));
        comandosBase.add(new PaletaItem("🌐", "Exportar Catálogo Web", "Crear página HTML interactiva con tu biblioteca", "", () -> {
            mainController.exportarWeb();
        }));
        comandosBase.add(new PaletaItem("🔍", "Buscar Duplicados", "Identificar libros repetidos por ISBN o título", "", () -> {
            mainController.buscarDuplicados();
        }));
        comandosBase.add(new PaletaItem("📚", "Ir a Mis Libros", "Explorar el catálogo principal", "Cmd+1", () -> {
            mainController.seleccionarPestanaDirecta(0);
        }));
        comandosBase.add(new PaletaItem("🤝", "Ir a Préstamos Activos", "Gestionar préstamos a socios", "Cmd+3", () -> {
            mainController.seleccionarPestanaDirecta(2);
        }));
        comandosBase.add(new PaletaItem("⭐", "Ir a Lista de Deseos", "Ver tus próximas lecturas deseadas", "Cmd+5", () -> {
            mainController.seleccionarPestanaDirecta(4);
        }));
        comandosBase.add(new PaletaItem("📖", "Ir a Sagas y Series", "Colecciones agrupadas", "Cmd+6", () -> {
            mainController.seleccionarPestanaDirecta(5);
        }));

        Runnable actualizarFiltro = () -> {
            String query = txtBusqueda.getText().toLowerCase().trim();
            List<PaletaItem> items = new ArrayList<>();

            // 1. Filtrar comandos coincidentes
            for (PaletaItem cmd : comandosBase) {
                if (query.isEmpty() || cmd.getTitulo().toLowerCase().contains(query) || cmd.getSubtitulo().toLowerCase().contains(query)) {
                    items.add(cmd);
                }
            }

            // 2. Filtrar libros coincidentes
            if (mainController.getListaLibrosCompleta() != null && !query.isEmpty()) {
                int countLibros = 0;
                for (Libro l : mainController.getListaLibrosCompleta()) {
                    boolean tituloCoincide = l.getTitulo() != null && l.getTitulo().toLowerCase().contains(query);
                    boolean autorCoincide = l.getAutor() != null && l.getAutor().toLowerCase().contains(query);
                    boolean generoCoincide = l.getGenero() != null && l.getGenero().toLowerCase().contains(query);
                    if (tituloCoincide || autorCoincide || generoCoincide) {
                        String sub = (l.getAutor() != null ? l.getAutor() : "Autor desconocido") + " • " + l.getEstadoLecturaEnum().getEtiqueta();
                        items.add(new PaletaItem("📖", l.getTitulo(), sub, "Ficha", () -> {
                            mainController.abrirDetalleLibroDirecto(l);
                        }));
                        countLibros++;
                        if (countLibros >= 15) {
                            break; // Límite para máxima fluidez
                        }
                    }
                }
            }

            listaResultados.setItems(FXCollections.observableArrayList(items));
            if (!items.isEmpty()) {
                listaResultados.getSelectionModel().select(0);
            }
        };

        txtBusqueda.textProperty().addListener((obs, oldVal, newVal) -> actualizarFiltro.run());
        actualizarFiltro.run();

        // Ejecutar selección
        Runnable ejecutarSeleccion = () -> {
            PaletaItem seleccionado = listaResultados.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                stage.close();
                Platform.runLater(() -> seleccionado.getAccion().run());
            }
        };

        txtBusqueda.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                listaResultados.getSelectionModel().selectNext();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                listaResultados.getSelectionModel().selectPrevious();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                ejecutarSeleccion.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
                e.consume();
            }
        });

        listaResultados.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ejecutarSeleccion.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
                e.consume();
            }
        });

        listaResultados.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                ejecutarSeleccion.run();
            }
        });

        // Pie informativo
        HBox pie = new HBox(15);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setStyle("-fx-padding: 4 6 0 6;");

        Label hintFlechas = new Label("↑↓ para navegar");
        hintFlechas.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        Label hintEnter = new Label("↵ Enter para abrir");
        hintEnter.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hintSpotlight = new Label("BiblioHouse Spotlight");
        hintSpotlight.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");

        pie.getChildren().addAll(hintFlechas, hintEnter, spacer, hintSpotlight);

        root.getChildren().addAll(boxBusqueda, sep, listaResultados, pie);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        stage.setScene(scene);
        stage.show();
        txtBusqueda.requestFocus();
    }
}
