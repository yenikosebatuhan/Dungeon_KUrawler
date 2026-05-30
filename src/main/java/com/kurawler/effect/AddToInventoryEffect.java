package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;

/**
 * Effect that adds an item to the hero's inventory.
 */
public class AddToInventoryEffect implements Effect {

    private final Item item;

    /**
     * @requires item != null
     * @modifies this.item
     * @effects Stores the item to be added for use in apply().
     */
    public AddToInventoryEffect(Item item) {
        this.item = item;
    }

    /**
     * @requires session != null && session.getHero() != null && item != null
     * @modifies hero.inventory
     * @effects Adds item to the hero's inventory if there is space. Verifies the
     *          inventory invariant (repOk) and notifies observers that an item was
     *          collected. Throws AssertionError if repOk fails.
     */
    @Override
    public void apply(GameSession session) {
        boolean added = session.getHero().getInventory().addItem(item);

        if (!session.getHero().getInventory().repOk()) {
            throw new AssertionError("Inventory invariant violated after AddToInventoryEffect!");
        }

        if (added) {
            System.out.println("[Effect] " + item.getName() + " added to inventory.");
            session.getHero().notifyItemCollected();
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a description string naming the item being added.
     */
    @Override
    public String describe() {
        return "Add " + item.getName() + " to inventory";
    }
}
