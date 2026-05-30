package com.kurawler.action;

import com.kurawler.effect.EatEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Consumable;
import java.util.Collections;
import java.util.List;

public class EatAction implements Action {
    private final Consumable consumable;

    /**
     * @requires consumable != null
     * @modifies this.consumable
     * @effects Stores the consumable to be used for use in resolve().
     */
    public EatAction(Consumable consumable) {
        this.consumable = consumable;
    }

    /**
     * @requires session != null && session.getHero() != null
     * @modifies none
     * @effects Returns a singleton list containing an EatEffect if consumable is
     *          in the hero's inventory; otherwise returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        if (!session.getHero().getInventory().hasItem(consumable)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new EatEffect(consumable));
    }
}