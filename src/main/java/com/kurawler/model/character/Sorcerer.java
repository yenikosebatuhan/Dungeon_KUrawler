package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Sorcerer extends Enemy {
    private static final float TELEPORT_CHANCE = 0.5f;
    private static final int ATTACK_COOLDOWN = 5;
    private static final int SPELL_DAMAGE = 8;
    private static final int SPELL_RANGE = 5;
    private static final Random RNG = new Random();

    private int ticksSinceLastAttack = 0;
    private Projectile lastSpell;

    /** PDF §2.5.2: marks whether this Sorcerer has already dropped its ring upon defeat. */
    private boolean ringDropped = false;

    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 5, 60, 40);
    }

    public boolean isRingDropped() { return ringDropped; }

    public void markRingDropped() { this.ringDropped = true; }

    @Override
    public void decideAction(Character target, Map map, GameSession session) {
        if (target == null || map == null || !isAlive() || !target.isAlive()) {
            return;
        }

        ticksSinceLastAttack++;

        int dist = Math.abs(target.getRow() - getRow()) + Math.abs(target.getCol() - getCol());
        boolean teleported = false;
        if (dist <= 1 && RNG.nextFloat() < TELEPORT_CHANCE) {
            teleported = teleportToRandomWalkable(map, target);
        }

        if (!teleported && ticksSinceLastAttack >= ATTACK_COOLDOWN && hasLineOfSight(target, map)) {
            Projectile p = castSpell(target, map);
            if (p != null && session != null) {
                session.addProjectile(p);
            }
            ticksSinceLastAttack = 0;
        }
    }

    public Projectile castSpell(Character target, Map map) {
        if (target == null || !target.isAlive() || map == null) {
            return null;
        }
        if (!hasLineOfSight(target, map)) {
            return null;
        }
        int dr = Integer.signum(target.getRow() - getRow());
        int dc = Integer.signum(target.getCol() - getCol());
        if (dr == 0 && dc == 0) {
            return null;
        }
        Projectile p = new Projectile("Arcane Bolt", "Crackling magic energy", SPELL_DAMAGE, dr, dc);
        p.setPosition(getRow() + dr, getCol() + dc);
        p.setMaxRange(SPELL_RANGE);
        this.lastSpell = p;
        return p;
    }

    private boolean hasLineOfSight(Character target, Map map) {
        int r = getRow();
        int c = getCol();
        int dr = Integer.signum(target.getRow() - r);
        int dc = Integer.signum(target.getCol() - c);
        if (dr == 0 && dc == 0) {
            return false;
        }
        if (dr != 0 && dc != 0
                && Math.abs(target.getRow() - r) != Math.abs(target.getCol() - c)) {
            return false;
        }
        r += dr;
        c += dc;
        while (r != target.getRow() || c != target.getCol()) {
            GridCell cell = map.getCell(r, c);
            if (cell == null) {
                return false;
            }
            if (cell.getStaticObject() != null && !cell.getStaticObject().isPassable()) {
                return false;
            }
            r += dr;
            c += dc;
        }
        return true;
    }

    /** @return true only if the sorcerer actually moved to a new cell */
    private boolean teleportToRandomWalkable(Map map, Character avoid) {
        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || cell.isBlocked()) {
                    continue;
                }
                if (avoid != null && avoid.getRow() == r && avoid.getCol() == c) {
                    continue;
                }
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        moveToCell(candidates.get(RNG.nextInt(candidates.size())));
        return true;
    }

    public float getTeleportChance() { return TELEPORT_CHANCE; }

    public int getAttackCooldown() { return ATTACK_COOLDOWN; }

    public int getSpellDamage() { return SPELL_DAMAGE; }

    public Projectile getLastSpell() { return lastSpell; }
}
