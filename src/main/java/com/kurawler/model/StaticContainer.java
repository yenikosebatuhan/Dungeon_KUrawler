package com.kurawler.model;

import com.kurawler.model.object.GameObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a static container placed on the map (e.g. chest).
 * Can be locked with a keyId; only a matching Key can unlock it.
 * Extends GameObject (not Item) since it cannot be picked up.
 */
public class StaticContainer extends GameObject {

    private final int capacityLimit;
    private final List<Item> contents;
    private boolean isLocked;
    private String requiredKeyId;

    /**
     * Creates a new unlocked StaticContainer.
     * @param name          The display name of the container
     * @param capacityLimit Maximum number of items this container can hold
     */
    public StaticContainer(String name, int capacityLimit) {
        super(name, "");
        this.capacityLimit = Math.max(0, capacityLimit);
        this.contents = new ArrayList<>();
        this.isLocked = false;
        this.requiredKeyId = null;
    }

    /**
     * Attempts to unlock this container using the provided Key.
     * @param key The key to use
     * @return true if the key matched and the container was unlocked,
     *         false if the key is null, the container is already
     *         unlocked, or the key does not match
     */
    public boolean unlock(Key key) {
        if (!isLocked) {
            return false;
        }
        if (key == null || key.getKeyId() == null || requiredKeyId == null) {
            return false;
        }
        if (requiredKeyId.equals(key.getKeyId())) {
            isLocked = false;
            return true;
        }
        return false;
    }

    /**
     * Opens the container and returns its contents.
     * @return unmodifiable view of contents
     * @throws IllegalStateException if the container is locked
     */
    public List<Item> openContainer() {
        if (isLocked) {
            throw new IllegalStateException(
                    "Container '" + getName() + "' is locked and cannot be opened");
        }
        return Collections.unmodifiableList(contents);
    }

    /**
     * Adds an item to this container if there is space.
     * @param item The item to add
     * @return true if successfully added, false if container is full
     *         or the item is null
     */
    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        if (contents.size() >= capacityLimit) {
            return false;
        }
        return contents.add(item);
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
     * When {@code locked} is false, the required key ID is cleared.
     * @param locked true to lock the container
     * @param keyId  The key ID required to unlock it; required when
     *               locked=true, ignored when locked=false
     */
    public void setLocked(boolean locked, String keyId) {
        if (locked) {
            if (keyId == null || keyId.isEmpty()) {
                throw new IllegalArgumentException(
                        "A non-empty keyId is required when locking a container");
            }
            this.requiredKeyId = keyId;
            this.isLocked = true;
        } else {
            this.isLocked = false;
            this.requiredKeyId = null;
        }
    }

    /**
     * Returns contextual actions for this container:
     * "UNLOCK" if locked, "OPEN" if unlocked.
     * @return list of action strings
     */
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        if (isLocked) {
            actions.add("UNLOCK");
        } else {
            actions.add("OPEN");
        }
        return actions;
    }
}
