package com.kurawler.model.object;

public class Wearable extends Item {

    public static final int ARMOR_DEFENSE_BONUS = 5;

    private int defenseBonus;

    public Wearable(String name, String description, int defenseBonus) {
        super(name, description);
        if (name != null && name.equalsIgnoreCase("Armor")) {
            this.defenseBonus = ARMOR_DEFENSE_BONUS;
        } else {
            this.defenseBonus = defenseBonus;
        }
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
}
