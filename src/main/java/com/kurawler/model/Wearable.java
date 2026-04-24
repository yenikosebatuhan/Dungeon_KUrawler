package com.kurawler.model;

import java.util.List;

/**
 * Represents a wearable item (e.g. armor) that modifies a Hero's stats.
 * Supported modifier types: "DEF", "STR".
 */
public class Wearable extends Item {

    /** Adds to the Hero's defence when equipped. */
    public static final String MOD_DEF = "DEF";
    /** Adds to the Hero's strength when equipped. */
    public static final String MOD_STR = "STR";

    private final int modifierValue;
    private final String modifierType;

    /**
     * Creates a new Wearable item.
     * @param name          The display name of the item
     * @param modifierValue The amount added to the stat when equipped
     * @param modifierType  Which stat is modified: "DEF" or "STR"
     * @throws IllegalArgumentException if modifierType is not one of the
     *         supported values
     */
    public Wearable(String name, int modifierValue, String modifierType) {
        super(name);
        if (modifierType == null
                || !(modifierType.equals(MOD_DEF) || modifierType.equals(MOD_STR))) {
            throw new IllegalArgumentException(
                    "modifierType must be DEF or STR (got: " + modifierType + ")");
        }
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
