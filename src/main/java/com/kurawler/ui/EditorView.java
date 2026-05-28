package com.kurawler.ui;

import com.kurawler.editor.LevelEditor;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.transform.Scale;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.HashMap;

/**
 * Level editor — PDF §4.2: drag palette → map, save/load JSON maps.
 */
public class EditorView {

    private static final int TILE_SIZE = 48;
    private static final int PALETTE_VIEWPORT_HEIGHT = 78;
    private static final int PALETTE_SCROLL_BTN_W = 26;
    private static final int PALETTE_SCROLL_STEP = 28;
    private static final int PALETTE_TOTAL_HEIGHT = 108;
    /** Sidebar sits flush against the map; extra scene width avoids macOS clipping. */
    private static final int SIDEBAR_WIDTH = 188;
    private static final int SIDEBAR_PAD = 4;
    private static final int SIDEBAR_BTN_WIDTH = SIDEBAR_WIDTH - SIDEBAR_PAD * 2;
    private static final int WINDOW_FRAME_PAD = 16;

    private static final String DRAG_OBJECT = "EDITOR_OBJ:";
    private static final String DRAG_CHARACTER = "EDITOR_CHAR:";
    private static final String DRAG_ERASE = "EDITOR_ERASE";

    private final ScreenManager screenManager;
    private final LevelEditor editor;
    private final TileRenderer renderer;

    private Stage stage;
    private HBox root;
    private VBox paletteBar;
    private ScrollPane paletteScroll;
    private ScrollPane sidebarScroll;
    private VBox mapColumn;
    private Canvas canvas;
    private Group canvasGroup;
    private Label statusLabel;
    private String selectedObjectName;
    private String selectedCharacterName;
    private EditMode mode = EditMode.PLACE_OBJECT;
    private int hoverRow = -1;
    private int hoverCol = -1;
    private String highlightedToolId;
    /** Highlight only top palette chips — not the sidebar text list. */
    private final java.util.Map<String, Button> paletteToolButtons = new HashMap<>();

    private enum EditMode {
        PLACE_OBJECT,
        PLACE_CHARACTER,
        ERASE
    }

    public EditorView(ScreenManager screenManager, GameSession session) {
        this.screenManager = screenManager;
        this.editor = new LevelEditor(session.getMap());
        this.renderer = new TileRenderer(TILE_SIZE);
    }



    public void show(Stage stage) {
        this.stage = stage;
        Map map = editor.getMap();
        int canvasW = map.getWidth() * TILE_SIZE;
        int canvasH = map.getHeight() * TILE_SIZE;

        canvas = new Canvas(canvasW, canvasH);
        wireCanvasInput();

        statusLabel = new Label("Drag from the palette above, or select a tool and click the map.");
        statusLabel.setFont(Font.font("Monospaced", 10));
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(SIDEBAR_BTN_WIDTH);
        statusLabel.setAlignment(Pos.TOP_LEFT);

        double scale = computeEditorScale(canvasW, canvasH);
        canvas.getTransforms().setAll(new Scale(scale, scale));
        canvasGroup = new Group(canvas);

        double scaledMapW = canvasW * scale;
        double scaledMapH = canvasH * scale;

        paletteBar = buildPaletteBar(scaledMapW);

        mapColumn = new VBox(0, paletteBar, canvasGroup);
        mapColumn.setAlignment(Pos.TOP_LEFT);
        mapColumn.setFillWidth(false);
        applyMapColumnWidth(scaledMapW);

        VBox sidebar = buildSidebar();
        constrainSidebar(sidebar);

        root = new HBox(0, mapColumn, sidebar);
        root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: #0f0f23;");
        HBox.setHgrow(mapColumn, Priority.NEVER);
        HBox.setHgrow(sidebar, Priority.NEVER);

        stage.setTitle("Dungeon KUrawler - Level Editor");
        stage.setScene(new Scene(root, sceneWidth(scaledMapW), sceneHeight(scaledMapH)));
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();

        String pending = screenManager.consumeEditorStatusMessage();
        if (pending != null) {
            statusLabel.setText(pending);
        }
        render();
    }

    private double computeEditorScale(int canvasW, int canvasH) {
        double maxCanvasW = 1280.0 - SIDEBAR_WIDTH - WINDOW_FRAME_PAD;
        double maxCanvasH = 720.0 - PALETTE_TOTAL_HEIGHT - WINDOW_FRAME_PAD;
        double scaleX = maxCanvasW / canvasW;
        double scaleY = maxCanvasH / canvasH;
        return Math.min(1.0, Math.min(scaleX, scaleY));
    }

    private void applyMapColumnWidth(double width) {
        mapColumn.setMinWidth(width);
        mapColumn.setPrefWidth(width);
        mapColumn.setMaxWidth(width);
    }


    /** PDF §4.2: palette top-left; wrapped rows scroll via themed controls on the right. */
    private VBox buildPaletteBar(double mapPixelW) {
        Label hint = new Label("PALETTE  (drag onto map)");
        hint.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        hint.setTextFill(Color.web("#ffd54f"));
        hint.setMaxWidth(mapPixelW - 16);
        hint.setAlignment(Pos.CENTER_LEFT);

        double chipsWidth = mapPixelW - PALETTE_SCROLL_BTN_W - 20;
        FlowPane chips = buildPaletteChips(chipsWidth);

        paletteScroll = new ScrollPane(chips);
        paletteScroll.setFitToWidth(true);
        paletteScroll.setPrefViewportWidth(chipsWidth);
        paletteScroll.setMinViewportWidth(chipsWidth);
        paletteScroll.setPrefViewportHeight(PALETTE_VIEWPORT_HEIGHT);
        paletteScroll.setMaxHeight(PALETTE_VIEWPORT_HEIGHT);
        paletteScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paletteScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paletteScroll.setPannable(true);
        paletteScroll.setVvalue(0);
        paletteScroll.setStyle(paletteScrollStyle());
        HBox.setHgrow(paletteScroll, Priority.ALWAYS);

        Button scrollUp = paletteScrollButton("\u25B2");
        Button scrollDown = paletteScrollButton("\u25BC");
        scrollUp.setOnAction(e -> nudgePaletteScroll(-1));
        scrollDown.setOnAction(e -> nudgePaletteScroll(1));

        Label scrollHint = new Label("Scroll");
        scrollHint.setFont(Font.font("Monospaced", 8));
        scrollHint.setTextFill(Color.web("#8877aa"));

        VBox scrollTools = new VBox(3, scrollUp, scrollHint, scrollDown);
        scrollTools.setAlignment(Pos.CENTER);
        scrollTools.setMinWidth(PALETTE_SCROLL_BTN_W);
        scrollTools.setPrefWidth(PALETTE_SCROLL_BTN_W);
        scrollTools.setPadding(new Insets(2, 2, 2, 0));
        scrollTools.setStyle("-fx-background-color: #221a32; -fx-border-color: #664466; "
                + "-fx-border-width: 0 0 0 1;");

        HBox chipsRow = new HBox(4, paletteScroll, scrollTools);
        chipsRow.setAlignment(Pos.TOP_LEFT);
        chipsRow.setMinWidth(mapPixelW - 16);
        chipsRow.setPrefWidth(mapPixelW - 16);
        chipsRow.setMaxWidth(mapPixelW - 16);

        VBox bar = new VBox(4, hint, chipsRow);
        bar.setAlignment(Pos.TOP_LEFT);
        bar.setMinWidth(mapPixelW);
        bar.setPrefWidth(mapPixelW);
        bar.setMaxWidth(mapPixelW);
        bar.setMinHeight(PALETTE_TOTAL_HEIGHT);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #664466; "
                + "-fx-border-width: 0 0 1 0;");
        return bar;
    }

    private FlowPane buildPaletteChips(double wrapWidth) {
        FlowPane chips = new FlowPane(6, 6);
        chips.setPrefWrapLength(wrapWidth);
        chips.setAlignment(Pos.TOP_LEFT);
        chips.setColumnHalignment(javafx.geometry.HPos.LEFT);

        for (GameObject obj : editor.getPalette().getAvailableObjects()) {
            chips.getChildren().add(createToolButton(obj.getName(), DRAG_OBJECT + obj.getName(), true));
        }
        chips.getChildren().add(sectionChip("|"));
        for (com.kurawler.model.character.Character character
                : editor.getPalette().getAvailableCharacters()) {
            chips.getChildren().add(createToolButton(
                    character.getName(), DRAG_CHARACTER + character.getName(), true));
        }
        chips.getChildren().add(createToolButton("Erase", DRAG_ERASE, true));
        return chips;
    }

    private void nudgePaletteScroll(int direction) {
        if (paletteScroll == null) {
            return;
        }
        double contentH = paletteScroll.getContent().getBoundsInLocal().getHeight();
        double viewH = paletteScroll.getViewportBounds().getHeight();
        if (contentH <= viewH + 1) {
            return;
        }
        double range = contentH - viewH;
        double delta = (PALETTE_SCROLL_STEP / range) * direction;
        double next = Math.max(0, Math.min(1, paletteScroll.getVvalue() + delta));
        paletteScroll.setVvalue(next);
    }

    private void nudgeSidebarScroll(int direction) {
        if (sidebarScroll == null) {
            return;
        }
        double contentH = sidebarScroll.getContent().getBoundsInLocal().getHeight();
        double viewH = sidebarScroll.getViewportBounds().getHeight();
        if (contentH <= viewH + 1) {
            return;
        }
        double range = contentH - viewH;
        double delta = (PALETTE_SCROLL_STEP / range) * direction;
        double next = Math.max(0, Math.min(1, sidebarScroll.getVvalue() + delta));
        sidebarScroll.setVvalue(next);
    }

    private Button paletteScrollButton(String glyph) {
        Button button = new Button(glyph);
        button.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
        button.setPrefSize(PALETTE_SCROLL_BTN_W, 28);
        button.setMinSize(PALETTE_SCROLL_BTN_W, 28);
        button.setStyle(compactChipStyle("#2a1a3e"));
        button.setOnMouseEntered(e -> button.setStyle(compactChipStyle("#3d2a5c")));
        button.setOnMouseExited(e -> button.setStyle(compactChipStyle("#2a1a3e")));
        return button;
    }


    private String paletteScrollStyle() {
        return "-fx-background-color: #1a1a2e; -fx-background: #1a1a2e; "
                + "-fx-border-color: #443355; -fx-border-width: 1; -fx-padding: 2;";
    }

    private VBox buildSidebar() {
        Label title = new Label("LEVEL EDITOR");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#e0e0e0"));

        Button randomItems = editorButton("Add Random Items");
        randomItems.setOnAction(e -> {
            com.kurawler.editor.RandomItemGenerator.PopulateResult result =
                    editor.addRandomItemsPerSpec();
            statusLabel.setText(String.format(
                    "Added %d floor items, %d container loot, hidden: %s (PDF §4.2).",
                    result.floorItemsPlaced,
                    result.containerLootAdded,
                    result.hiddenItemPlaced ? "yes" : "no"));
            render();
        });

        Button clearMap = editorButton("Clear");
        clearMap.setOnAction(e -> {
            editor.clearMap();
            statusLabel.setText("Map cleared.");
            render();
        });

        Button undo = editorButton("Undo");
        undo.setOnAction(e -> {
            if (editor.undo()) {
                statusLabel.setText("Undo applied.");
                render();
            } else {
                statusLabel.setText("Nothing to undo.");
            }
        });

        Button load = editorButton("Load");
        load.setOnAction(e -> {
            Map loaded = screenManager.showLoadMapDialog();
            if (loaded != null) {
                applyLoadedMap(loaded);
                statusLabel.setText("Loaded map " + loaded.getWidth() + "×" + loaded.getHeight() + ".");
            }
        });

        Button save = editorButton("Save");
        save.setOnAction(e -> {
            boolean saved = screenManager.showSaveMapDialog(editor.getMap());
            statusLabel.setText(saved ? "Map saved under maps/" : "Save cancelled or failed.");
        });

        Button play = editorButton("Play Map");
        play.setOnAction(e -> screenManager.playEditedMap(editor.getMap()));

        Button back = editorButton("Back");
        back.setOnAction(e -> screenManager.showMainMenu());

        VBox actions = new VBox(4,
                sectionLabel("Actions"),
                randomItems, clearMap, undo, load, save, play, back);
        actions.setAlignment(Pos.TOP_LEFT);
        actions.setFillWidth(false);

        VBox paletteList = new VBox(4);
        paletteList.setAlignment(Pos.TOP_LEFT);
        paletteList.getChildren().add(sectionLabel("Objects"));
        for (GameObject obj : editor.getPalette().getAvailableObjects()) {
            paletteList.getChildren().add(sidebarListButton(
                    obj.getName(), DRAG_OBJECT + obj.getName()));
        }
        paletteList.getChildren().add(sectionLabel("Characters"));
        for (com.kurawler.model.character.Character character
                : editor.getPalette().getAvailableCharacters()) {
            paletteList.getChildren().add(sidebarListButton(
                    character.getName(), DRAG_CHARACTER + character.getName()));
        }
        paletteList.getChildren().add(sidebarListButton("Erase", DRAG_ERASE));

        sidebarScroll = new ScrollPane(paletteList);
        sidebarScroll.setFitToWidth(true);
        double listWidth = SIDEBAR_BTN_WIDTH - PALETTE_SCROLL_BTN_W - 4;
        sidebarScroll.setPrefViewportWidth(listWidth);
        sidebarScroll.setMinViewportWidth(listWidth);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setPannable(true);
        sidebarScroll.setStyle(paletteScrollStyle());
        HBox.setHgrow(sidebarScroll, Priority.ALWAYS);

        Button listScrollUp = paletteScrollButton("\u25B2");
        Button listScrollDown = paletteScrollButton("\u25BC");
        listScrollUp.setOnAction(e -> nudgeSidebarScroll(-1));
        listScrollDown.setOnAction(e -> nudgeSidebarScroll(1));

        VBox listScrollTools = new VBox(4, listScrollUp, listScrollDown);
        listScrollTools.setAlignment(Pos.CENTER);
        listScrollTools.setMinWidth(PALETTE_SCROLL_BTN_W);
        listScrollTools.setPrefWidth(PALETTE_SCROLL_BTN_W);
        listScrollTools.setPadding(new Insets(2, 0, 2, 0));
        listScrollTools.setStyle("-fx-background-color: #221a32; -fx-border-color: #664466; "
                + "-fx-border-width: 1 0 0 0;");

        HBox objectListRow = new HBox(2, sidebarScroll, listScrollTools);
        objectListRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(objectListRow, Priority.ALWAYS);

        VBox tools = new VBox(6, title, statusLabel, actions, objectListRow);
        tools.setAlignment(Pos.TOP_LEFT);
        constrainSidebar(tools);
        tools.setPadding(new Insets(SIDEBAR_PAD));
        tools.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #664466; "
                + "-fx-border-width: 0 0 0 1;");
        title.setMaxWidth(SIDEBAR_BTN_WIDTH);
        return tools;
    }

    private void constrainSidebar(VBox sidebar) {
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);
        sidebar.setFillWidth(true);
    }

    private double sceneWidth(double mapPixelW) {
        return mapPixelW + SIDEBAR_WIDTH + WINDOW_FRAME_PAD;
    }

    private double sceneHeight(double mapPixelH) {
        return mapPixelH + PALETTE_TOTAL_HEIGHT + WINDOW_FRAME_PAD;
    }

    private Button sidebarListButton(String label, String payload) {
        Button button = editorButton(label);
        wireDragSource(button, payload);
        button.setOnAction(e -> applyToolSelection(label, payload));
        return button;
    }

    private void wireCanvasInput() {
        canvas.setOnMouseClicked(this::handleMapClick);
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

    private void applyDragSelection(String payload) {
        if (payload.startsWith(DRAG_OBJECT)) {
            selectObjectTool(payload.substring(DRAG_OBJECT.length()));
        } else if (payload.startsWith(DRAG_CHARACTER)) {
            selectCharacterTool(payload.substring(DRAG_CHARACTER.length()));
        } else if (DRAG_ERASE.equals(payload)) {
            selectEraseTool();
        }
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
            success = applyPayloadAt(board.getString(), row, col);
            statusLabel.setText(success
                    ? "Placed at (" + row + "," + col + ")."
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
            GameObject obj = editor.getPalette().getObjectByName(name);
            return obj != null && editor.placeObject(row, col, obj);
        }
        if (payload.startsWith(DRAG_CHARACTER)) {
            String name = payload.substring(DRAG_CHARACTER.length());
            com.kurawler.model.character.Character character =
                    editor.getPalette().getCharacterByName(name);
            return character != null && editor.placeCharacter(row, col, character);
        }
        return false;
    }

    private void applyLoadedMap(Map map) {
        editor.setMap(map);
        int canvasW = map.getWidth() * TILE_SIZE;
        int canvasH = map.getHeight() * TILE_SIZE;
        canvas.setWidth(canvasW);
        canvas.setHeight(canvasH);

        double scale = computeEditorScale(canvasW, canvasH);
        canvas.getTransforms().setAll(new Scale(scale, scale));

        double scaledMapW = canvasW * scale;
        double scaledMapH = canvasH * scale;

        if (stage != null && mapColumn != null) {
            VBox newPaletteBar = buildPaletteBar(scaledMapW);
            mapColumn.getChildren().set(0, newPaletteBar);
            paletteBar = newPaletteBar;
            applyMapColumnWidth(scaledMapW);
            stage.setWidth(sceneWidth(scaledMapW));
            stage.setHeight(sceneHeight(scaledMapH));
        }
        hoverRow = -1;
        hoverCol = -1;
        render();
    }

    private Button createToolButton(String label, String payload, boolean compact) {
        Button button = paletteChip(label);
        button.setContentDisplay(ContentDisplay.TOP);

        Canvas icon = new Canvas(36, 36);
        renderer.drawEditorPaletteIcon(icon.getGraphicsContext2D(), 36, label);

        Label nameLbl = new Label(shortPaletteLabel(label));
        nameLbl.setFont(Font.font("Monospaced", 9));
        nameLbl.setTextFill(Color.web("#c8b880"));

        VBox graphic = new VBox(4, icon, nameLbl);
        graphic.setAlignment(Pos.CENTER);
        button.setGraphic(graphic);

        paletteToolButtons.put(label, button);
        wireDragSource(button, payload);
        button.setOnAction(e -> applyToolSelection(label, payload));
        return button;
    }

    private String shortPaletteLabel(String name) {
        if (name.length() <= 10) {
            return name;
        }
        return name.substring(0, 9) + "…";
    }

    private void applyToolSelection(String label, String payload) {
        applyDragSelection(payload);
        refreshPaletteHighlight(label);
    }

    private void refreshPaletteHighlight(String toolId) {
        highlightedToolId = toolId;
        for (java.util.Map.Entry<String, Button> entry : paletteToolButtons.entrySet()) {
            boolean selected = toolId != null && toolId.equals(entry.getKey());
            String bg = "Erase".equals(entry.getKey()) ? "#4a2020" : "#2a1a3e";
            entry.getValue().setStyle(selected ? selectedChipStyle(bg) : chipStyle(bg));
        }
    }

    private void selectObjectTool(String name) {
        selectedObjectName = name;
        selectedCharacterName = null;
        mode = EditMode.PLACE_OBJECT;
        refreshPaletteHighlight(name);
        statusLabel.setText("Placing: " + name + " — drag onto map or click a tile");
    }

    private void selectCharacterTool(String name) {
        selectedCharacterName = name;
        selectedObjectName = null;
        mode = EditMode.PLACE_CHARACTER;
        refreshPaletteHighlight(name);
        statusLabel.setText("Placing: " + name + " — drag onto map or click a tile");
    }

    private void selectEraseTool() {
        mode = EditMode.ERASE;
        selectedObjectName = null;
        selectedCharacterName = null;
        refreshPaletteHighlight("Erase");
        statusLabel.setText("Erase — drag onto map or click a tile");
    }

    private void handleMapClick(MouseEvent e) {
        int row = tileRow(e.getY());
        int col = tileCol(e.getX());
        boolean changed = false;
        if (mode == EditMode.ERASE) {
            changed = editor.removeObject(row, col, null);
        } else if (mode == EditMode.PLACE_OBJECT && selectedObjectName != null) {
            GameObject obj = editor.getPalette().getObjectByName(selectedObjectName);
            changed = editor.placeObject(row, col, obj);
        } else if (mode == EditMode.PLACE_CHARACTER && selectedCharacterName != null) {
            com.kurawler.model.character.Character character =
                    editor.getPalette().getCharacterByName(selectedCharacterName);
            changed = editor.placeCharacter(row, col, character);
        }
        statusLabel.setText(changed
                ? "Updated cell (" + row + "," + col + ")."
                : "Cannot edit cell (" + row + "," + col + ").");
        render();
    }

    private int tileRow(double y) {
        return Math.max(0, Math.min(editor.getMap().getHeight() - 1, (int) (y / TILE_SIZE)));
    }

    private int tileCol(double x) {
        return Math.max(0, Math.min(editor.getMap().getWidth() - 1, (int) (x / TILE_SIZE)));
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        label.setTextFill(Color.web("#ffd54f"));
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    private Button editorButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Monospaced", 10));
        button.setMinWidth(SIDEBAR_BTN_WIDTH);
        button.setPrefWidth(SIDEBAR_BTN_WIDTH);
        button.setMaxWidth(SIDEBAR_BTN_WIDTH);
        button.setMinHeight(24);
        button.setPrefHeight(24);
        button.setMaxHeight(24);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setStyle(compactChipStyle("#2a1a3e"));
        return button;
    }

    private Button paletteChip(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Monospaced", 10));
        button.setMinWidth(Region.USE_COMPUTED_SIZE);
        button.setStyle(chipStyle("#2a1a3e"));
        return button;
    }

    private Label sectionChip(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#666"));
        return label;
    }

    private String chipStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #664466; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 4 8;";
    }

    private String compactChipStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #664466; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-padding: 3 6; -fx-alignment: center-left;";
    }

    private String selectedChipStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: #ffd54f; "
                + "-fx-border-color: #ffd54f; -fx-border-width: 2; -fx-cursor: hand; -fx-padding: 3 6;";
    }

    private void render() {
        Map map = editor.getMap();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0f0f23"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                int sx = col * TILE_SIZE;
                int sy = row * TILE_SIZE;
                GridCell cell = map.getCell(row, col);
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
                    drawObject(gc, obj, sx, sy);
                }

                com.kurawler.model.character.Character occupant = cell.getOccupyingCharacter();
                if (occupant instanceof Hero) {
                    renderer.drawHero(gc, sx, sy);
                } else if (occupant instanceof Knight) {
                    renderer.drawEnemy(gc, sx, sy, "Knight");
                } else if (occupant instanceof Sorcerer) {
                    renderer.drawEnemy(gc, sx, sy, "Sorcerer");
                }
            }
        }

        if (hoverRow >= 0 && hoverCol >= 0) {
            gc.setStroke(Color.web("#ffd54f", 0.85));
            gc.setLineWidth(2);
            gc.setLineDashes(6, 4);
            gc.strokeRect(hoverCol * TILE_SIZE + 2, hoverRow * TILE_SIZE + 2,
                    TILE_SIZE - 4, TILE_SIZE - 4);
            gc.setLineDashes(null);
        }

        gc.setStroke(Color.web("#c8a840", 0.22));
        gc.setLineWidth(1);
        for (int r = 0; r <= map.getHeight(); r++) {
            gc.strokeLine(0, r * TILE_SIZE, map.getWidth() * TILE_SIZE, r * TILE_SIZE);
        }
        for (int c = 0; c <= map.getWidth(); c++) {
            gc.strokeLine(c * TILE_SIZE, 0, c * TILE_SIZE, map.getHeight() * TILE_SIZE);
        }
    }

    private void drawObject(GraphicsContext gc, GameObject obj, int sx, int sy) {
        if (obj instanceof StaticContainer) {
            renderer.drawContainerByName(gc, sx, sy, obj.getName());
        } else if (obj instanceof Weapon) {
            renderer.drawWeaponByName(gc, sx, sy, obj.getName());
        } else if (obj instanceof Scroll) {
            renderer.drawItem(gc, sx, sy, "Scroll");
        } else if (obj instanceof Wearable) {
            renderer.drawItem(gc, sx, sy, "Wearable");
        } else if (obj instanceof Consumable) {
            Consumable c = (Consumable) obj;
            String pt = c.getManaAmount() > 0 ? "ManaPotion"
                    : c.getEnergyAmount() > 0 ? "EnergyPotion"
                    : "HealthPotion";
            renderer.drawConsumable(gc, sx, sy, pt);
        } else if (obj instanceof Key) {
            renderer.drawItem(gc, sx, sy, "Key");
        } else if (obj instanceof Item) {
            renderer.drawItem(gc, sx, sy, "Item");
        }
    }
}
