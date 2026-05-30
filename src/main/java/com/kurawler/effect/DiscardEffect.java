package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;

/**
 * Effect: removes an Item from the hero's inventory and drops it on the
 * hero's current map cell. Safely unequips the item first if it happens
 * to be the currently equipped weapon or wearable.
 *
 * @requires session != null && session.getHero() != null && item != null
 * @modifies hero.inventory, hero.equippedWeapon, hero.equippedWearable,
 *           hero.currentCell.objects
 * @effects Removes item from inventory. If item was equipped, unequips it
 *          (restoring the relevant stat). Places the item on the hero's
 *          current cell so it is visible on the map.
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
        if (item instanceof Weapon && item == hero.getEquippedWeapon()) {
            hero.setEquippedWeapon(null);
        } else if (item instanceof Wearable wearable && hero.isWearableEquipped(wearable)) {
            hero.unequipWearable(wearable);
        }

        hero.getInventory().removeItem(item);

        // Drop the item onto the hero's current cell so it is visible on the map
        GridCell cell = hero.getCurrentCell();
        if (cell != null) {
            cell.addObject(item);
        }
    }

    @Override
    public String describe() {
        return "Discard " + (item != null ? item.getName() : "nothing");
    }
}
