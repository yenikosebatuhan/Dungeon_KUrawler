package com.kurawler.model;

import com.kurawler.model.object.Projectile;
import java.util.List;

/**
 * Represents a weapon item that the Hero can equip.
 * Can be melee or ranged. Ranged weapons can fire Projectiles.
 */
public class Weapon extends Item {

    private static final int DEFAULT_PROJECTILE_SPEED = 2;

    private final int atkValue;
    private final boolean isRanged;

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
     * Only works if the weapon is ranged and a source cell is provided.
     *
     * @param sourceCell The cell from which the projectile is fired
     * @param direction  Direction to fire ("UP", "DOWN", "LEFT", "RIGHT")
     * @return a new Projectile positioned at the source cell, or null if
     *         the weapon is not ranged, the source cell is null, or the
     *         direction is invalid
     */
    public Projectile fireProjectile(GridCell sourceCell, String direction) {
        if (!isRanged || sourceCell == null || direction == null) {
            return null;
        }
        Projectile projectile = new Projectile(atkValue, direction, DEFAULT_PROJECTILE_SPEED);
        projectile.setPosition(sourceCell.getRow(), sourceCell.getCol());
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
