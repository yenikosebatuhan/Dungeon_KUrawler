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

    /** Inventory/detail text: base description plus {@code +ATK} when the weapon has damage. */
    public String getInventoryDescription() {
        String base = getDescription();
        if (damage <= 0) {
            return base != null ? base : "";
        }
        if (base != null && base.contains("+ATK")) {
            return base;
        }
        if (base != null && base.matches("(?s).*\\bATK \\d+\\b.*")) {
            return base.replaceAll("\\bATK (\\d+)\\b", "+ATK $1");
        }
        if (base == null || base.isBlank()) {
            return "+ATK " + damage;
        }
        return base.trim() + "\n+ATK " + damage;
    }

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.EQUIP);
    }
}
