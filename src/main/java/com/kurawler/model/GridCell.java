package com.kurawler.model;

import com.kurawler.model.object.GameObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A single cell in the dungeon grid.
 * Information Expert: GridCell knows its contents.
 */
public class GridCell {

    private final int row;
    private final int col;
    private final List<GameObject> objects;
    private boolean walkable;

    public GridCell(int row, int col) {
        this.row = row;
        this.col = col;
        this.objects = new ArrayList<>();
        this.walkable = true;
    }

    public void addObject(GameObject obj) {
        objects.add(obj);
    }

    public boolean removeObject(GameObject obj) {
        return objects.remove(obj);
    }

    public List<GameObject> getObjects() {
        return new ArrayList<>(objects);
    }

    public boolean hasObjects() {
        return !objects.isEmpty();
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public void setWalkable(boolean walkable) {
        this.walkable = walkable;
    }
}
