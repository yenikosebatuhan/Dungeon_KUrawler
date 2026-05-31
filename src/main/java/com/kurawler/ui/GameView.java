package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.AudioManager;
import com.kurawler.engine.GameLoop;
import com.kurawler.engine.TeamMatchGameLoop;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.WeaponCatalog;
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
import com.kurawler.effect.MapTransitionEffect;
import com.kurawler.effect.MoveEffect;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;

import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
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

/**
 * JavaFX game view — renders the grid and handles key/mouse input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    private static final int HUD_HEIGHT = 90;
    private static final int INFO_BAR_HEIGHT = 34;
    private static final double LERP_SPEED = 0.2;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Stage stage;
    private Canvas canvas;
    private Canvas hudCanvas;
    private Canvas infoCanvas;
    private Label messageLabel;
    private double mapScaleX = 1.0;
    private double mapScaleY = 1.0;
    private PauseTransition messageDismissTimer;
    private Label achievementsLabel;
    private Label achievementPopupLabel;
    private GameLoop gameLoop;
    private TeamMatchGameLoop teamMatchGameLoop;
    private AnimationTimer animationTimer;
    private boolean paused;
    private Scene playScene;
    private boolean playActive;
    private boolean endScreenShown;

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

        int viewportW = mapViewportWidth();
        int viewportH = mapViewportHeight();
        mapScaleX = viewportW / (double) canvasW;
        mapScaleY = viewportH / (double) canvasH;

        canvas = new Canvas(viewportW, viewportH);
        canvas.setFocusTraversable(true);
        canvas.setOnMouseClicked(e -> {
            handleMouseClick(e);
            canvas.requestFocus();
        });

        hudCanvas = new Canvas(ScreenManager.WINDOW_W, HUD_HEIGHT);
        hudCanvas.setFocusTraversable(false);
        hudCanvas.setOnMouseClicked(e -> {
            handleHudClick(e);
            canvas.requestFocus();
        });

        achievementsLabel = new Label();

        achievementPopupLabel = new Label();
        achievementPopupLabel.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        achievementPopupLabel.setTextFill(Color.web("#ffd54f"));
        achievementPopupLabel.setVisible(false);
        achievementPopupLabel.setEffect(new javafx.scene.effect.DropShadow(6, 0, 0, Color.web("#ffd54f", 0.5)));
        achievementPopupLabel.setMouseTransparent(true);

        messageLabel = new Label("");
        messageLabel.setFont(Font.font("Monospaced", 12));
        messageLabel.setTextFill(Color.web("#d4c8a8"));
        messageLabel.setMouseTransparent(true);
        messageLabel.setPadding(new Insets(0, 14, 0, 28));
        messageLabel.setMaxWidth(ScreenManager.WINDOW_W - 40);
        messageLabel.setWrapText(true);

        infoCanvas = new Canvas(ScreenManager.WINDOW_W, INFO_BAR_HEIGHT);
        infoCanvas.setFocusTraversable(false);
        infoCanvas.setMouseTransparent(true);

        StackPane infoStack = new StackPane(infoCanvas, messageLabel);
        infoStack.setFocusTraversable(false);
        infoStack.setPrefHeight(INFO_BAR_HEIGHT);
        infoStack.setMinHeight(INFO_BAR_HEIGHT);
        infoStack.setMaxHeight(INFO_BAR_HEIGHT);
        infoStack.setPrefWidth(ScreenManager.WINDOW_W);
        StackPane.setAlignment(messageLabel, Pos.CENTER_LEFT);

        StackPane hudStack = new StackPane(hudCanvas, achievementPopupLabel);
        hudStack.setFocusTraversable(false);
        hudStack.setPrefHeight(HUD_HEIGHT);
        hudStack.setPrefWidth(ScreenManager.WINDOW_W);
        StackPane.setAlignment(achievementPopupLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(achievementPopupLabel, new Insets(0, 100, 0, 0));

        AchievementManager achievementManager = session.getAchievementManager();
        if (achievementManager != null) {
            achievementManager.setOnUnlock(this::showAchievementPopup);
        }

        BorderPane root = new BorderPane();
        root.setTop(hudStack);
        root.setCenter(canvas);
        root.setBottom(infoStack);
        root.setFocusTraversable(false);
        root.setStyle("-fx-background-color: #0a0a10;");

        Scene scene = new Scene(root, ScreenManager.WINDOW_W, ScreenManager.WINDOW_H);
        scene.getRoot().setStyle(
                "-fx-font-family: 'Serif'; "
                + "-fx-base: #1e1b16; "
                + "-fx-accent: #8b7940; "
                + "-fx-focus-color: #c8a850; "
                + "-fx-faint-focus-color: transparent;");
        wirePlayKeyboard(scene, root, canvas);

        stage.setTitle("Dungeon KUrawler");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnShown(e -> Platform.runLater(() -> {
            stage.requestFocus();
            canvas.requestFocus();
        }));
        stage.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                Platform.runLater(() -> canvas.requestFocus());
            }
        });
        stage.show();
        Platform.runLater(() -> {
            stage.requestFocus();
            canvas.requestFocus();
        });

        playScene = scene;
        playActive = true;
        endScreenShown = false;
        hitParticles.clear();
        lastHeroHp = Integer.MAX_VALUE;

        if (!(session instanceof TeamMatchSession)) {
            session.setCurrentMode("Play");
        }

        // PDF §4.1.2: Show target item popup at the start of a new play session.
        // Team Match has no target item, so skip in that mode.
        // Skip for editor test play — return to editor without quest/death overlays.
        // IMPORTANT: defer startGameLoop() until the popup is dismissed so that
        // enemies do not move/attack while the player is still reading the quest.
        if (!(session instanceof TeamMatchSession)
                && !session.isEditorTestPlay()
                && session.getTargetObjective() != null) {
            javafx.application.Platform.runLater(() -> {
                new TargetItemPopup(stage, session.getTargetObjective()).show();
                Hero h = session.getHero();
                if (h != null) {
                    lastHeroHp = h.getHealth();
                }
                hitParticles.clear();
                startGameLoop(session);   // blocks above: showAndWait() returns only after dismiss
                Platform.runLater(() -> canvas.requestFocus());
            });
        } else {
            startGameLoop(session);
        }
        render();
        if (!session.isEditorTestPlay() && session.getTargetObjective() != null
                && "hall".equals(session.getCurrentMapId())) {
            showGameMessage(
                    "EXIT: east DOOR (\u2192). SEARCH a shelf (?) for the Golden Key. Win item is in a vault chest.");
        }
    }

    /** Shows feedback on the bottom info bar (combat, pickup, equip, errors). */
    private void showGameMessage(String text) {
        if (messageLabel != null) {
            messageLabel.setText(text != null ? text : "");
        }
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
                        animationTimer = null;
                        handlePlayEnded(session, true);
                    } else if (session.checkWinCondition()) {
                        animationTimer.stop();
                        animationTimer = null;
                        handlePlayEnded(session, false);
                    }
                }
            };
        }
        animationTimer.start();
    }

    /** Stops loop and UI; editor test play returns to editor without YOU DIED on editor. */
    public void shutdown() {
        playActive = false;
        endScreenShown = true;
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        if (teamMatchGameLoop != null) {
            teamMatchGameLoop.stop();
            teamMatchGameLoop = null;
        }
        gameLoop = null;
    }

    private boolean isPlaySceneActive() {
        return playActive && stage != null && stage.getScene() == playScene;
    }

    private void handlePlayEnded(GameSession session, boolean died) {
        if (session.isEditorTestPlay()) {
            Platform.runLater(() -> {
                if (!isPlaySceneActive()) {
                    return;
                }
                shutdown();
                String msg = died
                        ? "Test play ended — hero died. Map unchanged in editor."
                        : "Test play ended — objective complete.";
                screenManager.returnToEditorAfterTest(session.getMap(), msg);
            });
            return;
        }
        if (died) {
            showGameOver();
        } else {
            showVictory();
        }
    }

    private void showGameOver() {
        if (endScreenShown || !isPlaySceneActive()) {
            return;
        }
        endScreenShown = true;
        AudioManager.getInstance().playMusic("gameover", false);
        Platform.runLater(() -> {
            if (!isPlaySceneActive()) {
                return;
            }
            showEndScreen(
                    "\u2620 YOU DIED \u2620",
                    "Your hero has fallen in the dungeon.\nThe darkness claims another soul...",
                    "#ff4444", "#4a1a1a");
        });
    }

    private void showVictory() {
        if (endScreenShown || !isPlaySceneActive()) {
            return;
        }
        endScreenShown = true;
        AudioManager.getInstance().playMusic("victory", false);
        GameSession session = controller.getSession();
        String itemName = session.getTargetObjective() != null
                ? session.getTargetObjective().getName() : "the artifact";
        String time = formatTime(session.getPlayDurationTimer());
        Platform.runLater(() -> {
            if (!isPlaySceneActive()) {
                return;
            }
            showEndScreen(
                    "\u2605 VICTORY \u2605",
                    "You found the " + itemName + "!\n"
                    + "Time: " + time + "  |  Turns: " + session.getTurnCount()
                    + "\n\nCongratulations, brave adventurer!",
                    "#e8c84a", "#3a3a1a");
        });
    }

    private void showTeamMatchResult(String result) {
        Platform.runLater(() -> showEndScreen(
                "\u2694 MATCH OVER \u2694", result, "#6fc3df", "#1a2a3a"));
    }

    private void showEndScreen(String title, String message, String accent, String bgTint) {
        if (!isPlaySceneActive()) {
            return;
        }
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
            playActive = false;
            shutdown();
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
        String wpnName = formatEquippedWeaponLine(hero);
        String armorName = hero.getEquippedArmor() != null ? hero.getEquippedArmor().getName() : "None";
        String shieldName = hero.getEquippedShield() != null ? hero.getEquippedShield().getName() : "None";
        String ringName = hero.getEquippedRing() != null ? hero.getEquippedRing().getName() : "None";

        addStatRow(grid, 7, "Weapon:", wpnName);
        addStatRow(grid, 8, "Armor:", armorName);
        addStatRow(grid, 9, "Shield:", shieldName);
        addStatRow(grid, 10, "Ring:", ringName);

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
    /**
     * Mac spacebar (between Command keys) — KEY_PRESSED should be {@link KeyCode#SPACE};
     * also accept typed space character as a fallback on some layouts.
     */
    private static boolean isSpaceBarKey(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            return true;
        }
        String text = event.getText();
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == ' ') {
                    return true;
                }
            }
        }
        return false;
    }

    private void wirePlayKeyboard(Scene scene, BorderPane root, Canvas playCanvas) {
        achievementPopupLabel.setFocusTraversable(false);
        messageLabel.setFocusTraversable(false);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        Runnable focusGame = () -> {
            if (stage != null) {
                stage.requestFocus();
            }
            playCanvas.requestFocus();
        };
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> focusGame.run());
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        if (isSpaceBarKey(event)) {
            if (!paused) {
                event.consume();
                handleSpaceWeaponAttack();
                render();
            }
            return;
        }

        if (paused && event.getCode() != KeyCode.ESCAPE && event.getCode() != KeyCode.P) {
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
            case SPACE:
                if (!tryAttackFacingEnemy()) {
                    messageLabel.setText("No enemy in facing direction.");
                }
                event.consume();
                break;
            case E:
                if (tryUseAdjacentPortal()) {
                    render();
                    break;
                }
                if (tryOpenAdjacentContainer()) {
                    break;
                }
                if (!tryTakeAdjacentItem()) {
                    controller.useFirstConsumable();
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
                String shootResult = controller.heroShoot();
                if (shootResult != null && !shootResult.isEmpty()) {
                    if (shootResult.contains("Not enough")) {
                        AudioManager.getInstance().play("error");
                    } else {
                        AudioManager.getInstance().play("attack");
                    }
                    showGameMessage(shootResult);
                }
                break;
            case P:
                togglePause();
                break;
            case ESCAPE:
                event.consume();
                showInGameMenu();
                return;
            default:
                return;
        }

        event.consume();

        if (isMoveKey) {
            hero.setFacingFromDelta(dRow, dCol);
            if (hero.getEnergy() < 1) {
                showGameMessage("Not enough energy, you CAN NOT MOVE!");
            } else {
                controller.moveHero(dRow, dCol);
                showPortalMessageIfAny();
            }
        }

        render();
    }

    private void handleSpaceWeaponAttack() {
        if (paused) {
            return;
        }
        String result = controller.trySpaceWeaponAttack();
        if ("attack_melee".equals(result)) {
            Hero hero = controller.getSession().getHero();
            com.kurawler.model.Map map = controller.getSession().getMap();
            int[] dir = hero.attackDirection();
            int row = hero.getRow() + dir[0];
            int col = hero.getCol() + dir[1];
            GridCell cell = map.getCell(row, col);
            if (cell != null && cell.getOccupyingCharacter() instanceof Enemy enemy) {
                if (!enemy.isAlive()) {
                    cell.setOccupyingCharacter(null);
                    controller.getSession().decrementEnemyCount();
                    AudioManager.getInstance().play("enemyDown");
                    showGameMessage(enemy.getName() + " defeated!");
                } else {
                    AudioManager.getInstance().play("attack");
                    showGameMessage("Hit " + enemy.getName() + "! HP:" + enemy.getHealth());
                }
            }
        } else if (result != null && !result.isEmpty()) {
            if (result.contains("arrow") || result.contains("bomb") || result.contains("Loosed")
                    || result.contains("Threw") || result.contains("Strike")
                    || result.contains("detonated")) {
                AudioManager.getInstance().play("attack");
            } else if (result.contains("energy") || result.contains("Equip") || result.contains("enemy")
                    || result.contains("blocked") || result.contains("Cannot") || result.contains("Cast with F")) {
                AudioManager.getInstance().play("error");
            }
            showGameMessage(result);
        }
        render();
    }

    private void showInitialPlayMessages(GameSession session) {
        if (session.isEditorTestPlay() || session instanceof TeamMatchSession) {
            return;
        }
        showPortalMessageIfAny();
        if (messageLabel.getText() == null || messageLabel.getText().isEmpty()) {
            String mapId = session.getCurrentMapId();
            if (mapId != null && !mapId.isEmpty()) {
                showGameMessage("Entered " + mapDisplayName(mapId) + ".");
            }
        }
        if (session.getTargetObjective() != null && "hall".equals(session.getCurrentMapId())) {
            showGameMessage(
                    "EXIT: east DOOR (\u2192). SEARCH a shelf (?) for the Golden Key. Win item is in a vault chest.");
        }
    }

    private static String mapDisplayName(String mapId) {
        if ("hall".equals(mapId)) {
            return "Entry Hall";
        }
        if ("catacombs".equals(mapId)) {
            return "Catacombs";
        }
        if ("vault".equals(mapId)) {
            return "Treasure Vault";
        }
        return mapId;
    }

    private void showPortalMessageIfAny() {
        String portalMsg = controller.getSession().consumeLastPortalMessage();
        if (portalMsg != null && !portalMsg.isEmpty()) {
            showGameMessage(portalMsg);
        }
    }

    /**
     * Space key: melee attack the enemy one tile ahead in the hero's facing direction.
     */
    private boolean tryAttackFacingEnemy() {
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        int row = hero.getRow() + hero.getFacingRow();
        int col = hero.getCol() + hero.getFacingCol();
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null || !(cell.getOccupyingCharacter() instanceof Enemy enemy)
                || !enemy.isAlive()) {
            return false;
        }
        executeInteraction("attack", row, col, enemy.getName(), session);
        return true;
    }

    /**
     * E key: melee attack the first adjacent enemy (same as ATTACK in the click menu).
     */
    private boolean tryAttackAdjacentEnemy() {
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();
        int hr = hero.getRow();
        int hc = hero.getCol();
        int[][] offsets = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
                {0, 0}
        };
        for (int[] off : offsets) {
            int row = hr + off[0];
            int col = hc + off[1];
            GridCell cell = map.getCell(row, col);
            if (cell == null) {
                continue;
            }
            if (cell.getOccupyingCharacter() instanceof Enemy enemy && enemy.isAlive()) {
                executeInteraction("attack", row, col, enemy.getName(), session);
                return true;
            }
        }
        return false;
    }

    /**
     * E key: pick up the first item on the hero's tile or any adjacent tile (3×3).
     * If nothing to pick up, caller may fall back to using a consumable.
     */
    private boolean tryTakeAdjacentItem() {
        Hero hero = controller.getSession().getHero();
        com.kurawler.model.Map map = controller.getSession().getMap();
        int hr = hero.getRow();
        int hc = hero.getCol();

        int[][] offsets = {
                {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        for (int[] off : offsets) {
            int row = hr + off[0];
            int col = hc + off[1];
            GridCell cell = map.getCell(row, col);
            if (cell == null) {
                continue;
            }
            for (GameObject obj : cell.getObjects()) {
                if (!(obj instanceof Item)) {
                    continue;
                }
                if (hero.getInventory().isFull()) {
                    showGameMessage("Inventory full");
                    return true;
                }
                controller.interactWithObject(row, col, (Item) obj);
                AudioManager.getInstance().play("item");
                showGameMessage("Picked up " + obj.getName());
                render();
                return true;
            }
        }
        return false;
    }

    /**
     * E key: open the first adjacent StaticContainer (unlocked).
     */
    /** E key or fallback: use a door on the hero tile or any adjacent tile. */
    private boolean tryUseAdjacentPortal() {
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();
        int hr = hero.getRow();
        int hc = hero.getCol();
        int[][] offsets = {
                {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };
        for (int[] off : offsets) {
            int row = hr + off[0];
            int col = hc + off[1];
            GridCell cell = map.getCell(row, col);
            if (cell == null) {
                continue;
            }
            StaticObject so = cell.getStaticObject();
            if (so instanceof MapPortal) {
                usePortalAt(session, hero, row, col, (MapPortal) so);
                return true;
            }
        }
        return false;
    }

    private void usePortalAt(GameSession session, Hero hero, int row, int col, MapPortal portal) {
        if (!portal.isUnlocked() && !portal.tryUnlock(hero)) {
            showGameMessage(portal.getLockHint());
            AudioManager.getInstance().play("error");
            return;
        }
        if (hero.getRow() != row || hero.getCol() != col) {
            new MoveEffect(hero, row, col).apply(session);
        }
        new MapTransitionEffect(portal).apply(session);
        showPortalMessageIfAny();
        AudioManager.getInstance().play("interact");
    }

    private boolean tryOpenAdjacentContainer() {
        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();
        int hr = hero.getRow();
        int hc = hero.getCol();

        int[][] offsets = {
                {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        for (int[] off : offsets) {
            GridCell cell = map.getCell(hr + off[0], hc + off[1]);
            if (cell == null) {
                continue;
            }
            for (GameObject obj : cell.getObjects()) {
                if (!(obj instanceof StaticContainer)) {
                    continue;
                }
                StaticContainer sc = (StaticContainer) obj;
                if (sc.isLocked()) {
                    showGameMessage("It is locked. You need a specific key.");
                    AudioManager.getInstance().play("error");
                    return true;
                }
                AudioManager.getInstance().play("interact");
                showContainerContentsPopup(sc, session);
                return true;
            }
        }
        return false;
    }

    /** 3x3 Interaction: click an adjacent cell to interact with objects on it. */
    private void handleMouseClick(MouseEvent event) {
        int col = (int) (event.getX() / mapScaleX / TILE_SIZE);
        int row = (int) (event.getY() / mapScaleY / TILE_SIZE);

        GameSession session = controller.getSession();
        Hero hero = session.getHero();
        com.kurawler.model.Map map = session.getMap();

        int dRow = Math.abs(row - hero.getRow());
        int dCol = Math.abs(col - hero.getCol());
        
        if (dRow > 1 || dCol > 1) {
            showGameMessage("Too far! Must be adjacent or on the same tile.");
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
        if (staticObj instanceof MapPortal) {
            MapPortal portal = (MapPortal) staticObj;
            if (portal.isUnlocked() || portal.isPassable()) {
                actionList.add(new String[]{"ENTER", portal.getName(), "use_portal"});
            } else {
                actionList.add(new String[]{"UNLOCK DOOR", portal.getName(), "use_portal"});
            }
        } else if (staticObj != null && staticObj.isSearchable()
                && !(staticObj instanceof MapPortal)) {
            actionList.add(new String[]{"SEARCH", staticObj.getName(), "search"});
            actionList.add(new String[]{"BREAK", staticObj.getName(), "break"});
        }
        if (cell.getOccupyingCharacter() instanceof Enemy) {
            Enemy enemy = (Enemy) cell.getOccupyingCharacter();
            actionList.add(new String[]{"ATTACK", enemy.getName(), "attack"});
        }

        if (actionList.isEmpty()) {
            showGameMessage("Nothing to interact with here.");
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
                if (hero.getInventory().isFull()) {
                    showGameMessage("Inventory full");
                } else {
                    for (GameObject obj : cell.getObjects()) {
                        if (obj instanceof Item && obj.getName().equals(target)) {
                            controller.interactWithObject(row, col, (Item) obj);
                            AudioManager.getInstance().play("item");
                            showGameMessage("Picked up " + obj.getName());
                            break;
                        }
                    }
                }
                break;
            case "use_portal":
                StaticObject portalObj = cell.getStaticObject();
                if (portalObj instanceof MapPortal) {
                    usePortalAt(session, hero, row, col, (MapPortal) portalObj);
                }
                break;
            case "search":
                StaticObject so = cell.getStaticObject();
                if (so != null) {
                    Item hidden = so.getHiddenItem();
                    boolean searched = controller.searchObject(so);
                    if (hidden != null && searched)
                        showGameMessage("Found: " + hidden.getName() + "!");
                    else if (searched) showGameMessage("Nothing found.");
                    else showGameMessage("Cannot search.");
                    
                    AudioManager.getInstance().play("interact"); // Added interact sound
                }
                break;
            case "break":
                boolean broken = controller.breakObject(row, col);
                showGameMessage(broken ? "Broke it!" : "Cannot break.");
                AudioManager.getInstance().play("interact"); // Added interact sound
                break;
            case "attack":
                if (cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                    if (!hero.attack(enemy)) {
                        showGameMessage("Not enough energy to attack!");
                        AudioManager.getInstance().play("error");
                        break;
                    }
                    if (!enemy.isAlive()) {
                        cell.setOccupyingCharacter(null);
                        session.decrementEnemyCount();
                        AudioManager.getInstance().play("enemyDown");
                        showGameMessage(enemy.getName() + " defeated!");
                    } else {
                        AudioManager.getInstance().play("attack");
                        showGameMessage("Hit " + enemy.getName() + "! HP:" + enemy.getHealth());
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
                            showGameMessage("Unlocked " + target + "!");
                        }
                        AudioManager.getInstance().play("interact");
                        // Wait for any previous popup to close, then open contents
                        Platform.runLater(() -> showContainerContentsPopup(sc, session));
                        break;
                    }
                }
                break;
            case "locked_info":
                showGameMessage("It is locked. You need a specific key.");
                AudioManager.getInstance().play("error"); // Optional error sound
                break;
        }
        render();
    }

    private void showInGameMenu() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        AudioManager.getInstance().play("button");

        final boolean[] leavePlay = {false};

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(stage);
        popup.initStyle(StageStyle.TRANSPARENT);

        int popupW = 440;
        int popupH = 340;
        Canvas bg = new Canvas(popupW, popupH);
        drawDungeonBg(bg, popupW, popupH);

        Label title = new Label("\u2630  GAME MENU");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(6, 0, 1, Color.BLACK));

        Label subtitle = new Label(
                "Save your progress, load a previous run, or return to the title screen.");
        subtitle.setFont(Font.font("Monospaced", 12));
        subtitle.setTextFill(Color.web("#c8b880"));
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setMaxWidth(360);
        subtitle.setAlignment(Pos.CENTER);

        Button saveBtn = popupButton("Save Game", "#55efc4");
        Button loadBtn = popupButton("Load Game", "#5dade2");
        Button mainMenuBtn = popupButton("Main Menu", "#e8c84a");
        Button resumeBtn = popupButton("Resume", "#a09070");

        Runnable closePopup = popup::close;

        saveBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            boolean saved = screenManager.showSaveDialog();
            showGameMessage(saved ? "Game saved." : "Save cancelled.");
            closePopup.run();
        });
        loadBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            boolean loaded = screenManager.showLoadDialogAndStart();
            if (loaded) {
                leavePlay[0] = true;
                animationTimer = null;
                closePopup.run();
            } else {
                showGameMessage("No save file found.");
            }
        });
        mainMenuBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            leavePlay[0] = true;
            closePopup.run();
            GameSession s = controller.getSession();
            if (s.isEditorTestPlay()) {
                shutdown();
                screenManager.returnToEditorAfterTest(
                        s.getMap(), "Returned to level editor.");
            } else {
                screenManager.showMainMenu();
            }
        });
        resumeBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            closePopup.run();
        });

        VBox panel = new VBox(12, title, subtitle, saveBtn, loadBtn, mainMenuBtn, resumeBtn);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(22, 28, 24, 28));
        panel.setStyle("-fx-background-color: rgba(14,12,10,0.94); "
                + "-fx-border-color: #c8a840; -fx-border-width: 2.5; "
                + "-fx-border-radius: 10; -fx-background-radius: 10;");

        StackPane root = new StackPane(bg, panel);
        StackPane.setAlignment(panel, Pos.CENTER);

        Scene scene = new Scene(root, popupW, popupH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                resumeBtn.fire();
            }
        });
        popup.setScene(scene);
        popup.setResizable(false);
        popup.setOnShown(ev -> centerPopup(popup));
        popup.showAndWait();

        if (!leavePlay[0] && animationTimer != null) {
            animationTimer.start();
        }
        Platform.runLater(() -> canvas.requestFocus());
    }

    private void togglePause() {
        AudioManager.getInstance().play("button");
        paused = !paused;
        GameSession session = controller.getSession();
        if (paused) {
            session.setCurrentMode("Paused");
            showGameMessage("PAUSED");
        } else {
            session.setCurrentMode(session instanceof TeamMatchSession ? "TeamMatch" : "Play");
            showGameMessage("");
        }
        render();
    }

    private static int mapViewportWidth() {
        return ScreenManager.WINDOW_W;
    }

    private static int mapViewportHeight() {
        return ScreenManager.WINDOW_H - HUD_HEIGHT - INFO_BAR_HEIGHT;
    }

    private void updateMapDrawScale(com.kurawler.model.Map map) {
        int mapPixelW = map.getWidth() * TILE_SIZE;
        int mapPixelH = map.getHeight() * TILE_SIZE;
        mapScaleX = mapViewportWidth() / (double) mapPixelW;
        mapScaleY = mapViewportHeight() / (double) mapPixelH;
    }

    private void ensureCanvasFitsMap(com.kurawler.model.Map map) {
        int vw = mapViewportWidth();
        int vh = mapViewportHeight();
        if (canvas.getWidth() != vw || canvas.getHeight() != vh) {
            canvas.setWidth(vw);
            canvas.setHeight(vh);
        }
        canvas.getTransforms().clear();
        updateMapDrawScale(map);
        if (hudCanvas.getWidth() != vw) {
            hudCanvas.setWidth(vw);
        }
        if (infoCanvas != null && infoCanvas.getWidth() != vw) {
            infoCanvas.setWidth(vw);
        }
        if (messageLabel != null) {
            messageLabel.setMaxWidth(vw - 40);
        }
    }

    /** Puts newly fired projectiles on screen immediately (same frame as Space). */
    private void syncSessionProjectilesToLoop() {
        if (gameLoop == null) {
            return;
        }
        GameSession session = controller.getSession();
        java.util.List<com.kurawler.model.object.Projectile> pending =
                new java.util.ArrayList<>(session.getProjectiles());
        for (com.kurawler.model.object.Projectile p : pending) {
            gameLoop.addProjectile(p);
        }
        session.getProjectiles().clear();
    }

    /** Render the entire game state by reading the model. */
    private void render() {
        syncSessionProjectilesToLoop();
        GameSession session = controller.getSession();
        com.kurawler.model.Map map = session.getMap();
        Hero hero = session.getHero();
        if (session.consumeMapChanged() && gameLoop != null) {
            gameLoop.refreshMap();
        }
        ensureCanvasFitsMap(map);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int mapW = map.getWidth();
        int mapH = map.getHeight();

        gc.setFill(Color.web("#0f0f23"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        gc.scale(mapScaleX, mapScaleY);

        for (int r = 0; r < mapH; r++) {
            for (int c = 0; c < mapW; c++) {
                int sx = c * TILE_SIZE;
                int sy = r * TILE_SIZE;
                GridCell cell = map.getCell(r, c);

                StaticObject so = cell.getStaticObject();
                boolean isPortal = so instanceof MapPortal;
                if (isPortal) {
                    MapPortal portal = (MapPortal) so;
                    if (!portal.isPassable()) {
                        renderer.drawWall(gc, sx, sy);
                    } else {
                        renderer.drawFloor(gc, sx, sy);
                    }
                } else if (so != null && so.isSearchable()) {
                    // Searchable props (pool, column, bookshelf) — floor underneath, not wall tiles.
                    renderer.drawFloor(gc, sx, sy);
                } else if (so != null && !so.isPassable()) {
                    if (renderer.isWallName(so.getName())) {
                        renderer.drawWallByName(gc, sx, sy, so.getName());
                    } else if (renderer.spriteForAsset(so.getName()) != null) {
                        renderer.drawFloor(gc, sx, sy);
                    } else {
                        renderer.drawWall(gc, sx, sy);
                    }
                } else if (so != null && renderer.isFloorName(so.getName())) {
                    renderer.drawFloorByName(gc, sx, sy, so.getName());
                } else {
                    renderer.drawFloor(gc, sx, sy);
                }

                if (so != null && so.isSearchable() && !isPortal) {
                    renderer.drawSearchable(gc, sx, sy, so.getName());
                } else if (so != null && !isPortal
                        && renderer.spriteForAsset(so.getName()) != null
                        && !renderer.isWallName(so.getName())
                        && !renderer.isFloorName(so.getName())) {
                    renderer.drawStaticDecor(gc, sx, sy, so);
                }

                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) {
                        StaticContainer sc = (StaticContainer) obj;
                        renderer.drawContainerByName(gc, sx, sy, sc.getSpriteName());
                    } else if (obj instanceof Weapon) {
                        renderer.drawWeaponByName(gc, sx, sy, obj.getName());
                    } else if (obj instanceof Scroll) {
                        renderer.drawItem(gc, sx, sy, "Scroll");
                    } else if (obj instanceof Wearable) {
                        renderer.drawItemObject(gc, sx, sy, (Item) obj);
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

                if (isPortal) {
                    MapPortal portal = (MapPortal) so;
                    renderer.drawPortal(gc, sx, sy, portal.isUnlocked(), portal.getName());
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

        int currentHp = hero.getHealth();
        if (lastHeroHp != Integer.MAX_VALUE && currentHp < lastHeroHp) {
            spawnHitParticles(heroSx, heroSy);
        }
        lastHeroHp = currentHp;
        updateAndDrawHitParticles(gc);

        if (session instanceof TeamMatchSession) {
            drawTeamIndicator(gc, heroSx, heroSy, Team.TeamColor.BLUE);
        }

        if (gameLoop != null) {
            for (com.kurawler.model.object.Projectile proj : gameLoop.getActiveProjectiles()) {
                double[] pPos = lerpPosition(proj,
                        proj.getPositionCol() * TILE_SIZE, proj.getPositionRow() * TILE_SIZE);
            String projName = proj.getName();
            if ("Hero Bolt".equals(projName) || "Weapon Strike".equals(projName)) {
                renderer.drawHeroProjectile(gc, (int) pPos[0], (int) pPos[1]);
            } else if ("Staff Bolt".equals(projName)) {
                renderer.drawStaffBoltProjectile(gc, (int) pPos[0], (int) pPos[1]);
            } else if ("Arrow".equals(projName)) {
                renderer.drawArrowProjectile(gc, (int) pPos[0], (int) pPos[1],
                        proj.getDirectionRow(), proj.getDirectionCol());
            } else if ("Bomb".equals(projName)) {
                renderer.drawBombProjectile(gc, (int) pPos[0], (int) pPos[1]);
            } else {
                renderer.drawProjectile(gc, (int) pPos[0], (int) pPos[1]);
            }
            }
        }

        gc.restore();

        drawHud(session);
        drawInfoBar();
    }

    private void drawInfoBar() {
        if (infoCanvas == null) {
            return;
        }
        GraphicsContext gc = infoCanvas.getGraphicsContext2D();
        double w = infoCanvas.getWidth();
        double h = INFO_BAR_HEIGHT;

        gc.setFill(Color.web("#0e0c0a"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#c8a840"));
        gc.setLineWidth(2);
        gc.strokeLine(0, 0, w, 0);
        gc.setStroke(Color.web("#8b7530", 0.6));
        gc.setLineWidth(1);
        gc.strokeLine(0, 2, w, 2);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#8b7940"));
        gc.fillText("\u25B8", 10, h / 2 + 4);
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
        gc.fillText("\u2694 STR " + hero.getStrength(), statsX, barsTopY + 14);
        gc.setFill(hero.getEquippedWeapon() != null ? Color.web("#ffd54f") : Color.web("#e8c84a"));
        gc.fillText("\u2694 ATK " + hero.calculateDamage(), statsX, barsTopY + 28);
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("\u26E8 DEF " + hero.getDefensePower(), statsX, barsTopY + 42);

        String wpn = formatEquippedWeaponLine(hero);
        gc.setFill(Color.web("#aa9870"));
        gc.fillText("Wpn: " + wpn, statsX, barsTopY + 56);

        double statsX2 = statsX + 120;
        gc.setFill(Color.web("#c8b880"));
        gc.fillText("\u23F1 " + formatTime(session.getPlayDurationTimer()), statsX2, barsTopY + 14);
        gc.fillText("Turn " + session.getTurnCount(), statsX2, barsTopY + 14 + 20);
        gc.setFill(Color.web("#aa9870"));
        gc.fillText("Foes " + session.getActiveEnemyCount(), statsX2, barsTopY + 14 + 40);

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

        double menuBtnY = pad + 4 + btnSize + 4;
        double menuBtnW = btnSize + 8;
        drawGoldPanel(gc, btnX, menuBtnY, menuBtnW, btnSize);
        drawMenuButtonIcon(gc, btnX, menuBtnY, menuBtnW, btnSize);
    }

    /** Three-line menu glyph + MENU label for the HUD menu control. */
    private void drawMenuButtonIcon(GraphicsContext gc, double x, double y, double w, double h) {
        double cx = x + w / 2;
        double lineW = w * 0.52;
        double top = y + h * 0.22;
        gc.setStroke(Color.web("#e8c84a"));
        gc.setLineWidth(2.2);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (int i = 0; i < 3; i++) {
            double ly = top + i * (h * 0.17);
            gc.strokeLine(cx - lineW / 2, ly, cx + lineW / 2, ly);
        }
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        gc.setFill(Color.web("#c8b880"));
        gc.fillText("MENU", cx - 16, y + h - 6);
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

    private static String formatEquippedWeaponLine(Hero hero) {
        if (hero.getEquippedWeapon() == null) {
            return "None";
        }
        String line = hero.getEquippedWeapon().getName();
        if (hero.getEquippedArrow() != null) {
            line += " + Arrow (" + hero.getArrowAmmo() + "/" + hero.getArrowAmmoMax() + ")";
        }
        return line;
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

    private static final double INV_SLOT_W = 62;
    private static final double INV_SLOT_H = 60;
    private static final double[] INV_SLOT_X = { 48, 128, 208, 288, 48, 128, 208, 288 };
    private static final double[] INV_SLOT_Y = { 223, 223, 223, 223, 302, 302, 302, 302 };

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
            bagView.setSmooth(false);
            root.getChildren().add(bagView);
        } else {
            Canvas bg = new Canvas(popW, popH);
            drawDungeonBg(bg, popW, popH);
            root.getChildren().add(bg);
        }

        Pane overlay = new Pane();
        overlay.setPrefSize(popW, popH);
        overlay.setMaxSize(popW, popH);

        final double closeBtnY = 498;
        final double closeBtnH = 32;
        final double detailH = 108;
        final double detailGap = 12;
        final double detailY = closeBtnY - detailGap - detailH;

        Label countLabel = new Label();
        countLabel.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        countLabel.setTextFill(Color.web("#c8b880"));
        countLabel.setAlignment(Pos.CENTER);
        countLabel.setPrefWidth(120);
        countLabel.setLayoutX((popW - 120) / 2.0);
        countLabel.setLayoutY(158);

        VBox detailPane = new VBox(4);
        detailPane.setAlignment(Pos.CENTER);
        detailPane.setPrefSize(304, detailH);
        detailPane.setMinSize(304, detailH);
        detailPane.setMaxSize(304, detailH);
        detailPane.setLayoutX(48);
        detailPane.setLayoutY(detailY);
        detailPane.setStyle("-fx-background-color: rgba(20,14,6,0.82); "
                + "-fx-border-color: #8b7530; -fx-border-width: 1.5; -fx-border-radius: 5; "
                + "-fx-background-radius: 5; -fx-padding: 8 14 8 14;");

        Button closeBtn = new Button("Close Bag");
        closeBtn.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        closeBtn.setPrefWidth(220);
        closeBtn.setPrefHeight(closeBtnH);
        closeBtn.setLayoutX((popW - 220) / 2.0);
        closeBtn.setLayoutY(closeBtnY);
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

        overlay.getChildren().addAll(countLabel, detailPane, closeBtn);

        Item[] selected = new Item[1];
        StackPane[] slotPanes = new StackPane[8];
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            Inventory inv = hero.getInventory();
            countLabel.setText(inv.getItemCount() + " / 8");

            for (int i = 0; i < 8; i++) {
                if (slotPanes[i] != null) {
                    overlay.getChildren().remove(slotPanes[i]);
                }
            }
            int idx = 0;
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 4; c++) {
                    final Item item = inv.getItem(r, c);
                    StackPane slot = buildInventorySlot(item, selected, hero, refresh, popup, session);
                    slot.setLayoutX(INV_SLOT_X[idx]);
                    slot.setLayoutY(INV_SLOT_Y[idx]);
                    slotPanes[idx] = slot;
                    overlay.getChildren().add(0, slot);
                    idx++;
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
                hint.setAlignment(Pos.CENTER);
                hint.setMaxWidth(280);
                hint.setWrapText(true);
                hint.setStyle("-fx-text-alignment: center;");
                detailPane.getChildren().add(hint);
            }
        };
        refresh[0].run();

        root.getChildren().add(overlay);

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

    private static String shortItemLabel(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        return name.length() <= 9 ? name : name.substring(0, 8) + "\u2026";
    }

    private StackPane buildInventorySlot(Item item, Item[] selected, Hero hero,
            Runnable[] refresh, Stage popup, GameSession session) {
        double w = INV_SLOT_W;
        double h = INV_SLOT_H;
        StackPane slot = new StackPane();
        slot.setPrefSize(w, h);
        slot.setMinSize(w, h);
        slot.setMaxSize(w, h);
        slot.setAlignment(Pos.CENTER);

        if (item == null) {
            slot.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            return slot;
        }

        boolean isSelected = (item == selected[0]);
        boolean isEquipped = (item instanceof Weapon
                && (item == hero.getEquippedWeapon() || item == hero.getEquippedArrow()))
                || (item instanceof Wearable && item == hero.getEquippedWearable());

        String selBorder = "-fx-background-color: transparent; -fx-border-color: #e8c84a; -fx-border-width: 2.5; ";
        String normBorder = "-fx-background-color: transparent; -fx-border-color: #6a502888; -fx-border-width: 1; ";
        String radius = "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;";

        slot.setStyle((isSelected ? selBorder : normBorder) + radius);

        javafx.scene.image.Image sprite = renderer.getSpriteForItem(item);
        if (sprite != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(sprite);
            double icon = Math.min(w, h) - 12;
            iv.setFitWidth(icon);
            iv.setFitHeight(icon);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            StackPane iconWrap = new StackPane(iv);
            iconWrap.setPrefSize(w, h);
            iconWrap.setAlignment(Pos.CENTER);
            slot.getChildren().add(iconWrap);
        } else {
            Label nameTag = new Label(shortItemLabel(item.getName()));
            nameTag.setFont(Font.font("Monospaced", FontWeight.BOLD, 8));
            nameTag.setTextFill(Color.web("#e8c84a"));
            nameTag.setWrapText(true);
            nameTag.setTextAlignment(TextAlignment.CENTER);
            nameTag.setMaxWidth(w - 6);
            nameTag.setAlignment(Pos.CENTER);
            slot.getChildren().add(nameTag);
        }

        if (item instanceof Weapon && WeaponCatalog.isArrow(item.getName()) && hero.getArrowAmmo() > 0) {
            Label ammoBadge = new Label(hero.getArrowAmmo() + "/" + hero.getArrowAmmoMax());
            ammoBadge.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
            ammoBadge.setTextFill(Color.web("#f0e0b0"));
            ammoBadge.setStyle("-fx-background-color: rgba(40,25,8,0.92); "
                    + "-fx-padding: 1 3 1 3; -fx-background-radius: 3; "
                    + "-fx-border-color: #c8a840; -fx-border-width: 1; -fx-border-radius: 3;");
            StackPane.setAlignment(ammoBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(ammoBadge, new Insets(2, 2, 0, 0));
            slot.getChildren().add(ammoBadge);
        }

        if (isEquipped) {
            Label badge = new Label(item instanceof Weapon ? "\u2694" : "\u26E8");
            badge.setFont(Font.font(9));
            badge.setTextFill(Color.web("#60ff88"));
            badge.setStyle("-fx-background-color: rgba(0,70,0,0.78); "
                    + "-fx-padding: 0 2 0 2; -fx-background-radius: 2;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(3, 3, 0, 0));
            if (!(item instanceof Weapon && WeaponCatalog.isArrow(item.getName()))) {
                slot.getChildren().add(badge);
            }
        }

        slot.setOnMouseEntered(e -> {
            if (item != selected[0]) {
                slot.setStyle("-fx-background-color: transparent; -fx-border-color: #e8c84a; "
                        + "-fx-border-width: 2; " + radius);
            }
        });
        slot.setOnMouseExited(e -> {
            if (item != selected[0]) {
                slot.setStyle(normBorder + radius);
            }
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
        descLabel.setMaxHeight(32);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);

        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            boolean isArrow = WeaponCatalog.isArrow(weapon.getName());
            boolean bowEquipped = hero.getEquippedWeapon() != null
                    && WeaponCatalog.isBow(hero.getEquippedWeapon().getName());
            boolean equipped = isArrow
                    ? weapon == hero.getEquippedArrow()
                    : weapon == hero.getEquippedWeapon();
            Button btn = invActionButton(equipped ? "Unequip" : "\u2694 Equip", "#e8c84a");
            if (isArrow && !bowEquipped && !equipped) {
                btn.setDisable(true);
            }
            btn.setOnAction(e -> {
                if (isArrow && !bowEquipped) {
                    showGameMessage("Equip a bow before arrows.");
                    AudioManager.getInstance().play("error");
                    render();
                    return;
                }
                controller.equipWeapon(weapon);
                AudioManager.getInstance().play("interact");
                render();
                refresh[0].run();
            });
            actionRow.getChildren().add(btn);
        }
        if (item instanceof Wearable) {
            Wearable wearable = (Wearable) item;
            boolean worn = hero.isWearableEquipped(wearable);
            Button btn = invActionButton(worn ? "Remove" : "\u26E8 Wear", "#5dade2");
            btn.setOnAction(e -> {
                if (worn) controller.unequipWearable(wearable);
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
                for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                    eff.apply(session);
                }
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
            com.kurawler.model.object.PortableContainer bag =
                    (com.kurawler.model.object.PortableContainer) item;
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
            for (com.kurawler.effect.Effect eff : action.resolve(session)) {
                eff.apply(session);
            }
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
            boolean isArrow = WeaponCatalog.isArrow(weapon.getName());
            boolean bowEquipped = hero.getEquippedWeapon() != null
                    && WeaponCatalog.isBow(hero.getEquippedWeapon().getName());
            boolean equipped = isArrow
                    ? weapon == hero.getEquippedArrow()
                    : weapon == hero.getEquippedWeapon();
            String btnLabel = isArrow
                    ? (equipped ? "Unequip arrows" : "Equip arrows (with bow)")
                    : (equipped ? "Unequip" : "Equip " + weapon.getName());
            Button btn = popupButton(btnLabel, "#e8c84a");
            if (isArrow && !bowEquipped && !equipped) {
                btn.setDisable(true);
            }
            btn.setOnAction(e -> {
                if (isArrow && !bowEquipped) {
                    showGameMessage("Equip a bow before arrows.");
                    AudioManager.getInstance().play("error");
                    popup.close();
                    render();
                    return;
                }
                com.kurawler.action.EquipAction equipAction = new com.kurawler.action.EquipAction(weapon);
                for (com.kurawler.effect.Effect eff : equipAction.resolve(session)) {
                    eff.apply(session);
                    if (eff instanceof com.kurawler.effect.EquipEffect) {
                        String msg = ((com.kurawler.effect.EquipEffect) eff).getLastMessage();
                        if (msg != null && !msg.isEmpty()) {
                            showGameMessage(msg);
                            AudioManager.getInstance().play("error");
                            popup.close();
                            render();
                            return;
                        }
                    }
                }
                if (equipped) {
                    showGameMessage("Unequipped " + weapon.getName());
                } else if (WeaponCatalog.isBow(weapon.getName()) && hero.getEquippedArrow() != null) {
                    showGameMessage("Equipped bow and arrows.");
                } else {
                    showGameMessage("Equipped " + weapon.getName());
                }
                AudioManager.getInstance().play("interact");
                popup.close();
                render();
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
        if (animationTimer != null) {
            animationTimer.stop();
        }

        Hero hero = session.getHero();
        String containerName = getContainerDisplayName(containerObj);
        List<Item> contents = new ArrayList<>(getContainerContentsMutable(containerObj));

        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        int popW = 380;
        int popH = Math.max(260, 140 + contents.size() * 56);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        Canvas bg = new Canvas(popW, popH);
        GraphicsContext bgGc = bg.getGraphicsContext2D();
        bgGc.setFill(Color.web("#0e0c0a", 0.95));
        bgGc.fillRoundRect(0, 0, popW, popH, 8, 8);
        bgGc.setStroke(Color.web("#c8a840"));
        bgGc.setLineWidth(2.5);
        bgGc.strokeRoundRect(2, 2, popW - 4, popH - 4, 8, 8);
        bgGc.setStroke(Color.web("#8b7530", 0.5));
        bgGc.setLineWidth(1);
        bgGc.strokeRoundRect(6, 6, popW - 12, popH - 12, 6, 6);
        root.getChildren().add(bg);

        Label title = new Label(containerName.toUpperCase());
        title.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(4, 0, 1, Color.BLACK));

        Label fullMsg = new Label("Inventory full — cannot take more items.");
        fullMsg.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        fullMsg.setTextFill(Color.web("#ff6b6b"));
        fullMsg.setVisible(hero.getInventory().isFull());

        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);

        if (contents.isEmpty()) {
            Label emptyLbl = new Label("It is empty.");
            emptyLbl.setTextFill(Color.web("#888888"));
            itemBox.getChildren().add(emptyLbl);
        } else {
            for (Item item : contents) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER);

                StackPane iconPane = createItemIconPane(item);
                Label nameLbl = new Label(item.getName());
                nameLbl.setPrefWidth(150);
                nameLbl.setTextFill(Color.web("#c8b880"));

                Button takeBtn = popupButton("Take", "#55efc4");
                takeBtn.setPrefWidth(80);
                takeBtn.setPrefHeight(32);

                if (hero.getInventory().isFull()) {
                    takeBtn.setDisable(true);
                } else {
                    takeBtn.setOnAction(e -> {
                        com.kurawler.action.Action action =
                                new com.kurawler.action.TakeFromContainerAction(containerObj, item);
                        List<com.kurawler.effect.Effect> effects = action.resolve(session);
                        if (effects.isEmpty()) {
                            fullMsg.setVisible(true);
                            AudioManager.getInstance().play("error");
                            return;
                        }
                        for (com.kurawler.effect.Effect eff : effects) {
                            eff.apply(session);
                        }
                        AudioManager.getInstance().play("item");
                        showGameMessage("Took " + item.getName());

                        if (getContainerContentsMutable(containerObj).isEmpty()) {
                            popup.close();
                            return;
                        }
                        popup.close();
                        Platform.runLater(() -> showContainerContentsPopup(containerObj, session));
                    });
                }

                row.getChildren().addAll(iconPane, nameLbl, takeBtn);
                itemBox.getChildren().add(row);
            }
        }

        Button closeBtn = popupButton("Close", "#a09070");
        closeBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(12, title, fullMsg, itemBox, closeBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(22));
        root.getChildren().add(content);

        popup.setScene(new Scene(root, popW, popH));
        popup.setOnShown(e -> centerPopup(popup));
        popup.setOnHidden(e -> {
            if (animationTimer != null) {
                animationTimer.start();
            }
            Platform.runLater(() -> canvas.requestFocus());
            render();
        });
        popup.showAndWait();
    }

    private String getContainerDisplayName(Object containerObj) {
        if (containerObj instanceof StaticContainer) {
            return ((StaticContainer) containerObj).getName();
        }
        return ((com.kurawler.model.object.PortableContainer) containerObj).getName();
    }

    private List<Item> getContainerContentsMutable(Object containerObj) {
        if (containerObj instanceof StaticContainer) {
            return ((StaticContainer) containerObj).getContentsMutable();
        }
        return ((com.kurawler.model.object.PortableContainer) containerObj).getContentsMutable();
    }

    private StackPane createItemIconPane(Item item) {
        int iconSize = TILE_SIZE;
        Canvas iconCanvas = new Canvas(iconSize, iconSize);
        renderer.drawItemObject(iconCanvas.getGraphicsContext2D(), 0, 0, item);
        StackPane pane = new StackPane(iconCanvas);
        pane.setPrefSize(iconSize, iconSize);
        pane.setStyle("-fx-background-color: #1a1510; -fx-border-color: #8b7530; "
                + "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        return pane;
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
