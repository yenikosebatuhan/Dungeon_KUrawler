package com.kurawler.engine;

import com.kurawler.action.AttackAction;
import com.kurawler.controller.GameController;
import com.kurawler.model.character.Character;

/**
 * Handles combat resolution using the Action-Effect pipeline.
 */
public class CombatEngine {

    private final GameController controller;

    /**
     * @requires controller != null
     * @modifies this.controller
     * @effects Stores the controller for use in performAttack().
     */
    public CombatEngine(GameController controller) {
        this.controller = controller;
    }

    /**
     * Basic combat: attacker hits defender through the Action-Effect pipeline.
     *
     * @requires attacker != null && defender != null
     * @modifies attacker.energy, defender.health (via AttackAction pipeline)
     * @effects Creates an AttackAction and executes it through the controller,
     *          which applies the resulting DamageEffect to the defender.
     */
    public void performAttack(Character attacker, Character defender) {
        AttackAction attack = new AttackAction(attacker, defender);
        controller.executeAction(attack);
    }
}
