package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.Map;

public abstract class Enemy extends Character {

    /**
     * @requires name != null && maxHealth > 0
     * @modifies this (via Character constructor)
     * @effects Initialises the enemy's combat stats through the Character constructor.
     */
    protected Enemy(String name, int maxHealth, int attackPower, int defensePower,
                    int strength, int maxMana, int maxEnergy) {
        super(name, maxHealth, attackPower, defensePower, strength, maxMana, maxEnergy);
    }

    /**
     * Subclasses implement their own AI. Target can be any Character (Hero in
     * classic mode, nearest opponent in team match).
     *
     * @requires map != null
     * @modifies this.row, this.col, this.currentCell, target.health (via attacks)
     * @effects Executes one turn of the enemy's AI: moves toward or attacks the
     *          target depending on distance and subclass strategy.
     */
    public abstract void decideAction(Character target, Map map, GameSession session);
}
