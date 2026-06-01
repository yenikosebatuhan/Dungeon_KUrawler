package com.kurawler.model;

import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.character.Character;
import java.util.ArrayList;
import java.util.List;

public class GridCell {
    private final int row;
    private final int col;
    private final List<GameObject> objects;
    
    // Caner's logic
    private StaticObject staticObject;
    private Character occupyingCharacter;

    public GridCell(int row, int col) {
        this.row = row;
        this.col = col;
        this.objects = new ArrayList<>();
    }

    public void addObject(GameObject obj) { objects.add(obj); }
    public boolean removeObject(GameObject obj) { return objects.remove(obj); }
    public List<GameObject> getObjects() { return new ArrayList<>(objects); }
    public boolean hasObjects() { return !objects.isEmpty(); }
    public int getRow() { return row; }
    public int getCol() { return col; }

    // --- CANER'S CORE METHODS ---
    public int getX() { return col; }
    public int getY() { return row; }
    
    public StaticObject getStaticObject() { return staticObject; }
    public void setStaticObject(StaticObject obj) { this.staticObject = obj; }
    public Character getOccupyingCharacter() { return occupyingCharacter; }
    public void setOccupyingCharacter(Character character) { this.occupyingCharacter = character; }

    public boolean isBlocked() {
        if (occupyingCharacter != null) {
            return true;
        }
        if (hasObjects()) {
            return true;
        }
        return !isWalkableFloorTile();
    }

    /** Enemies cannot stand on walls, doors/portals, or occupied tiles. */
    public boolean isEnemyWalkable() {
        if (staticObject instanceof MapPortal) {
            return false;
        }
        if (occupyingCharacter != null) {
            return false;
        }
        if (hasObjects()) {
            return false;
        }
        return isWalkableFloorTile();
    }

    /**
     * Only bare interior floor tiles are walkable — not pools, bookshelves, columns,
     * chests, or dropped items (those live in {@link #objects}).
     */
    private boolean isWalkableFloorTile() {
        StaticObject so = staticObject;
        if (so == null) {
            return false;
        }
        if (so instanceof MapPortal) {
            return so.isPassable();
        }
        if (!so.isPassable()) {
            return false;
        }
        if (so.isSearchable() || so.wasSearched()) {
            return false;
        }
        return "Floor".equals(so.getName());
    }

    // Alias for teammates
    public boolean isWalkable() {
        return !isBlocked();
    }
    public void setWalkable(boolean walkable) { /* Kept so MapFactory doesn't crash, but handled via StaticObjects */ }
}
