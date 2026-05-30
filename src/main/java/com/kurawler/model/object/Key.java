package com.kurawler.model.object;

public class Key extends Item {

    private String keyId;

    public Key(String name, String description, String keyId) {
        super(name, description);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }

    public Key(String name, String keyId) {
        this(name, name, keyId);
    }
}
