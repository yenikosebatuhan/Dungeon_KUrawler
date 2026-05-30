package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Projectile; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameSession {
    private Map map;
    private final java.util.Map<String, Map> worldMaps = new HashMap<>();
    private String currentMapId;
    private String lastPortalMessage;
    private boolean mapChanged;
    private Hero hero;
    private ShadowClone shadowClone;
    private AchievementManager achievementManager;
    private int turnCount;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles; 
    
    private int playDurationTimer;
    private int activeEnemyCount;
    private int enemiesDefeatedCount;
    private int itemsCollectedCount;
    private int floorsClearedCount;
    private String currentMode;
    private String difficulty = "Normal";
    private Item targetObjective;
    private boolean editorTestPlay;

    public GameSession() {
        this.turnCount = 0;
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>(); 
        this.playDurationTimer = 0;
        this.activeEnemyCount = 0;
        this.currentMode = "Design";
        this.enemiesDefeatedCount = 0;
        this.itemsCollectedCount = 0;
        this.floorsClearedCount = 1;
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
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty != null ? difficulty : "Normal";
    }
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

    public java.util.Map<String, Map> getWorldMaps() { return worldMaps; }

    public void registerWorldMap(String mapId, Map worldMap) {
        if (mapId != null && worldMap != null) {
            worldMaps.put(mapId, worldMap);
        }
    }

    public String getCurrentMapId() { return currentMapId; }

    public void setCurrentMapId(String mapId) { this.currentMapId = mapId; }

    public void setLastPortalMessage(String message) {
        this.lastPortalMessage = message;
    }

    public String consumeLastPortalMessage() {
        String msg = lastPortalMessage;
        lastPortalMessage = null;
        return msg;
    }

    /**
     * Moves the hero to another registered map (door / stairs).
     *
     * @return true if the transition succeeded
     */
    public boolean transitionHeroTo(String mapId, int row, int col) {
        Map next = worldMaps.get(mapId);
        if (next == null) {
            lastPortalMessage = "That passage leads nowhere.";
            return false;
        }
        GridCell dest = next.getCell(row, col);
        if (dest == null || !dest.isWalkable()) {
            lastPortalMessage = "The other side is blocked.";
            return false;
        }
        if (hero != null && hero.getCurrentCell() != null) {
            hero.getCurrentCell().setOccupyingCharacter(null);
        }
        this.map = next;
        this.currentMapId = mapId;
        clearEnemies();
        if (shadowClone != null) {
            shadowClone = null;
        }
        if (hero != null) {
            hero.moveToCell(dest);
        }
        incrementFloorsCleared();
        mapChanged = true;
        lastPortalMessage = "Entered " + mapDisplayName(mapId) + ".";
        return true;
    }

    public boolean consumeMapChanged() {
        boolean changed = mapChanged;
        mapChanged = false;
        return changed;
    }

    private static String mapDisplayName(String mapId) {
        if ("hall".equals(mapId)) return "Entry Hall";
        if ("catacombs".equals(mapId)) return "Catacombs";
        if ("vault".equals(mapId)) return "Treasure Vault";
        return mapId;
    }
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

    public boolean isEditorTestPlay() { return editorTestPlay; }
    public void setEditorTestPlay(boolean editorTestPlay) { this.editorTestPlay = editorTestPlay; }

    public int getEnemiesDefeatedCount() { return enemiesDefeatedCount; }
    public void incrementEnemiesDefeated() { this.enemiesDefeatedCount++; }

    public int getItemsCollectedCount() { return itemsCollectedCount; }
    public void incrementItemsCollected() { this.itemsCollectedCount++; }

    public int getFloorsClearedCount() { return floorsClearedCount; }
    public void incrementFloorsCleared() { this.floorsClearedCount++; }
}
