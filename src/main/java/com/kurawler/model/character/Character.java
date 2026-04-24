package com.kurawler.model.character;

/**
 * Abstract base class for all characters.
 * Information Expert: Character knows its own stats and calculates damage.
 */
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

    /** Information Expert: damage = attackPower + strength. */
    public int calculateDamage() {
        return attackPower + strength;
    }

    public void takeDamage(int amount) {
        int effective = Math.max(0, amount - defensePower);
        this.health = Math.max(0, this.health - effective);
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void heal(int amount) {
        this.health = Math.min(maxHealth, this.health + amount);
    }

    public boolean useMana(int amount) {
        if (mana < amount) return false;
        this.mana -= amount;
        return true;
    }

    public void restoreMana(int amount) {
        this.mana = Math.min(maxMana, this.mana + amount);
    }

    public boolean useEnergy(int amount) {
        if (energy < amount) return false;
        this.energy -= amount;
        return true;
    }

    public void restoreEnergy(int amount) {
        this.energy = Math.min(maxEnergy, this.energy + amount);
    }

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
}
