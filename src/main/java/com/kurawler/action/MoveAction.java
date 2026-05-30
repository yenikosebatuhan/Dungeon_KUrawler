package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.MapTransitionEffect;
import com.kurawler.effect.MoveEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.character.Hero;
import java.util.ArrayList;
import java.util.List;

/**
 * Action: Hero moves in a direction.
 * Does NOT modify state — produces MoveEffect if destination is walkable.
 */
public class MoveAction implements Action {

    private final int deltaRow;
    private final int deltaCol;

    /**
     * @requires true
     * @modifies this.deltaRow, this.deltaCol
     * @effects Stores the movement delta for use in resolve().
     */
    public MoveAction(int deltaRow, int deltaCol) {
        this.deltaRow = deltaRow;
        this.deltaCol = deltaCol;
    }

    /**
     * @requires session != null && session.getHero() != null && session.getMap() != null
     * @modifies none
     * @effects Returns a MoveEffect if the target cell is walkable and hero has
     *          enough energy. Also appends a MapTransitionEffect if the target
     *          cell contains a MapPortal. Returns an empty list otherwise.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        Hero hero = session.getHero();
        int newRow = hero.getRow() + deltaRow;
        int newCol = hero.getCol() + deltaCol;

        if (hero.getEnergy() < 1) {
            System.out.println("[Action] " + hero.getName() + " has no energy to move!");
            return effects;
        }

        GridCell target = session.getMap().getCell(newRow, newCol);
        if (target == null) {
            return effects;
        }

        StaticObject staticObj = target.getStaticObject();
        if (staticObj instanceof MapPortal) {
            MapPortal portal = (MapPortal) staticObj;
            if (!target.isWalkable()) {
                if (portal.tryUnlock(hero)) {
                    effects.add(new MoveEffect(hero, newRow, newCol));
                    effects.add(new MapTransitionEffect(portal));
                } else {
                    session.setLastPortalMessage(portal.getLockHint());
                    System.out.println("[Action] " + portal.getName() + " is locked.");
                }
                return effects;
            }
            effects.add(new MoveEffect(hero, newRow, newCol));
            effects.add(new MapTransitionEffect(portal));
            return effects;
        }

        if (target.isWalkable()) {
            effects.add(new MoveEffect(hero, newRow, newCol));
        }

        return effects;
    }
}
