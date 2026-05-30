package com.kurawler.model.character;

import com.kurawler.model.GridCell;

public abstract class Character {
    private String name;
    private int health;
    private int maxHealth;
    private int attackPower;
    private int defensePower;
    private int strength;
    private int mana;
    private int maxMana;
    private int energy;
    private int maxEnergy;
    private int row;
    private int col;
    
    // Link to Map system
    private GridCell currentCell;
    private int rotation; // clockwise degrees, normalised to 0/90/180/270

    public int getRotation() { return rotation; }
    public void setRotation(int degrees) { this.rotation = ((degrees % 360) + 360) % 360; }

    protected Character(String name, int maxHealth, int attackPower, int defensePower,
                        int strength, int maxMana, int maxEnergy) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.attackPower = attackPower;
        this.defensePower = defensePower;
        this.strength = strength;
        this.maxMana = maxMana;
        this.mana = maxMana;
        this.maxEnergy = maxEnergy;
        this.energy = maxEnergy;
    }

    public int calculateDamage() { return attackPower + strength; }
 // --- BUG-07 FIX: Removed defense subtraction here, handled in CombatResolver ---
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }
    public boolean isAlive() { return health > 0; }
    public void heal(int amount) { this.health = Math.min(maxHealth, this.health + amount); }
    public boolean useMana(int amount) {
        if (mana < amount) return false;
        this.mana -= amount;
        return true;
    }
    public void restoreMana(int amount) { this.mana = Math.min(maxMana, this.mana + amount); }
    public boolean useEnergy(int amount) {
        if (energy < amount) return false;
        this.energy -= amount;
        return true;
    }
    public void restoreEnergy(int amount) { this.energy = Math.min(maxEnergy, this.energy + amount); }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getAttackPower() { return attackPower; }
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }
    public int getDefensePower() { return defensePower; }
    public void setDefensePower(int defensePower) { this.defensePower = defensePower; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(maxMana, mana)); }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(maxEnergy, energy)); }
    public void setHealth(int health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    public int getRow() { return row; }
    public int getCol() { return col; }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Atomically moves this character to a new cell, keeping
     * (row, col), currentCell, and the cell's occupant in sync.
     * Pass null to remove the character from the map.
     */
    public void moveToCell(GridCell newCell) {
        if (currentCell != null) {
            currentCell.setOccupyingCharacter(null);
        }
        this.currentCell = newCell;
        if (newCell != null) {
            newCell.setOccupyingCharacter(this);
            this.row = newCell.getRow();
            this.col = newCell.getCol();
        }
    }
    
    public GridCell getCurrentCell() { return currentCell; }
    public void setCurrentCell(GridCell cell) { this.currentCell = cell; }
    
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }
}