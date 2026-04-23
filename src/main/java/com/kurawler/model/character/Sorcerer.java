package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ranged enemy. Spec: 10HP, teleports, shoots projectiles dealing 8HP.
 */
public class Sorcerer extends Enemy {

    private static final float TELEPORT_CHANCE = 0.5f;
    private static final int   ATTACK_COOLDOWN = 5;   // ticks
    private static final int   SPELL_DAMAGE    = 8;   // spec
    private static final Random RNG = new Random();

    private int ticksSinceLastAttack = 0;
    private Projectile lastSpell;

    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 5, 60, 40);
        // HP=10, ATK=8 (projectile damage), DEF=0 (no armor),
        // STR=5, Mana=60, Energy=40
    }

    /**
     * Per-turn AI:
     *  - 50% chance to teleport to a random walkable, unoccupied cell
     *  - every ATTACK_COOLDOWN ticks, cast a spell at the hero (8 HP, mitigated by DEF)
     */
    public void decideAction(Hero hero, Map map, GameSession session) {
        if (hero == null || map == null || !isAlive() || !hero.isAlive()) return;

        if (RNG.nextFloat() < TELEPORT_CHANCE) {
            teleportToRandomWalkable(map, session, hero);
        }

        ticksSinceLastAttack++;
        if (ticksSinceLastAttack >= ATTACK_COOLDOWN) {
            castSpell(hero);
            ticksSinceLastAttack = 0;
        }
    }

    /**
     * Casts a spell at the hero. Builds a Projectile pointing at the hero
     * (consumed by the engine for animation/collision) and applies damage.
     * Returns the Projectile so the engine can track it; null if no LoS.
     */
    public Projectile castSpell(Hero hero) {
        if (hero == null || !hero.isAlive()) return null;
        int dr = Integer.signum(hero.getRow() - getRow());
        int dc = Integer.signum(hero.getCol() - getCol());
        Projectile p = new Projectile("Arcane Bolt", "Crackling magic energy",
                                      SPELL_DAMAGE, dr, dc);
        // Apply damage immediately (mitigated by hero DEF inside takeDamage).
        // When Batuhan's engine wires real projectile travel, replace this with
        // engine.spawnProjectile(p, getRow(), getCol()) and remove the direct hit.
        hero.takeDamage(SPELL_DAMAGE);
        this.lastSpell = p;
        return p;
    }

    private void teleportToRandomWalkable(Map map, GameSession session, Hero hero) {
        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || !cell.isWalkable()) continue;
                if (hero.getRow() == r && hero.getCol() == c) continue;
                if (session != null && session.getEnemyAt(r, c) != null) continue;
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) return;
        GridCell pick = candidates.get(RNG.nextInt(candidates.size()));
        setPosition(pick.getRow(), pick.getCol());
    }

    public float getTeleportChance() { return TELEPORT_CHANCE; }
    public int getAttackCooldown()   { return ATTACK_COOLDOWN; }
    public int getSpellDamage()      { return SPELL_DAMAGE; }
    public Projectile getLastSpell() { return lastSpell; }
}
