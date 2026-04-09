package com.kurawler.engine;

import com.kurawler.action.AttackAction;
import com.kurawler.controller.GameController;
import com.kurawler.model.character.Character;

/**
 * Handles combat resolution using the Action-Effect pipeline.
 */
public class CombatEngine {

    private final GameController controller;

    public CombatEngine(GameController controller) {
        this.controller = controller;
    }

    /** Basic combat: attacker hits defender through the Action-Effect pipeline. */
    public void performAttack(Character attacker, Character defender) {
        AttackAction attack = new AttackAction(attacker, defender);
        controller.executeAction(attack);
    }
}
