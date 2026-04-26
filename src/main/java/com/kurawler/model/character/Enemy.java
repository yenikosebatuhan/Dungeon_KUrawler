package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.Map;

public abstract class Enemy extends Character {

    protected Enemy(String name, int maxHealth, int attackPower, int defensePower,
                    int strength, int maxMana, int maxEnergy) {
        super(name, maxHealth, attackPower, defensePower, strength, maxMana, maxEnergy);
    }

    /** Subclasses implement their own AI. */
    public abstract void decideAction(Hero hero, Map map, GameSession session);
}
