package com.kurawler.model.object;

import java.util.List;

public class Key extends Item {

    private String keyId;

    public Key(String name, String description, String keyId) {
        super(name, description);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE);
    }
}
