package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;

/**
 * Effect: Searches a static object, revealing any hidden item and placing it on the map.
 */
public class SearchObjectEffect implements Effect {
    private final StaticObject object;

    /**
     * @requires object != null
     * @modifies this.object
     * @effects Stores the static object to be searched for use in apply().
     */
    public SearchObjectEffect(StaticObject object) {
        this.object = object;
    }

    /**
     * @requires session != null && session.getMap() != null
     * @modifies object.searched, cell.objects (if hidden item is found)
     * @effects Marks object as searched. If a hidden item is revealed, places it
     *          on the grid cell that contains the object. If the cell is not found
     *          in the map, re-stores the hidden item on the object.
     */
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

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the object being searched.
     */
    @Override
    public String describe() {
        return "Search " + (object != null ? object.getName() : "nothing");
    }
}
