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

    @Test
    void takeDamage_reducesHealthByGivenAmount() {
        int expected = hero.getHealth() - 5;
        hero.takeDamage(5);
        assertEquals(expected, hero.getHealth());
    }

    @Test
    void takeDamage_clampedAtZero_neverNegative() {
        hero.takeDamage(9999);
        assertEquals(0, hero.getHealth());
    }

    @Test
    void takeDamage_zeroDamage_doesNotChangeHealth() {
        int before = hero.getHealth();
        hero.takeDamage(0);
        assertEquals(before, hero.getHealth());
    }

    @Test
    void heal_increasesHealthByGivenAmount() {
        hero.takeDamage(10); // HP = maxHealth - 10
        int before = hero.getHealth();
        hero.heal(5);
        assertEquals(before + 5, hero.getHealth());
    }

    @Test
    void heal_clampedAtMaxHealth_neverExceedsMax() {
        hero.heal(9999);
        assertEquals(hero.getMaxHealth(), hero.getHealth());
    }

    @Test
    void isAlive_returnsFalse_afterLethalDamage() {
        hero.takeDamage(hero.getMaxHealth());
        assertFalse(hero.isAlive());
    }

    @Test
    void isAlive_returnsTrue_whenHealthIsPositive() {
        assertTrue(hero.isAlive());
    }

    // ── Mana ─────────────────────────────────────────────────────────────────

    @Test
    void useMana_returnsFalse_whenManaInsufficient() {
        hero.setMana(0);
        assertFalse(hero.useMana(10));
    }

    @Test
    void useMana_returnsTrueAndDecreasesMana_whenSufficient() {
        hero.setMana(50);
        boolean used = hero.useMana(20);
        assertTrue(used);
        assertEquals(30, hero.getMana());
    }

    @Test
    void restoreMana_increasesManaByClamped() {
        hero.setMana(10);
        hero.restoreMana(20);
        assertEquals(30, hero.getMana());
    }

    @Test
    void restoreMana_clampedAtMaxMana() {
        hero.setMana(0);
        hero.restoreMana(9999);
        assertEquals(hero.getMaxMana(), hero.getMana());
    }

    // ── Energy ───────────────────────────────────────────────────────────────

    @Test
    void useEnergy_returnsFalse_whenEnergyInsufficient() {
        hero.setEnergy(0);
        assertFalse(hero.useEnergy(1));
    }

    @Test
    void useEnergy_returnsTrueAndDecreasesEnergy_whenSufficient() {
        hero.setEnergy(50);
        boolean used = hero.useEnergy(20);
        assertTrue(used);
        assertEquals(30, hero.getEnergy());
    }

    @Test
    void restoreEnergy_increasesEnergyByClamped() {
        hero.setEnergy(10);
        hero.restoreEnergy(20);
        assertEquals(30, hero.getEnergy());
    }

    @Test
    void restoreEnergy_clampedAtMaxEnergy() {
        hero.setEnergy(0);
        hero.restoreEnergy(9999);
        assertEquals(hero.getMaxEnergy(), hero.getEnergy());
    }

    // ── STR / DEF ────────────────────────────────────────────────────────────

    @Test
    void setStrength_updatesStrengthCorrectly() {
        hero.setStrength(15);
        assertEquals(15, hero.getStrength());
    }

    @Test
    void setDefensePower_updatesDefensePowerCorrectly() {
        hero.setDefensePower(7);
        assertEquals(7, hero.getDefensePower());
    }
}
