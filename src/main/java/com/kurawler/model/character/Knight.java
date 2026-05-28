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
        super("Knight", 20, 1, 1, 1, 0, 80);
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
    public void decideAction(Character target, Map map, GameSession session) {
        if (target == null || map == null || !isAlive() || !target.isAlive()) return;
        int dRow = target.getRow() - getRow();
        int dCol = target.getCol() - getCol();

        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            com.kurawler.engine.CombatResolver.performMeleeAttack(this, target);
            return;
        }

        double euclid = Math.sqrt(dRow * dRow + dCol * dCol);
        if (euclid <= SIGHT_RADIUS) {
            if (Math.abs(dRow) >= Math.abs(dCol)) {
                if (!tryStep(Integer.signum(dRow), 0, map))
                    tryStep(0, Integer.signum(dCol), map);
            } else {
                if (!tryStep(0, Integer.signum(dCol), map))
                    tryStep(Integer.signum(dRow), 0, map);
            }
        } else {
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(4)];
            tryStep(d[0], d[1], map);
        }
    }

    private boolean tryStep(int dRow, int dCol, Map map) {
        if (dRow == 0 && dCol == 0) return false;
        int nr = getRow() + dRow;
        int nc = getCol() + dCol;
        GridCell cell = map.getCell(nr, nc);
        if (cell == null || cell.isBlocked()) return false;
        moveToCell(cell);
        return true;
    }

    public int getSightRadius() { return SIGHT_RADIUS; }
}