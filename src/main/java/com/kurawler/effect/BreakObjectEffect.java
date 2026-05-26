package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Item;

/**
 * Effect: Removes a breakable static object from the map and drops its contents.
 */
public class BreakObjectEffect implements Effect {
    private final int row;
    private final int col;

    public BreakObjectEffect(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * @requires session != null
     * @modifies cell (removes object, adds item), session
     * @effects Removes the static object from the cell. If it contained a hidden item,
     * adds that item to the grid cell's object list.
     */
    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return;
        }
        
        StaticObject object = cell.getStaticObject();
        
        if (object != null && object.isSearchable()) {
            
            Item hidden = object.revealHiddenItem();
            if (hidden != null) {
                cell.addObject(hidden);
            }
            
            cell.setStaticObject(null);
        }
    }

    @Override
    public String describe() {
        return "Break object at (" + row + "," + col + ") and drop its contents.";
    }
}