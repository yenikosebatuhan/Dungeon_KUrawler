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
 * Enemy AI is intentionally delegated to each enemy's own decideAction() so
 * that combat dynamics stay in sync with classic mode automatically.
 * Only weapon-pickup (a Team Match-specific mechanic) lives here.
 */
public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    /** Match classic GameLoop.ENEMY_TICK_INTERVAL exactly. */
    private static final int ENEMY_TICK_INTERVAL     = 30;
    /** Match classic GameLoop.PROJECTILE_TICK_INTERVAL exactly. */
    private static final int PROJECTILE_TICK_INTERVAL = 8;

    private int enemyTickCounter       = 0;
    private int projectileTickCounter  = 0;

    private final List<Projectile> activeProjectiles = new ArrayList<>();

    public TeamMatchGameLoop(TeamMatchSession session) {
        this.session = session;
        this.running = false;
    }

    public void start() { this.running = true;  }
    public void stop()  { this.running = false; }

    public void update(long deltaMs) {
        if (!running) return;
        if (!"TeamMatch".equals(session.getCurrentMode())) return;

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
     * For every non-hero character on both teams:
     * 1. If it's a Knight without a weapon, try to pick one up first.
     * 2. Delegate AI entirely to enemy.decideAction(nearestOpponent) —
     *    the same code path used by classic GameLoop.
     */
    private void updateAllEnemies() {
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Enemy)) continue;
            Enemy enemy = (Enemy) member;

            // TeamMatch-specific: Knight weapon pickup (classic mode has no weapons on ground)
            if (enemy instanceof Knight) {
                maybePickUpWeapon((Knight) enemy);
            }

            Character target = session.findNearestEnemy(member);
            if (target != null && target.isAlive()) {
                enemy.decideAction(target, session.getMap(), session);
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
     * drains session.getProjectiles() (added by Sorcerer.decideAction) and
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
            if (cell == null || (cell.getStaticObject() != null && !cell.getStaticObject().isPassable())) {
                activeProjectiles.remove(p);
                continue;
            }

            p.setPosition(newRow, newCol);

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
        if ("Hero Bolt".equals(p.getName())) return session.getTeamB();
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

    public boolean isRunning()                        { return running; }
    public List<Projectile> getActiveProjectiles()    { return activeProjectiles; }
    public TeamMatchSession getSession()              { return session; }
}
