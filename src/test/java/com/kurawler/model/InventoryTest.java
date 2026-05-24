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
 * JUnit tests for the Inventory abstract data type (Issue #33, #41).
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
 * Tests target required spec behaviour (TAKE, DISCARD, container, USE-EQUIP),
 * not incidental UI or movement bugs. repOk() checks the RI after each test.
 */
class InventoryTest {

    private Inventory inv;

    @BeforeEach
    void setUp() {
        inv = new Inventory();
    }

    /**
     * Verifies the representation invariant of an Inventory after mutations.
     *
     * @param inv inventory to check
     * @return true if count is in [0, 8] and no duplicate item references exist
     */
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

    /**
     * Container: a newly created inventory must start empty and not report full.
     */
    @Test
    void newInventoryIsEmpty() {
        assertEquals(0, inv.getItemCount());
        assertFalse(inv.isFull());
        assertTrue(repOk(inv));
    }

    /**
     * TAKE: addItem on an empty inventory must succeed (item enters a slot).
     */
    @Test
    void addItemToEmptyInventorySucceeds() {
        Item sword = new Weapon("Iron Sword", "A sturdy blade", 8);
        assertTrue(inv.addItem(sword));
        assertEquals(1, inv.getItemCount());
        assertTrue(inv.hasItem(sword));
        assertTrue(repOk(inv));
    }

    /**
     * TAKE: when all 8 slots are full, addItem must fail and leave inventory unchanged.
     */
    @Test
    void addItemToFullInventoryFails() {
        for (int i = 0; i < 8; i++) {
            assertTrue(inv.addItem(new Consumable("Potion" + i, "heals", 5)));
        }
        assertTrue(inv.isFull());
        Item extra = new Weapon("War Axe", "A heavy axe", 12);
        assertFalse(inv.addItem(extra));
        assertEquals(8, inv.getItemCount());
        assertFalse(inv.hasItem(extra));
        assertTrue(repOk(inv));
    }

    /**
     * DISCARD: removeItem must succeed when the item is present in the inventory.
     */
    @Test
    void removeExistingItemSucceeds() {
        Item sword = new Weapon("Iron Sword", "blade", 8);
        inv.addItem(sword);
        assertTrue(inv.removeItem(sword));
        assertEquals(0, inv.getItemCount());
        assertFalse(inv.hasItem(sword));
        assertTrue(repOk(inv));
    }

    /**
     * DISCARD: removeItem must fail when the item was never added; count stays 0.
     */
    @Test
    void removeNonExistentItemFails() {
        Item ghost = new Weapon("Ghost Blade", "not added", 1);
        assertFalse(inv.removeItem(ghost));
        assertEquals(0, inv.getItemCount());
        assertTrue(repOk(inv));
    }

    /**
     * Container: isFull must be false with 7 items and true with 8 (2x4 capacity).
     */
    @Test
    void isFullBoundary() {
        for (int i = 0; i < 7; i++) {
            inv.addItem(new Consumable("P" + i, "h", 5));
        }
        assertFalse(inv.isFull());
        inv.addItem(new Consumable("P8", "h", 5));
        assertTrue(inv.isFull());
        assertTrue(repOk(inv));
    }

    /**
     * USE-EQUIP: EquipAction equips the weapon but must not remove it from inventory (TAKE != EQUIP).
     */
    @Test
    void equipKeepsWeaponInInventory() {
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Iron Sword", "blade", 8);
        assertTrue(hero.getInventory().addItem(sword));

        applyEquipAction(hero, sword);

        assertSame(sword, hero.getEquippedWeapon());
        assertTrue(hero.getInventory().hasItem(sword));
        assertEquals(1, hero.getInventory().getItemCount());
        assertTrue(repOk(hero.getInventory()));
    }

    /**
     * USE-EQUIP: a second EquipAction on the same weapon unequips it; item still remains in inventory.
     */
    @Test
    void unequipKeepsWeaponInInventory() {
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Iron Sword", "blade", 8);
        hero.getInventory().addItem(sword);
        applyEquipAction(hero, sword);
        applyEquipAction(hero, sword);

        assertNull(hero.getEquippedWeapon());
        assertTrue(hero.getInventory().hasItem(sword));
        assertEquals(1, hero.getInventory().getItemCount());
        assertTrue(repOk(hero.getInventory()));
    }

    /**
     * USE-EQUIP: EquipAction must produce no effects if the weapon is not in the hero's inventory.
     */
    @Test
    void equipFailsWhenWeaponNotInInventory() {
        Hero hero = new Hero("TestHero");
        Weapon sword = new Weapon("Ghost Blade", "not owned", 5);
        GameSession session = new GameSession();
        session.setHero(hero);

        assertTrue(new EquipAction(sword).resolve(session).isEmpty());
        assertNull(hero.getEquippedWeapon());
        assertFalse(hero.getInventory().hasItem(sword));
        assertTrue(repOk(hero.getInventory()));
    }

    /** Resolves and applies EquipAction the same way as the game (resolve → effect.apply). */
    private static void applyEquipAction(Hero hero, Weapon weapon) {
        GameSession session = new GameSession();
        session.setHero(hero);
        for (Effect effect : new EquipAction(weapon).resolve(session)) {
            effect.apply(session);
        }
    }
}
