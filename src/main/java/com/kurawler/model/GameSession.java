package com.kurawler.model;

import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private Map map;
    private Hero hero;
    private int turnCount;
    private final List<Enemy> enemies;

    // Caner's Core Logic
    private int playDurationTimer;
    private int activeEnemyCount;
    private String currentMode; // "Play" or "Design"
    private Item targetObjective;

    public GameSession() {
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
        this.playDurationTimer = 0;
        this.activeEnemyCount = 0;
        this.currentMode = "Design";
    }

    public GameSession(Map map) {
        this();
        this.map = map;
    }

    // --- CANER'S CORE METHODS ---
    public void startGame() {
        this.currentMode = "Play";
        this.playDurationTimer = 0;
    }
    public void pauseGame() { /* Pause logic */ }
    
    public boolean checkWinCondition() {
        if (hero == null || targetObjective == null) return false;
        Inventory inv = hero.getInventory();
        return inv != null && inv.hasItem(targetObjective);
    }
    
    public String getCurrentMode() { return currentMode; }
    public void setCurrentMode(String mode) { this.currentMode = mode; }
    public int getPlayDurationTimer() { return playDurationTimer; }
    public void setPlayDurationTimer(int timer) { this.playDurationTimer = Math.max(0, timer); }
    public Item getTargetObjective() { return targetObjective; }
    public void setTargetObjective(Item item) { this.targetObjective = item; }
    public int getActiveEnemyCount() { return activeEnemyCount; }
    public void incrementEnemyCount() { this.activeEnemyCount++; }
    public void decrementEnemyCount() { if (this.activeEnemyCount > 0) this.activeEnemyCount--; }

    // --- TEAM'S METHODS ---
    public Map getMap() { return map; }
    public void setMap(Map map) { this.map = map; }
    public Hero getHero() { return hero; }
    public void setHero(Hero hero) { this.hero = hero; }
    public int getTurnCount() { return turnCount; }
    public void advanceTurn() { this.turnCount++; }
    public List<Enemy> getEnemies() { return enemies; }
    public void addEnemy(Enemy enemy) { 
        enemies.add(enemy); 
        incrementEnemyCount();
    }
    public boolean removeEnemy(Enemy enemy) { 
        boolean removed = enemies.remove(enemy);
        if (removed) decrementEnemyCount();
        return removed;
    }
    public Enemy getEnemyAt(int row, int col) {
        for (Enemy e : enemies) {
            if (e.getRow() == row && e.getCol() == col && e.isAlive()) return e;
        }
        return null;
    }
    public boolean isGameOver() { return hero != null && !hero.isAlive(); }
}