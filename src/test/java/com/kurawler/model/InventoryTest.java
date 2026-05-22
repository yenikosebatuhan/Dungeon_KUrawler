package com.kurawler.model;

import com.kurawler.action.EquipAction;
import com.kurawler.effect.Effect;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the Inventory abstract data type (Issue #33).
 *
 * Overview:
 *   An Inventory is a fixed-capacity (2 x 4 = 8 slot) container of Items
 *   attached to the Hero. Items are not stackable; each occupies one slot.
 *
 * Abstraction Function:
 *   AF(slots) = the multiset of non-null Items currently stored in slots[][].
 *
 * Representation Invariant (RI):
 *   - 0 <= getItemCount() <= 8
 *   - no Item reference appears in more than one slot (no duplicates)
 *
 * repOk() below checks this invariant after each operation.
 */
class InventoryTest {

    private Inventory inv;

    @BeforeEach
    void setUp() {
        inv = new Inventory();
    }

    /** Verifies the representation invariant of an Inventory. */
    private boolean repOk(Inventory inv) {
        int count = inv.getItemCount();
        if (count < 0 || count > 8) {
            return false;
        }
        List<Item> items = inv.getItems();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                if (items.get(i) == items.get(j)) {
                    return false; // same Item reference stored twice
                }
            }
        }
        return true;
    }

    @Test
    void newInventoryIsEmpty() {
        assertEquals(0, inv.getItemCount());
        assertFalse(inv.isFull());
        assertTrue(repOk(inv));
    }

    @Test
    void addItemToEmptyInventorySucceeds() {            // TAKE action core
        Item sword = new Weapon("Iron Sword", "A sturdy blade", 8);
        assertTrue(inv.addItem(sword));
        assertEquals(1, inv.getItemCount());
        assertTrue(inv.hasItem(sword));
        assertTrue(repOk(inv));
    }

    @Test
    void addItemToFullInventoryFails() {                // TAKE when inventory full
        for (int i = 0; i < 8; i++) {
            assertTrue(inv.addItem(new Consumable("Potion" + i, "heals", 5)));
        }
        assertTrue(inv.isFull());
        Item extra = new Weapon("War Axe", "A heavy axe", 12);
        assertFalse(inv.addItem(extra));                // rejected: no empty slot
        assertEquals(8, inv.getItemCount());
        assertFalse(inv.hasItem(extra));
        assertTrue(repOk(inv));
    }

    @Test
    void removeExistingItemSucceeds() {                 // DISCARD action core
        Item sword = new Weapon("Iron Sword", "blade", 8);
        inv.addItem(sword);
        assertTrue(inv.removeItem(sword));
        assertEquals(0, inv.getItemCount());
        assertFalse(inv.hasItem(sword));
        assertTrue(repOk(inv));
    }

    @Test
    void removeNonExistentItemFails() {                 // DISCARD of an absent item
        Item ghost = new Weapon("Ghost Blade", "not added", 1);
        assertFalse(inv.removeItem(ghost));
        assertEquals(0, inv.getItemCount());
        assertTrue(repOk(inv));
    }

    @Test
    void isFullBoundary() {                             // capacity boundary 7 vs 8
        for (int i = 0; i < 7; i++) {
            inv.addItem(new Consumable("P" + i, "h", 5));
        }
        assertFalse(inv.isFull());                      // 7 items -> not full
        inv.addItem(new Consumable("P8", "h", 5));
        assertTrue(inv.isFull());                       // 8 items -> full
        assertTrue(repOk(inv));
    }

    @Test
    void equipKeepsWeaponInInventory() {              // USE-EQUIP: TAKE != EQUIP (PDF §2.4.2)
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Iron Sword", "blade", 8);
        assertTrue(hero.getInventory().addItem(sword));

        applyEquipAction(hero, sword);

        assertSame(sword, hero.getEquippedWeapon());
        assertTrue(hero.getInventory().hasItem(sword));
        assertEquals(1, hero.getInventory().getItemCount());
        assertTrue(repOk(hero.getInventory()));
    }

    @Test
    void unequipKeepsWeaponInInventory() {            // USE-EQUIP toggle off; item stays in container
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Iron Sword", "blade", 8);
        hero.getInventory().addItem(sword);
        applyEquipAction(hero, sword);
        applyEquipAction(hero, sword);                // second apply unequips

        assertNull(hero.getEquippedWeapon());
        assertTrue(hero.getInventory().hasItem(sword));
        assertEquals(1, hero.getInventory().getItemCount());
        assertTrue(repOk(hero.getInventory()));
    }

    @Test
    void equipFailsWhenWeaponNotInInventory() {       // USE-EQUIP requires item in container
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Ghost Blade", "not owned", 5);
        GameSession session = new GameSession();
        session.setHero(hero);

        assertTrue(new EquipAction(sword).resolve(session).isEmpty());
        assertNull(hero.getEquippedWeapon());
        assertFalse(hero.getInventory().hasItem(sword));
        assertTrue(repOk(hero.getInventory()));
    }

    private static void applyEquipAction(Hero hero, Weapon weapon) {
        GameSession session = new GameSession();
        session.setHero(hero);
        for (Effect effect : new EquipAction(weapon).resolve(session)) {
            effect.apply(session);
        }
    }
}
