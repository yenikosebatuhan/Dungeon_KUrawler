package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;

/**
 * Main game loop that runs at approximately 60 FPS.
 * Coordinates enemy AI updates, projectile movement, enemy spawning,
 * and win/lose condition checks each frame.
 *
 * <p>Only runs updates when the session is in "Play" mode.
 * UI triggers {@link #start()} and {@link #stop()} to control the loop.</p>
 */
public class GameLoop {

    private GameSession session;
    private boolean isRunning;
    private long lastUpdateTime;
    private EnemySpawner enemySpawner;
    private CollisionDetector collisionDetector;
    private final List<Projectile> activeProjectiles;

    /** Target sleep time per frame in milliseconds (~60 FPS). */
    private static final long FRAME_SLEEP_MS = 16;

    /**
     * Creates a new game loop bound to the given session.
     *
     * @param session the active game session; must not be null
     */
    public GameLoop(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("GameSession must not be null");
        }
        this.session = session;
        this.enemySpawner = new EnemySpawner(session);
        this.collisionDetector = new CollisionDetector(session.getMap());
        this.activeProjectiles = new ArrayList<>();
    }

    /**
     * Starts the game loop. Blocks the calling thread until {@link #stop()} is called.
     */
    public void start() {
        isRunning = true;
        lastUpdateTime = System.currentTimeMillis();
        run();
    }

    /**
     * Signals the game loop to stop after the current frame completes.
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * Internal loop that calls {@link #update(long)} each frame
     * and sleeps to approximate 60 FPS.
     */
    private void run() {
        while (isRunning) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastUpdateTime;

            update(deltaTime);
            lastUpdateTime = currentTime;

            try {
                Thread.sleep(FRAME_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    /**
     * Performs one frame of game logic: enemy AI, projectile movement,
     * enemy spawning, and win/lose checks.
     * Public so that JavaFX AnimationTimer can drive the loop externally.
     *
     * @param deltaTime milliseconds elapsed since the last frame
     */
    public void update(long deltaTime) {
        if (!"Play".equals(session.getCurrentMode())) {
            return;
        }

        updateEnemies();
        updateProjectiles();
        enemySpawner.update(deltaTime);

        if (session.checkWinCondition()) {
            stop();
        }

        Hero hero = session.getHero();
        if (hero != null && !hero.isAlive()) {
            stop();
        }
    }

    /**
     * Scans the grid for all living enemies and lets each one decide its
     * action (chase, attack, wander, teleport, or cast spell) based on type.
     */
    private void updateEnemies() {
        Map map = session.getMap();
        Hero hero = session.getHero();
        if (hero == null || map == null) {
            return;
        }

        List<Enemy> enemies = findAllEnemies(map);
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            enemy.decideAction(hero, map, session);
        }
    }

    /**
     * Scans the entire grid to find all Enemy characters.
     *
     * @param map the game map to scan
     * @return a list of all enemies currently on the grid
     */
    private List<Enemy> findAllEnemies(Map map) {
        List<Enemy> enemies = new ArrayList<>();
        for (int x = 0; x < map.getGridWidth(); x++) {
            for (int y = 0; y < map.getGridHeight(); y++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell != null) {
                    Character occupant = cell.getOccupyingCharacter();
                    if (occupant instanceof Enemy) {
                        enemies.add((Enemy) occupant);
                    }
                }
            }
        }
        return enemies;
    }

    /**
     * Moves every active projectile one step in its travel direction.
     * If a projectile hits a character, applies damage via
     * {@link CombatResolver#applyProjectileDamage(Projectile, Character)}.
     * If a projectile hits a wall or goes out of bounds, it is removed.
     */
    private void updateProjectiles() {
        Map map = session.getMap();
        if (map == null) {
            return;
        }

        List<Projectile> snapshot = new ArrayList<>(activeProjectiles);
        for (Projectile projectile : snapshot) {
            int newRow = projectile.getPositionRow() + projectile.getDirectionRow();
            int newCol = projectile.getPositionCol() + projectile.getDirectionCol();

            GridCell targetCell = map.getCell(newRow, newCol);
            if (targetCell == null || (targetCell.getStaticObject() != null
                    && !targetCell.getStaticObject().isPassable())) {
                activeProjectiles.remove(projectile);
                continue;
            }

            projectile.setPosition(newRow, newCol);

            Character occupant = targetCell.getOccupyingCharacter();
            if (occupant != null && occupant.isAlive()) {
                CombatResolver.applyProjectileDamage(projectile, occupant);
                activeProjectiles.remove(projectile);
                if (!occupant.isAlive()) {
                    targetCell.setOccupyingCharacter(null);
                    session.decrementEnemyCount();
                }
            }
        }
    }

    /**
     * Adds a projectile to the active tracking list.
     *
     * @param projectile the projectile to track
     */
    public void addProjectile(Projectile projectile) {
        if (projectile != null) {
            activeProjectiles.add(projectile);
        }
    }

    /**
     * Returns the list of active projectiles.
     *
     * @return the active projectile list
     */
    public List<Projectile> getActiveProjectiles() {
        return activeProjectiles;
    }

    /**
     * Returns whether the game loop is currently running.
     *
     * @return true if the loop is active
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns the active game session.
     *
     * @return the current GameSession
     */
    public GameSession getSession() {
        return session;
    }

    /**
     * Returns the collision detector used by this loop.
     *
     * @return the CollisionDetector instance
     */
    public CollisionDetector getCollisionDetector() {
        return collisionDetector;
    }

    /**
     * Returns the enemy spawner used by this loop.
     *
     * @return the EnemySpawner instance
     */
    public EnemySpawner getEnemySpawner() {
        return enemySpawner;
    }
}
