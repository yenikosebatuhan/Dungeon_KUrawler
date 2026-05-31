package com.kurawler.model;

import com.kurawler.model.object.GameObject;
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
        if (staticObject != null && !staticObject.isPassable()) return true;
        if (occupyingCharacter != null && occupyingCharacter.isAlive()) return true;
        return false;
    }

    // Alias for teammates
    public boolean isWalkable() {
        return !isBlocked();
    }
    public void setWalkable(boolean walkable) { /* Kept so MapFactory doesn't crash, but handled via StaticObjects */ }
}