package com.kurawler.model.object;

public class Wearable extends Item {

    private int defenseBonus;

    public Wearable(String name, String description, int defenseBonus) {
        super(name, description);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    public static final int MOD_DEF = 0;
    public static final int MOD_ATK = 1;

    public Wearable(String name, int bonus, int modType) {
        this(name, name, bonus);
    }
}
