package com.kurawler.controller;

import com.kurawler.action.Action;
import com.kurawler.action.MoveAction;
import com.kurawler.action.TakeAction;
import com.kurawler.effect.AddToInventoryEffect;
import com.kurawler.effect.Effect;
import com.kurawler.effect.RemoveFromCellEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
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
     * Creates a MoveAction, executes it, then auto-picks up any items on the new cell.
     */
    public void moveHero(int deltaRow, int deltaCol) {
        MoveAction moveAction = new MoveAction(deltaRow, deltaCol);
        executeAction(moveAction);
        pickUpItemsOnHeroCell();
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

    /**
     * Interact with a specific item on a cell (triggered by 3x3 click menu).
     * Creates a TakeAction and runs it through the pipeline.
     */
    public void interactWithObject(int row, int col, Item item) {
        if (session.getHero().getInventory().isFull()) return;
        new RemoveFromCellEffect(row, col, item).apply(session);
        new AddToInventoryEffect(item).apply(session);
        session.advanceTurn();
    }

    public GameSession getSession() {
        return session;
    }
}
