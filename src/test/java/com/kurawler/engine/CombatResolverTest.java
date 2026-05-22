package com.kurawler.engine;

import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Individual method tests for {@link CombatResolver#calculateFinalDamage}.
 *
 * <p>The tested method has the following specification:</p>
 * <ul>
 *   <li>REQUIRES: attacker != null; defender != null;
 *       attacker.getStrength() &ge; 0; attacker.getAttackPower() &ge; 0;
 *       defender.getDefensePower() &ge; 0</li>
 *   <li>MODIFIES: nothing (pure function)</li>
 *   <li>EFFECTS: Returns max(1, (attacker.STR / 2) + atk - defender.DEF),
 *       where atk = attacker.getAttackPower() + (equippedWeapon damage if Hero, else 0)</li>
 * </ul>
 */
class CombatResolverTest {

    private Hero attacker;
    private Hero defender;

    @BeforeEach
    void setUp() {
        attacker = new Hero("Attacker");
        attacker.setStrength(10);
        attacker.setAttackPower(0);
        attacker.setEquippedWeapon(null);

        defender = new Hero("Defender");
        defender.setStrength(0);
        defender.setDefensePower(0);
    }

    /**
     * Tests the basic formula: (STR/2) + ATK - DEF.
     * STR=10, ATK=0, no weapon vs DEF=2  →  (10/2) + 0 - 2 = 3
     */
    @Test
    void calculateFinalDamage_basicFormula_correctResult() {
        defender.setDefensePower(2);
        int damage = CombatResolver.calculateFinalDamage(attacker, defender);
        assertEquals(3, damage);
    }

    /**
     * Tests minimum-damage guarantee: result is always at least 1.
     * STR=0, ATK=0, no weapon vs DEF=100  →  max(1, 0+0-100) = 1
     */
    @Test
    void calculateFinalDamage_highDefender_minimumDamageIsOne() {
        attacker.setStrength(0);
        defender.setDefensePower(100);
        int damage = CombatResolver.calculateFinalDamage(attacker, defender);
        assertEquals(1, damage);
    }

    /**
     * Tests that equipped weapon damage is added to ATK for Hero attackers.
     * STR=10, weapon=5, ATK=0 vs DEF=0  →  (10/2) + 5 - 0 = 10
     */
    @Test
    void calculateFinalDamage_heroWithWeapon_weaponDamageAdded() {
        attacker.setEquippedWeapon(new Weapon("Sword", "A sharp sword", 5));
        defender.setDefensePower(0);
        int damage = CombatResolver.calculateFinalDamage(attacker, defender);
        assertEquals(10, damage);
    }

    /**
     * Tests that without a weapon no bonus is added.
     * STR=10, no weapon, ATK=0 vs DEF=0  →  (10/2) + 0 - 0 = 5
     */
    @Test
    void calculateFinalDamage_heroWithoutWeapon_noWeaponBonus() {
        attacker.setEquippedWeapon(null);
        defender.setDefensePower(0);
        int damage = CombatResolver.calculateFinalDamage(attacker, defender);
        assertEquals(5, damage);
    }
}
