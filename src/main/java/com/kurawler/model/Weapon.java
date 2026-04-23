package com.kurawler.model;

import java.util.List;

/**
 * Represents a weapon item that the Hero can equip.
 * Can be melee or ranged. Ranged weapons can fire Projectiles.
 */
public class Weapon extends Item {

    private int atkValue;
    private boolean isRanged;

    /**
     * Creates a new Weapon.
     * @param name     The display name of the weapon
     * @param atkValue The attack value added to damage calculations
     * @param isRanged true if this weapon can fire projectiles
     */
    public Weapon(String name, int atkValue, boolean isRanged) {
        super(name);
        this.atkValue = atkValue;
        this.isRanged = isRanged;
    }

    /**
     * Returns the attack value of this weapon.
     * @return attack value
     */
    public int getAtkValue() {
        return atkValue;
    }

    /**
     * Returns whether this weapon is ranged.
     * @return true if ranged
     */
    public boolean isRanged() {
        return isRanged;
    }

    /**
     * Fires a projectile from the given cell in the given direction.
     * Only works if the weapon is ranged.
     * @param sourceCell The cell from which the projectile is fired
     * @param direction  Direction to fire ("UP", "DOWN", "LEFT", "RIGHT")
     * @return a new Projectile, or null if weapon is not ranged
     */
    public Projectile fireProjectile(GridCell sourceCell, String direction) {
        if (!isRanged) {
            return null;
        }
        Projectile projectile = new Projectile(atkValue, direction, 2);
        projectile.setCurrentCell(sourceCell);
        return projectile;
    }

    /**
     * Returns available actions for this weapon.
     * Includes base actions plus "EQUIP".
     * @return list of action strings
     */
    @Override
    public List<String> getAvailableActions() {
        List<String> actions = super.getAvailableActions();
        actions.add("EQUIP");
        return actions;
    }
}
