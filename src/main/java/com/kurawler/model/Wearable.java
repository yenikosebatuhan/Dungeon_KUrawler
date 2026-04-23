package com.kurawler.model;

import java.util.List;

/**
 * Represents a wearable item (e.g. armor) that modifies a Hero's stats.
 * Supported modifier types: "DEF", "STR".
 */
public class Wearable extends Item {

    private int modifierValue;
    private String modifierType; // "DEF" or "STR"

    /**
     * Creates a new Wearable item.
     * @param name          The display name of the item
     * @param modifierValue The amount added to the stat when equipped
     * @param modifierType  Which stat is modified: "DEF" or "STR"
     */
    public Wearable(String name, int modifierValue, String modifierType) {
        super(name);
        this.modifierValue = modifierValue;
        this.modifierType = modifierType;
    }

    /**
     * Returns the modifier value applied when this wearable is equipped.
     * @return modifier value
     */
    public int getModifierValue() {
        return modifierValue;
    }

    /**
     * Returns which stat this wearable modifies.
     * @return "DEF" or "STR"
     */
    public String getModifierType() {
        return modifierType;
    }

    /**
     * Returns available actions for this wearable.
     * Includes base actions plus "WEAR".
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = super.getAvailableActions();
        actions.add("WEAR");
        return actions;
    }
}
