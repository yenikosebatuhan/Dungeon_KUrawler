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
    private Wearable equippedWearable;

    private static final int ATTACK_ENERGY_COST = 1;
    private static final int RANGED_MANA_COST = 10;
    private static final int RANGED_DAMAGE = 6;
    private static final int MELEE_REACH = 1;
    private static final Random RNG = new Random();
    private final List<HeroObserver> observers = new ArrayList<>();
    private int facingRow = 0;
    private int facingCol = 1;

    /**
     * @requires name != null
     * @modifies this (via Character constructor), this.inventory
     * @effects Creates a Hero with 17 HP, 2 DEF, randomised STR in [8,15],
     *          80 mana, 100 energy, and an empty inventory.
     */
    public Hero(String name) {
        super(name, 17, 0, 2, 8 + RNG.nextInt(8), 80, 100);
        this.inventory = new Inventory();
    }

    /**
     * @requires observer != null
     * @modifies this.observers
     * @effects Adds observer to the list if not already present.
     */
    @Override
    public void addObserver(HeroObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * @requires true
     * @modifies this.observers
     * @effects Removes observer from the list if present.
     */
    @Override
    public void removeObserver(HeroObserver observer) {
        observers.remove(observer);
    }

    /**
     * @requires none
     * @modifies this.observers
     * @effects Removes all registered observers.
     */
    public void clearObservers() {
        observers.clear();
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns an unmodifiable view of the current observer list.
     */
    public List<HeroObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    /**
     * @requires true
     * @modifies this.row, this.col, this.currentCell, this.facingRow, this.facingCol,
     *           old cell occupant, newCell occupant, all HeroObservers
     * @effects Updates facing direction when newCell differs from the current cell,
     *          delegates move to super.moveToCell(), then notifies observers of movement.
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
     * @modifies this.health, all HeroObservers
     * @effects Delegates to super.takeDamage(), then notifies observers of actual
     *          damage taken (only if health changed).
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
     * @effects Returns strength + equippedWeapon.getDamage() (0 if no weapon equipped).
     */
    @Override
    public int calculateDamage() {
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
     *           - Else: moves hero to targetCell, updates facing direction,
     *             notifies all HeroObservers; returns true.
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
     * @requires target != null && target.isAlive()
     * @modifies this.energy, target.health, all HeroObservers
     * @effects Deducts ATTACK_ENERGY_COST energy. Calculates damage via
     *          CombatResolver and applies it to target. Notifies observers
     *          of the attack and whether the enemy was defeated.
     *          Returns false if target is null/dead or hero lacks energy or range.
     */
    public boolean attack(Enemy target) {
        if (target == null || !target.isAlive()) return false;
        if (getEnergy() < ATTACK_ENERGY_COST) return false;

        int dRow = Math.abs(target.getRow() - getRow());
        int dCol = Math.abs(target.getCol() - getCol());
        if (dRow + dCol > MELEE_REACH) return false;

        useEnergy(ATTACK_ENERGY_COST);

        boolean wasAlive = target.isAlive();

        int damage = com.kurawler.engine.CombatResolver.calculateFinalDamage(this, target);
        target.takeDamage(damage);

        notifyAttackResolved(damage, wasAlive && !target.isAlive());
        return true;
    }

    /**
     * @requires damage >= 0
     * @modifies all HeroObservers
     * @effects Notifies observers of the attack; also notifies enemy-defeated if
     *          defeatedEnemy is true.
     */
    public void notifyAttackResolved(int damage, boolean defeatedEnemy) {
        notifyAttacked(damage);
        if (defeatedEnemy) {
            notifyEnemyDefeated();
        }
    }

    /**
     * @requires none
     * @modifies all HeroObservers
     * @effects Notifies all observers that an item was collected.
     */
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
     * @modifies this.equippedWearable, this.defensePower
     * @effects If a wearable is already equipped, removes its bonus first.
     *          Then equips wearable and adds its defense bonus to defensePower.
     *          No-op if wearable is already the equipped item.
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
     * @requires none
     * @modifies this.equippedWearable, this.defensePower
     * @effects Removes the equipped wearable's defense bonus and sets equippedWearable
     *          to null. No-op if nothing is equipped.
     */
    public void unequipWearable() {
        if (equippedWearable == null) return;
        setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        equippedWearable = null;
    }

    /**
     * @requires consumable != null
     * @modifies this.inventory, this.health or this.mana or this.energy
     * @effects Removes consumable from inventory and restores the appropriate
     *          stat (HP, mana, or energy). Returns false if consumable is not
     *          in inventory.
     */
    public boolean useConsumable(Consumable consumable) {
        if (consumable == null || !inventory.removeItem(consumable)) return false;
        if (consumable.getHealAmount() > 0) heal(consumable.getHealAmount());
        if (consumable.getManaAmount() > 0) restoreMana(consumable.getManaAmount());
        if (consumable.getEnergyAmount() > 0) restoreEnergy(consumable.getEnergyAmount());
        return true;
    }

    /**
     * @requires getMana() >= RANGED_MANA_COST
     * @modifies this.mana, this.lastSpell (via Projectile constructor)
     * @effects Deducts RANGED_MANA_COST mana. Creates a Projectile aimed in the
     *          hero's facing direction. Returns null if mana is insufficient.
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

    /**
     * @requires none
     * @modifies none
     * @effects Returns the row component of the hero's current facing direction.
     */
    public int getFacingRow() { return facingRow; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the column component of the hero's current facing direction.
     */
    public int getFacingCol() { return facingCol; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the hero's inventory.
     */
    public Inventory getInventory() { return inventory; }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the currently equipped weapon, or null.
     */
    public Weapon getEquippedWeapon() { return equippedWeapon; }

    /**
     * @requires true
     * @modifies this.equippedWeapon
     * @effects Sets the equipped weapon (null to unequip).
     */
    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the currently equipped wearable, or null.
     */
    public Wearable getEquippedWearable() { return equippedWearable; }
}
