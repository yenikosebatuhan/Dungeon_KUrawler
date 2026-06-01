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
    private boolean unlimitedVision = false;

    /**
     * @effects Creates a Knight with 20 HP, ATK 4 (PDF §2.5.1), DEF 1, STR 1, 80 energy.
     */
    public Knight() {
        super("Knight", 20, 4, 1, 1, 0, 80);
    }

    /**
     * @modifies this.equippedMatchWeapon, this.attackPower
     * @effects Stores the weapon and sets attackPower to 4 + weapon.getDamage().
     */
    public void equipMatchWeapon(Weapon weapon) {
        this.equippedMatchWeapon = weapon;
        if (weapon != null) {
            setAttackPower(4 + weapon.getDamage());
        }
    }

    public Weapon getEquippedMatchWeapon() { return equippedMatchWeapon; }
    public void setUnlimitedVision(boolean v) { this.unlimitedVision = v; }

    /**
     * PDF §2.5.1: Knight melee deals 4 HP before DEF (via attackPower, not STR stack).
     */
    @Override
    public int calculateDamage() {
        return getAttackPower();
    }

    /**
     * @effects If orthogonally adjacent to target, melee attack. Within SIGHT_RADIUS,
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

        double euclid = Math.sqrt(dRow * dRow + dCol * dCol);
        if (unlimitedVision || euclid <= SIGHT_RADIUS) {
            if (Math.abs(dRow) >= Math.abs(dCol)) {
                if (!tryStep(Integer.signum(dRow), 0, map)) {
                    tryStep(0, Integer.signum(dCol), map);
                }
            } else {
                if (!tryStep(0, Integer.signum(dCol), map)) {
                    tryStep(Integer.signum(dRow), 0, map);
                }
            }
        } else {
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            int[] d = dirs[RNG.nextInt(4)];
            tryStep(d[0], d[1], map);
        }
    }

    private boolean tryStep(int dRow, int dCol, Map map) {
        if (dRow == 0 && dCol == 0) {
            return false;
        }
        int nr = getRow() + dRow;
        int nc = getCol() + dCol;
        GridCell cell = map.getCell(nr, nc);
        // isBlocked() rejects walls, columns and furniture/props (searchable statics)
        // but not dropped items, so knights can still step onto weapon/potion tiles.
        if (cell == null || cell.isBlocked()) {
            return false;
        }
        moveToCell(cell);
        return true;
    }

    public int getSightRadius() { return SIGHT_RADIUS; }
}
