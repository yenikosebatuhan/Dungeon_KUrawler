package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.Team;
import com.kurawler.model.TeamMatchSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    private static final int AI_TICK_INTERVAL = 12;
    private static final int SORCERER_ATTACK_INTERVAL = 30;
    private static final int SORCERER_TELEPORT_INTERVAL = 42;
    private static final Random RNG = new Random();

    private int aiTickCounter = 0;
    private int sorcererAttackCounter = 0;
    private int sorcererTeleportCounter = 0;
    private final List<Projectile> activeProjectiles = new ArrayList<>();

    public TeamMatchGameLoop(TeamMatchSession session) {
        this.session = session;
        this.running = false;
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public void update(long deltaMs) {
        if (!running) return;
        if (!"TeamMatch".equals(session.getCurrentMode())) return;

        aiTickCounter++;
        sorcererAttackCounter++;
        sorcererTeleportCounter++;

        if (aiTickCounter >= AI_TICK_INTERVAL) {
            aiTickCounter = 0;
            updateAllKnights();
        }

        if (sorcererAttackCounter >= SORCERER_ATTACK_INTERVAL) {
            sorcererAttackCounter = 0;
            updateSorcererAttacks();
        }

        if (sorcererTeleportCounter >= SORCERER_TELEPORT_INTERVAL) {
            sorcererTeleportCounter = 0;
            updateSorcererTeleports();
        }

        updateProjectiles();
        cleanUpDead();

        if (session.checkWinCondition()) {
            stop();
        }
    }

    private void updateAllKnights() {
        updateTeamKnights(session.getTeamA());
        updateTeamKnights(session.getTeamB());
    }

    private void updateTeamKnights(Team team) {
        for (Character member : team.getMembers()) {
            if (!member.isAlive()) continue;
            if (member instanceof Hero) continue;
            if (member instanceof Knight) {
                updateKnightAI((Knight) member);
            }
        }
    }

    private void updateKnightAI(Knight knight) {
        if (knight.getEquippedMatchWeapon() == null) {
            seekAndPickWeapon(knight);
        } else {
            chaseAndAttackNearest(knight);
        }
    }

    private void seekAndPickWeapon(Knight knight) {
        Weapon nearest = session.findNearestWeapon(knight);
        if (nearest == null) {
            chaseAndAttackNearest(knight);
            return;
        }

        GridCell weaponCell = findCellWithWeapon(nearest);
        if (weaponCell == null) {
            chaseAndAttackNearest(knight);
            return;
        }

        if (knight.getRow() == weaponCell.getRow() && knight.getCol() == weaponCell.getCol()) {
            knight.equipMatchWeapon(nearest);
            weaponCell.removeObject(nearest);
            return;
        }

        moveTowards(knight, weaponCell.getRow(), weaponCell.getCol());

        GridCell currentCell = session.getMap().getCell(knight.getRow(), knight.getCol());
        if (currentCell != null) {
            for (GameObject obj : new ArrayList<>(currentCell.getObjects())) {
                if (obj instanceof Weapon && knight.getEquippedMatchWeapon() == null) {
                    knight.equipMatchWeapon((Weapon) obj);
                    currentCell.removeObject(obj);
                    break;
                }
            }
        }
    }

    private void chaseAndAttackNearest(Knight knight) {
        Character target = session.findNearestEnemy(knight);
        if (target == null || !target.isAlive()) return;

        int dRow = target.getRow() - knight.getRow();
        int dCol = target.getCol() - knight.getCol();

        if (Math.abs(dRow) + Math.abs(dCol) == 1) {
            CombatResolver.performMeleeAttack(knight, target);
            if (!target.isAlive()) {
                GridCell cell = session.getMap().getCell(target.getRow(), target.getCol());
                if (cell != null) cell.setOccupyingCharacter(null);
            }
        } else {
            moveTowards(knight, target.getRow(), target.getCol());
        }
    }

    private void moveTowards(Character character, int targetRow, int targetCol) {
        int dRow = Integer.signum(targetRow - character.getRow());
        int dCol = Integer.signum(targetCol - character.getCol());
        Map map = session.getMap();

        if (Math.abs(targetRow - character.getRow()) >= Math.abs(targetCol - character.getCol())) {
            if (!tryMove(character, dRow, 0, map)) {
                tryMove(character, 0, dCol, map);
            }
        } else {
            if (!tryMove(character, 0, dCol, map)) {
                tryMove(character, dRow, 0, map);
            }
        }
    }

    private boolean tryMove(Character character, int dRow, int dCol, Map map) {
        if (dRow == 0 && dCol == 0) return false;
        int nr = character.getRow() + dRow;
        int nc = character.getCol() + dCol;
        GridCell target = map.getCell(nr, nc);
        if (target == null || target.isBlocked()) return false;
        character.moveToCell(target);
        return true;
    }

    private void updateSorcererAttacks() {
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Sorcerer)) continue;
            Sorcerer sorcerer = (Sorcerer) member;
            Character target = session.findNearestEnemy(sorcerer);
            if (target == null || !target.isAlive()) continue;

            int dr = Integer.signum(target.getRow() - sorcerer.getRow());
            int dc = Integer.signum(target.getCol() - sorcerer.getCol());
            if (dr == 0 && dc == 0) continue;

            Projectile p = new Projectile("Arcane Bolt", "Magic projectile",
                    sorcerer.getSpellDamage(), dr, dc);
            p.setPosition(sorcerer.getRow() + dr, sorcerer.getCol() + dc);
            activeProjectiles.add(p);
        }
    }

    private void updateSorcererTeleports() {
        Map map = session.getMap();
        for (Character member : getAllCharacters()) {
            if (!member.isAlive() || !(member instanceof Sorcerer)) continue;
            if (RNG.nextFloat() >= 0.5f) continue;

            List<GridCell> candidates = new ArrayList<>();
            for (int r = 0; r < map.getHeight(); r++) {
                for (int c = 0; c < map.getWidth(); c++) {
                    GridCell cell = map.getCell(r, c);
                    if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
                        candidates.add(cell);
                    }
                }
            }
            if (!candidates.isEmpty()) {
                member.moveToCell(candidates.get(RNG.nextInt(candidates.size())));
            }
        }
    }

    private void updateProjectiles() {
        Map map = session.getMap();
        List<Projectile> snapshot = new ArrayList<>(activeProjectiles);
        for (Projectile p : snapshot) {
            int newRow = p.getPositionRow() + p.getDirectionRow();
            int newCol = p.getPositionCol() + p.getDirectionCol();

            GridCell cell = map.getCell(newRow, newCol);
            if (cell == null || (cell.getStaticObject() != null && !cell.getStaticObject().isPassable())) {
                activeProjectiles.remove(p);
                continue;
            }

            p.setPosition(newRow, newCol);

            Character occupant = cell.getOccupyingCharacter();
            if (occupant != null && occupant.isAlive()) {
                CombatResolver.applyProjectileDamage(p, occupant);
                activeProjectiles.remove(p);
                if (!occupant.isAlive()) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private void cleanUpDead() {
        Map map = session.getMap();
        for (Character c : getAllCharacters()) {
            if (!c.isAlive()) {
                GridCell cell = map.getCell(c.getRow(), c.getCol());
                if (cell != null && cell.getOccupyingCharacter() == c) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private GridCell findCellWithWeapon(Weapon weapon) {
        Map map = session.getMap();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getObjects().contains(weapon)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private List<Character> getAllCharacters() {
        List<Character> all = new ArrayList<>();
        all.addAll(session.getTeamA().getMembers());
        all.addAll(session.getTeamB().getMembers());
        return all;
    }

    public boolean isRunning() { return running; }
    public List<Projectile> getActiveProjectiles() { return activeProjectiles; }
    public TeamMatchSession getSession() { return session; }
}
