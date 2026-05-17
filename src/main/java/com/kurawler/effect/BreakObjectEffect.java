package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.StaticObject;

public class BreakObjectEffect implements Effect {
    private final int row;
    private final int col;

    public BreakObjectEffect(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return;
        }
        StaticObject object = cell.getStaticObject();
        if (object != null && object.isSearchable()) {
            cell.setStaticObject(null);
        }
    }

    @Override
    public String describe() {
        return "Break object at (" + row + "," + col + ")";
    }
}
