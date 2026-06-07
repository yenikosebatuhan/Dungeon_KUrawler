package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;
import java.util.LinkedHashMap;

/**
 * Level editor: manages a world of named maps and delegates single-map edits
 * to the underlying {@link MapEditor}.
 */
public class LevelEditor {

    private final MapEditor mapEditor;
    private final LinkedHashMap<String, Map> worldMaps = new LinkedHashMap<>();
    private String currentMapId;

    public LevelEditor(Map map) {
        this.mapEditor = new MapEditor(map);
        this.currentMapId = "map_1";
        worldMaps.put(currentMapId, map);
    }

    // ── Multi-map management ────────────────────────────────────────────────

    /** Returns all maps in this world (insertion-ordered). */
    public LinkedHashMap<String, Map> getWorldMaps() {
        return worldMaps;
    }

    public String getCurrentMapId() {
        return currentMapId;
    }

    /** Switch the active editing map.  No-op if {@code mapId} is unknown. */
    public void switchToMap(String mapId) {
        Map m = worldMaps.get(mapId);
        if (m == null) return;
        currentMapId = mapId;
        mapEditor.setCurrentMap(m);
        mapEditor.clearUndoHistory();
    }

    /**
     * Create a blank map with the same dimensions as the current map, register
     * it under a unique ID derived from {@code baseName}, switch to it, and
     * return the new ID.
     */
    public String createNewMap(String baseName) {
        String id = uniqueId(baseName);
        Map current = mapEditor.getCurrentMap();
        Map fresh = new Map(current.getWidth(), current.getHeight());
        paveFloor(fresh);
        worldMaps.put(id, fresh);
        switchToMap(id);
        return id;
    }

    /** Remove a map from the world.  The current map is changed to the first remaining one. */
    public void removeMap(String mapId) {
        if (worldMaps.size() <= 1) return; // keep at least one
        worldMaps.remove(mapId);
        if (mapId.equals(currentMapId)) {
            switchToMap(worldMaps.keySet().iterator().next());
        }
    }

    /** Rename a map ID (order is preserved). */
    public void renameMap(String oldId, String newId) {
        if (!worldMaps.containsKey(oldId) || worldMaps.containsKey(newId)) return;
        LinkedHashMap<String, Map> rebuilt = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Map> e : worldMaps.entrySet()) {
            rebuilt.put(e.getKey().equals(oldId) ? newId : e.getKey(), e.getValue());
        }
        worldMaps.clear();
        worldMaps.putAll(rebuilt);
        if (oldId.equals(currentMapId)) currentMapId = newId;
    }

    /** Load a full world into the editor, replacing any existing maps. */
    public void loadWorld(LinkedHashMap<String, Map> maps, String startMapId) {
        worldMaps.clear();
        worldMaps.putAll(maps);
        String id = maps.containsKey(startMapId) ? startMapId : maps.keySet().iterator().next();
        currentMapId = id;
        mapEditor.setCurrentMap(maps.get(id));
        mapEditor.clearUndoHistory();
    }

    // ── Single-map delegates ─────────────────────────────────────────────────

    public void setMap(Map map) {
        mapEditor.setCurrentMap(map);
        mapEditor.clearUndoHistory();
        worldMaps.put(currentMapId, map);
    }

    public boolean placeObject(int row, int col, GameObject obj) {
        return mapEditor.placeObject(obj, col, row);
    }

    public boolean removeObject(int row, int col, GameObject obj) {
        if (obj == null) return mapEditor.deleteObject(col, row);
        return mapEditor.deleteObject(col, row, obj);
    }

    public boolean placeCharacter(int row, int col, Character character) {
        return mapEditor.placeCharacter(character, col, row);
    }

    public boolean addItemToContainer(int row, int col, Item item) {
        return mapEditor.addItemToContainer(item, col, row);
    }

    public GameObject generateRandomItem(int row, int col) {
        return mapEditor.generateRandomItem(col, row);
    }

    public RandomItemGenerator.PopulateResult addRandomItemsPerSpec() {
        return mapEditor.populateRandomItemsPerSpec();
    }

    public void clearMap() {
        mapEditor.clearMap();
    }

    public boolean undo() {
        if (!mapEditor.undo()) {
            return false;
        }
        worldMaps.put(currentMapId, mapEditor.getCurrentMap());
        return true;
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

    /** Keeps the world registry pointing at the map currently shown in the editor. */
    public void syncCurrentMapToWorld() {
        worldMaps.put(currentMapId, mapEditor.getCurrentMap());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String uniqueId(String base) {
        if (!worldMaps.containsKey(base)) return base;
        int n = 2;
        while (worldMaps.containsKey(base + "_" + n)) n++;
        return base + "_" + n;
    }

    private static void paveFloor(Map map) {
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null) {
                    cell.setStaticObject(new StaticObject("Dungeon Floor", "Stone floor", true, false));
                    cell.setWalkable(true);
                }
            }
        }
    }
}
