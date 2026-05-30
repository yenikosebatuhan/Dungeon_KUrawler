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

    public DamageEffect(Character target, int amount) {
        this(null, target, amount);
    }

    public DamageEffect(Character source, Character target, int amount) {
        this.source = source;
        this.target = target;
        this.amount = amount;
    }

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

    @Override
    public String describe() {
        return "Deal " + amount + " damage to " + target.getName();
    }
}
