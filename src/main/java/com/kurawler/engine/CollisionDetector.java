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
     * @requires map != null
     * @modifies this.map
     * @effects Stores map reference; throws IllegalArgumentException if map is null.
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
     * @requires map != null (enforced by constructor)
     * @modifies nothing
     * @effects Returns true if (row, col) is outside the map bounds
     *          (map.getCell returns null), or if the cell exists but is not
     *          walkable (occupied by a character or contains an impassable
     *          StaticObject). Returns false if the cell is in bounds and walkable.
     *
     * @param row the row coordinate to check
     * @param col the column coordinate to check
     * @return true if collision detected (blocked or out of bounds); false if clear
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
     * @requires map != null (enforced by constructor)
     * @modifies nothing
     * @effects Returns true if and only if all three conditions hold:
     *            (1) (row, col) is within map bounds,
     *            (2) the cell is walkable (no impassable StaticObject and no
     *                occupying character),
     *            (3) the cell has no GameObject items placed on it.
     *          Returns false if any condition is not met.
     *
     * @param row the row coordinate to check
     * @param col the column coordinate to check
     * @return true if the cell exists, is walkable, and carries no objects
     */
    public boolean isCellEmpty(int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return false;
        }
        return cell.isWalkable() && !cell.hasObjects();
    }

    /**
     * Returns the GridCell at the given position, or null if out of bounds.
     *
     * @requires none
     * @modifies none
     * @effects Returns map.getCell(row, col); null if coordinates are out of bounds.
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
     * @requires map != null
     * @modifies this.map
     * @effects Sets the map to the new value; throws IllegalArgumentException if null.
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
     * @requires none
     * @modifies none
     * @effects Returns the current Map instance.
     * @return the current Map instance
     */
    public Map getMap() {
        return map;
    }
}
