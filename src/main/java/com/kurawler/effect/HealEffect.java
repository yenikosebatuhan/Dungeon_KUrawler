package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;

/**
 * Effect that heals a character.
 */
public class HealEffect implements Effect {

    private final Character target;
    private final int amount;

    /**
     * @requires target != null && amount >= 0
     * @modifies this.target, this.amount
     * @effects Stores target and heal amount for use in apply().
     */
    public HealEffect(Character target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    /**
     * @requires session != null && target != null && amount >= 0
     * @modifies target.health
     * @effects Increases target's health by amount, capped at maxHealth.
     */
    @Override
    public void apply(GameSession session) {
        target.heal(amount);
        System.out.println("[Effect] " + target.getName() + " heals for " + amount
                + ". HP: " + target.getHealth() + "/" + target.getMaxHealth());
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string describing the heal target and amount.
     */
    @Override
    public String describe() {
        return "Heal " + target.getName() + " for " + amount;
    }
}
