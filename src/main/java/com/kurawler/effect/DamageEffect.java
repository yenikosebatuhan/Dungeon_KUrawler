package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;

/**
 * Effect that deals damage to a character.
 */
public class DamageEffect implements Effect {

    private final Character target;
    private final int amount;

    public DamageEffect(Character target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(GameSession session) {
        target.takeDamage(amount);
        System.out.println("[Effect] " + target.getName() + " takes " + amount
                + " damage. HP: " + target.getHealth() + "/" + target.getMaxHealth());
    }

    @Override
    public String describe() {
        return "Deal " + amount + " damage to " + target.getName();
    }
}
