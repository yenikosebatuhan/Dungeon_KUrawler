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
}
