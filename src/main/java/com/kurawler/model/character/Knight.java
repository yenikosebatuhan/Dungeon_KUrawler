package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import java.util.Random;

/**
 * Melee enemy. Spec: 20HP, chases hero within 5 cells, melee attack.
 */
public class Knight extends Enemy {

    private static final int SIGHT_RADIUS = 5;
    private static final Random RNG = new Random();

    public Knight() {
        super("Knight", 20, 4, 1, 8, 0, 80);
        // HP=20, ATK=4 (spec: deals 4HP), DEF=1 (armor absorbs 1),
        // STR=8, Mana=0, Energy=80
    }

    /**
     * Per-turn AI:
     *  - if adjacent to the hero (Manhattan == 1): attack with calculateDamage()
     *  - else if hero within Euclidean SIGHT_RADIUS: step one tile toward hero
     *  - else: wander one tile in a random direction
     */
    public void decideAction(Hero hero, Map map, GameSession session) {
        if (hero == null || map == null || !isAlive() || !hero.isAlive()) return;

        int dRow = hero.getRow() - getRow();
        int dCol = hero.getCol() - getCol();

        // Adjacent → attack the hero
        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            hero.takeDamage(calculateDamage()); // attackPower(4) + strength(8) - hero DEF
            return;
        }

        double euclid = Math.sqrt(dRow * dRow + dCol * dCol);
        if (euclid <= SIGHT_RADIUS) {
            // Greedy chase: step along the larger-magnitude axis first
            if (Math.abs(dRow) >= Math.abs(dCol)) {
                if (!tryStep(Integer.signum(dRow), 0, map, session, hero)) {
                    tryStep(0, Integer.signum(dCol), map, session, hero);
                }
            } else {
                if (!tryStep(0, Integer.signum(dCol), map, session, hero)) {
                    tryStep(Integer.signum(dRow), 0, map, session, hero);
                }
            }
        } else {
            // Wander
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(4)];
            tryStep(d[0], d[1], map, session, hero);
        }
    }

    /**
     * Attempts one step. Returns true if the enemy actually moved.
     * Refuses if target is out of bounds, unwalkable, occupied by hero, or
     * occupied by another enemy.
     */
    private boolean tryStep(int dRow, int dCol, Map map, GameSession session, Hero hero) {
        if (dRow == 0 && dCol == 0) return false;
        int nr = getRow() + dRow;
        int nc = getCol() + dCol;
        GridCell target = map.getCell(nr, nc);
        if (target == null || !target.isWalkable()) return false;
        if (hero.getRow() == nr && hero.getCol() == nc) return false;
        if (session != null && session.getEnemyAt(nr, nc) != null) return false;
        setPosition(nr, nc);
        return true;
    }

    public int getSightRadius() { return SIGHT_RADIUS; }
}
