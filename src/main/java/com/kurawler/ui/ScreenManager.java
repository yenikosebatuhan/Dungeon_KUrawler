package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.MapFactory;
import javafx.stage.Stage;

/**
 * Manages screen transitions. UI delegates here to switch between
 * Main Menu, Play Mode, and Build Mode.
 */
public class ScreenManager {

    private final Stage stage;

    public ScreenManager(Stage stage) {
        this.stage = stage;
    }

    public void showMainMenu() {
        MainMenuScreen menu = new MainMenuScreen(this);
        stage.setScene(menu.createScene());
        stage.setTitle("Dungeon KUrawler");
        stage.setResizable(false);
        stage.show();
    }

    public void showPlayMode() {
        GameSession session = MapFactory.createDefaultMap();
        showPlayMode(session);
    }

    public void showPlayMode(GameSession session) {
        GameController controller = new GameController(session);
        GameView view = new GameView(controller, this);
        view.show(stage);
    }

    public void showBuildMode() {
        // TODO: Phase 3 — EditorView
        System.out.println("[ScreenManager] Build mode not yet implemented.");
    }

    public Stage getStage() {
        return stage;
    }
}
