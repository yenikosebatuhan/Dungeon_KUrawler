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

    public RemoveFromCellEffect(int row, int col, GameObject object) {
        this.row = row;
        this.col = col;
        this.object = object;
    }

    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell != null) {
            cell.removeObject(object);
            System.out.println("[Effect] " + object.getName() + " removed from cell (" + row + "," + col + ").");
        }
    }

    @Override
    public String describe() {
        return "Remove " + object.getName() + " from cell (" + row + "," + col + ")";
    }
}
