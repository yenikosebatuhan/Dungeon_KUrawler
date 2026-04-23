package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import java.util.List;

/**
 * Core editor service for map operations.
 */
public class MapEditor {

    private Map currentMap;
    private final ObjectPalette palette;
    private final PlacementValidator validator;
    private final RandomItemGenerator itemGenerator;
    private GameObject selectedObject;

    public MapEditor(int width, int height) {
        this(new Map(width, height));
    }

    public MapEditor(Map map) {
        this.currentMap = map;
        this.palette = new ObjectPalette();
        this.validator = new PlacementValidator();
        this.itemGenerator = new RandomItemGenerator();
    }

    public boolean placeObject(GameObject obj, int row, int col) {
        GridCell cell = currentMap.getCell(row, col);
        if (!validator.isValidPlacement(obj, cell)) {
            return false;
        }

        cell.addObject(obj);
        updateWalkabilityAfterPlacement(obj, cell);
        return true;
    }

    public boolean deleteObject(int row, int col) {
        GridCell cell = currentMap.getCell(row, col);
        if (!validator.canDeleteObject(cell)) {
            return false;
        }

        List<GameObject> objects = cell.getObjects();
        GameObject removed = objects.get(objects.size() - 1);
        boolean deleted = cell.removeObject(removed);
        if (deleted) {
            syncWalkability(cell);
        }
        return deleted;
    }

    public boolean deleteObject(int row, int col, GameObject obj) {
        GridCell cell = currentMap.getCell(row, col);
        if (cell == null || obj == null) {
            return false;
        }

        boolean deleted = cell.removeObject(obj);
        if (deleted) {
            syncWalkability(cell);
        }
        return deleted;
    }

    public void clearMap() {
        for (int row = 0; row < currentMap.getHeight(); row++) {
            for (int col = 0; col < currentMap.getWidth(); col++) {
                GridCell cell = currentMap.getCell(row, col);
                boolean hadObjects = cell.hasObjects();
                for (GameObject obj : cell.getObjects()) {
                    cell.removeObject(obj);
                }
                if (hadObjects) {
                    syncWalkability(cell);
                }
            }
        }
    }

    public void generateRandomItems(int count) {
        itemGenerator.generateRandomItems(currentMap, count);
    }

    public Item generateRandomItem(int row, int col) {
        return itemGenerator.generateAndPlaceRandomItem(currentMap, row, col);
    }

    public Map getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(Map map) {
        this.currentMap = map;
    }

    public ObjectPalette getPalette() {
        return palette;
    }

    public void setSelectedObject(GameObject obj) {
        this.selectedObject = obj;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    private void updateWalkabilityAfterPlacement(GameObject obj, GridCell cell) {
        if (validator.blocksMovement(obj)) {
            cell.setWalkable(false);
        }
    }

    private void syncWalkability(GridCell cell) {
        cell.setWalkable(!validator.containsBlockingObject(cell));
    }
}
