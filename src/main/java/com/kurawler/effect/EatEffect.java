package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Consumable;

/**
 * Effect: Consumes a consumable item, restores stats, and removes it from inventory.
 */
public class EatEffect implements Effect {
    private final Consumable consumable;

    /**
     * @requires consumable != null
     * @modifies this.consumable
     * @effects Stores the consumable to be used for apply().
     */
    public EatEffect(Consumable consumable) {
        this.consumable = consumable;
    }

    /**
     * @requires session != null && session.getHero() != null && consumable != null
     * @modifies hero.health or hero.mana or hero.energy, hero.inventory
     * @effects Calls hero.useConsumable() which restores the appropriate stat and
     *          removes the item from inventory. Verifies inventory invariant (repOk)
     *          after consumption. Throws AssertionError if repOk fails.
     */
    @Override
    public void apply(GameSession session) {
        boolean consumed = session.getHero().useConsumable(consumable);

        if (consumed) {
            if (!session.getHero().getInventory().repOk()) {
                throw new AssertionError("Inventory invariant violated after EatEffect!");
            }
            System.out.println("[Effect] Consumed " + consumable.getName());
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the consumable being eaten.
     */
    @Override
    public String describe() {
        return "Eat " + consumable.getName();
    }
}
