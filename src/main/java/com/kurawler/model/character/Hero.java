package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.Consumable;
import com.kurawler.model.Weapon;
import com.kurawler.model.Wearable;
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
        int weaponDmg = (equippedWeapon != null) ? equippedWeapon.getAtkValue() : 0;
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

        GridCell targetCell = map.getCell(targetRow, targetCol);
        if (targetCell == null || !targetCell.isWalkable()) {
            return false;
        }
        // Don't walk into an enemy
        if (session != null && session.getEnemyAt(targetRow, targetCol) != null) {
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
     * The previous wearable's stat bonus (if any) is removed first.
     * Currently only DEF-type wearables affect defense; STR-type
     * wearables affect strength.
     */
    public void equipWearable(Wearable wearable) {
        if (wearable == null) return;
        if (equippedWearable != null) {
            // Strip old bonus
            applyWearableModifier(equippedWearable, -1);
        }
        equippedWearable = wearable;
        applyWearableModifier(wearable, +1);
    }

    private void applyWearableModifier(Wearable wearable, int sign) {
        int delta = sign * wearable.getModifierValue();
        if (Wearable.MOD_DEF.equals(wearable.getModifierType())) {
            setDefensePower(getDefensePower() + delta);
        } else if (Wearable.MOD_STR.equals(wearable.getModifierType())) {
            setStrength(getStrength() + delta);
        }
    }

    /**
     * Consumes an item from the inventory: restores the relevant stat
     * (HP/MANA/ENERGY) by its restoreAmount and removes it from inventory.
     * No-op if the consumable isn't carried.
     */
    public boolean useConsumable(Consumable consumable) {
        if (consumable == null) return false;
        if (!inventory.removeItem(consumable)) return false;
        int amount = consumable.getRestoreAmount();
        switch (consumable.getStatType()) {
            case Consumable.STAT_HP:
                heal(amount);
                break;
            case Consumable.STAT_MANA:
                restoreMana(amount);
                break;
            case Consumable.STAT_ENERGY:
                restoreEnergy(amount);
                break;
            default:
                // Unknown stat type — safe no-op
                break;
        }
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
