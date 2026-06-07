package com.kurawler.ui;

import com.kurawler.controller.GameController;
import com.kurawler.engine.AudioManager;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
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
    private static final Path MAPS_DIR = Paths.get("maps");
    private static final Path LEGACY_MAP_SAVE = Paths.get("map_save.json");
    private static final DateTimeFormatter SAVE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final int WINDOW_W = 1248;
    public static final int WINDOW_H = 810;

    private final Stage stage;
    private final MapSerializer serializer = new MapSerializer();
    private final MapDeserializer deserializer = new MapDeserializer();
    private GameSession currentSession;
    private GameView activeGameView;
    private String pendingEditorStatus;
    private String lastGameType = "Default";
    private String lastDifficulty = "Normal";
    /** Map chosen for the next main-menu Team Match launch (not reused from other modes). */
    private com.kurawler.model.Map pendingTeamMatchMap;
    /** Last map file saved from the level editor — used for Team Match. */
    private Path lastSavedEditorMapPath;
    private String lastSavedEditorWorldStartId;

    public ScreenManager(Stage stage) {
        this.stage = stage;
    }

    public void shutdownActiveGameView() {
        if (activeGameView != null) {
            activeGameView.shutdown();
            activeGameView = null;
        }
    }

    public String consumeEditorStatusMessage() {
        String msg = pendingEditorStatus;
        pendingEditorStatus = null;
        return msg;
    }

    public void showMainMenu() {
        shutdownActiveGameView();
        pendingTeamMatchMap = null;
        AudioManager.getInstance().playMusic("menu", true);
        MainMenuScreen menu = new MainMenuScreen(this);
        stage.setScene(menu.createScene());
        stage.setTitle("Dungeon KUrawler");
        stage.setResizable(false);
        stage.show();
    }

    public void startNewGame() {
        currentSession = null;
        showDifficultyScreen("Default");
    }

    /** PBI-16: play a map or world JSON from maps/ with a win objective. */
    public void startNewGameFromCustomMap() {
        MapFileEntry chosen = pickCustomMapFileForPlay();
        if (chosen == null) {
            return;
        }
        GameSession session = loadSessionFromMapFile(chosen.path);
        if (session == null) {
            EditorStyledDialog.showMessage(stage, "Custom Map",
                    "Could not load the selected file.\n"
                    + "Use a .json map or a .world.json saved from the editor.");
            return;
        }
        prepareSessionForPlay(session);
        MapFactory.configureObjectiveForMap(resolveObjectiveMap(session), session);
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();
        lastGameType = "Custom";
        lastDifficulty = "Normal";
        showPlayMode(session);
    }

    /** Dialog for New Game (Custom Map) — lists single-map JSON and editor world files. */
    private MapFileEntry pickCustomMapFileForPlay() {
        List<MapFileEntry> entries = new ArrayList<>();
        entries.addAll(listWorldFiles());
        entries.addAll(listSingleMapFiles());
        if (entries.isEmpty()) {
            EditorStyledDialog.showMessage(stage, "Custom Map",
                    "No map files found in maps/.\n"
                    + "Save a map (.json) or world (.world.json) in the level editor first.");
            return null;
        }
        Optional<MapFileEntry> picked = EditorStyledDialog.chooseDeletableItem(
                stage,
                "Custom Map",
                "Pick a dungeon to play.",
                entries,
                MapFileEntry::toString,
                entry -> deleteFileQuiet(entry.path));
        if (!picked.isPresent()) {
            return null;
        }
        return picked.get();
    }

    private GameSession loadSessionFromMapFile(Path path) {
        String filename = path.getFileName().toString();
        if (filename.endsWith(".world.json")) {
            MapDeserializer.WorldData data = deserializer.loadWorldFromFile(path.toString());
            if (data == null || data.maps == null || data.maps.isEmpty()) {
                return null;
            }
            String startId = data.startMapId != null && data.maps.containsKey(data.startMapId)
                    ? data.startMapId
                    : data.maps.keySet().iterator().next();
            com.kurawler.model.Map startMap = data.maps.get(startId);
            GameSession session = new GameSession(startMap);
            for (java.util.Map.Entry<String, com.kurawler.model.Map> e : data.maps.entrySet()) {
                session.registerWorldMap(e.getKey(), e.getValue());
            }
            session.setCurrentMapId(startId);
            return session;
        }
        com.kurawler.model.Map map = deserializer.loadMapFromFile(path.toString());
        return map != null ? new GameSession(map) : null;
    }

    private com.kurawler.model.Map resolveObjectiveMap(GameSession session) {
        java.util.Map<String, com.kurawler.model.Map> world = session.getWorldMaps();
        if (world != null && !world.isEmpty()) {
            return world.values().stream().reduce((a, b) -> b).orElse(session.getMap());
        }
        return session.getMap();
    }

    public void showPlayMode() {
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        showPlayMode(currentSession);
    }

    public void showPlayMode(GameSession session) {
        shutdownActiveGameView();
        AudioManager.getInstance().playMusic("combat", true);
        currentSession = session;
        session.wireObservers();
        GameController controller = new GameController(session);
        GameView view = new GameView(controller, this);
        activeGameView = view;
        view.show(stage);
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
        return serializer.saveGameState(currentSession, savePath(saveName).toString(), saveName);
    }

    public boolean showSaveDialog() {
        Optional<String> result = EditorStyledDialog.promptText(
                stage,
                "Save Game",
                "Name your save — it goes in the saves/ folder.",
                defaultSaveName());
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
        return showLoadDialogAndStart(null);
    }

    /** In-game load menu: Team Match sessions only list Team Match saves. */
    public String resolveLoadFilterForSession(GameSession session) {
        if (session instanceof TeamMatchSession) {
            return "TeamMatch";
        }
        String saved = session != null ? session.getSavedGameType() : null;
        if ("TeamMatch".equals(saved)) {
            return "TeamMatch";
        }
        return null;
    }

    public boolean showLoadDialogAndStart(String requiredGameType) {
        List<SaveEntry> saves = listSaves();
        if (requiredGameType != null) {
            saves = filterSavesByGameType(saves, requiredGameType);
        }
        if (saves.isEmpty()) {
            if ("TeamMatch".equals(requiredGameType)) {
                showInfo("Load Game", "No Team Match saves found.\nSave your match from the in-game menu first.");
            } else {
                showInfo("Load Game", "No saves found in the saves folder.");
            }
            return false;
        }

        Optional<SaveEntry> picked = EditorStyledDialog.chooseDeletableItem(
                stage,
                "Load Game",
                "TeamMatch".equals(requiredGameType)
                        ? "Choose a Team Match save to continue."
                        : "Choose a saved game to continue.",
                saves,
                SaveEntry::toString,
                entry -> deleteFileQuiet(entry.path));
        if (!picked.isPresent()) {
            return false;
        }
        return loadSavedGame(picked.get().path);
    }

    public boolean loadSavedGame(Path path) {
        String gameType = deserializer.readGameType(path.toString());
        GameSession loadedSession = new GameSession();
        if (!deserializer.loadGameState(path.toString(), loadedSession)) {
            showInfo("Load Game", "Could not load the selected save.");
            return false;
        }
        if (gameType == null || gameType.isBlank()) {
            gameType = inferGameType(loadedSession);
        }
        GameSession sessionToPlay;
        if ("TeamMatch".equals(gameType)) {
            sessionToPlay = TeamMatchSession.fromLoadedGameSession(loadedSession);
        } else {
            sessionToPlay = loadedSession;
            if ("Paused".equals(sessionToPlay.getCurrentMode())) {
                sessionToPlay.setCurrentMode("Play");
            }
        }
        currentSession = sessionToPlay;
        lastGameType = gameType;
        lastDifficulty = sessionToPlay.getDifficulty();
        showPlayMode(currentSession);
        return true;
    }

    private List<SaveEntry> filterSavesByGameType(List<SaveEntry> saves, String requiredGameType) {
        List<SaveEntry> filtered = new ArrayList<>();
        for (SaveEntry entry : saves) {
            String gameType = deserializer.readGameType(entry.path.toString());
            if (requiredGameType.equals(gameType)) {
                filtered.add(entry);
                continue;
            }
            if (gameType == null && "TeamMatch".equals(requiredGameType) && isLegacyTeamMatchSave(entry.path)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private boolean isLegacyTeamMatchSave(Path path) {
        GameSession probe = new GameSession();
        if (!deserializer.loadGameState(path.toString(), probe)) {
            return false;
        }
        return "TeamMatch".equals(inferGameType(probe));
    }

    private String inferGameType(GameSession session) {
        if (session instanceof TeamMatchSession || "TeamMatch".equals(session.getCurrentMode())) {
            return "TeamMatch";
        }
        if (looksLikeTeamMatchSave(session)) {
            return "TeamMatch";
        }
        if ("Paused".equals(session.getCurrentMode())) {
            return "Play";
        }
        String mode = session.getCurrentMode();
        return mode != null && !mode.isBlank() ? mode : "Play";
    }

    private boolean looksLikeTeamMatchSave(GameSession session) {
        if (session.getHero() == null || session.getMap() == null) {
            return false;
        }
        List<com.kurawler.model.character.Enemy> enemies = session.getEnemies();
        if (enemies.size() < 6 || enemies.size() > 8) {
            return false;
        }
        for (com.kurawler.model.character.Enemy enemy : enemies) {
            if (!(enemy instanceof com.kurawler.model.character.Knight)
                    && !(enemy instanceof com.kurawler.model.character.Sorcerer)) {
                return false;
            }
        }
        return true;
    }

    public void showHelp() {
        AudioManager.getInstance().playMusic("menu", true);
        HelpScreen helpScreen = new HelpScreen(this);
        stage.setScene(helpScreen.createScene());
        stage.setTitle("Dungeon KUrawler - Help");
        stage.setResizable(false);
        stage.show();
    }

    public void startRandomDungeon() {
        showDifficultyScreen("Random");
    }

    public void playAgain() {
        shutdownActiveGameView();
        launchGameWithDifficulty(lastGameType, lastDifficulty);
    }

    public void startTeamMatch() {
        shutdownActiveGameView();
        pendingTeamMatchMap = null;
        com.kurawler.model.Map map = resolveTeamMatchMap();
        if (map == null) {
            EditorStyledDialog.showMessage(stage, "Team Match",
                    "Save a map in the level editor first.\nUse Save in the editor toolbar.");
            return;
        }

        com.kurawler.model.Map playMap = cloneMapForPlay(map);
        if (playMap == null) {
            EditorStyledDialog.showMessage(stage, "Team Match Error",
                    "Could not load the saved editor map for Team Match.");
            return;
        }

        TeamMatchSession tmSession = new TeamMatchSession(playMap);
        if (!tmSession.validateMap()) {
            EditorStyledDialog.showMessage(stage, "Team Match Error",
                    tmSession.describeValidationFailure());
            return;
        }

        pendingTeamMatchMap = playMap;
        showDifficultyScreen("TeamMatch");
    }

    /** Rebuilds the current Team Match on the same map (in-game restart / play again). */
    public void restartTeamMatch() {
        shutdownActiveGameView();
        com.kurawler.model.Map map = currentSession != null && currentSession.getMap() != null
                ? currentSession.getMap()
                : MapFactory.createDefaultMap().getMap();
        TeamMatchSession tmSession = new TeamMatchSession(map);
        if (!tmSession.validateMap()) {
            EditorStyledDialog.showMessage(stage, "Team Match Error",
                    "Map does not have enough open space for Team Match.");
            showMainMenu();
            return;
        }
        tmSession.initializeMatch();
        String difficulty = lastDifficulty != null ? lastDifficulty : "Normal";
        lastGameType = "TeamMatch";
        applyDifficultyToSession(tmSession, difficulty);
        showPlayMode(tmSession);
    }

    public void showBuildMode() {
        shutdownActiveGameView();
        pendingTeamMatchMap = null;
        AudioManager.getInstance().playMusic("menu", true);
        if (currentSession == null) {
            currentSession = MapFactory.createDefaultMap();
        }
        EditorView editorView = new EditorView(this, currentSession);
        editorView.show(stage);
    }

    /** After Play This Map — return to editor without game-over overlay on editor UI. */
    public void returnToEditorAfterTest(com.kurawler.model.Map map, String statusMessage) {
        shutdownActiveGameView();
        restoreHeroForEditor(map);
        currentSession = new GameSession(map);
        pendingEditorStatus = statusMessage;
        new EditorView(this, currentSession).show(stage);
    }

    /** After Play — return to editor, restoring multi-map world when present. */
    public void returnToEditorAfterTest(GameSession session, String statusMessage) {
        if (session == null) {
            return;
        }
        com.kurawler.model.Map map = session.getMap();
        shutdownActiveGameView();
        if (map != null) {
            restoreHeroForEditor(map);
        }
        pendingEditorStatus = statusMessage;
        java.util.Map<String, com.kurawler.model.Map> world = session.getWorldMaps();
        if (world != null && !world.isEmpty()) {
            java.util.LinkedHashMap<String, com.kurawler.model.Map> linked =
                    new java.util.LinkedHashMap<>(world);
            String startId = session.getCurrentMapId();
            String resolvedStart = (startId != null && linked.containsKey(startId))
                    ? startId : linked.keySet().iterator().next();
            new EditorView(this, linked, resolvedStart).show(stage);
        } else if (map != null) {
            currentSession = new GameSession(map);
            new EditorView(this, currentSession).show(stage);
        } else {
            showBuildMode();
        }
    }

    /** Play a full multi-map world from the editor. */
    public void playEditedWorld(java.util.LinkedHashMap<String, com.kurawler.model.Map> maps,
                                String startMapId) {
        if (maps == null || maps.isEmpty()) {
            return;
        }
        com.kurawler.model.Map startMap = maps.containsKey(startMapId)
                ? maps.get(startMapId) : maps.values().iterator().next();
        String resolvedStart = maps.containsKey(startMapId)
                ? startMapId : maps.keySet().iterator().next();
        GameSession session = new GameSession(startMap);
        session.setEditorTestPlay(true);
        for (java.util.Map.Entry<String, com.kurawler.model.Map> e : maps.entrySet()) {
            session.registerWorldMap(e.getKey(), e.getValue());
        }
        session.setCurrentMapId(resolvedStart);
        prepareSessionForPlay(session);
        showPlayMode(session);
    }

    /** Team Match from the level editor — uses the last map saved via Save. */
    public void playEditedMapAsTeamMatch() {
        pendingTeamMatchMap = null;
        com.kurawler.model.Map map = resolveTeamMatchMap();
        if (map == null) {
            EditorStyledDialog.showMessage(stage, "Team Match",
                    "Save your map first (Save button), then start Team Match.");
            return;
        }
        launchTeamMatchOnMap(map, true);
    }

    private void launchTeamMatchOnMap(com.kurawler.model.Map map, boolean editorTestPlay) {
        shutdownActiveGameView();
        com.kurawler.model.Map playMap = cloneMapForPlay(map);
        if (playMap == null) {
            EditorStyledDialog.showMessage(stage, "Team Match Error",
                    "Could not prepare the saved map for Team Match.");
            return;
        }

        TeamMatchSession tmSession = new TeamMatchSession(playMap);
        if (!tmSession.validateMap()) {
            EditorStyledDialog.showMessage(stage, "Team Match Error",
                    tmSession.describeValidationFailure());
            return;
        }
        tmSession.initializeMatch();
        tmSession.setEditorTestPlay(editorTestPlay);
        tmSession.setAchievementManager(new AchievementManager());
        tmSession.wireObservers();
        showPlayMode(tmSession);
    }

    private com.kurawler.model.Map resolveTeamMatchMap() {
        if (lastSavedEditorMapPath != null && Files.isRegularFile(lastSavedEditorMapPath)) {
            com.kurawler.model.Map map =
                    loadEditorMapFile(lastSavedEditorMapPath, lastSavedEditorWorldStartId);
            if (map != null) {
                return map;
            }
        }
        return loadMostRecentEditorMapFile();
    }

    private com.kurawler.model.Map loadEditorMapFile(Path path, String preferredWorldStartId) {
        String name = path.getFileName().toString();
        if (name.endsWith(".world.json")) {
            MapDeserializer.WorldData data = deserializer.loadWorldFromFile(path.toString());
            if (data == null || data.maps == null || data.maps.isEmpty()) {
                return null;
            }
            String startId = preferredWorldStartId;
            if (startId == null || !data.maps.containsKey(startId)) {
                startId = data.startMapId;
            }
            if (startId == null || !data.maps.containsKey(startId)) {
                startId = data.maps.keySet().iterator().next();
            }
            return data.maps.get(startId);
        }
        if (name.endsWith(".json")) {
            return deserializer.loadMapFromFile(path.toString());
        }
        return null;
    }

    private com.kurawler.model.Map loadMostRecentEditorMapFile() {
        List<MapFileEntry> entries = new ArrayList<>();
        entries.addAll(listWorldFiles());
        entries.addAll(listSingleMapFiles());
        if (entries.isEmpty()) {
            return null;
        }
        entries.sort(Comparator.comparing((MapFileEntry e) -> lastModifiedSafe(e.path)).reversed());
        MapFileEntry latest = entries.get(0);
        lastSavedEditorMapPath = latest.path;
        if (latest.path.getFileName().toString().endsWith(".world.json")) {
            MapDeserializer.WorldData data = deserializer.loadWorldFromFile(latest.path.toString());
            lastSavedEditorWorldStartId = data != null ? data.startMapId : null;
        } else {
            lastSavedEditorWorldStartId = null;
        }
        return loadEditorMapFile(latest.path, lastSavedEditorWorldStartId);
    }

    private void rememberEditorSaveForTeamMatch(Path path, String worldStartMapId) {
        lastSavedEditorMapPath = path;
        lastSavedEditorWorldStartId = worldStartMapId;
    }

    private com.kurawler.model.Map cloneMapForPlay(com.kurawler.model.Map source) {
        if (source == null) {
            return null;
        }
        String json = serializer.mapToJson(source);
        com.kurawler.model.Map cloned = deserializer.loadMapFromJson(json);
        return cloned != null ? cloned : source;
    }

    /** Save world JSON (all editor maps) to maps/<name>.world.json. */
    public boolean showSaveWorldDialog(java.util.LinkedHashMap<String, com.kurawler.model.Map> maps,
                                       String startMapId) {
        if (maps == null || maps.isEmpty()) {
            return false;
        }
        Optional<String> result = EditorStyledDialog.promptText(
                stage, "Save World", "Name for your multi-map world file.", "my_world");
        if (!result.isPresent()) {
            return false;
        }
        String name = result.get().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!name.endsWith(".world.json")) {
            name += ".world.json";
        }
        try {
            Files.createDirectories(MAPS_DIR);
        } catch (IOException e) {
            showInfo("Save World", "Could not create maps folder.");
            return false;
        }
        Path target = MAPS_DIR.resolve(name);
        boolean saved = serializer.saveWorldToFile(maps, startMapId, target.toString());
        if (!saved) {
            showInfo("Save World", "Could not save world file.");
        } else {
            rememberEditorSaveForTeamMatch(target, startMapId);
        }
        return saved;
    }

    /** Load a world JSON file into the editor. Returns null on cancel/failure. */
    public MapDeserializer.WorldData showLoadWorldDialog() {
        List<MapFileEntry> files = listWorldFiles();
        if (files.isEmpty()) {
            EditorStyledDialog.showMessage(stage, "Load World",
                    "No world files found.\nSave a world first — files end in .world.json.");
            return null;
        }
        Optional<MapFileEntry> picked = EditorStyledDialog.chooseDeletableItem(
                stage, "Load World",
                "Pick a world file to open.",
                files,
                MapFileEntry::toString,
                entry -> deleteFileQuiet(entry.path));
        if (!picked.isPresent()) {
            return null;
        }
        MapDeserializer.WorldData data =
                deserializer.loadWorldFromFile(picked.get().path.toString());
        if (data == null) {
            EditorStyledDialog.showMessage(stage, "Load World", "Could not load the world file.");
        }
        return data;
    }

    private List<MapFileEntry> listWorldFiles() {
        List<MapFileEntry> entries = new ArrayList<>();
        collectWorldFiles(MAPS_DIR, "", entries);
        // Older builds saved to maps/maps/ by mistake — still list those for Load.
        collectWorldFiles(MAPS_DIR.resolve("maps"), "maps/", entries);
        entries.sort(Comparator.comparing((MapFileEntry e) -> lastModifiedSafe(e.path)).reversed());
        return entries;
    }

    private void collectWorldFiles(Path dir, String labelPrefix, List<MapFileEntry> entries) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.list(dir)) {
            paths
                    .filter(p -> p.getFileName().toString().endsWith(".world.json"))
                    .forEach(p -> entries.add(new MapFileEntry(
                            p, "[World] " + labelPrefix + p.getFileName().toString())));
        } catch (IOException ignored) {
            // empty list
        }
    }

    public boolean saveEditedMap(com.kurawler.model.Map map, String filename) {
        return map != null && serializer.saveMapToFile(map, filename);
    }

    /** Save editor map to {@code maps/<name>.json} (PDF §4.2). */
    public boolean showSaveMapDialog(com.kurawler.model.Map map) {
        if (map == null) {
            return false;
        }
        Optional<String> result = EditorStyledDialog.promptText(
                stage,
                "Save Map",
                "Store your dungeon as JSON in the maps/ folder.",
                "my_dungeon");
        if (!result.isPresent()) {
            return false;
        }
        String name = result.get().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!name.endsWith(".json")) {
            name += ".json";
        }
        try {
            Files.createDirectories(MAPS_DIR);
        } catch (IOException e) {
            showInfo("Save Map", "Could not create maps folder.");
            return false;
        }
        Path target = MAPS_DIR.resolve(name);
        boolean saved = serializer.saveMapToFile(map, target.toString());
        if (saved) {
            rememberEditorSaveForTeamMatch(target, null);
        }
        return saved;
    }

    /** Load a map JSON into the level editor (PDF §4.2). */
    public com.kurawler.model.Map showLoadMapDialog() {
        List<MapFileEntry> maps = listMapFiles();
        if (maps.isEmpty()) {
            EditorStyledDialog.showMessage(stage, "Load Map",
                    "No map files found.\nSave a map first — it goes in the maps/ folder.");
            return null;
        }

        Optional<MapFileEntry> picked = EditorStyledDialog.chooseDeletableItem(
                stage,
                "Load Map",
                "Pick a saved dungeon to open in the editor.",
                maps,
                MapFileEntry::toString,
                entry -> deleteFileQuiet(entry.path));
        if (!picked.isPresent()) {
            return null;
        }
        MapFileEntry chosen = picked.get();
        if (chosen.path.getFileName().toString().endsWith(".world.json")) {
            EditorStyledDialog.showMessage(stage, "Load Map",
                    "This is a world file (.world.json).\n"
                    + "Use Load in the editor toolbar, or play it via New Game (Custom Map).");
            return null;
        }
        com.kurawler.model.Map loaded = deserializer.loadMapFromFile(chosen.path.toString());
        if (loaded == null) {
            EditorStyledDialog.showMessage(stage, "Load Map", "Could not load the selected map.");
        }
        return loaded;
    }

    private List<MapFileEntry> listMapFiles() {
        List<MapFileEntry> entries = new ArrayList<>();
        entries.addAll(listSingleMapFiles());
        return entries;
    }

    /** Single-map JSON only (excludes editor world bundles). */
    private List<MapFileEntry> listSingleMapFiles() {
        List<MapFileEntry> entries = new ArrayList<>();
        if (Files.isRegularFile(LEGACY_MAP_SAVE)) {
            entries.add(new MapFileEntry(LEGACY_MAP_SAVE, "map_save.json (project root)"));
        }
        if (Files.isDirectory(MAPS_DIR)) {
            try (Stream<Path> paths = Files.list(MAPS_DIR)) {
                paths
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return name.endsWith(".json") && !name.endsWith(".world.json");
                        })
                        .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                        .forEach(p -> entries.add(new MapFileEntry(
                                p, p.getFileName().toString())));
            } catch (IOException ignored) {
                // empty list
            }
        }
        return entries;
    }

    public void playEditedMap(com.kurawler.model.Map map) {
        if (map == null) {
            return;
        }
        GameSession session = new GameSession(map);
        session.setEditorTestPlay(true);
        prepareSessionForPlay(session);
        showPlayMode(session);
    }

    private void restoreHeroForEditor(com.kurawler.model.Map map) {
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.getOccupyingCharacter() instanceof Hero) {
                    Hero hero = (Hero) cell.getOccupyingCharacter();
                    hero.heal(hero.getMaxHealth());
                    hero.restoreMana(hero.getMaxMana());
                    hero.restoreEnergy(hero.getMaxEnergy());
                }
            }
        }
    }

    private void prepareSessionForPlay(GameSession session) {
        session.clearEnemies();
        Hero hero = adoptPlacedHero(session);
        com.kurawler.model.Map map = session.getMap();
        if (hero == null) {
            hero = collectEnemiesFromMap(map, session);
        } else {
            collectEnemiesFromMap(map, session);
        }

        if (hero == null) {
            hero = new Hero("Adventurer");
            GridCell spawnCell = firstWalkableCell(map);
            if (spawnCell != null) {
                Character blocking = spawnCell.getOccupyingCharacter();
                if (blocking instanceof Enemy) {
                    ((Enemy) blocking).moveToCell(null);
                } else if (blocking != null) {
                    blocking.moveToCell(null);
                }
                hero.moveToCell(spawnCell);
            }
            session.setHero(hero);
        }

        // PDF Phase II: Shadow Clone appears only after READing a Shadow Clone
        // Scroll (auto-spawned every 15 s), not at game start.
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();
    }

    /** Uses the hero tile placed in the level editor / map JSON, including multi-map worlds. */
    private Hero adoptPlacedHero(GameSession session) {
        Hero hero = findHeroOnMap(session.getMap());
        String heroMapId = null;
        if (hero == null && session.getWorldMaps() != null) {
            for (java.util.Map.Entry<String, com.kurawler.model.Map> entry : session.getWorldMaps().entrySet()) {
                hero = findHeroOnMap(entry.getValue());
                if (hero != null) {
                    heroMapId = entry.getKey();
                    break;
                }
            }
        }
        if (hero == null) {
            return null;
        }
        if (heroMapId != null && !heroMapId.equals(session.getCurrentMapId())) {
            session.setCurrentMapId(heroMapId);
            session.setMap(session.getWorldMaps().get(heroMapId));
        }
        GridCell cell = hero.getCurrentCell();
        if (cell != null) {
            hero.moveToCell(cell);
        }
        session.setHero(hero);
        return hero;
    }

    private Hero findHeroOnMap(com.kurawler.model.Map map) {
        if (map == null) {
            return null;
        }
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.getOccupyingCharacter() instanceof Hero placed) {
                    return placed;
                }
            }
        }
        return null;
    }

    private Hero collectEnemiesFromMap(com.kurawler.model.Map map, GameSession session) {
        Hero hero = null;
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell == null) {
                    continue;
                }
                Character occupant = cell.getOccupyingCharacter();
                if (occupant instanceof Hero placed) {
                    hero = placed;
                    placed.moveToCell(cell);
                    session.setHero(placed);
                } else if (occupant instanceof Enemy) {
                    Enemy enemy = (Enemy) occupant;
                    enemy.moveToCell(cell);
                    session.addEnemy(enemy);
                }
            }
        }
        return hero;
    }

    private GridCell firstWalkableCell(com.kurawler.model.Map map) {
        GridCell fallback = null;
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell == null || cell.isBlocked() || cell.getOccupyingCharacter() != null) {
                    continue;
                }
                boolean interior = row > 0 && row < map.getHeight() - 1
                        && col > 0 && col < map.getWidth() - 1;
                if (interior) {
                    return cell;
                }
                if (fallback == null) {
                    fallback = cell;
                }
            }
        }
        return fallback;
    }

    public void showDifficultyScreen(String gameType) {
        shutdownActiveGameView();
        AudioManager.getInstance().playMusic("menu", true);
        DifficultySelectionScreen diffScreen = new DifficultySelectionScreen(this, gameType);
        stage.setScene(diffScreen.createScene());
        stage.setTitle("Select Difficulty");
        stage.setResizable(false);
        stage.show();
    }

    public void launchGameWithDifficulty(String gameType, String difficulty) {
        lastGameType = gameType;
        lastDifficulty = difficulty;

        if ("Random".equals(gameType)) {
            com.kurawler.model.ProceduralMapGenerator generator =
                    new com.kurawler.model.ProceduralMapGenerator();
            currentSession = generator.generate();
        } else if ("TeamMatch".equals(gameType)) {
            com.kurawler.model.Map map;
            if (pendingTeamMatchMap != null) {
                map = pendingTeamMatchMap;
                pendingTeamMatchMap = null;
            } else if (currentSession instanceof TeamMatchSession
                    && currentSession.getMap() != null) {
                map = currentSession.getMap();
            } else {
                com.kurawler.model.Map saved = resolveTeamMatchMap();
                if (saved == null) {
                    EditorStyledDialog.showMessage(stage, "Team Match",
                            "Save a map in the level editor first.\nUse Save in the editor toolbar.");
                    showMainMenu();
                    return;
                }
                map = cloneMapForPlay(saved);
                if (map == null) {
                    EditorStyledDialog.showMessage(stage, "Team Match Error",
                            "Could not load the saved editor map for Team Match.");
                    showMainMenu();
                    return;
                }
            }
            TeamMatchSession tmSession = new TeamMatchSession(map);
            tmSession.initializeMatch();
            currentSession = tmSession;
        } else {
            currentSession = MapFactory.createDefaultMap();
        }

        applyDifficultyToSession(currentSession, difficulty);
        showPlayMode(currentSession);
    }

    private void applyDifficultyToSession(GameSession session, String difficulty) {
        session.setDifficulty(difficulty);
        Hero hero = session.getHero();

        session.applyDifficultyHeroStats();

        List<com.kurawler.model.Map> mapsToCheck = new ArrayList<>();
        mapsToCheck.add(session.getMap());
        if (session.getWorldMaps() != null) {
            mapsToCheck.addAll(session.getWorldMaps().values());
        }

        for (com.kurawler.model.Map m : mapsToCheck) {
            if (m == null) {
                continue;
            }
            for (int r = 0; r < m.getHeight(); r++) {
                for (int c = 0; c < m.getWidth(); c++) {
                    GridCell cell = m.getCell(r, c);
                    if (cell == null) {
                        continue;
                    }

                    if (cell.getOccupyingCharacter() instanceof Enemy) {
                        Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                        if ("Easy".equals(difficulty)) {
                            enemy.setMaxHealth((int) (enemy.getMaxHealth() * 0.7));
                            enemy.setHealth((int) (enemy.getHealth() * 0.7));
                            enemy.setStrength((int) (enemy.getStrength() * 0.7));
                        } else if ("Hard".equals(difficulty)) {
                            enemy.setMaxHealth((int) (enemy.getMaxHealth() * 1.3));
                            enemy.setHealth((int) (enemy.getHealth() * 1.3));
                            enemy.setStrength((int) (enemy.getStrength() * 1.3));
                        }
                    }

                    if ("Hard".equals(difficulty)) {
                        List<GameObject> toRemove = new ArrayList<>();
                        for (GameObject obj : cell.getObjects()) {
                            if (obj instanceof Consumable && "Health Potion".equals(obj.getName())) {
                                toRemove.add(obj);
                            } else if (obj instanceof StaticContainer) {
                                StaticContainer sc = (StaticContainer) obj;
                                List<Item> chestItems = new ArrayList<>(sc.getContents());
                                sc.getContents().clear();
                                for (Item item : chestItems) {
                                    if (!(item instanceof Consumable
                                            && "Health Potion".equals(item.getName()))) {
                                        sc.addItem(item);
                                    }
                                }
                            }
                        }
                        for (GameObject obj : toRemove) {
                            cell.removeObject(obj);
                        }
                    }
                }
            }
        }
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
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .forEach(p -> {
                        String[] meta = deserializer.readSaveMetadata(p.toString());
                        String saveName = meta[0] != null ? meta[0]
                                : p.getFileName().toString().replaceFirst("\\.json$", "");
                        String savedAt  = meta[1] != null ? formatSavedAt(meta[1])
                                : SAVE_TIME_FORMAT.format(lastModifiedSafe(p));
                        saves.add(new SaveEntry(p, saveName, savedAt));
                    });
        } catch (IOException e) {
            showInfo("Load Game", "Could not read saves folder.");
        }
        return saves;
    }

    private String formatSavedAt(String isoInstant) {
        try {
            java.time.Instant instant = java.time.Instant.parse(isoInstant);
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            return SAVE_TIME_FORMAT.format(ldt);
        } catch (Exception e) {
            return isoInstant;
        }
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
        EditorStyledDialog.showMessage(stage, title, message);
    }

    private boolean deleteFileQuiet(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            showInfo("Delete", "Could not delete the file.");
            return false;
        }
    }

    private static class MapFileEntry {
        private final Path path;
        private final String label;

        private MapFileEntry(Path path, String label) {
            this.path = path;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class SaveEntry {
        private final Path path;
        private final String saveName;
        private final String savedAt;

        private SaveEntry(Path path, String saveName, String savedAt) {
            this.path = path;
            this.saveName = saveName;
            this.savedAt  = savedAt;
        }

        @Override
        public String toString() {
            return saveName + "  (" + savedAt + ")";
        }
    }
}
