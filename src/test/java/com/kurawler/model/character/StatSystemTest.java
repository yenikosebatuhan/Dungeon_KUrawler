package com.kurawler.model.character;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Character stat system (HP, Mana, Energy, STR, DEF).
 *
 * Uses Hero as the concrete Character instance (Character itself is abstract).
 * The Hero constructor randomises STR; we normalise it via setStrength(10)
 * in setUp() so all tests are deterministic.
 */
class StatSystemTest {

    private Hero hero;

    @BeforeEach
    void setUp() {
        hero = new Hero("TestHero");
        hero.setStrength(10); // eliminate randomness from constructor
    }

    // ── Health (HP) ──────────────────────────────────────────────────────────

    /**
     * Tests that takeDamage reduces health by exactly the given amount.
     * initial HP - 5  →  HP decreases by 5
     */
    @Test
    void takeDamage_reducesHealthByGivenAmount() {
        int expected = hero.getHealth() - 5;
        hero.takeDamage(5);
        assertEquals(expected, hero.getHealth());
    }

    /**
     * Tests that takeDamage clamps health at zero, never goes negative.
     * damage=9999 (exceeds max HP)  →  HP = 0
     */
    @Test
    void takeDamage_clampedAtZero_neverNegative() {
        hero.takeDamage(9999);
        assertEquals(0, hero.getHealth());
    }

    /**
     * Tests that takeDamage with 0 amount leaves health unchanged.
     * damage=0  →  HP unchanged
     */
    @Test
    void takeDamage_zeroDamage_doesNotChangeHealth() {
        int before = hero.getHealth();
        hero.takeDamage(0);
        assertEquals(before, hero.getHealth());
    }

    /**
     * Tests that heal increases health by the given amount.
     * HP reduced by 10, then healed by 5  →  HP = (initial - 10) + 5
     */
    @Test
    void heal_increasesHealthByGivenAmount() {
        hero.takeDamage(10); // HP = maxHealth - 10
        int before = hero.getHealth();
        hero.heal(5);
        assertEquals(before + 5, hero.getHealth());
    }

    /**
     * Tests that heal clamps at maxHealth, never exceeds it.
     * heal=9999  →  HP = maxHealth
     */
    @Test
    void heal_clampedAtMaxHealth_neverExceedsMax() {
        hero.heal(9999);
        assertEquals(hero.getMaxHealth(), hero.getHealth());
    }

    /**
     * Tests that isAlive returns false after lethal damage reduces HP to 0.
     * damage = maxHealth  →  isAlive() = false
     */
    @Test
    void isAlive_returnsFalse_afterLethalDamage() {
        hero.takeDamage(hero.getMaxHealth());
        assertFalse(hero.isAlive());
    }

    /**
     * Tests that isAlive returns true for a freshly created hero with positive HP.
     * fresh hero (no damage)  →  isAlive() = true
     */
    @Test
    void isAlive_returnsTrue_whenHealthIsPositive() {
        assertTrue(hero.isAlive());
    }

    // ── Mana ─────────────────────────────────────────────────────────────────

    /**
     * Tests that useMana returns false when mana is insufficient (mana=0).
     * mana=0, use=10  →  returns false, mana unchanged
     */
    @Test
    void useMana_returnsFalse_whenManaInsufficient() {
        hero.setMana(0);
        assertFalse(hero.useMana(10));
    }

    /**
     * Tests that useMana returns true and decreases mana when sufficient.
     * mana=50, use=20  →  returns true, mana=30
     */
    @Test
    void useMana_returnsTrueAndDecreasesMana_whenSufficient() {
        hero.setMana(50);
        boolean used = hero.useMana(20);
        assertTrue(used);
        assertEquals(30, hero.getMana());
    }

    /**
     * Tests that restoreMana increases mana by the given amount.
     * mana=10, restore=20  →  mana=30
     */
    @Test
    void restoreMana_increasesManaByClamped() {
        hero.setMana(10);
        hero.restoreMana(20);
        assertEquals(30, hero.getMana());
    }

    /**
     * Tests that restoreMana clamps at maxMana, never exceeds it.
     * mana=0, restore=9999  →  mana = maxMana
     */
    @Test
    void restoreMana_clampedAtMaxMana() {
        hero.setMana(0);
        hero.restoreMana(9999);
        assertEquals(hero.getMaxMana(), hero.getMana());
    }

    // ── Energy ───────────────────────────────────────────────────────────────

    /**
     * Tests that useEnergy returns false when energy is insufficient (energy=0).
     * energy=0, use=1  →  returns false
     */
    @Test
    void useEnergy_returnsFalse_whenEnergyInsufficient() {
        hero.setEnergy(0);
        assertFalse(hero.useEnergy(1));
    }

    /**
     * Tests that useEnergy returns true and decreases energy when sufficient.
     * energy=50, use=20  →  returns true, energy=30
     */
    @Test
    void useEnergy_returnsTrueAndDecreasesEnergy_whenSufficient() {
        hero.setEnergy(50);
        boolean used = hero.useEnergy(20);
        assertTrue(used);
        assertEquals(30, hero.getEnergy());
    }

    /**
     * Tests that restoreEnergy increases energy by the given amount.
     * energy=10, restore=20  →  energy=30
     */
    @Test
    void restoreEnergy_increasesEnergyByClamped() {
        hero.setEnergy(10);
        hero.restoreEnergy(20);
        assertEquals(30, hero.getEnergy());
    }

    /**
     * Tests that restoreEnergy clamps at maxEnergy, never exceeds it.
     * energy=0, restore=9999  →  energy = maxEnergy
     */
    @Test
    void restoreEnergy_clampedAtMaxEnergy() {
        hero.setEnergy(0);
        hero.restoreEnergy(9999);
        assertEquals(hero.getMaxEnergy(), hero.getEnergy());
    }

    // ── STR / DEF ────────────────────────────────────────────────────────────

    /**
     * Tests that setStrength stores and returns the correct value.
     * setStrength(15)  →  getStrength() = 15
     */
    @Test
    void setStrength_updatesStrengthCorrectly() {
        hero.setStrength(15);
        assertEquals(15, hero.getStrength());
    }

    /**
     * Tests that setDefensePower stores and returns the correct value.
     * setDefensePower(7)  →  getDefensePower() = 7
     */
    @Test
    void setDefensePower_updatesDefensePowerCorrectly() {
        hero.setDefensePower(7);
        assertEquals(7, hero.getDefensePower());
    }
}
