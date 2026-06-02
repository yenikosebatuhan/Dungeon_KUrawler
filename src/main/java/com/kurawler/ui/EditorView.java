package com.kurawler.ui;

import com.kurawler.editor.LevelEditor;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.ResourceAssetCatalog;
import com.kurawler.model.WeaponCatalog;
import com.kurawler.model.character.Assassin;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Guard;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import com.kurawler.ui.TileRenderer.AssetKind;
import com.kurawler.ui.TileRenderer.EditorAsset;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Dungeon-themed level editor. Every sprite in the project's sheets is sliced
 * into a placeable asset (via {@link TileRenderer}'s atlas) and grouped into
 * stone-tab categories. The map starts completely empty so the designer places
 * everything themselves.
 */
public class EditorView {

    private static final int TILE_SIZE = 48;

    private static final int TOOLBAR_HEIGHT = 52;
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_PAD = 10;
    private static final int CARD_SIZE = 96;
    private static final int CARD_ICON = 48;
    private static final int WINDOW_FRAME_PAD = 16;
    private static final double MAX_WINDOW_W = 1280.0;
    private static final double MAX_WINDOW_H = 760.0;
    private static final int BG_TILE = 48;

    // Dungeon-stone palette
    private static final String STONE_DARK = "#13110d";
    private static final String GOLD = "#c8b880";
    private static final String GOLD_BRIGHT = "#f0e0a0";

    private static final String DRAG_OBJECT = "EDITOR_OBJ:";
    private static final String DRAG_CHARACTER = "EDITOR_CHAR:";
    private static final String DRAG_ERASE = "EDITOR_ERASE";

    private final ScreenManager screenManager;
    private final LevelEditor editor;
    private final TileRenderer renderer;

    private final java.util.Map<String, AssetKind> assetKinds = new HashMap<>();
    private final LinkedHashMap<String, List<EditorAsset>> assetsByCategory = new LinkedHashMap<>();

    private Stage stage;
    private StackPane sceneRoot;
    private Canvas bgCanvas;
    private Canvas canvas;
    private Group canvasGroup;
    private Label statusLabel;

    private FlowPane paletteGrid;
    private ScrollPane paletteScroll;
    private TextField searchField;
    private String activeCategory;
    private String searchQuery = "";

    private String selectedObjectName;
    private String selectedCharacterName;
    private EditMode mode = EditMode.PLACE_OBJECT;
    private String selectedToolId;
    private int hoverRow = -1;
    private int hoverCol = -1;
    private int lastPaintRow = -1;
    private int lastPaintCol = -1;
    private String lastEditMessage;
    private int currentRotation = 0; // unused placeholder kept for compat

    private final java.util.Map<String, Button> assetCards = new HashMap<>();
    private final java.util.Map<String, Button> categoryChips = new HashMap<>();
    private Button eraseButton;
    private Button rotateButton;

    private enum EditMode {
        PLACE_OBJECT,
        PLACE_CHARACTER,
        ERASE,
        ROTATE
    }

    public EditorView(ScreenManager screenManager, GameSession session) {
        this.screenManager = screenManager;
        this.renderer = new TileRenderer(TILE_SIZE);

        Map src = session != null ? session.getMap() : null;
        int w = src != null ? src.getWidth() : 22;
        int h = src != null ? src.getHeight() : 16;
        Map blank = new Map(w, h);
        pavePlainFloor(blank);
        this.editor = new LevelEditor(blank);

        indexCatalog();
    }

    /** Constructor used when returning from test-play with a full multi-map world. */
    public EditorView(ScreenManager screenManager,
                      java.util.LinkedHashMap<String, Map> worldMaps, String startMapId) {
        this.screenManager = screenManager;
        this.renderer = new TileRenderer(TILE_SIZE);

        Map startMap = worldMaps.containsKey(startMapId)
                ? worldMaps.get(startMapId) : worldMaps.values().iterator().next();
        this.editor = new LevelEditor(startMap);
        this.editor.loadWorld(worldMaps, startMapId);

        indexCatalog();
    }

    /** Replace the current map (single-map load from test-play fallback). */
    public void loadMap(Map map) {
        editor.setMap(map);
    }

    /** Fills cells that have no staticObject with an explicit Floor tile (fixes game rendering after load). */
    private void fillNullCells(Map map) {
        StaticObject floor = new StaticObject("Floor", "Stone floor", true, false);
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                if (map.getCell(row, col).getStaticObject() == null) {
                    map.getCell(row, col).setStaticObject(floor);
                }
            }
        }
    }

    /** Clears all cells to an explicit Floor tile so game rendering matches the editor. */
    private void pavePlainFloor(Map map) {
        StaticObject floor = new StaticObject("Floor", "Stone floor", true, false);
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                map.getCell(row, col).setStaticObject(floor);
            }
        }
    }

    private void indexCatalog() {
        for (EditorAsset asset : renderer.getEditorAssets()) {
            assetKinds.put(asset.name, asset.kind);
            assetsByCategory.computeIfAbsent(asset.category, k -> new ArrayList<>()).add(asset);
        }
    }

    public void show(Stage stage) {
        this.stage = stage;
        Map map = editor.getMap();
        int canvasW = map.getWidth() * TILE_SIZE;
        int canvasH = map.getHeight() * TILE_SIZE;

        canvas = new Canvas(canvasW, canvasH);
        wireCanvasInput();

        statusLabel = new Label("Empty map. Pick an asset, then drag or click to place it.");
        statusLabel.setFont(Font.font("Monospaced", 10));
        statusLabel.setTextFill(Color.web("#b8a988"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(PANEL_WIDTH - PANEL_PAD * 2);

        double scale = computeEditorScale(canvasW, canvasH);
        canvas.getTransforms().setAll(new Scale(scale, scale));
        canvasGroup = new Group(canvas);

        StackPane canvasArea = new StackPane(canvasGroup);
        canvasArea.setAlignment(Pos.CENTER);
        canvasArea.setStyle("-fx-background-color: " + STONE_DARK + ";");
        HBox.setHgrow(canvasArea, Priority.ALWAYS);

        VBox panel = buildLeftPanel();

        HBox body = new HBox(panel, canvasArea);
        body.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox content = new VBox(buildToolbar(), body);

        double sceneW = sceneWidth(canvasW, scale);
        double sceneH = sceneHeight(canvasH, scale);

        bgCanvas = new Canvas(sceneW, sceneH);
        drawDungeonBackground(bgCanvas, sceneW, sceneH);

        sceneRoot = new StackPane(bgCanvas, content);

        stage.setTitle("Dungeon KUrawler - Level Editor");
        Scene scene = new Scene(sceneRoot, sceneW, sceneH);
        // 'R' rotates the asset that gets placed next (also available as a toolbar button).
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.R) {
                cycleRotation();
            }
        });
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();

        if (!assetsByCategory.isEmpty()) {
            selectCategory(assetsByCategory.keySet().iterator().next());
        }

        String pending = screenManager.consumeEditorStatusMessage();
        if (pending != null) {
            statusLabel.setText(pending);
        }
        render();
    }

    // ---------- LAYOUT ----------

    private HBox buildToolbar() {
        Label title = new Label("\u2692  LEVEL EDITOR");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(GOLD));
        title.setPadding(new Insets(0, 8, 0, 6));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Map selector ───────────────────────────────────────────────────
        ComboBox<String> mapSelector = new ComboBox<>();
        mapSelector.getItems().addAll(editor.getWorldMaps().keySet());
        mapSelector.setValue(editor.getCurrentMapId());
        mapSelector.setStyle("-fx-background-color:#3a3220;-fx-text-fill:#e8c84a;"
                + "-fx-font-family:Serif;-fx-pref-width:110px;");
        mapSelector.setOnAction(e -> {
            String sel = mapSelector.getValue();
            if (sel != null && !sel.equals(editor.getCurrentMapId())) {
                editor.switchToMap(sel);
                render();
            }
        });

        Button newMap = stoneButton("+Map", 60);
        newMap.setOnAction(e -> {
            Optional<String> name = EditorStyledDialog.promptText(
                    stage, "New Map", "Enter a name for the new map:", "map_2");
            if (!name.isPresent()) return;
            String id = name.get().replaceAll("[^a-zA-Z0-9_-]", "_");
            editor.createNewMap(id);
            pavePlainFloor(editor.getMap());
            mapSelector.getItems().setAll(editor.getWorldMaps().keySet());
            mapSelector.setValue(editor.getCurrentMapId());
            render();
            setStatus("Created map '" + editor.getCurrentMapId() + "'.");
        });

        Button delMap = stoneButton("-Map", 60);
        delMap.setOnAction(e -> {
            if (editor.getWorldMaps().size() <= 1) { setStatus("Cannot delete the last map."); return; }
            String cur = editor.getCurrentMapId();
            editor.removeMap(cur);
            mapSelector.getItems().setAll(editor.getWorldMaps().keySet());
            mapSelector.setValue(editor.getCurrentMapId());
            render();
            setStatus("Deleted map '" + cur + "'.");
        });

        // ── Save / Load / Play ─────────────────────────────────────────────
        Button save = stoneButton("Save", 70);
        save.setOnAction(e -> {
            boolean saved = screenManager.showSaveWorldDialog(editor.getWorldMaps(), editor.getCurrentMapId());
            setStatus(saved ? "World saved under maps/." : "Save cancelled or failed.");
        });

        Button load = stoneButton("Load", 70);
        load.setOnAction(e -> {
            com.kurawler.persistence.MapDeserializer.WorldData data = screenManager.showLoadWorldDialog();
            if (data != null) {
                for (Map loadedMap : data.maps.values()) {
                    fillNullCells(loadedMap);
                }
                editor.loadWorld(data.maps, data.startMapId);
                mapSelector.getItems().setAll(editor.getWorldMaps().keySet());
                mapSelector.setValue(editor.getCurrentMapId());
                render();
                setStatus("Loaded world with " + data.maps.size() + " map(s).");
            }
        });

        Button play = stoneButton("Play", 70);
        play.setOnAction(e -> screenManager.playEditedWorld(editor.getWorldMaps(), editor.getCurrentMapId()));

        Button teamMatch = stoneButton("Team Match", 104);
        teamMatch.setOnAction(e -> screenManager.playEditedMapAsTeamMatch(editor.getMap()));

        Button random = stoneButton("Random", 84);
        random.setOnAction(e -> {
            com.kurawler.editor.RandomItemGenerator.PopulateResult result =
                    editor.addRandomItemsPerSpec();
            setStatus(String.format(
                    "Added %d floor items, %d container loot, hidden: %s.",
                    result.floorItemsPlaced,
                    result.containerLootAdded,
                    result.hiddenItemPlaced ? "yes" : "no"));
            render();
        });

        Button clear = stoneButton("Clear", 70);
        clear.setOnAction(e -> {
            editor.clearMap();
            pavePlainFloor(editor.getMap());
            setStatus("Map cleared.");
            render();
        });

        Button undo = stoneButton("Undo", 70);
        undo.setOnAction(e -> {
            if (editor.undo()) {
                setStatus("Undo applied.");
                render();
            } else {
                setStatus("Nothing to undo.");
            }
        });

        rotateButton = stoneButton("Rotate \u21BB", 84);
        rotateButton.setOnAction(e -> cycleRotation());
        rotateButton.setOnMouseEntered(e  -> rotateButton.setStyle(mode == EditMode.ROTATE ? ROTATE_ACTIVE : stoneHover()));
        rotateButton.setOnMouseExited(e   -> rotateButton.setStyle(mode == EditMode.ROTATE ? ROTATE_ACTIVE : stoneBase()));
        rotateButton.setOnMousePressed(e  -> rotateButton.setStyle(mode == EditMode.ROTATE ? ROTATE_ACTIVE : stonePressed()));
        rotateButton.setOnMouseReleased(e -> rotateButton.setStyle(mode == EditMode.ROTATE ? ROTATE_ACTIVE
                : (rotateButton.isHover() ? stoneHover() : stoneBase())));

        Button back = stoneButton("Main Menu", 104);
        back.setOnAction(e -> screenManager.showMainMenu());

        HBox bar = new HBox(5, title, mapSelector, newMap, delMap, spacer, save, load, play, teamMatch, random, rotateButton, clear, undo, back);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setMinHeight(TOOLBAR_HEIGHT);
        bar.setPrefHeight(TOOLBAR_HEIGHT);
        bar.setStyle("-fx-background-color: linear-gradient(to bottom, "
                + "rgba(42,38,30,0.94), rgba(24,21,16,0.96)); "
                + "-fx-border-color: #8b7940 transparent #3a3220 transparent; -fx-border-width: 2 0 2 0;");
        return bar;
    }

    private VBox buildLeftPanel() {
        Label header = new Label("ASSET  FORGE");
        header.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        header.setTextFill(Color.web(GOLD_BRIGHT));

        searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D  Search assets...");
        searchField.setFont(Font.font("Monospaced", 11));
        searchField.setStyle(searchFieldStyle());
        searchField.textProperty().addListener((obs, old, val) -> {
            searchQuery = val == null ? "" : val.trim().toLowerCase();
            rebuildPaletteCards();
        });

        FlowPane categoryStrip = new FlowPane(5, 5);
        categoryStrip.setAlignment(Pos.TOP_LEFT);
        for (String cat : assetsByCategory.keySet()) {
            Button chip = categoryChip(cat);
            categoryChips.put(cat, chip);
            categoryStrip.getChildren().add(chip);
        }

        eraseButton = new Button("\u2716  Erase Tool");
        eraseButton.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        eraseButton.setMaxWidth(Double.MAX_VALUE);
        eraseButton.setMinHeight(28);
        eraseButton.setStyle(eraseStyle(false));
        eraseButton.setOnAction(e -> selectEraseTool());
        wireDragSource(eraseButton, DRAG_ERASE);

        paletteGrid = new FlowPane(8, 8);
        paletteGrid.setAlignment(Pos.TOP_LEFT);
        paletteGrid.setPadding(new Insets(2));

        paletteScroll = new ScrollPane(paletteGrid);
        paletteScroll.setFitToWidth(true);
        paletteScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paletteScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        paletteScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        paletteScroll.getStylesheets().add(dungeonScrollStylesheet());
        VBox.setVgrow(paletteScroll, Priority.ALWAYS);

        VBox panel = new VBox(8, header, searchField, categoryStrip, eraseButton, paletteScroll, statusLabel);
        panel.setPadding(new Insets(PANEL_PAD));
        panel.setMinWidth(PANEL_WIDTH);
        panel.setPrefWidth(PANEL_WIDTH);
        panel.setMaxWidth(PANEL_WIDTH);
        panel.setStyle("-fx-background-color: linear-gradient(to right, "
                + "rgba(28,24,18,0.95), rgba(20,17,12,0.92)); "
                + "-fx-border-color: transparent #8b7940 transparent transparent; -fx-border-width: 0 2 0 0;");
        return panel;
    }

    private void selectCategory(String category) {
        activeCategory = category;
        // Picking a category exits search mode.
        if (searchField != null && !searchField.getText().isEmpty()) {
            searchField.clear(); // triggers listener -> rebuildPaletteCards()
        } else {
            rebuildPaletteCards();
        }
    }

    private void rebuildPaletteCards() {
        paletteGrid.getChildren().clear();
        assetCards.clear();

        List<EditorAsset> shown = new ArrayList<>();
        if (!searchQuery.isEmpty()) {
            // Search spans every category so the designer can find anything fast.
            for (List<EditorAsset> group : assetsByCategory.values()) {
                for (EditorAsset asset : group) {
                    if (asset.name.toLowerCase().contains(searchQuery)) {
                        shown.add(asset);
                    }
                }
            }
        } else if (activeCategory != null) {
            shown = assetsByCategory.getOrDefault(activeCategory, new ArrayList<>());
        }

        // Category chips are only "active" when not searching.
        for (java.util.Map.Entry<String, Button> e : categoryChips.entrySet()) {
            boolean on = searchQuery.isEmpty() && e.getKey().equals(activeCategory);
            e.getValue().setStyle(categoryChipStyle(on));
        }

        for (EditorAsset asset : shown) {
            paletteGrid.getChildren().add(createAssetCard(asset));
        }
        if (shown.isEmpty()) {
            Label empty = new Label("No assets match \"" + searchQuery + "\"");
            empty.setFont(Font.font("Monospaced", 10));
            empty.setTextFill(Color.web("#8a7c5c"));
            empty.setWrapText(true);
            empty.setPadding(new Insets(10, 4, 4, 4));
            paletteGrid.getChildren().add(empty);
        }
        refreshSelectionHighlight();
        if (paletteScroll != null) {
            paletteScroll.setVvalue(0);
        }
    }

    private Button createAssetCard(EditorAsset asset) {
        boolean isCharacter = asset.kind == AssetKind.HERO
                || asset.kind == AssetKind.KNIGHT
                || asset.kind == AssetKind.GUARD
                || asset.kind == AssetKind.SORCERER
                || asset.kind == AssetKind.ASSASSIN;
        String payload = (isCharacter ? DRAG_CHARACTER : DRAG_OBJECT) + asset.name;

        Canvas icon = new Canvas(CARD_ICON, CARD_ICON);
        renderer.drawEditorPaletteIcon(icon.getGraphicsContext2D(), CARD_ICON, asset.name);

        Label label = new Label(asset.name);
        label.setFont(Font.font("Monospaced", 9));
        label.setTextFill(Color.web("#cabb95"));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(CARD_SIZE - 10);

        VBox box = new VBox(3, icon, label);
        box.setAlignment(Pos.CENTER);

        Button card = new Button();
        card.setGraphic(box);
        card.setPrefSize(CARD_SIZE, CARD_SIZE + 8);
        card.setMinSize(CARD_SIZE, CARD_SIZE + 8);
        card.setStyle(cardStyle(false));

        assetCards.put(asset.name, card);
        wireDragSource(card, payload);
        card.setOnAction(e -> applyDragSelection(payload));
        return card;
    }

    // ---------- SELECTION ----------

    private void applyDragSelection(String payload) {
        if (payload.startsWith(DRAG_OBJECT)) {
            selectObjectTool(payload.substring(DRAG_OBJECT.length()));
        } else if (payload.startsWith(DRAG_CHARACTER)) {
            selectCharacterTool(payload.substring(DRAG_CHARACTER.length()));
        } else if (DRAG_ERASE.equals(payload)) {
            selectEraseTool();
        }
    }

    private void selectObjectTool(String name) {
        selectedObjectName = name;
        selectedCharacterName = null;
        mode = EditMode.PLACE_OBJECT;
        selectedToolId = name;
        refreshSelectionHighlight();
        setStatus("Placing: " + name + " — drag onto the map or click a tile.");
    }

    private void selectCharacterTool(String name) {
        selectedCharacterName = name;
        selectedObjectName = null;
        mode = EditMode.PLACE_CHARACTER;
        selectedToolId = name;
        refreshSelectionHighlight();
        setStatus("Placing: " + name + " — drag onto the map or click a tile.");
    }

    private void selectEraseTool() {
        mode = EditMode.ERASE;
        selectedObjectName = null;
        selectedCharacterName = null;
        selectedToolId = DRAG_ERASE;
        refreshSelectionHighlight();
        setStatus("Erase — drag onto the map or click a tile to remove.");
    }

    private void refreshSelectionHighlight() {
        for (java.util.Map.Entry<String, Button> e : assetCards.entrySet()) {
            e.getValue().setStyle(cardStyle(e.getKey().equals(selectedToolId)));
        }
        if (eraseButton != null) {
            eraseButton.setStyle(eraseStyle(DRAG_ERASE.equals(selectedToolId)));
        }
    }

    // ---------- CANVAS INPUT ----------

    private void wireCanvasInput() {
        // Click-and-drag painting: press, then drag the cursor to fill every cell it
        // passes over (gaps from fast drags are filled by interpolation).
        canvas.setOnMousePressed(e -> {
            lastPaintRow = -1;
            lastPaintCol = -1;
            paintAt(e);
        });
        canvas.setOnMouseDragged(this::paintAt);
        canvas.setOnMouseReleased(e -> {
            lastPaintRow = -1;
            lastPaintCol = -1;
        });
        canvas.setOnDragOver(this::handleDragOver);
        canvas.setOnDragEntered(this::handleDragOver);
        canvas.setOnDragExited(e -> {
            hoverRow = -1;
            hoverCol = -1;
            render();
            e.consume();
        });
        canvas.setOnDragDropped(this::handleDragDropped);
    }

    private void wireDragSource(Button button, String payload) {
        button.setOnDragDetected(e -> {
            Dragboard board = button.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(payload);
            board.setContent(content);
            applyDragSelection(payload);
            e.consume();
        });
    }

    private void handleDragOver(DragEvent e) {
        if (e.getDragboard().hasString()) {
            e.acceptTransferModes(TransferMode.COPY);
            hoverRow = tileRow(e.getY());
            hoverCol = tileCol(e.getX());
            render();
        }
        e.consume();
    }

    private void handleDragDropped(DragEvent e) {
        Dragboard board = e.getDragboard();
        boolean success = false;
        if (board.hasString()) {
            int row = tileRow(e.getY());
            int col = tileCol(e.getX());
            lastEditMessage = null;
            success = applyPayloadAt(board.getString(), row, col);
            setStatus(lastEditMessage != null
                    ? lastEditMessage
                    : success ? "Placed at (" + row + "," + col + ")."
                    : "Cannot place at (" + row + "," + col + ").");
            render();
        }
        e.setDropCompleted(success);
        hoverRow = -1;
        hoverCol = -1;
        e.consume();
    }

    private boolean applyPayloadAt(String payload, int row, int col) {
        if (DRAG_ERASE.equals(payload)) {
            return editor.removeObject(row, col, null);
        }
        if (payload.startsWith(DRAG_OBJECT)) {
            String name = payload.substring(DRAG_OBJECT.length());
            AssetKind kind = assetKinds.get(name);
            if (kind == AssetKind.PORTAL) {
                MapPortal portal = showPortalConfigDialog(name, row, col);
                return portal != null && editor.placeObject(row, col, portal);
            }
            GameObject obj = createObjectFor(name);
            if (obj instanceof Item && editor.addItemToContainer(row, col, (Item) obj)) {
                noteContainerLoot(row, col, (Item) obj);
                return true;
            }
            return obj != null && editor.placeObject(row, col, obj);
        }
        if (payload.startsWith(DRAG_CHARACTER)) {
            String name = payload.substring(DRAG_CHARACTER.length());
            Character character = createCharacterFor(name);
            return character != null && editor.placeCharacter(row, col, character);
        }
        return false;
    }

    /** Paints the current selection at the cursor, interpolating across skipped cells. */
    private void paintAt(MouseEvent e) {
        int row = tileRow(e.getY());
        int col = tileCol(e.getX());
        if (row == lastPaintRow && col == lastPaintCol) {
            return;
        }
        boolean changed;
        lastEditMessage = null;
        if (lastPaintRow >= 0 && (Math.abs(row - lastPaintRow) > 1 || Math.abs(col - lastPaintCol) > 1)) {
            changed = paintLine(lastPaintRow, lastPaintCol, row, col);
        } else {
            changed = applyAtCell(row, col);
        }
        lastPaintRow = row;
        lastPaintCol = col;
        setStatus(lastEditMessage != null
                ? lastEditMessage
                : changed ? "Updated cell (" + row + "," + col + ")."
                : "Cannot edit cell (" + row + "," + col + ").");
        render();
    }

    /** Applies the current tool to a straight line of cells (Bresenham) for smooth drags. */
    private boolean paintLine(int r0, int c0, int r1, int c1) {
        boolean any = false;
        int dr = Math.abs(r1 - r0), dc = Math.abs(c1 - c0);
        int sr = r0 < r1 ? 1 : -1, sc = c0 < c1 ? 1 : -1;
        int err = dc - dr;
        int r = r0, c = c0;
        while (true) {
            any |= applyAtCell(r, c);
            if (r == r1 && c == c1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dr) { err -= dr; c += sc; }
            if (e2 < dc) { err += dc; r += sr; }
        }
        return any;
    }

    /** Places/erases at a single cell according to the active mode. No render/status. */
    private boolean applyAtCell(int row, int col) {
        if (mode == EditMode.ROTATE) {
            GridCell cell = editor.getMap().getCell(row, col);
            if (cell == null) return false;
            StaticObject so = cell.getStaticObject();
            if (so != null) {
                so.setRotation(so.getRotation() + 90);
                return true;
            }
            for (GameObject obj : cell.getObjects()) {
                obj.setRotation(obj.getRotation() + 90);
                return true;
            }
            return false;
        }
        if (mode == EditMode.ERASE) {
            return editor.removeObject(row, col, null);
        }
        if (mode == EditMode.PLACE_OBJECT && selectedObjectName != null) {
            AssetKind kind = assetKinds.get(selectedObjectName);
            if (kind == AssetKind.PORTAL) {
                MapPortal portal = showPortalConfigDialog(selectedObjectName, row, col);
                return portal != null && editor.placeObject(row, col, portal);
            }
            GameObject obj = createObjectFor(selectedObjectName);
            if (obj instanceof Item && editor.addItemToContainer(row, col, (Item) obj)) {
                noteContainerLoot(row, col, (Item) obj);
                return true;
            }
            return obj != null && editor.placeObject(row, col, obj);
        }
        if (mode == EditMode.PLACE_CHARACTER && selectedCharacterName != null) {
            Character character = createCharacterFor(selectedCharacterName);
            return character != null && editor.placeCharacter(row, col, character);
        }
        return false;
    }

    // ---------- ASSET FACTORIES (kind -> model object) ----------

    private GameObject createObjectFor(String name) {
        AssetKind kind = assetKinds.get(name);
        if (kind == null) {
            return null;
        }
        GameObject obj;
        switch (kind) {
            case WEAPON:
                obj = WeaponCatalog.create(name);
                break;
            case ITEM:
                obj = createItemFor(name);
                break;
            case CONTAINER:
                obj = ResourceAssetCatalog.createContainer(name);
                break;
            case STATIC:
                obj = ResourceAssetCatalog.createStatic(name);
                break;
            case NPC:
                obj = new StaticObject(name, "A dungeon dweller", false, false);
                break;
            case TERRAIN_WALL:
                obj = ResourceAssetCatalog.createStatic(name);
                break;
            case TERRAIN_FLOOR:
                obj = ResourceAssetCatalog.createStatic(name);
                break;
            default:
                return null;
        }
        if (obj != null) {
            obj.setRotation(currentRotation);
        }
        return obj;
    }

    /** Builds the right model type for a catalog item from its real name. */
    private Item createItemFor(String name) {
        return ResourceAssetCatalog.createItem(name);
    }

    /**
     * Shows a modal dialog to configure a MapPortal.
     * Returns null if the user cancelled.
     */
    private MapPortal showPortalConfigDialog(String assetName, int row, int col) {
        boolean isStairs = assetName.toLowerCase().contains("stair");

        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Configure Portal");

        // Target map dropdown: existing map IDs + "(same map / door only)"
        List<String> mapChoices = new ArrayList<>();
        mapChoices.add("— door only (same map) —");
        mapChoices.addAll(editor.getWorldMaps().keySet());
        mapChoices.add("+ Create new map…");

        ComboBox<String> targetMapBox = new ComboBox<>();
        targetMapBox.getItems().addAll(mapChoices);
        targetMapBox.setValue(mapChoices.size() > 1 ? mapChoices.get(1) : mapChoices.get(0));
        targetMapBox.setMaxWidth(200);

        TextField spawnRowField = new TextField("0");
        spawnRowField.setPrefWidth(55);
        TextField spawnColField = new TextField("0");
        spawnColField.setPrefWidth(55);

        List<String> keyChoices = Arrays.asList("None", "Bone Key", "Silver Key", "Gold Key",
                "Copper Key", "Brass Key", "Key Ring");
        ComboBox<String> keyBox = new ComboBox<>();
        keyBox.getItems().addAll(keyChoices);
        keyBox.setValue("None");
        keyBox.setMaxWidth(150);

        CheckBox stairsCheck = new CheckBox("Stairs mode (search to use)");
        stairsCheck.setSelected(isStairs);
        stairsCheck.setTextFill(Color.web("#e8c84a"));

        Label title = new Label("Configure Portal / Door");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#e8c84a"));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color:#2a261e;");
        grid.add(label("Target Map:"), 0, 0); grid.add(targetMapBox, 1, 0);
        grid.add(label("Spawn Row:"), 0, 1);  grid.add(spawnRowField, 1, 1);
        grid.add(label("Spawn Col:"), 0, 2);  grid.add(spawnColField, 1, 2);
        grid.add(label("Req. Key:"), 0, 3);   grid.add(keyBox, 1, 3);
        grid.add(stairsCheck, 0, 4, 2, 1);

        Button ok = stoneButton("Place", 80);
        Button cancel = stoneButton("Cancel", 80);
        HBox buttons = new HBox(10, ok, cancel);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(8, 12, 12, 12));
        buttons.setStyle("-fx-background-color:#2a261e;");

        VBox root = new VBox(0,
                new javafx.scene.layout.StackPane(title) {{
                    setStyle("-fx-background-color:#1a1610;");
                    setPadding(new Insets(10, 12, 10, 12));
                }},
                grid, buttons);
        root.setStyle("-fx-border-color:#8b7940;-fx-border-width:2;");

        final MapPortal[] result = {null};
        ok.setOnAction(ev -> {
            String sel = targetMapBox.getValue();
            String targetId = null;
            if (sel != null && sel.startsWith("+ Create")) {
                String newId = editor.createNewMap("map_" + (editor.getWorldMaps().size()));
                pavePlainFloor(editor.getMap());
                // switch back to the original map for placing
                editor.switchToMap(editor.getWorldMaps().keySet().stream()
                        .filter(k -> !k.equals(newId)).findFirst().orElse(newId));
                targetId = newId;
            } else if (sel != null && !sel.startsWith("—")) {
                targetId = sel;
            }
            int tr = 0, tc = 0;
            try { tr = Integer.parseInt(spawnRowField.getText().trim()); } catch (Exception ignored) {}
            try { tc = Integer.parseInt(spawnColField.getText().trim()); } catch (Exception ignored) {}
            String keyChoice = keyBox.getValue();
            String keyId = "None".equals(keyChoice) ? null : keyChoiceToId(keyChoice);
            boolean stairs = stairsCheck.isSelected();
            result[0] = new MapPortal(assetName, assetName + " connection",
                    targetId, tr, tc, keyId, keyId == null, stairs);
            dialog.close();
        });
        cancel.setOnAction(ev -> dialog.close());

        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
        return result[0];
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#c8b870"));
        l.setFont(Font.font("Serif", 13));
        return l;
    }

    private String keyChoiceToId(String choice) {
        switch (choice) {
            case "Bone Key":   return "bone";
            case "Silver Key": return "silver";
            case "Gold Key":   return "gold";
            case "Copper Key": return "copper";
            case "Brass Key":  return "brass";
            case "Key Ring":   return "keyring";
            default:           return null;
        }
    }

    private void noteContainerLoot(int row, int col, Item item) {
        StaticContainer container = findContainerAt(row, col);
        int count = container == null ? 0 : container.getContents().size();
        lastEditMessage = "Added " + item.getName() + " to "
                + (container == null ? "container" : container.getName())
                + " (" + count + " item" + (count == 1 ? "" : "s") + ").";
    }

    private StaticContainer findContainerAt(int row, int col) {
        GridCell cell = editor.getMap().getCell(row, col);
        if (cell == null) {
            return null;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer) {
                return (StaticContainer) obj;
            }
        }
        return null;
    }

    private Character createCharacterFor(String name) {
        AssetKind kind = assetKinds.get(name);
        if (kind == null) {
            return null;
        }
        Character character;
        String displayName = name;
        switch (kind) {
            case HERO:
                character = new Hero(displayName);
                break;
            case KNIGHT:
                character = new Knight();
                character.setName(displayName);
                break;
            case GUARD:
                character = new Guard();
                character.setName(displayName);
                break;
            case SORCERER:
                character = new Sorcerer();
                character.setName(displayName);
                break;
            case ASSASSIN:
                character = new Assassin();
                character.setName(displayName);
                break;
            default:
                return null;
        }
        applyVariantStats(character, displayName);
        character.setRotation(currentRotation);
        return character;
    }

    private void applyVariantStats(Character character, String displayName) {
        String n = displayName == null ? "" : displayName.toLowerCase();
        if (n.contains("horned guard blue")) {
            character.setAttackPower(6);
        } else if (n.contains("horned guard brown")) {
            character.setAttackPower(4);
        } else if (n.contains("hero yellow")) {
            character.setAttackPower(8);
        } else if (n.contains("hero blonde")) {
            character.setAttackPower(6);
        } else if (n.contains("sorcerer red")) {
            character.setAttackPower(7);
        } else if (n.contains("sorcerer purple")) {
            character.setAttackPower(5);
        } else if (n.contains("assassin red")) {
            character.setAttackPower(7);
        } else if (n.contains("assassin blue")) {
            character.setAttackPower(5);
        }
    }

    private int tileRow(double y) {
        return Math.max(0, Math.min(editor.getMap().getHeight() - 1, (int) (y / TILE_SIZE)));
    }

    private int tileCol(double x) {
        return Math.max(0, Math.min(editor.getMap().getWidth() - 1, (int) (x / TILE_SIZE)));
    }

    // ---------- MAP / LOADING ----------

    void applyLoadedMap(Map map) {
        editor.setMap(map);
        int canvasW = map.getWidth() * TILE_SIZE;
        int canvasH = map.getHeight() * TILE_SIZE;
        canvas.setWidth(canvasW);
        canvas.setHeight(canvasH);

        double scale = computeEditorScale(canvasW, canvasH);
        canvas.getTransforms().setAll(new Scale(scale, scale));

        if (stage != null) {
            double sceneW = sceneWidth(canvasW, scale);
            double sceneH = sceneHeight(canvasH, scale);
            stage.setWidth(sceneW);
            stage.setHeight(sceneH);
            stage.centerOnScreen();
            if (bgCanvas != null) {
                bgCanvas.setWidth(sceneW);
                bgCanvas.setHeight(sceneH);
                drawDungeonBackground(bgCanvas, sceneW, sceneH);
            }
        }
        hoverRow = -1;
        hoverCol = -1;
        render();
    }

    private double computeEditorScale(int canvasW, int canvasH) {
        double maxCanvasW = MAX_WINDOW_W - PANEL_WIDTH - WINDOW_FRAME_PAD;
        double maxCanvasH = MAX_WINDOW_H - TOOLBAR_HEIGHT - WINDOW_FRAME_PAD;
        double scaleX = maxCanvasW / canvasW;
        double scaleY = maxCanvasH / canvasH;
        return Math.min(1.0, Math.min(scaleX, scaleY));
    }

    private double sceneWidth(int canvasW, double scale) {
        return canvasW * scale + PANEL_WIDTH + WINDOW_FRAME_PAD;
    }

    private double sceneHeight(int canvasH, double scale) {
        return canvasH * scale + TOOLBAR_HEIGHT + WINDOW_FRAME_PAD;
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    private static final String ROTATE_ACTIVE =
            "-fx-background-color:#c8a830;-fx-text-fill:#1a1610;"
            + "-fx-font-family:Serif;-fx-font-weight:bold;-fx-font-size:13;"
            + "-fx-background-radius:5;-fx-border-radius:5;"
            + "-fx-border-color:#8b6914;-fx-border-width:2;-fx-cursor:hand;"
            + "-fx-effect:dropshadow(gaussian,#ffd54f,10,0.7,0,0);";

    private void cycleRotation() {
        if (mode == EditMode.ROTATE) {
            mode = EditMode.PLACE_OBJECT;
            if (rotateButton != null) rotateButton.setStyle(stoneBase());
        } else {
            mode = EditMode.ROTATE;
            if (rotateButton != null) rotateButton.setStyle(ROTATE_ACTIVE);
        }
    }

    // ---------- RENDER ----------

    private void render() {
        Map map = editor.getMap();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.web(STONE_DARK));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                int sx = col * TILE_SIZE;
                int sy = row * TILE_SIZE;
                GridCell cell = map.getCell(row, col);
                drawTileBase(gc, cell.getStaticObject(), sx, sy);

                for (GameObject obj : cell.getObjects()) {
                    drawObject(gc, obj, sx, sy);
                }

                Character occupant = cell.getOccupyingCharacter();
                if (occupant != null) {
                    withRotation(gc, sx, sy, occupant.getRotation(), () -> {
                        if (occupant instanceof Hero) {
                            renderer.drawHeroByName(gc, sx, sy, occupant.getName());
                        } else if (occupant instanceof Knight) {
                            renderer.drawEnemy(gc, sx, sy, "Knight", false, occupant.getName());
                        } else if (occupant instanceof Guard) {
                            renderer.drawEnemy(gc, sx, sy, "Guard", false, occupant.getName());
                        } else if (occupant instanceof Sorcerer) {
                            renderer.drawEnemy(gc, sx, sy, "Sorcerer", false, occupant.getName());
                        } else if (occupant instanceof Assassin) {
                            renderer.drawEnemy(gc, sx, sy, "Assassin", false, occupant.getName());
                        }
                    });
                }
            }
        }

        if (hoverRow >= 0 && hoverCol >= 0) {
            gc.setStroke(Color.web(GOLD, 0.9));
            gc.setLineWidth(2);
            gc.setLineDashes(6, 4);
            gc.strokeRect(hoverCol * TILE_SIZE + 2, hoverRow * TILE_SIZE + 2,
                    TILE_SIZE - 4, TILE_SIZE - 4);
            gc.setLineDashes(null);
        }

        gc.setStroke(Color.web("#c8a840", 0.16));
        gc.setLineWidth(1);
        for (int r = 0; r <= map.getHeight(); r++) {
            gc.strokeLine(0, r * TILE_SIZE, map.getWidth() * TILE_SIZE, r * TILE_SIZE);
        }
        for (int c = 0; c <= map.getWidth(); c++) {
            gc.strokeLine(c * TILE_SIZE, 0, c * TILE_SIZE, map.getHeight() * TILE_SIZE);
        }
    }

    private void drawTileBase(GraphicsContext gc, StaticObject so, int sx, int sy) {
        if (so == null) {
            renderer.drawFloor(gc, sx, sy);
            return;
        }
        withRotation(gc, sx, sy, so.getRotation(), () -> {
            String name = so.getName();
            if (so instanceof MapPortal) {
                MapPortal portal = (MapPortal) so;
                renderer.drawFloor(gc, sx, sy);
                renderer.drawPortalOrNavSprite(gc, sx, sy, portal);
            } else if (renderer.isWallName(name) && !so.isSearchable()) {
                renderer.drawWallByName(gc, sx, sy, name);
            } else if (renderer.isFloorName(name)) {
                renderer.drawFloorByName(gc, sx, sy, name);
            } else if (so.isSearchable()) {
                renderer.drawFloor(gc, sx, sy);
                renderer.drawSearchable(gc, sx, sy, name);
            } else {
                renderer.drawFloor(gc, sx, sy);
                renderer.drawStaticDecor(gc, sx, sy, so);
            }
        });
    }

    private void drawObject(GraphicsContext gc, GameObject obj, int sx, int sy) {
        withRotation(gc, sx, sy, obj.getRotation(), () -> {
            if (obj instanceof StaticContainer) {
                StaticContainer container = (StaticContainer) obj;
                renderer.drawContainerByName(gc, sx, sy, container.getSpriteName());
                drawContainerLootBadge(gc, sx, sy, container);
            } else if (obj instanceof Item) {
                renderer.drawItemObject(gc, sx, sy, (Item) obj);
            }
        });
    }

    private void drawContainerLootBadge(GraphicsContext gc, int sx, int sy, StaticContainer container) {
        int count = container.getContents().size();
        if (count <= 0) {
            return;
        }
        String text = String.valueOf(count);
        gc.save();
        gc.setFill(Color.web("#17130c", 0.88));
        gc.fillRoundRect(sx + TILE_SIZE - 19, sy + 3, 16, 14, 5, 5);
        gc.setStroke(Color.web(GOLD_BRIGHT));
        gc.strokeRoundRect(sx + TILE_SIZE - 19, sy + 3, 16, 14, 5, 5);
        gc.setFill(Color.web(GOLD_BRIGHT));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        gc.fillText(text, sx + TILE_SIZE - 14, sy + 13);
        gc.restore();
    }

    private void withRotation(GraphicsContext gc, int sx, int sy, int rot, Runnable draw) {
        renderer.applyRotation(gc, sx, sy, rot, draw);
    }

    private void drawDungeonBackground(Canvas bg, double w, double h) {
        GraphicsContext gc = bg.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.web("#0a0a08"));
        gc.fillRect(0, 0, w, h);

        SpriteSheet walls = new SpriteSheet("/assets/walls_x3.png", BG_TILE);
        if (walls.isLoaded()) {
            Image[] wallTiles = {
                    walls.getSprite(0, 1), walls.getSprite(1, 1),
                    walls.getSprite(2, 1), walls.getSprite(3, 1), walls.getSprite(4, 1)
            };
            Image[] floorTiles = {
                    walls.getSprite(0, 3), walls.getSprite(1, 3),
                    walls.getSprite(2, 3), walls.getSprite(3, 3)
            };
            for (int y = 0; y <= h; y += BG_TILE) {
                for (int x = 0; x <= w; x += BG_TILE) {
                    int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                    boolean wall = (y < BG_TILE * 2) || hash % 6 == 0;
                    Image tile = wall
                            ? wallTiles[hash % wallTiles.length]
                            : floorTiles[hash % floorTiles.length];
                    if (tile != null) {
                        gc.drawImage(tile, x, y, BG_TILE, BG_TILE);
                    }
                }
            }
        }
        // Darken so the UI panels read clearly over the texture.
        gc.setFill(Color.web("#0a0908", 0.62));
        gc.fillRect(0, 0, w, h);
    }

    // ---------- STONE THEME STYLES ----------

    private Button stoneButton(String text, double width) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        btn.setPrefWidth(width);
        btn.setPrefHeight(32);
        btn.setMinHeight(32);
        btn.setStyle(stoneBase());
        btn.setOnMouseEntered(e -> btn.setStyle(stoneHover()));
        btn.setOnMouseExited(e -> btn.setStyle(stoneBase()));
        btn.setOnMousePressed(e -> btn.setStyle(stonePressed()));
        btn.setOnMouseReleased(e -> btn.setStyle(btn.isHover() ? stoneHover() : stoneBase()));
        return btn;
    }

    private String stoneBase() {
        return "-fx-background-color: linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16, #2a2720); "
                + "-fx-text-fill: " + GOLD + "; -fx-background-radius: 5; -fx-border-radius: 5; "
                + "-fx-border-color: #8b7940 #5a4e30 #3a3220 #8b7940; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);";
    }

    private String stoneHover() {
        return "-fx-background-color: linear-gradient(to bottom, #5a5448, #3e3a30, #2a2720, #353028); "
                + "-fx-text-fill: " + GOLD_BRIGHT + "; -fx-background-radius: 5; -fx-border-radius: 5; "
                + "-fx-border-color: #c8a850 #8b7940 #5a4e30 #c8a850; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(200,168,80,0.3), 10, 0, 0, 0);";
    }

    private String stonePressed() {
        return "-fx-background-color: linear-gradient(to bottom, #1e1b16, #252018, #2e2a22); "
                + "-fx-text-fill: #a89860; -fx-background-radius: 5; -fx-border-radius: 5; "
                + "-fx-border-color: #5a4e30 #8b7940 #8b7940 #5a4e30; -fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.8), 7, 0, 2, 2);";
    }

    private Button categoryChip(String label) {
        Button chip = new Button(label);
        chip.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        chip.setOnAction(e -> selectCategory(label));
        chip.setStyle(categoryChipStyle(false));
        return chip;
    }

    private String categoryChipStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: linear-gradient(to bottom, #5a5040, #3a3328); "
                    + "-fx-text-fill: " + GOLD_BRIGHT + "; -fx-border-color: #c8a850; -fx-border-width: 1.5; "
                    + "-fx-cursor: hand; -fx-padding: 3 9; -fx-background-radius: 4; -fx-border-radius: 4;";
        }
        return "-fx-background-color: linear-gradient(to bottom, #322c22, #201c14); "
                + "-fx-text-fill: #9a8e70; -fx-border-color: #5a4e30; -fx-border-width: 1; "
                + "-fx-cursor: hand; -fx-padding: 3 9; -fx-background-radius: 4; -fx-border-radius: 4;";
    }

    private String searchFieldStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #1a1610, #241f16); "
                + "-fx-text-fill: " + GOLD_BRIGHT + "; -fx-prompt-text-fill: #7a6e50; "
                + "-fx-border-color: #5a4e30; -fx-border-width: 1.5; "
                + "-fx-background-radius: 5; -fx-border-radius: 5; -fx-padding: 5 8; "
                + "-fx-highlight-fill: #8b7940; -fx-highlight-text-fill: #15120c; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.6), 5, 0, 0, 1);";
    }

    /** Scoped dungeon-styled scrollbar (dark stone track, aged-gold thumb). */
    private String dungeonScrollStylesheet() {
        String css = ".scroll-pane,.scroll-pane>.viewport{-fx-background-color:transparent;}"
                + ".scroll-pane>.corner{-fx-background-color:transparent;}"
                + ".scroll-bar:vertical{-fx-background-color:transparent;-fx-pref-width:12;-fx-padding:0;}"
                + ".scroll-bar:vertical>.track{-fx-background-color:rgba(20,17,12,0.85);"
                + "-fx-background-radius:6;-fx-border-color:#3a3220;-fx-border-radius:6;-fx-border-width:1;}"
                + ".scroll-bar:vertical>.track-background{-fx-background-color:transparent;}"
                + ".scroll-bar:vertical>.thumb{-fx-background-color:linear-gradient(to right,#8b7940,#5a4e2c);"
                + "-fx-background-radius:6;-fx-border-color:#c8a850;-fx-border-width:1;-fx-border-radius:6;}"
                + ".scroll-bar:vertical>.thumb:hover{-fx-background-color:linear-gradient(to right,#d4b860,#8b7940);}"
                + ".scroll-bar:vertical>.increment-button,.scroll-bar:vertical>.decrement-button{"
                + "-fx-background-color:transparent;-fx-padding:0;-fx-pref-height:0;}"
                + ".scroll-bar:vertical>.increment-arrow,.scroll-bar:vertical>.decrement-arrow{"
                + "-fx-shape:\"\";-fx-padding:0;}";
        return "data:text/css;base64,"
                + java.util.Base64.getEncoder().encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String cardStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: linear-gradient(to bottom, #3a3322, #221d12); "
                    + "-fx-border-color: #f0e0a0; -fx-border-width: 2; -fx-cursor: hand; -fx-padding: 4; "
                    + "-fx-background-radius: 5; -fx-border-radius: 5; "
                    + "-fx-effect: dropshadow(gaussian, rgba(200,168,80,0.45), 9, 0, 0, 0);";
        }
        return "-fx-background-color: linear-gradient(to bottom, #2a2519, #15120c); "
                + "-fx-border-color: #594d2f; -fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 4; "
                + "-fx-background-radius: 5; -fx-border-radius: 5; "
                + "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.55), 5, 0, 0, 2);";
    }

    private String eraseStyle(boolean selected) {
        String border = selected ? "#ff8866" : "#6a3a28";
        String bg = selected
                ? "linear-gradient(to bottom, #5a2a1c, #3a1a12)"
                : "linear-gradient(to bottom, #34221a, #1f1410)";
        return "-fx-background-color: " + bg + "; -fx-text-fill: #ecc0a0; "
                + "-fx-border-color: " + border + "; -fx-border-width: " + (selected ? 2 : 1.5) + "; "
                + "-fx-cursor: hand; -fx-padding: 3 6; -fx-background-radius: 4; -fx-border-radius: 4;";
    }
}
