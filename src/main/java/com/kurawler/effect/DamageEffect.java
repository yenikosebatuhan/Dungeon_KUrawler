package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;

/**
 * Effect that deals damage to a character.
 */
public class DamageEffect implements Effect {

    private final Character source;
    private final Character target;
    private final int amount;

    /**
     * @requires target != null && amount >= 0
     * @modifies this.source, this.target, this.amount
     * @effects Creates a DamageEffect with no source character (e.g. trap damage).
     */
    public DamageEffect(Character target, int amount) {
        this(null, target, amount);
    }

    /**
     * @requires target != null && amount >= 0
     * @modifies this.source, this.target, this.amount
     * @effects Stores source, target, and damage amount for use in apply().
     */
    public DamageEffect(Character source, Character target, int amount) {
        this.source = source;
        this.target = target;
        this.amount = amount;
    }

    /**
     * @requires session != null && target != null
     * @modifies target.health, source (observer notifications if source is Hero)
     * @effects Deals amount damage to target. If source is a Hero, calls
     *          notifyAttackResolved() so observers (e.g. AchievementManager) are
     *          informed of the attack and whether the enemy was defeated.
     */
    @Override
    public void apply(GameSession session) {
        boolean wasAlive = target.isAlive();
        target.takeDamage(amount);
        if (source instanceof Hero) {
            boolean defeatedEnemy = target instanceof Enemy && wasAlive && !target.isAlive();
            ((Hero) source).notifyAttackResolved(amount, defeatedEnemy);
        }
        System.out.println("[Effect] " + target.getName() + " takes " + amount
                + " damage. HP: " + target.getHealth() + "/" + target.getMaxHealth());
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string describing the damage dealt and its target.
     */
    @Override
    public String describe() {
        return "Deal " + amount + " damage to " + target.getName();
    }
}
