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

    /** Time between spawn attempts in milliseconds. */
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
        if (getActiveEnemyCount() >= MAX_ACTIVE_ENEMIES) {
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
            GridCell spawnCell = findEdgeSpawnLocation();
            if (spawnCell != null) {
                enemy.setPosition(spawnCell.getRow(), spawnCell.getCol());
                session.addEnemy(enemy);
            }
        }
    }

    /**
     * Finds a random walkable cell along the map edges (top or bottom row)
     * that is not occupied by the hero or another enemy.
     *
     * @return a valid edge GridCell for spawning, or null if none available
     */
    private GridCell findEdgeSpawnLocation() {
        Map map = session.getMap();
        if (map == null) {
            return null;
        }

        List<GridCell> candidates = new ArrayList<>();
        int lastRow = map.getHeight() - 1;

        for (int col = 0; col < map.getWidth(); col++) {
            addIfValidSpawn(map.getCell(0, col), candidates);
            addIfValidSpawn(map.getCell(lastRow, col), candidates);
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Adds a cell to the candidate list if it is walkable and not occupied.
     *
     * @param cell       the cell to evaluate
     * @param candidates the list to add valid cells to
     */
    private void addIfValidSpawn(GridCell cell, List<GridCell> candidates) {
        if (cell == null || !cell.isWalkable()) {
            return;
        }
        int row = cell.getRow();
        int col = cell.getCol();

        if (session.getHero() != null
                && session.getHero().getRow() == row
                && session.getHero().getCol() == col) {
            return;
        }
        if (session.getEnemyAt(row, col) != null) {
            return;
        }
        candidates.add(cell);
    }

    /**
     * Counts the number of living enemies currently in the session.
     *
     * @return the active enemy count
     */
    public int getActiveEnemyCount() {
        int count = 0;
        for (Enemy enemy : session.getEnemies()) {
            if (enemy.isAlive()) {
                count++;
            }
        }
        return count;
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
