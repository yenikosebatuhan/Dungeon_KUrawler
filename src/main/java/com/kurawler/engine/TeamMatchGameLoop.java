package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.List;

/**
 * Team Match game loop.
 *
 * Sorcerer AI is delegated to Sorcerer.decideAction() (classic path).
 * Knight AI is handled here to satisfy Team Match rules:
 *   - Weaponless Knight steps toward the nearest available weapon; picks it up on arrival.
 *   - Armed Knight chases and attacks the nearest opponent with no sight-radius limit.
 * Classic Knight.decideAction() (5-cell sight radius) is never called in Team Match.
 */
public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    /** Match classic GameLoop.ENEMY_TICK_INTERVAL exactly. */
    private static final int ENEMY_TICK_INTERVAL     = 42;
    /** Match classic GameLoop.PROJECTILE_TICK_INTERVAL exactly. */
    private static final int PROJECTILE_TICK_INTERVAL = 11;

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

    /**
     * For every non-hero enemy on both teams:
     * - Knight: handled by teamMatchKnightTick (weapon-seek or armed-chase, no sight limit).
     * - Other enemies (Sorcerer): classic decideAction with projectile team-tagging.
     */
    private void updateAllEnemies() {
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Enemy)) continue;
            Enemy enemy = (Enemy) member;

            if (enemy instanceof Knight) {
                teamMatchKnightTick((Knight) enemy);
            } else {
                int queueBefore = session.getProjectiles().size();
                Character target = session.findNearestEnemy(member);
                if (target != null && target.isAlive()) {
                    enemy.decideAction(target, session.getMap(), session);
                }
                Team enemyTeam = session.getTeamForCharacter(member);
                List<Projectile> queue = session.getProjectiles();
                for (int i = queueBefore; i < queue.size(); i++) {
                    queue.get(i).setOwnerTeam(enemyTeam);
                }
            }
        }
    }

    /**
     * Team Match Knight AI for one tick:
     * - No weapon: step toward nearest available weapon, then pick up if arrived.
     * - Armed: step toward nearest opponent and attack when adjacent.
     *   No sight-radius cap (unlike classic Knight.decideAction).
     */
    private void teamMatchKnightTick(Knight knight) {
        if (!knight.isAlive()) return;
        maybePickUpWeapon(knight);

        if (knight.getEquippedMatchWeapon() == null) {
            GridCell weaponCell = findNearestWeaponCell(knight);
            if (weaponCell != null) {
                stepToward(knight, weaponCell.getRow(), weaponCell.getCol());
                maybePickUpWeapon(knight);
            } else {
                attackNearestEnemy(knight);
            }
        } else {
            attackNearestEnemy(knight);
        }
    }

    /** Move toward and attack (when adjacent) the nearest opposing team member. */
    private void attackNearestEnemy(Knight knight) {
        Character target = session.findNearestEnemy(knight);
        if (target == null || !target.isAlive()) return;
        int dRow = target.getRow() - knight.getRow();
        int dCol = target.getCol() - knight.getCol();
        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            CombatResolver.performMeleeAttack(knight, target);
        } else {
            stepToward(knight, target.getRow(), target.getCol());
        }
    }

    /** Returns the nearest map cell that still holds a Weapon object, or null. */
    GridCell findNearestWeaponCell(Knight knight) {
        com.kurawler.model.Map map = session.getMap();
        GridCell nearest = null;
        double minDist = Double.MAX_VALUE;
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) continue;
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof Weapon) {
                        double dist = Math.sqrt(
                            Math.pow(r - knight.getRow(), 2) + Math.pow(c - knight.getCol(), 2));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = cell;
                        }
                        break;
                    }
                }
            }
        }
        return nearest;
    }

    /** Step one cell toward (targetRow, targetCol), preferring the dominant axis. */
    private void stepToward(Character mover, int targetRow, int targetCol) {
        com.kurawler.model.Map map = session.getMap();
        int dRow = targetRow - mover.getRow();
        int dCol = targetCol - mover.getCol();
        if (dRow == 0 && dCol == 0) return;
        if (Math.abs(dRow) >= Math.abs(dCol)) {
            if (!stepTo(mover, mover.getRow() + Integer.signum(dRow), mover.getCol(), map))
                stepTo(mover, mover.getRow(), mover.getCol() + Integer.signum(dCol), map);
        } else {
            if (!stepTo(mover, mover.getRow(), mover.getCol() + Integer.signum(dCol), map))
                stepTo(mover, mover.getRow() + Integer.signum(dRow), mover.getCol(), map);
        }
    }

    private boolean stepTo(Character mover, int nr, int nc, com.kurawler.model.Map map) {
        GridCell cell = map.getCell(nr, nc);
        if (cell == null || cell.isBlocked()) return false;
        mover.moveToCell(cell);
        return true;
    }

    /** Picks up a weapon from the Knight's current cell if unarmed. */
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
     * drains session.getProjectiles() (added by Sorcerer.decideAction) and
     * moves each projectile one step, applying damage on hit.
     */
    private void updateProjectiles() {
        // Any projectile in the session queue without an owner was fired by the Hero (Team B).
        for (Projectile p : session.getProjectiles()) {
            if (p.getOwnerTeam() == null) {
                p.setOwnerTeam(session.getTeamB());
            }
        }
        activeProjectiles.addAll(session.getProjectiles());
        session.getProjectiles().clear();

        List<Projectile> snapshot = new ArrayList<>(activeProjectiles);
        for (Projectile p : snapshot) {
            int newRow = p.getPositionRow() + p.getDirectionRow();
            int newCol = p.getPositionCol() + p.getDirectionCol();

            GridCell cell = session.getMap().getCell(newRow, newCol);
            if (cell == null || (cell.getStaticObject() != null && !cell.getStaticObject().isPassable())) {
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
                Team firingTeam = p.getOwnerTeam();
                Team targetTeam = session.getTeamForCharacter(occupant);
                // Damage only when both teams are known and they differ — no friendly fire.
                if (firingTeam != null && targetTeam != null && !firingTeam.equals(targetTeam)) {
                    CombatResolver.applyProjectileDamage(p, occupant);
                    activeProjectiles.remove(p);
                    if (!occupant.isAlive()) {
                        cell.setOccupyingCharacter(null);
                    }
                }
            }
        }
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
    private static final long MS_PER_ENERGY_REGEN = 1000;
    private long energyRegenAccumulatorMs = 0;

    private void updatePlayTimer(long deltaMs) {
        if (deltaMs <= 0) return;
        playTimeAccumulatorMs += deltaMs;
        if (playTimeAccumulatorMs >= 1000) {
            int elapsed = (int) (playTimeAccumulatorMs / 1000);
            session.setPlayDurationTimer(session.getPlayDurationTimer() + elapsed);
            playTimeAccumulatorMs %= 1000;
        }
        updateEnergyRegen(deltaMs);
    }

    private void updateEnergyRegen(long deltaMs) {
        com.kurawler.model.character.Hero hero = session.getHero();
        if (hero == null || !hero.isAlive() || hero.getEnergy() >= hero.getMaxEnergy()) return;
        energyRegenAccumulatorMs += deltaMs;
        while (energyRegenAccumulatorMs >= MS_PER_ENERGY_REGEN
                && hero.getEnergy() < hero.getMaxEnergy()) {
            energyRegenAccumulatorMs -= MS_PER_ENERGY_REGEN;
            hero.restoreEnergy(1);
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
