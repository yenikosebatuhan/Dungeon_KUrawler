package com.kurawler.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Main menu screen — New Game, Level Editor, Help, Exit.
 * Contains NO game logic.
 */
public class MainMenuScreen {

    private final ScreenManager screenManager;

    public MainMenuScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public Scene createScene() {
        Label title = new Label("Dungeon KUrawler");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#e0e0e0"));

        Label subtitle = new Label("A COMP 302 Dungeon Crawler");
        subtitle.setFont(Font.font("Monospaced", 14));
        subtitle.setTextFill(Color.web("#888888"));

        Button newGameBtn = createMenuButton("New Game");
        newGameBtn.setOnAction(e -> screenManager.startNewGame());

        Button editorBtn = createMenuButton("Level Editor");
        editorBtn.setOnAction(e -> screenManager.showBuildMode());

        Button saveBtn = createMenuButton("Save Map");
        saveBtn.setOnAction(e -> {
            boolean ok = screenManager.saveCurrentMap("map_save.json");
            System.out.println(ok ? "[Menu] Map saved to map_save.json" : "[Menu] Save failed.");
        });

        Button loadBtn = createMenuButton("Load Map");
        loadBtn.setOnAction(e -> {
            boolean ok = screenManager.loadMap("map_save.json");
            System.out.println(ok ? "[Menu] Map loaded from map_save.json" : "[Menu] Load failed.");
        });

        Button helpBtn = createMenuButton("Help");
        helpBtn.setOnAction(e -> {
            // TODO: Phase 5 — Help screen
            System.out.println("[Menu] Help not yet implemented.");
        });

        Button exitBtn = createMenuButton("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        VBox layout = new VBox(20, title, subtitle, newGameBtn, editorBtn, saveBtn, loadBtn, helpBtn, exitBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(60));
        layout.setStyle("-fx-background-color: #0f0f23;");

        return new Scene(layout, 500, 500);
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        btn.setPrefWidth(220);
        btn.setPrefHeight(40);
        btn.setStyle(
            "-fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; "
            + "-fx-border-color: #444466; -fx-border-width: 1; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e ->
            btn.setStyle("-fx-background-color: #2a2a4e; -fx-text-fill: #ffffff; "
                + "-fx-border-color: #6666aa; -fx-border-width: 1; -fx-cursor: hand;")
        );
        btn.setOnMouseExited(e ->
            btn.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #444466; -fx-border-width: 1; -fx-cursor: hand;")
        );
        return btn;
    }
}
