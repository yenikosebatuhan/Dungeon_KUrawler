package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns enemies on a fixed interval (PDF §2.5).
 *
 * <p>Spawn rules:
 * <ul>
 *   <li>One spawn attempt every {@value #SPAWN_INTERVAL_MS} ms (9 seconds)</li>
 *   <li>60% Knight, 30% Sorcerer, 10% no spawn</li>
 *   <li>Maximum {@value #MAX_ACTIVE_ENEMIES} active enemies at once</li>
 *   <li>Classic mode: random empty cell on the map edge (next to walls)</li>
 *   <li>Editor test play: interior floor tiles reachable from the hero (design choice)</li>
 * </ul>
 * </p>
 */
public class EnemySpawner {

    /** PDF §2.5: enemies spawn every 9 seconds. */
    public static final long SPAWN_INTERVAL_MS = 9000L;

    /** PDF §2.5: up to 5 enemies on the floor at once. */
    public static final int MAX_ACTIVE_ENEMIES = 5;

    /** PDF §2.5: 60% Knight, 30% Sorcerer, 10% no spawn. */
    private static final int KNIGHT_ROLL_MAX = 60;
    private static final int SORCERER_ROLL_MAX = 90;

    private final GameSession session;
    private final Random random;

    public EnemySpawner(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("GameSession must not be null");
        }
        this.session = session;
        this.random = new Random();
    }

    public void update(long deltaTime) {
        if (deltaTime <= 0) {
            return;
        }
        long accumulator = session.getEnemySpawnAccumulatorMs() + deltaTime;
        while (accumulator >= SPAWN_INTERVAL_MS) {
            attemptSpawn();
            accumulator -= SPAWN_INTERVAL_MS;
        }
        session.setEnemySpawnAccumulatorMs(accumulator);
    }

    public long getTimeSinceLastSpawn() {
        return session.getEnemySpawnAccumulatorMs();
    }

    public void resetTimer() {
        session.setEnemySpawnAccumulatorMs(0);
    }

    private void attemptSpawn() {
        if (countLivingEnemiesOnMap() >= MAX_ACTIVE_ENEMIES) {
            return;
        }

        Enemy enemy = rollSpawnType();
        if (enemy == null) {
            return;
        }

        applyDifficultyScaling(enemy);
        GridCell spawnCell = findSpawnLocation();
        if (spawnCell != null) {
            enemy.moveToCell(spawnCell);
            session.addEnemy(enemy);
        }
    }

    /** @return Knight, Sorcerer, or null for a skipped spawn attempt (10%). */
    private Enemy rollSpawnType() {
        int roll = random.nextInt(100);
        if (roll < KNIGHT_ROLL_MAX) {
            return new Knight();
        }
        if (roll < SORCERER_ROLL_MAX) {
            return new Sorcerer();
        }
        return null;
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

        com.kurawler.model.character.Hero hero = session.getHero();

        if (session.isEditorTestPlay()) {
            List<GridCell> reachable = collectReachableSpawns(map, hero, 3);
            if (reachable.isEmpty()) {
                reachable = collectReachableSpawns(map, hero, 1);
            }
            if (!reachable.isEmpty()) {
                return reachable.get(random.nextInt(reachable.size()));
            }
            return null;
        }

        List<GridCell> edgeCandidates = new ArrayList<>();
        int innerTop = 1;
        int innerBottom = map.getHeight() - 2;
        int innerLeft = 1;
        int innerRight = map.getWidth() - 2;
        if (innerBottom >= innerTop && innerRight >= innerLeft) {
            for (int c = innerLeft; c <= innerRight; c++) {
                addIfValidSpawn(map.getCell(innerTop, c), edgeCandidates);
                addIfValidSpawn(map.getCell(innerBottom, c), edgeCandidates);
            }
            for (int r = innerTop + 1; r < innerBottom; r++) {
                addIfValidSpawn(map.getCell(r, innerLeft), edgeCandidates);
                addIfValidSpawn(map.getCell(r, innerRight), edgeCandidates);
            }
        }
        if (!edgeCandidates.isEmpty()) {
            return edgeCandidates.get(random.nextInt(edgeCandidates.size()));
        }

        return pickAnyWalkableSpawn(map, hero);
    }

    /** Editor test: spawn only on empty floor tiles reachable from the hero (inside the same room). */
    private List<GridCell> collectReachableSpawns(Map map,
            com.kurawler.model.character.Hero hero, int minHeroDistance) {
        List<GridCell> candidates = new ArrayList<>();
        if (hero == null) {
            return candidates;
        }
        int height = map.getHeight();
        int width = map.getWidth();
        boolean[][] seen = new boolean[height][width];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int hr = hero.getRow();
        int hc = hero.getCol();
        if (hr < 0 || hr >= height || hc < 0 || hc >= width) {
            return candidates;
        }
        seen[hr][hc] = true;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nr = hr + d[0];
            int nc = hc + d[1];
            if (nr >= 0 && nr < height && nc >= 0 && nc < width && !seen[nr][nc]) {
                GridCell n = map.getCell(nr, nc);
                if (n != null && n.isEnemyWalkable()) {
                    seen[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0];
            int c = pos[1];
            GridCell cell = map.getCell(r, c);
            if (isValidEmptySpawn(cell)) {
                int dist = Math.abs(r - hr) + Math.abs(c - hc);
                if (dist >= minHeroDistance) {
                    candidates.add(cell);
                }
            }
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= height || nc < 0 || nc >= width || seen[nr][nc]) {
                    continue;
                }
                GridCell next = map.getCell(nr, nc);
                if (next != null && next.isEnemyWalkable()) {
                    seen[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return candidates;
    }

    private GridCell pickAnyWalkableSpawn(Map map, com.kurawler.model.character.Hero hero) {
        List<GridCell> fallback = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (!isValidEmptySpawn(cell)) {
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

    private boolean isValidEmptySpawn(GridCell cell) {
        return cell != null && cell.isEnemyWalkable() && cell.getOccupyingCharacter() == null
                && !cell.hasObjects();
    }

    private void addIfValidSpawn(GridCell cell, List<GridCell> candidates) {
        if (!isValidEmptySpawn(cell)) {
            return;
        }
        candidates.add(cell);
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
}
