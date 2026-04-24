package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;
import java.util.List;

public class MapEditor {

    private Map currentMap;
    private final ObjectPalette palette;
    private final PlacementValidator validator;
    private final RandomItemGenerator itemGenerator;
    private GameObject selectedObject;
    private Character selectedCharacter;

    public MapEditor(int width, int height) {
        this(new Map(width, height));
    }

    public MapEditor(Map map) {
        this.currentMap = map;
        this.palette = new ObjectPalette();
        this.validator = new PlacementValidator();
        this.itemGenerator = new RandomItemGenerator();
    }

    public boolean placeObject(GameObject obj, int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (!validator.isValidPlacement(obj, cell)) {
            return false;
        }

        if (obj instanceof StaticObject) {
            cell.setStaticObject((StaticObject) obj);
        } else {
            cell.addObject(obj);
        }

        return true;
    }

    public boolean placeCharacter(Character character, int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (!validator.isValidCharacterPlacement(character, cell)) {
            return false;
        }

        cell.setOccupyingCharacter(character);
        character.setPosition(cell.getRow(), cell.getCol());
        return true;
    }

    public boolean deleteObject(int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (cell == null) {
            return false;
        }

        if (cell.getStaticObject() != null) {
            cell.setStaticObject(null);
            return true;
        }

        if (cell.getOccupyingCharacter() != null) {
            cell.setOccupyingCharacter(null);
            return true;
        }

        List<GameObject> objects = cell.getObjects();
        if (objects.isEmpty()) {
            return false;
        }

        return cell.removeObject(objects.get(objects.size() - 1));
    }

    public boolean deleteObject(int x, int y, GameObject obj) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (cell == null || obj == null) {
            return false;
        }

        if (obj instanceof StaticObject && cell.getStaticObject() == obj) {
            cell.setStaticObject(null);
            return true;
        }

        return cell.removeObject(obj);
    }

    public boolean deleteCharacter(int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (cell == null || cell.getOccupyingCharacter() == null) {
            return false;
        }

        cell.setOccupyingCharacter(null);
        return true;
    }

    public void clearMap() {
        for (int x = 0; x < currentMap.getGridWidth(); x++) {
            for (int y = 0; y < currentMap.getGridHeight(); y++) {
                GridCell cell = currentMap.getCellAt(x, y);
                if (cell == null) {
                    continue;
                }

                cell.setStaticObject(null);
                cell.setOccupyingCharacter(null);
                cell.getObjects().clear();
            }
        }
    }

    public void generateRandomItems(int count) {
        itemGenerator.generateRandomItems(currentMap, count);
    }

    public Item generateRandomItem(int x, int y) {
        return itemGenerator.generateAndPlaceRandomItem(currentMap, x, y);
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
}
