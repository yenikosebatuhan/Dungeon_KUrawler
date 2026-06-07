package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;

/**
 * Effect: removes an Item from the hero's inventory (§2.4.2 DISCARD).
 * Safely unequips the item first if it is the currently equipped weapon or wearable,
 * then drops it on the hero's current cell.
 *
 * @requires session != null && session.getHero() != null && item != null
 * @modifies hero.inventory, hero.equippedWeapon, hero.equippedWearable, hero cell objects
 * @effects Removes item from inventory, unequips if needed, places item on the map
 *          at the hero's row/col.
 */
public class DiscardEffect implements Effect {

    private final Item item;

    public DiscardEffect(Item item) {
        this.item = item;
    }

    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || item == null) return;
        if (!hero.getInventory().hasItem(item)) return;

        // Unequip first so stats are restored before the item leaves inventory
        if (item instanceof Weapon) {
            if (item == hero.getEquippedWeapon()) {
                hero.setEquippedWeapon(null);
            }
            if (item == hero.getEquippedArrow()) {
                hero.setEquippedArrow(null);
            }
            if (item == hero.getSecondaryMeleeWeapon()) {
                hero.setSecondaryMeleeWeapon(null);
            }
        } else if (item instanceof Wearable wearable && hero.isWearableEquipped(wearable)) {
            hero.unequipWearable(wearable);
        }

        hero.getInventory().removeItem(item);

        GridCell cell = session.getMap().getCell(hero.getRow(), hero.getCol());
        if (cell != null) {
            cell.addObject(item);
        }
    }

    @Override
    public String describe() {
        return "Discard " + (item != null ? item.getName() : "nothing");
    }
}
