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
        boolean added = session.getHero().getInventory().addItem(item);
        if (added) {
            System.out.println("[Effect] " + item.getName() + " added to inventory.");
            session.getHero().notifyItemCollected();
        } else {
            System.out.println("[Effect] Inventory full! Could not add " + item.getName() + ".");
        }
    }

    @Override
    public String describe() {
        return "Add " + item.getName() + " to inventory";
    }
}
