package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PDF §2.5.2 — 10 HP, no armor (DEF 0). Fires a line projectile at the hero every 5 s;
 * moves only by teleport (7 s, 50% chance to a random empty floor cell). Always knows
 * the hero's position for aiming; walls and large floor objects block the bolt in flight.
 */
public class Sorcerer extends Enemy {
    private static final float TELEPORT_CHANCE = 0.5f;
    /** Every 7 seconds, 50% chance to teleport to a random empty walkable tile. */
    private static final long TELEPORT_INTERVAL_MS = 7000L;
    /** Magical projectile at most once every 5 real-world seconds. */
    public static final long SPELL_ATTACK_INTERVAL_MS = 5000L;
    private static final int SPELL_DAMAGE = 8;
    /** Max tiles the bolt travels after leaving the sorcerer's cell. */
    private static final int SPELL_RANGE = 5;
    private static final long CAST_ANIM_DURATION_MS = 450L;
    private static final Random RNG = new Random();

    private long spellAttackTimerMs = 0;
    private long teleportTimerMs = 0;
    private long castAnimUntilMs = 0;
    private Projectile lastSpell;

    /** PDF §2.5.2: marks whether this Sorcerer has already dropped its ring upon defeat. */
    private boolean ringDropped = false;

    /** 10 HP, ATK 8 (bolt damage), DEF 0, STR 0 per spec. */
    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 0, 60, 40);
    }

    public boolean isRingDropped() { return ringDropped; }

    public void markRingDropped() { this.ringDropped = true; }

    public long getSpellAttackTimerMs() { return spellAttackTimerMs; }

    public void setSpellAttackTimerMs(long spellAttackTimerMs) {
        this.spellAttackTimerMs = Math.max(0L, spellAttackTimerMs);
    }

    public long getTeleportTimerMs() { return teleportTimerMs; }

    public void setTeleportTimerMs(long teleportTimerMs) {
        this.teleportTimerMs = Math.max(0L, teleportTimerMs);
    }

    public boolean isCastAnimActive() {
        return System.currentTimeMillis() < castAnimUntilMs;
    }

    private void triggerCastAnim() {
        castAnimUntilMs = System.currentTimeMillis() + CAST_ANIM_DURATION_MS;
    }

    /**
     * §2.5.2: Sorcerer does not walk. Spell fire and teleport are driven by
     * {@link #tickSpellAttack} and {@link #tickTeleport} from the game loop.
     */
    @Override
    public void decideAction(Character target, Map map, GameSession session) {
        // No movement — teleport-only.
    }

    /**
     * §2.5.2: launch a bolt toward the hero every {@link #SPELL_ATTACK_INTERVAL_MS}.
     * Hero position is always known; line-of-sight is not required to start a cast.
     */
    public void tickSpellAttack(long deltaMs, Character target, Map map, GameSession session) {
        if (deltaMs <= 0 || target == null || map == null || session == null
                || !isAlive() || !target.isAlive()) {
            return;
        }
        spellAttackTimerMs += deltaMs;
        if (spellAttackTimerMs < SPELL_ATTACK_INTERVAL_MS) {
            return;
        }
        spellAttackTimerMs -= SPELL_ATTACK_INTERVAL_MS;

        Projectile p = castSpellToward(target);
        if (p != null) {
            if (session instanceof TeamMatchSession tm) {
                Team team = tm.getTeamForCharacter(this);
                if (team != null) {
                    p.setSourceTeamName(team.getName());
                }
            }
            session.addProjectile(p);
            triggerCastAnim();
        }
    }

    /**
     * §2.5.2: every 7 s, 50% chance to teleport to a random empty floor cell.
     */
    public void tickTeleport(long deltaMs, Map map, Character avoid, GameSession session) {
        if (deltaMs <= 0 || map == null || !isAlive()) {
            return;
        }
        teleportTimerMs += deltaMs;
        while (teleportTimerMs >= TELEPORT_INTERVAL_MS) {
            teleportTimerMs -= TELEPORT_INTERVAL_MS;
            if (RNG.nextFloat() < TELEPORT_CHANCE) {
                teleportToRandomEmptyCell(map, avoid, session);
            }
        }
    }

    /**
     * Aims at the hero's known position (8-direction). Walls/objects block the bolt in
     * {@link com.kurawler.engine.GameLoop#updateProjectiles()}.
     */
    public Projectile castSpellToward(Character target) {
        if (target == null || !target.isAlive()) {
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

    private void teleportToRandomEmptyCell(Map map, Character avoid, GameSession session) {
        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || !cell.isEnemyWalkable()) {
                    continue;
                }
                if (avoid != null && avoid.getRow() == r && avoid.getCol() == c) {
                    continue;
                }
                if (session != null && session.isActiveShadowCloneAt(r, c)) {
                    continue;
                }
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        int oldRow = getRow();
        int oldCol = getCol();
        GridCell dest = candidates.get(RNG.nextInt(candidates.size()));
        moveToCell(dest);
        // Editor/load paths can leave a stale occupant on the old tile when currentCell
        // was not wired; without this, GameView snaps the sorcerer back every frame.
        if (oldRow >= 0 && oldCol >= 0) {
            GridCell stale = map.getCell(oldRow, oldCol);
            if (stale != null && stale != dest && stale.getOccupyingCharacter() == this) {
                stale.setOccupyingCharacter(null);
            }
        }
    }

    public float getTeleportChance() { return TELEPORT_CHANCE; }

    public int getAttackCooldownSeconds() {
        return (int) (SPELL_ATTACK_INTERVAL_MS / 1000L);
    }

    public int getSpellDamage() { return SPELL_DAMAGE; }

    public Projectile getLastSpell() { return lastSpell; }
}
