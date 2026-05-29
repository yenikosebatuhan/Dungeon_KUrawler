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
 * <p>Spawn rules (from spec):
 * <ul>
 *   <li>One spawn attempt every {@value #SPAWN_INTERVAL} ms (9 seconds)</li>
 *   <li>Maximum {@value #MAX_ACTIVE_ENEMIES} active enemies at once</li>
 *   <li>60% chance Knight, 30% chance Sorcerer, 10% chance nothing spawns</li>
 *   <li>Enemies appear on walkable edge cells (row 0 or last row)</li>
 * </ul>
 * </p>
 */
public class EnemySpawner {

    private GameSession session;
    private long timeSinceLastSpawn;
    private Random random;

    /** Time between spawn attempts in milliseconds (9 seconds per spec). */
    public static final long SPAWN_INTERVAL = 9000;

    /** Maximum number of living enemies allowed on the map. */
    public static final int MAX_ACTIVE_ENEMIES = 5;

    /**
     * Creates a spawner for the given session.
     *
     * @param session the active game session; must not be null
     */
    public EnemySpawner(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("GameSession must not be null");
        }
        this.session = session;
        this.timeSinceLastSpawn = 0;
        this.random = new Random();
    }

    /**
     * Accumulates elapsed time and attempts a spawn when the interval is reached.
     *
     * @param deltaTime milliseconds elapsed since the last call
     */
    public void update(long deltaTime) {
        timeSinceLastSpawn += deltaTime;

        if (timeSinceLastSpawn >= SPAWN_INTERVAL) {
            spawnEnemy();
            timeSinceLastSpawn = 0;
        }
    }

    /**
     * Attempts to spawn a single enemy. Does nothing if the map already has
     * {@value #MAX_ACTIVE_ENEMIES} or more living enemies.
     *
     * <p>Roll distribution: 0-59 = Knight, 60-89 = Sorcerer, 90-99 = no spawn.</p>
     */
    private void spawnEnemy() {
        if (session.getActiveEnemyCount() >= MAX_ACTIVE_ENEMIES) {
            return;
        }

        int roll = random.nextInt(100);
        Enemy enemy = null;

        if (roll < 60) {
            enemy = new Knight();
        } else if (roll < 90) {
            enemy = new Sorcerer();
        }

        if (enemy != null) {
            String diff = session.getDifficulty();
            if ("Easy".equals(diff)) {
                enemy.setMaxHealth((int)(enemy.getMaxHealth() * 0.7));
                enemy.setHealth((int)(enemy.getHealth() * 0.7));
                enemy.setStrength((int)(enemy.getStrength() * 0.7));
            } else if ("Hard".equals(diff)) {
                enemy.setMaxHealth((int)(enemy.getMaxHealth() * 1.3));
                enemy.setHealth((int)(enemy.getHealth() * 1.3));
                enemy.setStrength((int)(enemy.getStrength() * 1.3));
            }

            GridCell spawnCell = findSpawnLocation();
            if (spawnCell != null) {
                enemy.moveToCell(spawnCell);
                session.addEnemy(enemy);
            }
        }
    }
    /**
     * Finds a random walkable, unoccupied cell for spawning.
     * Prefers edge cells (top/bottom row); falls back to any walkable cell
     * far from the hero if edges are all walls.
     *
     * @return a valid GridCell for spawning, or null if none available
     */
    private GridCell findSpawnLocation() {
        Map map = session.getMap();
        if (map == null) {
            return null;
        }

        List<GridCell> edgeCandidates = new ArrayList<>();
        int lastRow = map.getHeight() - 1;
        int lastCol = map.getWidth() - 1;
        for (int c = 0; c < map.getWidth(); c++) {
            addIfValidSpawn(map.getCell(0, c), edgeCandidates);
            addIfValidSpawn(map.getCell(lastRow, c), edgeCandidates);
        }
        for (int r = 1; r < lastRow; r++) {
            addIfValidSpawn(map.getCell(r, 0), edgeCandidates);
            addIfValidSpawn(map.getCell(r, lastCol), edgeCandidates);
        }

        if (!edgeCandidates.isEmpty()) {
            return edgeCandidates.get(random.nextInt(edgeCandidates.size()));
        }

        // Fallback: any walkable cell at least 5 tiles from hero
        List<GridCell> fallback = new ArrayList<>();
        com.kurawler.model.character.Hero hero = session.getHero();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || cell.isBlocked()) continue;
                if (cell.getOccupyingCharacter() != null) continue;
                if (hero != null) {
                    int dist = Math.abs(r - hero.getRow()) + Math.abs(c - hero.getCol());
                    if (dist < 5) continue;
                }
                fallback.add(cell);
            }
        }
        if (fallback.isEmpty()) return null;
        return fallback.get(random.nextInt(fallback.size()));
    }

    /**
     * Adds a cell to the candidate list if it is walkable and not occupied.
     *
     * @param cell       the cell to evaluate
     * @param candidates the list to add valid cells to
     */
    private void addIfValidSpawn(GridCell cell, List<GridCell> candidates) {
        if (cell == null || cell.isBlocked()) {
            return;
        }
        if (cell.getOccupyingCharacter() != null) {
            return;
        }
        candidates.add(cell);
    }

    /**
     * Returns the time accumulated since the last spawn attempt.
     *
     * @return elapsed time in milliseconds
     */
    public long getTimeSinceLastSpawn() {
        return timeSinceLastSpawn;
    }

    /**
     * Resets the spawn timer to zero.
     */
    public void resetTimer() {
        this.timeSinceLastSpawn = 0;
    }
}
