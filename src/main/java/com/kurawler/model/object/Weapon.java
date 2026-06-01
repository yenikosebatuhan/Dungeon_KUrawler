package com.kurawler.model.object;

import java.util.List;

public class Weapon extends Item {

    private int damage;

    public Weapon(String name, String description, int damage) {
        super(name, description);
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.EQUIP);
    }
}
