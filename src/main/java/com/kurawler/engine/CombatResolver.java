package com.kurawler.engine;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Projectile;

/**
 * Resolves combat interactions between characters and projectiles.
 *
 * <p>Damage formula for melee: {@code (attacker.STR + weapon.ATK) - defender.DEF}.
 * The defender's DEF is already subtracted inside {@link Character#takeDamage(int)},
 * so this class computes the raw outgoing damage only.</p>
 */
public class CombatResolver {

    /**
     * Performs a melee attack from attacker to defender.
     * Damage is calculated as attacker's STR plus equipped weapon ATK (if Hero).
     * The defender's DEF is applied inside {@link Character#takeDamage(int)}.
     *
     * @param attacker the character dealing damage; must not be null
     * @param defender the character receiving damage; must not be null
     */
    public static void performMeleeAttack(Character attacker, Character defender) {
        if (attacker == null || defender == null) {
            return;
        }
        if (!attacker.isAlive() || !defender.isAlive()) {
            return;
        }
        int baseDamage = calculateBaseDamage(attacker);
        defender.takeDamage(baseDamage);
    }

    /**
     * Calculates raw outgoing damage for a character.
     * For a Hero, adds the equipped weapon's damage to STR.
     * For other characters, uses STR only.
     *
     * @param attacker the attacking character
     * @return the raw damage value before defender's DEF is applied
     */
    private static int calculateBaseDamage(Character attacker) {
        int damage = attacker.getStrength();

        if (attacker instanceof Hero) {
            Hero hero = (Hero) attacker;
            if (hero.getEquippedWeapon() != null) {
                damage += hero.getEquippedWeapon().getDamage();
            }
        }

        return damage;
    }

    /**
     * Applies projectile damage to a target character.
     * Uses the projectile's damage value directly; defender's DEF
     * is applied inside {@link Character#takeDamage(int)}.
     *
     * @param projectile the projectile hitting the target; must not be null
     * @param target     the character being hit; must not be null
     */
    public static void applyProjectileDamage(Projectile projectile, Character target) {
        if (projectile == null || target == null) {
            return;
        }
        if (!target.isAlive()) {
            return;
        }
        target.takeDamage(projectile.getDamage());
    }
}
