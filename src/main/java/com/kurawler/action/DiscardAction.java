package com.kurawler.action;

import com.kurawler.effect.DiscardEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero discards an Item from the inventory, dropping it on the
 * current map cell.
 *
 * @requires item != null
 * @modifies none (delegates all state changes to DiscardEffect)
 * @effects If item is in the hero's inventory, returns a singleton list
 *          containing a new DiscardEffect; otherwise returns an empty list.
 */
public class DiscardAction implements Action {

    private final Item item;

    public DiscardAction(Item item) {
        this.item = item;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        if (!session.getHero().getInventory().hasItem(item)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new DiscardEffect(item));
    }
}
