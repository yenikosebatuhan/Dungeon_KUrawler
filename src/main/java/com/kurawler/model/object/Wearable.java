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

    public static final int MOD_DEF = 0;
    public static final int MOD_ATK = 1;

    public Wearable(String name, int bonus, int modType) {
        this(name, name, bonus);
    }

    public boolean isShield() {
        String n = getName();
        return n != null && n.toLowerCase().contains("shield");
    }

    public boolean isRing() {
        String n = getName();
        if (n == null) return false;
        String lower = n.toLowerCase();
        return lower.contains("ring") || lower.contains("amulet");
    }

    public boolean isBodyArmor() {
        return !isShield() && !isRing();
    }
}
