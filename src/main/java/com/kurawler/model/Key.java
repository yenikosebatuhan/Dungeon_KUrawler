package com.kurawler.model;

import java.util.List;

/**
 * Represents a key item used to unlock StaticContainers.
 * Each key has a unique keyId that must match the container's required key.
 */
public class Key extends Item {

    private final String keyId;

    /**
     * Creates a new Key item.
     * @param name  The display name of the key
     * @param keyId The unique identifier used to match with a locked container
     * @throws IllegalArgumentException if keyId is null or empty
     */
    public Key(String name, String keyId) {
        super(name);
        if (keyId == null || keyId.isEmpty()) {
            throw new IllegalArgumentException("keyId must be non-empty");
        }
        this.keyId = keyId;
    }

    /**
     * Returns the unique identifier of this key.
     * @return key ID string (never null)
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns available actions for this key.
     * Includes base actions plus "USE".
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = super.getAvailableActions();
        actions.add("USE");
        return actions;
    }
}
