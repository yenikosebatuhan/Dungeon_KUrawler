package com.kurawler.action;

import com.kurawler.effect.DamageEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;
import java.util.ArrayList;
import java.util.List;

/**
 * Action: One character attacks another.
 * Does NOT modify state — produces DamageEffect.
 */
public class AttackAction implements Action {

    private final Character attacker;
    private final Character defender;

    public AttackAction(Character attacker, Character defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();

        if (attacker.isAlive() && defender.isAlive()) {
            int damage = com.kurawler.engine.CombatResolver.calculateFinalDamage(attacker, defender);
            effects.add(new DamageEffect(attacker, defender, damage));
        }

        return effects;
    }
}
