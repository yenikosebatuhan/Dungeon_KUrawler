package com.kurawler.model;

import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
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

    public GameSession(int mapWidth, int mapHeight) {
        this.map = new Map(mapWidth, mapHeight);
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
    }

    /** Constructor that accepts a pre-built map. */
    public GameSession(Map map) {
        this.map = map;
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
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
}
