package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;

/**
 * Detects collisions and checks cell accessibility on the game map.
 *
 * <p>A collision occurs when a cell is out of bounds or not walkable.
 * Used by {@link GameLoop} and {@link EnemySpawner} to validate
 * movement targets and spawn locations.</p>
 */
public class CollisionDetector {

    private Map map;

    /**
     * Creates a collision detector for the given map.
     *
     * @param map the game map to check collisions against; must not be null
     */
    public CollisionDetector(Map map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null");
        }
        this.map = map;
    }

    /**
     * Checks whether the given grid position is blocked (wall or out of bounds).
     *
     * @param row the row coordinate to check
     * @param col the column coordinate to check
     * @return true if the cell is out of bounds or not walkable (collision); false if clear
     */
    public boolean checkCollision(int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return true;
        }
        return !cell.isWalkable();
    }

    /**
     * Checks whether a cell is empty and walkable (valid for movement or spawning).
     *
     * @param row the row coordinate to check
     * @param col the column coordinate to check
     * @return true if the cell exists, is walkable, and has no objects on it
     */
    public boolean isCellEmpty(int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return false;
        }
        return cell.isWalkable() && cell.getObjects().isEmpty();
    }

    /**
     * Returns the GridCell at the given position, or null if out of bounds.
     *
     * @param row the row coordinate
     * @param col the column coordinate
     * @return the GridCell at (row, col), or null if coordinates are invalid
     */
    public GridCell getCellAt(int row, int col) {
        return map.getCell(row, col);
    }

    /**
     * Updates the map reference (e.g., when a new level is loaded).
     *
     * @param map the new map; must not be null
     */
    public void setMap(Map map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null");
        }
        this.map = map;
    }

    /**
     * Returns the map this detector operates on.
     *
     * @return the current Map instance
     */
    public Map getMap() {
        return map;
    }
}
