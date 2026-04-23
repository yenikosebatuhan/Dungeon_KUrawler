package com.kurawler.editor;

import java.util.List;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;

public class MapEditor {

    private Map currentMap;
    private final ObjectPalette palette;
    private final PlacementValidator validator;
    private final RandomItemGenerator itemGenerator;
    private GameObject selectedObject;
    private Character selectedCharacter;

    public MapEditor(int width, int height) {
        this.currentMap = new Map(width, height);
        this.palette = new ObjectPalette();
        this.validator = new PlacementValidator();
        this.itemGenerator = new RandomItemGenerator();
    }

    public boolean placeObject(GameObject obj, int row, int col) {
        GridCell cell = currentMap.getCell(row, col);

        if (!validator.isValidPlacement(obj, cell)) {
            return false;
        }

        boolean placed = currentMap.placeObject(row, col, obj);
        if (placed && isBlockingStaticObject(obj)) {
            cell.setWalkable(false);
        }

        return placed;
    }

    public boolean placeCharacter(Character character, int row, int col) {
        GridCell cell = currentMap.getCell(row, col);

        if (!validator.isValidCharacterPlacement(character, cell)) {
            return false;
        }

        character.setPosition(row, col);
        return true;
    }

    public boolean deleteObject(int row, int col) {
        GridCell cell = currentMap.getCell(row, col);

        if (cell == null) {
            return false;
        }

        List<GameObject> objects = cell.getObjects();
        if (!objects.isEmpty()) {
            GameObject objectToRemove = findPreferredObjectToRemove(objects);
            boolean removed = currentMap.removeObject(row, col, objectToRemove);

            if (removed && isBlockingStaticObject(objectToRemove)) {
                cell.setWalkable(true);
            }

            return removed;
        }

        if (!cell.isWalkable()) {
            cell.setWalkable(true);
            return true;
        }

        return false;
    }

    public void clearMap() {
        for (int row = 0; row < currentMap.getHeight(); row++) {
            for (int col = 0; col < currentMap.getWidth(); col++) {
                GridCell cell = currentMap.getCell(row, col);
                if (cell == null) {
                    continue;
                }

                for (GameObject obj : cell.getObjects()) {
                    cell.removeObject(obj);
                }
                cell.setWalkable(true);
            }
        }
    }

    public void generateRandomItems(int count) {
        itemGenerator.generateRandomItems(currentMap, count);
    }

    public Map getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(Map map) {
        if (map != null) {
            this.currentMap = map;
        }
    }

    public ObjectPalette getPalette() {
        return palette;
    }

    public void setSelectedObject(GameObject obj) {
        this.selectedObject = obj;
        this.selectedCharacter = null;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedCharacter(Character character) {
        this.selectedCharacter = character;
        this.selectedObject = null;
    }

    public Character getSelectedCharacter() {
        return selectedCharacter;
    }

    private GameObject findPreferredObjectToRemove(List<GameObject> objects) {
        for (GameObject obj : objects) {
            if (obj instanceof StaticObject || obj instanceof StaticContainer) {
                return obj;
            }
        }
        return objects.get(objects.size() - 1);
    }

    private boolean isBlockingStaticObject(GameObject obj) {
        return obj instanceof StaticObject && "wall".equalsIgnoreCase(obj.getName());
    }
}
