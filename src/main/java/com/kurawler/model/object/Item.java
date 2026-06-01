package com.kurawler.model.object;

import java.util.List;

/**
 * An object that can be picked up and placed in Inventory.
 */
public abstract class Item extends GameObject {

    protected Item(String name, String description) {
        super(name, description);
    }

    /** Actions offered when the item is on the map (§1.2 click menu). */
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.BREAK);
    }
}
