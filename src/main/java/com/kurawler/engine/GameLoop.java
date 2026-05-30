package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
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
    private long playTimeAccumulatorMs;

    /** Target sleep time per frame in milliseconds (~60 FPS). */
    private static final long FRAME_SLEEP_MS = 16;

    /** Enemy AI runs every this many frames (~2 moves/sec at 60 FPS). */
    private static final int ENEMY_TICK_INTERVAL = 30;
    private static final int PROJECTILE_TICK_INTERVAL = 8;
    private int enemyTickCounter = 0;
    private int projectileTickCounter = 0;

    /** Phase II — Shadow Clone Scroll auto-spawn interval (15 seconds). */
    private static final long SHADOW_CLONE_SCROLL_SPAWN_INTERVAL_MS = 15_000;
    private long shadowCloneScrollSpawnAccumulatorMs = 0;
    private final java.util.Random rng = new java.util.Random();

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

    /** Call after the active map changes (door / stairs). */
    public void refreshMap() {
        if (session != null && session.getMap() != null) {
            collisionDetector.setMap(session.getMap());
        }
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

        // --- BUG 1 FIX: Shadow Clone Expiration Timer ---
        // PBI-07: Tell the clone how much time has passed, and destroy it if time is up
        ShadowClone clone = session.getShadowClone();
        if (clone != null) {
            clone.tick(deltaTime);
            if (clone.isExpired()) {
                session.getHero().removeObserver(clone);
                session.setShadowClone(null);
            }
        }
        // ------------------------------------------------

        updateEnemies();
        if (++projectileTickCounter >= PROJECTILE_TICK_INTERVAL) {
            projectileTickCounter = 0;
            updateProjectiles();
        }
        enemySpawner.update(deltaTime);
        updatePlayTimer(deltaTime);

        // Phase II features added in this PR:
        handleSorcererRingDrops();              // §2.5.2 — ring drops from defeated sorcerers
        updateShadowCloneScrollSpawn(deltaTime); // Phase II — auto-spawn scroll every 15 s

        if (session.checkWinCondition()) {
            stop();
        }

        Hero hero = session.getHero();
        if (hero != null && !hero.isAlive()) {
            stop();
        }
    }

    /** One energy point per second (100 EN max ≈ 100 s from empty). */
    private static final long MS_PER_ENERGY_REGEN = 1000;
    private long energyRegenAccumulatorMs;

    private void updatePlayTimer(long deltaTime) {
        if (deltaTime <= 0) return;
        playTimeAccumulatorMs += deltaTime;
        if (playTimeAccumulatorMs >= 1000) {
            int elapsedSeconds = (int) (playTimeAccumulatorMs / 1000);
            session.setPlayDurationTimer(session.getPlayDurationTimer() + elapsedSeconds);
            playTimeAccumulatorMs %= 1000;
        }
        updateEnergyRegen(deltaTime);
    }

    private void updateEnergyRegen(long deltaTime) {
        Hero hero = session.getHero();
        if (hero == null || !hero.isAlive() || hero.getEnergy() >= hero.getMaxEnergy()) {
            return;
        }
        energyRegenAccumulatorMs += deltaTime;
        while (energyRegenAccumulatorMs >= MS_PER_ENERGY_REGEN
                && hero.getEnergy() < hero.getMaxEnergy()) {
            energyRegenAccumulatorMs -= MS_PER_ENERGY_REGEN;
            hero.restoreEnergy(1);
        }
    }

    /**
     * Scans the grid for all living enemies and lets each one decide its
     * action (chase, attack, wander, teleport, or cast spell) based on type.
     */
    private void updateEnemies() {
        if (++enemyTickCounter < ENEMY_TICK_INTERVAL) return;
        enemyTickCounter = 0;

        Map map = session.getMap();
        Hero hero = session.getHero();
        if (hero == null || map == null) {
            return;
        }

        // --- BUG 2 FIX: Enemies Deceived by Clone ---
        // PBI-07: If a ShadowClone is active on the map, all enemies must target it instead of the Hero.
        ShadowClone clone = session.getShadowClone();
        Character target = (clone != null && clone.isAlive()) ? clone : hero;
        // ---------------------------------------------

        List<Enemy> enemies = findAllEnemies(map);
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            // Pass the target (either the clone or the real hero)
            enemy.decideAction(target, map, session);
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

        activeProjectiles.addAll(session.getProjectiles());
        session.getProjectiles().clear();

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
                boolean isHeroProjectile = "Hero Bolt".equals(projectile.getName());
                boolean isHero = occupant instanceof Hero;
                if (isHeroProjectile == isHero) {
                    continue;
                }
                CombatResolver.applyProjectileDamage(projectile, occupant);
                activeProjectiles.remove(projectile);
                if (!occupant.isAlive() && !isHero) {
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

    /**
     * PDF §2.5.2: When a Sorcerer is defeated, it may drop a magical ring
     * that adds to DEF when worn. Scans the grid for newly-defeated sorcerers
     * and drops a Ring onto their cell exactly once.
     */
    private void handleSorcererRingDrops() {
        Map map = session.getMap();
        if (map == null) return;
        for (int x = 0; x < map.getGridWidth(); x++) {
            for (int y = 0; y < map.getGridHeight(); y++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell == null) continue;
                Character occupant = cell.getOccupyingCharacter();
                if (occupant instanceof com.kurawler.model.character.Sorcerer) {
                    com.kurawler.model.character.Sorcerer s =
                            (com.kurawler.model.character.Sorcerer) occupant;
                    if (!s.isAlive() && !s.isRingDropped()) {
                        // 60% chance the sorcerer was carrying a ring
                        if (rng.nextDouble() < 0.60) {
                            cell.addObject(com.kurawler.model.object.Ring.sorcererDrop());
                        }
                        s.markRingDropped();
                    }
                }
            }
        }
    }

    /**
     * Phase II — Shadow Clone: every 15 seconds a Shadow Clone Scroll appears
     * on a random walkable, unoccupied cell of the map, regardless of how many
     * scrolls already exist.
     *
     * @param deltaTime milliseconds elapsed since the last frame
     */
    private void updateShadowCloneScrollSpawn(long deltaTime) {
        // Shadow Clone is a single-player feature; skip it in Team Match.
        if (session instanceof com.kurawler.model.TeamMatchSession) return;

        // Frame-based fallback: even if deltaTime is 0/unreliable, advance ~16 ms per frame.
        long step = (deltaTime > 0) ? deltaTime : 16;
        shadowCloneScrollSpawnAccumulatorMs += step;
        if (shadowCloneScrollSpawnAccumulatorMs < SHADOW_CLONE_SCROLL_SPAWN_INTERVAL_MS) {
            return;
        }
        shadowCloneScrollSpawnAccumulatorMs = 0;

        Map map = session.getMap();
        if (map == null) return;

        // Collect candidate cells: walkable, no character, no static blocker.
        List<GridCell> candidates = new ArrayList<>();
        for (int x = 0; x < map.getGridWidth(); x++) {
            for (int y = 0; y < map.getGridHeight(); y++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell == null) continue;
                if (!cell.isWalkable()) continue;
                if (cell.getOccupyingCharacter() != null) continue;
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            System.out.println("[ShadowCloneScroll] spawn skipped — no candidate cells");
            return;
        }

        GridCell target = candidates.get(rng.nextInt(candidates.size()));
        target.addObject(new com.kurawler.model.object.ShadowCloneScroll());
        System.out.println("[ShadowCloneScroll] spawned at row=" + target.getRow()
                + " col=" + target.getCol());
    }
}