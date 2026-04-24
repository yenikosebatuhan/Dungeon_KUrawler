package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.MapFactory;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import javafx.stage.Stage;

/**
 * Manages screen transitions. UI delegates here to switch between
 * Main Menu, Play Mode, and Build Mode.
 */
public class ScreenManager {

    private final Stage stage;
    private final MapSerializer serializer = new MapSerializer();
    private final MapDeserializer deserializer = new MapDeserializer();
    private GameSession currentSession;

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

    public void startNewGame() {
        currentSession = null;
        showPlayMode();
    }

    public void showPlayMode() {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        showPlayMode(currentSession);
    }

    public void showPlayMode(GameSession session) {
        currentSession = session;
        GameController controller = new GameController(session);
        GameView view = new GameView(controller, this);
        view.show(stage);
    }

    public boolean saveCurrentMap(String filename) {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        return serializer.saveMapToFile(currentSession.getMap(), filename);
    }

    public boolean loadMap(String filename) {
        com.kurawler.model.Map loaded = deserializer.loadMapFromFile(filename);
        if (loaded != null) {
            currentSession = new GameSession(loaded);
            return true;
        }
        return false;
    }

    public void showBuildMode() {
        // TODO: Phase 3 — EditorView
        System.out.println("[ScreenManager] Build mode not yet implemented.");
    }

    public Stage getStage() {
        return stage;
    }
}
