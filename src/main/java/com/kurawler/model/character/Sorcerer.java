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
    private static final Random RNG = new Random();

    private int ticksSinceLastAttack = 0;
    private Projectile lastSpell;

    /** PDF §2.5.2: marks whether this Sorcerer has already dropped its ring upon defeat. */
    private boolean ringDropped = false;

    /**
     * @requires none
     * @modifies this (via Enemy/Character constructors)
     * @effects Creates a Sorcerer with 10 HP, ATK 8, DEF 0, STR 5, 60 mana, 40 energy.
     */
    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 5, 60, 40);
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true once the post-defeat ring drop has been handled.
     */
    public boolean isRingDropped() { return ringDropped; }

    /**
     * @requires none
     * @modifies this.ringDropped
     * @effects Sets ringDropped to true so the ring is dropped at most once.
     */
    public void markRingDropped() { this.ringDropped = true; }

    /**
     * @requires map != null
     * @modifies this.row, this.col, this.currentCell (teleport),
     *           this.ticksSinceLastAttack, session.projectiles (if spell cast)
     * @effects Increments tick counter. If target is adjacent, may teleport away
     *          with TELEPORT_CHANCE probability. Otherwise if ATTACK_COOLDOWN ticks
     *          have elapsed, casts a spell toward target and adds the projectile to
     *          the session.
     */
    @Override
    public void decideAction(Character target, Map map, GameSession session) {
        if (target == null || map == null || !isAlive() || !target.isAlive()) return;

        ticksSinceLastAttack++;

        int dist = Math.abs(target.getRow() - getRow()) + Math.abs(target.getCol() - getCol());
        boolean teleported = false;
        if (dist <= 1 && RNG.nextFloat() < TELEPORT_CHANCE) {
            teleportToRandomWalkable(map, target);
            teleported = true;
        }

        if (!teleported && ticksSinceLastAttack >= ATTACK_COOLDOWN) {
            Projectile p = castSpell(target, map);
            if (p != null && session != null) {
                session.addProjectile(p);
            }
            ticksSinceLastAttack = 0;
        }
    }

    /**
     * @requires target != null && map != null
     * @modifies this.lastSpell
     * @effects Creates an Arcane Bolt projectile aimed toward target if there is
     *          line of sight; returns null otherwise. Stores result in lastSpell.
     */
    public Projectile castSpell(Character target, Map map) {
        if (target == null || !target.isAlive() || map == null) return null;
        if (!hasLineOfSight(target, map)) return null;
        int dr = Integer.signum(target.getRow() - getRow());
        int dc = Integer.signum(target.getCol() - getCol());
        Projectile p = new Projectile("Arcane Bolt", "Crackling magic energy", SPELL_DAMAGE, dr, dc);
        p.setPosition(getRow() + dr, getCol() + dc);
        this.lastSpell = p;
        return p;
    }

    private boolean hasLineOfSight(Character target, Map map) {
        int r = getRow(), c = getCol();
        int dr = Integer.signum(target.getRow() - r);
        int dc = Integer.signum(target.getCol() - c);
        if (dr == 0 && dc == 0) return false;
        if (dr != 0 && dc != 0 && Math.abs(target.getRow() - r) != Math.abs(target.getCol() - c)) return false;
        r += dr; c += dc;
        while (r != target.getRow() || c != target.getCol()) {
            GridCell cell = map.getCell(r, c);
            if (cell == null) return false;
            if (cell.getStaticObject() != null && !cell.getStaticObject().isPassable()) return false;
            r += dr; c += dc;
        }
        return true;
    }

    private void teleportToRandomWalkable(Map map, Character avoid) {
        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || cell.isBlocked()) continue;
                if (avoid != null && avoid.getRow() == r && avoid.getCol() == c) continue;
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) return;
        moveToCell(candidates.get(RNG.nextInt(candidates.size())));
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the probability of teleporting when the target is adjacent.
     */
    public float getTeleportChance() { return TELEPORT_CHANCE; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the number of ticks between spell casts.
     */
    public int getAttackCooldown() { return ATTACK_COOLDOWN; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the base damage of the Sorcerer's spell.
     */
    public int getSpellDamage() { return SPELL_DAMAGE; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the most recently cast Projectile, or null if none cast yet.
     */
    public Projectile getLastSpell() { return lastSpell; }
}
