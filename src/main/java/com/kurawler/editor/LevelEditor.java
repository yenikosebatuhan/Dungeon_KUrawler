package com.kurawler.editor;

import com.kurawler.model.*;
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

    /** Generate a random item and place it at the given cell. */
    public Item generateRandomItem(int row, int col) {
        return mapEditor.generateRandomItem(col, row);
    }

    public void clearMap() {
        mapEditor.clearMap();
    }

    public ObjectPalette getPalette() {
        return mapEditor.getPalette();
    }

    public Map getMap() {
        return mapEditor.getCurrentMap();
    }
}
