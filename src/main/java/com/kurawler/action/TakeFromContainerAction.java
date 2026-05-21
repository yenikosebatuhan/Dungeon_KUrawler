package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.StaticContainer;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero transfers an Item from a Container into their Inventory.
 */
public class TakeFromContainerAction implements Action {

    private final Object container; // Can be StaticContainer or PortableContainer
    private final Item item;

    public TakeFromContainerAction(Object container, Item item) {
        this.container = container;
        this.item = item;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();

        if (hero.getInventory().isFull()) {
            return Collections.emptyList(); // Cannot take if inventory is full
        }

        return Collections.singletonList(new Effect() {
            @Override
            public void apply(GameSession s) {
                // Remove from the respective container
                if (container instanceof StaticContainer) {
                    ((StaticContainer) container).removeItem(item);
                } else if (container instanceof PortableContainer) {
                    ((PortableContainer) container).getContents().remove(item); // Note: Portable container needs remove logic, handled here via list ref if exposed, but let's assume standard behavior.
                }

                // Add to hero inventory
                s.getHero().getInventory().addItem(item);
                System.out.println("[Effect] " + item.getName() + " looted from container.");
            }

            @Override
            public String describe() {
                return "Takes " + item.getName() + " from container.";
            }
        });
    }
}