package com.kurawler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all items in the game.
 * Extends GameObject and adds inventory-related properties.
 */
public class Item extends GameObject {

    protected boolean takesInventorySlot;
    protected boolean isTargetObjective;

    /**
     * Creates a new Item with the given name.
     * @param name The display name of the item
     */
    public Item(String name) {
        super(name);
        this.takesInventorySlot = true;
        this.isTargetObjective = false;
    }

    /**
     * Returns whether this item occupies an inventory slot.
     * @return true if item takes a slot
     */
    public boolean takesInventorySlot() {
        return takesInventorySlot;
    }

    /**
     * Returns whether this item is the win-condition objective.
     * @return true if item is the target objective
     */
    public boolean isTargetObjective() {
        return isTargetObjective;
    }

    /**
     * Sets whether this item is the target objective.
     * @param target true to mark as objective
     */
    public void setTargetObjective(boolean target) {
        this.isTargetObjective = target;
    }

    /**
     * Returns available actions for this item.
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("DISCARD");
        return actions;
    }
}
