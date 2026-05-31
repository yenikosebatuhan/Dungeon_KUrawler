package com.kurawler.model.achievement;

import com.kurawler.model.observer.HeroObserver;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Bonus feature: tracks hero events and unlocks achievements.
 *
 * This class is also an Observer of Hero, which keeps the feature independent
 * from movement, combat, inventory, and UI code.
 */
public class AchievementManager implements HeroObserver {

    private final List<Achievement> achievements = new ArrayList<>();
    private Consumer<Achievement> onUnlock;

    private int totalMoves;
    private int shadowCloneMoves;
    private int enemiesKilled;
    private int itemsCollected;
    private int damageTaken;

    private final Achievement firstBlood;
    private final Achievement explorer;
    private final Achievement survivor;
    private final Achievement treasureHunter;
    private final Achievement shadowDancer;

    public AchievementManager() {
        firstBlood = new Achievement("First Blood", "Defeat your first enemy");
        explorer = new Achievement("Explorer", "Move 50 tiles");
        survivor = new Achievement("Survivor", "Take 20 damage and survive");
        treasureHunter = new Achievement("Treasure Hunter", "Collect 5 items");
        shadowDancer = new Achievement("Shadow Dancer", "Move 30 times with the shadow clone");

        achievements.add(firstBlood);
        achievements.add(explorer);
        achievements.add(survivor);
        achievements.add(treasureHunter);
        achievements.add(shadowDancer);
    }

    public void setOnUnlock(Consumer<Achievement> callback) {
        this.onUnlock = callback;
    }

    @Override
    public void onHeroMoved(int newRow, int newCol, int deltaRow, int deltaCol) {
        totalMoves++;
        if (totalMoves >= 50) unlock(explorer);
    }

    public void onShadowCloneMoved() {
        shadowCloneMoves++;
        if (shadowCloneMoves >= 30) unlock(shadowDancer);
    }

    @Override
    public void onHeroAttacked(int damage) {
        // Current achievements do not use attack count, but this event is available for extension.
    }

    @Override
    public void onHeroDamageTaken(int amount) {
        damageTaken += amount;
        if (damageTaken >= 20) unlock(survivor);
    }

    @Override
    public void onEnemyDefeated() {
        enemiesKilled++;
        if (enemiesKilled >= 1) unlock(firstBlood);
    }

    @Override
    public void onItemCollected() {
        itemsCollected++;
        if (itemsCollected >= 5) unlock(treasureHunter);
    }

    public List<Achievement> getAchievements() { return new ArrayList<>(achievements); }
    public int getTotalMoves() { return totalMoves; }
    public int getShadowCloneMoves() { return shadowCloneMoves; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public int getItemsCollected() { return itemsCollected; }
    public int getDamageTaken() { return damageTaken; }

    public void restoreProgress(int totalMoves, int shadowCloneMoves, int enemiesKilled, int itemsCollected,
                                int damageTaken, List<String> unlockedNames) {
        this.totalMoves = Math.max(0, totalMoves);
        this.shadowCloneMoves = Math.max(0, shadowCloneMoves);
        this.enemiesKilled = Math.max(0, enemiesKilled);
        this.itemsCollected = Math.max(0, itemsCollected);
        this.damageTaken = Math.max(0, damageTaken);

        Set<String> unlocked = new HashSet<>();
        if (unlockedNames != null) {
            unlocked.addAll(unlockedNames);
        }

        for (Achievement achievement : achievements) {
            if (unlocked.contains(achievement.getName())) {
                achievement.unlock();
            }
        }
    }

    private void unlock(Achievement achievement) {
        if (achievement.isUnlocked()) return;
        achievement.unlock();
        if (onUnlock != null) onUnlock.accept(achievement);
    }
}
