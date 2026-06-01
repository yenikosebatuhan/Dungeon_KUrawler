package com.kurawler.controller;

import com.kurawler.action.Action;
import com.kurawler.action.BreakAction;
import com.kurawler.action.EquipAction;
import com.kurawler.action.ReadAction;
import com.kurawler.action.SearchAction;
import com.kurawler.action.WearAction;
import com.kurawler.action.MoveAction;
import com.kurawler.action.TakeAction;
import com.kurawler.effect.AddToInventoryEffect;
import com.kurawler.effect.BreakObjectEffect;
import com.kurawler.effect.Effect;
import com.kurawler.effect.RemoveFromCellEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.Inventory;
import com.kurawler.model.WeaponCatalog;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import java.util.List;

/**
 * GRASP Controller: Handles all system operations.
 * UI delegates to this controller — UI contains NO logic.
 *
 * The controller coordinates the Action-Effect cycle:
 * 1. Receives an Action from the UI/engine
 * 2. Resolves the Action into Effects
 * 3. Applies each Effect to the GameSession
 */
public class GameController {

    private final GameSession session;
    private final MapSerializer serializer = new MapSerializer();
    private final MapDeserializer deserializer = new MapDeserializer();

    public GameController(GameSession session) {
        this.session = session;
    }

    public boolean saveMap(String filename) {
        return serializer.saveGameState(session, filename);
    }

    public boolean loadMap(String filename) {
        return deserializer.loadGameState(filename, session);
    }

    public void startNewGame() {
        session.setCurrentMode("Play");
    }

    public void pauseGame() {
        session.setCurrentMode("Paused");
    }

    /**
     * Execute an action through the Action-Effect pipeline.
     * This is the central method — all game mutations flow through here.
     */
    public void executeAction(Action action) {
        List<Effect> effects = action.resolve(session);
        for (Effect effect : effects) {
            effect.apply(session);
        }
        session.advanceTurn();
    }

    /**
     * Called by the UI when the player presses a movement key.
     * Creates a MoveAction and executes it.
     *
     * <p>PDF §1.2: items are NOT auto-collected by walking over them. The player
     * must click an adjacent item and choose TAKE from the action menu. Therefore
     * no auto-pickup is performed here.</p>
     */
    public void moveHero(int deltaRow, int deltaCol) {
        MoveAction moveAction = new MoveAction(deltaRow, deltaCol);
        executeAction(moveAction);
    }

    /**
     * Auto-pickup: after hero moves, pick up all items on the current cell.
     * Creates TakeAction for each Item found on the hero's cell.
     */
    private void pickUpItemsOnHeroCell() {
        Hero hero = session.getHero();
        GridCell cell = session.getMap().getCell(hero.getRow(), hero.getCol());
        if (cell == null) return;

        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof Item) {
                TakeAction take = new TakeAction((Item) obj);
                List<Effect> effects = take.resolve(session);
                for (Effect effect : effects) {
                    effect.apply(session);
                }
            }
        }
    }

    /**
     * Uses the first Consumable found in the hero's inventory.
     * Heals the hero and removes the item from inventory.
     */
    public boolean useFirstConsumable() {
        Hero hero = session.getHero();
        Inventory inv = hero.getInventory();
        for (Item item : inv.getItems()) {
            if (item instanceof Consumable) {
                Consumable c = (Consumable) item;
                boolean used = hero.useConsumable(c);
                if (used) {
                    System.out.println("[Action] Used " + c.getName()
                            + " → HP: " + hero.getHealth() + "/" + hero.getMaxHealth());
                }
                return used;
            }
        }
        System.out.println("[Action] No consumable in inventory.");
        return false;
    }

    public boolean wearItem(Wearable wearable) {
        if (wearable == null || !session.getHero().getInventory().hasItem(wearable)) {
            return false;
        }
        executeAction(new WearAction(wearable));
        return true;
    }

    public boolean equipWeapon(Weapon weapon) {
        if (weapon == null || !session.getHero().getInventory().hasItem(weapon)) {
            return false;
        }
        executeAction(new EquipAction(weapon));
        return true;
    }

    public boolean unequipWearable(Wearable wearable) {
        Hero hero = session.getHero();
        if (wearable == null || !hero.isWearableEquipped(wearable)) {
            return false;
        }
        hero.unequipWearable(wearable);
        return true;
    }

    public boolean readScroll(Scroll scroll) {
        if (scroll == null || !session.getHero().getInventory().hasItem(scroll)) {
            return false;
        }
        executeAction(new ReadAction(scroll));
        showPortalMessageIfAny();
        return true;
    }

    private void showPortalMessageIfAny() {
        String msg = session.consumeLastPortalMessage();
        if (msg != null && !msg.isEmpty()) {
            System.out.println("[Scroll] " + msg);
        }
    }

    public boolean searchObject(StaticObject object) {
        if (object == null || !object.isSearchable()) {
            return false;
        }
        executeAction(new SearchAction(object));
        return true;
    }

    public BreakAction.BreakRoll breakObject(int row, int col) {
        BreakAction.BreakRoll roll = BreakAction.attempt(session, row, col);
        if (roll.succeeded()) {
            new BreakObjectEffect(row, col).apply(session);
            session.advanceTurn();
        }
        return roll;
    }

    /**
     * Interact with a specific item on a cell (triggered by 3x3 click menu).
     * Creates a TakeAction and runs it through the pipeline.
     */
    public boolean interactWithObject(int row, int col, Item item) {
        if (item == null || session.getHero().getInventory().isFull()) {
            return false;
        }
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return false;
        }
        boolean onCell = false;
        for (GameObject obj : cell.getObjects()) {
            if (obj == item) {
                onCell = true;
                break;
            }
        }
        if (!onCell) {
            return false;
        }
        if (!session.getHero().getInventory().addItem(item)) {
            return false;
        }
        if (item instanceof Weapon && WeaponCatalog.isArrow(item.getName())) {
            session.getHero().onArrowPickedUp();
        }
        new RemoveFromCellEffect(row, col, item).apply(session);
        session.getHero().notifyItemCollected();
        session.advanceTurn();
        return true;
    }

    public String heroShoot() {
        Hero hero = session.getHero();
        if (hero == null) {
            return "";
        }
        int cost = hero.getMagicManaCost();
        if (hero.getMana() < cost) {
            if (hero.hasStaffEquipped()) {
                return "Not enough mana to cast! (Need " + cost + " MP)";
            }
            return "Not enough mana to shoot!";
        }
        Projectile p = hero.shootProjectile();
        if (p == null) {
            return "Not enough mana to shoot!";
        }
        session.addProjectile(p);
        session.advanceTurn();
        if (hero.hasStaffEquipped()) {
            return "Staff spell! (MP -" + cost + ")";
        }
        return "Fired a bolt! (MP -" + cost + ")";
    }

    /**
     * Space: attack with the equipped weapon toward the first enemy in the facing direction.
     * Bow → arrow; bomb → throw; staff → use F (mana); everything else → melee.
     *
     * @return status message for the HUD (empty if nothing happened)
     */
    public String trySpaceWeaponAttack() {
        Hero hero = session.getHero();
        if (hero == null || session.getMap() == null) {
            return "";
        }

        Weapon weapon = hero.getEquippedWeapon();

        if (hero.hasEquippedBomb()) {
            String bombResult = tryBombThrow(hero);
            if (bombResult != null) {
                return bombResult;
            }
        }

        if (weapon != null && WeaponCatalog.isBow(weapon.getName())) {
            String bowResult = tryBowAttack(hero);
            if (bowResult != null) {
                return bowResult;
            }
        }

        if (weapon != null && WeaponCatalog.isStaffLike(weapon.getName())) {
            if (hero.canMeleeFacingEnemy(session)) {
                return tryMeleeFacingAttack(hero);
            }
            return "Cast with F — staves use mana at range.";
        }

        return tryMeleeFacingAttack(hero);
    }

    private String tryBombThrow(Hero hero) {
        Projectile projectile = hero.throwBomb(session);
        if (projectile != null) {
            session.addProjectile(projectile);
            session.advanceTurn();
            return "Threw a bomb!";
        }
        if (hero.canBombMeleeFacingEnemy(session)) {
            return tryBombMeleeAttack(hero);
        }
        int[] dir = hero.attackDirection();
        GridCell launch = session.getMap().getCell(
                hero.getRow() + dir[0], hero.getCol() + dir[1]);
        if (launch == null || (launch.getStaticObject() != null
                && !launch.getStaticObject().isPassable())) {
            return "Cannot throw — blocked in that direction.";
        }
        return "Not enough energy to throw!";
    }

    private String tryBombMeleeAttack(Hero hero) {
        int selfDamage = hero.bombMeleeDetonate(session);
        if (selfDamage < 0) {
            if (!hero.canBombMeleeFacingEnemy(session)) {
                return "No enemy adjacent for a bomb blast.";
            }
            return "Not enough energy to detonate!";
        }
        session.advanceTurn();
        return "Bomb detonated! You took " + selfDamage + " damage.";
    }

    private String tryBowAttack(Hero hero) {
        if (hero.canUseArrowWeapon()) {
            Projectile projectile = hero.shootArrow();
            if (projectile != null) {
                session.addProjectile(projectile);
                session.advanceTurn();
                return "Loosed an arrow! (" + hero.getArrowAmmo() + "/" + hero.getArrowAmmoMax() + ")";
            }
            return "Not enough energy to shoot!";
        }
        if (hero.canMeleeFacingEnemy(session)) {
            return tryMeleeFacingAttack(hero);
        }
        return "Equip arrows with your bow (I).";
    }

    private String tryMeleeFacingAttack(Hero hero) {
        if (!hero.canMeleeFacingEnemy(session)) {
            return "No enemy in that direction.";
        }
        if (!hero.attackFacingEnemy(session)) {
            return "Not enough energy to attack!";
        }
        session.advanceTurn();
        return "attack_melee";
    }

    public GameSession getSession() {
        return session;
    }
}
