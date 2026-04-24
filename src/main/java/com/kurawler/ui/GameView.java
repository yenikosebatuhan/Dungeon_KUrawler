package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.GameLoop;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * JavaFX game view — renders the grid and handles key/mouse input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    private static final int SIDEBAR_WIDTH = 250;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Stage stage;
    private Canvas canvas;
    private Label statusLabel;
    private Label inventoryLabel;
    private Label messageLabel;
    private GameLoop gameLoop;
    private AnimationTimer animationTimer;

    public GameView(GameController controller, ScreenManager screenManager) {
        this.controller = controller;
        this.screenManager = screenManager;
        this.renderer = new TileRenderer(TILE_SIZE);
    }

    /** Build and show the game window. */
    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        GameSession session = controller.getSession();
        int mapWidth = session.getMap().getWidth();
        int mapHeight = session.getMap().getHeight();

        int canvasW = mapWidth * TILE_SIZE;
        int canvasH = mapHeight * TILE_SIZE;

        canvas = new Canvas(canvasW, canvasH);
        canvas.setOnMouseClicked(this::handleMouseClick);

        statusLabel = new Label();
        statusLabel.setFont(Font.font("Monospaced", 13));
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setWrapText(true);

        inventoryLabel = new Label();
        inventoryLabel.setFont(Font.font("Monospaced", 12));
        inventoryLabel.setTextFill(Color.LIGHTGRAY);
        inventoryLabel.setWrapText(true);

        messageLabel = new Label("WASD/Arrows: Move | E: Use Potion\nClick adjacent tile to interact.");
        messageLabel.setFont(Font.font("Monospaced", 11));
        messageLabel.setTextFill(Color.web("#aaaaaa"));
        messageLabel.setWrapText(true);

        Button menuBtn = new Button("Menu");
        menuBtn.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        menuBtn.setPrefWidth(220);
        menuBtn.setStyle("-fx-background-color: #2a1a3e; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #664466; -fx-border-width: 1; -fx-cursor: hand;");
        menuBtn.setOnAction(e -> showInGameMenu());

        VBox sidebar = new VBox(12, statusLabel, inventoryLabel, messageLabel, menuBtn);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setStyle("-fx-padding: 12; -fx-background-color: #1a1a2e;");

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(sidebar);
        root.setStyle("-fx-background-color: #0f0f23;");

        Scene scene = new Scene(root, canvasW + SIDEBAR_WIDTH, canvasH);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        stage.setTitle("Dungeon KUrawler");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        session.setCurrentMode("Play");
        startGameLoop(session);
        render();
    }

    private void startGameLoop(GameSession session) {
        gameLoop = new GameLoop(session);
        final long[] lastTime = {System.nanoTime()};

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long deltaMs = (now - lastTime[0]) / 1_000_000;
                lastTime[0] = now;
                gameLoop.update(deltaMs);
                render();
            }
        };
        animationTimer.start();
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W: case UP:    controller.moveHero(-1,  0); break;
            case S: case DOWN:  controller.moveHero( 1,  0); break;
            case A: case LEFT:  controller.moveHero( 0, -1); break;
            case D: case RIGHT: controller.moveHero( 0,  1); break;
            case E:             controller.useFirstConsumable(); break;
            case ESCAPE:        showInGameMenu(); return;
            default:            return;
        }
        render();
    }

    /** 3x3 Interaction: click an adjacent cell to interact with objects on it. */
    private void handleMouseClick(MouseEvent event) {
        int col = (int) (event.getX() / TILE_SIZE);
        int row = (int) (event.getY() / TILE_SIZE);

        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();

        int dRow = Math.abs(row - hero.getRow());
        int dCol = Math.abs(col - hero.getCol());
        if (dRow > 1 || dCol > 1 || (dRow == 0 && dCol == 0)) {
            messageLabel.setText("Too far to interact!\nMust be adjacent (3x3).");
            return;
        }

        GridCell cell = map.getCell(row, col);
        if (cell == null) return;

        ContextMenu contextMenu = new ContextMenu();
        boolean hasAction = false;

        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof Item) {
                Item item = (Item) obj;
                MenuItem takeItem = new MenuItem("TAKE: " + item.getName());
                takeItem.setOnAction(e -> {
                    controller.interactWithObject(row, col, item);
                    messageLabel.setText("Picked up " + item.getName());
                    render();
                });
                contextMenu.getItems().add(takeItem);
                hasAction = true;
            }
        }

        StaticObject staticObj = cell.getStaticObject();
        if (staticObj != null && staticObj.isSearchable()) {
            MenuItem searchItem = new MenuItem("SEARCH: " + staticObj.getName());
            searchItem.setOnAction(e -> {
                messageLabel.setText("Searched " + staticObj.getName() + "... nothing found.");
                render();
            });
            contextMenu.getItems().add(searchItem);
            hasAction = true;
        }

        if (cell.getOccupyingCharacter() instanceof Enemy) {
            Enemy enemy = (Enemy) cell.getOccupyingCharacter();
            MenuItem attackItem = new MenuItem("ATTACK: " + enemy.getName());
            attackItem.setOnAction(e -> {
                hero.attack(enemy);
                if (!enemy.isAlive()) {
                    cell.setOccupyingCharacter(null);
                    session.decrementEnemyCount();
                    messageLabel.setText(enemy.getName() + " defeated!");
                } else {
                    messageLabel.setText("Hit " + enemy.getName()
                            + "! HP: " + enemy.getHealth());
                }
                render();
            });
            contextMenu.getItems().add(attackItem);
            hasAction = true;
        }

        if (!hasAction) {
            messageLabel.setText("Nothing to interact with here.");
            return;
        }

        double screenX = event.getScreenX();
        double screenY = event.getScreenY();
        contextMenu.show(canvas, screenX, screenY);
    }

    private void showInGameMenu() {
        if (animationTimer != null) animationTimer.stop();

        ButtonType save    = new ButtonType("Save");
        ButtonType load    = new ButtonType("Load");
        ButtonType mainMenu = new ButtonType("Main Menu");
        ButtonType resume  = new ButtonType("Resume");

        Alert alert = new Alert(Alert.AlertType.NONE,
                "Press Save to save your progress,\nLoad to restore a save, or\nMain Menu to quit.",
                save, load, mainMenu, resume);
        alert.setTitle("Dungeon KUrawler");
        alert.setHeaderText("Game Menu");
        alert.initOwner(stage);

        alert.showAndWait().ifPresent(choice -> {
            if (choice == save) {
                controller.saveMap("map_save.json");
                messageLabel.setText("Game saved.");
            } else if (choice == load) {
                controller.loadMap("map_save.json");
                messageLabel.setText("Game loaded.");
                render();
            } else if (choice == mainMenu) {
                screenManager.showMainMenu();
                return;
            }
        });

        if (animationTimer != null) animationTimer.start();
    }

    /** Render the entire game state by reading the model. */
    private void render() {
        GameSession session = controller.getSession();
        com.kurawler.model.Map map = session.getMap();
        Hero hero = session.getHero();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int mapW = map.getWidth();
        int mapH = map.getHeight();

        gc.setFill(Color.web("#0f0f23"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int r = 0; r < mapH; r++) {
            for (int c = 0; c < mapW; c++) {
                int sx = c * TILE_SIZE;
                int sy = r * TILE_SIZE;
                GridCell cell = map.getCell(r, c);

                if (!cell.isWalkable()) {
                    renderer.drawWall(gc, sx, sy);
                } else {
                    renderer.drawFloor(gc, sx, sy);
                }

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

                // Draw enemies on occupied cells
                if (cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                    if (enemy.isAlive()) {
                        String type = (enemy instanceof Knight) ? "Knight" : "Sorcerer";
                        renderer.drawEnemy(gc, sx, sy, type);
                    }
                }
            }
        }

        int heroSx = hero.getCol() * TILE_SIZE;
        int heroSy = hero.getRow() * TILE_SIZE;
        renderer.drawHero(gc, heroSx, heroSy);

        gc.setStroke(Color.web("#ffffff", 0.06));
        gc.setLineWidth(1);
        for (int r = 0; r <= mapH; r++) {
            gc.strokeLine(0, r * TILE_SIZE, mapW * TILE_SIZE, r * TILE_SIZE);
        }
        for (int c = 0; c <= mapW; c++) {
            gc.strokeLine(c * TILE_SIZE, 0, c * TILE_SIZE, mapH * TILE_SIZE);
        }

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
            + "\n  Enemies: " + session.getActiveEnemyCount()
        );

        Inventory inv = hero.getInventory();
        StringBuilder sb = new StringBuilder("  Inventory (" + inv.getItemCount() + "/8):\n");
        for (int r = 0; r < 2; r++) {
            sb.append("  ");
            for (int c = 0; c < 4; c++) {
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
