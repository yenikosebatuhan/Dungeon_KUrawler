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
     * Calculates total damage output for this character.
     *
     * <p>REQUIRES: true</p>
     *
     * <p>MODIFIES: none</p>
     *
     * <p>EFFECTS: Returns attackPower + strength. Does not depend on
     *            equipped items or external state.</p>
     */
    public int calculateDamage() { return attackPower + strength; }

    /**
     * Reduces this character's health by the given amount.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.health</p>
     *
     * <p>EFFECTS: Sets health = max(0, health - amount). Health never becomes
     *            negative and never exceeds maxHealth.</p>
     */
    // --- BUG-07 FIX: Removed defense subtraction here, handled in CombatResolver ---
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }
    /**
     * Tests if this character is alive.
     *
     * <p>REQUIRES: true</p>
     *
     * <p>MODIFIES: none</p>
     *
     * <p>EFFECTS: Returns true iff health > 0.</p>
     */
    public boolean isAlive() { return health > 0; }

    /**
     * Restores this character's health by the given amount.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.health</p>
     *
     * <p>EFFECTS: Sets health = min(maxHealth, health + amount). Health is clamped
     *            to maxHealth and never exceeds it.</p>
     */
    public void heal(int amount) { this.health = Math.min(maxHealth, this.health + amount); }
    /**
     * Attempts to use the given amount of mana.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.mana (if sufficient)</p>
     *
     * <p>EFFECTS: If mana >= amount, decreases mana by amount and returns true.
     *            Otherwise, leaves mana unchanged and returns false.</p>
     */
    public boolean useMana(int amount) {
        if (mana < amount) return false;
        this.mana -= amount;
        return true;
    }

    /**
     * Restores this character's mana by the given amount.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.mana</p>
     *
     * <p>EFFECTS: Sets mana = min(maxMana, mana + amount). Mana is clamped
     *            to maxMana and never exceeds it.</p>
     */
    public void restoreMana(int amount) { this.mana = Math.min(maxMana, this.mana + amount); }
    /**
     * Attempts to use the given amount of energy.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.energy (if sufficient)</p>
     *
     * <p>EFFECTS: If energy >= amount, decreases energy by amount and returns true.
     *            Otherwise, leaves energy unchanged and returns false.</p>
     */
    public boolean useEnergy(int amount) {
        if (energy < amount) return false;
        this.energy -= amount;
        return true;
    }

    /**
     * Restores this character's energy by the given amount.
     *
     * <p>REQUIRES: amount >= 0</p>
     *
     * <p>MODIFIES: this.energy</p>
     *
     * <p>EFFECTS: Sets energy = min(maxEnergy, energy + amount). Energy is clamped
     *            to maxEnergy and never exceeds it.</p>
     */
    public void restoreEnergy(int amount) { this.energy = Math.min(maxEnergy, this.energy + amount); }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getAttackPower() { return attackPower; }

    /**
     * Sets this character's attack power.
     *
     * <p>REQUIRES: attackPower >= 0</p>
     *
     * <p>MODIFIES: this.attackPower</p>
     *
     * <p>EFFECTS: Sets attackPower to the given value.</p>
     */
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }
    public int getDefensePower() { return defensePower; }

    /**
     * Sets this character's defense power.
     *
     * <p>REQUIRES: defensePower >= 0</p>
     *
     * <p>MODIFIES: this.defensePower</p>
     *
     * <p>EFFECTS: Sets defensePower to the given value.</p>
     */
    public void setDefensePower(int defensePower) { this.defensePower = defensePower; }
    public int getStrength() { return strength; }

    /**
     * Sets this character's strength stat.
     *
     * <p>REQUIRES: strength >= 0</p>
     *
     * <p>MODIFIES: this.strength</p>
     *
     * <p>EFFECTS: Sets strength to the given value. Affects calculateDamage().</p>
     */
    public void setStrength(int strength) { this.strength = strength; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }

    /**
     * Sets this character's current mana (clamped to [0, maxMana]).
     *
     * <p>REQUIRES: true (input clamped internally)</p>
     *
     * <p>MODIFIES: this.mana</p>
     *
     * <p>EFFECTS: Sets mana = max(0, min(maxMana, mana)). Value is automatically
     *            clamped to the valid range.</p>
     */
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(maxMana, mana)); }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }

    /**
     * Sets this character's current energy (clamped to [0, maxEnergy]).
     *
     * <p>REQUIRES: true (input clamped internally)</p>
     *
     * <p>MODIFIES: this.energy</p>
     *
     * <p>EFFECTS: Sets energy = max(0, min(maxEnergy, energy)). Value is automatically
     *            clamped to the valid range.</p>
     */
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(maxEnergy, energy)); }

    /**
     * Sets this character's current health (clamped to [0, maxHealth]).
     *
     * <p>REQUIRES: true (input clamped internally)</p>
     *
     * <p>MODIFIES: this.health</p>
     *
     * <p>EFFECTS: Sets health = max(0, min(maxHealth, health)). Value is automatically
     *            clamped to the valid range.</p>
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
}