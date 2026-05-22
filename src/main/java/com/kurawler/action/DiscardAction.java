package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero permanently discards an Item from the inventory.
 */
public class DiscardAction implements Action {

    private final Item item;

    public DiscardAction(Item item) {
        this.item = item;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();
        
        if (!hero.getInventory().hasItem(item)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new Effect() {
            @Override
            public void apply(GameSession s) {
                Hero h = s.getHero();
                // Safe unequip checks before discarding
                if (item == h.getEquippedWeapon()) {
                    h.setEquippedWeapon(null);
                }
                if (item == h.getEquippedWearable()) {
                    h.unequipWearable();
                }
                
                h.getInventory().removeItem(item);
                System.out.println("[Effect] " + item.getName() + " permanently discarded.");
            }

            @Override
            public String describe() {
                return "Discards " + item.getName();
            }
        });
    }
}