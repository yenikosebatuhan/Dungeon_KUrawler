package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;

/**
 * Effect that heals a character.
 */
public class HealEffect implements Effect {

    private final Character target;
    private final int amount;

    public HealEffect(Character target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(GameSession session) {
        target.heal(amount);
        System.out.println("[Effect] " + target.getName() + " heals for " + amount
                + ". HP: " + target.getHealth() + "/" + target.getMaxHealth());
    }

    @Override
    public String describe() {
        return "Heal " + target.getName() + " for " + amount;
    }
}
