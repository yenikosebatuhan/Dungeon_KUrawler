package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages screen transitions. UI delegates here to switch between
 * Main Menu, Play Mode, and Build Mode.
 */
public class ScreenManager {
    private static final Path SAVES_DIR = Paths.get("saves");
    private static final DateTimeFormatter SAVE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
        session.wireObservers();
        GameController controller = new GameController(session);
        GameView view = new GameView(controller, this);
        view.show(stage);
    }
    
    public boolean hasActiveSession() {
        return currentSession != null;
    }

    public boolean saveCurrentMap(String filename) {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        return serializer.saveMapToFile(currentSession.getMap(), filename);
    }

    public boolean saveCurrentGameAs(String saveName) {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        return serializer.saveGameState(currentSession, savePath(saveName).toString());
    }

    public boolean showSaveDialog() {
        TextInputDialog dialog = new TextInputDialog(defaultSaveName());
        dialog.setTitle("Save Game");
        dialog.setHeaderText("Save current game");
        dialog.setContentText("Save name:");
        dialog.initOwner(stage);

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            return false;
        }
        return saveCurrentGameAs(result.get().trim());
    }

    public boolean loadMap(String filename) {
        com.kurawler.model.Map loaded = deserializer.loadMapFromFile(filename);
        if (loaded != null) {
            currentSession = new GameSession(loaded);
            return true;
        }
        return false;
    }

    public boolean showLoadDialogAndStart() {
        List<SaveEntry> saves = listSaves();
        if (saves.isEmpty()) {
            showInfo("Load Game", "No saves found in the saves folder.");
            return false;
        }

        ChoiceDialog<SaveEntry> dialog = new ChoiceDialog<>(saves.get(0), saves);
        dialog.setTitle("Load Game");
        dialog.setHeaderText("Choose a saved game");
        dialog.setContentText("Save:");
        dialog.initOwner(stage);

        Optional<SaveEntry> result = dialog.showAndWait();
        if (!result.isPresent()) {
            return false;
        }
        return loadSavedGame(result.get().path);
    }

    public boolean loadSavedGame(Path path) {
        GameSession loadedSession = new GameSession();
        if (!deserializer.loadGameState(path.toString(), loadedSession)) {
            showInfo("Load Game", "Could not load the selected save.");
            return false;
        }
        currentSession = loadedSession;
        showPlayMode(currentSession);
        return true;
    }

    public void showHelp() {
        HelpScreen helpScreen = new HelpScreen(this);
        stage.setScene(helpScreen.createScene());
        stage.setTitle("Dungeon KUrawler - Help");
        stage.setResizable(false);
        stage.show();
    }

    public void startRandomDungeon() {
        com.kurawler.model.ProceduralMapGenerator generator =
                new com.kurawler.model.ProceduralMapGenerator();
        currentSession = generator.generate();
        showPlayMode(currentSession);
    }

    public void startTeamMatch() {
        com.kurawler.model.Map map;
        if (currentSession != null && currentSession.getMap() != null) {
            map = currentSession.getMap();
        } else {
            map = MapFactory.createDefaultMap().getMap();
        }

        TeamMatchSession tmSession = new TeamMatchSession(map);
        if (!tmSession.validateMap()) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Map does not have enough open space for Team Match.");
            alert.setTitle("Team Match Error");
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }

        tmSession.initializeMatch();
        currentSession = tmSession;

        GameController controller = new GameController(tmSession);
        GameView view = new GameView(controller, this);
        view.show(stage);
    }

    public void showBuildMode() {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        EditorView editorView = new EditorView(this, currentSession);
        editorView.show(stage);
    }

    public boolean saveEditedMap(com.kurawler.model.Map map, String filename) {
        return map != null && serializer.saveMapToFile(map, filename);
    }

    public void playEditedMap(com.kurawler.model.Map map) {
        if (map == null) {
            return;
        }
        GameSession session = new GameSession(map);
        prepareSessionForPlay(session);
        showPlayMode(session);
    }

    private void prepareSessionForPlay(GameSession session) {
        com.kurawler.model.Map map = session.getMap();
        Hero hero = null;
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell.getOccupyingCharacter() instanceof Hero) {
                    hero = (Hero) cell.getOccupyingCharacter();
                    hero.setCurrentCell(cell);
                    session.setHero(hero);
                } else if (cell.getOccupyingCharacter() instanceof Enemy) {
                    session.addEnemy((Enemy) cell.getOccupyingCharacter());
                }
            }
        }

        if (hero == null) {
            hero = new Hero("Adventurer");
            GridCell spawnCell = firstWalkableCell(map);
            if (spawnCell != null) {
                hero.moveToCell(spawnCell);
            }
            session.setHero(hero);
        }

        // PDF Phase II: Shadow Clone appears only after READing a Shadow Clone
        // Scroll (auto-spawned every 15 s), not at game start.
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();
    }

    private GridCell firstWalkableCell(com.kurawler.model.Map map) {
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && !cell.isBlocked()) {
                    return cell;
                }
            }
        }
        return null;
    }

    public Stage getStage() {
        return stage;
    }

    private Path savePath(String saveName) {
        String safeName = saveName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.isEmpty()) {
            safeName = defaultSaveName();
        }
        if (!safeName.endsWith(".json")) {
            safeName += ".json";
        }
        return SAVES_DIR.resolve(safeName);
    }

    private String defaultSaveName() {
        return "save_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now());
    }

    private List<SaveEntry> listSaves() {
        List<SaveEntry> saves = new ArrayList<>();
        if (!Files.isDirectory(SAVES_DIR)) {
            return saves;
        }
        try (Stream<Path> paths = Files.list(SAVES_DIR)) {
            paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .forEach(path -> saves.add(new SaveEntry(path, lastModifiedSafe(path))));
        } catch (IOException e) {
            showInfo("Load Game", "Could not read saves folder.");
        }
        return saves;
    }

    private LocalDateTime lastModifiedSafe(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(),
                    java.time.ZoneId.systemDefault());
        } catch (IOException e) {
            return LocalDateTime.MIN;
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private static class SaveEntry {
        private final Path path;
        private final LocalDateTime modifiedAt;

        private SaveEntry(Path path, LocalDateTime modifiedAt) {
            this.path = path;
            this.modifiedAt = modifiedAt;
        }

        @Override
        public String toString() {
            String fileName = path.getFileName().toString().replaceFirst("\\.json$", "");
            return fileName + "  (" + SAVE_TIME_FORMAT.format(modifiedAt) + ")";
        }
    }
}
