package com.kurawler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Hero's item inventory with a maximum capacity of 8 slots.
 */
public class Inventory {

    private static final int MAX_CAPACITY = 8;
    private List<Item> items;
    private int itemCount;

    /**
     * Creates a new empty Inventory.
     */
    public Inventory() {
        this.items = new ArrayList<>(MAX_CAPACITY);
        this.itemCount = 0;
    }

    /**
     * Adds an item to the inventory if there is space.
     * @param item The item to add
     * @return true if successfully added, false if inventory is full or item is null
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (itemCount >= MAX_CAPACITY) {
            return false;
        }
        items.add(item);
        itemCount++;
        return true;
    }

    /**
     * Removes a specific item from the inventory.
     * @param item The item to remove
     * @return true if item was found and removed, false otherwise
     */
    public boolean removeItem(Item item) {
        if (item == null) {
            return false;
        }
        if (items.remove(item)) {
            itemCount--;
            return true;
        }
        return false;
    }

    /**
     * Returns the list of all items in the inventory.
     * @return list of items
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Returns the current number of items in inventory.
     * @return item count
     */
    public int getItemCount() {
        return itemCount;
    }

    /**
     * Returns whether the inventory is at maximum capacity.
     * @return true if full
     */
    public boolean isFull() {
        return itemCount >= MAX_CAPACITY;
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
        return items.contains(item);
    }

    /**
     * Returns the maximum capacity of this inventory.
     * @return max capacity (8)
     */
    public int getMaxCapacity() {
        return MAX_CAPACITY;
    }
}
