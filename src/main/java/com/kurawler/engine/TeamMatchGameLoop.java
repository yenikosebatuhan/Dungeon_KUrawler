package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.List;

/**
 * Team Match game loop.
 *
 * Enemy AI: knights seek weapons then opponents; sorcerers use timed spell/teleport.
 * Weapon pickup is handled when a knight steps onto a weapon tile.
 */
public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    /** Knight pace: 60 frames/step, sped up 50% → 60 / 1.5 = 40 frames per step. */
    private static final int ENEMY_TICK_INTERVAL     = 40;
    /** Match classic GameLoop.PROJECTILE_TICK_INTERVAL (14). */
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
            sorcerer.tickTeleport(deltaMs, map, avoid, session);
            if (target != null && target.isAlive()) {
                sorcerer.tickSpellAttack(deltaMs, target, map, session);
            }
        }
    }

    /**
     * Team Match AI:
     * <ul>
     *   <li>Knights without a weapon move toward the nearest ground weapon</li>
     *   <li>Knights with a weapon (and Sorcerers' opponents) chase the nearest enemy</li>
     * </ul>
     */
    private void updateAllEnemies() {
        Map map = session.getMap();
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Enemy)) {
                continue;
            }
            Enemy enemy = (Enemy) member;

            if (enemy instanceof Knight knight) {
                maybePickUpWeapon(knight);
                if (knight.getEquippedMatchWeapon() == null) {
                    GridCell weaponCell = session.findNearestWeaponCell(knight);
                    if (weaponCell != null) {
                        knight.moveToward(weaponCell.getRow(), weaponCell.getCol(), map);
                        continue;
                    }
                }
            }

            if (enemy instanceof Sorcerer) {
                continue;
            }

            Character target = session.findNearestEnemy(member);
            if (target != null && target.isAlive()) {
                enemy.decideAction(target, map, session);
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
                Team firingTeam = resolveProjectileTeam(p);
                Team targetTeam = session.getTeamForCharacter(occupant);
                if (firingTeam == null || targetTeam == null) {
                    continue;
                }
                if (firingTeam.equals(targetTeam)) {
                    continue;
                }
                CombatResolver.applyProjectileDamage(p, occupant);
                activeProjectiles.remove(p);
                if (!occupant.isAlive()) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private Team resolveProjectileTeam(Projectile p) {
        String teamName = p.getSourceTeamName();
        if (teamName != null) {
            if (teamName.equals(session.getTeamA().getName())) {
                return session.getTeamA();
            }
            if (teamName.equals(session.getTeamB().getName())) {
                return session.getTeamB();
            }
        }
        if ("Hero Bolt".equals(p.getName()) || "Arrow".equals(p.getName()) || "Bomb".equals(p.getName())
                || "Staff Bolt".equals(p.getName()) || "Weapon Strike".equals(p.getName())) {
            return session.getTeamB();
        }
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
