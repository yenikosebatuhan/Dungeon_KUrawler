package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.AudioManager;
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
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;

import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.scene.layout.ColumnConstraints;

/**
 * JavaFX game view — renders the grid and handles key/mouse input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    private static final int HUD_HEIGHT = 90;
    private static final double LERP_SPEED = 0.2;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Stage stage;
    private Canvas canvas;
    private Canvas hudCanvas;
    private Label messageLabel;
    private Label achievementsLabel;
    private Label achievementPopupLabel;
    private GameLoop gameLoop;
    private TeamMatchGameLoop teamMatchGameLoop;
    private AnimationTimer animationTimer;
    private boolean paused;
    private class Splatter {
        double x, y, life = 1.0;
        Splatter(double x, double y) { this.x = x; this.y = y; }
    }
    private final java.util.List<Splatter> hitSplatters = new java.util.ArrayList<>();

    private final java.util.Map<Object, double[]> smoothPositions = new java.util.HashMap<>();
    private final List<DustParticle> hitParticles = new ArrayList<>();
    private int lastHeroHp = Integer.MAX_VALUE;

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

        hudCanvas = new Canvas(canvasW, HUD_HEIGHT);
        hudCanvas.setOnMouseClicked(this::handleHudClick);

        achievementsLabel = new Label();

        achievementPopupLabel = new Label();
        achievementPopupLabel.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        achievementPopupLabel.setTextFill(Color.web("#ffd54f"));
        achievementPopupLabel.setStyle(
                "-fx-background-color: rgba(14,12,10,0.92);"
                + "-fx-border-color: #c8a840;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 5 14 5 14;");
        achievementPopupLabel.setVisible(false);
        achievementPopupLabel.setEffect(new javafx.scene.effect.DropShadow(8, 0, 0, Color.web("#c8a840", 0.6)));
        achievementPopupLabel.setMouseTransparent(true);

        messageLabel = new Label("");
        messageLabel.setFont(Font.font("Monospaced", 11));
        messageLabel.setTextFill(Color.web("#aaa090"));
        messageLabel.setMouseTransparent(true);

        StackPane hudStack = new StackPane(hudCanvas, achievementPopupLabel, messageLabel);
        hudStack.setPrefHeight(HUD_HEIGHT);
        StackPane.setAlignment(achievementPopupLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(achievementPopupLabel, new Insets(0, 100, 0, 0));
        StackPane.setAlignment(messageLabel, Pos.CENTER);

        AchievementManager achievementManager = session.getAchievementManager();
        if (achievementManager != null) {
            achievementManager.setOnUnlock(this::showAchievementPopup);
        }

        BorderPane root = new BorderPane();
        root.setTop(hudStack);
        root.setCenter(canvas);
        root.setStyle("-fx-background-color: #0a0a10;");

        Scene scene = new Scene(root, canvasW, canvasH + HUD_HEIGHT);
        scene.getRoot().setStyle(
                "-fx-font-family: 'Serif'; "
                + "-fx-base: #1e1b16; "
                + "-fx-accent: #8b7940; "
                + "-fx-focus-color: #c8a850; "
                + "-fx-faint-focus-color: transparent;");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        stage.setTitle("Dungeon KUrawler");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        if (!(session instanceof TeamMatchSession)) {
            session.setCurrentMode("Play");
        }

        // PDF §4.1.2: Show target item popup at the start of a new play session.
        // Team Match has no target item, so skip in that mode.
        // IMPORTANT: defer startGameLoop() until the popup is dismissed so that
        // enemies do not move/attack while the player is still reading the quest.
        if (!(session instanceof TeamMatchSession) && session.getTargetObjective() != null) {
            javafx.application.Platform.runLater(() -> {
                new TargetItemPopup(stage, session.getTargetObjective()).show();
                startGameLoop(session);   // blocks above: showAndWait() returns only after dismiss
            });
        } else {
            startGameLoop(session);
        }
        render();
    }

    private void handleHudClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double pad = 6;
        double portraitSize = HUD_HEIGHT - pad * 2;
        
        if (x >= pad && x < pad + portraitSize && y >= pad && y < pad + portraitSize) {
            AudioManager.getInstance().play("button");
            showCharacterSheetPopup(controller.getSession());
            return;
        }
        double barStartX = pad + portraitSize + 8;
        double barW = 150;
        double statsX = barStartX + barW + 14;
        double statsX2 = statsX + 120;
        double achX = statsX2 + 110;
        double bagBtnX = achX + 68;
        double btnX = bagBtnX + 68;
        double btnSize = (HUD_HEIGHT - pad * 2 - 8) / 2.0 - 2;

        if (x >= achX && x < achX + 60 && y >= pad && y < HUD_HEIGHT - pad) {
            showAchievementsPopup();
        } else if (x >= bagBtnX && x < bagBtnX + 60 && y >= pad && y < HUD_HEIGHT - pad) {
            showInventoryPopup();
        } else if (x >= btnX && x < btnX + btnSize + 8 && y >= pad + 4 && y < pad + 4 + btnSize) {
            togglePause();
        } else if (x >= btnX && x < btnX + btnSize + 8 && y >= pad + 4 + btnSize + 4 && y < HUD_HEIGHT - pad) {
            AudioManager.getInstance().play("button");
            showInGameMenu();
        }
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
                    if (!paused) {
                        teamMatchGameLoop.update(deltaMs);
                    }
                    render();

                    if (tmSession.isGameOver()) {
                        animationTimer.stop();
                        if (tmSession.getMatchResult() != null) {
                            showTeamMatchResult(tmSession.getMatchResult());
                        } else {
                            showTeamMatchGameOver();
                        }
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
                    if (!paused) {
                        gameLoop.update(deltaMs);
                    }
                    render();

                    if (session.isGameOver()) {
                        animationTimer.stop();
                        showGameOver();
                    } else if (session.checkWinCondition()) {
                        animationTimer.stop();
                        showVictory();
                    }
                }
            };
        }
        animationTimer.start();
    }



    private void showGameOver() {
        AudioManager.getInstance().play("gameOver");
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        
        String causeOfDeath = "Slain in battle"; 
        String stats = String.format("Cause of Death: %s\nHP at Death: %d / %d\nTime Played: %s\nFloors Cleared: %d\nEnemies Defeated: %d",
                causeOfDeath, 
                Math.max(0, hero.getHealth()), hero.getMaxHealth(),
                formatTime(session.getPlayDurationTimer()),
                session.getFloorsClearedCount(),
                session.getEnemiesDefeatedCount()
        );

        Platform.runLater(() -> showEndScreen(
                "\u2620 YOU DIED \u2620",
                "Your hero has fallen in the dungeon.\nThe darkness claims another soul...",
                stats,
                "#ff4444", 
                "#3a0a0a"  
        ));
    }

    private void showVictory() {
        AudioManager.getInstance().play("victory");
        GameSession session = controller.getSession();
        String itemName = session.getTargetObjective() != null
                ? session.getTargetObjective().getName() : "the artifact";
                
        String stats = String.format("Time Taken: %s\nTotal Turns: %d\nItems Collected: %d\nEnemies Defeated: %d",
                formatTime(session.getPlayDurationTimer()),
                session.getTurnCount(),
                session.getItemsCollectedCount(),
                session.getEnemiesDefeatedCount()
        );

        Platform.runLater(() -> showEndScreen(
                "\u2605 VICTORY \u2605",
                "You found the " + itemName + "!\nCongratulations, brave adventurer!",
                stats,
                "#e8c84a", 
                "#2a2510"  
        ));
    }

    private void showTeamMatchResult(String result) {
        GameSession session = controller.getSession();
        boolean heroWon = "Team B Wins!".equals(result);
        String accent  = heroWon ? "#4a9eff" : "#ff4a4a";
        String bgTint  = heroWon ? "#0a1a3a" : "#2a0a0a";
        String title   = heroWon ? "\u2694 VICTORY \u2694" : "\u2620 DEFEAT \u2620";
        String stats   = String.format("Result: %s\nTime Played: %s\nTurns: %d",
                result,
                formatTime(session.getPlayDurationTimer()),
                session.getTurnCount());
        Platform.runLater(() -> showEndScreen(title, result, stats, accent, bgTint, screenManager::startTeamMatch));
    }

    private void showTeamMatchGameOver() {
        GameSession session = controller.getSession();
        String stats = String.format("Result: Hero Eliminated\nTime Played: %s\nTurns: %d",
                formatTime(session.getPlayDurationTimer()),
                session.getTurnCount());
        Platform.runLater(() -> showEndScreen(
                "\u2620 HERO FALLEN \u2620",
                "Your hero was eliminated!\nTeam A claims victory.",
                stats, "#ff4a4a", "#2a0a0a",
                screenManager::startTeamMatch));
    }

    private void showEndScreen(String title, String message, String statsText, String accent, String bgTint) {
        showEndScreen(title, message, statsText, accent, bgTint, screenManager::startNewGame);
    }

    private void showEndScreen(String title, String message, String statsText, String accent, String bgTint, Runnable playAgain) {
        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 480;
        int popH = 400; 

        Canvas bg = new Canvas(popW, popH);
        GraphicsContext gc = bg.getGraphicsContext2D();
        SpriteSheet wallSheet = new SpriteSheet("/assets/walls_x3.png", 48);
        
        gc.setFill(Color.web("#0a0a10"));
        gc.fillRect(0, 0, popW, popH);
        if (wallSheet.isLoaded()) {
            for (int y = 0; y < popH; y += 48) {
                for (int x = 0; x < popW; x += 48) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    javafx.scene.image.Image tile = wallSheet.getSprite(hash % 5, 1);
                    if (tile != null) gc.drawImage(tile, x, y, 48, 48);
                }
            }
        }
        gc.setFill(Color.web(bgTint, 0.85)); 
        gc.fillRect(0, 0, popW, popH);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Serif", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(accent));
        javafx.scene.effect.InnerShadow engrave = new javafx.scene.effect.InnerShadow(3, 1, 2, Color.web("#000000", 0.8));
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(25, Color.web(accent, 0.6));
        glow.setInput(engrave);
        titleLabel.setEffect(glow);

        Label msgLabel = new Label(message);
        msgLabel.setFont(Font.font("Serif", 16));
        msgLabel.setTextFill(Color.web("#d0c0a0"));
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(TextAlignment.CENTER);
        msgLabel.setMaxWidth(400);

        GridPane statsGrid = new GridPane();
        statsGrid.setAlignment(Pos.CENTER);
        statsGrid.setHgap(20);
        statsGrid.setVgap(8);
        statsGrid.setPadding(new Insets(15));
        statsGrid.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 8; -fx-border-color: " + accent + "44; -fx-border-radius: 8; -fx-border-width: 1;");

        String[] statLines = statsText.split("\n");
        for (int i = 0; i < statLines.length; i++) {
            String[] parts = statLines[i].split(":");
            if (parts.length == 2) {
                Label lblKey = new Label(parts[0].trim());
                lblKey.setFont(Font.font("Serif", FontWeight.NORMAL, 14));
                lblKey.setTextFill(Color.web("#a09070"));
                
                Label lblVal = new Label(parts[1].trim());
                lblVal.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
                lblVal.setTextFill(Color.web(accent));
                
                statsGrid.add(lblKey, 0, i);
                statsGrid.add(lblVal, 1, i);
            } else {
                Label lblSingle = new Label(statLines[i]);
                lblSingle.setTextFill(Color.web("#a09070"));
                statsGrid.add(lblSingle, 0, i, 2, 1);
            }
        }

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button menuBtn = popupButton("Return to Menu", "#a09070");
        menuBtn.setPrefWidth(160);
        menuBtn.setOnAction(e -> {
            popup.close();
            screenManager.showMainMenu();
        });

        Button playAgainBtn = popupButton("Play Again", accent);
        playAgainBtn.setPrefWidth(160);
        playAgainBtn.setOnAction(e -> {
            popup.close();
            playAgain.run();
        });

        buttonBox.getChildren().addAll(menuBtn, playAgainBtn);

        VBox content = new VBox(20, titleLabel, msgLabel, statsGrid, buttonBox);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-border-color: " + accent + "88; -fx-border-width: 3; -fx-border-radius: 4; -fx-background-radius: 4;");

        StackPane root = new StackPane(bg, content);
        
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), root);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        popup.setScene(new Scene(root, popW, popH));
        popup.setTitle("Game Over");

        popup.setOnShown(e -> centerPopup(popup));
        popup.showAndWait();
    }
    
    /**
     * BUG-04 / SECTION 2.4: Displays the Character Sheet (Stats & Equipment)
     */
    private void showCharacterSheetPopup(GameSession session) {
        if (animationTimer != null) animationTimer.stop();
        Hero hero = session.getHero();

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 340;
        int popH = 420;
        Canvas bg = new Canvas(popW, popH);
        drawDungeonBg(bg, popW, popH);

        Label title = new Label("CHARACTER SHEET");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.BLACK));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Name & Class
        addStatRow(grid, 0, "Name:", hero.getName());
        
        // Core Stats
        addStatRow(grid, 1, "Health (HP):", hero.getHealth() + " / " + hero.getMaxHealth());
        addStatRow(grid, 2, "Mana (MP):", hero.getMana() + " / " + hero.getMaxMana());
        addStatRow(grid, 3, "Energy (EN):", hero.getEnergy() + " / " + hero.getMaxEnergy());
        
        // Combat Stats (Dynamic reading ensures correct values)
        addStatRow(grid, 4, "Strength (STR):", String.valueOf(hero.getStrength()));
        addStatRow(grid, 5, "Defense (DEF):", String.valueOf(hero.getDefensePower()));
        addStatRow(grid, 6, "Total Damage:", String.valueOf(hero.calculateDamage()));

        // Equipment
        String wpnName = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "None";
        String armorName = hero.getEquippedWearable() != null ? hero.getEquippedWearable().getName() : "None";
        
        addStatRow(grid, 7, "Weapon:", wpnName);
        addStatRow(grid, 8, "Armor:", armorName);

        Button closeBtn = popupButton("Close", "#a09070");
        closeBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(20, title, grid, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-border-color: #8b794088; -fx-border-width: 2; -fx-border-radius: 6;");

        StackPane root = new StackPane(bg, content);
        popup.setScene(new Scene(root, popW, popH));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { 
            if (animationTimer != null) animationTimer.start(); 
            // Force keyboard focus back to the game canvas!
            Platform.runLater(() -> canvas.requestFocus()); 
        });
        popup.showAndWait();
    }

    /** Helper method for Character Sheet layout */
    private void addStatRow(GridPane grid, int row, String labelText, String valueText) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web("#a09070"));
        
        Label val = new Label(valueText);
        val.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        val.setTextFill(Color.web("#c8b880"));
        
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
    private void handleKeyPress(KeyEvent event) {
        if (paused && event.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
            return;
        }

        Hero hero = controller.getSession().getHero();
        boolean isMoveKey = false;
        int dRow = 0, dCol = 0;

        switch (event.getCode()) {
            case W: case UP:    isMoveKey = true; dRow = -1; dCol = 0; break;
            case S: case DOWN:  isMoveKey = true; dRow = 1; dCol = 0; break;
            case A: case LEFT:  isMoveKey = true; dRow = 0; dCol = -1; break;
            case D: case RIGHT: isMoveKey = true; dRow = 0; dCol = 1; break;
            case E:
                Item itemToTake = null;
                GridCell targetCell = null;

                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int r = hero.getRow() + dr;
                        int c = hero.getCol() + dc;
                        GridCell cell = controller.getSession().getMap().getCell(r, c);
                        
                        if (cell != null) {
                            for (GameObject obj : cell.getObjects()) {
                                if (obj instanceof Item) {
                                    itemToTake = (Item) obj;
                                    targetCell = cell;
                                    break; 
                                }
                            }
                        }
                        if (itemToTake != null) break;
                    }
                    if (itemToTake != null) break;
                }

                if (itemToTake != null) {
                    if (hero.getInventory().isFull()) {
                        messageLabel.setText("Inventory is full!");
                        AudioManager.getInstance().play("error");
                    } else {
                        controller.interactWithObject(targetCell.getRow(), targetCell.getCol(), itemToTake);
                        AudioManager.getInstance().play("item");
                        messageLabel.setText("Picked up " + itemToTake.getName());
                    }
                } else {
                    messageLabel.setText("Nothing to pick up nearby.");
                }
                break;
            case C:             
                AudioManager.getInstance().play("button");
                showCharacterSheetPopup(controller.getSession()); 
                break;
            case I:             
                AudioManager.getInstance().play("button");
                showInventoryPopup(); 
                break;
            case F:
                boolean shot = controller.heroShoot();
                if (shot) {
                    AudioManager.getInstance().play("attack");
                    messageLabel.setText("Fired a projectile! (MP -10)");
                } else {
                    messageLabel.setText("Not enough mana to shoot!");
                }
                break;
            case SPACE: 
                event.consume(); // PREVENTS JavaFX from clicking hidden buttons
                int tRow = hero.getRow() + hero.getFacingRow();
                int tCol = hero.getCol() + hero.getFacingCol();
                
                if (hero.getEnergy() < 1) {
                    messageLabel.setText("Not enough energy to attack!");
                    AudioManager.getInstance().play("error");
                    break;
                }
                
                boolean hit = controller.attackEnemyAt(tRow, tCol);
                if (hit) {
                    AudioManager.getInstance().play("attack");
                    spawnHitParticles(tCol * TILE_SIZE + (TILE_SIZE / 2.0), tRow * TILE_SIZE + (TILE_SIZE / 2.0));
                } else {
                    messageLabel.setText("No enemy there!");
                }
                break;
            case ESCAPE:        showInGameMenu(); return;
            default:            return;
        }

        if (isMoveKey) {
            if (hero.getEnergy() < 1) {
                messageLabel.setText("Not enough energy, you CAN NOT MOVE!");
                
            } else {
                controller.moveHero(dRow, dCol);
            }
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
        
        if (dRow > 1 || dCol > 1) {
            messageLabel.setText("Too far! Must be adjacent or on the same tile.");
            return;
        }

        GridCell cell = map.getCell(row, col);
        if (cell == null) return;

        java.util.List<String[]> actionList = new java.util.ArrayList<>();

        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer) {
                StaticContainer sc = (StaticContainer) obj;
                if (sc.isLocked()) {
                    // Check if Hero has the required key
                    boolean hasKey = false;
                    for (Item invItem : hero.getInventory().getItems()) {
                        if (invItem instanceof com.kurawler.model.object.Key && 
                           ((com.kurawler.model.object.Key) invItem).getKeyId().equals(sc.getRequiredKeyId())) {
                            hasKey = true;
                            break;
                        }
                    }
                    if (hasKey) {
                        actionList.add(new String[]{"OPEN (with Key)", sc.getName(), "open_locked"});
                    } else {
                        actionList.add(new String[]{"LOCKED", sc.getName(), "locked_info"});
                    }
                } else {
                    actionList.add(new String[]{"OPEN", sc.getName(), "open_container"});
                }
            } else if (obj instanceof Item) {
                actionList.add(new String[]{"TAKE", obj.getName(), "take"});
            }
        }
        StaticObject staticObj = cell.getStaticObject();
        if (staticObj != null && staticObj.isSearchable()) {
            actionList.add(new String[]{"SEARCH", staticObj.getName(), "search"});
            actionList.add(new String[]{"BREAK", staticObj.getName(), "break"});
        }
        if (cell.getOccupyingCharacter() instanceof Enemy) {
            Enemy enemy = (Enemy) cell.getOccupyingCharacter();
            actionList.add(new String[]{"ATTACK", enemy.getName(), "attack"});
        }

        if (actionList.isEmpty()) {
            messageLabel.setText("Nothing to interact with here.");
            return;
        }

        if (animationTimer != null) animationTimer.stop();

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 300;
        int popH = 60 + actionList.size() * 44 + 50;
        Canvas bg = new Canvas(popW, popH);
        drawDungeonBg(bg, popW, popH);

        Label title = new Label("ITEM MENU");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.BLACK));

        VBox btns = new VBox(6);
        btns.setAlignment(Pos.CENTER);

        for (String[] action : actionList) {
            String verb = action[0];
            String target = action[1];
            String type = action[2];
            String color = "#e8c84a";
            if ("attack".equals(type)) color = "#e74c3c";
            else if ("search".equals(type)) color = "#5dade2";
            else if ("break".equals(type)) color = "#e67e22";

            Button btn = popupButton(verb + " " + target, color);
            
            // --- BUG-02 FIX: Disable TAKE button if inventory is full ---
            if ("take".equals(type) && hero.getInventory().isFull()) {
                btn.setDisable(true);
                btn.setText(verb + " " + target + " (Full)");
            }

            btn.setOnAction(e -> {
                popup.close();
                executeInteraction(type, row, col, target, session);
            });
            btns.getChildren().add(btn);
        }

        Button cancelBtn = popupButton("Cancel", "#a09070");
        cancelBtn.setOnAction(e -> popup.close());
        btns.getChildren().add(cancelBtn);

        VBox content = new VBox(10, title, btns);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-border-color: #8b794088; -fx-border-width: 2; -fx-border-radius: 6;");

        StackPane root = new StackPane(bg, content);
        popup.setScene(new Scene(root, popW, popH));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { 
            if (animationTimer != null) animationTimer.start(); 
            // Force keyboard focus back to the game canvas!
            Platform.runLater(() -> canvas.requestFocus()); 
        });
        popup.showAndWait();
    }

    private void executeInteraction(String type, int row, int col, String target, GameSession session) {
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();
        GridCell cell = map.getCell(row, col);
        if (cell == null) return;

        switch (type) {
            case "take":
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof Item && obj.getName().equals(target)) {
                        controller.interactWithObject(row, col, (Item) obj);
                        session.incrementItemsCollected();
                        AudioManager.getInstance().play("item");
                        messageLabel.setText("Picked up " + obj.getName());
                        break;
                    }
                }
                break;
            case "search":
                StaticObject so = cell.getStaticObject();
                if (so != null) {
                    Item hidden = so.getHiddenItem();
                    boolean searched = controller.searchObject(so);
                    if (hidden != null && hero.getInventory().hasItem(hidden))
                        messageLabel.setText("Found: " + hidden.getName() + "!");
                    else if (searched) messageLabel.setText("Nothing found.");
                    else messageLabel.setText("Cannot search.");
                    
                    AudioManager.getInstance().play("interact"); // Added interact sound
                }
                break;
            case "break":
                boolean broken = controller.breakObject(row, col);
                messageLabel.setText(broken ? "Broke it!" : "Cannot break.");
                AudioManager.getInstance().play("interact"); // Added interact sound
                break;
            case "attack":
                if (cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                    hero.attack(enemy);
                    spawnHitParticles(col * TILE_SIZE, row * TILE_SIZE);
                    if (!enemy.isAlive()) {
                        cell.setOccupyingCharacter(null);
                        session.decrementEnemyCount();
                        session.incrementEnemiesDefeated();
                        AudioManager.getInstance().play("enemyDown");
                        messageLabel.setText(enemy.getName() + " defeated!");
                    } else {
                        AudioManager.getInstance().play("attack");
                        messageLabel.setText("Hit " + enemy.getName() + "! HP:" + enemy.getHealth());
                    }
                }
                break;
            case "open_container":
            case "open_locked":
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer && obj.getName().equals(target)) {
                        StaticContainer sc = (StaticContainer) obj;
                        if ("open_locked".equals(type)) {
                            sc.unlock();
                            messageLabel.setText("Unlocked " + target + "!");
                        }
                        AudioManager.getInstance().play("interact");
                        // Wait for any previous popup to close, then open contents
                        Platform.runLater(() -> showContainerContentsPopup(sc, session));
                        break;
                    }
                }
                break;
            case "locked_info":
                messageLabel.setText("It is locked. You need a specific key.");
                AudioManager.getInstance().play("error"); // Optional error sound
                break;
        }
        render();
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
                boolean saved = screenManager.showSaveDialog();
                messageLabel.setText(saved ? "Game saved." : "Save cancelled.");
            } else if (choice == load) {
                boolean loaded = screenManager.showLoadDialogAndStart();
                if (loaded) {
                    animationTimer = null;
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

    private void togglePause() {
        AudioManager.getInstance().play("button");
        paused = !paused;
        GameSession session = controller.getSession();
        if (paused) {
            session.setCurrentMode("Paused");
            messageLabel.setText("PAUSED");
        } else {
            session.setCurrentMode(session instanceof TeamMatchSession ? "TeamMatch" : "Play");
            messageLabel.setText("");
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

                if (so != null && !"Wall".equals(so.getName()) && !"Floor".equals(so.getName())) {
                    renderer.drawStaticObject(gc, sx, sy, so.getName());
                }

                if (so != null && so.isSearchable()) {
                    renderer.drawSearchableMarker(gc, sx, sy);
                }

                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) {
                        renderer.drawContainerByName(gc, sx, sy, obj.getName());
                    } else if (obj instanceof Weapon) {
                        renderer.drawWeaponByName(gc, sx, sy, obj.getName());
                    } else if (obj instanceof Scroll) {
                        renderer.drawItem(gc, sx, sy, "Scroll");
                    } else if (obj instanceof Wearable) {
                        renderer.drawItem(gc, sx, sy, "Wearable");
                    } else if (obj instanceof Consumable) {
                        Consumable cons = (Consumable) obj;
                        String pt = cons.getManaAmount() > 0 ? "ManaPotion"
                                  : cons.getEnergyAmount() > 0 ? "EnergyPotion"
                                  : "HealthPotion";
                        renderer.drawConsumable(gc, sx, sy, pt);
                    } else if (obj instanceof Key) {
                        renderer.drawItem(gc, sx, sy, "Key");
                    } else if (obj instanceof Item) {
                        renderer.drawItem(gc, sx, sy, "Item");
                    }
                }

                Character occupant = cell.getOccupyingCharacter();
                if (occupant != null && occupant.isAlive() && !(occupant instanceof Hero)) {
                    double[] pos = lerpPosition(occupant, c * TILE_SIZE, r * TILE_SIZE);
                    String type = (occupant instanceof Knight) ? "Knight" : "Sorcerer";
                    renderer.drawEnemy(gc, (int) pos[0], (int) pos[1], type);
                    renderer.drawHpBar(gc, (int) pos[0], (int) pos[1],
                            occupant.getHealth(), occupant.getMaxHealth());

                    if (session instanceof TeamMatchSession) {
                        TeamMatchSession tmSession = (TeamMatchSession) session;
                        Team team = tmSession.getTeamForCharacter(occupant);
                        if (team != null) {
                            String lbl = (occupant instanceof Knight) ? "KNIGHT" : "SORCER";
                            drawTeamIndicator(gc, (int) pos[0], (int) pos[1], team.getColor(), lbl);
                        }
                    }
                }
            }
        }

        ShadowClone shadowClone = session.getShadowClone();
        if (shadowClone != null && shadowClone.isAlive()) {
            double[] clonePos = lerpPosition(shadowClone,
                    shadowClone.getCol() * TILE_SIZE, shadowClone.getRow() * TILE_SIZE);
            renderer.drawShadowClone(gc, (int) clonePos[0], (int) clonePos[1]);
        }

        double[] heroPos = lerpPosition(hero, hero.getCol() * TILE_SIZE, hero.getRow() * TILE_SIZE);
        int heroSx = (int) heroPos[0];
        int heroSy = (int) heroPos[1];
        renderer.drawHeroFacing(gc, heroSx, heroSy, hero.getFacingRow(), hero.getFacingCol());
        renderer.drawHpBar(gc, heroSx, heroSy, hero.getHealth(), hero.getMaxHealth());

        int currentHp = hero.getHealth();
        if (currentHp < lastHeroHp) {
            spawnHitParticles(heroSx, heroSy);
        }
        lastHeroHp = currentHp;
        updateAndDrawHitParticles(gc);

        if (session instanceof TeamMatchSession) {
            drawTeamIndicator(gc, heroSx, heroSy, Team.TeamColor.BLUE, "HERO");
        }

        if (gameLoop != null) {
            for (com.kurawler.model.object.Projectile proj : gameLoop.getActiveProjectiles()) {
                double[] pPos = lerpPosition(proj,
                        proj.getPositionCol() * TILE_SIZE, proj.getPositionRow() * TILE_SIZE);
                if ("Hero Bolt".equals(proj.getName())) {
                    renderer.drawHeroProjectile(gc, (int) pPos[0], (int) pPos[1]);
                } else {
                    renderer.drawProjectile(gc, (int) pPos[0], (int) pPos[1]);
                }
            }
        }
        if (teamMatchGameLoop != null) {
            for (com.kurawler.model.object.Projectile proj : teamMatchGameLoop.getActiveProjectiles()) {
                double[] pPos = lerpPosition(proj,
                        proj.getPositionCol() * TILE_SIZE, proj.getPositionRow() * TILE_SIZE);
                renderer.drawProjectile(gc, (int) pPos[0], (int) pPos[1]);
            }
        }
        /**
         * @requires gc is initialized, hitSplatters is valid
         * @modifies hitSplatters, gc state
         * @effects Draws hit splatters and safely fades them out without negative alpha exceptions.
         */
        // --- Red Hit-Splatter Particle Rendering ---
        for (int i = hitSplatters.size() - 1; i >= 0; i--) {
            Splatter s = hitSplatters.get(i);
            
            double safeAlpha = Math.max(0.0, Math.min(1.0, s.life));
            gc.setGlobalAlpha(safeAlpha);
            
            gc.setFill(Color.web("#ff3333")); // Bright red splash
            gc.fillOval(s.x - 18, s.y - 18, 36, 36); 
            gc.setFill(Color.web("#990000")); // Dark red core
            gc.fillOval(s.x - 8, s.y - 8, 16, 16);
            gc.setGlobalAlpha(1.0);
            
            s.life -= 0.02; // Slower fade out
            if (s.life <= 0) hitSplatters.remove(i);
        }

        drawHud(session);
    }

    private javafx.scene.image.Image bagIconImg;

    private void drawHud(GameSession session) {
        Hero hero = session.getHero();
        GraphicsContext gc = hudCanvas.getGraphicsContext2D();
        double w = hudCanvas.getWidth();
        double h = HUD_HEIGHT;

        if (bagIconImg == null) {
            try {
                bagIconImg = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/assets/bag_icon.png"));
            } catch (Exception ignored) {}
        }

        // Dark background
        gc.setFill(Color.web("#0e0c0a"));
        gc.fillRect(0, 0, w, h);

        // Gold border lines
        gc.setStroke(Color.web("#c8a840"));
        gc.setLineWidth(2);
        gc.strokeLine(0, 0, w, 0);
        gc.strokeLine(0, h - 1, w, h - 1);
        gc.setStroke(Color.web("#8b7530", 0.6));
        gc.setLineWidth(1);
        gc.strokeLine(0, 2, w, 2);
        gc.strokeLine(0, h - 3, w, h - 3);

        double pad = 6;

        // ── SECTION 1: Hero portrait (pad, pad) ──
        double portraitSize = h - pad * 2;
        drawGoldPanel(gc, pad, pad, portraitSize, portraitSize);
        renderer.drawHeroFacing(gc, (int)(pad + (portraitSize - TILE_SIZE) / 2),
                (int)(pad + (portraitSize - TILE_SIZE) / 2),
                hero.getFacingRow(), hero.getFacingCol());

        // ── SECTION 2: Stat bars ──
        double barStartX = pad + portraitSize + 8;
        double barW = 150;
        double barH = 18;
        double barGap = 4;
        double barsTopY = pad + 4;

        drawStatBar(gc, barStartX, barsTopY, barW, barH,
                "HP", hero.getHealth(), hero.getMaxHealth(), "#4a0e0e", "#b71c1c", "#ef5350");
        drawStatBar(gc, barStartX, barsTopY + barH + barGap, barW, barH,
                "MP", hero.getMana(), hero.getMaxMana(), "#0d2a5a", "#1565c0", "#42a5f5");
        drawStatBar(gc, barStartX, barsTopY + (barH + barGap) * 2, barW, barH,
                "EN", hero.getEnergy(), hero.getMaxEnergy(), "#4a3d00", "#f9a825", "#f4d03f");

        // ── SECTION 3: Stats text ──
        double statsX = barStartX + barW + 14;

        // Gold divider line
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.setLineWidth(1);
        gc.strokeLine(statsX - 7, pad + 4, statsX - 7, h - pad - 4);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("\u2694 ATK " + hero.calculateDamage(), statsX, barsTopY + 14);
        gc.fillText("\u26E8 DEF " + hero.getDefensePower(), statsX, barsTopY + 14 + 20);

        String wpn = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "None";
        gc.setFill(Color.web("#aa9870"));
        gc.fillText("Wpn: " + wpn, statsX, barsTopY + 14 + 40);

        double statsX2 = statsX + 120;
        gc.setFill(Color.web("#c8b880"));
        gc.fillText("\u23F1 " + formatTime(session.getPlayDurationTimer()), statsX2, barsTopY + 14);
        gc.fillText("Turn " + session.getTurnCount(), statsX2, barsTopY + 14 + 20);
        if (session instanceof TeamMatchSession) {
            TeamMatchSession tms = (TeamMatchSession) session;
            int aAlive = tms.getTeamA().getAliveMembers().size();
            int aTotal = tms.getTeamA().getMembers().size();
            int bAlive = tms.getTeamB().getAliveMembers().size();
            int bTotal = tms.getTeamB().getMembers().size();
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
            gc.setFill(Color.rgb(255, 90, 90));
            gc.fillText("\u25A0 A: " + aAlive + "/" + aTotal, statsX2, barsTopY + 14 + 40);
            gc.setFill(Color.rgb(90, 150, 255));
            gc.fillText("\u25A0 B: " + bAlive + "/" + bTotal, statsX2 + 62, barsTopY + 14 + 40);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        } else {
            gc.setFill(Color.web("#aa9870"));
            gc.fillText("Foes " + session.getActiveEnemyCount(), statsX2, barsTopY + 14 + 40);
        }

        // ── SECTION 4: Achievements (trophy icon) ──
        double achX = statsX2 + 110;
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.strokeLine(achX - 7, pad + 4, achX - 7, h - pad - 4);

        AchievementManager am = session.getAchievementManager();
        int achUnlocked = 0;
        int achTotal = 0;
        if (am != null) {
            for (Achievement a : am.getAchievements()) {
                achTotal++;
                if (a.isUnlocked()) achUnlocked++;
            }
        }
        drawGoldPanel(gc, achX, pad + 4, 60, h - pad * 2 - 8);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("\uD83C\uDFC6", achX + 16, pad + 32);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillText(achUnlocked + "/" + achTotal, achX + 14, pad + 58);

        // ── SECTION 5: Bag button ──
        double bagBtnX = achX + 68;
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.strokeLine(bagBtnX - 7, pad + 4, bagBtnX - 7, h - pad - 4);

        drawGoldPanel(gc, bagBtnX, pad + 4, 60, h - pad * 2 - 8);
        if (bagIconImg != null && !bagIconImg.isError()) {
            gc.drawImage(bagIconImg, bagBtnX + 6, pad + 6, 48, 48);
        }
        Inventory inv = hero.getInventory();
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText(inv.getItemCount() + "/8", bagBtnX + 16, h - pad - 4);

        // ── SECTION 6: Pause & Menu buttons ──
        double btnX = bagBtnX + 68;
        double btnSize = (h - pad * 2 - 8) / 2 - 2;

        drawGoldPanel(gc, btnX, pad + 4, btnSize + 8, btnSize);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText(paused ? "\u25B6" : "\u23F8", btnX + 8, pad + 4 + btnSize - 8);

        drawGoldPanel(gc, btnX, pad + 4 + btnSize + 4, btnSize + 8, btnSize);
        gc.fillText("\u2630", btnX + 8, h - pad - 10);
    }

    private void drawGoldPanel(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#0a0908"));
        gc.fillRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.web("#c8a840", 0.7));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.web("#8b7530", 0.3));
        gc.setLineWidth(0.5);
        gc.strokeRoundRect(x + 2, y + 2, w - 4, h - 4, 3, 3);
    }

    private void drawStatBar(GraphicsContext gc, double x, double y, double w, double h,
                             String label, int val, int max,
                             String bgColor, String barColor, String lightColor) {
        gc.setFill(Color.web("#000000", 0.5));
        gc.fillRoundRect(x - 1, y - 1, w + 2, h + 2, 5, 5);
        gc.setFill(Color.web(bgColor));
        gc.fillRoundRect(x, y, w, h, 4, 4);

        double ratio = max > 0 ? Math.max(0, Math.min(1.0, (double) val / max)) : 0;
        if (ratio > 0) {
            double fillW = Math.max(6, w * ratio);
            gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(lightColor)),
                    new Stop(0.5, Color.web(barColor)),
                    new Stop(1, Color.web(bgColor))));
            gc.fillRoundRect(x, y, fillW, h, 4, 4);
            gc.setFill(Color.web("#ffffff", 0.18));
            gc.fillRoundRect(x + 2, y + 1, fillW - 4, h * 0.35, 2, 2);
        }

        gc.setStroke(Color.web("#c8a840", 0.3));
        gc.setLineWidth(0.5);
        gc.strokeRoundRect(x, y, w, h, 4, 4);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
        gc.setFill(Color.web("#ffffff", 0.95));
        gc.fillText(label + " " + val + "/" + max, x + 4, y + h - 4);
    }


    private void showAchievementPopup(Achievement achievement) {
        achievementPopupLabel.setText("\uD83C\uDFC6 " + achievement.getName() + " unlocked!");
        achievementPopupLabel.setVisible(true);

        PauseTransition hide = new PauseTransition(Duration.seconds(3));
        hide.setOnFinished(e -> achievementPopupLabel.setVisible(false));
        hide.play();
    }

    private void showAchievementsPopup() {
        GameSession session = controller.getSession();
        AchievementManager am = session.getAchievementManager();
        if (am == null) return;

        if (animationTimer != null) animationTimer.stop();

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(stage);
        popup.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: rgba(14,12,10,0.95); -fx-border-color: #c8a840; "
                + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label title = new Label("\uD83C\uDFC6  Achievements");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#e8c84a"));
        root.getChildren().add(title);

        for (Achievement a : am.getAchievements()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle(a.isUnlocked()
                    ? "-fx-background-color: rgba(200,168,64,0.15); -fx-background-radius: 6;"
                    : "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6;");

            Label icon = new Label(a.isUnlocked() ? "\u2605" : "\u2606");
            icon.setFont(Font.font("Serif", FontWeight.BOLD, 20));
            icon.setTextFill(a.isUnlocked() ? Color.web("#e8c84a") : Color.web("#555555"));

            VBox info = new VBox(2);
            Label name = new Label(a.getName());
            name.setFont(Font.font("Serif", FontWeight.BOLD, 14));
            name.setTextFill(a.isUnlocked() ? Color.web("#e8c84a") : Color.web("#888888"));
            Label desc = new Label(a.getDescription());
            desc.setFont(Font.font("Serif", 11));
            desc.setTextFill(a.isUnlocked() ? Color.web("#c8b880") : Color.web("#666666"));
            info.getChildren().addAll(name, desc);

            row.getChildren().addAll(icon, info);
            root.getChildren().add(row);
        }

        Button closeBtn = popupButton("Close", "#c8a840");
        closeBtn.setOnAction(e -> popup.close());
        root.getChildren().add(closeBtn);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnHidden(e -> { 
            if (animationTimer != null) animationTimer.start(); 
            // Force keyboard focus back to the game canvas!
            Platform.runLater(() -> canvas.requestFocus()); 
        });
        popup.show();
        centerPopup(popup);
    }

    private void updateSidebar(GameSession session) {
    }

    private String formatTime(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void drawTeamIndicator(GraphicsContext gc, int sx, int sy, Team.TeamColor teamColor) {
        drawTeamIndicator(gc, sx, sy, teamColor, null);
    }

    private void drawTeamIndicator(GraphicsContext gc, int sx, int sy, Team.TeamColor teamColor, String label) {
        boolean isRed  = (teamColor == Team.TeamColor.RED);
        Color primary  = isRed ? Color.rgb(255, 46, 62)  : Color.rgb(46, 91, 255);
        Color glow     = isRed ? Color.rgb(255, 46, 62, 0.30) : Color.rgb(46, 91, 255, 0.30);
        Color tint     = isRed ? Color.rgb(255, 46, 62, 0.10) : Color.rgb(46, 91, 255, 0.10);
        Color nameplateBg = isRed ? Color.rgb(153, 10, 20, 0.92) : Color.rgb(10, 40, 153, 0.92);

        // subtle tint over entire tile
        gc.setFill(tint);
        gc.fillRect(sx, sy, TILE_SIZE, TILE_SIZE);

        // simulated glow: outer translucent ring → inner solid border
        gc.setStroke(glow);
        gc.setLineWidth(5);
        gc.strokeRect(sx - 1, sy - 1, TILE_SIZE + 2, TILE_SIZE + 2);
        gc.setStroke(primary);
        gc.setLineWidth(2.5);
        gc.strokeRect(sx + 1, sy + 1, TILE_SIZE - 2, TILE_SIZE - 2);

        // nameplate ABOVE the sprite
        String text = (label != null && !label.isEmpty()) ? label : (isRed ? "A" : "B");
        double plateW = TILE_SIZE;
        double plateH = 13;
        double plateY = sy - plateH - 1;

        gc.setFill(nameplateBg);
        gc.fillRoundRect(sx, plateY, plateW, plateH, 4, 4);
        gc.setStroke(primary);
        gc.setLineWidth(1);
        gc.strokeRoundRect(sx, plateY, plateW, plateH, 4, 4);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, sx + TILE_SIZE / 2.0, plateY + plateH - 2);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
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

    /**
     * Creates a temporary hit-splatter particle at the specified screen coordinates.
     *
     * @requires x and y > 0
     * @modifies hitSplatters list
     * @effects Instantiates a Splatter object that will be rendered and faded out over time.
     */
    private void spawnHitParticles(double x, double y) {
        hitSplatters.add(new Splatter(x, y));
    }
    
    private void buildInventoryUI(GameSession session) {
    }

    private void showInventoryPopup() {
        if (animationTimer != null) animationTimer.stop();
        GameSession session = controller.getSession();
        Hero hero = session.getHero();

        boolean wasPaused = paused;
        if (!wasPaused) {
            paused = true;
            session.setCurrentMode("Paused");
        }

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 400;
        int popH = 560;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        javafx.scene.image.Image bagImg = null;
        try {
            bagImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/Inventory x4.png"));
        } catch (Exception ignored) {}
        if (bagImg != null && !bagImg.isError()) {
            javafx.scene.image.ImageView bagView = new javafx.scene.image.ImageView(bagImg);
            bagView.setFitWidth(popW);
            bagView.setFitHeight(popH);
            bagView.setPreserveRatio(false);
            root.getChildren().add(bagView);
        } else {
            Canvas bg = new Canvas(popW, popH);
            drawDungeonBg(bg, popW, popH);
            root.getChildren().add(bg);
        }

        // Title
        Label titleLabel = new Label();
        titleLabel.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#3a2a10"));
        titleLabel.setEffect(new javafx.scene.effect.DropShadow(2, 0, 1, Color.web("#00000044")));

        // Item grid
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        // Detail panel shown when a slot is selected
        VBox detailPane = new VBox(8);
        detailPane.setAlignment(Pos.CENTER);
        detailPane.setPrefWidth(330);
        detailPane.setMinHeight(150);
        detailPane.setStyle("-fx-background-color: rgba(20,14,6,0.82); "
                + "-fx-border-color: #8b7530; -fx-border-width: 1.5; -fx-border-radius: 5; "
                + "-fx-background-radius: 5; -fx-padding: 10 16 10 16;");

        // Close button
        Button closeBtn = new Button("Close Bag");
        closeBtn.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        closeBtn.setPrefWidth(160);
        closeBtn.setPrefHeight(32);
        String cBase = "-fx-background-color: linear-gradient(to bottom, #8b7530, #6a5520); "
                + "-fx-text-fill: #f0e0b0; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: #3a2a10; -fx-border-width: 1.5; -fx-cursor: hand;";
        String cHover = "-fx-background-color: linear-gradient(to bottom, #a08940, #8b7530); "
                + "-fx-text-fill: #ffffff; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: #e8c84a; -fx-border-width: 1.5; -fx-cursor: hand;";
        closeBtn.setStyle(cBase);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(cHover));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(cBase));
        closeBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(12, titleLabel, grid, detailPane, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(55, 35, 35, 35));
        root.getChildren().add(content);

        // Shared mutable state: which item is selected
        Item[] selected = new Item[1];

        // Refresh rebuilds grid + detail pane without closing the popup
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            Inventory inv = hero.getInventory();
            titleLabel.setText("INVENTORY (" + inv.getItemCount() + "/8)");

            grid.getChildren().clear();
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 4; c++) {
                    final Item item = inv.getItem(r, c);
                    grid.add(buildInventorySlot(item, selected, hero, refresh, popup, session), c, r);
                }
            }

            detailPane.getChildren().clear();
            if (selected[0] != null && hero.getInventory().hasItem(selected[0])) {
                buildItemDetailPane(detailPane, selected[0], hero, session, popup, refresh);
            } else {
                selected[0] = null;
                Label hint = new Label("Select an item to see actions");
                hint.setTextFill(Color.web("#8b7a40"));
                hint.setFont(Font.font("Serif", FontWeight.BOLD, 12));
                detailPane.getChildren().add(hint);
            }
        };
        refresh[0].run();

        Scene scene = new Scene(root, popW, popH);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> {
            if (!wasPaused) {
                paused = false;
                session.setCurrentMode(session instanceof TeamMatchSession ? "TeamMatch" : "Play");
            }
            if (animationTimer != null) animationTimer.start();
            Platform.runLater(() -> canvas.requestFocus());
        });
        popup.showAndWait();
    }

    private StackPane buildInventorySlot(Item item, Item[] selected, Hero hero,
            Runnable[] refresh, Stage popup, GameSession session) {
        int SIZE = 68;
        StackPane slot = new StackPane();
        slot.setPrefSize(SIZE, SIZE);
        slot.setMinSize(SIZE, SIZE);
        slot.setMaxSize(SIZE, SIZE);

        if (item == null) {
            slot.setStyle("-fx-background-color: #c0a86044; -fx-border-color: #8b753044; "
                    + "-fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");
            return slot;
        }

        boolean isSelected = (item == selected[0]);
        boolean isEquipped = (item instanceof Weapon && item == hero.getEquippedWeapon())
                || (item instanceof Wearable && item == hero.getEquippedWearable());

        String selBg    = "-fx-background-color: #2a5c1888; ";
        String selBorder = "-fx-border-color: #e8c84a; -fx-border-width: 2.5; ";
        String normBg    = "-fx-background-color: #c0a860aa; ";
        String normBorder = "-fx-border-color: #8b7530; -fx-border-width: 1.5; ";
        String radius    = "-fx-background-radius: 5; -fx-border-radius: 5; -fx-cursor: hand;";

        slot.setStyle((isSelected ? selBg + selBorder : normBg + normBorder) + radius);

        // Item sprite
        javafx.scene.image.Image sprite = renderer.getSpriteForItem(item);
        if (sprite != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(sprite);
            iv.setFitWidth(44);
            iv.setFitHeight(44);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            slot.getChildren().add(iv);
        }

        // Equipped badge (top-right corner)
        if (isEquipped) {
            Label badge = new Label(item instanceof Weapon ? "\u2694" : "\u26E8");
            badge.setFont(Font.font(9));
            badge.setTextFill(Color.web("#60ff88"));
            badge.setStyle("-fx-background-color: rgba(0,70,0,0.78); "
                    + "-fx-padding: 0 2 0 2; -fx-background-radius: 2;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(3, 3, 0, 0));
            slot.getChildren().add(badge);
        }

        // Hover / click handlers
        slot.setOnMouseEntered(e -> {
            if (item != selected[0])
                slot.setStyle("-fx-background-color: #c8b060cc; -fx-border-color: #e8c84a88; "
                        + "-fx-border-width: 1.5; " + radius);
        });
        slot.setOnMouseExited(e -> {
            if (item != selected[0])
                slot.setStyle(normBg + normBorder + radius);
        });
        slot.setOnMouseClicked(e -> {
            selected[0] = item;
            refresh[0].run();
        });

        return slot;
    }

    private void buildItemDetailPane(VBox pane, Item item, Hero hero,
            GameSession session, Stage popup, Runnable[] refresh) {
        Label nameLabel = new Label(item.getName());
        nameLabel.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#e8c84a"));

        Label typeLabel = new Label(item.getClass().getSimpleName());
        typeLabel.setFont(Font.font("Serif", 11));
        typeLabel.setTextFill(Color.web("#aaa090"));

        Label descLabel = new Label(item.getDescription());
        descLabel.setFont(Font.font("Serif", 11));
        descLabel.setTextFill(Color.web("#c8b880"));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(290);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);

        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            boolean equipped = weapon == hero.getEquippedWeapon();
            Button btn = invActionButton(equipped ? "Unequip" : "\u2694 Equip", "#e8c84a");
            btn.setOnAction(e -> {
                controller.equipWeapon(weapon);
                AudioManager.getInstance().play("interact");
                render();
                refresh[0].run();
            });
            actionRow.getChildren().add(btn);
        }
        if (item instanceof Wearable) {
            Wearable wearable = (Wearable) item;
            boolean worn = wearable == hero.getEquippedWearable();
            Button btn = invActionButton(worn ? "Remove" : "\u26E8 Wear", "#5dade2");
            btn.setOnAction(e -> {
                if (worn) controller.unequipWearable();
                else controller.wearItem(wearable);
                AudioManager.getInstance().play("interact");
                render();
                refresh[0].run();
            });
            actionRow.getChildren().add(btn);
        }
        if (item instanceof Consumable) {
            Consumable cons = (Consumable) item;
            Button btn = invActionButton("Use", "#55efc4");
            btn.setOnAction(e -> {
                com.kurawler.action.Action action = new com.kurawler.action.EatAction(cons);
                for (com.kurawler.effect.Effect eff : action.resolve(session)) eff.apply(session);
                AudioManager.getInstance().play("interact");
                render();
                refresh[0].run();
            });
            actionRow.getChildren().add(btn);
        }
        if (item instanceof Scroll) {
            Scroll scroll = (Scroll) item;
            Button btn = invActionButton("Read", "#a29bfe");
            btn.setOnAction(e -> {
                controller.readScroll(scroll);
                AudioManager.getInstance().play("interact");
                popup.close();
                render();
            });
            actionRow.getChildren().add(btn);
        }
        if (item instanceof com.kurawler.model.object.PortableContainer) {
            com.kurawler.model.object.PortableContainer bag = (com.kurawler.model.object.PortableContainer) item;
            Button btn = invActionButton("Open", "#f39c12");
            btn.setOnAction(e -> {
                AudioManager.getInstance().play("interact");
                popup.close();
                Platform.runLater(() -> showContainerContentsPopup(bag, session));
            });
            actionRow.getChildren().add(btn);
        }

        Button discardBtn = invActionButton("Discard", "#e74c3c");
        discardBtn.setOnAction(e -> {
            com.kurawler.action.Action action = new com.kurawler.action.DiscardAction(item);
            for (com.kurawler.effect.Effect eff : action.resolve(session)) eff.apply(session);
            AudioManager.getInstance().play("interact");
            render();
            refresh[0].run();
        });
        actionRow.getChildren().add(discardBtn);

        pane.getChildren().addAll(nameLabel, typeLabel, descLabel, actionRow);
    }

    private Button invActionButton(String text, String hexColor) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        btn.setPrefHeight(26);
        String base = "-fx-background-color: " + hexColor + "33; -fx-text-fill: " + hexColor + "; "
                + "-fx-border-color: " + hexColor + "88; -fx-border-width: 1; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + hexColor + "66; -fx-text-fill: #ffffff; "
                + "-fx-border-color: " + hexColor + "; -fx-border-width: 1; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void showItemActionPopup(Item item, GameSession session) {
        if (animationTimer != null) animationTimer.stop();
        Hero hero = session.getHero();

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        Label title = new Label("ITEM MENU");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.BLACK));

        Label nameLabel = new Label(item.getName());
        nameLabel.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#c8b880"));

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            boolean equipped = weapon == hero.getEquippedWeapon();
            Button btn = popupButton(equipped ? "Unequip" : "Equip " + weapon.getName(), "#e8c84a");
            btn.setOnAction(e -> {
                controller.equipWeapon(weapon);
                AudioManager.getInstance().play("interact");
                popup.close(); render();
            });
            actions.getChildren().add(btn);
        }
        if (item instanceof Wearable) {
            Wearable wearable = (Wearable) item;
            boolean worn = wearable == hero.getEquippedWearable();
            Button btn = popupButton(worn ? "Remove" : "Wear " + wearable.getName(), "#5dade2");
            btn.setOnAction(e -> {
                if (worn) controller.unequipWearable();
                else controller.wearItem(wearable);
                AudioManager.getInstance().play("interact");
                popup.close(); render();
            });
            actions.getChildren().add(btn);
        }
        if (item instanceof Scroll) {
            Scroll scroll = (Scroll) item;
            Button btn = popupButton("Read " + scroll.getName(), "#a29bfe");
            btn.setOnAction(e -> { 
                controller.readScroll(scroll); 
                AudioManager.getInstance().play("interact");
                popup.close(); render(); 
            });
            actions.getChildren().add(btn);
        }
        if (item instanceof Consumable) {
            Consumable cons = (Consumable) item;
            Button btn = popupButton("Eat " + cons.getName(), "#55efc4");
            btn.setOnAction(e -> {
                // Use EatAction pipeline
                com.kurawler.action.Action action = new com.kurawler.action.EatAction(cons);
                for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                    eff.apply(session);
                }
                AudioManager.getInstance().play("interact");
                popup.close(); render();
            });
            actions.getChildren().add(btn);
        }
        if (item instanceof com.kurawler.model.object.PortableContainer) {
            com.kurawler.model.object.PortableContainer bag = (com.kurawler.model.object.PortableContainer) item;
            Button btn = popupButton("Open " + bag.getName(), "#f39c12");
            btn.setOnAction(e -> {
                AudioManager.getInstance().play("interact");
                popup.close();
                Platform.runLater(() -> showContainerContentsPopup(bag, session));
            });
            actions.getChildren().add(btn);
        }

        Button discardBtn = popupButton("Discard", "#e74c3c");
        discardBtn.setOnAction(e -> {
            // Use DiscardAction pipeline
            com.kurawler.action.Action action = new com.kurawler.action.DiscardAction(item);
            for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                eff.apply(session);
            }
            AudioManager.getInstance().play("interact");
            popup.close(); render();
        });
        actions.getChildren().add(discardBtn);

        Button cancelBtn = popupButton("Cancel", "#a09070");
        cancelBtn.setOnAction(e -> popup.close());
        actions.getChildren().add(cancelBtn);

        VBox content = new VBox(10, title, nameLabel, actions);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        // Solid CSS background so the popup is never blank even if the Canvas
        // background fails to render on some platforms.
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #2a2418, #15110c); "
                + "-fx-border-color: #8b7940; -fx-border-width: 2; "
                + "-fx-border-radius: 6; -fx-background-radius: 6;");

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: #15110c;");
        popup.setScene(new Scene(root, 300, 260));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { 
            if (animationTimer != null) animationTimer.start(); 
            // Force keyboard focus back to the game canvas!
            Platform.runLater(() -> canvas.requestFocus()); 
        });
        popup.showAndWait();
    }

    private void showContainerContentsPopup(Object containerObj, GameSession session) {
        if (animationTimer != null) animationTimer.start(); // Keep background alive
        Hero hero = session.getHero();
        
        String containerName = (containerObj instanceof StaticContainer) 
            ? ((StaticContainer) containerObj).getName() 
            : ((com.kurawler.model.object.PortableContainer) containerObj).getName();
            
        List<Item> contents = (containerObj instanceof StaticContainer)
            ? ((StaticContainer) containerObj).getContents()
            : ((com.kurawler.model.object.PortableContainer) containerObj).getContents();

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 350;
        int popH = Math.max(200, 100 + (contents.size() * 50));
        Canvas bg = new Canvas(popW, popH);
        drawDungeonBg(bg, popW, popH);

        Label title = new Label(containerName.toUpperCase() + " CONTENTS");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e8c84a"));

        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);

        if (contents.isEmpty()) {
            Label emptyLbl = new Label("It is empty.");
            emptyLbl.setTextFill(Color.web("#888888"));
            itemBox.getChildren().add(emptyLbl);
        } else {
            for (Item item : contents) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER);
                
                Label nameLbl = new Label(item.getName());
                nameLbl.setPrefWidth(140);
                nameLbl.setTextFill(Color.web("#c8b880"));
                
                Button takeBtn = popupButton("Take", "#55efc4");
                takeBtn.setPrefWidth(80);
                takeBtn.setPrefHeight(30);
                
                if (hero.getInventory().isFull()) {
                    takeBtn.setDisable(true);
                    takeBtn.setText("Full");
                } else {
                    takeBtn.setOnAction(e -> {
                        com.kurawler.action.Action action = new com.kurawler.action.TakeFromContainerAction(containerObj, item);
                        for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                            eff.apply(session);
                        }
                        AudioManager.getInstance().play("item");
                        popup.close();
                        // Re-open to refresh the UI
                        Platform.runLater(() -> showContainerContentsPopup(containerObj, session));
                    });
                }
                
                row.getChildren().addAll(nameLbl, takeBtn);
                itemBox.getChildren().add(row);
            }
        }

        Button closeBtn = popupButton("Close", "#a09070");
        closeBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(15, title, itemBox, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-border-color: #8b794088; -fx-border-width: 2; -fx-border-radius: 6;");

        StackPane root = new StackPane(bg, content);
        popup.setScene(new Scene(root, popW, popH));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { 
            if (animationTimer != null) animationTimer.start(); 
            Platform.runLater(() -> canvas.requestFocus()); 
        });
        popup.showAndWait();
        render(); // Re-render when done
    }
    private void showItemActions(Item item, GameSession session, MouseEvent event) {
        showItemActionPopup(item, session);
    }

    private void updateAchievements(GameSession session) {
        AchievementManager manager = session.getAchievementManager();
        if (manager == null) {
            achievementsLabel.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Achievement achievement : manager.getAchievements()) {
            sb.append(achievement.isUnlocked() ? "  \u2605 " : "  \u2606 ")
              .append(achievement.getName())
              .append("\n");
        }
        achievementsLabel.setText(sb.toString());
    }

    private double[] lerpPosition(Object entity, double targetX, double targetY) {
        double[] pos = smoothPositions.get(entity);
        if (pos == null) {
            pos = new double[]{targetX, targetY};
            smoothPositions.put(entity, pos);
        }
        pos[0] += (targetX - pos[0]) * LERP_SPEED;
        pos[1] += (targetY - pos[1]) * LERP_SPEED;
        if (Math.abs(pos[0] - targetX) < 0.5) pos[0] = targetX;
        if (Math.abs(pos[1] - targetY) < 0.5) pos[1] = targetY;
        return pos;
    }

    private Button popupButton(String text, String accentColor) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        btn.setPrefWidth(240);
        btn.setPrefHeight(36);
        String base = "-fx-background-color: linear-gradient(to bottom, #3a3520, #252015); "
                + "-fx-text-fill: " + accentColor + "; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accentColor + "66; -fx-border-width: 1.5; -fx-cursor: hand;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #4a4530, #353025); "
                + "-fx-text-fill: #ffffff; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accentColor + "; -fx-border-width: 1.5; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, " + accentColor + "44, 6, 0, 0, 0);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void drawDungeonBg(Canvas c, int w, int h) {
        GraphicsContext gc = c.getGraphicsContext2D();
        SpriteSheet wallSheet = new SpriteSheet("/assets/walls_x3.png", 48);
        gc.setFill(Color.web("#0a0a10"));
        gc.fillRect(0, 0, w, h);
        if (wallSheet.isLoaded()) {
            for (int y = 0; y < h; y += 48) {
                for (int x = 0; x < w; x += 48) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    javafx.scene.image.Image tile = wallSheet.getSprite(hash % 5, 1);
                    if (tile != null) gc.drawImage(tile, x, y, 48, 48);
                }
            }
        }
        gc.setFill(Color.web("#0a0a10", 0.8));
        gc.fillRect(0, 0, w, h);
    }

    private void centerPopup(Stage popup) {
        popup.setX(stage.getX() + (stage.getWidth() - popup.getWidth()) / 2);
        popup.setY(stage.getY() + (stage.getHeight() - popup.getHeight()) / 2);
    }

    private ContextMenu styledContextMenu() {
        ContextMenu cm = new ContextMenu();
        cm.setStyle("-fx-background-color: linear-gradient(to bottom, #2e2a22, #1e1b16); "
                + "-fx-border-color: #8b7940; -fx-border-width: 2; "
                + "-fx-padding: 4;");
        return cm;
    }

    private void spawnHitParticles(int tileSx, int tileSy) {
        double cx = tileSx + TILE_SIZE / 2.0;
        double cy = tileSy + TILE_SIZE / 2.0;
        for (int i = 0; i < 10; i++) {
            hitParticles.add(new DustParticle(cx, cy));
        }
    }

    private void updateAndDrawHitParticles(GraphicsContext gc) {
        gc.save();
        java.util.Iterator<DustParticle> it = hitParticles.iterator();
        while (it.hasNext()) {
            DustParticle p = it.next();
            p.update();
            if (p.isExpired()) {
                it.remove();
                continue;
            }
            gc.setGlobalAlpha(p.getAlpha());
            gc.setFill(Color.web("#cc1111"));
            gc.fillOval(p.x - p.getRadius(), p.y - p.getRadius(),
                    p.getRadius() * 2, p.getRadius() * 2);
        }
        gc.restore();
    }
}
