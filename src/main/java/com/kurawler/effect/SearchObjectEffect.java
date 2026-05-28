package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;

public class SearchObjectEffect implements Effect {
    private final StaticObject object;

    public SearchObjectEffect(StaticObject object) {
        this.object = object;
    }

    @Override
    public void apply(GameSession session) {
        if (object == null || !object.isSearchable()) {
            return;
        }

        object.markSearched();
        Item found = object.revealHiddenItem();
        if (found == null) {
            return;
        }

        com.kurawler.model.Map map = session.getMap();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.getStaticObject() == object) {
                    cell.addObject(found);
                    return;
                }
            }
        }

        object.setHiddenItem(found);
    }

    @Override
    public String describe() {
        return "Search " + (object != null ? object.getName() : "nothing");
    }
}
