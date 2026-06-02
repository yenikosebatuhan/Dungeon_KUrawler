package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.List;

/**
 * Team Match game loop.
 *
 * Enemy AI is intentionally delegated to each enemy's own decideAction() so
 * that combat dynamics stay in sync with classic mode automatically.
 * Only weapon-pickup (a Team Match-specific mechanic) lives here.
 */
public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    /** Knight pace: 60 frames/step, sped up 50% → 60 / 1.5 = 40 frames per step. */
    private static final int ENEMY_TICK_INTERVAL     = 40;
    /** Match classic GameLoop.PROJECTILE_TICK_INTERVAL (14) so projectiles travel at the same speed. */
    private static final int PROJECTILE_TICK_INTERVAL = 14;

    private int enemyTickCounter       = 0;
    private int projectileTickCounter  = 0;
    private long playTimeAccumulatorMs = 0;

    private final List<Projectile> activeProjectiles = new ArrayList<>();

    /**
     * @requires session != null
     * @modifies this.session, this.running, this.activeProjectiles
     * @effects Stores the session and initialises running to false.
     */
    public TeamMatchGameLoop(TeamMatchSession session) {
        this.session = session;
        this.running = false;
    }

    /**
     * @requires none
     * @modifies this.running
     * @effects Sets running to true.
     */
    public void start() { this.running = true;  }

    /**
     * @requires none
     * @modifies this.running
     * @effects Sets running to false.
     */
    public void stop()  { this.running = false; }

    /**
     * @requires deltaMs >= 0
     * @modifies session.characters (health via attacks), this.activeProjectiles,
     *           this.enemyTickCounter, this.projectileTickCounter
     * @effects No-op if not running or session mode is not "TeamMatch". Otherwise
     *          runs enemy AI every ENEMY_TICK_INTERVAL frames, moves projectiles
     *          every PROJECTILE_TICK_INTERVAL frames, cleans up dead characters,
     *          and stops the loop when win condition is met.
     */
    public void update(long deltaMs) {
        if (!running) return;
        if (!"TeamMatch".equals(session.getCurrentMode())) return;

        updatePlayTimer(deltaMs);
        updateSorcererTimedAbilities(deltaMs);

        if (++enemyTickCounter >= ENEMY_TICK_INTERVAL) {
            enemyTickCounter = 0;
            updateAllEnemies();
        }

        if (++projectileTickCounter >= PROJECTILE_TICK_INTERVAL) {
            projectileTickCounter = 0;
            updateProjectiles();
        }

        cleanUpDead();

        if (session.checkWinCondition()) {
            stop();
        }
    }

    private void updateSorcererTimedAbilities(long deltaMs) {
        if (deltaMs <= 0 || session.getMap() == null) {
            return;
        }
        Map map = session.getMap();
        for (Character member : getAllCharacters()) {
            if (!(member instanceof Sorcerer sorcerer) || !sorcerer.isAlive()) {
                continue;
            }
            Character target = session.findNearestEnemy(member);
            Character avoid = target != null && target.isAlive() ? target : null;
            sorcerer.tickTeleport(deltaMs, map, avoid);
            if (target != null && target.isAlive()) {
                sorcerer.tickSpellAttack(deltaMs, target, map, session);
            }
        }
    }

    /**
     * For every non-hero character on both teams:
     * 1. If it's a Knight without a weapon, try to pick one up first.
     * 2. Delegate AI entirely to enemy.decideAction(nearestOpponent) —
     *    the same code path used by classic GameLoop.
     */
    private void updateAllEnemies() {
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Enemy)) continue;
            Enemy enemy = (Enemy) member;

            if (enemy instanceof Sorcerer) continue;

            if (enemy instanceof Knight knight) {
                maybePickUpWeapon(knight);
                maybePickUpPotion(knight);

                // Lowest HP priority: seek health potion when below 40% HP
                if (knight.getHealth() < 0.4 * knight.getMaxHealth()) {
                    GridCell potionCell = session.findNearestHealPotionCell(knight);
                    if (potionCell != null) {
                        moveKnightToward(knight, potionCell.getRow(), potionCell.getCol());
                        continue;
                    }
                }

                if (knight.getEquippedMatchWeapon() == null) {
                    GridCell weaponCell = session.findNearestWeaponCell(knight);
                    if (weaponCell != null) {
                        moveKnightToward(knight, weaponCell.getRow(), weaponCell.getCol());
                    }
                    continue;
                }
            }

            Character target = session.findNearestEnemy(member);
            if (target != null && target.isAlive()) {
                enemy.decideAction(target, session.getMap(), session);
            }
        }
    }

    private void moveKnightToward(Knight knight, int targetRow, int targetCol) {
        int dRow = targetRow - knight.getRow();
        int dCol = targetCol - knight.getCol();
        Map map = session.getMap();
        if (Math.abs(dRow) >= Math.abs(dCol)) {
            if (!knightStep(knight, Integer.signum(dRow), 0, map))
                knightStep(knight, 0, Integer.signum(dCol), map);
        } else {
            if (!knightStep(knight, 0, Integer.signum(dCol), map))
                knightStep(knight, Integer.signum(dRow), 0, map);
        }
    }

    // isBlocked() rejects walls and furniture/props but not dropped items, so
    // knights can still step onto weapon/potion tiles to pick them up.
    private boolean knightStep(Knight knight, int dRow, int dCol, Map map) {
        if (dRow == 0 && dCol == 0) return false;
        int nr = knight.getRow() + dRow;
        int nc = knight.getCol() + dCol;
        GridCell cell = map.getCell(nr, nc);
        if (cell == null || cell.isBlocked()) return false;
        knight.moveToCell(cell);
        return true;
    }

    private void maybePickUpPotion(Knight knight) {
        GridCell cell = session.getMap().getCell(knight.getRow(), knight.getCol());
        if (cell == null) return;
        for (GameObject obj : new ArrayList<>(cell.getObjects())) {
            if (obj instanceof Consumable cons && cons.getHealAmount() > 0) {
                knight.heal(cons.getHealAmount());
                cell.removeObject(obj);
                break;
            }
        }
    }

    /** Picks up a weapon from the Knight's current cell if available. */
    private void maybePickUpWeapon(Knight knight) {
        if (knight.getEquippedMatchWeapon() != null) return;
        GridCell cell = session.getMap().getCell(knight.getRow(), knight.getCol());
        if (cell == null) return;
        for (GameObject obj : new ArrayList<>(cell.getObjects())) {
            if (obj instanceof Weapon) {
                knight.equipMatchWeapon((Weapon) obj);
                cell.removeObject(obj);
                break;
            }
        }
    }

    /**
     * Mirrors classic GameLoop.updateProjectiles():
     * drains session.getProjectiles() (added by Sorcerer.tickSpellAttack) and
     * moves each projectile one step, applying damage on hit.
     */
    private void updateProjectiles() {
        // Drain session queue — same as classic GameLoop
        activeProjectiles.addAll(session.getProjectiles());
        session.getProjectiles().clear();

        List<Projectile> snapshot = new ArrayList<>(activeProjectiles);
        for (Projectile p : snapshot) {
            int newRow = p.getPositionRow() + p.getDirectionRow();
            int newCol = p.getPositionCol() + p.getDirectionCol();

            GridCell cell = session.getMap().getCell(newRow, newCol);
            if (GameLoop.blocksProjectile(cell)) {
                activeProjectiles.remove(p);
                continue;
            }

            p.setPosition(newRow, newCol);
            p.incrementStepsTraveled();
            if (p.hasExceededRange()) {
                activeProjectiles.remove(p);
                continue;
            }

            Character occupant = cell.getOccupyingCharacter();
            if (occupant != null && occupant.isAlive()) {
                // Determine which team fired this projectile
                Team firingTeam = findFiringTeam(p);
                Team targetTeam = session.getTeamForCharacter(occupant);
                // Only damage opponents
                if (firingTeam == null || !firingTeam.equals(targetTeam)) {
                    CombatResolver.applyProjectileDamage(p, occupant);
                    activeProjectiles.remove(p);
                    if (!occupant.isAlive()) {
                        cell.setOccupyingCharacter(null);
                    }
                }
            }
        }
    }

    /**
     * Best-effort: find which team fired a projectile by checking position proximity
     * to team members. Falls back to null (hits anyone) if undetermined.
     */
    private Team findFiringTeam(Projectile p) {
        // "Hero Bolt" is always Team B
        if ("Hero Bolt".equals(p.getName()) || "Arrow".equals(p.getName()) || "Bomb".equals(p.getName())
                || "Staff Bolt".equals(p.getName())) {
            return session.getTeamB();
        }
        // Arcane Bolt — identify by which sorcerer is closest to the origin
        // Since we can't store the owner in Projectile easily, treat all Arcane Bolts as
        // coming from "the other team" relative to Hero (conservative — hits anyone not Hero).
        return null;
    }

    private void cleanUpDead() {
        for (Character c : getAllCharacters()) {
            if (!c.isAlive()) {
                GridCell cell = session.getMap().getCell(c.getRow(), c.getCol());
                if (cell != null && cell.getOccupyingCharacter() == c) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private List<Character> getAllCharacters() {
        List<Character> all = new ArrayList<>();
        all.addAll(session.getTeamA().getMembers());
        all.addAll(session.getTeamB().getMembers());
        return all;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true if the loop is currently running.
     */
    private void updatePlayTimer(long deltaMs) {
        if (deltaMs <= 0) return;
        playTimeAccumulatorMs += deltaMs;
        if (playTimeAccumulatorMs >= 1000) {
            int elapsed = (int) (playTimeAccumulatorMs / 1000);
            session.setPlayDurationTimer(session.getPlayDurationTimer() + elapsed);
            playTimeAccumulatorMs %= 1000;
        }
    }

    public boolean isRunning()                        { return running; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the live list of active projectiles (not a copy).
     */
    public List<Projectile> getActiveProjectiles()    { return activeProjectiles; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the TeamMatchSession this loop is bound to.
     */
    public TeamMatchSession getSession()              { return session; }
}
