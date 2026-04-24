package com.kurawler.model;

import com.kurawler.model.object.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * 2x4 inventory grid.
 * Information Expert: Inventory manages items (knows capacity, contents).
 */
public class Inventory {

    private static final int ROWS = 2;
    private static final int COLS = 4;
    private final Item[][] slots;
    
    public Inventory() {
        this.slots = new Item[ROWS][COLS];
    }
    
    public boolean addItem(Item item) {
        if (item == null) return false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == null) {
                    slots[r][c] = item;
                    return true; // Item successfully placed in grid
                }
            }
        }
        return false; // inventory full
    }

    // Hero class needs this to consume potions
    public boolean removeItem(Item item) {
        if (item == null) return false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == item) {
                    slots[r][c] = null;
                    return true; // Item successfully removed
                }
            }
        }
        return false;
    }

    public Item getItem(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return slots[row][col];
    }

    public boolean isFull() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getItemCount() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) {
                    count++;
                }
            }
        }
        return count;
    }
    
    // --- ADAPTERS FOR CANER'S GAME SESSION LOGIC ---

    // GameSession checkWinCondition() needs this
    public boolean hasItem(Item item) {
        if (item == null) return false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == item) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Converts the 2D array back into a flat List so Caner's old tests don't break
    public List<Item> getItems() {
        List<Item> flattenedList = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) {
                    flattenedList.add(slots[r][c]);
                }
            }
        }
        return flattenedList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Inventory (" + getItemCount() + "/" + (ROWS * COLS) + "):\n");
        for (int r = 0; r < ROWS; r++) {
            sb.append("  [");
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) {
                    sb.append(slots[r][c].getName());
                } else {
                    sb.append("empty");
                }
                if (c < COLS - 1) sb.append(" | ");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
}