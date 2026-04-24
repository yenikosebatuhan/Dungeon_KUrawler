package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.Random;

public class Hero extends Character {
    private final Inventory inventory;
    private Weapon equippedWeapon;
    private Wearable equippedWearable;

    private static final int ATTACK_ENERGY_COST = 1;
    private static final int MELEE_REACH = 1;

    public Hero(String name) {
        super(name, 17, 0, 2, 8 + new Random().nextInt(8), 80, 100);
        this.inventory = new Inventory();
    }

    @Override
    public int calculateDamage() {
        int weaponDmg = (equippedWeapon != null) ? equippedWeapon.getDamage() : 0;
        return getStrength() + weaponDmg;
    }

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
        // Caner's perfect collision detection!
        if (targetCell == null || targetCell.isBlocked()) return false;

        // Clear old cell, claim new cell
        if (getCurrentCell() != null) getCurrentCell().setOccupyingCharacter(null);
        targetCell.setOccupyingCharacter(this);
        setCurrentCell(targetCell);
        setPosition(targetRow, targetCol);
        return true;
    }

    public boolean attack(Enemy target) {
        if (target == null || !target.isAlive()) return false;
        if (getEnergy() < ATTACK_ENERGY_COST) return false;

        int dRow = Math.abs(target.getRow() - getRow());
        int dCol = Math.abs(target.getCol() - getCol());
        if (dRow + dCol > MELEE_REACH) return false;

        useEnergy(ATTACK_ENERGY_COST);
        target.takeDamage(calculateDamage());
        return true;
    }

    public void equipWearable(Wearable wearable) {
        if (wearable == null) return;
        if (equippedWearable != null) setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        equippedWearable = wearable;
        setDefensePower(getDefensePower() + wearable.getDefenseBonus());
    }

    public boolean useConsumable(Consumable consumable) {
        if (consumable == null || !inventory.removeItem(consumable)) return false;
        heal(consumable.getHealAmount());
        return true;
    }

    public Inventory getInventory() { return inventory; }
    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(Weapon weapon) { this.equippedWeapon = weapon; }
    public Wearable getEquippedWearable() { return equippedWearable; }
}