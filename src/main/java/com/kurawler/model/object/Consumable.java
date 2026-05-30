package com.kurawler.model.object;

public class Consumable extends Item {

    private int healAmount;
    private int manaAmount;
    private int energyAmount;

    public Consumable(String name, String description, int healAmount) {
        super(name, description);
        this.healAmount = healAmount;
        this.manaAmount = 0;
        this.energyAmount = 0;
    }

    public Consumable(String name, String description, int healAmount, int manaAmount, int energyAmount) {
        super(name, description);
        this.healAmount = healAmount;
        this.manaAmount = manaAmount;
        this.energyAmount = energyAmount;
    }

    public static final int STAT_HP = 0;
    public static final int STAT_MP = 1;
    public static final int STAT_EN = 2;

    public Consumable(String name, int amount, int statType) {
        this(name, name, amount);
    }

    public int getHealAmount() { return healAmount; }
    public int getManaAmount() { return manaAmount; }
    public int getEnergyAmount() { return energyAmount; }
}
