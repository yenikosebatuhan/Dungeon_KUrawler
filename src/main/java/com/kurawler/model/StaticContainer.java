package com.kurawler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a static container placed on the map (e.g. chest).
 * Can be locked with a keyId; only a matching Key can unlock it.
 * Extends GameObject (not Item) since it cannot be picked up.
 */
public class StaticContainer extends GameObject {

    private int capacityLimit;
    private boolean isLocked;
    private String requiredKeyId;
    private List<Item> contents;

    /**
     * Creates a new unlocked StaticContainer.
     * @param name          The display name of the container
     * @param capacityLimit Maximum number of items this container can hold
     */
    public StaticContainer(String name, int capacityLimit) {
        super(name);
        this.capacityLimit = capacityLimit;
        this.contents = new ArrayList<>();
        this.isLocked = false;
        this.requiredKeyId = null;
    }

    /**
     * Attempts to unlock this container using the provided Key.
     * @param key The key to use
     * @return true if the key matched and the container was unlocked, false otherwise
     */
    public boolean unlock(Key key) {
        if (key == null) {
            return false;
        }
        if (isLocked && key.getKeyId().equals(requiredKeyId)) {
            isLocked = false;
            return true;
        }
        return false;
    }

    /**
     * Opens the container and returns its contents if it is not locked.
     * @return list of items inside, or empty list if locked
     */
    public List<Item> openContainer() {
        if (!isLocked) {
            return contents;
        }
        return new ArrayList<>();
    }

    /**
     * Adds an item to this container if there is space.
     * @param item The item to add
     */
    public void addItem(Item item) {
        if (item == null) {
            return;
        }
        if (contents.size() < capacityLimit) {
            contents.add(item);
        }
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
     * Returns whether this container is currently locked.
     * @return true if locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Sets the lock state and required key ID for this container.
     * @param locked true to lock the container
     * @param keyId  The key ID required to unlock it (only used when locked=true)
     */
    public void setLocked(boolean locked, String keyId) {
        this.isLocked = locked;
        this.requiredKeyId = keyId;
    }

    /**
     * Returns the key ID required to unlock this container.
     * @return required key ID, or null if not locked
     */
    public String getRequiredKeyId() {
        return requiredKeyId;
    }

    /**
     * Returns the maximum number of items this container can hold.
     * @return capacity limit
     */
    public int getCapacityLimit() {
        return capacityLimit;
    }

    /**
     * Returns available actions for this container.
     * "OPEN" is only available when not locked.
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        if (!isLocked) {
            actions.add("OPEN");
        } else {
            actions.add("UNLOCK");
        }
        return actions;
    }
}
