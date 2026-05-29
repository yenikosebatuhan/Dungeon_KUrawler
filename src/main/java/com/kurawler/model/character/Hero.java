package com.kurawler.model.character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.observer.HeroObserver;
import com.kurawler.model.observer.HeroSubject;

public class Hero extends Character implements HeroSubject {
    private final Inventory inventory;
    private Weapon equippedWeapon;
    private Wearable equippedWearable;

    private static final int ATTACK_ENERGY_COST = 1;
    private static final int RANGED_MANA_COST = 10;
    private static final int RANGED_DAMAGE = 6;
    private static final int MELEE_REACH = 1;
    private static final Random RNG = new Random();
    private final List<HeroObserver> observers = new ArrayList<>();
    private int facingRow = 0;
    private int facingCol = 1;

    public Hero(String name) {
        super(name, 17, 0, 2, 8 + RNG.nextInt(8), 80, 100);
        this.inventory = new Inventory();
    }

    @Override
    public void addObserver(HeroObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(HeroObserver observer) {
        observers.remove(observer);
    }

    public void clearObservers() {
        observers.clear();
    }

    public List<HeroObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    @Override
    public void moveToCell(GridCell newCell) {
        if (newCell == null) {
            super.moveToCell(null);
            return;
        }

        int deltaRow = newCell.getRow() - getRow();
        int deltaCol = newCell.getCol() - getCol();
        if (deltaRow != 0 || deltaCol != 0) {
            facingRow = deltaRow;
            facingCol = deltaCol;
        }
        super.moveToCell(newCell);
        notifyMoved(getRow(), getCol(), deltaRow, deltaCol);
    }

    @Override
    public void takeDamage(int amount) {
        int before = getHealth();
        super.takeDamage(amount);
        int actualDamage = before - getHealth();
        if (actualDamage > 0) {
            notifyDamageTaken(actualDamage);
        }
    }

    /**
     * Calculates total damage output for this hero, including equipped weapon bonus.
     *
     * <p>REQUIRES: true</p>
     *
     * <p>MODIFIES: nothing</p>
     *
     * <p>EFFECTS: Returns getStrength() + equippedWeapon.getDamage() if a weapon
     *            is equipped, or getStrength() if no weapon is equipped.</p>
     */
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

        if (targetCell == null || targetCell.isBlocked()) return false;

        moveToCell(targetCell);
        return true;
    }

    public boolean attack(Enemy target) {
        if (target == null || !target.isAlive()) return false;
        if (getEnergy() < ATTACK_ENERGY_COST) return false;

        int dRow = Math.abs(target.getRow() - getRow());
        int dCol = Math.abs(target.getCol() - getCol());
        if (dRow + dCol > MELEE_REACH) return false;

        useEnergy(ATTACK_ENERGY_COST);
        
        boolean wasAlive = target.isAlive();
        
        // --- BUG-07 FIX: Route Hero attacks through centralized CombatResolver ---
        int damage = com.kurawler.engine.CombatResolver.calculateFinalDamage(this, target);
        target.takeDamage(damage);
        
        notifyAttackResolved(damage, wasAlive && !target.isAlive());
        return true;
    }

    public void notifyAttackResolved(int damage, boolean defeatedEnemy) {
        notifyAttacked(damage);
        if (defeatedEnemy) {
            notifyEnemyDefeated();
        }
    }

    public void notifyItemCollected() {
        for (HeroObserver observer : new ArrayList<>(observers)) {
            observer.onItemCollected();
        }
    }

    private void notifyMoved(int newRow, int newCol, int deltaRow, int deltaCol) {
        for (HeroObserver observer : new ArrayList<>(observers)) {
            observer.onHeroMoved(newRow, newCol, deltaRow, deltaCol);
        }
    }

    private void notifyAttacked(int damage) {
        for (HeroObserver observer : new ArrayList<>(observers)) {
            observer.onHeroAttacked(damage);
        }
    }

    private void notifyDamageTaken(int amount) {
        for (HeroObserver observer : new ArrayList<>(observers)) {
            observer.onHeroDamageTaken(amount);
        }
    }

    private void notifyEnemyDefeated() {
        for (HeroObserver observer : new ArrayList<>(observers)) {
            observer.onEnemyDefeated();
        }
    }

    /**
     * Equips the given wearable item, adjusting this hero's defense power accordingly.
     *
     * <p>REQUIRES: wearable != null</p>
     *
     * <p>MODIFIES: this.defensePower, this.equippedWearable</p>
     *
     * <p>EFFECTS:  If wearable == equippedWearable, does nothing.
     *              Otherwise, if equippedWearable != null, subtracts
     *              equippedWearable.getDefenseBonus() from defensePower first.
     *              Then sets equippedWearable = wearable and adds
     *              wearable.getDefenseBonus() to defensePower.</p>
     */
    public void equipWearable(Wearable wearable) {
        if (wearable == null || wearable == equippedWearable) return;
        if (equippedWearable != null) {
            setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        }
        equippedWearable = wearable;
        setDefensePower(getDefensePower() + wearable.getDefenseBonus());
    }

    /**
     * Unequips the currently equipped wearable item, if any.
     *
     * <p>REQUIRES: true</p>
     *
     * <p>MODIFIES: this.defensePower, this.equippedWearable</p>
     *
     * <p>EFFECTS: If equippedWearable != null, subtracts its defense bonus from
     *            defensePower and sets equippedWearable = null.
     *            If equippedWearable == null, does nothing.</p>
     */
    public void unequipWearable() {
        if (equippedWearable == null) return;
        setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        equippedWearable = null;
    }

    /**
     * Uses (consumes) a consumable item from inventory.
     *
     * <p>REQUIRES: consumable != null</p>
     *
     * <p>MODIFIES: this.health, this.mana, this.energy, inventory</p>
     *
     * <p>EFFECTS: If consumable is not in inventory, returns false and does nothing.
     *            Otherwise, removes consumable from inventory and applies its effects:
     *            - Heals by consumable.getHealAmount() (clamped to maxHealth)
     *            - Restores mana by consumable.getManaAmount() (clamped to maxMana)
     *            - Restores energy by consumable.getEnergyAmount() (clamped to maxEnergy)
     *            Returns true if consumable was consumed, false otherwise.</p>
     */
    public boolean useConsumable(Consumable consumable) {
        if (consumable == null || !inventory.removeItem(consumable)) return false;
        if (consumable.getHealAmount() > 0) heal(consumable.getHealAmount());
        if (consumable.getManaAmount() > 0) restoreMana(consumable.getManaAmount());
        if (consumable.getEnergyAmount() > 0) restoreEnergy(consumable.getEnergyAmount());
        return true;
    }

    public Projectile shootProjectile() {
        if (getMana() < RANGED_MANA_COST) return null;
        useMana(RANGED_MANA_COST);
        int dmg = RANGED_DAMAGE + (equippedWeapon != null ? equippedWeapon.getDamage() / 2 : 0);
        Projectile p = new Projectile("Hero Bolt", "A focused energy bolt", dmg, facingRow, facingCol);
        p.setPosition(getRow() + facingRow, getCol() + facingCol);
        notifyAttacked(dmg);
        return p;
    }

    public int getFacingRow() { return facingRow; }
    public int getFacingCol() { return facingCol; }

    public Inventory getInventory() { return inventory; }
    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(Weapon weapon) { this.equippedWeapon = weapon; }
    public Wearable getEquippedWearable() { return equippedWearable; }

    /**
     * Checks the representation invariant of this Hero.
     *
     * <p>RI: name != null and non-empty; 0 <= health <= maxHealth; maxHealth > 0;
     *        0 <= mana <= maxMana; 0 <= energy <= maxEnergy;
     *        strength >= 0; attackPower >= 0; defensePower >= 0;
     *        inventory != null</p>
     *
     * @return true iff all invariant conditions hold
     */
    public boolean repOk() {
        if (getName() == null || getName().isEmpty()) return false;
        if (getHealth() < 0 || getHealth() > getMaxHealth()) return false;
        if (getMana() < 0 || getMana() > getMaxMana()) return false;
        if (getEnergy() < 0 || getEnergy() > getMaxEnergy()) return false;
        if (getMaxHealth() <= 0 || getMaxMana() < 0 || getMaxEnergy() < 0) return false;
        if (getStrength() < 0 || getAttackPower() < 0 || getDefensePower() < 0) return false;
        if (inventory == null) return false;
        return true;
    }
}
