package com.kurawler.model.object;

import java.util.List;

public class Wearable extends Item {

    /** Default body-armor DEF hint for editor palette (actual loot uses {@link com.kurawler.model.WearableCatalog}). */
    public static final int ARMOR_DEFENSE_BONUS = 5;

    private int defenseBonus;

    public Wearable(String name, String description, int defenseBonus) {
        super(name, description);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    /** Shield occupies its own equip slot. */
    public boolean isShield() {
        String n = getName();
        return n != null && n.toLowerCase().contains("shield");
    }

    /** Ring / amulet slot — separate from body armor. */
    public boolean isRing() {
        String n = getName();
        if (n == null) {
            return false;
        }
        String lower = n.toLowerCase();
        return lower.contains("ring") || lower.contains("amulet");
    }

    /** Body armor (not shield, not ring). */
    public boolean isBodyArmor() {
        return !isShield() && !isRing();
    }

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.WEAR);
    }
}
