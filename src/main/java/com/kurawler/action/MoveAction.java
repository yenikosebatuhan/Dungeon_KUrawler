package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.MoveEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
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

    public MoveAction(int deltaRow, int deltaCol) {
        this.deltaRow = deltaRow;
        this.deltaCol = deltaCol;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        Hero hero = session.getHero();
        int newRow = hero.getRow() + deltaRow;
        int newCol = hero.getCol() + deltaCol;

        GridCell target = session.getMap().getCell(newRow, newCol);
        if (target != null && target.isWalkable()) {
            effects.add(new MoveEffect(hero, newRow, newCol));
        }

        return effects;
    }
}
