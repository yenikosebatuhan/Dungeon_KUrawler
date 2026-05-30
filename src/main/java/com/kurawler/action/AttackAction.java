package com.kurawler.action;

import com.kurawler.effect.DamageEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;

import java.util.ArrayList;
import java.util.List;

public class AttackAction implements Action {

    private final Character attacker;
    private final Character defender;

    public AttackAction(Character attacker, Character defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    /**
     * Resolves the combat interaction into a DamageEffect.
     *
     * @requires attacker != null && defender != null
     * @modifies attacker (energy state), defender (health through DamageEffect)
     * @effects Deducts 1 energy from the attacker if they are a Hero.
     * Produces a DamageEffect resolving the combat damage based on the centralized formula.
     * Returns an empty list if the attacker lacks energy or the combatants are dead.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();

        if (attacker.isAlive() && defender.isAlive()) {
            
            if (attacker instanceof Hero) {
                Hero hero = (Hero) attacker;
                if (!hero.useEnergy(1)) {
                    return effects; // Not enough energy to attack
                }
            }

            int damage = com.kurawler.engine.CombatResolver.calculateFinalDamage(attacker, defender);
            effects.add(new DamageEffect(attacker, defender, damage));
        }

        return effects;
    }
}