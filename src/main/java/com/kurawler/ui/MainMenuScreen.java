package com.kurawler.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import com.kurawler.engine.AudioManager;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class MainMenuScreen {

    private static final int W = ScreenManager.WINDOW_W;
    private static final int H = ScreenManager.WINDOW_H;
    private static final int BG_TILE = 48;

    private final ScreenManager screenManager;

    public MainMenuScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public Scene createScene() {
        Canvas bg = new Canvas(W, H);
        drawDungeonBackground(bg);

        Pane darkOverlay = new Pane();
        darkOverlay.setPrefSize(W, H);
        darkOverlay.setStyle("-fx-background-color: "
                + "radial-gradient(center 50% 30%, radius 70%, rgba(0,0,0,0.15), rgba(0,0,0,0.85));");

        Label title = new Label("DUNGEON\nKURAWLER");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 50));
        title.setTextFill(Color.web("#e8c84a"));
        title.setTextAlignment(TextAlignment.CENTER);
        InnerShadow engrave = new InnerShadow(4, 1, 2, Color.web("#5a4210"));
        DropShadow outerGlow = new DropShadow(30, Color.web("#ff9500", 0.5));
        outerGlow.setInput(engrave);
        DropShadow deepShadow = new DropShadow(8, 3, 4, Color.web("#000000", 0.8));
        deepShadow.setInput(outerGlow);
        title.setEffect(deepShadow);

        Label subtitle = new Label("\u2620  A COMP 302 Dungeon Crawler  \u2620");
        subtitle.setFont(Font.font("Serif", FontWeight.NORMAL, 13));
        subtitle.setTextFill(Color.web("#8a7a55"));
        subtitle.setEffect(new DropShadow(4, 1, 1, Color.web("#000000", 0.7)));

        Label ornament = new Label("\u2501\u2501\u2501\u2501 \u2726 \u2726 \u2726 \u2501\u2501\u2501\u2501");
        ornament.setFont(Font.font("Monospaced", 16));
        ornament.setTextFill(Color.web("#8b6914", 0.7));
        ornament.setEffect(new DropShadow(4, 0, 1, Color.web("#000000", 0.5)));

        Button newGameBtn = stoneButton("\u2694  New Game");
        newGameBtn.setOnAction(e -> screenManager.startNewGame());

        Button customMapBtn = stoneButton("\u2694  New Game (Custom Map)");
        customMapBtn.setOnAction(e -> screenManager.startNewGameFromCustomMap());

        Button randomBtn = stoneButton("\u2728  Random Dungeon");
        randomBtn.setOnAction(e -> screenManager.startRandomDungeon());

        Button teamMatchBtn = stoneButton("\u2693  Team Match");
        teamMatchBtn.setOnAction(e -> screenManager.startTeamMatch());

        Button editorBtn = stoneButton("\u270F  Level Editor");
        editorBtn.setOnAction(e -> screenManager.showBuildMode());

        Label ornament2 = new Label("\u2501\u2501\u2501\u2501 \u2726 \u2726 \u2726 \u2501\u2501\u2501\u2501");
        ornament2.setFont(Font.font("Monospaced", 16));
        ornament2.setTextFill(Color.web("#8b6914", 0.7));
        ornament2.setEffect(new DropShadow(4, 0, 1, Color.web("#000000", 0.5)));

        Button loadBtn = smallStoneButton("Load");
        loadBtn.setOnAction(e -> screenManager.showLoadDialogAndStart());

        Button helpBtn = smallStoneButton("Help");
        helpBtn.setOnAction(e -> screenManager.showHelp());

        Button exitBtn = smallStoneButton("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        Button musicBtn = smallStoneButton("Music: " + (AudioManager.getInstance().isMusicEnabled() ? "ON" : "OFF"));
        musicBtn.setOnAction(e -> {
            AudioManager audio = AudioManager.getInstance();
            boolean newState = !audio.isMusicEnabled();
            audio.setMusicEnabled(newState);
            musicBtn.setText("Music: " + (newState ? "ON" : "OFF"));
            audio.play("button");
            if (newState) {
                audio.playMusic("menu", true);
            }
        });

        HBox bottomRow = new HBox(12, loadBtn, musicBtn, helpBtn, exitBtn);
        bottomRow.setAlignment(Pos.CENTER);

        Label version = new Label("v2.0 \u2022 KU Spring 2026");
        version.setFont(Font.font("Monospaced", 10));
        version.setTextFill(Color.web("#5a5a66"));
        version.setEffect(new DropShadow(2, 0, 1, Color.BLACK));

        VBox menu = new VBox(14,
                title, subtitle, ornament,
                newGameBtn, customMapBtn, randomBtn, teamMatchBtn, editorBtn,
                ornament2, bottomRow, version);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(40, 40, 24, 40));

        StackPane root = new StackPane(bg, darkOverlay, menu);
        return new Scene(root, W, H);
    }

    private void drawDungeonBackground(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        SpriteSheet walls = new SpriteSheet("/assets/walls_x3.png", BG_TILE);

        gc.setFill(Color.web("#0a0a14"));
        gc.fillRect(0, 0, W, H);

        if (!walls.isLoaded()) return;

        Image[] wallTiles = {
                walls.getSprite(0, 1), walls.getSprite(1, 1),
                walls.getSprite(2, 1), walls.getSprite(3, 1),
                walls.getSprite(4, 1)
        };
        Image[] floorTiles = {
                walls.getSprite(0, 3), walls.getSprite(1, 3),
                walls.getSprite(2, 3), walls.getSprite(3, 3)
        };

        for (int y = 0; y < H; y += BG_TILE) {
            for (int x = 0; x < W; x += BG_TILE) {
                int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                boolean isWall = (y < BG_TILE * 2) || (y > H - BG_TILE * 2)
                        || (x < BG_TILE) || (x > W - BG_TILE * 2)
                        || hash % 7 == 0;

                Image tile;
                if (isWall) {
                    tile = wallTiles[hash % wallTiles.length];
                } else {
                    tile = floorTiles[hash % floorTiles.length];
                }
                if (tile != null) {
                    gc.drawImage(tile, x, y, BG_TILE, BG_TILE);
                }
            }
        }

        gc.setFill(Color.web("#0a0a14", 0.55));
        gc.fillRect(0, 0, W, H);
    }

    private Button stoneButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        btn.setPrefWidth(320);
        btn.setPrefHeight(50);
        btn.setAlignment(Pos.CENTER);

        String base = "-fx-background-color: "
                + "linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16, #2a2720, #3a362e); "
                + "-fx-text-fill: #c8b880; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: #8b7940 #5a4e30 #3a3220 #8b7940; "
                + "-fx-border-width: 2; "
                + "-fx-cursor: hand; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.5), 6, 0, 0, 3);";

        String hover = "-fx-background-color: "
                + "linear-gradient(to bottom, #5a5448, #3e3a30, #2a2720, #353028, #4a4638); "
                + "-fx-text-fill: #f0e0a0; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: #c8a850 #8b7940 #5a4e30 #c8a850; "
                + "-fx-border-width: 2; "
                + "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(200,168,80,0.3), 12, 0, 0, 0);";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button smallStoneButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        btn.setPrefWidth(90);
        btn.setPrefHeight(34);
        btn.setAlignment(Pos.CENTER);

        String base = "-fx-background-color: "
                + "linear-gradient(to bottom, #3a3830, #252320, #1a1918, #252320); "
                + "-fx-text-fill: #9a9080; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; "
                + "-fx-border-color: #6a6050 #3a3830 #2a2820 #6a6050; "
                + "-fx-border-width: 1.5; "
                + "-fx-cursor: hand; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 2);";

        String hover = "-fx-background-color: "
                + "linear-gradient(to bottom, #4a4840, #353330, #2a2928, #353330); "
                + "-fx-text-fill: #e0d0a0; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; "
                + "-fx-border-color: #a08850 #6a6050 #3a3830 #a08850; "
                + "-fx-border-width: 1.5; "
                + "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(180,150,70,0.25), 8, 0, 0, 0);";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }
}
