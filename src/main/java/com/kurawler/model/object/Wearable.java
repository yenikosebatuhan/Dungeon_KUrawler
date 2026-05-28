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
}
