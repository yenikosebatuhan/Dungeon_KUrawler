package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
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
    private static final int MELEE_REACH = 1;
    private static final Random RNG = new Random();
    private final List<HeroObserver> observers = new ArrayList<>();

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
        int damage = calculateDamage();
        boolean wasAlive = target.isAlive();
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

    public void equipWearable(Wearable wearable) {
        if (wearable == null || wearable == equippedWearable) return;
        if (equippedWearable != null) {
            setDefensePower(getDefensePower() - equippedWearable.getDefenseBonus());
        }
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
