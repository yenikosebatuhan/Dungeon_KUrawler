package com.kurawler.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
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
        Canvas bg = new Canvas(W, H);
        drawDungeonBackground(bg);

        Label title = new Label("SELECT DIFFICULTY");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 50));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new DropShadow(15, Color.web("#e8c84a", 0.4)));

        Label descLabel = new Label("Normal: Default enemy stats and starting energy.");
        descLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        descLabel.setTextFill(Color.web("#d0d0e0"));
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(700);
        descLabel.setPrefHeight(80);

        Button easyBtn = stoneButton("Easy");
        Button normalBtn = stoneButton("Normal");
        Button hardBtn = stoneButton("Hard");

        // Set Default selection highlight
        normalBtn.setStyle(normalBtn.getStyle() + "-fx-border-color: #55efc4;");

        easyBtn.setOnAction(e -> {
            selectedDifficulty = "Easy";
            descLabel.setText("Easy: Enemies have -30% HP and ATK. Hero starts with 150 Energy.");
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            easyBtn.setStyle(easyBtn.getStyle() + "-fx-border-color: #55efc4;");
        });

        normalBtn.setOnAction(e -> {
            selectedDifficulty = "Normal";
            descLabel.setText("Normal: Default enemy stats and starting energy.");
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            normalBtn.setStyle(normalBtn.getStyle() + "-fx-border-color: #55efc4;");
        });

        hardBtn.setOnAction(e -> {
            selectedDifficulty = "Hard";
            descLabel.setText("Hard: Enemies have +30% HP and ATK. Hero starts with 75 Energy. No Health Potions on the map!");
            resetButtonStyles(easyBtn, normalBtn, hardBtn);
            hardBtn.setStyle(hardBtn.getStyle() + "-fx-border-color: #ff4a4a;");
        });

        HBox diffBox = new HBox(30, easyBtn, normalBtn, hardBtn);
        diffBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("CONFIRM & START");
        confirmBtn.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        confirmBtn.setPrefWidth(300);
        confirmBtn.setPrefHeight(60);
        confirmBtn.setStyle("-fx-background-color: #e8c84a; -fx-text-fill: #15110c; -fx-background-radius: 6; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> screenManager.launchGameWithDifficulty(isRandomMap, selectedDifficulty));

        Button backBtn = new Button("\u2190 Back");
        backBtn.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c8b880; -fx-cursor: hand;");
        backBtn.setOnAction(e -> screenManager.showMainMenu());

        VBox layout = new VBox(40, title, diffBox, descLabel, confirmBtn, backBtn);
        layout.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(bg, layout);
        return new Scene(root, W, H);
    }

    private void resetButtonStyles(Button... btns) {
        String base = "-fx-background-color: linear-gradient(to bottom, #4a4438, #2e2a22); "
                + "-fx-text-fill: #c8b880; -fx-background-radius: 6; -fx-border-radius: 6; "
                + "-fx-border-color: #8b7940; -fx-border-width: 2; -fx-cursor: hand;";
        for (Button b : btns) b.setStyle(base);
    }

    private Button stoneButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        btn.setPrefWidth(200);
        btn.setPrefHeight(60);
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
        gc.setFill(Color.web("#0a0a14", 0.80));
        gc.fillRect(0, 0, W, H);
    }
}