package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Consumable;

/**
 * Effect: Consumes a consumable item, heals stats, and removes from inventory.
 */
public class EatEffect implements Effect {
    private final Consumable consumable;

    public EatEffect(Consumable consumable) {
        this.consumable = consumable;
    }

    @Override
    public void apply(GameSession session) {
        // Stats restore ve item removal mantığını tetikle
        boolean consumed = session.getHero().useConsumable(consumable);
        
        if (consumed) {
            // --- ACCEPTANCE CRITERIA: repOk() passes after eat action ---
            if (!session.getHero().getInventory().repOk()) {
                throw new AssertionError("Inventory invariant violated after EatEffect!");
            }
            System.out.println("[Effect] Consumed " + consumable.getName());
        }
    }

    @Override
    public String describe() {
        return "Eat " + consumable.getName();
    }
}