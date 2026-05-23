package com.kurawler.ui;

import com.kurawler.editor.LevelEditor;
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
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class EditorView {
    private static final int TILE_SIZE = 48;
    private static final int SIDEBAR_WIDTH = 300;

    private final ScreenManager screenManager;
    private final LevelEditor editor;
    private final TileRenderer renderer;
    private Canvas canvas;
    private Label statusLabel;
    private String selectedObjectName;
    private String selectedCharacterName;
    private EditMode mode = EditMode.PLACE_OBJECT;

    private enum EditMode {
        PLACE_OBJECT,
        PLACE_CHARACTER,
        ERASE,
        RANDOM_ITEM
    }

    public EditorView(ScreenManager screenManager, GameSession session) {
        this.screenManager = screenManager;
        this.editor = new LevelEditor(session.getMap());
        this.renderer = new TileRenderer(TILE_SIZE);
    }

    public void show(Stage stage) {
        Map map = editor.getMap();
        canvas = new Canvas(map.getWidth() * TILE_SIZE, map.getHeight() * TILE_SIZE);
        canvas.setOnMouseClicked(e -> handleMapClick(
                (int) (e.getY() / TILE_SIZE),
                (int) (e.getX() / TILE_SIZE)));

        statusLabel = new Label("Select a tool, then click the map.");
        statusLabel.setFont(Font.font("Monospaced", 12));
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setWrapText(true);

        VBox sidebar = buildSidebar();
        BorderPane root = new BorderPane(canvas, null, sidebar, null, null);
        root.setStyle("-fx-background-color: #0f0f23;");

        stage.setTitle("Dungeon KUrawler - Level Editor");
        stage.setScene(new Scene(root, canvas.getWidth() + SIDEBAR_WIDTH, canvas.getHeight()));
        stage.setResizable(false);
        stage.show();
        render();
    }

    private VBox buildSidebar() {
        Label title = new Label("LEVEL EDITOR");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e0e0e0"));

        VBox tools = new VBox(8, title, statusLabel);
        tools.setPrefWidth(SIDEBAR_WIDTH);
        tools.setStyle("-fx-padding: 12; -fx-background-color: #1a1a2e;");

        Label objects = sectionLabel("Objects");
        tools.getChildren().add(objects);
        for (GameObject obj : editor.getPalette().getAvailableObjects()) {
            Button button = editorButton(obj.getName());
            button.setOnAction(e -> {
                selectedObjectName = obj.getName();
                selectedCharacterName = null;
                mode = EditMode.PLACE_OBJECT;
                statusLabel.setText("Placing object: " + selectedObjectName);
            });
            tools.getChildren().add(button);
        }

        Label characters = sectionLabel("Characters");
        tools.getChildren().add(characters);
        for (com.kurawler.model.character.Character character
                : editor.getPalette().getAvailableCharacters()) {
            Button button = editorButton(character.getName());
            button.setOnAction(e -> {
                selectedCharacterName = character.getName();
                selectedObjectName = null;
                mode = EditMode.PLACE_CHARACTER;
                statusLabel.setText("Placing character: " + selectedCharacterName);
            });
            tools.getChildren().add(button);
        }

        Button randomItem = editorButton("Random Item");
        randomItem.setOnAction(e -> {
            mode = EditMode.RANDOM_ITEM;
            selectedObjectName = null;
            selectedCharacterName = null;
            statusLabel.setText("Random item mode selected.");
        });

        Button erase = editorButton("Erase");
        erase.setOnAction(e -> {
            mode = EditMode.ERASE;
            selectedObjectName = null;
            selectedCharacterName = null;
            statusLabel.setText("Erase mode selected.");
        });

        Button save = editorButton("Save Map");
        save.setOnAction(e -> {
            boolean saved = screenManager.saveEditedMap(editor.getMap(), "map_save.json");
            statusLabel.setText(saved ? "Map saved to map_save.json" : "Save failed.");
        });

        Button play = editorButton("Play This Map");
        play.setOnAction(e -> screenManager.playEditedMap(editor.getMap()));

        Button back = editorButton("Back To Menu");
        back.setOnAction(e -> screenManager.showMainMenu());

        tools.getChildren().addAll(randomItem, erase, save, play, back);
        return tools;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#ffd54f"));
        return label;
    }

    private Button editorButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Monospaced", 12));
        button.setPrefWidth(SIDEBAR_WIDTH - 28);
        button.setStyle("-fx-background-color: #2a1a3e; -fx-text-fill: #e0e0e0; "
                + "-fx-border-color: #664466; -fx-cursor: hand;");
        return button;
    }

    private void handleMapClick(int row, int col) {
        boolean changed = false;
        if (mode == EditMode.ERASE) {
            changed = editor.removeObject(row, col, null);
        } else if (mode == EditMode.RANDOM_ITEM) {
            changed = editor.generateRandomItem(row, col) != null;
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

        gc.setStroke(Color.web("#ffffff", 0.08));
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
