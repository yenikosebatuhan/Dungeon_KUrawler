package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.HealEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import java.util.ArrayList;
import java.util.List;

/**
 * Action: Hero consumes a consumable item from inventory.
 * Does NOT modify state — produces HealEffect.
 */
public class EatAction implements Action {

    private final Consumable consumable;

    public EatAction(Consumable consumable) {
        this.consumable = consumable;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        Hero hero = session.getHero();

        // Only resolve if hero has this consumable
        if (hero.getInventory().removeItem(consumable)) {
            effects.add(new HealEffect(hero, consumable.getHealAmount()));
        }

        return effects;
    }
}
