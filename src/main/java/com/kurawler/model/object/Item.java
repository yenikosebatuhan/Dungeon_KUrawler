package com.kurawler.model.object;

/**
 * An object that can be picked up and placed in Inventory.
 */
public abstract class Item extends GameObject {

    protected Item(String name, String description) {
        super(name, description);
    }
}
