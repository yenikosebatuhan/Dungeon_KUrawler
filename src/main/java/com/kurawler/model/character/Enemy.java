package com.kurawler.model.character;

/**
 * Abstract base for all enemy types.
 */
public abstract class Enemy extends Character {

    protected Enemy(String name, int maxHealth, int attackPower, int defensePower,
                    int strength, int maxMana, int maxEnergy) {
        super(name, maxHealth, attackPower, defensePower, strength, maxMana, maxEnergy);
    }
}
