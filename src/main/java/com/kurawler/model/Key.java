package com.kurawler.model;

import java.util.List;

/**
 * Represents a key item used to unlock StaticContainers.
 * Each key has a unique keyId that must match the container's required key.
 */
public class Key extends Item {

    private String keyId;

    /**
     * Creates a new Key item.
     * @param name  The display name of the key
     * @param keyId The unique identifier used to match with a locked container
     */
    public Key(String name, String keyId) {
        super(name);
        this.keyId = keyId;
    }

    /**
     * Returns the unique identifier of this key.
     * @return key ID string
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
