package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;

/**
 * Effect that adds an item to the hero's inventory.
 */
public class AddToInventoryEffect implements Effect {

    private final Item item;

    public AddToInventoryEffect(Item item) {
        this.item = item;
    }

    @Override
    public void apply(GameSession session) {
        // Envantere ekle
        boolean added = session.getHero().getInventory().addItem(item);
        
        // --- PBI-03 ACCEPTANCE CRITERIA: repOk() passes ---
        if (!session.getHero().getInventory().repOk()) {
            throw new AssertionError("Inventory invariant violated after AddToInventoryEffect!");
        }

        if (added) {
            System.out.println("[Effect] " + item.getName() + " added to inventory.");
            session.getHero().notifyItemCollected();
        }
    }

    @Override
    public String describe() {
        return "Add " + item.getName() + " to inventory";
    }
}