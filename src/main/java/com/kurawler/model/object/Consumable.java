package com.kurawler.model.object;

public class Consumable extends Item {

    private int healAmount;

    public Consumable(String name, String description, int healAmount) {
        super(name, description);
        this.healAmount = healAmount;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public static final int STAT_HP = 0;
    public static final int STAT_MP = 1;
    public static final int STAT_EN = 2;

    public Consumable(String name, int amount, int statType) {
        this(name, name, amount);
    }
}
