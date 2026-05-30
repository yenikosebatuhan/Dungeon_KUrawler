package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.GameObject;

/**
 * Effect that removes an object from a grid cell.
 */
public class RemoveFromCellEffect implements Effect {

    private final int row;
    private final int col;
    private final GameObject object;

    /**
     * @requires object != null && row >= 0 && col >= 0
     * @modifies this.row, this.col, this.object
     * @effects Stores the target cell coordinates and object for use in apply().
     */
    public RemoveFromCellEffect(int row, int col, GameObject object) {
        this.row = row;
        this.col = col;
        this.object = object;
    }

    /**
     * @requires session != null && session.getMap() != null && object != null
     * @modifies cell.objects at (row, col)
     * @effects Removes object from the cell at (row, col) if the cell exists.
     */
    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell != null) {
            cell.removeObject(object);
            System.out.println("[Effect] " + object.getName() + " removed from cell (" + row + "," + col + ").");
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the object and its cell coordinates.
     */
    @Override
    public String describe() {
        return "Remove " + object.getName() + " from cell (" + row + "," + col + ")";
    }
}
