package com.kurawler.model.character;

import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Ring;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ADT (Abstract Data Type) test for {@link Hero}.
 *
 * <h2>Overview</h2>
 * {@code Hero} represents the player character in Dungeon KUrawler.
 * It extends {@code Character} by adding two equipment slots (weapon, wearable)
 * and an inventory grid (2 rows × 4 columns, max 8 items).
 * Each stat (health, mana, energy) is a non-negative integer bounded by its
 * corresponding maximum. The equipped wearable's defence bonus is tracked by
 * adjusting {@code defensePower} in place when items are equipped or unequipped.
 *
 * <h2>Abstract Function</h2>
 * <pre>
 * AF(h) = Hero {
 *   name          = h.getName(),
 *   health        ∈ [0, maxHealth],   maxHealth = h.getMaxHealth()
 *   mana          ∈ [0, maxMana],     maxMana   = h.getMaxMana()
 *   energy        ∈ [0, maxEnergy],   maxEnergy = h.getMaxEnergy()
 *   strength      = h.getStrength()
 *   attackPower   = h.getAttackPower()
 *   defensePower  = h.getDefensePower()      // includes wearable bonus if equipped
 *   equippedWeapon  = h.getEquippedWeapon()  // null means unarmed
 *   equippedWearable= h.getEquippedWearable()// null means unarmoured
 *   inventory     = h.getInventory()
 *   attackDamage  = strength + (equippedWeapon != null ? equippedWeapon.getDamage() : 0)
 * }
 * </pre>
 *
 * <h2>Representation Invariant</h2>
 * <ul>
 *   <li>name != null and not empty</li>
 *   <li>maxHealth &gt; 0;  0 &le; health &le; maxHealth</li>
 *   <li>maxMana &ge; 0;    0 &le; mana &le; maxMana</li>
 *   <li>maxEnergy &ge; 0;  0 &le; energy &le; maxEnergy</li>
 *   <li>strength &ge; 0;   attackPower &ge; 0;   defensePower &ge; 0</li>
 *   <li>inventory != null</li>
 * </ul>
 *
 * The {@code repOk()} method that enforces these conditions is defined in
 * {@link Hero#repOk()}.
 */
class HeroADTTest {

    private Hero hero;

    @BeforeEach
    void setUp() {
        hero = new Hero("TestHero");
        hero.setStrength(10); // fix randomised STR
    }

    // ── repOk after construction and basic operations ─────────────────────────

    @Test
    void repOk_freshHero_holdsRI() {
        assertTrue(hero.repOk());
    }

    @Test
    void repOk_afterPartialDamage_holdsRI() {
        hero.takeDamage(5);
        assertTrue(hero.repOk());
    }

    @Test
    void repOk_afterLethalDamage_healthZeroStillValid() {
        hero.takeDamage(9999);
        assertEquals(0, hero.getHealth());
        // health == 0 is the "dead" state but NOT an RI violation
        assertTrue(hero.repOk());
    }

    @Test
    void repOk_afterEquipWearable_holdsRI() {
        hero.equipWearable(new Wearable("Iron Armor", "Heavy plate", 3));
        assertTrue(hero.repOk());
    }

    @Test
    void repOk_afterEquipAndUnequipRing_holdsRI() {
        hero.equipWearable(Ring.sorcererDrop());
        hero.unequipWearable();
        assertTrue(hero.repOk());
    }

    @Test
    void repOk_afterUsingConsumable_holdsRI() {
        Consumable potion = new Consumable("Potion", "Restores HP and mana", 5, 10, 0);
        hero.takeDamage(5);       // ensure there is room to heal
        hero.setMana(0);          // ensure there is room to restore mana
        hero.getInventory().addItem(potion);
        boolean consumed = hero.useConsumable(potion);
        assertTrue(consumed, "Consumable should be usable when it is in the inventory");
        assertTrue(hero.repOk());
    }

    // ── calculateDamage reflects AF correctly ─────────────────────────────────

    @Test
    void calculateDamage_withWeapon_equalsStrengthPlusWeaponDamage() {
        Weapon sword = new Weapon("Sword", "Sharp blade", 4);
        hero.setEquippedWeapon(sword);
        assertEquals(hero.getStrength() + 4, hero.calculateDamage());
        assertTrue(hero.repOk());
    }

    @Test
    void calculateDamage_withoutWeapon_equalsStrengthOnly() {
        hero.setEquippedWeapon(null);
        assertEquals(hero.getStrength(), hero.calculateDamage());
        assertTrue(hero.repOk());
    }
}
