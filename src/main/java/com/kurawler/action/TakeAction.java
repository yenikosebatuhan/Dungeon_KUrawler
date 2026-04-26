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

    public TakeAction(Item targetItem) {
        this.targetItem = targetItem;
    }

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
        for (GameObject obj : cell.getGameObjects()) {
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
