package com.kurawler.model.character;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Hero extends Character implements HeroSubject {
    private final Inventory inventory;
    private Weapon equippedWeapon;
    private Wearable equippedArmor;
    private Wearable equippedShield;
    private Wearable equippedRing;

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

    /**
     * @requires none
     * @modifies this.row, this.col, this.currentCell, this.facingRow, this.facingCol,
     *           previous cell's occupyingCharacter, newCell.occupyingCharacter, observers
     * @effects Moves this hero to newCell, updates facing direction, and notifies observers.
     *          If newCell == null, removes hero from the map.
     */
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

    /**
     * @requires amount >= 0
     * @modifies this.health, observers
     * @effects Delegates to super.takeDamage(amount) and notifies observers of actual damage
     *          dealt. Does nothing beyond notification if actual damage is 0.
     */
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
     * Returns the total damage including strength and equipped weapon.
     *
     * @requires none
     * @modifies none
     * @effects Returns calculated damage as strength + weapon damage.
     */
    @Override
    public int calculateDamage() {
        // Logic: STR + Weapon ATK
        int weaponDmg = (equippedWeapon != null) ? equippedWeapon.getDamage() : 0;
        return getStrength() + weaponDmg;
    }

    /**
     * @requires direction != null
     *           && direction.toUpperCase() in {"UP", "DOWN", "LEFT", "RIGHT"}
     *           && map != null
     *           && this.currentCell != null (hero must be placed on the map)
     *
     * @modifies this.row, this.col, this.currentCell,
     *           this.facingRow, this.facingCol,
     *           this.currentCell (previous).occupyingCharacter,
     *           targetCell.occupyingCharacter,
     *           every registered HeroObserver (notified via onHeroMoved)
     *
     * @effects  Let (dRow, dCol) be the unit delta for direction
     *           and targetCell = map.getCell(this.row + dRow, this.col + dCol).
     *           - If direction == null, map == null, or direction is not a
     *             recognised cardinal string: returns false, this unchanged.
     *           - Else if targetCell == null (out of bounds) or
     *             targetCell.isBlocked() (wall or occupied character):
     *             returns false, this unchanged.
     *           - Else: moves hero to targetCell —
     *               this.row     = targetCell.row,
     *               this.col     = targetCell.col,
     *               this.currentCell = targetCell,
     *               this.facingRow/facingCol = (dRow, dCol),
     *               previous cell's occupyingCharacter set to null,
     *               targetCell.occupyingCharacter set to this,
     *               all HeroObservers notified with new position and delta;
     *             returns true.
     *           Note: session is accepted for API consistency but is not
     *           read or modified by this method.
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

        if (targetCell == null || targetCell.isBlocked()) return false;

        moveToCell(targetCell);
        return true;
    }

    /**
     * @requires target != null
     * @modifies this.energy, target.health, observers
     * @effects If target is dead, out of melee range, or hero lacks energy, returns false.
     *          Otherwise spends ATTACK_ENERGY_COST energy, deals CombatResolver-calculated
     *          damage to target, notifies observers, and returns true.
     */
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
     * @requires wearable != null
     * @modifies this.defensePower, this.equippedArmor, this.equippedShield, this.equippedRing
     * @effects If wearable is already equipped in its slot, does nothing. Otherwise unequips
     *          the current item in that slot, equips wearable, and adjusts defensePower.
     */
    public void equipWearable(Wearable wearable) {
        if (wearable == null) {
            return;
        }
        Wearable current = getWearableInSlot(wearable);
        if (wearable == current) {
            return;
        }
        if (current != null) {
            setDefensePower(getDefensePower() - current.getDefenseBonus());
        }
        setWearableInSlot(wearable);
        setDefensePower(getDefensePower() + wearable.getDefenseBonus());
    }

    /**
     * @requires wearable != null
     * @modifies this.defensePower, this.equippedArmor, this.equippedShield, this.equippedRing
     * @effects If wearable is equipped in its slot, unequips it and subtracts its defense
     *          bonus from defensePower. Otherwise does nothing.
     */
    public void unequipWearable(Wearable wearable) {
        if (wearable == null) {
            return;
        }
        Wearable current = getWearableInSlot(wearable);
        if (current != wearable) {
            return;
        }
        setDefensePower(getDefensePower() - current.getDefenseBonus());
        clearWearableSlot(wearable);
    }

    /**
     * @requires none
     * @modifies this.defensePower, this.equippedArmor, this.equippedShield, this.equippedRing
     * @effects Unequips the first filled wearable slot (armor, then shield, then ring).
     *          If every wearable slot is empty, does nothing.
     */
    public void unequipWearable() {
        if (equippedArmor != null) {
            unequipWearable(equippedArmor);
        } else if (equippedShield != null) {
            unequipWearable(equippedShield);
        } else if (equippedRing != null) {
            unequipWearable(equippedRing);
        }
    }

    public boolean isWearableEquipped(Wearable wearable) {
        return wearable != null && wearable == getWearableInSlot(wearable);
    }

    private Wearable getWearableInSlot(Wearable wearable) {
        if (wearable.isShield()) {
            return equippedShield;
        }
        if (wearable.isRing()) {
            return equippedRing;
        }
        return equippedArmor;
    }

    private void setWearableInSlot(Wearable wearable) {
        if (wearable.isShield()) {
            equippedShield = wearable;
        } else if (wearable.isRing()) {
            equippedRing = wearable;
        } else {
            equippedArmor = wearable;
        }
    }

    private void clearWearableSlot(Wearable wearable) {
        if (wearable.isShield()) {
            equippedShield = null;
        } else if (wearable.isRing()) {
            equippedRing = null;
        } else {
            equippedArmor = null;
        }
    }

    /**
     * @requires consumable != null
     * @modifies this.health, this.mana, this.energy, inventory
     * @effects If consumable is not in inventory, returns false. Otherwise removes it from
     *          inventory, applies its heal, mana, and energy amounts, and returns true.
     */
    public boolean useConsumable(Consumable consumable) {
        if (consumable == null || !inventory.removeItem(consumable)) return false;
        if (consumable.getHealAmount() > 0) heal(consumable.getHealAmount());
        if (consumable.getManaAmount() > 0) restoreMana(consumable.getManaAmount());
        if (consumable.getEnergyAmount() > 0) restoreEnergy(consumable.getEnergyAmount());
        return true;
    }

    /**
     * @requires none
     * @modifies this.mana, observers
     * @effects If mana < RANGED_MANA_COST, returns null. Otherwise spends RANGED_MANA_COST
     *          mana, notifies observers, and returns a projectile positioned one cell ahead
     *          in the facing direction.
     */
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
    /**
     * Equips a weapon and notifies all observers of the stat change.
     *
     * @requires weapon != null
     * @modifies this.equippedWeapon, observers
     * @effects Sets the equipped weapon and triggers a stats update notification for the UI.
     */
    public void setEquippedWeapon(Weapon weapon) { 
        this.equippedWeapon = weapon; 
    }


    /** Body armor / ring slot (not shield). */
    public Wearable getEquippedArmor() { return equippedArmor; }

    public Wearable getEquippedShield() { return equippedShield; }

    public Wearable getEquippedRing() { return equippedRing; }

    /** @deprecated use {@link #getEquippedArmor()} — kept for older save format */
    public Wearable getEquippedWearable() { return equippedArmor; }

    /**
     * RI: name != null and non-empty; 0 <= health <= maxHealth; maxHealth > 0;
     *     0 <= mana <= maxMana; 0 <= energy <= maxEnergy;
     *     strength >= 0; attackPower >= 0; defensePower >= 0; inventory != null.
     *
     * @requires none
     * @modifies none
     * @effects Returns true iff all representation invariant conditions hold.
     */
    public boolean repOk() {
        if (getName() == null || getName().isEmpty()) return false;
        if (getHealth() < 0 || getHealth() > getMaxHealth()) return false;
        if (getMana() < 0 || getMana() > getMaxMana()) return false;
        if (getEnergy() < 0 || getEnergy() > getMaxEnergy()) return false;
        if (getMaxHealth() <= 0 || getMaxMana() < 0 || getMaxEnergy() < 0) return false;
        if (getStrength() < 0 || getAttackPower() < 0 || getDefensePower() < 0) return false;
        return inventory != null;
    }
}
