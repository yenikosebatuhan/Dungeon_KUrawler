package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.GameLoop;
import com.kurawler.engine.TeamMatchGameLoop;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.achievement.Achievement;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

/**
 * JavaFX game view — renders the grid and handles key/mouse input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    private static final int SIDEBAR_WIDTH = 340;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Stage stage;
    private Canvas canvas;
    private Label statusLabel;
    private Label inventoryLabel;
    private Label messageLabel;
    private Label achievementsLabel;
    private Label achievementPopupLabel;
    private GameLoop gameLoop;
    private TeamMatchGameLoop teamMatchGameLoop;
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

        achievementsLabel = new Label();
        achievementsLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        achievementsLabel.setTextFill(Color.web("#ffd54f"));
        achievementsLabel.setWrapText(true);
        achievementsLabel.setPrefWidth(SIDEBAR_WIDTH - 24);
        achievementsLabel.setStyle("-fx-padding: 8; -fx-border-color: #6c5ce7; "
                + "-fx-border-width: 1; -fx-background-color: #201f3f;");

        achievementPopupLabel = new Label();
        achievementPopupLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        achievementPopupLabel.setTextFill(Color.web("#ffd54f"));
        achievementPopupLabel.setWrapText(true);
        achievementPopupLabel.setVisible(false);

        Button menuBtn = new Button("Menu");
        menuBtn.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        menuBtn.setPrefWidth(220);
        menuBtn.setStyle("-fx-background-color: #2a1a3e; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #664466; -fx-border-width: 1; -fx-cursor: hand;");
        menuBtn.setOnAction(e -> showInGameMenu());

        AchievementManager achievementManager = session.getAchievementManager();
        if (achievementManager != null) {
            achievementManager.setOnUnlock(this::showAchievementPopup);
        }

        VBox sidebar = new VBox(12, statusLabel, achievementsLabel, inventoryLabel,
                messageLabel, achievementPopupLabel, menuBtn);
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

        if (!(session instanceof TeamMatchSession)) {
            session.setCurrentMode("Play");
        }
        startGameLoop(session);
        render();
    }

    private void startGameLoop(GameSession session) {
        final long[] lastTime = {System.nanoTime()};

        if (session instanceof TeamMatchSession) {
            TeamMatchSession tmSession = (TeamMatchSession) session;
            teamMatchGameLoop = new TeamMatchGameLoop(tmSession);
            teamMatchGameLoop.start();

            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long deltaMs = (now - lastTime[0]) / 1_000_000;
                    lastTime[0] = now;
                    teamMatchGameLoop.update(deltaMs);
                    render();

                    if (!teamMatchGameLoop.isRunning() && tmSession.getMatchResult() != null) {
                        animationTimer.stop();
                        showTeamMatchResult(tmSession.getMatchResult());
                    }
                }
            };
        } else {
            gameLoop = new GameLoop(session);
            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long deltaMs = (now - lastTime[0]) / 1_000_000;
                    lastTime[0] = now;
                    gameLoop.update(deltaMs);
                    render();
                }
            };
        }
        animationTimer.start();
    }

    private void showTeamMatchResult(String result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, result);
        alert.setTitle("Team Match Over");
        alert.setHeaderText("Match Result");
        alert.initOwner(stage);
        alert.showAndWait();
        screenManager.showMainMenu();
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
                boolean loaded = controller.loadMap("map_save.json");
                if (loaded) {
                    animationTimer.stop();
                    animationTimer = null;
                    screenManager.showPlayMode(controller.getSession());
                    return;
                } else {
                    messageLabel.setText("No save file found.");
                }
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

                StaticObject so = cell.getStaticObject();
                if (so != null && !so.isPassable()) {
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

                Character occupant = cell.getOccupyingCharacter();
                if (occupant != null && occupant.isAlive() && !(occupant instanceof Hero)) {
                    String type = (occupant instanceof Knight) ? "Knight" : "Sorcerer";
                    renderer.drawEnemy(gc, sx, sy, type);

                    if (session instanceof TeamMatchSession) {
                        TeamMatchSession tmSession = (TeamMatchSession) session;
                        Team team = tmSession.getTeamForCharacter(occupant);
                        if (team != null) {
                            drawTeamIndicator(gc, sx, sy, team.getColor());
                        }
                    }
                }
            }
        }

        ShadowClone shadowClone = session.getShadowClone();
        if (shadowClone != null && shadowClone.isAlive()) {
            renderer.drawShadowClone(gc, shadowClone.getCol() * TILE_SIZE,
                    shadowClone.getRow() * TILE_SIZE);
        }

        int heroSx = hero.getCol() * TILE_SIZE;
        int heroSy = hero.getRow() * TILE_SIZE;
        renderer.drawHero(gc, heroSx, heroSy);

        if (session instanceof TeamMatchSession) {
            drawTeamIndicator(gc, heroSx, heroSy, Team.TeamColor.BLUE);
        }

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

    private void showAchievementPopup(Achievement achievement) {
        achievementPopupLabel.setText("Achievement unlocked:\n" + achievement.getName());
        achievementPopupLabel.setVisible(true);

        PauseTransition hide = new PauseTransition(Duration.seconds(3));
        hide.setOnFinished(e -> achievementPopupLabel.setVisible(false));
        hide.play();
    }

    private void updateSidebar(GameSession session) {
        Hero hero = session.getHero();
        String wpn = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "None";

        if (session instanceof TeamMatchSession) {
            TeamMatchSession tm = (TeamMatchSession) session;
            statusLabel.setText(
                "  *** TEAM MATCH ***\n\n"
                + "  YOU: " + hero.getName() + "\n"
                + "  HP: " + hero.getHealth() + "/" + hero.getMaxHealth()
                + "  STR: " + hero.getStrength() + "\n"
                + "  DEF: " + hero.getDefensePower()
                + "  Weapon: " + wpn + "\n"
            );
            inventoryLabel.setText(
                "  \u25CF Team A (RED)  ["
                + tm.getTeamA().aliveCount() + "/4 alive]\n"
                + teamRoster(tm.getTeamA())
                + "\n  \u25CF Team B (BLUE) ["
                + tm.getTeamB().aliveCount() + "/4 alive]\n"
                + teamRoster(tm.getTeamB())
            );
        } else {
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
        updateAchievements(session);
    }

    private void drawTeamIndicator(GraphicsContext gc, int sx, int sy, Team.TeamColor teamColor) {
        Color borderColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(255, 60, 60, 0.9)
                : Color.rgb(60, 120, 255, 0.9);
        Color dotColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(255, 80, 80)
                : Color.rgb(80, 140, 255);

        gc.setStroke(borderColor);
        gc.setLineWidth(2.5);
        gc.strokeRect(sx + 1, sy + 1, TILE_SIZE - 2, TILE_SIZE - 2);

        gc.setFill(dotColor);
        gc.fillOval(sx + TILE_SIZE - 12, sy + 2, 9, 9);
        gc.setStroke(Color.web("#000000", 0.6));
        gc.setLineWidth(1);
        gc.strokeOval(sx + TILE_SIZE - 12, sy + 2, 9, 9);
    }

    private String teamRoster(Team team) {
        StringBuilder sb = new StringBuilder();
        for (Character member : team.getMembers()) {
            String icon = (member instanceof Hero) ? "  \u2694 "
                    : (member instanceof Sorcerer) ? "  \u2728 " : "  \u2620 ";
            if (member.isAlive()) {
                sb.append(icon).append(member.getName())
                  .append("  HP:").append(member.getHealth())
                  .append("/").append(member.getMaxHealth()).append("\n");
            } else {
                sb.append(icon).append(member.getName())
                  .append("  -- DEAD --\n");
            }
        }
        return sb.toString();
    }

    private void updateAchievements(GameSession session) {
        AchievementManager manager = session.getAchievementManager();
        if (manager == null) {
            achievementsLabel.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder("  BADGES / ACHIEVEMENTS\n");
        for (Achievement achievement : manager.getAchievements()) {
            sb.append("  ")
              .append(achievement.isUnlocked() ? "[UNLOCKED] " : "[LOCKED]   ")
              .append(achievement.getName())
              .append("\n");
        }
        achievementsLabel.setText(sb.toString());
    }
}
