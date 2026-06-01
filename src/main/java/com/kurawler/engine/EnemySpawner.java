package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns enemies at the edges of the map on a fixed interval.
 *
 * <p>Spawn rules:
 * <ul>
 *   <li>No spawn for the first {@value #INITIAL_QUIET_MS} ms of play</li>
 *   <li>First spawn between 3–6 s (always a Knight)</li>
 *   <li>Then one attempt every 3–5 s (random interval)</li>
 *   <li>Maximum {@value #MAX_ACTIVE_ENEMIES} active enemies at once</li>
 *   <li>50% Knight, 40% Sorcerer, 10% no spawn (after the first Knight)</li>
 * </ul>
 * </p>
 */
public class EnemySpawner {

    private GameSession session;
    private long timeSinceLastSpawn;
    private long totalPlayMs;
    private boolean firstSpawnDone;
    private long firstSpawnAtMs;
    private long nextSpawnIntervalMs;
    private Random random;

    /** No edge spawns during the first 3 seconds. */
    public static final long INITIAL_QUIET_MS = 3000;

    /** First Knight may appear in [3 s, 6 s] after play starts. */
    public static final long FIRST_SPAWN_WINDOW_MS = 3000;

    /** Min/max delay between spawn attempts after the first Knight (3–5 s). */
    public static final long SPAWN_INTERVAL_MIN_MS = 3000;
    public static final long SPAWN_INTERVAL_MAX_MS = 5000;

    /** Maximum number of living enemies allowed on the map. */
    public static final int MAX_ACTIVE_ENEMIES = 5;

    public EnemySpawner(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("GameSession must not be null");
        }
        this.session = session;
        this.timeSinceLastSpawn = 0;
        this.totalPlayMs = 0;
        this.firstSpawnDone = false;
        this.random = new Random();
        scheduleFirstSpawn();
    }

    private void scheduleFirstSpawn() {
        firstSpawnAtMs = INITIAL_QUIET_MS + random.nextInt((int) FIRST_SPAWN_WINDOW_MS + 1);
    }

    private void scheduleNextSpawn() {
        long span = SPAWN_INTERVAL_MAX_MS - SPAWN_INTERVAL_MIN_MS + 1;
        nextSpawnIntervalMs = SPAWN_INTERVAL_MIN_MS + random.nextInt((int) span);
    }

    public void update(long deltaTime) {
        if (deltaTime > 0) {
            totalPlayMs += deltaTime;
        }

        if (!firstSpawnDone) {
            if (totalPlayMs < firstSpawnAtMs) {
                return;
            }
            spawnEnemy(true);
            firstSpawnDone = true;
            timeSinceLastSpawn = 0;
            scheduleNextSpawn();
            return;
        }

        timeSinceLastSpawn += deltaTime;
        if (timeSinceLastSpawn >= nextSpawnIntervalMs) {
            spawnEnemy(false);
            timeSinceLastSpawn = 0;
            scheduleNextSpawn();
        }
    }

    /**
     * @param forceKnight true for the delayed first spawn (always Knight)
     */
    private void spawnEnemy(boolean forceKnight) {
        if (countLivingEnemiesOnMap() >= MAX_ACTIVE_ENEMIES) {
            return;
        }

        Enemy enemy = null;
        if (forceKnight) {
            enemy = new Knight();
        } else if (!hasLivingSorcererOnMap()) {
            // Keep sorcerers in the mix (40% roll can miss several times in a row).
            enemy = new Sorcerer();
        } else {
            int roll = random.nextInt(100);
            if (roll < 50) {
                enemy = new Knight();
            } else if (roll < 90) {
                enemy = new Sorcerer();
            }
        }

        if (enemy != null) {
            applyDifficultyScaling(enemy);
            GridCell spawnCell = findSpawnLocation();
            if (spawnCell != null) {
                enemy.moveToCell(spawnCell);
                session.addEnemy(enemy);
            }
        }
    }

    private void applyDifficultyScaling(Enemy enemy) {
        String diff = session.getDifficulty();
        if ("Easy".equals(diff)) {
            enemy.setMaxHealth((int) (enemy.getMaxHealth() * 0.7));
            enemy.setHealth((int) (enemy.getHealth() * 0.7));
            enemy.setStrength((int) (enemy.getStrength() * 0.7));
        } else if ("Hard".equals(diff)) {
            enemy.setMaxHealth((int) (enemy.getMaxHealth() * 1.3));
            enemy.setHealth((int) (enemy.getHealth() * 1.3));
            enemy.setStrength((int) (enemy.getStrength() * 1.3));
        }
    }

    private GridCell findSpawnLocation() {
        Map map = session.getMap();
        if (map == null) {
            return null;
        }

        List<GridCell> edgeCandidates = new ArrayList<>();
        int innerTop = 1;
        int innerBottom = map.getHeight() - 2;
        int innerLeft = 1;
        int innerRight = map.getWidth() - 2;
        if (innerBottom < innerTop || innerRight < innerLeft) {
            return null;
        }
        for (int c = innerLeft; c <= innerRight; c++) {
            addIfValidSpawn(map.getCell(innerTop, c), edgeCandidates);
            addIfValidSpawn(map.getCell(innerBottom, c), edgeCandidates);
        }
        for (int r = innerTop + 1; r < innerBottom; r++) {
            addIfValidSpawn(map.getCell(r, innerLeft), edgeCandidates);
            addIfValidSpawn(map.getCell(r, innerRight), edgeCandidates);
        }

        if (!edgeCandidates.isEmpty()) {
            return edgeCandidates.get(random.nextInt(edgeCandidates.size()));
        }

        List<GridCell> fallback = new ArrayList<>();
        com.kurawler.model.character.Hero hero = session.getHero();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || !cell.isEnemyWalkable()) {
                    continue;
                }
                if (cell.getOccupyingCharacter() != null) {
                    continue;
                }
                if (hero != null) {
                    int dist = Math.abs(r - hero.getRow()) + Math.abs(c - hero.getCol());
                    if (dist < 5) {
                        continue;
                    }
                }
                fallback.add(cell);
            }
        }
        if (fallback.isEmpty()) {
            return null;
        }
        return fallback.get(random.nextInt(fallback.size()));
    }

    private void addIfValidSpawn(GridCell cell, List<GridCell> candidates) {
        if (cell == null || !cell.isEnemyWalkable()) {
            return;
        }
        if (cell.getOccupyingCharacter() != null) {
            return;
        }
        candidates.add(cell);
    }

    public long getTimeSinceLastSpawn() {
        return timeSinceLastSpawn;
    }

    public void resetTimer() {
        timeSinceLastSpawn = 0;
        totalPlayMs = 0;
        firstSpawnDone = false;
        scheduleFirstSpawn();
    }

    private int countLivingEnemiesOnMap() {
        Map map = session.getMap();
        if (map == null) {
            return 0;
        }
        int n = 0;
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getOccupyingCharacter() instanceof Enemy e && e.isAlive()) {
                    n++;
                }
            }
        }
        return n;
    }

    private boolean hasLivingSorcererOnMap() {
        Map map = session.getMap();
        if (map == null) {
            return false;
        }
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getOccupyingCharacter() instanceof Sorcerer s && s.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }
}
