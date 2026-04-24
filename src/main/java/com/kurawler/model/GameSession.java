package com.kurawler.model;

import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import com.kurawler.model.character.Enemy;

/**
 * Central controller managing the game's lifecycle and primary state variables.
 * Tracks the current mode (Play/Design), hero status, map reference, and
 * enemy population. This class serves as the root of the game state hierarchy.
 */
public class GameSession {
    private int playDurationTimer;
    private int activeEnemyCount;
    private String currentMode; // "Play" or "Design"
    private Map map;
    private Hero heroTracker;
    private Item targetObjective;
    
    /**
     * Constructs a new GameSession with default values.
     * Initial mode is "Design" to allow map setup before gameplay.
     */
    public GameSession() {
        this.playDurationTimer = 0;
        this.activeEnemyCount = 0;
        this.currentMode = "Design"; // Start in design mode
        this.map = null;
        this.heroTracker = null;
        this.targetObjective = null;
    }
    
    /**
     * Initializes and starts the gameplay session.
     * Resets timers and switches to "Play" mode.
     */
    public void startGame() {
        this.currentMode = "Play";
        this.playDurationTimer = 0;
        this.activeEnemyCount = 0;
    }
    
    /**
     * Pauses the game, halting all system updates.
     * The current state is preserved for resumption.
     */
    public void pauseGame() {
        // In a full implementation, this would signal the game loop to stop
        // For now, we just maintain the current mode
        // The game loop will check this state and stop updates
    }
    
    /**
     * Checks if the win condition has been met.
     * Win condition: Hero's inventory contains the target objective item.
     * @return true if game is won, false otherwise
     */
    public boolean checkWinCondition() {
        // Cannot win without a hero or target objective
        if (heroTracker == null || targetObjective == null) {
            return false;
        }
        
        // Check if hero's inventory contains the target objective
        Inventory heroInventory = heroTracker.getInventory();
        if (heroInventory == null) {
            return false;
        }
        
        return heroInventory.hasItem(targetObjective);
    }
    
    /**
     * Gets the current game map.
     * @return The Map instance, or null if not set
     */
    public Map getMap() {
        return this.map;
    }
    
    /**
     * Sets the game map.
     * @param map The Map to use for this session
     */
    public void setMap(Map map) {
        this.map = map;
    }
    
    /**
     * Gets the hero being tracked in this session.
     * @return The Hero instance, or null if not set
     */
    public Hero getHero() {
        return this.heroTracker;
    }
    
    /**
     * Sets the hero for this session.
     * @param hero The Hero to track
     */
    public void setHero(Hero hero) {
        this.heroTracker = hero;
    }
    
    public Enemy getEnemyAt(int row, int col) {
        if (this.map == null) return null;
        
        GridCell cell = map.getCellAt(col, row);
        if (cell != null && cell.getOccupyingCharacter() instanceof Enemy) {
            return (Enemy) cell.getOccupyingCharacter();
        }
        return null;
    }
    /**
     * Gets the current count of active enemies on the map.
     * @return The number of enemies currently spawned
     */
    public int getActiveEnemyCount() {
        return activeEnemyCount;
    }
    
    /**
     * Increments the active enemy count by one.
     * Called when a new enemy spawns.
     */
    public void incrementEnemyCount() {
        this.activeEnemyCount++;
    }
    
    /**
     * Decrements the active enemy count by one.
     * Called when an enemy is defeated or removed.
     * Count cannot go below zero.
     */
    public void decrementEnemyCount() {
        if (this.activeEnemyCount > 0) {
            this.activeEnemyCount--;
        }
    }
    
    /**
     * Gets the current game mode.
     * @return "Play" or "Design"
     */
    public String getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Sets the game mode.
     * @param mode The mode to switch to ("Play" or "Design")
     * @throws IllegalArgumentException if mode is invalid
     */
    public void setCurrentMode(String mode) {
        if (!"Play".equals(mode) && !"Design".equals(mode)) {
            throw new IllegalArgumentException(
                "Invalid mode: " + mode + ". Must be 'Play' or 'Design'"
            );
        }
        this.currentMode = mode;
    }
    
    /**
     * Gets the play duration timer.
     * @return The elapsed play time in appropriate units
     */
    public int getPlayDurationTimer() {
        return playDurationTimer;
    }
    
    /**
     * Sets the play duration timer.
     * @param timer The timer value to set
     */
    public void setPlayDurationTimer(int timer) {
        this.playDurationTimer = Math.max(0, timer);
    }
    
    /**
     * Gets the target objective item that must be found to win.
     * @return The target Item, or null if not set
     */
    public Item getTargetObjective() {
        return targetObjective;
    }
    
    /**
     * Sets the target objective item.
     * @param item The Item that must be found to win
     */
    public void setTargetObjective(Item item) {
        this.targetObjective = item;
    }
    
    @Override
    public String toString() {
        return "GameSession{mode='" + currentMode + 
               "', enemies=" + activeEnemyCount +
               ", timer=" + playDurationTimer + "}";
    }
}