package com.kurawler.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class DifficultySelectionScreen {
    private static final int W = 1280;
    private static final int H = 720;
    private final ScreenManager screenManager;
    private final boolean isRandomMap;
    private String selectedDifficulty = "Normal";

    public DifficultySelectionScreen(ScreenManager screenManager, boolean isRandomMap) {
        this.screenManager = screenManager;
        this.isRandomMap = isRandomMap;
    }

    public Scene createScene() {
        // --- 1. Background with Torchlight Vignette ---
        Canvas bg = new Canvas(W, H);
        drawDungeonBackground(bg);

        Pane vignette = new Pane();
        vignette.setPrefSize(W, H);
        vignette.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 75%, rgba(10,10,20,0.4), rgba(5,5,10,0.95));");

        // --- 2. Title & Ornament ---
        Label title = new Label("SELECT DIFFICULTY");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 55));
        title.setTextFill(Color.web("#e8c84a"));
        
        InnerShadow engrave = new InnerShadow(4, 1, 2, Color.web("#5a4210"));
        DropShadow outerGlow = new DropShadow(20, Color.web("#ff9500", 0.4));
        outerGlow.setInput(engrave);
        title.setEffect(outerGlow);

        Label ornament = new Label("\u2501\u2501\u2501\u2501 \u2726 \u2726 \u2726 \u2501\u2501\u2501\u2501");
        ornament.setFont(Font.font("Monospaced", 20));
        ornament.setTextFill(Color.web("#8b6914", 0.7));
        ornament.setEffect(new DropShadow(4, 0, 1, Color.web("#000000", 0.5)));

        // --- 3. Description Plaque ---
        Label descLabel = new Label("Normal:\nDefault enemy stats and starting energy.");
        descLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        descLabel.setTextFill(Color.web("#e8c84a"));
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setWrapText(true);

        StackPane descPlaque = new StackPane(descLabel);
        descPlaque.setMaxWidth(750);
        descPlaque.setPrefHeight(100);
        descPlaque.setPadding(new Insets(15, 30, 15, 30));
        descPlaque.setStyle("-fx-background-color: rgba(15, 15, 25, 0.85); "
                + "-fx-background-radius: 8; -fx-border-color: #8b7940; "
                + "-fx-border-width: 2; -fx-border-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 5);");

        // --- 4. Difficulty Buttons ---
        Button easyBtn = stoneButton("Easy");
        Button normalBtn = stoneButton("Normal");
        Button hardBtn = stoneButton("Hard");

        // Initial State
        setActiveStyle(normalBtn, "#e8c84a");

        easyBtn.setOnAction(e -> {
            selectedDifficulty = "Easy";
            descLabel.setText("Easy:\nEnemies have -30% HP and ATK. Hero starts with 150 Energy.");
            descLabel.setTextFill(Color.web("#55efc4"));
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            setActiveStyle(easyBtn, "#55efc4");
            descPlaque.setStyle(descPlaque.getStyle().replace("#8b7940", "#55efc4").replace("#ff4a4a", "#55efc4"));
        });

        normalBtn.setOnAction(e -> {
            selectedDifficulty = "Normal";
            descLabel.setText("Normal:\nDefault enemy stats and starting energy.");
            descLabel.setTextFill(Color.web("#e8c84a"));
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            setActiveStyle(normalBtn, "#e8c84a");
            descPlaque.setStyle(descPlaque.getStyle().replace("#55efc4", "#8b7940").replace("#ff4a4a", "#8b7940"));
        });

        hardBtn.setOnAction(e -> {
            selectedDifficulty = "Hard";
            descLabel.setText("Hard:\nEnemies have +30% HP and ATK. Hero starts with 75 Energy.\nNo Health Potions on the map!");
            descLabel.setTextFill(Color.web("#ff4a4a"));
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            setActiveStyle(hardBtn, "#ff4a4a");
            descPlaque.setStyle(descPlaque.getStyle().replace("#8b7940", "#ff4a4a").replace("#55efc4", "#ff4a4a"));
        });

        HBox diffBox = new HBox(40, easyBtn, normalBtn, hardBtn);
        diffBox.setAlignment(Pos.CENTER);

        // --- 5. Action Buttons ---
        Button confirmBtn = new Button("\u2694 CONFIRM & START");
        confirmBtn.setFont(Font.font("Serif", FontWeight.BOLD, 24));
        confirmBtn.setPrefWidth(350);
        confirmBtn.setPrefHeight(65);
        String confirmBase = "-fx-background-color: linear-gradient(to bottom, #f3d054, #d4a017, #b8860b); "
                + "-fx-text-fill: #15110c; -fx-background-radius: 6; -fx-border-radius: 6; "
                + "-fx-border-color: #5a4210; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(232, 200, 74, 0.4), 15, 0, 0, 0);";
        String confirmHover = "-fx-background-color: linear-gradient(to bottom, #ffe066, #e8b825, #cda015); "
                + "-fx-text-fill: #000000; -fx-background-radius: 6; -fx-border-radius: 6; "
                + "-fx-border-color: #8b6914; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(232, 200, 74, 0.7), 20, 0, 0, 0);";
        confirmBtn.setStyle(confirmBase);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmHover));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(confirmBase));
        confirmBtn.setOnAction(e -> screenManager.launchGameWithDifficulty(isRandomMap, selectedDifficulty));

        Button backBtn = new Button("\u2190 Return to Main Menu");
        backBtn.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b7940; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e8c84a; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b7940; -fx-cursor: hand;"));
        backBtn.setOnAction(e -> screenManager.showMainMenu());

        // --- Layout Assembly ---
        VBox titleBox = new VBox(5, title, ornament);
        titleBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(45, titleBox, diffBox, descPlaque, confirmBtn, backBtn);
        layout.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(bg, vignette, layout);
        return new Scene(root, W, H);
    }

    private void setActiveStyle(Button btn, String colorHex) {
        String active = "-fx-background-color: linear-gradient(to bottom, #1e1b16, #252018, #2e2a22); "
                + "-fx-text-fill: " + colorHex + "; "
                + "-fx-background-radius: 6; -fx-border-radius: 6; "
                + "-fx-border-color: " + colorHex + "; -fx-border-width: 2.5; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.9), 10, 0, 2, 2);";
        btn.setStyle(active);
    }

    private void resetButtonStyles(Button... btns) {
        String base = "-fx-background-color: linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16, #2a2720); "
                + "-fx-text-fill: #9a9080; -fx-background-radius: 6; -fx-border-radius: 6; "
                + "-fx-border-color: #5a4e30 #3a3220 #2a2215 #5a4e30; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.5), 6, 0, 0, 3);";
        for (Button b : btns) {
            b.setStyle(base);
        }
    }

    private Button stoneButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 26));
        btn.setPrefWidth(220);
        btn.setPrefHeight(70);
        resetButtonStyles(btn);
        return btn;
    }

    private void drawDungeonBackground(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        SpriteSheet walls = new SpriteSheet("/assets/walls_x3.png", 48);
        gc.setFill(Color.web("#0a0a14"));
        gc.fillRect(0, 0, W, H);
        if (walls.isLoaded()) {
            for (int y = 0; y <= H; y += 48) {
                for (int x = 0; x <= W; x += 48) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    javafx.scene.image.Image tile = walls.getSprite(hash % 4, 3);
                    if (tile != null) gc.drawImage(tile, x, y, 48, 48);
                }
            }
        }
    }
}