package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Projectile; 
import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private Map map;
    private Hero hero;
    private ShadowClone shadowClone;
    private AchievementManager achievementManager;
    private int turnCount;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles; 
    
    // Caner's Core Logic
    private int playDurationTimer;
    private int activeEnemyCount;
    private String currentMode; 
    private Item targetObjective;

    public GameSession() {
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>(); 
        this.playDurationTimer = 0;
        this.activeEnemyCount = 0;
        this.currentMode = "Design";
    }

    public GameSession(Map map) {
        this();
        this.map = map;
    }

    // --- PROJECTILE MANAGEMENT (NEW) ---
    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public void addProjectile(Projectile p) {
        if (p != null) projectiles.add(p);
    }

    public void removeProjectile(Projectile p) {
        projectiles.remove(p);
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
        if (inv == null) return false;
        if (inv.hasItem(targetObjective)) return true;
        for (Item item : inv.getItems()) {
            if (isSameObjective(item, targetObjective)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameObjective(Item item, Item target) {
        if (item == null || target == null) return false;
        if (!item.getName().equals(target.getName())) return false;
        if (item instanceof Key && target instanceof Key) {
            return ((Key) item).getKeyId().equals(((Key) target).getKeyId());
        }
        return item.getClass().equals(target.getClass());
    }

    public String getCurrentMode() { return currentMode; }
    public void setCurrentMode(String mode) { this.currentMode = mode; }
    public boolean isTeamMatch() { return "TeamMatch".equals(currentMode); }
    public int getPlayDurationTimer() { return playDurationTimer; }
    public void setPlayDurationTimer(int timer) { this.playDurationTimer = Math.max(0, timer); }
    public Item getTargetObjective() { return targetObjective; }
    public void setTargetObjective(Item item) { this.targetObjective = item; }
    public int getActiveEnemyCount() { return activeEnemyCount; }
    public void incrementEnemyCount() { this.activeEnemyCount++; }
    public void decrementEnemyCount() { if (this.activeEnemyCount > 0) this.activeEnemyCount--; }
    public void resetActiveEnemyCount() { this.activeEnemyCount = 0; }

    // --- TEAM'S METHODS ---
    public Map getMap() { return map; }
    public void setMap(Map map) { this.map = map; }
    public Hero getHero() { return hero; }
    public void setHero(Hero hero) { this.hero = hero; }
    public ShadowClone getShadowClone() { return shadowClone; }
    public void setShadowClone(ShadowClone shadowClone) { this.shadowClone = shadowClone; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    public void wireObservers() {
        if (hero == null) return;
        hero.clearObservers();
        if (shadowClone != null) {
            hero.addObserver(shadowClone);
        }
        if (achievementManager != null) {
            hero.addObserver(achievementManager);
        }
    }

    public int getTurnCount() { return turnCount; }
    public void advanceTurn() { this.turnCount++; }
    public List<Enemy> getEnemies() { return enemies; }
    public void clearEnemies() {
        enemies.clear();
        resetActiveEnemyCount();
    }
    public void addEnemy(Enemy enemy) { 
        if (enemy != null && !enemies.contains(enemy)) {
            enemies.add(enemy); 
            incrementEnemyCount();
        }
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
