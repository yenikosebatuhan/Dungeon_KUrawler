package com.kurawler.model;

import com.kurawler.model.object.Item;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Group ADT test for Inventory.
 * Verifies the Rep Invariant, Abstraction Function, and core behaviour.
 */
class InventoryTest {

    private Inventory inv;
    private Weapon sword;
    private Weapon bow;

    @BeforeEach
    void setUp() {
        inv = new Inventory();
        sword = new Weapon("Sword", "A blade", 5);
        bow   = new Weapon("Bow",   "A ranged weapon", 3);
    }

    // --- Rep Invariant ---

    @Test
    void repOkOnFreshInventory() {
        assertTrue(inv.repOk());
    }

    @Test
    void repOkAfterAddAndRemove() {
        inv.addItem(sword);
        assertTrue(inv.repOk());
        inv.removeItem(sword);
        assertTrue(inv.repOk());
    }

    @Test
    void repOkOnFullInventory() {
        for (int i = 0; i < 8; i++) {
            inv.addItem(new Weapon("W" + i, "", i));
        }
        assertTrue(inv.repOk());
    }

    // --- addItem ---

    @Test
    void addItemIncreasesCount() {
        inv.addItem(sword);
        assertEquals(1, inv.getItemCount());
    }

    @Test
    void addItemReturnsTrueWhenSpaceAvailable() {
        assertTrue(inv.addItem(sword));
    }

    @Test
    void addItemReturnsFalseWhenFull() {
        for (int i = 0; i < 8; i++) {
            inv.addItem(new Weapon("W" + i, "", i));
        }
        assertFalse(inv.addItem(bow));
    }

    @Test
    void addedItemIsRetrievable() {
        inv.addItem(sword);
        assertTrue(inv.hasItem(sword));
    }

    // --- removeItem ---

    @Test
    void removeItemDecreasesCount() {
        inv.addItem(sword);
        inv.removeItem(sword);
        assertEquals(0, inv.getItemCount());
    }

    @Test
    void removeItemReturnsFalseForAbsentItem() {
        assertFalse(inv.removeItem(sword));
    }

    @Test
    void removedItemNoLongerPresent() {
        inv.addItem(sword);
        inv.removeItem(sword);
        assertFalse(inv.hasItem(sword));
    }

    // --- isFull / capacity ---

    @Test
    void freshInventoryIsNotFull() {
        assertFalse(inv.isFull());
    }

    @Test
    void inventoryIsFullAfterEightItems() {
        for (int i = 0; i < 8; i++) {
            inv.addItem(new Weapon("W" + i, "", i));
        }
        assertTrue(inv.isFull());
    }

    @Test
    void itemCountNeverExceedsEight() {
        for (int i = 0; i < 10; i++) {
            inv.addItem(new Weapon("W" + i, "", i));
        }
        assertTrue(inv.getItemCount() <= 8);
    }
}
