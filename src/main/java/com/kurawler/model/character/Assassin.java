package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.Map;
import java.util.Random;

public class Assassin extends Enemy {
    private static final int SIGHT_RADIUS = 6;
    private static final Random RNG = new Random();

    public Assassin() {
        super("Assassin", 14, 5, 0, 3, 0, 100);
    }

    @Override
    public void decideAction(Character target, Map map, GameSession session) {
        if (target == null || map == null || !isAlive() || !target.isAlive()) {
            return;
        }
        int dRow = target.getRow() - getRow();
        int dCol = target.getCol() - getCol();
        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            com.kurawler.engine.CombatResolver.performMeleeAttack(this, target);
            return;
        }
        double euclid = Math.sqrt(dRow * dRow + dCol * dCol);
        if (euclid <= SIGHT_RADIUS) {
            if (!tryEnemyStep(Integer.signum(dRow), Integer.signum(dCol), map, session)) {
                if (!tryEnemyStep(Integer.signum(dRow), 0, map, session)) {
                    tryEnemyStep(0, Integer.signum(dCol), map, session);
                }
            }
        } else {
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(dirs.length)];
            tryEnemyStep(d[0], d[1], map, session);
        }
    }
}
