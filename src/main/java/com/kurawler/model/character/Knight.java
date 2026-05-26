package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Weapon;
import java.util.Random;

public class Knight extends Enemy {
    private static final int SIGHT_RADIUS = 5;
    private static final Random RNG = new Random();

    private Weapon equippedMatchWeapon;

    public Knight() {
    	super("Knight", 20, 1, 2, 8, 0, 80);
    	}

    public void equipMatchWeapon(Weapon weapon) {
        this.equippedMatchWeapon = weapon;
        if (weapon != null) {
            setAttackPower(4 + weapon.getDamage());
        }
    }

    public Weapon getEquippedMatchWeapon() { return equippedMatchWeapon; }

    /**
     * PDF §2.5.1: a Knight's melee attack deals 4 HP of damage (before the
     * defender's DEF is subtracted). Uses the attackPower field (4 by default,
     * or 4 + weapon ATK in Team Match) instead of attackPower + strength, which
     * was far too high (4 + 8 = 12) and killed the hero in 1-2 hits.
     */
    @Override
    public int calculateDamage() {
        return getAttackPower();
    }

    @Override
    public void decideAction(Hero hero, Map map, GameSession session) {
        if (hero == null || map == null || !isAlive() || !hero.isAlive()) return;
        int dRow = hero.getRow() - getRow();
        int dCol = hero.getCol() - getCol();

        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            System.out.println("[AI] Knight: Attacking hero!");
            // --- BUG-07 FIX: Route AI attacks through centralized CombatResolver ---
            com.kurawler.engine.CombatResolver.performMeleeAttack(this, hero);
            return;
        }

        double euclid = Math.sqrt(dRow * dRow + dCol * dCol);
        if (euclid <= SIGHT_RADIUS) {
            System.out.println("[AI] Knight: Chasing hero");
            if (Math.abs(dRow) >= Math.abs(dCol)) {
                if (!tryStep(Integer.signum(dRow), 0, map, session, hero))
                    tryStep(0, Integer.signum(dCol), map, session, hero);
            } else {
                if (!tryStep(0, Integer.signum(dCol), map, session, hero))
                    tryStep(Integer.signum(dRow), 0, map, session, hero);
            }
        } else {
            System.out.println("[AI] Knight: Roaming");
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(4)];
            tryStep(d[0], d[1], map, session, hero);
        }
    }

    private boolean tryStep(int dRow, int dCol, Map map, GameSession session, Hero hero) {
        if (dRow == 0 && dCol == 0) return false;
        int nr = getRow() + dRow;
        int nc = getCol() + dCol;
        GridCell target = map.getCell(nr, nc);
        
        if (target == null || target.isBlocked()) return false;
        
        moveToCell(target);
        return true;
    }

    public int getSightRadius() { return SIGHT_RADIUS; }
}