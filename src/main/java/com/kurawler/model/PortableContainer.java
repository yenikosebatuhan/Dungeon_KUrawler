package com.kurawler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a portable container (e.g. pouch) that the Hero can carry
 * in inventory. Can hold a limited number of items inside it.
 * Extends Item so it can itself be stored in the Hero's Inventory.
 */
public class PortableContainer extends Item {

    private final int capacityLimit;
    private final List<Item> contents;

    /**
     * Creates a new PortableContainer.
     * @param name          The display name of the container
     * @param capacityLimit Maximum number of items this container can hold
     */
    public PortableContainer(String name, int capacityLimit) {
        super(name);
        this.capacityLimit = Math.max(0, capacityLimit);
        this.contents = new ArrayList<>();
    }

    /**
     * Adds an item into this container if there is space.
     * A container cannot contain itself (prevents infinite recursion).
     *
     * @param item The item to add
     * @return true if successfully added, false if the container is full,
     *         the item is null, the item is this container itself, or
     *         the item is already present
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (item == this) {
            return false;
        }
        if (contents.size() >= capacityLimit) {
            return false;
        }
        if (contents.contains(item)) {
            return false;
        }
        return contents.add(item);
    }

    /**
     * Returns an unmodifiable view of the items currently inside
     * this container.
     * @return unmodifiable list of contents
     */
    public List<Item> getContents() {
        return Collections.unmodifiableList(contents);
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
