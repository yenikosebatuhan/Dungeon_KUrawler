package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.Random;

/**
 * The player-controlled hero.
 * Creator: Hero creates its own Inventory.
 * Stats per spec: HP=17, Mana=80, STR=random(8-15), DEF=2, Energy=100.
 */
public class Hero extends Character {

    private final Inventory inventory;
    private Weapon equippedWeapon;
    private Wearable equippedWearable;

    /** Energy cost for one melee swing. */
    private static final int ATTACK_ENERGY_COST = 1;

    /** Manhattan reach for a melee attack (orthogonal neighbours only). */
    private static final int MELEE_REACH = 1;

    public Hero(String name) {
        super(name,
              17,                           // maxHealth (spec)
              0,                            // attackPower (base, weapon adds)
              2,                            // defensePower (spec)
              8 + new Random().nextInt(8),  // strength: random 8-15
              80,                           // maxMana (spec)
              100);                         // maxEnergy
        this.inventory = new Inventory();
    }

    @Override
    public int calculateDamage() {
        int weaponDmg = (equippedWeapon != null) ? equippedWeapon.getDamage() : 0;
        return getStrength() + weaponDmg;
    }

    /**
     * Moves the hero one cell in the given direction.
     * Validates: in-bounds, walkable, not occupied by another live character.
     *
     * @param direction one of "UP","DOWN","LEFT","RIGHT" (case-insensitive)
     * @param map       the active map
     * @param session   used to detect enemy occupancy at the target cell
     * @return true if the hero actually moved
     */
    public boolean move(String direction, Map map, GameSession session) {
        if (direction == null || map == null) return false;

        int dRow = 0, dCol = 0;
        switch (direction.toUpperCase()) {
            case "UP":    dRow = -1; break;
            case "DOWN":  dRow =  1; break;
            case "LEFT":  dCol = -1; break;
            case "RIGHT": dCol =  1; break;
            default: return false;
        }

        int targetRow = getRow() + dRow;
        int targetCol = getCol() + dCol;

        // Use Caner's API: getCellAt(x, y) where x=col, y=row
        GridCell targetCell = map.getCellAt(targetCol, targetRow);
        
        // Caner's isBlocked() automatically handles BOTH walls AND occupying characters/enemies!
        if (targetCell == null || targetCell.isBlocked()) {
            return false;
        }

        setPosition(targetRow, targetCol);
        return true;
    }

    /**
     * Performs a melee attack on the given enemy if it's within reach
     * and the hero has enough energy. Damage uses calculateDamage()
     * (which already factors in the equipped weapon).
     *
     * @return true if the attack was actually performed
     */
    public boolean attack(Enemy target) {
        if (target == null || !target.isAlive()) return false;
        if (getEnergy() < ATTACK_ENERGY_COST)   return false;

        int dRow = Math.abs(target.getRow() - getRow());
        int dCol = Math.abs(target.getCol() - getCol());
        // Orthogonal adjacency (Manhattan distance == 1)
        if (dRow + dCol > MELEE_REACH) return false;

        useEnergy(ATTACK_ENERGY_COST);
        target.takeDamage(calculateDamage());
        return true;
    }

    /**
     * Equips a wearable, replacing any previous one.
     * The previous wearable's defense bonus (if any) is removed first.
     */
    public void equipWearable(Wearable wearable) {
        if (wearable == null) return;
        if (equippedWearable != null) {
            // Strip old bonus
            setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        }
        equippedWearable = wearable;
        setDefensePower(getDefensePower() + wearable.getDefenseBonus());
    }

    /**
     * Consumes an item from the inventory: heals the hero by its healAmount
     * and removes it from inventory. No-op if the consumable isn't carried.
     */
    public boolean useConsumable(Consumable consumable) {
        if (consumable == null) return false;
        if (!inventory.removeItem(consumable)) return false;
        heal(consumable.getHealAmount());
        return true;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }

    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    public Wearable getEquippedWearable() {
        return equippedWearable;
    }
}
