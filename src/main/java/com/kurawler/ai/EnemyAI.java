package com.kurawler.ai;

import com.kurawler.action.Action;
import com.kurawler.action.MoveAction;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Enemy;

/**
 * Simple AI that determines what action an enemy should take.
 * Stub: currently just moves toward the hero.
 */
public class EnemyAI {

    /** Decide the next action for the given enemy. */
    public Action decideAction(Enemy enemy, GameSession session) {
        // TODO: Implement pathfinding / smarter AI
        int heroRow = session.getHero().getRow();
        int heroCol = session.getHero().getCol();

        int dRow = Integer.signum(heroRow - enemy.getRow());
        int dCol = Integer.signum(heroCol - enemy.getCol());

        return new MoveAction(dRow, dCol);
    }
}
