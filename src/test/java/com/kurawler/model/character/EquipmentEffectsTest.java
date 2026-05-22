package com.kurawler.model.character;

import com.kurawler.model.object.Ring;
import com.kurawler.model.object.Wearable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ring and armor equip / unequip effects on Hero's DEF stat.
 *
 * Hero starts with defensePower = 2 (from constructor).
 * All tests capture baseDef in setUp and assert relative changes so the
 * tests remain correct if the default defence is ever adjusted.
 */
class EquipmentEffectsTest {

    private Hero hero;
    private int baseDef;

    @BeforeEach
    void setUp() {
        hero = new Hero("TestHero");
        hero.setStrength(10);
        baseDef = hero.getDefensePower(); // 2 by default
    }

    // ── Armor (generic Wearable) ──────────────────────────────────────────────

    @Test
    void equipWearable_increasesDefensePowerByBonus() {
        hero.equipWearable(new Wearable("Iron Armor", "Basic armor", 3));
        assertEquals(baseDef + 3, hero.getDefensePower());
    }

    @Test
    void unequipWearable_restoresDefensePowerToOriginal() {
        Wearable armor = new Wearable("Iron Armor", "Basic armor", 3);
        hero.equipWearable(armor);
        hero.unequipWearable();
        assertEquals(baseDef, hero.getDefensePower());
    }

    @Test
    void equipDifferentWearable_swapsBonusCorrectly() {
        // Equip first armor (+2), then second armor (+5).
        // Final DEF should be baseDef + 5, NOT baseDef + 2 + 5.
        hero.equipWearable(new Wearable("Light Armor", "Light", 2));
        hero.equipWearable(new Wearable("Heavy Armor", "Heavy", 5));
        assertEquals(baseDef + 5, hero.getDefensePower());
    }

    @Test
    void equipSameWearableTwice_doesNotStackBonus() {
        Wearable armor = new Wearable("Iron Armor", "Basic armor", 3);
        hero.equipWearable(armor);
        int defAfterFirst = hero.getDefensePower();
        hero.equipWearable(armor); // same reference — no-op per spec
        assertEquals(defAfterFirst, hero.getDefensePower());
    }

    // ── Ring ─────────────────────────────────────────────────────────────────

    @Test
    void equipSorcererRing_increasesDefByOne() {
        hero.equipWearable(Ring.sorcererDrop()); // +1 DEF
        assertEquals(baseDef + 1, hero.getDefensePower());
    }

    @Test
    void unequipRing_restoresOriginalDef() {
        hero.equipWearable(Ring.sorcererDrop());
        hero.unequipWearable();
        assertEquals(baseDef, hero.getDefensePower());
    }

    @Test
    void equipRingAfterArmor_replacesArmorBonus() {
        // Equip armor (+4), then ring (+2).
        // DEF should be baseDef + 2 (ring bonus), not baseDef + 4 + 2.
        hero.equipWearable(new Wearable("Iron Armor", "Heavy", 4)); // DEF = baseDef + 4
        hero.equipWearable(new Ring("Magic Ring", "Enchanted", 2)); // DEF = baseDef + 2
        assertEquals(baseDef + 2, hero.getDefensePower());
    }
}
