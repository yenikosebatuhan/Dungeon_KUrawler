package com.kurawler.model;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TeamMatchSession extends GameSession {

    private final Team teamA;
    private final Team teamB;
    private final List<Weapon> matchWeapons;
    private String matchResult;

    private static final int TEAM_SIZE = 4;
    private static final int WEAPON_COUNT = 6;
    private static final int MIN_ATK = 3;
    private static final int MAX_ATK = 8;
    private static final Random RNG = new Random();

    public TeamMatchSession(Map map) {
        super(map);
        setCurrentMode("TeamMatch");
        this.teamA = new Team("Team A", Team.TeamColor.RED);
        this.teamB = new Team("Team B", Team.TeamColor.BLUE);
        this.matchWeapons = new ArrayList<>();
    }

    public void initializeMatch() {
        clearMapEnemies();
        clearMapWeapons();
        createTeams();
        placeTeams();
        placeWeapons();
    }

    private void clearMapWeapons() {
        Map map = getMap();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) continue;
                cell.getObjects().stream()
                    .filter(obj -> obj instanceof Weapon)
                    .toList()
                    .forEach(cell::removeObject);
            }
        }
    }

    private void clearMapEnemies() {
        Map map = getMap();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) continue;
                if (cell.getOccupyingCharacter() instanceof com.kurawler.model.character.Enemy) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private void createTeams() {
        Sorcerer sorcA = new Sorcerer();
        sorcA.setHealth(10);
        teamA.addMember(sorcA);
        for (int i = 0; i < 3; i++) {
            Knight k = new Knight();
            k.setUnlimitedVision(true);
            teamA.addMember(k);
        }

        Hero hero = new Hero("Hero");
        setHero(hero);
        teamB.addMember(hero);

        Sorcerer sorcB = new Sorcerer();
        sorcB.setHealth(10);
        teamB.addMember(sorcB);
        for (int i = 0; i < 2; i++) {
            Knight k = new Knight();
            k.setUnlimitedVision(true);
            teamB.addMember(k);
        }
    }

    private void placeTeams() {
        Map map = getMap();
        int halfCol = map.getWidth() / 2;

        List<GridCell> leftCells = collectWalkableCells(0, halfCol);
        List<GridCell> rightCells = collectWalkableCells(halfCol, map.getWidth());

        Collections.shuffle(leftCells, RNG);
        Collections.shuffle(rightCells, RNG);

        placeTeamMembers(teamA, leftCells);
        placeTeamMembers(teamB, rightCells);
    }

    private List<GridCell> collectWalkableCells(int colStart, int colEnd) {
        Map map = getMap();
        List<GridCell> cells = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = colStart; c < colEnd; c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private void placeTeamMembers(Team team, List<GridCell> candidates) {
        int idx = 0;
        for (Character member : team.getMembers()) {
            if (idx < candidates.size()) {
                member.moveToCell(candidates.get(idx));
                idx++;
            }
        }
    }

    private void placeWeapons() {
        Map map = getMap();
        List<GridCell> allWalkable = collectWalkableCells(0, map.getWidth());
        List<GridCell> free = new ArrayList<>();
        for (GridCell cell : allWalkable) {
            if (cell.getOccupyingCharacter() == null && !cell.hasObjects()) {
                free.add(cell);
            }
        }
        Collections.shuffle(free, RNG);

        String[] names = {"Dagger", "Axe", "Hammer", "Staff", "Bow", "Sword"};
        for (int i = 0; i < WEAPON_COUNT && i < free.size(); i++) {
            int atk = MIN_ATK + i;
            Weapon w = new Weapon(names[i], "ATK " + atk, atk);
            free.get(i).addObject(w);
            matchWeapons.add(w);
        }
    }

    public boolean validateMap() {
        Map map = getMap();
        if (map == null) return false;
        int halfCol = map.getWidth() / 2;
        int leftCount = collectWalkableCells(0, halfCol).size();
        int rightCount = collectWalkableCells(halfCol, map.getWidth()).size();
        return leftCount >= TEAM_SIZE && rightCount >= TEAM_SIZE;
    }

    @Override
    public boolean checkWinCondition() {
        if (teamA.isEliminated()) {
            matchResult = "Team B Wins!";
            return true;
        }
        if (teamB.isEliminated()) {
            matchResult = "Team A Wins!";
            return true;
        }
        return false;
    }

    @Override
    public boolean isGameOver() {
        return checkWinCondition();
    }

    public Team getTeamForCharacter(Character c) {
        if (teamA.getMembers().contains(c)) return teamA;
        if (teamB.getMembers().contains(c)) return teamB;
        return null;
    }

    public Team getOpposingTeam(Character c) {
        Team own = getTeamForCharacter(c);
        if (own == teamA) return teamB;
        if (own == teamB) return teamA;
        return null;
    }

    /** True only when attacker and target are on different teams (Team Match). */
    public boolean canDamage(Character attacker, Character target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return false;
        }
        Team attackerTeam = getTeamForCharacter(attacker);
        Team targetTeam = getTeamForCharacter(target);
        if (attackerTeam == null || targetTeam == null) {
            return false;
        }
        return attackerTeam != targetTeam;
    }

    @Override
    public void addProjectile(com.kurawler.model.object.Projectile p) {
        if (p != null && getHero() != null && isHeroOwnedProjectile(p.getName())) {
            Team heroTeam = getTeamForCharacter(getHero());
            if (heroTeam != null) {
                p.setSourceTeamName(heroTeam.getName());
            }
        }
        super.addProjectile(p);
    }

    private static boolean isHeroOwnedProjectile(String name) {
        return "Hero Bolt".equals(name) || "Arrow".equals(name) || "Bomb".equals(name)
                || "Staff Bolt".equals(name) || "Weapon Strike".equals(name);
    }

    public Character findNearestEnemy(Character c) {
        Team opposing = getOpposingTeam(c);
        if (opposing == null) return null;

        Character nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Character enemy : opposing.getAliveMembers()) {
            double dist = euclideanDist(c, enemy);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        }
        return nearest;
    }

    public GridCell findNearestWeaponCell(Character c) {
        Map map = getMap();
        GridCell nearest = null;
        double minDist = Double.MAX_VALUE;
        for (int r = 0; r < map.getHeight(); r++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(r, col);
                if (cell == null) continue;
                for (var obj : cell.getObjects()) {
                    if (obj instanceof Weapon) {
                        double dist = Math.sqrt(
                            Math.pow(r - c.getRow(), 2) + Math.pow(col - c.getCol(), 2));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = cell;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    public GridCell findNearestHealPotionCell(Character c) {
        Map map = getMap();
        GridCell nearest = null;
        double minDist = Double.MAX_VALUE;
        for (int r = 0; r < map.getHeight(); r++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(r, col);
                if (cell == null) continue;
                for (var obj : cell.getObjects()) {
                    if (obj instanceof Consumable cons && cons.getHealAmount() > 0) {
                        double dist = Math.sqrt(
                            Math.pow(r - c.getRow(), 2) + Math.pow(col - c.getCol(), 2));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = cell;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    public Weapon findNearestWeapon(Character c) {
        Map map = getMap();
        Weapon nearest = null;
        double minDist = Double.MAX_VALUE;

        for (int r = 0; r < map.getHeight(); r++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(r, col);
                if (cell == null) continue;
                for (var obj : cell.getObjects()) {
                    if (obj instanceof Weapon) {
                        double dist = Math.sqrt(
                            Math.pow(r - c.getRow(), 2) + Math.pow(col - c.getCol(), 2));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = (Weapon) obj;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private double euclideanDist(Character a, Character b) {
        return Math.sqrt(Math.pow(a.getRow() - b.getRow(), 2)
                       + Math.pow(a.getCol() - b.getCol(), 2));
    }

    public Team getTeamA() { return teamA; }
    public Team getTeamB() { return teamB; }
    public List<Weapon> getMatchWeapons() { return Collections.unmodifiableList(matchWeapons); }
    public String getMatchResult() { return matchResult; }
}
