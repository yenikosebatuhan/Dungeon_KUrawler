package com.kurawler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a portable container (e.g. pouch) that the Hero can carry in inventory.
 * Can hold a limited number of items inside it.
 * Extends Item so it can itself be stored in the Hero's Inventory.
 */
public class PortableContainer extends Item {

    private int capacityLimit;
    private List<Item> contents;

    /**
     * Creates a new PortableContainer.
     * @param name          The display name of the container
     * @param capacityLimit Maximum number of items this container can hold
     */
    public PortableContainer(String name, int capacityLimit) {
        super(name);
        this.capacityLimit = capacityLimit;
        this.contents = new ArrayList<>();
    }

    /**
     * Adds an item into this container if there is space.
     * @param item The item to add
     * @return true if successfully added, false if container is full or item is null
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (contents.size() >= capacityLimit) {
            return false;
        }
        contents.add(item);
        return true;
    }

    /**
     * Removes a specific item from this container.
     * @param item The item to remove
     * @return true if item was found and removed, false otherwise
     */
    public boolean removeItem(Item item) {
        return contents.remove(item);
    }

    /**
     * Returns the list of items currently inside this container.
     * @return list of contents
     */
    public List<Item> getContents() {
        return contents;
    }

    /**
     * Returns the maximum number of items this container can hold.
     * @return capacity limit
     */
    public int getCapacityLimit() {
        return capacityLimit;
    }

    /**
     * Returns whether this container is full.
     * @return true if at capacity
     */
    public boolean isFull() {
        return contents.size() >= capacityLimit;
    }

    /**
     * Returns available actions for this container.
     * Includes base actions plus "OPEN".
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = super.getAvailableActions();
        actions.add("OPEN");
        return actions;
    }
}
