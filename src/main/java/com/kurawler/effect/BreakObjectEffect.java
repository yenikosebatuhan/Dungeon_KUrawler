package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Effect: Marks a breakable container open or removes a crate/barrel floor prop.
 */
public class BreakObjectEffect implements Effect {
    public static final int BREAK_ENERGY_COST = 5;

    private final int row;
    private final int col;

    /** Spends break attempt energy (success or failure). */
    public static boolean spendAttemptEnergy(Hero hero) {
        return hero != null && hero.useEnergy(BREAK_ENERGY_COST);
    }

    public BreakObjectEffect(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return;
        }

        for (GameObject obj : new ArrayList<>(cell.getObjects())) {
            if (!(obj instanceof StaticContainer sc) || !sc.isBreakable()) {
                continue;
            }
            sc.markBroken();
            if (sc.isLocked()) {
                sc.unlock();
            }
            return;
        }

        StaticObject object = cell.getStaticObject();
        if (!MapFactory.isBreakableStaticObject(object)) {
            return;
        }
        Item hidden = object.revealHiddenItem();
        if (hidden != null) {
            cell.addObject(hidden);
        }
        cell.setStaticObject(null);
    }

    @Override
    public String describe() {
        return "Break object at (" + row + "," + col + ") and drop its contents.";
    }
}
