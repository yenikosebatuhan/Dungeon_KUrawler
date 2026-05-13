package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero consumes a consumable item from the inventory.
 * Safely delegates the state modification to an Effect.
 */
public class EatAction implements Action {

    private final Consumable consumable;

    public EatAction(Consumable consumable) {
        this.consumable = consumable;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();

        // Check if the hero actually has the item before generating an effect
        if (!hero.getInventory().hasItem(consumable)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new Effect() {
            @Override
            public void apply(GameSession s) {
                // The useConsumable method handles removal and all stat restorations
                s.getHero().useConsumable(consumable);
                System.out.println("[Effect] " + consumable.getName() + " consumed for HP/MP/EN.");
            }

            @Override
            public String describe() {
                return "Consumes " + consumable.getName();
            }
        });
    }
}