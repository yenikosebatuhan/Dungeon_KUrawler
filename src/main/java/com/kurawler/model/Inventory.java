package com.kurawler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the Hero's item inventory, laid out as a fixed 2x4 grid
 * (2 rows × 4 columns = 8 slots).
 *
 * <p>Information Expert: Inventory knows which slot each item occupies
 * and enforces the capacity constraint. Slots have stable positions —
 * removing an item leaves its slot empty; it does NOT compact the grid.</p>
 *
 * <p>Demo milestone: "Move one item from the ground into the 2x4
 * inventory via a TAKE action, showing it occupies a slot and
 * disappears from the map."</p>
 */
public class Inventory {

    /** Number of rows in the inventory grid. */
    public static final int ROWS = 2;
    /** Number of columns in the inventory grid. */
    public static final int COLS = 4;
    /** Maximum number of items (ROWS * COLS = 8). */
    public static final int MAX_CAPACITY = ROWS * COLS;

    private final Item[][] slots;

    /**
     * Creates a new empty 2x4 Inventory.
     */
    public Inventory() {
        this.slots = new Item[ROWS][COLS];
    }

    /**
     * Adds an item to the first empty slot (scanning row-major, left to right).
     * @param item The item to add
     * @return true if successfully added, false if the inventory is full,
     *         the item is null, or the item is already present
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (hasItem(item)) {
            return false;
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == null) {
                    slots[r][c] = item;
                    return true;
                }
            }
        }
        return false; // full
    }

    /**
     * Removes a specific item from the inventory. The freed slot
     * becomes null — the grid is NOT compacted.
     * @param item The item to remove
     * @return true if item was found and removed, false otherwise
     */
    public boolean removeItem(Item item) {
        if (item == null) {
            return false;
        }
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

    /**
     * Returns the item at a specific slot, or null if the slot is empty
     * or out of range. Used by the UI to render the 2x4 grid.
     * @param row row index [0, ROWS)
     * @param col column index [0, COLS)
     * @return the item at (row, col), or null
     */
    public Item getItem(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return slots[row][col];
    }

    /**
     * Returns an unmodifiable, compacted list of items currently held
     * (empty slots are skipped). Order is row-major.
     * @return unmodifiable list of non-null items
     */
    public List<Item> getItems() {
        List<Item> result = new ArrayList<>(MAX_CAPACITY);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] != null) {
                    result.add(slots[r][c]);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the current number of items in the inventory
     * (counts non-null slots).
     * @return item count [0, MAX_CAPACITY]
     */
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

    /**
     * Returns whether every slot is occupied.
     * @return true if full
     */
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

    /**
     * Checks whether the inventory contains a specific item.
     * @param item The item to search for
     * @return true if item is in inventory
     */
    public boolean hasItem(Item item) {
        if (item == null) {
            return false;
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (slots[r][c] == item) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Human-readable 2x4 grid representation, e.g.:
     * <pre>
     * Inventory (3/8):
     *   [Sword | Potion | empty | empty ]
     *   [Key   | empty  | empty | empty ]
     * </pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Inventory (")
                .append(getItemCount()).append("/").append(MAX_CAPACITY).append("):\n");
        for (int r = 0; r < ROWS; r++) {
            sb.append("  [");
            for (int c = 0; c < COLS; c++) {
                if (c > 0) {
                    sb.append(" | ");
                }
                sb.append(slots[r][c] != null ? slots[r][c].getName() : "empty");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
}
