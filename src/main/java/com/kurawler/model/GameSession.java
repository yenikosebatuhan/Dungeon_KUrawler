package com.kurawler.model;

import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;

/**
 * Root aggregate for a game session.
 * Creator: GameSession creates Map.
 */
public class GameSession {

    private Map map;
    private Hero hero;
    private int turnCount;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles;
    private String currentMode;

    public GameSession(int mapWidth, int mapHeight) {
        this.map = new Map(mapWidth, mapHeight);
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.currentMode = "Play";
    }

    /**
     * Constructor that accepts a pre-built map.
     *
     * @param map the pre-built map to use
     */
    public GameSession(Map map) {
        this.map = map;
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.currentMode = "Play";
    }

    public Map getMap() { return map; }
    public void setMap(Map map) { this.map = map; }

    public Hero getHero() { return hero; }
    public void setHero(Hero hero) { this.hero = hero; }

    public int getTurnCount() { return turnCount; }
    public void advanceTurn() { this.turnCount++; }

    public List<Enemy> getEnemies() { return enemies; }

    public void addEnemy(Enemy enemy) { enemies.add(enemy); }

    public boolean removeEnemy(Enemy enemy) { return enemies.remove(enemy); }

    public Enemy getEnemyAt(int row, int col) {
        for (Enemy e : enemies) {
            if (e.getRow() == row && e.getCol() == col && e.isAlive()) {
                return e;
            }
        }
        return null;
    }

    public boolean isGameOver() {
        return hero != null && !hero.isAlive();
    }

    /**
     * Returns the current game mode ("Play" or "Build").
     *
     * @return the active mode string
     */
    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * Sets the current game mode.
     *
     * @param mode "Play" or "Build"
     */
    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    /**
     * Checks whether the win condition is met (all enemies eliminated).
     *
     * @return true if no living enemies remain on the map
     */
    public boolean checkWinCondition() {
        for (Enemy e : enemies) {
            if (e.isAlive()) {
                return false;
            }
        }
        return !enemies.isEmpty();
    }

    /**
     * Returns the list of active projectiles.
     *
     * @return a list of projectiles currently in flight
     */
    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    /**
     * Adds a projectile to the active projectile list.
     *
     * @param projectile the projectile to track
     */
    public void addProjectile(Projectile projectile) {
        if (projectile != null) {
            projectiles.add(projectile);
        }
    }

    /**
     * Removes a projectile from the active list.
     *
     * @param projectile the projectile to remove
     * @return true if the projectile was found and removed
     */
    public boolean removeProjectile(Projectile projectile) {
        return projectiles.remove(projectile);
    }
}
