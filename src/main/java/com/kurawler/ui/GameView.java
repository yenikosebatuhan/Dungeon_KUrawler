package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.AudioManager;
import com.kurawler.engine.GameLoop;
import com.kurawler.engine.TeamMatchGameLoop;
import com.kurawler.model.GameSession;
import com.kurawler.model.MapFactory;
import com.kurawler.model.GridCell;
import com.kurawler.model.WeaponCatalog;
import com.kurawler.model.Inventory;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.achievement.Achievement;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Assassin;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Guard;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.ShadowCloneScroll;
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
import javafx.geometry.VPos;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * JavaFX game view — renders the grid and handles key/mouse input.
 * Contains NO game logic. Delegates all actions to GameController.
 */
public class GameView {

    private static final int TILE_SIZE = 48;
    /** Taller HUD so the stats panel fits timer + ATK + DEF + WPN + weapon name (ref. image 2). */
    private static final int HUD_HEIGHT = 104;
    private static final int INFO_BAR_HEIGHT = 40;
    private static final double LERP_SPEED = 0.20;
    /** Fraction of flexible width given to the resource bars panel (rest = stats). */
    private static final double HUD_BARS_WIDTH_RATIO = 0.60;
    /** Minimum interval between steps while a movement key is held (ms). */
    private static final long HELD_MOVE_INTERVAL_MS = 190;

    private final GameController controller;
    private final ScreenManager screenManager;
    private final TileRenderer renderer;

    private Stage stage;
    private Canvas canvas;
    private Canvas hudCanvas;
    private Canvas infoCanvas;
    private Label messageLabel;
    private Label achievementBarLabel;
    private double mapScaleX = 1.0;
    private double mapScaleY = 1.0;
    private PauseTransition messageDismissTimer;
    private PauseTransition achievementBannerTimer;
    private String gameMessageLine = "";
    private String achievementBannerLine = "";
    private long achievementBannerUntilMs = 0;
    private GameLoop gameLoop;
    private TeamMatchGameLoop teamMatchGameLoop;
    private AnimationTimer animationTimer;
    private long animationLastNanoTime = System.nanoTime();
    private boolean paused;
    private Scene playScene;
    private boolean playActive;
    private boolean endScreenShown;

    private final java.util.Map<Object, double[]> smoothPositions = new java.util.HashMap<>();
    private long animTick = 0;
    private final List<DustParticle> hitParticles = new ArrayList<>();
    private int lastHeroHp = Integer.MAX_VALUE;
    private final Set<KeyCode> heldMoveKeys = EnumSet.noneOf(KeyCode.class);
    private KeyCode lastPressedMoveKey;
    private long lastMoveStepMs;

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

        achievementBarLabel = new Label("");
        achievementBarLabel.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        achievementBarLabel.setTextFill(Color.web("#ffd54f"));
        achievementBarLabel.setMouseTransparent(true);
        achievementBarLabel.setPadding(new Insets(0, 0, 0, 8));
        achievementBarLabel.setVisible(false);
        achievementBarLabel.setManaged(false);

        messageLabel = new Label("");
        messageLabel.setFont(Font.font("Monospaced", 12));
        messageLabel.setTextFill(Color.web("#d4c8a8"));
        messageLabel.setMouseTransparent(true);
        messageLabel.setPadding(new Insets(0, 14, 0, 4));
        messageLabel.setMaxWidth(ScreenManager.WINDOW_W - 160);
        messageLabel.setWrapText(false);

        infoCanvas = new Canvas(ScreenManager.WINDOW_W, INFO_BAR_HEIGHT);
        infoCanvas.setFocusTraversable(false);
        infoCanvas.setMouseTransparent(true);

        StackPane infoStack = new StackPane(infoCanvas);
        HBox infoRow = new HBox(8, achievementBarLabel, messageLabel);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setMouseTransparent(true);
        infoRow.setPadding(new Insets(0, 8, 0, 20));
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        infoStack.getChildren().add(infoRow);
        infoStack.setFocusTraversable(false);
        infoStack.setPrefHeight(INFO_BAR_HEIGHT);
        infoStack.setMinHeight(INFO_BAR_HEIGHT);
        infoStack.setMaxHeight(INFO_BAR_HEIGHT);
        infoStack.setPrefWidth(ScreenManager.WINDOW_W);
        StackPane.setAlignment(infoRow, Pos.CENTER_LEFT);

        StackPane hudStack = new StackPane(hudCanvas);
        hudStack.setFocusTraversable(false);
        hudStack.setPrefHeight(HUD_HEIGHT);
        hudStack.setPrefWidth(ScreenManager.WINDOW_W);

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
                && "gatehouse".equals(session.getCurrentMapId())) {
            String relicName = session.getTargetObjective().getName();
            showGameMessage("Find the " + relicName
                    + " \u2014 hidden in the Inner Vault chest. Get the Iron Key (\u2192 chest) to unlock the east gate.");
        }
    }

    /** Shows feedback on the bottom info bar (combat, pickup, equip, errors). */
    private void showGameMessage(String text) {
        gameMessageLine = text != null ? text : "";
        refreshBottomInfoBar(controller.getSession());
    }

    /** Bottom bar: game messages; trophy text only while an achievement unlock banner is active. */
    private void refreshBottomInfoBar(GameSession session) {
        if (achievementBarLabel == null || messageLabel == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean showAchievementBanner = achievementBannerUntilMs > now
                && achievementBannerLine != null && !achievementBannerLine.isEmpty();

        achievementBarLabel.setVisible(showAchievementBanner);
        achievementBarLabel.setManaged(showAchievementBanner);
        if (showAchievementBanner) {
            achievementBarLabel.setText(achievementBannerLine);
            messageLabel.setText(gameMessageLine != null ? gameMessageLine.trim() : "");
            return;
        }

        achievementBarLabel.setText("");
        messageLabel.setText(gameMessageLine != null ? gameMessageLine : "");
    }

    private void handleHudClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        HudLayout layout = computeHudLayout(hudCanvas.getWidth(), HUD_HEIGHT);

        if (x >= layout.portraitX && x < layout.portraitX + layout.portraitW
                && y >= layout.panelY && y < layout.panelY + layout.panelH) {
            AudioManager.getInstance().play("button");
            showCharacterSheetPopup(controller.getSession());
            return;
        }
        if (x >= layout.achX && x < layout.achX + layout.iconW
                && y >= layout.panelY && y < layout.panelY + layout.panelH) {
            showAchievementsPopup();
        } else if (x >= layout.bagX && x < layout.bagX + layout.iconW
                && y >= layout.panelY && y < layout.panelY + layout.panelH) {
            showInventoryPopup();
        } else if (x >= layout.pauseX && x < layout.pauseX + layout.iconW
                && y >= layout.panelY && y < layout.panelY + layout.panelH) {
            togglePause();
        } else if (x >= layout.menuX && x < layout.menuX + layout.iconW
                && y >= layout.panelY && y < layout.panelY + layout.panelH) {
            AudioManager.getInstance().play("button");
            showInGameMenu();
        } else {
            GameSession session = controller.getSession();
            if (session.isEditorTestPlay()) {
                double edBtnX = layout.menuX + layout.iconW + layout.gap;
                if (x >= edBtnX && x < edBtnX + 76
                        && y >= layout.panelY && y < layout.panelY + layout.panelH) {
                    AudioManager.getInstance().play("button");
                    shutdown();
                    screenManager.returnToEditorAfterTest(session.getMap(), "Returned to editor.");
                }
            }
        }
    }

    /** Panel positions sized to fill the entire HUD canvas (reference layout). */
    private static final class HudLayout {
        final double panelY, panelH, gap;
        final double portraitX, portraitW;
        final double barsX, barsW;
        final double statsX, statsW;
        final double pauseX, achX, bagX, menuX, iconW;

        HudLayout(double panelY, double panelH, double gap,
                  double portraitX, double portraitW,
                  double barsX, double barsW,
                  double statsX, double statsW,
                  double pauseX, double achX, double bagX, double menuX, double iconW) {
            this.panelY = panelY;
            this.panelH = panelH;
            this.gap = gap;
            this.portraitX = portraitX;
            this.portraitW = portraitW;
            this.barsX = barsX;
            this.barsW = barsW;
            this.statsX = statsX;
            this.statsW = statsW;
            this.pauseX = pauseX;
            this.achX = achX;
            this.bagX = bagX;
            this.menuX = menuX;
            this.iconW = iconW;
        }
    }

    private static HudLayout computeHudLayout(double hudWidth, double hudHeight) {
        double gap = 4;
        double margin = 2;
        double panelY = margin;
        double panelH = hudHeight - margin * 2;
        double iconW = panelH;
        double portraitW = panelH;

        double x = margin;
        x += portraitW + gap;

        double iconsTotal = iconW * 4 + gap * 3;
        double flex = hudWidth - margin * 2 - portraitW - gap - iconsTotal - gap;
        if (flex < 120) {
            flex = 120;
        }
        double barsW = flex * HUD_BARS_WIDTH_RATIO;
        double statsW = flex - barsW;

        double barsX = x;
        x += barsW + gap;
        double statsX = x;
        x += statsW + gap;
        double pauseX = x;
        double achX = pauseX + iconW + gap;
        double bagX = achX + iconW + gap;
        double menuX = bagX + iconW + gap;

        return new HudLayout(panelY, panelH, gap,
                margin, portraitW, barsX, barsW, statsX, statsW,
                pauseX, achX, bagX, menuX, iconW);
    }

    /** Resets frame timing after a modal popup so deltaTime does not jump by seconds. */
    private void resumeAnimationTimer() {
        animationLastNanoTime = System.nanoTime();
        if (animationTimer != null) {
            animationTimer.start();
        }
    }

    private void startGameLoop(GameSession session) {
        animationLastNanoTime = System.nanoTime();

        if (session instanceof TeamMatchSession) {
            TeamMatchSession tmSession = (TeamMatchSession) session;
            teamMatchGameLoop = new TeamMatchGameLoop(tmSession);
            teamMatchGameLoop.start();

            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long deltaMs = (now - animationLastNanoTime) / 1_000_000L;
                    if (deltaMs > 250L) {
                        deltaMs = 16L;
                    }
                    animationLastNanoTime = now;
                    if (!paused) {
                        teamMatchGameLoop.update(deltaMs);
                        processHeldMovement();
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
                    long deltaMs = (now - animationLastNanoTime) / 1_000_000L;
                    if (deltaMs > 250L) {
                        deltaMs = 16L;
                    }
                    animationLastNanoTime = now;
                    if (gameLoop != null) {
                        gameLoop.updateTimedMapObjects();
                    }
                    if (!paused) {
                        gameLoop.update(deltaMs);
                        processHeldMovement();
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
                screenManager.returnToEditorAfterTest(session, msg);
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
        Platform.runLater(() -> {
            GameSession session = controller.getSession();
            boolean heroWon = result != null && result.contains("Team B");
            String title = heroWon ? "\u2694 VICTORY \u2694" : "\u2620 DEFEAT \u2620";
            String accent = heroWon ? "#6fc3df" : "#ef5350";
            String bgTint = heroWon ? "#1a2a3a" : "#3a1a1a";
            String timeStr = session != null ? formatTime(session.getPlayDurationTimer()) : "00:00";
            int turns = session != null ? session.getTurnCount() : 0;
            String message = (result != null ? result : "")
                    + "\nTime: " + timeStr + "  |  Turns: " + turns;
            showTeamMatchEndScreen(title, message, accent, bgTint);
        });
    }

    private void showTeamMatchEndScreen(String title, String message, String accent, String bgTint) {
        if (!isPlaySceneActive()) return;
        Stage popup = new Stage();
        popup.initOwner(stage);
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);

        Canvas bg = new Canvas(420, 300);
        GraphicsContext gc2 = bg.getGraphicsContext2D();
        SpriteSheet wallSheet = new SpriteSheet("/assets/walls_x3.png", 48);
        gc2.setFill(Color.web("#0a0a10"));
        gc2.fillRect(0, 0, 420, 300);
        if (wallSheet.isLoaded()) {
            for (int y = 0; y < 300; y += 48) {
                for (int x = 0; x < 420; x += 48) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    javafx.scene.image.Image tile = wallSheet.getSprite(hash % 5, 1);
                    if (tile != null) gc2.drawImage(tile, x, y, 48, 48);
                }
            }
        }
        gc2.setFill(Color.web(bgTint, 0.7));
        gc2.fillRect(0, 0, 420, 300);

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

        Button playAgainBtn = new Button("\u25b6  Play Again");
        playAgainBtn.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        playAgainBtn.setPrefWidth(200);
        playAgainBtn.setPrefHeight(40);
        playAgainBtn.setStyle(base);
        playAgainBtn.setOnMouseEntered(e -> playAgainBtn.setStyle(hover));
        playAgainBtn.setOnMouseExited(e -> playAgainBtn.setStyle(base));
        playAgainBtn.setOnAction(e -> {
            popup.close();
            playActive = false;
            shutdown();
            screenManager.startTeamMatch();
        });

        Button menuBtn = new Button("Return to Menu");
        menuBtn.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        menuBtn.setPrefWidth(200);
        menuBtn.setPrefHeight(40);
        menuBtn.setStyle(base);
        menuBtn.setOnMouseEntered(e -> menuBtn.setStyle(hover));
        menuBtn.setOnMouseExited(e -> menuBtn.setStyle(base));
        menuBtn.setOnAction(e -> {
            popup.close();
            playActive = false;
            shutdown();
            screenManager.showMainMenu();
        });

        VBox content = new VBox(16, titleLabel, msgLabel, playAgainBtn, menuBtn);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-border-color: " + accent + "66; -fx-border-width: 3; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");
        content.setOpacity(0);

        StackPane root = new StackPane(bg, content);
        Scene popupScene = new Scene(root, 420, 300);
        popupScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER
                    || e.getCode() == KeyCode.ESCAPE) {
                e.consume();
            }
        });
        popup.setScene(popupScene);
        popup.setTitle(title);
        popup.setOnShown(e -> {
            popup.setX(stage.getX() + (stage.getWidth() - 420) / 2);
            popup.setY(stage.getY() + (stage.getHeight() - 300) / 2);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(400), content);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        popup.showAndWait();
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
        popup.setOnHidden(e -> resumeAnimationTimer());
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
        messageLabel.setFocusTraversable(false);
        if (achievementBarLabel != null) {
            achievementBarLabel.setFocusTraversable(false);
        }
        playCanvas.setFocusTraversable(true);

        playCanvas.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        playCanvas.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyRelease);

        Runnable focusGame = () -> {
            if (stage != null) {
                stage.requestFocus();
            }
            playCanvas.requestFocus();
        };
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> focusGame.run());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!playCanvas.isFocused()) {
                focusGame.run();
            }
        });
    }

    private void handleKeyRelease(KeyEvent event) {
        if (isMovementKey(event.getCode())) {
            heldMoveKeys.remove(event.getCode());
            if (event.getCode() == lastPressedMoveKey) {
                lastPressedMoveKey = heldMoveKeys.stream().findFirst().orElse(null);
            }
        }
    }

    private static boolean isMovementKey(KeyCode code) {
        return code == KeyCode.W || code == KeyCode.UP
                || code == KeyCode.S || code == KeyCode.DOWN
                || code == KeyCode.A || code == KeyCode.LEFT
                || code == KeyCode.D || code == KeyCode.RIGHT;
    }

    private static int[] movementDeltaForKey(KeyCode code) {
        if (code == KeyCode.W || code == KeyCode.UP) {
            return new int[]{-1, 0};
        }
        if (code == KeyCode.S || code == KeyCode.DOWN) {
            return new int[]{1, 0};
        }
        if (code == KeyCode.A || code == KeyCode.LEFT) {
            return new int[]{0, -1};
        }
        if (code == KeyCode.D || code == KeyCode.RIGHT) {
            return new int[]{0, 1};
        }
        return new int[]{0, 0};
    }

    private void clearHeldMovementKeys() {
        heldMoveKeys.clear();
        lastPressedMoveKey = null;
    }

    /** Repeat movement while keys stay down (macOS often drops single KEY_PRESSED events). */
    private void processHeldMovement() {
        if (paused || lastPressedMoveKey == null || !heldMoveKeys.contains(lastPressedMoveKey)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastMoveStepMs < HELD_MOVE_INTERVAL_MS) {
            return;
        }
        int[] delta = movementDeltaForKey(lastPressedMoveKey);
        if (delta[0] != 0 || delta[1] != 0) {
            tryHeroMove(delta[0], delta[1]);
            lastMoveStepMs = now;
        }
    }

    private boolean tryHeroMove(int dRow, int dCol) {
        Hero hero = controller.getSession().getHero();
        if (hero == null) {
            return false;
        }
        hero.setFacingFromDelta(dRow, dCol);
        if (hero.getEnergy() < 1) {
            showGameMessage("Not enough energy, you CAN NOT MOVE!");
            return false;
        }
        int beforeRow = hero.getRow();
        int beforeCol = hero.getCol();
        controller.moveHero(dRow, dCol);
        showPortalMessageIfAny();
        if (hero.getRow() == beforeRow && hero.getCol() == beforeCol) {
            showGameMessage("Blocked");
            return false;
        }
        return true;
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

        boolean isMoveKey = isMovementKey(event.getCode());
        int dRow = 0;
        int dCol = 0;

        switch (event.getCode()) {
            case W: case UP:    dRow = -1; break;
            case S: case DOWN:  dRow = 1; break;
            case A: case LEFT:  dCol = -1; break;
            case D: case RIGHT: dCol = 1; break;
            case E:
                if (tryUseAdjacentPortal()) {
                    render();
                    break;
                }
                if (tryOpenAdjacentBookshelf()) {
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
            boolean firstPress = !heldMoveKeys.contains(event.getCode());
            heldMoveKeys.add(event.getCode());
            lastPressedMoveKey = event.getCode();
            if (firstPress) {
                lastMoveStepMs = System.currentTimeMillis();
                tryHeroMove(dRow, dCol);
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
            GameSession session = controller.getSession();
            // Scan facing cells directly so we find the enemy even if it just died
            // (findFirstEnemyInDirection skips dead enemies, causing cleanup to be skipped).
            int[] dir = hero.attackDirection();
            int reach = hero.meleeFacingReach();
            com.kurawler.model.Map map = session.getMap();
            Enemy hitEnemy = null;
            for (int step = 1; step <= reach; step++) {
                GridCell c = map.getCell(hero.getRow() + dir[0] * step, hero.getCol() + dir[1] * step);
                if (c == null) break;
                if (c.getStaticObject() != null && !c.getStaticObject().isPassable()) break;
                if (c.getOccupyingCharacter() instanceof Enemy) {
                    hitEnemy = (Enemy) c.getOccupyingCharacter();
                    break;
                }
            }
            if (hitEnemy != null && !hitEnemy.isAlive()) {
                AudioManager.getInstance().play("enemyDown");
                showGameMessage(hitEnemy.getName() + " defeated!");
            } else if (hitEnemy != null) {
                AudioManager.getInstance().play("attack");
                String verb = hero.getEquippedWeapon() == null ? "Punched" : "Hit";
                showGameMessage(verb + " " + hitEnemy.getName() + "! HP:" + hitEnemy.getHealth());
            } else {
                AudioManager.getInstance().play("attack");
                showGameMessage(hero.getEquippedWeapon() == null ? "Punch!" : "Strike!");
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
        if (gameMessageLine == null || gameMessageLine.isEmpty()) {
            String mapId = session.getCurrentMapId();
            if (mapId != null && !mapId.isEmpty()) {
                showGameMessage("Entered " + mapDisplayName(mapId) + ".");
            }
        }
        if (session.getTargetObjective() != null && "gatehouse".equals(session.getCurrentMapId())) {
            String relicName = session.getTargetObjective().getName();
            showGameMessage("Find the " + relicName
                    + " \u2014 hidden in the Inner Vault chest. Get the Iron Key (\u2192 chest) to open the east gate.");
        }
    }

    private static String mapDisplayName(String mapId) {
        if ("gatehouse".equals(mapId))  return "The Gatehouse";
        if ("armory".equals(mapId))     return "The Armory";
        if ("innervault".equals(mapId)) return "The Inner Vault";
        return mapId != null ? mapId : "";
    }

    private void showPortalMessageIfAny() {
        String portalMsg = controller.getSession().consumeLastPortalMessage();
        if (portalMsg != null && !portalMsg.isEmpty()) {
            showGameMessage(portalMsg);
        }
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


    /** Opens an adjacent bookshelf/shelf (container popup, same as chest). */
    private boolean tryOpenAdjacentBookshelf() {
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
            StaticContainer shelf = MapFactory.resolveBookshelfContainer(map, row, col);
            if (shelf != null) {
                AudioManager.getInstance().play("interact");
                showContainerContentsPopup(shelf, session);
                return true;
            }
        }
        return false;
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
                if (!sc.isShelfLike() && sc.isEmpty()) {
                    sc.removeFromMapIfLooted(map);
                    continue;
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
                    // Locked: no key → only the (hard) BREAK option remains.
                    if (sc.isBreakable()) {
                        actionList.add(new String[]{"BREAK", sc.getName(), "break"});
                    }
                } else {
                    // Unlocked: player may simply OPEN it, or choose to BREAK it open.
                    actionList.add(new String[]{"OPEN", sc.getName(), "open_container"});
                    if (sc.isBreakable()) {
                        actionList.add(new String[]{"BREAK", sc.getName(), "break"});
                    }
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
            if (MapFactory.isShelfStaticObject(staticObj)) {
                actionList.add(new String[]{"OPEN", staticObj.getName(), "open_bookshelf"});
            }
            actionList.add(new String[]{"SEARCH", staticObj.getName(), "search"});
            if (MapFactory.isBreakableStaticObject(staticObj)) {
                actionList.add(new String[]{"BREAK", staticObj.getName(), "break"});
            }
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
        popup.setOnHidden(e -> resumeAnimationTimer());
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
                if (so instanceof MapPortal && ((MapPortal) so).isStairsMode()) {
                    usePortalAt(session, hero, row, col, (MapPortal) so);
                } else if (so != null) {
                    Item hidden = so.getHiddenItem();
                    boolean searched = controller.searchObject(so);
                    if (hidden != null && searched)
                        showGameMessage("Found: " + hidden.getName() + "!");
                    else if (searched) showGameMessage("Nothing found.");
                    else showGameMessage("Cannot search.");
                    AudioManager.getInstance().play("interact");
                }
                break;
            case "break":
                com.kurawler.action.BreakAction.BreakRoll breakRoll = controller.breakObject(row, col);
                showGameMessage(breakRoll.message());
                AudioManager.getInstance().play(breakRoll.succeeded() ? "break" : "error");
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
            case "open_bookshelf":
                StaticContainer shelf = MapFactory.resolveBookshelfContainer(map, row, col);
                if (shelf != null) {
                    AudioManager.getInstance().play("interact");
                    Platform.runLater(() -> showContainerContentsPopup(shelf, session));
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
        int popupH = 440;
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
        
        // NEW MUSIC BUTTON (Adapted to use popupButton instead of pauseMenuBtn)
        Button musicBtn = popupButton("\u266B  Music: " + (AudioManager.getInstance().isMusicEnabled() ? "ON" : "OFF"), "#f39c12");
        musicBtn.setOnAction(e -> {
            AudioManager audio = AudioManager.getInstance();
            boolean newState = !audio.isMusicEnabled();
            audio.setMusicEnabled(newState);
            musicBtn.setText("\u266B  Music: " + (newState ? "ON" : "OFF"));
            audio.play("button");
            if (newState) {
                audio.playMusic("combat", true); 
            }
        });

        Button restartBtn  = popupButton("Restart Level", "#e17055");
        Button mainMenuBtn = popupButton("Main Menu", "#e8c84a");
        Button resumeBtn   = popupButton("Resume", "#a09070");

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
                screenManager.returnToEditorAfterTest(s, "Returned to level editor.");
            } else {
                screenManager.showMainMenu();
            }
        });
        resumeBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            closePopup.run();
        });
        restartBtn.setOnAction(e -> {
            AudioManager.getInstance().play("button");
            leavePlay[0] = false;
            closePopup.run();
            boolean ok = com.kurawler.model.MapFactory.restartCurrentFloor(controller.getSession());
            if (ok) {
                if (gameLoop != null) gameLoop.refreshMap();
                render();
                showGameMessage("Level restarted.");
            }
        });

        VBox panel = new VBox(12, title, subtitle, saveBtn, loadBtn, musicBtn, restartBtn, mainMenuBtn, resumeBtn);
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

        if (!leavePlay[0]) {
            resumeAnimationTimer();
        }
        Platform.runLater(() -> canvas.requestFocus());
    }

    private void togglePause() {
        AudioManager.getInstance().play("button");
        paused = !paused;
        GameSession session = controller.getSession();
        if (paused) {
            clearHeldMovementKeys();
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
            messageLabel.setMaxWidth(Math.max(200, vw - 160));
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
        animTick++;
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
                final int soRot = so != null ? so.getRotation() : 0;

                renderer.applyRotation(gc, sx, sy, soRot, () -> {
                    if (isPortal) {
                        MapPortal portal = (MapPortal) so;
                        if (!portal.isPassable()) {
                            renderer.drawWall(gc, sx, sy);
                        } else {
                            renderer.drawFloor(gc, sx, sy);
                        }
                    } else if (so != null && so.isSearchable()) {
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

                    if (isPortal) {
                        MapPortal portal = (MapPortal) so;
                        renderer.drawPortalOrNavSprite(gc, sx, sy, portal);
                    }
                });

                for (GameObject obj : cell.getObjects()) {
                    final int objRot = obj.getRotation();
                    renderer.applyRotation(gc, sx, sy, objRot, () -> {
                        if (obj instanceof StaticContainer) {
                            StaticContainer sc = (StaticContainer) obj;
                            if (!sc.shouldDisplayOnMap()) {
                                return;
                            }
                            renderer.drawContainerByName(gc, sx, sy, sc.getSpriteName());
                        } else if (obj instanceof Weapon) {
                            renderer.drawWeaponByName(gc, sx, sy, obj.getName());
                        } else if (obj instanceof Item) {
                            if (obj instanceof ShadowCloneScroll
                                    && ((ShadowCloneScroll) obj).isMapVisibilityExpired()) {
                                return;
                            }
                            renderer.drawItemObject(gc, sx, sy, (Item) obj);
                        }
                    });
                }

                Character occupant = cell.getOccupyingCharacter();
                if (occupant != null && occupant.isAlive() && !(occupant instanceof Hero)) {
                    double targetX = c * TILE_SIZE, targetY = r * TILE_SIZE;
                    double[] pos = lerpPosition(occupant, targetX, targetY);
                    boolean enemyMoving = Math.abs(pos[0] - targetX) > 1.5
                            || Math.abs(pos[1] - targetY) > 1.5;
                    int enemyWalkFrame = enemyMoving ? (int)(animTick / 5) % 2 : 0;
                    String type = (occupant instanceof Knight) ? "Knight"
                            : (occupant instanceof Guard) ? "Guard"
                            : (occupant instanceof Assassin) ? "Assassin" : "Sorcerer";
                    boolean mirrorKnight = (occupant instanceof Knight || occupant instanceof Guard)
                            && occupant.getCol() > hero.getCol();

                    Team occupantTeam = null;
                    if (session instanceof TeamMatchSession) {
                        occupantTeam = ((TeamMatchSession) session).getTeamForCharacter(occupant);
                    }
                    if (occupantTeam != null) {
                        Color tint = occupantTeam.getColor() == Team.TeamColor.RED
                                ? Color.rgb(200, 0, 0, 0.15) : Color.rgb(0, 60, 200, 0.15);
                        gc.setFill(tint);
                        gc.fillRect((int) pos[0], (int) pos[1], TILE_SIZE, TILE_SIZE);
                    }

                    renderer.drawEnemy(gc, (int) pos[0], (int) pos[1], type, mirrorKnight, occupant.getName(), enemyWalkFrame);
                    renderer.drawHpBar(gc, (int) pos[0], (int) pos[1],
                            occupant.getHealth(), occupant.getMaxHealth());

                    if (occupantTeam != null) {
                        String label = (occupant instanceof Knight) ? "KNIGHT"
                                : (occupant instanceof Sorcerer) ? "SORCER"
                                : (occupant instanceof Assassin) ? "ASSASN" : type.toUpperCase();
                        drawTeamNameplate(gc, (int) pos[0], (int) pos[1], label, occupantTeam.getColor());
                        drawTeamIndicator(gc, (int) pos[0], (int) pos[1], occupantTeam.getColor());
                    }
                }
            }
        }

        ShadowClone shadowClone = session.getShadowClone();
        if (shadowClone != null && shadowClone.isAlive() && !shadowClone.isExpired()) {
            double[] clonePos = lerpPosition(shadowClone,
                    shadowClone.getCol() * TILE_SIZE, shadowClone.getRow() * TILE_SIZE);
            renderer.drawShadowClone(gc, (int) clonePos[0], (int) clonePos[1]);
        }

        double heroTargetX = hero.getCol() * TILE_SIZE, heroTargetY = hero.getRow() * TILE_SIZE;
        double[] heroPos = lerpPosition(hero, heroTargetX, heroTargetY);
        int heroSx = (int) heroPos[0];
        int heroSy = (int) heroPos[1];
        boolean heroMoving = Math.abs(heroPos[0] - heroTargetX) > 1.5
                || Math.abs(heroPos[1] - heroTargetY) > 1.5;
        // Walk frame is driven by lerp state, not a clock — character is in walk
        // pose exactly while visually moving, idle the moment it arrives.
        boolean wearingArmor = hero.getEquippedArmor() != null;
        boolean wearingShield = hero.getEquippedShield() != null;
        renderer.drawHeroFacing(gc, heroSx, heroSy, hero.getFacingRow(), hero.getFacingCol(),
                heroMoving ? 1 : 0, wearingArmor);
        if (wearingShield) {
            renderer.drawEquippedShieldOnHero(gc, heroSx, heroSy,
                    hero.getFacingRow(), hero.getFacingCol(),
                    hero.getEquippedShield().getName());
        }
        drawHeroEquippedWeapon(gc, heroSx, heroSy, hero);
        renderer.drawHpBar(gc, heroSx, heroSy, hero.getHealth(), hero.getMaxHealth());

        int currentHp = hero.getHealth();
        if (lastHeroHp != Integer.MAX_VALUE && currentHp < lastHeroHp) {
            spawnHitParticles(heroSx, heroSy);
        }
        lastHeroHp = currentHp;
        updateAndDrawHitParticles(gc);

        if (session instanceof TeamMatchSession) {
            gc.setFill(Color.rgb(0, 60, 200, 0.15));
            gc.fillRect(heroSx, heroSy, TILE_SIZE, TILE_SIZE);
            drawTeamNameplate(gc, heroSx, heroSy, "HERO", Team.TeamColor.BLUE);
            drawTeamIndicator(gc, heroSx, heroSy, Team.TeamColor.BLUE);
        }

        java.util.List<com.kurawler.model.object.Projectile> activeProjs =
                (teamMatchGameLoop != null) ? teamMatchGameLoop.getActiveProjectiles()
                : (gameLoop != null) ? gameLoop.getActiveProjectiles()
                : java.util.Collections.emptyList();
        for (com.kurawler.model.object.Projectile proj : activeProjs) {
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

        gc.restore();

        drawHud(session);
        drawInfoBar();
        refreshBottomInfoBar(session);
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
        HudLayout L = computeHudLayout(w, h);

        if (bagIconImg == null) {
            try {
                javafx.scene.image.Image raw = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/assets/bag - empty.png"));
                bagIconImg = SpriteAtlas.trimOpaqueBounds(raw);
                if (bagIconImg == null) {
                    bagIconImg = raw;
                }
            } catch (Exception ignored) {}
        }

        gc.setFill(Color.web("#0e0c0a"));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.web("#c8a840"));
        gc.setLineWidth(1.5);
        gc.strokeRect(0.5, 0.5, w - 1, h - 1);

        // Portrait
        drawOrnateHudPanel(gc, L.portraitX, L.panelY, L.portraitW, L.panelH);
        renderer.drawHero(gc,
                (int) (L.portraitX + (L.portraitW - TILE_SIZE) / 2),
                (int) (L.panelY + (L.panelH - TILE_SIZE) / 2));

        // HEALTH / ENERGY / MANA — bars fill panel height
        drawOrnateHudPanel(gc, L.barsX, L.panelY, L.barsW, L.panelH);
        double barPad = 10;
        double barW = L.barsW - barPad * 2;
        double barGap = 5;
        double barH = (L.panelH - barPad * 2 - barGap * 2) / 3.0;
        double barX = L.barsX + barPad;
        double barY = L.panelY + barPad;
        drawReferenceResourceBar(gc, barX, barY, barW, barH,
                "HEALTH", hero.getHealth(), hero.getMaxHealth(),
                "#3a0808", "#b71c1c", "#ef5350");
        drawReferenceResourceBar(gc, barX, barY + barH + barGap, barW, barH,
                "ENERGY", hero.getEnergy(), hero.getMaxEnergy(),
                "#0a2a0a", "#2e7d32", "#66bb6a");
        drawReferenceResourceBar(gc, barX, barY + (barH + barGap) * 2, barW, barH,
                "MANA", hero.getMana(), hero.getMaxMana(),
                "#0a1a3a", "#1565c0", "#64b5f6");

        // Stats (timer / ATK / DEF / WPN) — stay in HUD panel
        drawOrnateHudPanel(gc, L.statsX, L.panelY, L.statsW, L.panelH);
        double tx = L.statsX + 12;
        double innerW = L.statsW - 24;
        double lineStep = (L.panelH - 20) / 6.0;
        double ty = L.panelY + 18;
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        if (session instanceof TeamMatchSession) {
            TeamMatchSession tmHud = (TeamMatchSession) session;
            gc.setFill(Color.web("#6fc3df"));
            gc.fillText("TEAM MATCH", tx, ty);
            ty += lineStep;
            gc.setFill(Color.rgb(255, 80, 80));
            gc.fillText("\u25A0 A: " + tmHud.getTeamA().aliveCount() + "/4", tx, ty);
            ty += lineStep;
            gc.setFill(Color.rgb(80, 140, 255));
            gc.fillText("\u25A0 B: " + tmHud.getTeamB().aliveCount() + "/4", tx, ty);
        } else {
            gc.setFill(Color.web("#ffffff", 0.92));
            gc.fillText("\u23F1  " + formatTime(session.getPlayDurationTimer()), tx, ty);
            ty += lineStep;
            gc.setFill(Color.web("#ffffff", 0.95));
            gc.fillText("\u2756  STR " + hero.getStrength(), tx, ty);
            ty += lineStep;
            gc.setFill(hero.getEquippedWeapon() != null ? Color.web("#ffd54f") : Color.web("#e8c84a"));
            gc.fillText("\u2694  ATK " + hero.calculateDamage(), tx, ty);
            ty += lineStep;
            gc.setFill(Color.web("#ffffff", 0.95));
            gc.fillText("\u26E8  DEF " + hero.getDefensePower(), tx, ty);
            ty += lineStep;
            gc.setFill(Color.web("#ffffff", 0.88));
            gc.fillText("\u2694  WPN", tx, ty);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
            gc.setFill(Color.web("#d4c8a8"));
            gc.fillText(fitTextToWidth(gc, formatEquippedWeaponLine(hero), innerW), tx, ty + lineStep * 0.85);
        }

        // Play / pause
        drawOrnateHudPanel(gc, L.pauseX, L.panelY, L.iconW, L.panelH);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 26));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText(paused ? "▶" : "⏸",
                L.pauseX + L.iconW / 2.0 - 10, L.panelY + L.panelH / 2.0 + 9);

        // Trophy + progress count (HUD)
        AchievementManager am = session.getAchievementManager();
        int achUnlocked = 0;
        int achTotal = 0;
        if (am != null) {
            for (Achievement a : am.getAchievements()) {
                achTotal++;
                if (a.isUnlocked()) {
                    achUnlocked++;
                }
            }
        }
        drawOrnateHudPanel(gc, L.achX, L.panelY, L.iconW, L.panelH);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 24));
        gc.setFill(Color.web("#e8c84a"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("\uD83C\uDFC6", L.achX + L.iconW / 2.0, L.panelY + L.panelH / 2.0 - 6);
        gc.setTextBaseline(VPos.BASELINE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#ffffff", 0.95));
        gc.fillText(achUnlocked + "/" + achTotal, L.achX + 8, L.panelY + L.panelH - 10);

        // Bag (transparent PNG — no placeholder background)
        drawOrnateHudPanel(gc, L.bagX, L.panelY, L.iconW, L.panelH);
        if (bagIconImg != null && !bagIconImg.isError()) {
            double bagPad = Math.max(6, L.iconW * 0.12);
            double maxIcon = L.iconW - bagPad * 2;
            double countH = 14;
            double availH = L.panelH - bagPad * 2 - countH;
            double iw = bagIconImg.getWidth();
            double ih = bagIconImg.getHeight();
            double scale = Math.min(maxIcon / iw, availH / ih);
            double dw = iw * scale;
            double dh = ih * scale;
            double dx = L.bagX + (L.iconW - dw) / 2.0;
            double dy = L.panelY + bagPad + (availH - dh) / 2.0;
            gc.drawImage(bagIconImg, dx, dy, dw, dh);
        }
        Inventory inv = hero.getInventory();
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#ffffff", 0.95));
        gc.fillText(inv.getItemCount() + "/8", L.bagX + 8, L.panelY + L.panelH - 10);

        // Menu
        drawOrnateHudPanel(gc, L.menuX, L.panelY, L.iconW, L.panelH);
        drawMenuButtonIcon(gc, L.menuX, L.panelY, L.iconW, L.panelH);

        if (session.isEditorTestPlay()) {
            double edBtnX = L.menuX + L.iconW + L.gap;
            double edW = w - edBtnX - L.gap;
            if (edW > 40) {
                drawOrnateHudPanel(gc, edBtnX, L.panelY, edW, L.panelH);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
                gc.setFill(Color.web("#e8c84a"));
                gc.fillText("Back to Editor", edBtnX + 8, L.panelY + L.panelH / 2.0 + 4);
            }
        }
    }

    /** Reference HUD frame: dark fill + double gold border + corner dots. */
    private void drawOrnateHudPanel(GraphicsContext gc, double x, double y, double pw, double ph) {
        gc.setFill(Color.web("#0a0908"));
        gc.fillRect(x, y, pw, ph);
        gc.setStroke(Color.web("#8b7530"));
        gc.setLineWidth(1);
        gc.strokeRect(x + 0.5, y + 0.5, pw - 1, ph - 1);
        gc.setStroke(Color.web("#c8a840"));
        gc.setLineWidth(1.5);
        gc.strokeRect(x + 2.5, y + 2.5, pw - 5, ph - 5);
        gc.setFill(Color.web("#c8a840"));
        double d = 2;
        gc.fillOval(x + 1, y + 1, d, d);
        gc.fillOval(x + pw - d - 1, y + 1, d, d);
        gc.fillOval(x + 1, y + ph - d - 1, d, d);
        gc.fillOval(x + pw - d - 1, y + ph - d - 1, d, d);
    }

    /**
     * Reference resource bar: white label on the left, colored fill, val/max on the right.
     */
    private void drawReferenceResourceBar(GraphicsContext gc, double x, double y, double w, double h,
                                            String label, int val, int max,
                                            String trackColor, String fillColor, String highlightColor) {
        gc.setFill(Color.web(trackColor));
        gc.fillRoundRect(x, y, w, h, 3, 3);

        double labelW = 58;
        double valueW = 48;
        double fillX = x + labelW;
        double fillMaxW = w - labelW - valueW;
        double ratio = max > 0 ? Math.max(0, Math.min(1.0, (double) val / max)) : 0;
        if (ratio > 0) {
            double fillW = Math.max(4, fillMaxW * ratio);
            gc.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(fillColor)),
                    new Stop(0.55, Color.web(highlightColor)),
                    new Stop(1, Color.web(highlightColor, 0.85))));
            gc.fillRoundRect(fillX, y + 1, fillW, h - 2, 2, 2);
        }

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
        gc.setFill(Color.web("#ffffff", 0.95));
        gc.fillText(label, x + 6, y + h - 5);
        String valueText = val + "/" + max;
        gc.fillText(valueText, x + w - valueW + 4, y + h - 5);
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
        gc.setFill(Color.web("#e74c3c"));
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
        if (achievementBarLabel == null || achievement == null) {
            return;
        }
        achievementBannerLine = "\uD83C\uDFC6 " + achievement.getName() + " unlocked!";
        achievementBannerUntilMs = System.currentTimeMillis() + 3000;
        refreshBottomInfoBar(controller.getSession());
        if (achievementBannerTimer != null) {
            achievementBannerTimer.stop();
        }
        achievementBannerTimer = new PauseTransition(Duration.seconds(3));
        achievementBannerTimer.setOnFinished(e -> {
            achievementBannerUntilMs = 0;
            achievementBannerLine = "";
            refreshBottomInfoBar(controller.getSession());
        });
        achievementBannerTimer.play();
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
        popup.setOnHidden(e -> resumeAnimationTimer());
        popup.show();
        centerPopup(popup);
    }

    private void updateSidebar(GameSession session) {
    }

    /** Draws the hero's equipped melee/ranged weapon in-hand on the map (arrows excluded). */
    private void drawHeroEquippedWeapon(GraphicsContext gc, int screenX, int screenY, Hero hero) {
        if (hero == null) {
            return;
        }
        Weapon equipped = hero.getEquippedWeapon();
        if (equipped == null || WeaponCatalog.isArrow(equipped.getName())) {
            return;
        }
        renderer.drawEquippedWeaponInHand(
                gc, screenX, screenY, hero.getFacingRow(), hero.getFacingCol(), equipped.getName());
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

    /** Truncates with ellipsis so weapon names stay inside the stats panel. */
    private static String fitTextToWidth(GraphicsContext gc, String text, double maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        javafx.scene.text.Text measure = new javafx.scene.text.Text(text);
        measure.setFont(gc.getFont());
        String fitted = text;
        while (measure.getLayoutBounds().getWidth() > maxWidth && fitted.length() > 1) {
            fitted = fitted.substring(0, fitted.length() - 1);
            measure.setText(fitted + "\u2026");
        }
        return measure.getText();
    }

    private static String formatTime(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void drawTeamIndicator(GraphicsContext gc, int sx, int sy, Team.TeamColor teamColor) {
        Color borderColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(255, 60, 60, 0.9)
                : Color.rgb(60, 120, 255, 0.9);
        Color glowColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(255, 60, 60, 0.25)
                : Color.rgb(60, 120, 255, 0.25);
        Color dotColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(255, 80, 80)
                : Color.rgb(80, 140, 255);

        // Outer translucent glow ring
        gc.setStroke(glowColor);
        gc.setLineWidth(5.0);
        gc.strokeRect(sx - 1, sy - 1, TILE_SIZE + 2, TILE_SIZE + 2);

        // Inner solid stroke
        gc.setStroke(borderColor);
        gc.setLineWidth(2.0);
        gc.strokeRect(sx + 1, sy + 1, TILE_SIZE - 2, TILE_SIZE - 2);

        gc.setFill(dotColor);
        gc.fillOval(sx + TILE_SIZE - 12, sy + 2, 9, 9);
        gc.setStroke(Color.web("#000000", 0.6));
        gc.setLineWidth(1);
        gc.strokeOval(sx + TILE_SIZE - 12, sy + 2, 9, 9);
    }

    private void drawTeamNameplate(GraphicsContext gc, int sx, int sy, String label, Team.TeamColor teamColor) {
        Color bgColor = (teamColor == Team.TeamColor.RED)
                ? Color.rgb(140, 20, 20, 0.85)
                : Color.rgb(20, 40, 160, 0.85);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        double textW = label.length() * 6.0;
        double plateW = textW + 6;
        double plateH = 12;
        double plateX = sx + (TILE_SIZE - plateW) / 2.0;
        double plateY = sy - plateH - 1;
        gc.setFill(bgColor);
        gc.fillRoundRect(plateX, plateY, plateW, plateH, 2, 2);
        gc.setFill(Color.WHITE);
        gc.fillText(label, plateX + 3, plateY + plateH - 2);
    }

    private String teamRoster(Team team) {
        StringBuilder sb = new StringBuilder();
        for (Character member : team.getMembers()) {
            String icon = (member instanceof Hero) ? "  \u2694 "
                    : (member instanceof Sorcerer) ? "  \u2728 "
                    : (member instanceof Assassin) ? "  A " : "  \u2620 ";
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
        clearHeldMovementKeys();

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
            clearHeldMovementKeys();
            if (!wasPaused) {
                paused = false;
                session.setCurrentMode(session instanceof TeamMatchSession ? "TeamMatch" : "Play");
            }
            resumeAnimationTimer();
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
                if (controller.readScroll(scroll)) {
                    AudioManager.getInstance().play("interact");
                    String msg = session.consumeLastPortalMessage();
                    showGameMessage(msg != null && !msg.isEmpty()
                            ? msg : "Shadow Clone summoned!");
                    render();
                    refresh[0].run();
                } else {
                    AudioManager.getInstance().play("error");
                    showGameMessage("Could not read that scroll.");
                }
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
                if (controller.readScroll(scroll)) {
                    AudioManager.getInstance().play("interact");
                    String msg = session.consumeLastPortalMessage();
                    showGameMessage(msg != null && !msg.isEmpty()
                            ? msg : "Shadow Clone summoned!");
                } else {
                    AudioManager.getInstance().play("error");
                    showGameMessage("Could not read that scroll.");
                }
                popup.close();
                render();
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
        popup.setOnHidden(e -> resumeAnimationTimer());
        popup.showAndWait();
    }

    private void showContainerContentsPopup(Object containerObj, GameSession session) {
        if (animationTimer != null) {
            animationTimer.stop();
        }

        if (containerObj instanceof StaticContainer) {
            StaticContainer sc = (StaticContainer) containerObj;
            if (!sc.isShelfLike() && sc.isEmpty()) {
                sc.removeFromMapIfLooted(session.getMap());
                render();
                return;
            }
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
                            if (containerObj instanceof StaticContainer) {
                                ((StaticContainer) containerObj).removeFromMapIfLooted(session.getMap());
                            }
                            popup.close();
                            render();
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
            resumeAnimationTimer();
            Platform.runLater(() -> canvas.requestFocus());
            render();
        });
        popup.showAndWait();
    }

    private String getContainerDisplayName(Object containerObj) {
        if (containerObj instanceof StaticContainer) {
            StaticContainer sc = (StaticContainer) containerObj;
            if (sc.isShelfLike()) {
                com.kurawler.model.Map map = controller.getSession().getMap();
                for (int r = 0; r < map.getHeight(); r++) {
                    for (int c = 0; c < map.getWidth(); c++) {
                        GridCell cell = map.getCell(r, c);
                        if (cell == null) {
                            continue;
                        }
                        for (GameObject obj : cell.getObjects()) {
                            if (obj == sc && cell.getStaticObject() != null) {
                                return cell.getStaticObject().getName();
                            }
                        }
                    }
                }
            }
            return sc.getName();
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

    private double[] lerpPosition(Object entity, double targetX, double targetY) {
        double[] pos = smoothPositions.get(entity);
        if (pos == null) {
            pos = new double[]{targetX, targetY};
            smoothPositions.put(entity, pos);
        }
        pos[0] += (targetX - pos[0]) * LERP_SPEED;
        pos[1] += (targetY - pos[1]) * LERP_SPEED;
        if (Math.abs(pos[0] - targetX) < 1.0) pos[0] = targetX;
        if (Math.abs(pos[1] - targetY) < 1.0) pos[1] = targetY;
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
