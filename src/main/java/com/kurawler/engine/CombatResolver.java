package com.kurawler.engine;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Projectile;

/**
 * Resolves combat interactions between characters and projectiles.
 *
 * <p>Sophisticated Damage Formula: {@code Max(1, (attacker.STR / 2 + weapon.ATK) - defender.DEF)}.</p>
 */
public class CombatResolver {

    /**
     * Performs a melee attack from attacker to defender using the new formula.
     */
    public static void performMeleeAttack(Character attacker, Character defender) {
        if (attacker == null || defender == null || !attacker.isAlive() || !defender.isAlive()) {
            return;
        }
        
        int finalDamage = calculateFinalDamage(attacker, defender);
        defender.takeDamage(finalDamage);
    }

    /**
     * Calculates the exact damage applying STR, ATK, and subtracting the defender's DEF.
     * Ensures damage is at least 1.
     *
     * @requires attacker != null && defender != null
     * @modifies none
     * @effects Calculates and returns the final damage value as an integer based on the 
     * formula: Max(1, (attacker.STR / 2 + attacker.ATK) - defender.DEF). 
     * If the attacker is a Hero, the equipped weapon's damage is included in ATK.
     * Does not mutate the state of the attacker or the defender.
     */
    public static int calculateFinalDamage(Character attacker, Character defender) {
        int str = attacker.getStrength();
        int atk = attacker.getAttackPower(); // Default base ATK

        // Add equipped weapon damage if the attacker is a Hero
        if (attacker instanceof Hero) {
            Hero hero = (Hero) attacker;
            if (hero.getEquippedWeapon() != null) {
                atk += hero.getEquippedWeapon().getDamage();
            }
        }

        int def = defender.getDefensePower();

        // Sophisticated PDF formula
        int damage = (str / 2) + atk - def;
        
        return Math.max(1, damage); // Minimum damage is 1
    }

    public static void applyProjectileDamage(Projectile projectile, Character target) {
        if (projectile == null || target == null || !target.isAlive()) {
            return;
        }
        
        // Projectiles bypass normal STR formula but still respect DEF
        int damage = projectile.getDamage() - target.getDefensePower();
        target.takeDamage(Math.max(1, damage));
    }
}