package com.kurawler.model;

import java.util.List;

/**
 * Represents a consumable item that restores a Hero's stat.
 * Supported stat types: "HP", "ENERGY", "MANA".
 */
public class Consumable extends Item {

    private int restoreAmount;
    private String statType; // "HP", "ENERGY", or "MANA"

    /**
     * Creates a new Consumable item.
     * @param name          The display name of the item
     * @param restoreAmount How much of the stat to restore when consumed
     * @param statType      Which stat is restored: "HP", "ENERGY", or "MANA"
     */
    public Consumable(String name, int restoreAmount, String statType) {
        super(name);
        this.restoreAmount = restoreAmount;
        this.statType = statType;
    }

    /**
     * Returns the amount this consumable restores.
     * @return restore amount
     */
    public int getRestoreAmount() {
        return restoreAmount;
    }

    /**
     * Returns which stat this consumable restores.
     * @return "HP", "ENERGY", or "MANA"
     */
    public String getStatType() {
        return statType;
    }

    /**
     * Returns available actions for this consumable.
     * HP/ENERGY items get "EAT"; MANA items get "DRINK".
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = super.getAvailableActions();
        if (statType.equals("HP") || statType.equals("ENERGY")) {
            actions.add("EAT");
        } else if (statType.equals("MANA")) {
            actions.add("DRINK");
        }
        return actions;
    }
}
