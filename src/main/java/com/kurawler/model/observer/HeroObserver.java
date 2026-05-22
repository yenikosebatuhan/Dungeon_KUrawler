package com.kurawler.model.observer;

/**
 * Observer role for systems that react to important hero events.
 */
public interface HeroObserver {
    void onHeroMoved(int newRow, int newCol, int deltaRow, int deltaCol);
    void onHeroAttacked(int damage);
    void onHeroDamageTaken(int amount);
    void onEnemyDefeated();
    void onItemCollected();
}
