package com.kurawler.model.object;

import java.util.List;

public class Consumable extends Item {

    /** Standard red potion heal (hero max HP is typically ~17). */
    public static final int HEALTH_POTION_HEAL = 10;
    /** Standard energy potion / apple restore. */
    public static final int ENERGY_POTION_ENERGY = 40;

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

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.EAT, ItemAction.BREAK);
    }
}
