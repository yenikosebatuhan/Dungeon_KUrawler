package com.kurawler.model;

import java.util.List;

/**
 * Represents a consumable item that restores a Hero's stat.
 * Supported stat types: "HP", "ENERGY", "MANA".
 */
public class Consumable extends Item {

    /** Restores hit points; triggers the "EAT" action. */
    public static final String STAT_HP = "HP";
    /** Restores energy; triggers the "EAT" action. */
    public static final String STAT_ENERGY = "ENERGY";
    /** Restores mana; triggers the "DRINK" action. */
    public static final String STAT_MANA = "MANA";

    private final int restoreAmount;
    private final String statType;

    /**
     * Creates a new Consumable item.
     * @param name          The display name of the item
     * @param restoreAmount How much of the stat to restore when consumed
     *                      (non-negative)
     * @param statType      Which stat is restored: "HP", "ENERGY", or "MANA"
     * @throws IllegalArgumentException if statType is not one of the
     *         supported values
     */
    public Consumable(String name, int restoreAmount, String statType) {
        super(name);
        if (statType == null
                || !(statType.equals(STAT_HP)
                        || statType.equals(STAT_ENERGY)
                        || statType.equals(STAT_MANA))) {
            throw new IllegalArgumentException(
                    "statType must be one of HP, ENERGY, MANA (got: " + statType + ")");
        }
        this.restoreAmount = Math.max(0, restoreAmount);
        this.statType = statType;
    }

    /**
     * Returns the amount this consumable restores.
     * @return restore amount (>= 0)
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
        if (STAT_HP.equals(statType) || STAT_ENERGY.equals(statType)) {
            actions.add("EAT");
        } else if (STAT_MANA.equals(statType)) {
            actions.add("DRINK");
        }
        return actions;
    }
}
