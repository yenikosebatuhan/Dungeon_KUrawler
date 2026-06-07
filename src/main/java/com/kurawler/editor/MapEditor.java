package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import java.util.List;

public class MapEditor {

    private Map currentMap;
    private final ObjectPalette palette;
    private final PlacementValidator validator;
    private final RandomItemGenerator itemGenerator;
    private final EditorUndoStack undoStack = new EditorUndoStack();
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

    public void pushUndoSnapshot() {
        undoStack.push(currentMap);
    }

    public boolean undo() {
        Map restored = undoStack.popRestore();
        if (restored == null) {
            return false;
        }
        currentMap = restored;
        return true;
    }

    public boolean canUndo() {
        return undoStack.canUndo();
    }

    public void clearUndoHistory() {
        undoStack.clear();
    }

    public boolean placeObject(GameObject obj, int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (!validator.isValidPlacement(obj, cell)) {
            return false;
        }

        pushUndoSnapshot();
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

        pushUndoSnapshot();
        Character displaced = cell.getOccupyingCharacter();
        if (displaced != null && displaced != character) {
            displaced.moveToCell(null);
        }
        character.moveToCell(cell);
        return true;
    }

    public boolean deleteObject(int x, int y) {
        GridCell cell = currentMap.getCellAt(x, y);

        if (cell == null) {
            return false;
        }

        if (cell.getStaticObject() != null) {
            pushUndoSnapshot();
            cell.setStaticObject(null);
            return true;
        }

        if (cell.getOccupyingCharacter() != null) {
            pushUndoSnapshot();
            cell.getOccupyingCharacter().moveToCell(null);
            return true;
        }

        List<GameObject> objects = cell.getObjects();
        if (objects.isEmpty()) {
            return false;
        }

        pushUndoSnapshot();
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

        pushUndoSnapshot();
        cell.getOccupyingCharacter().moveToCell(null);
        return true;
    }

    public void clearMap() {
        pushUndoSnapshot();
        for (int x = 0; x < currentMap.getGridWidth(); x++) {
            for (int y = 0; y < currentMap.getGridHeight(); y++) {
                GridCell cell = currentMap.getCellAt(x, y);
                if (cell == null) {
                    continue;
                }

                cell.setStaticObject(null);
                cell.setOccupyingCharacter(null);
                for (GameObject obj : cell.getObjects()) cell.removeObject(obj);
            }
        }
    }

    public void generateRandomItems(int count) {
        itemGenerator.generateRandomItems(currentMap, count);
    }

    /** PDF §4.2 random loot: 5 floor items + 1 hidden searchable + container rolls. */
    public RandomItemGenerator.PopulateResult populateRandomItemsPerSpec() {
        pushUndoSnapshot();
        return itemGenerator.populatePerDesignDocument(currentMap);
    }

    public GameObject generateRandomItem(int x, int y) {
        return itemGenerator.generateAndPlaceRandomItem(currentMap, x, y);
    }

    /** Adds loot to a chest/crate already placed on the cell (editor drag onto container). */
    public boolean addItemToContainer(Item item, int x, int y) {
        if (item == null) {
            return false;
        }
        GridCell cell = currentMap.getCellAt(x, y);
        if (cell == null) {
            return false;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer) {
                pushUndoSnapshot();
                ((StaticContainer) obj).addItem(item);
                return true;
            }
        }
        return false;
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
