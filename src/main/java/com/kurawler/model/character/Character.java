package com.kurawler.model.character;

import com.kurawler.engine.ShadowCloneScrollRules;
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

    private GridCell currentCell;
    private int rotation;

    /**
     * @requires none
     * @modifies none
     * @effects Returns current rotation in degrees (0, 90, 180, or 270).
     */
    public int getRotation() { return rotation; }

    /**
     * @requires true
     * @modifies this.rotation
     * @effects Sets rotation to (degrees % 360 + 360) % 360, normalised to [0, 360).
     */
    public void setRotation(int degrees) { this.rotation = ((degrees % 360) + 360) % 360; }

    /**
     * @requires name != null && maxHealth > 0 && maxMana >= 0 && maxEnergy >= 0
     * @modifies this.name, this.health, this.maxHealth, this.attackPower,
     *           this.defensePower, this.strength, this.mana, this.maxMana,
     *           this.energy, this.maxEnergy
     * @effects Initialises all combat stats; health and mana and energy start at max.
     */
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
     * @requires none
     * @modifies none
     * @effects Returns generated melee damage: (STR / 2) + ATK (PDF §3, before DEF).
     */
    public int calculateDamage() {
        return (strength / 2) + attackPower;
    }

    /**
     * @requires amount >= 0
     * @modifies this.health
     * @effects Reduces health by amount, clamped to 0. Does not go below 0.
     */
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true iff health > 0.
     */
    public boolean isAlive() { return health > 0; }

    /**
     * @requires amount >= 0
     * @modifies this.health
     * @effects Increases health by amount, capped at maxHealth.
     */
    public void heal(int amount) { this.health = Math.min(maxHealth, this.health + amount); }

    /**
     * @requires amount >= 0
     * @modifies this.mana (only if mana >= amount)
     * @effects Deducts amount from mana and returns true; returns false without
     *          modifying mana if mana < amount.
     */
    public boolean useMana(int amount) {
        if (mana < amount) return false;
        this.mana -= amount;
        return true;
    }

    /**
     * @requires amount >= 0
     * @modifies this.mana
     * @effects Increases mana by amount, capped at maxMana.
     */
    public void restoreMana(int amount) { this.mana = Math.min(maxMana, this.mana + amount); }

    /**
     * @requires amount >= 0
     * @modifies this.energy (only if energy >= amount)
     * @effects Deducts amount from energy and returns true; returns false without
     *          modifying energy if energy < amount.
     */
    public boolean useEnergy(int amount) {
        if (energy < amount) return false;
        this.energy -= amount;
        return true;
    }

    /**
     * @requires amount >= 0
     * @modifies this.energy
     * @effects Increases energy by amount, capped at maxEnergy.
     */
    public void restoreEnergy(int amount) { this.energy = Math.min(maxEnergy, this.energy + amount); }

    /**
     * @requires none
     * @modifies none
     * @effects Returns this.name.
     */
    public String getName() { return name; }

    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current health.
     */
    public int getHealth() { return health; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns maximum health.
     */
    public int getMaxHealth() { return maxHealth; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current attack power.
     */
    public int getAttackPower() { return attackPower; }

    /**
     * @requires attackPower >= 0
     * @modifies this.attackPower
     * @effects Sets attackPower to the given value.
     */
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current defense power.
     */
    public int getDefensePower() { return defensePower; }

    /**
     * @requires defensePower >= 0
     * @modifies this.defensePower
     * @effects Sets defensePower to the given value.
     */
    public void setDefensePower(int defensePower) { this.defensePower = defensePower; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current strength.
     */
    public int getStrength() { return strength; }

    /**
     * @requires strength >= 0
     * @modifies this.strength
     * @effects Sets strength to the given value.
     */
    public void setStrength(int strength) { this.strength = strength; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current mana.
     */
    public int getMana() { return mana; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns maximum mana.
     */
    public int getMaxMana() { return maxMana; }

    /**
     * @requires true
     * @modifies this.mana
     * @effects Sets mana clamped to [0, maxMana].
     */
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(maxMana, mana)); }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current energy.
     */
    public int getEnergy() { return energy; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns maximum energy.
     */
    public int getMaxEnergy() { return maxEnergy; }

    /**
     * @requires true
     * @modifies this.energy
     * @effects Sets energy clamped to [0, maxEnergy].
     */
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(maxEnergy, energy)); }

    /**
     * @requires true
     * @modifies this.health
     * @effects Sets health clamped to [0, maxHealth].
     */
    public void setHealth(int health) { this.health = Math.max(0, Math.min(maxHealth, health)); }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current grid row.
     */
    public int getRow() { return row; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns current grid column.
     */
    public int getCol() { return col; }

    /**
     * @requires true
     * @modifies this.row, this.col
     * @effects Sets row and col without updating currentCell or cell occupants.
     */
    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Atomically moves this character to a new cell, keeping
     * (row, col), currentCell, and the cell's occupant in sync.
     * Pass null to remove the character from the map.
     *
     * @requires none
     * @modifies this.currentCell, this.row, this.col,
     *           old currentCell's occupyingCharacter, newCell's occupyingCharacter
     * @effects Clears occupant on the previous cell, sets this.currentCell to newCell,
     *          and updates this.row/col from newCell. If newCell is null, only removes
     *          this character from its previous cell.
     */
    public void moveToCell(GridCell newCell) {
        if (currentCell != null) {
            if (currentCell.getOccupyingCharacter() == this) {
                currentCell.setOccupyingCharacter(null);
            }
        }
        this.currentCell = newCell;
        if (newCell != null) {
            newCell.setOccupyingCharacter(this);
            this.row = newCell.getRow();
            this.col = newCell.getCol();
            if (this instanceof Enemy) {
                ShadowCloneScrollRules.removeFromCell(newCell);
            }
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the GridCell this character currently occupies, or null.
     */
    public GridCell getCurrentCell() { return currentCell; }

    /**
     * @requires true
     * @modifies this.currentCell
     * @effects Sets currentCell without updating cell occupant references.
     */
    public void setCurrentCell(GridCell cell) { this.currentCell = cell; }

    /**
     * @requires maxHealth > 0
     * @modifies this.maxHealth
     * @effects Sets the maximum health value.
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * @requires maxEnergy >= 0
     * @modifies this.maxEnergy
     * @effects Sets the maximum energy value.
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }
}
