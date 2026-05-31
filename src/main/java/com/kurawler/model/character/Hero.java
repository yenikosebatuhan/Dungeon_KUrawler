package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.WeaponCatalog;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.observer.HeroObserver;
import com.kurawler.model.observer.HeroSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hero extends Character implements HeroSubject {
    private final Inventory inventory;
    private Weapon equippedWeapon;
    private Weapon equippedArrow;
    private Wearable equippedArmor;
    private Wearable equippedShield;
    private Wearable equippedRing;

    private static final int ATTACK_ENERGY_COST = 1;
    private static final int RANGED_MANA_COST = 10;
    private static final int STAFF_MANA_BONUS = 10;
    private static final int STAFF_SPELL_DAMAGE_BONUS = 10;
    private static final int RANGED_DAMAGE = 6;
    private static final int MELEE_REACH = 1;
    /** Space melee: enemy up to this many tiles straight ahead. */
    private static final int MELEE_FACING_REACH = 2;
    private static final int ARROW_AMMO_ON_EQUIP = 3;
    private static final int BOMB_MAX_RANGE = 5;
    private static final int STAFF_STRIKE_RANGE = 6;
    private static final int STARTING_STRENGTH = 10;
    private final List<HeroObserver> observers = new ArrayList<>();
    private int facingRow = 0;
    private int facingCol = 1;
    private int arrowAmmo;
    private int arrowAmmoMax;

    public Hero(String name) {
        super(name, 17, 0, 2, STARTING_STRENGTH, 80, 100);
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
        setFacingFromDelta(deltaRow, deltaCol);
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

    public boolean attack(Enemy target) {
        return attack(target, MELEE_REACH);
    }

    public boolean attack(Enemy target, int maxManhattanReach) {
        if (target == null || !target.isAlive()) return false;
        if (getEnergy() < ATTACK_ENERGY_COST) return false;

        int dRow = Math.abs(target.getRow() - getRow());
        int dCol = Math.abs(target.getCol() - getCol());
        if (dRow + dCol > maxManhattanReach) return false;
        if (!isOnFacingLine(target)) return false;

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

    /** Unequips the first filled wearable slot (armor, then shield, then ring). */
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

    public boolean useConsumable(Consumable consumable) {
        if (consumable == null || !inventory.removeItem(consumable)) return false;
        if (consumable.getHealAmount() > 0) heal(consumable.getHealAmount());
        if (consumable.getManaAmount() > 0) restoreMana(consumable.getManaAmount());
        if (consumable.getEnergyAmount() > 0) restoreEnergy(consumable.getEnergyAmount());
        return true;
    }

    public boolean hasStaffEquipped() {
        return equippedWeapon != null && WeaponCatalog.isStaffLike(equippedWeapon.getName());
    }

    public int getMagicManaCost() {
        if (hasStaffEquipped()) {
            return RANGED_MANA_COST + STAFF_MANA_BONUS;
        }
        return RANGED_MANA_COST;
    }

    public Projectile shootProjectile() {
        int manaCost = getMagicManaCost();
        if (getMana() < manaCost) {
            return null;
        }
        useMana(manaCost);
        int dmg = getMagicBoltDamage();
        int[] dir = attackDirection();
        String projName = hasStaffEquipped() ? "Staff Bolt" : "Hero Bolt";
        String desc = hasStaffEquipped()
                ? "Arcane energy from " + equippedWeapon.getName()
                : "A focused energy bolt";
        Projectile p = new Projectile(projName, desc, dmg, dir[0], dir[1]);
        p.setPosition(getRow(), getCol());
        if (hasStaffEquipped()) {
            p.setMaxRange(STAFF_STRIKE_RANGE);
        }
        notifyAttacked(dmg);
        return p;
    }

    private int getMagicBoltDamage() {
        if (hasStaffEquipped()) {
            return RANGED_DAMAGE + STAFF_SPELL_DAMAGE_BONUS + equippedWeapon.getDamage()
                    + getStrength() / 2;
        }
        int weaponBonus = equippedWeapon != null ? equippedWeapon.getDamage() / 2 : 0;
        return RANGED_DAMAGE + weaponBonus;
    }

    public Projectile shootArrow() {
        if (equippedWeapon == null || !WeaponCatalog.isBow(equippedWeapon.getName())) {
            return null;
        }
        if (getEnergy() < ATTACK_ENERGY_COST || arrowAmmo < 1) {
            return null;
        }
        useEnergy(ATTACK_ENERGY_COST);
        arrowAmmo--;
        if (arrowAmmo <= 0) {
            arrowAmmo = 0;
            removeAllArrowItemsFromInventory();
            equippedArrow = null;
        }
        int[] dir = attackDirection();
        int dmg = getStrength() + WeaponCatalog.damageFor("Arrow") + equippedWeapon.getDamage() / 2;
        Projectile p = new Projectile("Arrow", "A loosed arrow", dmg, dir[0], dir[1]);
        p.setPosition(getRow(), getCol());
        notifyAttacked(dmg);
        return p;
    }

    public boolean hasEquippedBomb() {
        return equippedWeapon != null && WeaponCatalog.isBomb(equippedWeapon.getName());
    }

    /** Melee reach for Space attacks — flail hits only the adjacent tile. */
    public int meleeFacingReach() {
        if (equippedWeapon != null && WeaponCatalog.isFlailBall(equippedWeapon.getName())) {
            return MELEE_REACH;
        }
        return MELEE_FACING_REACH;
    }

    public boolean canBombMeleeFacingEnemy(GameSession session) {
        return hasEquippedBomb() && findFirstEnemyInDirection(session, MELEE_REACH) != null;
    }

    /**
     * Detonates the equipped bomb on an enemy exactly one tile ahead.
     *
     * @return splash damage dealt to this hero, or -1 if the detonation did not happen
     */
    public int bombMeleeDetonate(GameSession session) {
        if (!hasEquippedBomb() || !inventory.hasItem(equippedWeapon)) {
            return -1;
        }
        if (getEnergy() < ATTACK_ENERGY_COST) {
            return -1;
        }
        Enemy target = findFirstEnemyInDirection(session, MELEE_REACH);
        if (target == null) {
            return -1;
        }

        useEnergy(ATTACK_ENERGY_COST);
        Weapon bomb = equippedWeapon;
        int enemyDmg = getStrength() + bomb.getDamage() + 4;
        int selfDmg = Math.max(1, bomb.getDamage() + 3);

        inventory.removeItem(bomb);
        setEquippedWeapon(null);

        boolean wasAlive = target.isAlive();
        target.takeDamage(enemyDmg);
        takeDamage(selfDmg);

        if (session != null && session.getMap() != null && wasAlive && !target.isAlive()) {
            GridCell cell = session.getMap().getCell(target.getRow(), target.getCol());
            if (cell != null) {
                cell.setOccupyingCharacter(null);
            }
            session.decrementEnemyCount();
        }

        notifyAttackResolved(enemyDmg, wasAlive && !target.isAlive());
        return selfDmg;
    }

    /**
     * Throws the equipped bomb. Requires bomb equipped and a clear tile in facing direction.
     */
    public Projectile throwBomb(GameSession session) {
        if (!hasEquippedBomb() || !inventory.hasItem(equippedWeapon)) {
            return null;
        }
        if (getEnergy() < ATTACK_ENERGY_COST) {
            return null;
        }
        int[] dir = attackDirection();
        int firstRow = getRow() + dir[0];
        int firstCol = getCol() + dir[1];
        if (!isPassableForProjectile(session, firstRow, firstCol)) {
            return null;
        }

        useEnergy(ATTACK_ENERGY_COST);
        Weapon bomb = equippedWeapon;
        int dmg = getStrength() + bomb.getDamage() + 4;
        inventory.removeItem(bomb);
        setEquippedWeapon(null);

        Projectile p = new Projectile("Bomb", "A thrown bomb", dmg, dir[0], dir[1]);
        p.setPosition(getRow(), getCol());
        p.setMaxRange(BOMB_MAX_RANGE);
        notifyAttacked(dmg);
        return p;
    }

    private static boolean isPassableForProjectile(GameSession session, int row, int col) {
        if (session == null || session.getMap() == null) {
            return false;
        }
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return false;
        }
        if (cell.getStaticObject() != null && !cell.getStaticObject().isPassable()) {
            return false;
        }
        return true;
    }

    /**
     * Updates facing from a movement key or step. Called even when the hero cannot move
     * (blocked tile, no energy) so Space attacks aim where the player is pointing.
     */
    public void setFacingFromDelta(int deltaRow, int deltaCol) {
        if (deltaRow == 0 && deltaCol == 0) {
            return;
        }
        facingRow = Integer.signum(deltaRow);
        facingCol = Integer.signum(deltaCol);
    }

    /**
     * Attack/projectile direction — matches {@code TileRenderer.drawHeroFacing} (horizontal
     * facing wins when both axes are set, e.g. after a portal jump).
     */
    public int[] attackDirection() {
        if (facingCol > 0) {
            return new int[]{0, 1};
        }
        if (facingCol < 0) {
            return new int[]{0, -1};
        }
        if (facingRow < 0) {
            return new int[]{-1, 0};
        }
        if (facingRow > 0) {
            return new int[]{1, 0};
        }
        return new int[]{0, 1};
    }

    /**
     * First living enemy on a straight line in {@link #attackDirection()}, up to {@code maxSteps}.
     */
    public Enemy findFirstEnemyInDirection(GameSession session, int maxSteps) {
        if (session == null || session.getMap() == null || maxSteps < 1) {
            return null;
        }
        int[] dir = attackDirection();
        Map map = session.getMap();
        for (int step = 1; step <= maxSteps; step++) {
            int row = getRow() + dir[0] * step;
            int col = getCol() + dir[1] * step;
            GridCell cell = map.getCell(row, col);
            if (cell == null) {
                return null;
            }
            if (cell.getStaticObject() != null && !cell.getStaticObject().isPassable()) {
                return null;
            }
            if (cell.getOccupyingCharacter() instanceof Enemy enemy && enemy.isAlive()) {
                return enemy;
            }
        }
        return null;
    }

    private boolean isOnFacingLine(Enemy target) {
        int[] dir = attackDirection();
        int dr = target.getRow() - getRow();
        int dc = target.getCol() - getCol();
        if (dir[0] != 0) {
            return dc == 0 && Integer.signum(dr) == dir[0];
        }
        if (dir[1] != 0) {
            return dr == 0 && Integer.signum(dc) == dir[1];
        }
        return dr == 0 && dc > 0;
    }

    public boolean attackFacingEnemy(GameSession session) {
        int reach = meleeFacingReach();
        Enemy target = findFirstEnemyInDirection(session, reach);
        return target != null && attack(target, reach);
    }

    public boolean canMeleeFacingEnemy(GameSession session) {
        return findFirstEnemyInDirection(session, meleeFacingReach()) != null;
    }

    /**
     * @return null if equip blocked; empty string on success; message if blocked with reason
     */
    public String toggleEquipWeapon(Weapon weapon) {
        if (weapon == null || !inventory.hasItem(weapon)) {
            return "Not in inventory.";
        }
        String name = weapon.getName();
        if (WeaponCatalog.isArrow(name)) {
            if (equippedWeapon == null || !WeaponCatalog.isBow(equippedWeapon.getName())) {
                return "Equip a bow before arrows.";
            }
            if (equippedArrow == weapon) {
                equippedArrow = null;
            } else {
                equippedArrow = weapon;
                grantArrowAmmo(ARROW_AMMO_ON_EQUIP);
            }
            return "";
        }
        if (equippedWeapon == weapon) {
            equippedWeapon = null;
            if (WeaponCatalog.isBow(name)) {
                equippedArrow = null;
            }
            return "";
        }
        if (WeaponCatalog.isArrow(name)) {
            return "Equip a bow before arrows.";
        }
        if (equippedWeapon != null && WeaponCatalog.isStaffLike(equippedWeapon.getName())
                && !WeaponCatalog.isStaffLike(name)) {
            return "Unequip your staff before using another weapon.";
        }
        equippedWeapon = weapon;
        equippedArrow = null;
        if (WeaponCatalog.isBow(name)) {
            Weapon arrow = findArrowInInventory();
            if (arrow != null) {
                equippedArrow = arrow;
                grantArrowAmmo(ARROW_AMMO_ON_EQUIP);
            }
        }
        return "";
    }

    /** Called after an Arrow item is added to inventory from the map. */
    public void onArrowPickedUp() {
        arrowAmmo++;
        if (arrowAmmoMax < arrowAmmo) {
            arrowAmmoMax = arrowAmmo;
        }
        consolidateArrowInventorySlots();
    }

    private void grantArrowAmmo(int amount) {
        arrowAmmo = amount;
        arrowAmmoMax = amount;
        consolidateArrowInventorySlots();
        if (equippedArrow == null) {
            equippedArrow = findArrowInInventory();
        }
    }

    private void consolidateArrowInventorySlots() {
        Weapon keep = findArrowInInventory();
        for (Item item : new ArrayList<>(inventory.getItems())) {
            if (item instanceof Weapon && WeaponCatalog.isArrow(item.getName()) && item != keep) {
                inventory.removeItem(item);
            }
        }
        if (arrowAmmo > 0 && keep == null && !inventory.isFull()) {
            inventory.addItem(WeaponCatalog.create("Arrow"));
            equippedArrow = findArrowInInventory();
        }
        if (arrowAmmo <= 0) {
            removeAllArrowItemsFromInventory();
        }
    }

    private void removeAllArrowItemsFromInventory() {
        for (Item item : new ArrayList<>(inventory.getItems())) {
            if (item instanceof Weapon && WeaponCatalog.isArrow(item.getName())) {
                inventory.removeItem(item);
            }
        }
    }

    private Weapon findArrowInInventory() {
        for (Item item : inventory.getItems()) {
            if (item instanceof Weapon && WeaponCatalog.isArrow(item.getName())) {
                return (Weapon) item;
            }
        }
        return null;
    }

    public boolean canUseArrowWeapon() {
        return equippedWeapon != null
                && WeaponCatalog.isBow(equippedWeapon.getName())
                && arrowAmmo > 0;
    }

    public int getArrowAmmo() {
        return arrowAmmo;
    }

    public int getArrowAmmoMax() {
        return arrowAmmoMax > 0 ? arrowAmmoMax : ARROW_AMMO_ON_EQUIP;
    }

    public int getArrowCount() {
        return arrowAmmo;
    }

    public int getFacingRow() { return facingRow; }
    public int getFacingCol() { return facingCol; }

    public Inventory getInventory() { return inventory; }
    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public Weapon getEquippedArrow() { return equippedArrow; }

    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
        if (weapon == null || !WeaponCatalog.isBow(weapon.getName())) {
            equippedArrow = null;
        }
    }

    public void setEquippedArrow(Weapon arrow) {
        this.equippedArrow = arrow;
    }


    /** Body armor / ring slot (not shield). */
    public Wearable getEquippedArmor() { return equippedArmor; }

    public Wearable getEquippedShield() { return equippedShield; }

    public Wearable getEquippedRing() { return equippedRing; }

    /** @deprecated use {@link #getEquippedArmor()} — kept for older save format */
    public Wearable getEquippedWearable() { return equippedArmor; }
}
