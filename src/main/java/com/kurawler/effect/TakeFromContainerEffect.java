package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.StaticContainer;

/**
 * Removes an item from a static or portable container and adds it to the hero inventory.
 */
public class TakeFromContainerEffect implements Effect {

    private final Object container;
    private final Item item;

    public TakeFromContainerEffect(Object container, Item item) {
        this.container = container;
        this.item = item;
    }

    @Override
    public void apply(GameSession session) {
        if (session == null || session.getHero() == null || item == null || container == null) {
            return;
        }
        if (session.getHero().getInventory().isFull()) {
            return;
        }

        if (container instanceof StaticContainer) {
            ((StaticContainer) container).removeItem(item);
        } else if (container instanceof PortableContainer) {
            ((PortableContainer) container).removeItem(item);
        } else {
            return;
        }

        if (session.getHero().getInventory().addItem(item)) {
            session.getHero().notifyItemCollected();
        }
    }

    @Override
    public String describe() {
        return "Take " + (item != null ? item.getName() : "item") + " from container";
    }
}
