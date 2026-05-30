package com.kurawler.ui;

import java.util.List;

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

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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

    private final java.util.Map<Object, double[]> smoothPositions = new java.util.HashMap<>();

    private javafx.scene.image.Image hudBarImg;
    private javafx.scene.image.Image invBgImg;
    private Label toastLabel;
    private PauseTransition toastTimer;
    private Region dimOverlay;
    private StackPane inGameMenuOverlay;

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
        achievementPopupLabel.setVisible(false);
        achievementPopupLabel.setEffect(new javafx.scene.effect.DropShadow(6, 0, 0, Color.web("#ffd54f", 0.5)));
        achievementPopupLabel.setMouseTransparent(true);

        messageLabel = new Label("");
        messageLabel.setFont(Font.font("Monospaced", 11));
        messageLabel.setTextFill(Color.web("#aaa090"));
        messageLabel.setMouseTransparent(true);

        toastLabel = new Label();
        toastLabel.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        toastLabel.setTextFill(Color.web("#f0e0b0"));
        toastLabel.setStyle("-fx-background-color: rgba(10,8,5,0.88); "
                + "-fx-background-radius: 6; -fx-padding: 5 16 5 16; "
                + "-fx-border-color: #8b7940; -fx-border-width: 1; -fx-border-radius: 6;");
        toastLabel.setMouseTransparent(true);
        toastLabel.setVisible(false);

        StackPane hudStack = new StackPane(hudCanvas, achievementPopupLabel, messageLabel);
        hudStack.setPrefHeight(HUD_HEIGHT);
        StackPane.setAlignment(achievementPopupLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(achievementPopupLabel, new Insets(0, 100, 0, 0));
        StackPane.setAlignment(messageLabel, Pos.CENTER);

        AchievementManager achievementManager = session.getAchievementManager();
        if (achievementManager != null) {
            achievementManager.setOnUnlock(this::showAchievementPopup);
        }

        StackPane canvasStack = new StackPane(canvas, toastLabel);
        StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
        StackPane.setMargin(toastLabel, new Insets(10, 0, 0, 0));

        dimOverlay = new Region();
        dimOverlay.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                        Color.color(0, 0, 0, 0.55),
                        javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        dimOverlay.setVisible(false);

        inGameMenuOverlay = new StackPane();
        inGameMenuOverlay.setVisible(false);
        inGameMenuOverlay.setAlignment(Pos.CENTER);
        inGameMenuOverlay.setMouseTransparent(true);

        BorderPane root = new BorderPane();
        root.setTop(hudStack);
        root.setCenter(canvasStack);
        root.setStyle("-fx-background-color: #0a0a10;");

        StackPane outerStack = new StackPane(root, dimOverlay, inGameMenuOverlay);
        Scene scene = new Scene(outerStack, canvasW, canvasH + HUD_HEIGHT);
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
        double w = hudCanvas.getWidth();
        double btnSize  = (HUD_HEIGHT - pad * 2 - 8) / 2.0 - 2;
        double btnX     = w - pad - (btnSize + 8);
        double bagBtnX  = btnX - 8 - 60;
        double achX     = bagBtnX - 8 - 60;
        double midLeft  = statsX + 120 + 14;
        double midRight = achX - 8;
        double statsX2  = midLeft + Math.max(0, (midRight - midLeft - 100) / 2.0);

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
        Platform.runLater(() -> showEndScreen(
                "\u2620 YOU DIED \u2620",
                "Your hero has fallen in the dungeon.\nThe darkness claims another soul...",
                "#ff4444", "#4a1a1a"));
    }

    private void showVictory() {
        AudioManager.getInstance().play("victory");
        GameSession session = controller.getSession();
        String itemName = session.getTargetObjective() != null
                ? session.getTargetObjective().getName() : "the artifact";
        String time = formatTime(session.getPlayDurationTimer());
        Platform.runLater(() -> showEndScreen(
                "\u2605 VICTORY \u2605",
                "You found the " + itemName + "!\n"
                + "Time: " + time + "  |  Turns: " + session.getTurnCount()
                + "\n\nCongratulations, brave adventurer!",
                "#e8c84a", "#3a3a1a"));
    }

    private void showTeamMatchResult(String result) {
        Platform.runLater(() -> showEndScreen(
                "\u2694 MATCH OVER \u2694", result, "#6fc3df", "#1a2a3a"));
    }

    private void showEndScreen(String title, String message, String accent, String bgTint) {
        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);

        Canvas bg = new Canvas(420, 300);
        GraphicsContext gc = bg.getGraphicsContext2D();
        SpriteSheet wallSheet = new SpriteSheet("/assets/walls_x3.png", 48);
        gc.setFill(Color.web("#0a0a10"));
        gc.fillRect(0, 0, 420, 300);
        if (wallSheet.isLoaded()) {
            for (int y = 0; y < 300; y += 48) {
                for (int x = 0; x < 420; x += 48) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    javafx.scene.image.Image tile = wallSheet.getSprite(hash % 5, 1);
                    if (tile != null) gc.drawImage(tile, x, y, 48, 48);
                }
            }
        }
        gc.setFill(Color.web(bgTint, 0.7));
        gc.fillRect(0, 0, 420, 300);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Serif", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(accent));
        javafx.scene.effect.InnerShadow engrave =
                new javafx.scene.effect.InnerShadow(3, 1, 2, Color.web("#000000", 0.6));
        javafx.scene.effect.DropShadow glow =
                new javafx.scene.effect.DropShadow(20, Color.web(accent, 0.5));
        glow.setInput(engrave);
        titleLabel.setEffect(glow);

        Label msgLabel = new Label(message);
        msgLabel.setFont(Font.font("Serif", 15));
        msgLabel.setTextFill(Color.web("#c8b880"));
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setMaxWidth(360);
        msgLabel.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.BLACK));

        Button okBtn = new Button("Return to Menu");
        okBtn.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        okBtn.setPrefWidth(200);
        okBtn.setPrefHeight(40);
        String base = "-fx-background-color: "
                + "linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16, #2a2720); "
                + "-fx-text-fill: " + accent + "; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "88 #3a3220 #3a3220 " + accent + "88; "
                + "-fx-border-width: 2; -fx-cursor: hand;";
        String hover = "-fx-background-color: "
                + "linear-gradient(to bottom, #5a5448, #3e3a30, #2a2720, #353028); "
                + "-fx-text-fill: #ffffff; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + " " + accent + "aa " + accent + "aa " + accent + "; "
                + "-fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, " + accent + "44, 10, 0, 0, 0);";
        okBtn.setStyle(base);
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(hover));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(base));
        okBtn.setOnAction(e -> {
            popup.close();
            screenManager.showMainMenu();
        });

        VBox content = new VBox(20, titleLabel, msgLabel, okBtn);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-border-color: " + accent + "66; -fx-border-width: 3; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");

        StackPane root = new StackPane(bg, content);
        popup.setScene(new Scene(root, 420, 300));
        popup.setTitle(title);

        popup.setOnShown(e -> {
            popup.setX(stage.getX() + (stage.getWidth() - 420) / 2);
            popup.setY(stage.getY() + (stage.getHeight() - 300) / 2);
        });
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
        String wpnName    = hero.getEquippedWeapon() != null  ? hero.getEquippedWeapon().getName()  : "None";
        String armorName  = hero.getEquippedArmor()  != null  ? hero.getEquippedArmor().getName()   : "None";
        String shieldName = hero.getEquippedShield() != null  ? hero.getEquippedShield().getName()  : "None";
        String ringName   = hero.getEquippedRing()   != null  ? hero.getEquippedRing().getName()    : "None";

        addStatRow(grid, 7,  "Weapon:", wpnName);
        addStatRow(grid, 8,  "Armor:",  armorName);
        addStatRow(grid, 9,  "Shield:", shieldName);
        addStatRow(grid, 10, "Ring:",   ringName);

        Button closeBtn = popupButton("Close", "#a09070");
        closeBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(20, title, grid, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-border-color: #8b794088; -fx-border-width: 2; -fx-border-radius: 6;");

        StackPane root = new StackPane(bg, content);
        popup.setScene(new Scene(root, popW, popH));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { if (animationTimer != null) animationTimer.start(); });
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
        if (inGameMenuOverlay != null && inGameMenuOverlay.isVisible()
                && event.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
            return;
        }
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
            case E:             controller.useFirstConsumable(); break;
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
                    showToast("Fired a projectile! (MP -10)");
                } else {
                    showToast("Not enough mana to shoot!");
                }
                break;
            case ESCAPE:        showInGameMenu(); return;
            default:            return;
        }

        // --- ENERJİ KONTROLÜ (BUG-01 FIX) ---
        if (isMoveKey) {
            if (hero.getEnergy() < 1) {
                showToast("Not enough energy, you CAN NOT MOVE!");
                
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
            showToast("Too far! Must be adjacent or on the same tile.");
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
            showToast("Nothing to interact with here.");
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
        popup.setOnHidden(e -> { if (animationTimer != null) animationTimer.start(); });
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
                        AudioManager.getInstance().play("item");
                        showToast("Picked up " + obj.getName());
                        break;
                    }
                }
                break;
            case "search":
                StaticObject so = cell.getStaticObject();
                if (so != null) {
                    Item hidden = so.getHiddenItem();
                    boolean searched = controller.searchObject(so, row, col);
                    if (searched && hidden != null)
                        showToast("Found: " + hidden.getName() + "!");
                    else if (searched) showToast("Nothing found.");
                    else showToast("Cannot search.");
                    AudioManager.getInstance().play("interact");
                }
                break;
            case "break":
                boolean broken = controller.breakObject(row, col);
                showToast(broken ? "Broke it!" : "Cannot break.");
                AudioManager.getInstance().play("interact"); // Added interact sound
                break;
            case "attack":
                if (cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                    hero.attack(enemy);
                    if (!enemy.isAlive()) {
                        cell.setOccupyingCharacter(null);
                        session.decrementEnemyCount();
                        AudioManager.getInstance().play("enemyDown");
                        showToast(enemy.getName() + " defeated!");
                    } else {
                        AudioManager.getInstance().play("attack");
                        showToast("Hit " + enemy.getName() + "! HP:" + enemy.getHealth());
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
                            showToast("Unlocked " + target + "!");
                        }
                        AudioManager.getInstance().play("interact");
                        // Wait for any previous popup to close, then open contents
                        Platform.runLater(() -> showContainerContentsPopup(sc, session));
                        break;
                    }
                }
                break;
            case "locked_info":
                showToast("It is locked. You need a specific key.");
                AudioManager.getInstance().play("error"); // Optional error sound
                break;
        }
        render();
    }

    private void showInGameMenu() {
        if (inGameMenuOverlay != null && inGameMenuOverlay.isVisible()) return;
        if (animationTimer != null) animationTimer.stop();

        // Dim the background
        dimOverlay.setVisible(true);
        dimOverlay.setMouseTransparent(false);

        String titleStyle = "-fx-font-family: 'Serif'; -fx-font-size: 22px; "
                + "-fx-font-weight: bold; -fx-text-fill: #e8c84a;";
        Label menuTitle = new Label("GAME MENU");
        menuTitle.setStyle(titleStyle);
        menuTitle.setEffect(new javafx.scene.effect.DropShadow(6, 0, 2, Color.BLACK));

        Button resumeBtn    = menuButton("Resume",          "#55efc4");
        Button saveBtn      = menuButton("Save",            "#e8c84a");
        Button loadBtn      = menuButton("Load",            "#5dade2");
        Button mainMenuBtn  = menuButton("Return to Menu",  "#e74c3c");

        VBox menuBox = new VBox(10, menuTitle, resumeBtn, saveBtn, loadBtn, mainMenuBtn);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(30, 40, 30, 40));
        menuBox.setMaxWidth(280);
        menuBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2a2418, #12100c); "
                + "-fx-border-color: #c8a840; -fx-border-width: 2; "
                + "-fx-border-radius: 8; -fx-background-radius: 8;");
        menuBox.setEffect(new javafx.scene.effect.DropShadow(20, 0, 4, Color.BLACK));

        inGameMenuOverlay.getChildren().setAll(menuBox);
        inGameMenuOverlay.setVisible(true);
        inGameMenuOverlay.setMouseTransparent(false);

        Runnable closeMenu = () -> {
            inGameMenuOverlay.setVisible(false);
            inGameMenuOverlay.setMouseTransparent(true);
            dimOverlay.setVisible(false);
            dimOverlay.setMouseTransparent(true);
        };

        resumeBtn.setOnAction(e -> {
            closeMenu.run();
            if (animationTimer != null) animationTimer.start();
        });
        saveBtn.setOnAction(e -> {
            closeMenu.run();
            boolean saved = screenManager.showSaveDialog();
            showToast(saved ? "Game saved." : "Save cancelled.");
            if (animationTimer != null) animationTimer.start();
        });
        loadBtn.setOnAction(e -> {
            closeMenu.run();
            boolean loaded = screenManager.showLoadDialogAndStart();
            if (!loaded) {
                showToast("No save file found.");
                if (animationTimer != null) animationTimer.start();
            }
        });
        mainMenuBtn.setOnAction(e -> {
            closeMenu.run();
            screenManager.showMainMenu();
        });
    }

    private Button menuButton(String text, String accent) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        String base = "-fx-background-color: linear-gradient(to bottom, #3a3520, #252015); "
                + "-fx-text-fill: " + accent + "; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "66; -fx-border-width: 1.5; -fx-cursor: hand;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #4a4530, #353025); "
                + "-fx-text-fill: #ffffff; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "; -fx-border-width: 1.5; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(ev -> btn.setStyle(hover));
        btn.setOnMouseExited(ev -> btn.setStyle(base));
        return btn;
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

                if (so != null && so.isSearchable()) {
                    renderer.drawSearchable(gc, sx, sy, so.getName());
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
                        renderer.drawItem(gc, sx, sy, "Consumable");
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
                            drawTeamIndicator(gc, (int) pos[0], (int) pos[1], team.getColor());
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

        if (session instanceof TeamMatchSession) {
            drawTeamIndicator(gc, heroSx, heroSy, Team.TeamColor.BLUE);
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

        drawHud(session);
    }

    private void drawHud(GameSession session) {
        Hero hero = session.getHero();
        GraphicsContext gc = hudCanvas.getGraphicsContext2D();
        double w = hudCanvas.getWidth();
        double h = HUD_HEIGHT;

        if (hudBarImg == null) {
            try {
                hudBarImg = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/assets/hud_bar.png"));
            } catch (Exception ignored) {}
        }

        gc.setFill(Color.web("#0e0c0a"));
        gc.fillRect(0, 0, w, h);
        if (hudBarImg != null && !hudBarImg.isError()) {
            gc.drawImage(hudBarImg, 0, 0, w, h);
        } else {
            gc.setStroke(Color.web("#c8a840"));
            gc.setLineWidth(2);
            gc.strokeLine(0, 0, w, 0);
            gc.strokeLine(0, h - 1, w, h - 1);
            gc.setStroke(Color.web("#8b7530", 0.6));
            gc.setLineWidth(1);
            gc.strokeLine(0, 2, w, 2);
            gc.strokeLine(0, h - 3, w, h - 3);
        }

        double pad = 6;

        // SECTION 1: Portrait
        double portraitSize = h - pad * 2;
        drawGoldPanel(gc, pad, pad, portraitSize, portraitSize);
        renderer.drawHeroFacing(gc,
                (int)(pad + (portraitSize - TILE_SIZE) / 2),
                (int)(pad + (portraitSize - TILE_SIZE) / 2),
                hero.getFacingRow(), hero.getFacingCol());

        // SECTION 2: HP / MP / EN bars
        double barStartX = pad + portraitSize + 8;
        double barW      = 150;
        double barH      = 18;
        double barGap    = 4;
        double barsTopY  = pad + 4;

        drawStatBar(gc, barStartX, barsTopY, barW, barH,
                "HP", hero.getHealth(),  hero.getMaxHealth(),  "#4a0e0e", "#b71c1c", "#ef5350");
        drawStatBar(gc, barStartX, barsTopY + barH + barGap, barW, barH,
                "MP", hero.getMana(),    hero.getMaxMana(),    "#0d2a5a", "#1565c0", "#42a5f5");
        drawStatBar(gc, barStartX, barsTopY + (barH + barGap) * 2, barW, barH,
                "EN", hero.getEnergy(),  hero.getMaxEnergy(),  "#4a3d00", "#f9a825", "#f4d03f");

        // SECTION 3: STR / ATK / DEF / Wpn text
        double statsX = barStartX + barW + 14;

        // Right-anchored sections — stretch with canvas width
        double btnSize  = (h - pad * 2 - 8) / 2.0 - 2;
        double btnX     = w - pad - (btnSize + 8);
        double bagBtnX  = btnX - 8 - 60;
        double achX     = bagBtnX - 8 - 60;
        // Timer section centred in the gap between stats and achievement panels
        double midLeft  = statsX + 120 + 14;
        double midRight = achX - 8;
        double statsX2  = midLeft + Math.max(0, (midRight - midLeft - 100) / 2.0);

        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.setLineWidth(1);
        gc.strokeLine(statsX - 7, pad + 4, statsX - 7, h - pad - 4);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("⚔ STR " + hero.getStrength(),    statsX, barsTopY + 14);
        gc.setFill(hero.getEquippedWeapon() != null ? Color.web("#ffd54f") : Color.web("#e8c84a"));
        gc.fillText("⚔ ATK " + hero.calculateDamage(), statsX, barsTopY + 28);
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("⛨ DEF " + hero.getDefensePower(), statsX, barsTopY + 42);
        String wpn = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "None";
        gc.setFill(Color.web("#aa9870"));
        gc.fillText("Wpn: " + wpn, statsX, barsTopY + 56);

        // SECTION 4: Timer / Turn / Foes
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.setLineWidth(1);
        gc.strokeLine(statsX2 - 7, pad + 4, statsX2 - 7, h - pad - 4);
        gc.setFill(Color.web("#c8b880"));
        gc.fillText("⏱ " + formatTime(session.getPlayDurationTimer()), statsX2, barsTopY + 14);
        gc.fillText("Turn " + session.getTurnCount(),         statsX2, barsTopY + 14 + 20);
        gc.setFill(Color.web("#aa9870"));
        gc.fillText("Foes " + session.getActiveEnemyCount(),  statsX2, barsTopY + 14 + 40);

        // SECTION 5: Achievement panel
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.strokeLine(achX - 7, pad + 4, achX - 7, h - pad - 4);

        AchievementManager am = session.getAchievementManager();
        int achUnlocked = 0, achTotal = 0;
        if (am != null) {
            for (com.kurawler.model.achievement.Achievement a : am.getAchievements()) {
                achTotal++;
                if (a.isUnlocked()) achUnlocked++;
            }
        }
        drawGoldPanel(gc, achX, pad + 4, 60, h - pad * 2 - 8);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("🏆", achX + 16, pad + 32);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillText(achUnlocked + "/" + achTotal, achX + 14, pad + 50);

        // SECTION 6: Bag button
        gc.setStroke(Color.web("#c8a840", 0.4));
        gc.strokeLine(bagBtnX - 7, pad + 4, bagBtnX - 7, h - pad - 4);

        drawGoldPanel(gc, bagBtnX, pad + 4, 60, h - pad * 2 - 8);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 26));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("🎒", bagBtnX + 14, pad + 36);
        Inventory inv = hero.getInventory();
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillText(inv.getItemCount() + "/8", bagBtnX + 14, pad + 52);
        
        // SECTION 7: Pause & Menu buttons (stacked)

        drawGoldPanel(gc, btnX, pad + 4, btnSize + 8, btnSize);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText(paused ? "▶" : "⏸", btnX + (btnSize + 8) / 2.0 - 6, pad + 4 + btnSize - 8);

        drawGoldPanel(gc, btnX, pad + 4 + btnSize + 4, btnSize + 8, btnSize);
        gc.fillText("☰", btnX + (btnSize + 8) / 2.0 - 6, h - pad - 10);
    }

    /** Show a floating toast above the map canvas; auto-hides after 2 s with fade-out. */
    private void showToast(String msg) {
        if (toastLabel == null) return;
        if (toastTimer != null) toastTimer.stop();
        toastLabel.setText(msg);
        toastLabel.setOpacity(1.0);
        toastLabel.setVisible(true);
        toastTimer = new PauseTransition(Duration.seconds(2));
        toastTimer.setOnFinished(e -> {
            javafx.animation.FadeTransition fade =
                    new javafx.animation.FadeTransition(Duration.millis(400), toastLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(f -> toastLabel.setVisible(false));
            fade.play();
        });
        toastTimer.play();
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

    private void buildInventoryUI(GameSession session) {
    }

    private void showInventoryPopup() {
        if (animationTimer != null) animationTimer.stop();
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        Inventory inv = hero.getInventory();

        if (invBgImg == null) {
            try {
                invBgImg = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/assets/Inventory.png"));
            } catch (Exception ignored) {}
        }

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        // Popup at 3.25× native (80×117) — fits 4 slots across while keeping the chest proportional
        int popW = 260;
        int popH = 380;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        // Background: Inventory.png scaled to match popup dimensions
        if (invBgImg != null && !invBgImg.isError()) {
            javafx.scene.image.ImageView bgView = new javafx.scene.image.ImageView(invBgImg);
            bgView.setFitWidth(popW);
            bgView.setFitHeight(popH);
            bgView.setPreserveRatio(true);
            root.getChildren().add(bgView);
        } else {
            Canvas bg = new Canvas(popW, popH);
            drawDungeonBg(bg, popW, popH);
            root.getChildren().add(bg);
        }

        // ── Item detail panel (bottom section of popup) ──
        Label detailName = new Label("Select an item");
        detailName.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        detailName.setTextFill(Color.web("#e8c84a"));
        Label detailType = new Label("");
        detailType.setFont(Font.font("Serif", 12));
        detailType.setTextFill(Color.web("#c8b880"));
        Label detailDesc = new Label("");
        detailDesc.setFont(Font.font("Serif", 11));
        detailDesc.setTextFill(Color.web("#a09070"));
        detailDesc.setWrapText(true);
        detailDesc.setMaxWidth(220);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);

        // ── Inventory grid (2 rows × 4 cols, aligned to blue area of Inventory x4.png) ──
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                Item item = inv.getItem(r, c);
                Button slot = new Button();
                slot.setPrefSize(50, 50);

                if (item != null) {
                    boolean isEquippedWpn  = item instanceof Weapon   && item == hero.getEquippedWeapon();
                    boolean isEquippedWear = item instanceof Wearable  && hero.isWearableEquipped((Wearable) item);
                    String prefix = isEquippedWpn ? "⚔ " : isEquippedWear ? "⛨ " : "";
                    slot.setText(prefix + item.getName());
                    slot.setFont(Font.font("Serif", FontWeight.BOLD, 10));
                    slot.setWrapText(true);
                    String equipped = (isEquippedWpn || isEquippedWear)
                            ? "-fx-border-color: #55efc4; " : "-fx-border-color: #8b7530; ";
                    String slotBase = "-fx-background-color: linear-gradient(to bottom, #d4c090, #b8a470); "
                            + equipped + "-fx-border-width: 2; -fx-cursor: hand; "
                            + "-fx-background-radius: 3; -fx-border-radius: 3;";
                    slot.setStyle(slotBase);
                    slot.setTextFill(Color.web("#2a1a00"));
                    final Item fItem = item;
                    slot.setOnMouseEntered(e -> slot.setStyle(
                            "-fx-background-color: linear-gradient(to bottom, #e8d4a0, #ccb480); "
                            + equipped + "-fx-border-width: 2; -fx-cursor: hand; "
                            + "-fx-background-radius: 3; -fx-border-radius: 3; "
                            + "-fx-effect: dropshadow(gaussian, #e8c84a44, 6, 0, 0, 0);"));
                    slot.setOnMouseExited(e -> slot.setStyle(slotBase));
                    slot.setOnAction(e -> {
                        // Update detail panel inline
                        detailName.setText(fItem.getName());
                        VBox.setMargin(detailName, new Insets(0, 0, 0, 0));
                        String typeStr = fItem instanceof Weapon ? "Weapon"
                                : fItem instanceof Wearable ? "Armor"
                                : fItem instanceof Consumable ? "Consumable"
                                : fItem instanceof Scroll ? "Scroll"
                                : fItem instanceof Key ? "Key" : "Item";
                        detailType.setText("Type: " + typeStr);
                        detailDesc.setText(fItem.getDescription() != null ? fItem.getDescription() : "");
                        // Rebuild action buttons
                        actionRow.getChildren().clear();
                        if (fItem instanceof Weapon) {
                            Weapon w = (Weapon) fItem;
                            boolean eq = w == hero.getEquippedWeapon();
                            Button b = invActionButton(eq ? "Unequip" : "Equip", "#e8c84a");
                            b.setOnAction(ev -> {
                                com.kurawler.action.Action a = new com.kurawler.action.EquipAction(w);
                                for (com.kurawler.effect.Effect ef : a.resolve(session)) ef.apply(session);
                                AudioManager.getInstance().play("interact");
                                popup.close(); render();
                            });
                            actionRow.getChildren().add(b);
                        }
                        if (fItem instanceof Wearable) {
                            Wearable ww = (Wearable) fItem;
                            boolean worn = hero.isWearableEquipped(ww);
                            Button b = invActionButton(worn ? "Remove" : "Wear", "#5dade2");
                            b.setOnAction(ev -> {
                                if (worn) controller.unequipWearable(ww); else controller.wearItem(ww);
                                AudioManager.getInstance().play("interact");
                                popup.close(); render();
                            });
                            actionRow.getChildren().add(b);
                        }
                        if (fItem instanceof Scroll) {
                            Scroll sc = (Scroll) fItem;
                            Button b = invActionButton("Read", "#a29bfe");
                            b.setOnAction(ev -> { controller.readScroll(sc); AudioManager.getInstance().play("interact"); popup.close(); render(); });
                            actionRow.getChildren().add(b);
                        }
                        if (fItem instanceof Consumable) {
                            Consumable co = (Consumable) fItem;
                            Button b = invActionButton("Eat", "#55efc4");
                            b.setOnAction(ev -> {
                                com.kurawler.action.Action a = new com.kurawler.action.EatAction(co);
                                for (com.kurawler.effect.Effect ef : a.resolve(session)) ef.apply(session);
                                AudioManager.getInstance().play("interact");
                                popup.close(); render();
                            });
                            actionRow.getChildren().add(b);
                        }
                        if (fItem instanceof com.kurawler.model.object.PortableContainer) {
                            com.kurawler.model.object.PortableContainer bag =
                                    (com.kurawler.model.object.PortableContainer) fItem;
                            Button b = invActionButton("Open", "#f39c12");
                            b.setOnAction(ev -> {
                                AudioManager.getInstance().play("interact");
                                popup.close();
                                Platform.runLater(() -> showContainerContentsPopup(bag, session));
                            });
                            actionRow.getChildren().add(b);
                        }
                        Button dropBtn = invActionButton("Drop", "#e74c3c");
                        dropBtn.setOnAction(ev -> {
                            com.kurawler.action.Action a = new com.kurawler.action.DiscardAction(fItem);
                            for (com.kurawler.effect.Effect ef : a.resolve(session)) ef.apply(session);
                            AudioManager.getInstance().play("interact");
                            popup.close(); render();
                        });
                        actionRow.getChildren().add(dropBtn);
                    });
                } else {
                    slot.setStyle("-fx-background-color: #3a5a7888; -fx-border-color: #2a4a6866; "
                            + "-fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
                    slot.setDisable(true);
                }
                grid.add(slot, c, r);
            }
        }

        Button closeBtn = invActionButton("Close", "#a09070");
        closeBtn.setPrefWidth(100);
        closeBtn.setOnAction(e -> popup.close());

        VBox detailBox = new VBox(4, detailName, detailType, detailDesc, actionRow);
        detailBox.setAlignment(Pos.CENTER_LEFT);
        detailBox.setPadding(new Insets(4, 12, 0, 12));
        VBox.setMargin(detailName, new Insets(10, 0, 0, 0));

        VBox content = new VBox(0, grid, detailBox, closeBtn);
        content.setAlignment(Pos.CENTER);
        // Top padding aligns grid with the blue cell area of Inventory.png at 3.25× (260×380)
        content.setPadding(new Insets(155, 14, 8, 14));
        // Push detailBox below the blue cell zone into the lower brown panel
        VBox.setMargin(detailBox, new Insets(50, 0, 0, 0));
        VBox.setMargin(closeBtn, new Insets(6, 0, 0, 0));

        root.getChildren().add(content);
        Scene scene = new Scene(root, popW, popH);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> { if (animationTimer != null) animationTimer.start(); });
        popup.showAndWait();
    }

    private Button invActionButton(String text, String accent) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        btn.setPrefHeight(30);
        String base = "-fx-background-color: linear-gradient(to bottom, #3a3520, #252015); "
                + "-fx-text-fill: " + accent + "; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "66; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 4 12 4 12;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #4a4530, #353025); "
                + "-fx-text-fill: #ffffff; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 4 12 4 12;";
        btn.setStyle(base);
        btn.setOnMouseEntered(ev -> btn.setStyle(hover));
        btn.setOnMouseExited(ev -> btn.setStyle(base));
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
                // Use EquipAction pipeline
                com.kurawler.action.Action action = new com.kurawler.action.EquipAction(weapon);
                for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                    eff.apply(session);
                }
                AudioManager.getInstance().play("interact");
                popup.close(); render();
            });
            actions.getChildren().add(btn);
        }
        if (item instanceof Wearable) {
            Wearable wearable = (Wearable) item;
            boolean worn = hero.isWearableEquipped(wearable);
            Button btn = popupButton(worn ? "Remove" : "Wear " + wearable.getName(), "#5dade2");
            btn.setOnAction(e -> {
                if (worn) controller.unequipWearable(wearable);
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
        popup.setOnHidden(e -> { if (animationTimer != null) animationTimer.start(); });
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
}
