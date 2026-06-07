package com.kurawler.engine;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;

/**
 * Resolves combat interactions between characters and projectiles.
 *
 * <p>Melee damage formula (PDF §3):
 * {@code Max(1, (attacker.STR / 2 + attacker.ATK) - defender.DEF)}.</p>
 */
public class CombatResolver {

    /**
     * Performs a melee attack from attacker to defender using the PDF formula.
     *
     * @requires attacker != null && defender != null
     * @modifies defender.health
     * @effects Calculates final damage via calculateFinalDamage() and applies it
     *          to defender. No-op if either character is null or not alive.
     */
    public static void performMeleeAttack(Character attacker, Character defender) {
        if (attacker == null || defender == null || !attacker.isAlive() || !defender.isAlive()) {
            return;
        }
        if (attacker instanceof Enemy && defender.getCurrentCell() != null) {
            ShadowCloneScrollRules.removeFromCell(defender.getCurrentCell());
        }
        if (defender instanceof ShadowClone) {
            return;
        }

        int finalDamage = calculateFinalDamage(attacker, defender);
        defender.takeDamage(finalDamage);
    }

    /**
     * Generated melee damage before DEF: {@code (STR / 2) + ATK}.
     * Hero/Knight weapon bonuses are included in ATK.
     */
    public static int calculateGeneratedDamage(Character attacker) {
        if (attacker == null) {
            return 0;
        }
        return (attacker.getStrength() / 2) + resolveAttackPower(attacker);
    }

    /**
     * Final melee damage after DEF: {@code Max(1, generated - defender.DEF)}.
     */
    public static int calculateFinalDamage(Character attacker, Character defender) {
        if (attacker == null || defender == null) {
            return 1;
        }
        int generated = calculateGeneratedDamage(attacker);
        return Math.max(1, generated - defender.getDefensePower());
    }

    /**
     * Resolves total ATK for the PDF formula, including equipped weapons where applicable.
     */
    static int resolveAttackPower(Character attacker) {
        if (attacker instanceof Hero hero) {
            return hero.getMeleeAttackPower();
        }
        if (attacker instanceof Knight knight) {
            int atk = knight.getAttackPower();
            Weapon weapon = knight.getEquippedMatchWeapon();
            if (weapon != null) {
                atk += weapon.getDamage();
            }
            return atk;
        }
        return attacker.getAttackPower();
    }

    /**
     * Applies projectile damage to a target character, respecting DEF but bypassing STR.
     *
     * @requires projectile != null && target != null
     * @modifies target.health
     * @effects Deals Max(1, projectile.damage - target.DEF) to target. No-op if
     *          either argument is null or target is not alive.
     */
    public static void applyProjectileDamage(Projectile projectile, Character target) {
        if (projectile == null || target == null || !target.isAlive()) {
            return;
        }
        if (target instanceof ShadowClone) {
            return;
        }

        int damage = projectile.getDamage() - target.getDefensePower();
        target.takeDamage(Math.max(1, damage));
    }
}
