package com.kurawler.action;

import com.kurawler.effect.AddToInventoryEffect;
import com.kurawler.effect.Effect;
import com.kurawler.effect.RemoveFromCellEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Action: Hero picks up an item from the current cell.
 * Does NOT modify state — produces RemoveFromCellEffect + AddToInventoryEffect.
 */
public class TakeAction implements Action {

    private final Item targetItem;

    /**
     * @requires targetItem != null
     * @modifies this.targetItem
     * @effects Stores the item to be picked up for use in resolve().
     */
    public TakeAction(Item targetItem) {
        this.targetItem = targetItem;
    }

    /**
     * @requires session != null && session.getHero() != null && session.getMap() != null
     * @modifies none
     * @effects Returns [RemoveFromCellEffect, AddToInventoryEffect] if targetItem
     *          is on the hero's current cell and inventory is not full;
     *          otherwise returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        Hero hero = session.getHero();
        int row = hero.getRow();
        int col = hero.getCol();

        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return effects;
        }

        // Check that the item is actually on this cell
        boolean found = false;
        for (GameObject obj : cell.getObjects()) {
            if (obj == targetItem) {
                found = true;
                break;
            }
        }

        if (found && !hero.getInventory().isFull()) {
            effects.add(new RemoveFromCellEffect(row, col, targetItem));
            effects.add(new AddToInventoryEffect(targetItem));
        }

        return effects;
    }
}
