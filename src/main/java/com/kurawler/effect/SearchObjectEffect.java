package com.kurawler.effect;

import com.kurawler.model.GridCell;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;

public class SearchObjectEffect implements Effect {
    private final StaticObject object;
    private final int row;
    private final int col;

    public SearchObjectEffect(StaticObject object, int row, int col) {
        this.object = object;
        this.row = row;
        this.col = col;
    }

    @Override
    public void apply(GameSession session) {
        if (object == null || !object.isSearchable()) {
            return;
        }

        Item found = object.revealHiddenItem();
        if (found == null) {
            return;
        }

        GridCell cell = session.getMap().getCell(row, col);
        if (cell != null) {
            cell.addObject(found);
        }
    }

    @Override
    public String describe() {
        return "Search " + (object != null ? object.getName() : "nothing");
    }
}
