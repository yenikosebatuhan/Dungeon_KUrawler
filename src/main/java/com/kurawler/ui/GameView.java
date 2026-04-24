package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.Consumable;
import com.kurawler.model.Item;
import com.kurawler.model.Key;
import com.kurawler.model.StaticContainer;
import com.kurawler.model.Weapon;
import com.kurawler.model.object.GameObject;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * JavaFX game view — renders the grid and handles key input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    private static final int SIDEBAR_WIDTH = 250;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Canvas canvas;
    private Label statusLabel;
    private Label inventoryLabel;
    private Label messageLabel;

    public GameView(GameController controller, ScreenManager screenManager) {
        this.controller = controller;
        this.screenManager = screenManager;
        this.renderer = new TileRenderer(TILE_SIZE);
    }

    /** Build and show the game window. */
    public void show(Stage stage) {
        GameSession session = controller.getSession();
        int mapWidth = session.getMap().getWidth();
        int mapHeight = session.getMap().getHeight();

        int canvasW = mapWidth * TILE_SIZE;
        int canvasH = mapHeight * TILE_SIZE;

        canvas = new Canvas(canvasW, canvasH);

        // Sidebar
        statusLabel = new Label();
        statusLabel.setFont(Font.font("Monospaced", 13));
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setWrapText(true);

        inventoryLabel = new Label();
        inventoryLabel.setFont(Font.font("Monospaced", 12));
        inventoryLabel.setTextFill(Color.LIGHTGRAY);
        inventoryLabel.setWrapText(true);

        messageLabel = new Label("Use WASD or Arrow Keys to move.\nWalk over items to pick them up.");
        messageLabel.setFont(Font.font("Monospaced", 11));
        messageLabel.setTextFill(Color.web("#aaaaaa"));
        messageLabel.setWrapText(true);

        VBox sidebar = new VBox(12, statusLabel, inventoryLabel, messageLabel);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setStyle("-fx-padding: 12; -fx-background-color: #1a1a2e;");

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(sidebar);
        root.setStyle("-fx-background-color: #0f0f23;");

        Scene scene = new Scene(root, canvasW + SIDEBAR_WIDTH, canvasH);
        scene.setOnKeyPressed(this::handleKeyPress);

        stage.setTitle("Dungeon KUrawler");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        render();
    }

    /** Capture key input and delegate to controller. No logic here. */
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W: case UP:
                controller.moveHero(-1, 0);
                break;
            case S: case DOWN:
                controller.moveHero(1, 0);
                break;
            case A: case LEFT:
                controller.moveHero(0, -1);
                break;
            case D: case RIGHT:
                controller.moveHero(0, 1);
                break;
            default:
                return;
        }
        render();
    }

    /** Render the entire game state by reading the model. */
    private void render() {
        GameSession session = controller.getSession();
        com.kurawler.model.Map map = session.getMap();
        Hero hero = session.getHero();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int mapW = map.getWidth();
        int mapH = map.getHeight();

        // Clear
        gc.setFill(Color.web("#0f0f23"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw grid
        for (int r = 0; r < mapH; r++) {
            for (int c = 0; c < mapW; c++) {
                int sx = c * TILE_SIZE;
                int sy = r * TILE_SIZE;
                GridCell cell = map.getCell(r, c);

                // Floor or wall
                if (!cell.isWalkable()) {
                    renderer.drawWall(gc, sx, sy);
                } else {
                    renderer.drawFloor(gc, sx, sy);
                }

                // Objects on cell
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) {
                        renderer.drawContainer(gc, sx, sy);
                    } else if (obj instanceof Weapon) {
                        renderer.drawItem(gc, sx, sy, "Weapon");
                    } else if (obj instanceof Consumable) {
                        renderer.drawItem(gc, sx, sy, "Consumable");
                    } else if (obj instanceof Key) {
                        renderer.drawItem(gc, sx, sy, "Key");
                    } else if (obj instanceof Item) {
                        renderer.drawItem(gc, sx, sy, "Item");
                    }
                }
            }
        }

        // Draw hero on top
        int heroSx = hero.getCol() * TILE_SIZE;
        int heroSy = hero.getRow() * TILE_SIZE;
        renderer.drawHero(gc, heroSx, heroSy);

        // Grid lines (subtle)
        gc.setStroke(Color.web("#ffffff", 0.06));
        gc.setLineWidth(1);
        for (int r = 0; r <= mapH; r++) {
            gc.strokeLine(0, r * TILE_SIZE, mapW * TILE_SIZE, r * TILE_SIZE);
        }
        for (int c = 0; c <= mapW; c++) {
            gc.strokeLine(c * TILE_SIZE, 0, c * TILE_SIZE, mapH * TILE_SIZE);
        }

        // Update sidebar
        updateSidebar(session);
    }

    private void updateSidebar(GameSession session) {
        Hero hero = session.getHero();
        String wpn = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "None";
        statusLabel.setText(
            "  " + hero.getName() + "\n"
            + "  HP:  " + hero.getHealth() + " / " + hero.getMaxHealth() + "\n"
            + "  MP:  " + hero.getMana() + " / " + hero.getMaxMana() + "\n"
            + "  NRG: " + hero.getEnergy() + " / " + hero.getMaxEnergy() + "\n"
            + "  STR: " + hero.getStrength() + "  DEF: " + hero.getDefensePower() + "\n"
            + "  DMG: " + hero.calculateDamage() + "\n"
            + "  Weapon: " + wpn + "\n"
            + "  Pos: (" + hero.getRow() + "," + hero.getCol() + ")"
            + "  Turn: " + session.getTurnCount()
        );

        Inventory inv = hero.getInventory();
        StringBuilder sb = new StringBuilder("  Inventory (" + inv.getItemCount() + "/" + Inventory.MAX_CAPACITY + "):\n");
        for (int r = 0; r < Inventory.ROWS; r++) {
            sb.append("  ");
            for (int c = 0; c < Inventory.COLS; c++) {
                Item item = inv.getItem(r, c);
                if (item != null) {
                    sb.append("[").append(item.getName()).append("] ");
                } else {
                    sb.append("[  --  ] ");
                }
            }
            sb.append("\n");
        }
        inventoryLabel.setText(sb.toString());
    }
}
