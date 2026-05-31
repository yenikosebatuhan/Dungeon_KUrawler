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

    public int getHealAmount() { return healAmount; }
    public int getManaAmount() { return manaAmount; }
    public int getEnergyAmount() { return energyAmount; }
}
