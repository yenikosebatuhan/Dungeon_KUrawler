package com.kurawler.action;

import com.kurawler.effect.DiscardEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero discards an Item from the inventory; it drops on the hero's tile (§2.4.2).
 *
 * @requires item != null
 * @modifies none (delegates all state changes to DiscardEffect)
 * @effects If item is in the hero's inventory, returns a singleton list
 *          containing a new DiscardEffect; otherwise returns an empty list.
 */
public class DiscardAction implements Action {

    private final Item item;

    /**
     * @requires item != null
     * @modifies this.item
     * @effects Stores the item to be discarded for use in resolve().
     */
    public DiscardAction(Item item) {
        this.item = item;
    }

    /**
     * @requires session != null && session.getHero() != null
     * @modifies none
     * @effects Returns a singleton list containing a DiscardEffect if item is
     *          in the hero's inventory; otherwise returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        if (!session.getHero().getInventory().hasItem(item)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new DiscardEffect(item));
    }
}
