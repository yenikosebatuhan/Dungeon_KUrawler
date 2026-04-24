package com.kurawler.model;

import com.kurawler.model.object.GameObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all items in the game.
 * Extends GameObject and adds inventory-related properties.
 *
 * <p>Items are GameObjects that (usually) occupy an inventory slot and
 * expose a list of contextual actions the UI can offer the player.</p>
 */
public abstract class Item extends GameObject {

    protected boolean takesInventorySlot;
    protected boolean isTargetObjective;

    /**
     * Creates a new Item with the given name and an empty description.
     * @param name The display name of the item
     */
    protected Item(String name) {
        super(name, "");
        this.takesInventorySlot = true;
        this.isTargetObjective = false;
    }

    /**
     * Creates a new Item with the given name and description.
     * @param name        The display name of the item
     * @param description Flavour text describing the item
     */
    protected Item(String name, String description) {
        super(name, description);
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
     * Returns the list of contextual actions available for this item
     * (e.g. "DISCARD", "EQUIP", "EAT", ...).
     * Subclasses should override to append their own actions.
     * @return list of action strings (never null)
     */
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("DISCARD");
        return actions;
    }
}
