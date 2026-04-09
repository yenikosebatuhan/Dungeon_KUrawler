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
}
