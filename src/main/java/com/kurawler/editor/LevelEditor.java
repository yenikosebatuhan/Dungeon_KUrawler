package com.kurawler.editor;

import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;

/**
 * Level editor for placing/removing objects and generating random items.
 * Controller delegates editor operations here.
 */
public class LevelEditor {

    private final MapEditor mapEditor;

    public LevelEditor(Map map) {
        this.mapEditor = new MapEditor(map);
    }

    public void setMap(Map map) {
        mapEditor.setCurrentMap(map);
        mapEditor.clearUndoHistory();
    }

    public boolean placeObject(int row, int col, GameObject obj) {
        return mapEditor.placeObject(obj, col, row);
    }

    public boolean removeObject(int row, int col, GameObject obj) {
        if (obj == null) {
            return mapEditor.deleteObject(col, row);
        }
        return mapEditor.deleteObject(col, row, obj);
    }

    public boolean placeCharacter(int row, int col, Character character) {
        return mapEditor.placeCharacter(character, col, row);
    }

    /** Generate a random item and place it at the given cell. */
    public GameObject generateRandomItem(int row, int col) {
        return mapEditor.generateRandomItem(col, row);
    }

    /** PDF §4.2: five floor items, one hidden searchable item, container fill rolls. */
    public RandomItemGenerator.PopulateResult addRandomItemsPerSpec() {
        return mapEditor.populateRandomItemsPerSpec();
    }

    public void clearMap() {
        mapEditor.clearMap();
    }

    public boolean undo() {
        return mapEditor.undo();
    }

    public boolean canUndo() {
        return mapEditor.canUndo();
    }

    public ObjectPalette getPalette() {
        return mapEditor.getPalette();
    }

    public Map getMap() {
        return mapEditor.getCurrentMap();
    }
}
