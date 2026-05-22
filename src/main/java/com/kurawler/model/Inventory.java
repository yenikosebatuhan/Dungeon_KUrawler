package com.kurawler.model;

import com.kurawler.model.object.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Overview: An Inventory is a bounded 2×4 grid that holds up to 8 items for a
 * hero. Items occupy individual slots; no two items share a slot.
 *
 * Abstraction Function:
 *   AF(inv) = the set { slots[r][c] | slots[r][c] != null, 0<=r<2, 0<=c<4 }
 *   representing the collection of items currently carried by the hero.
 *
 * Representation Invariant:
 *   slots != null
 *   slots.length == 2 && slots[i].length == 4 for all i
 *   each non-null slot holds a distinct Item reference (no duplicates)
 */
public class Inventory {
    private static final int ROWS = 2;
    private static final int COLS = 4;
    private final Item[][] slots;

    public Inventory() {
        this.slots = new Item[ROWS][COLS];
    }

    /**
     * @requires item != null
     * @modifies this (the inventory's slot array)
     * @effects  If the inventory has at least one empty slot, places item in the
     *           first empty slot (row-major order) and returns true. Otherwise
     *           the inventory is left unchanged and returns false.
     */
    public boolean addItem(Item item) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == null) {
                    slots[r][c] = item;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @requires item != null
     * @modifies this
     * @effects  If item currently occupies a slot, empties that slot and returns
     *           true; otherwise the inventory is left unchanged and returns false.
     */
    public boolean removeItem(Item item) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == item) {
                    slots[r][c] = null;
                    return true;
                }
            }
        }
        return false;
    }

    public Item getItem(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null;
        return slots[row][col];
    }

    /**
     * @requires true (no precondition)
     * @modifies nothing (pure query)
     * @effects  Returns true iff all ROWS*COLS (8) slots are occupied.
     */
    public boolean isFull() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == null) return false;
            }
        }
        return true;
    }

    public int getItemCount() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) count++;
            }
        }
        return count;
    }
    
    // --- ADAPTERS FOR CANER ---
    public boolean hasItem(Item item) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == item) return true;
            }
        }
        return false;
    }
    
    public List<Item> getItems() {
        List<Item> list = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) list.add(slots[r][c]);
            }
        }
        return list;
    }

    @Override
    public String toString() {
        // Efekan's original toString kept same
        return "Inventory (" + getItemCount() + "/" + (ROWS * COLS) + ")";
    }
}