package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
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
     *
     * @param deltaTime milliseconds elapsed since the last frame
     */
    private void update(long deltaTime) {
        if (!"Play".equals(session.getCurrentMode())) {
            return;
        }

        updateEnemies();
        updateProjectiles();
        enemySpawner.update(deltaTime);

        if (session.checkWinCondition()) {
            stop();
        }
        if (session.isGameOver()) {
            stop();
        }
    }

    /**
     * Iterates over all living enemies and lets each one decide its action
     * (chase, attack, wander, teleport, or cast spell) based on its type.
     */
    private void updateEnemies() {
        Map map = session.getMap();
        Hero hero = session.getHero();
        if (hero == null || map == null) {
            return;
        }

        List<Enemy> snapshot = new ArrayList<>(session.getEnemies());
        for (Enemy enemy : snapshot) {
            if (!enemy.isAlive()) {
                continue;
            }
            if (enemy instanceof Knight) {
                ((Knight) enemy).decideAction(hero, map, session);
            } else if (enemy instanceof Sorcerer) {
                ((Sorcerer) enemy).decideAction(hero, map, session);
            }
        }
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

        List<Projectile> snapshot = new ArrayList<>(session.getProjectiles());
        for (Projectile projectile : snapshot) {
            int oldRow = projectile.getPositionRow();
            int oldCol = projectile.getPositionCol();
            int newRow = oldRow + projectile.getDirectionRow();
            int newCol = oldCol + projectile.getDirectionCol();

            GridCell targetCell = map.getCell(newRow, newCol);
            if (targetCell == null || !targetCell.isWalkable()) {
                session.removeProjectile(projectile);
                continue;
            }

            projectile.setPosition(newRow, newCol);

            Hero hero = session.getHero();
            if (hero != null && hero.isAlive()
                    && hero.getRow() == newRow && hero.getCol() == newCol) {
                CombatResolver.applyProjectileDamage(projectile, hero);
                session.removeProjectile(projectile);
                continue;
            }

            Enemy enemy = session.getEnemyAt(newRow, newCol);
            if (enemy != null && enemy.isAlive()) {
                CombatResolver.applyProjectileDamage(projectile, enemy);
                session.removeProjectile(projectile);
            }
        }
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
