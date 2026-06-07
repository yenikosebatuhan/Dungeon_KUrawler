package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameSession {
    private com.kurawler.model.Map map;
    private final java.util.Map<String, com.kurawler.model.Map> worldMaps = new HashMap<>();
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
    /** Launch mode for save/load filtering: Default, Custom, Random, TeamMatch, Play, Design. */
    private String savedGameType = "Play";
    /** Wall-clock anchor for Shadow Clone Scroll auto-spawn (persisted across save/load). */
    private long lastShadowCloneScrollSpawnMs;
    /** Elapsed ms toward the next procedural enemy spawn (PDF §2.5). */
    private long enemySpawnAccumulatorMs;
    /** True when playing a map launched from the level editor (no game-over popup on editor). */
    private boolean editorTestPlay;

    /** Hero inventory + equipped items captured at the moment of entering a floor. */
    private List<Item> floorEntryInventorySnapshot = new ArrayList<>();
    private Weapon floorEntryWeapon;
    private Weapon floorEntryArrow;
    private Wearable floorEntryArmor;
    private Wearable floorEntryShield;
    private Wearable floorEntryRing;

    /** Team Match save/load: enemy → team name ("Team A" / "Team B"). */
    private final java.util.Map<Enemy, String> savedEnemyTeamNames = new HashMap<>();

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

    public GameSession(com.kurawler.model.Map map) {
        this();
        this.map = map;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public void addProjectile(Projectile p) {
        if (p != null) {
            projectiles.add(p);
        }
    }

    public void removeProjectile(Projectile p) {
        projectiles.remove(p);
    }

    public void startGame() {
        this.currentMode = "Play";
        this.playDurationTimer = 0;
    }

    public void pauseGame() { /* Pause logic */ }

    public boolean checkWinCondition() {
        if (hero == null || targetObjective == null) {
            return false;
        }
        Inventory inv = hero.getInventory();
        if (inv == null) {
            return false;
        }
        if (inv.hasItem(targetObjective)) {
            return true;
        }
        for (Item item : inv.getItems()) {
            if (isSameObjective(item, targetObjective)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameObjective(Item item, Item target) {
        if (item == null || target == null) {
            return false;
        }
        if (!item.getName().equals(target.getName())) {
            return false;
        }
        if (item instanceof Key && target instanceof Key) {
            return ((Key) item).getKeyId().equals(((Key) target).getKeyId());
        }
        return item.getClass().equals(target.getClass());
    }

    public String getCurrentMode() { return currentMode; }
    public void setCurrentMode(String mode) { this.currentMode = mode; }

    public String getSavedGameType() {
        return savedGameType != null ? savedGameType : "Play";
    }

    public void setSavedGameType(String savedGameType) {
        this.savedGameType = savedGameType != null ? savedGameType : "Play";
    }

    public long getLastShadowCloneScrollSpawnMs() {
        return lastShadowCloneScrollSpawnMs;
    }

    public void setLastShadowCloneScrollSpawnMs(long lastShadowCloneScrollSpawnMs) {
        this.lastShadowCloneScrollSpawnMs = Math.max(0L, lastShadowCloneScrollSpawnMs);
    }

    public long getEnemySpawnAccumulatorMs() {
        return enemySpawnAccumulatorMs;
    }

    public void setEnemySpawnAccumulatorMs(long enemySpawnAccumulatorMs) {
        this.enemySpawnAccumulatorMs = Math.max(0L, enemySpawnAccumulatorMs);
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty != null ? difficulty : "Normal";
    }

    public static int maxEnergyForDifficulty(String difficulty) {
        if ("Easy".equals(difficulty)) {
            return 150;
        }
        if ("Hard".equals(difficulty)) {
            return 75;
        }
        return 100;
    }

    public void applyDifficultyHeroStats() {
        applyDifficultyHeroStats(true);
    }

    public void applyDifficultyHeroStats(boolean fillToMax) {
        Hero hero = getHero();
        if (hero == null) {
            return;
        }
        int cap = maxEnergyForDifficulty(difficulty);
        hero.setMaxEnergy(cap);
        if (fillToMax) {
            hero.setEnergy(cap);
        } else if (hero.getEnergy() > cap) {
            hero.setEnergy(hero.getEnergy());
        }
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

    public com.kurawler.model.Map getMap() { return map; }
    public void setMap(com.kurawler.model.Map map) { this.map = map; }

    public java.util.Map<String, com.kurawler.model.Map> getWorldMaps() { return worldMaps; }

    public void registerWorldMap(String mapId, com.kurawler.model.Map worldMap) {
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

    public boolean transitionHeroTo(String mapId, int row, int col) {
        com.kurawler.model.Map next = worldMaps.get(mapId);
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
        for (int r = 0; r < next.getHeight(); r++) {
            for (int c = 0; c < next.getWidth(); c++) {
                GridCell cell = next.getCell(r, c);
                if (cell == null) {
                    continue;
                }
                if (cell.getOccupyingCharacter() instanceof Enemy e && e.isAlive()) {
                    addEnemy(e);
                }
            }
        }
        if (shadowClone != null) {
            setShadowClone(null);
        }
        if (hero != null) {
            hero.moveToCell(dest);
        }
        snapshotFloorInventory();
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
        if ("hall".equals(mapId)) {
            return "Entry Hall";
        }
        if ("catacombs".equals(mapId)) {
            return "Catacombs";
        }
        if ("vault".equals(mapId)) {
            return "Treasure Vault";
        }
        return mapId;
    }

    public Hero getHero() { return hero; }
    public void setHero(Hero hero) { this.hero = hero; }
    public ShadowClone getShadowClone() { return shadowClone; }

    /** True when an active shadow clone occupies {@code (row, col)} — enemies may not enter that tile. */
    public boolean isActiveShadowCloneAt(int row, int col) {
        if (shadowClone == null || !shadowClone.isAlive() || shadowClone.isExpired()) {
            return false;
        }
        return shadowClone.getRow() == row && shadowClone.getCol() == col;
    }

    public void setShadowClone(ShadowClone shadowClone) {
        if (this.shadowClone != null) {
            this.shadowClone.setOnMove(null);
            if (hero != null) {
                hero.removeObserver(this.shadowClone);
            }
        }
        this.shadowClone = shadowClone;
        wireShadowClone();
    }

    public AchievementManager getAchievementManager() { return achievementManager; }
    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
        wireShadowClone();
    }

    private void wireShadowClone() {
        if (shadowClone == null) {
            return;
        }
        shadowClone.setOnMove(achievementManager != null
                ? achievementManager::onShadowCloneMoved
                : null);
        if (hero != null) {
            hero.addObserver(shadowClone);
        }
    }

    public void wireObservers() {
        if (hero == null) {
            return;
        }
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
        if (removed) {
            decrementEnemyCount();
            savedEnemyTeamNames.remove(enemy);
        }
        return removed;
    }
    public Enemy getEnemyAt(int row, int col) {
        for (Enemy e : enemies) {
            if (e.getRow() == row && e.getCol() == col && e.isAlive()) {
                return e;
            }
        }
        return null;
    }

    /** Records team assignment while deserializing Team Match saves. */
    public void setEnemyTeamName(Enemy enemy, String teamName) {
        if (enemy != null && teamName != null && !teamName.isBlank()) {
            savedEnemyTeamNames.put(enemy, teamName);
        }
    }

    public String getSavedEnemyTeamName(Enemy enemy) {
        return savedEnemyTeamNames.get(enemy);
    }

    public boolean isGameOver() { return hero != null && !hero.isAlive(); }
    public boolean isEditorTestPlay() { return editorTestPlay; }
    public void setEditorTestPlay(boolean editorTestPlay) {
        this.editorTestPlay = editorTestPlay;
    }
    public int getEnemiesDefeatedCount() { return enemiesDefeatedCount; }
    public void incrementEnemiesDefeated() { this.enemiesDefeatedCount++; }
    public void setEnemiesDefeatedCount(int count) { this.enemiesDefeatedCount = Math.max(0, count); }
    public int getItemsCollectedCount() { return itemsCollectedCount; }
    public void incrementItemsCollected() { this.itemsCollectedCount++; }
    public void setItemsCollectedCount(int count) { this.itemsCollectedCount = Math.max(0, count); }
    public int getFloorsClearedCount() { return floorsClearedCount; }
    public void incrementFloorsCleared() { this.floorsClearedCount++; }
    public void setFloorsClearedCount(int count) { this.floorsClearedCount = Math.max(0, count); }

    public void snapshotFloorInventory() {
        floorEntryInventorySnapshot = new ArrayList<>();
        if (hero == null) {
            return;
        }
        floorEntryInventorySnapshot.addAll(hero.getInventory().getItems());
        floorEntryWeapon = hero.getEquippedWeapon();
        floorEntryArrow = hero.getEquippedArrow();
        floorEntryArmor = hero.getEquippedArmor();
        floorEntryShield = hero.getEquippedShield();
        floorEntryRing = hero.getEquippedRing();
    }

    public void restoreFloorInventory() {
        if (hero == null) {
            return;
        }
        for (Item item : new ArrayList<>(hero.getInventory().getItems())) {
            if (!floorEntryInventorySnapshot.contains(item)) {
                hero.getInventory().removeItem(item);
                if (item instanceof Weapon) {
                    if (hero.getEquippedWeapon() == item) {
                        hero.setEquippedWeapon(null);
                    }
                    if (hero.getEquippedArrow() == item) {
                        hero.setEquippedArrow(null);
                    }
                } else if (item instanceof Wearable) {
                    hero.unequipWearable((Wearable) item);
                }
            }
        }
        if (hero.getEquippedWeapon() == null && floorEntryWeapon != null
                && hero.getInventory().hasItem(floorEntryWeapon)) {
            hero.setEquippedWeapon(floorEntryWeapon);
        }
        if (hero.getEquippedArrow() == null && floorEntryArrow != null
                && hero.getInventory().hasItem(floorEntryArrow)) {
            hero.setEquippedArrow(floorEntryArrow);
        }
        if (floorEntryArmor != null && hero.getInventory().hasItem(floorEntryArmor)
                && !hero.isWearableEquipped(floorEntryArmor)) {
            hero.equipWearable(floorEntryArmor);
        }
        if (floorEntryShield != null && hero.getInventory().hasItem(floorEntryShield)
                && !hero.isWearableEquipped(floorEntryShield)) {
            hero.equipWearable(floorEntryShield);
        }
        if (floorEntryRing != null && hero.getInventory().hasItem(floorEntryRing)
                && !hero.isWearableEquipped(floorEntryRing)) {
            hero.equipWearable(floorEntryRing);
        }
    }

    /** Restart Level: refill hero vitals to their current maximums. */
    public void restoreHeroToFullVitality() {
        if (hero == null) {
            return;
        }
        hero.setHealth(hero.getMaxHealth());
        hero.setMana(hero.getMaxMana());
        hero.setEnergy(hero.getMaxEnergy());
    }

    /** Copies runtime play state from another session that shares the same map instance. */
    public void copyPlayStateFrom(GameSession other) {
        if (other == null) {
            return;
        }
        setHero(other.getHero());
        clearEnemies();
        for (Enemy enemy : other.getEnemies()) {
            addEnemy(enemy);
        }
        savedEnemyTeamNames.clear();
        savedEnemyTeamNames.putAll(other.savedEnemyTeamNames);
        setShadowClone(other.getShadowClone());
        setAchievementManager(other.getAchievementManager());
        setPlayDurationTimer(other.getPlayDurationTimer());
        setDifficulty(other.getDifficulty());
        setSavedGameType(other.getSavedGameType());
        setLastShadowCloneScrollSpawnMs(other.getLastShadowCloneScrollSpawnMs());
        setEnemySpawnAccumulatorMs(other.getEnemySpawnAccumulatorMs());
        setEnemiesDefeatedCount(other.getEnemiesDefeatedCount());
        setItemsCollectedCount(other.getItemsCollectedCount());
        setFloorsClearedCount(other.getFloorsClearedCount());
        setTargetObjective(other.getTargetObjective());
        setEditorTestPlay(other.isEditorTestPlay());
    }
}
