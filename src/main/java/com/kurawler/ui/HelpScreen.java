package com.kurawler.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Help screen — PBI-19: key bindings and rules, dungeon theme matching main menu.
 */
public class HelpScreen {

    private static final int W = 560;
    private static final int H = 680;
    private static final int BG_TILE = 48;

    private final ScreenManager screenManager;

    public HelpScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public Scene createScene() {
        Canvas bg = new Canvas(W, H);
        drawDungeonBackground(bg);

        Pane darkOverlay = new Pane();
        darkOverlay.setPrefSize(W, H);
        darkOverlay.setStyle("-fx-background-color: "
                + "radial-gradient(center 50% 30%, radius 70%, rgba(0,0,0,0.2), rgba(0,0,0,0.88));");

        Label title = new Label("HELP & CONTROLS");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 34));
        title.setTextFill(Color.web("#e8c84a"));
        title.setTextAlignment(TextAlignment.CENTER);
        InnerShadow engrave = new InnerShadow(3, 1, 2, Color.web("#5a4210"));
        DropShadow glow = new DropShadow(18, Color.web("#c8a850", 0.45));
        glow.setInput(engrave);
        title.setEffect(glow);

        Label subtitle = new Label("Dungeon KUrawler — how to survive");
        subtitle.setFont(Font.font("Serif", 13));
        subtitle.setTextFill(Color.web("#9a9080"));

        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(8, 4, 8, 4));
        contentBox.setMaxWidth(460);

        addSection(contentBox, "GOAL",
                "Find the target artifact shown at the start of a run.\n"
                + "Story maps: Golden Key is hidden in a shelf (Entry Hall or Catacombs);\n"
                + "the win item is inside a chest in the Treasure Vault.");
        addSection(contentBox, "MOVEMENT",
                "W / A / S / D  or  Arrow Keys — move one tile\n"
                + "Each step costs energy.");
        addSection(contentBox, "COMBAT",
                "Space — attack facing direction (melee, bow, bomb; unarmed punch if no weapon)\n"
                + "Bow + arrows: equipping arrows gives 3 shots; flies in facing direction\n"
                + "Bomb: up to 5 tiles; adjacent blast hurts you too\n"
                + "Flail Ball: melee gürz — 1 tile in facing direction (Space)\n"
                + "Staff / scepter: F — mana spell (20 MP, stronger bolt); no other weapon while equipped\n"
                + "F — mana bolt without staff (10 MP)");
        addSection(contentBox, "INVENTORY",
                "I — open inventory popup\n"
                + "E — door, chest, take item, or use potion (priority order)\n"
                + "Click ground tile (adjacent) — TAKE / OPEN / SEARCH / BREAK / ATTACK");
        addSection(contentBox, "SPECIAL ABILITIES",
                "C — character sheet (stats & equipment)\n"
                + "Shadow Clone Scroll — READ from inventory");
        addSection(contentBox, "GAME FLOW",
                "P — pause / resume\n"
                + "Esc — in-game menu (save, load, main menu)\n"
                + "HP reaches 0 — game over\n"
                + "Collect the target item — victory");

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background: transparent; -fx-background-color: rgba(14,12,10,0.75); "
                + "-fx-border-color: #8b7940; -fx-border-width: 2; -fx-border-radius: 6; "
                + "-fx-background-radius: 6;");

        Button backBtn = stoneButton("\u2190  Back to Menu");
        backBtn.setOnAction(e -> screenManager.showMainMenu());

        VBox layout = new VBox(14, title, subtitle, scrollPane, backBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(36, 40, 28, 40));

        StackPane root = new StackPane(bg, darkOverlay, layout);
        return new Scene(root, W, H);
    }

    private void addSection(VBox parent, String heading, String body) {
        Label h = new Label(heading);
        h.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        h.setTextFill(Color.web("#e8c84a"));
        h.setPadding(new Insets(10, 0, 2, 0));

        Label b = new Label(body);
        b.setFont(Font.font("Monospaced", 12));
        b.setTextFill(Color.web("#c8b880"));
        b.setWrapText(true);
        b.setLineSpacing(3);
        b.setPadding(new Insets(0, 0, 4, 10));

        parent.getChildren().addAll(h, b);
    }

    private Button stoneButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        btn.setPrefWidth(240);
        btn.setPrefHeight(44);
        String base = "-fx-background-color: "
                + "linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16); "
                + "-fx-text-fill: #c8b880; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: #8b7940 #5a4e30 #3a3220 #8b7940; -fx-border-width: 2; -fx-cursor: hand;";
        String hover = "-fx-background-color: "
                + "linear-gradient(to bottom, #5a5448, #3e3a30, #2a2720); "
                + "-fx-text-fill: #f0e0a0; -fx-border-color: #c8a850; "
                + "-fx-effect: dropshadow(gaussian, rgba(200,168,80,0.35), 10, 0, 0, 0);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void drawDungeonBackground(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        SpriteSheet walls = new SpriteSheet("/assets/walls_x3.png", BG_TILE);
        gc.setFill(Color.web("#0a0a14"));
        gc.fillRect(0, 0, W, H);
        if (!walls.isLoaded()) {
            return;
        }
        Image[] wallTiles = {
                walls.getSprite(0, 1), walls.getSprite(1, 1),
                walls.getSprite(2, 1), walls.getSprite(3, 1)
        };
        Image[] floorTiles = {
                walls.getSprite(0, 3), walls.getSprite(1, 3),
                walls.getSprite(2, 3), walls.getSprite(3, 3)
        };
        for (int y = 0; y < H; y += BG_TILE) {
            for (int x = 0; x < W; x += BG_TILE) {
                int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                boolean isWall = y < BG_TILE * 2 || y > H - BG_TILE * 2
                        || x < BG_TILE || x > W - BG_TILE * 2 || hash % 7 == 0;
                Image tile = isWall ? wallTiles[hash % wallTiles.length]
                        : floorTiles[hash % floorTiles.length];
                if (tile != null) {
                    gc.drawImage(tile, x, y, BG_TILE, BG_TILE);
                }
            }
        }
        gc.setFill(Color.web("#0a0a14", 0.5));
        gc.fillRect(0, 0, W, H);
    }
}
