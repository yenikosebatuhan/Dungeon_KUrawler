package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.Map;
import com.kurawler.model.object.Weapon;
import java.util.Random;

public class Knight extends Enemy {
    private static final int SIGHT_RADIUS = 5;
    private static final Random RNG = new Random();

    private Weapon equippedMatchWeapon;
    private boolean unlimitedVision = false;

    /**
     * @effects Creates a Knight with 20 HP, ATK 4 (PDF §2.5.1), DEF 1, STR 1, 80 energy.
     */
    public Knight() {
        super("Knight", 20, 4, 1, 1, 0, 80);
    }

    /**
     * @modifies this.equippedMatchWeapon
     * @effects Stores the equipped Team Match weapon (does not mutate base ATK;
     *          the weapon's damage is added on top in calculateDamage()).
     */
    public void equipMatchWeapon(Weapon weapon) {
        this.equippedMatchWeapon = weapon;
    }

    public Weapon getEquippedMatchWeapon() { return equippedMatchWeapon; }
    public void setUnlimitedVision(boolean v) { this.unlimitedVision = v; }

    /**
     * PDF §3: Knight melee deals (STR / 2 + ATK) before DEF. Team Match weapon ATK is included.
     */
    @Override
    public int calculateDamage() {
        return com.kurawler.engine.CombatResolver.calculateGeneratedDamage(this);
    }

    /**
     * PDF §2.5.1: hero is visible when {@code ceil(Euclidean distance) ≤ SIGHT_RADIUS}.
     */
    private static boolean withinSight(int dRow, int dCol) {
        double euclid = Math.sqrt((double) dRow * dRow + (double) dCol * dCol);
        return (int) Math.ceil(euclid) <= SIGHT_RADIUS;
    }

    /**
     * @effects If orthogonally adjacent to target, melee attack. Within sight range,
     *          steps toward target; otherwise moves randomly.
     */
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

        if (unlimitedVision || withinSight(dRow, dCol)) {
            moveToward(target.getRow(), target.getCol(), map, session);
        } else {
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(4)];
            tryEnemyStep(d[0], d[1], map, session);
        }
    }

    /** One step toward a map cell (weapon pickup, Team Match chase, etc.). */
    public void moveToward(int targetRow, int targetCol, Map map) {
        moveToward(targetRow, targetCol, map, null);
    }

    public void moveToward(int targetRow, int targetCol, Map map, GameSession session) {
        if (map == null || !isAlive()) {
            return;
        }
        int dRow = targetRow - getRow();
        int dCol = targetCol - getCol();
        if (dRow == 0 && dCol == 0) {
            return;
        }
        if (Math.abs(dRow) >= Math.abs(dCol)) {
            if (!tryEnemyStep(Integer.signum(dRow), 0, map, session)) {
                tryEnemyStep(0, Integer.signum(dCol), map, session);
            }
        } else {
            if (!tryEnemyStep(0, Integer.signum(dCol), map, session)) {
                tryEnemyStep(Integer.signum(dRow), 0, map, session);
            }
        }
    }

    public int getSightRadius() { return SIGHT_RADIUS; }
}
