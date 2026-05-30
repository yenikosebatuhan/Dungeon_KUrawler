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

    /**
     * REQUIRES: true
     * MODIFIES: nothing
     * EFFECTS:  Returns attackPower + strength. Does not depend on equipped items.
     */
    public int calculateDamage() { return attackPower + strength; }
 // --- BUG-07 FIX: Removed defense subtraction here, handled in CombatResolver ---
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.health
     * EFFECTS:  Sets health = max(0, health - amount). Health never becomes negative.
     */
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }
    /**
     * REQUIRES: true
     * MODIFIES: nothing
     * EFFECTS:  Returns true iff health > 0.
     */
    public boolean isAlive() { return health > 0; }
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.health
     * EFFECTS:  Sets health = min(maxHealth, health + amount). Health never exceeds maxHealth.
     */
    public void heal(int amount) { this.health = Math.min(maxHealth, this.health + amount); }
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.mana (only if mana >= amount)
     * EFFECTS:  If mana >= amount, decreases mana by amount and returns true.
     *           Otherwise leaves mana unchanged and returns false.
     */
    public boolean useMana(int amount) {
        if (mana < amount) return false;
        this.mana -= amount;
        return true;
    }
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.mana
     * EFFECTS:  Sets mana = min(maxMana, mana + amount). Mana never exceeds maxMana.
     */
    public void restoreMana(int amount) { this.mana = Math.min(maxMana, this.mana + amount); }
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.energy (only if energy >= amount)
     * EFFECTS:  If energy >= amount, decreases energy by amount and returns true.
     *           Otherwise leaves energy unchanged and returns false.
     */
    public boolean useEnergy(int amount) {
        if (energy < amount) return false;
        this.energy -= amount;
        return true;
    }
    /**
     * REQUIRES: amount >= 0
     * MODIFIES: this.energy
     * EFFECTS:  Sets energy = min(maxEnergy, energy + amount). Energy never exceeds maxEnergy.
     */
    public void restoreEnergy(int amount) { this.energy = Math.min(maxEnergy, this.energy + amount); }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getAttackPower() { return attackPower; }
    /**
     * REQUIRES: attackPower >= 0
     * MODIFIES: this.attackPower
     * EFFECTS:  Sets attackPower to the given value.
     */
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }
    public int getDefensePower() { return defensePower; }
    /**
     * REQUIRES: defensePower >= 0
     * MODIFIES: this.defensePower
     * EFFECTS:  Sets defensePower to the given value.
     */
    public void setDefensePower(int defensePower) { this.defensePower = defensePower; }
    public int getStrength() { return strength; }
    /**
     * REQUIRES: strength >= 0
     * MODIFIES: this.strength
     * EFFECTS:  Sets strength to the given value. Affects calculateDamage().
     */
    public void setStrength(int strength) { this.strength = strength; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    /**
     * REQUIRES: true (value clamped internally)
     * MODIFIES: this.mana
     * EFFECTS:  Sets mana = max(0, min(maxMana, mana)).
     */
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(maxMana, mana)); }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }
    /**
     * REQUIRES: true (value clamped internally)
     * MODIFIES: this.energy
     * EFFECTS:  Sets energy = max(0, min(maxEnergy, energy)).
     */
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(maxEnergy, energy)); }
    /**
     * REQUIRES: true (value clamped internally)
     * MODIFIES: this.health
     * EFFECTS:  Sets health = max(0, min(maxHealth, health)).
     */
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
    
    /**
     * REQUIRES: maxHealth > 0
     * MODIFIES: this.maxHealth
     * EFFECTS:  Sets the maximum health cap to the given value.
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * REQUIRES: maxEnergy >= 0
     * MODIFIES: this.maxEnergy
     * EFFECTS:  Sets the maximum energy cap to the given value.
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }
}