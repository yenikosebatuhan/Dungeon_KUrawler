package com.kurawler.controller;

import com.kurawler.action.Action;
import com.kurawler.action.MoveAction;
import com.kurawler.action.TakeAction;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticObject;
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
        // Turn tracking removed — Caner's new GameSession no longer has advanceTurn()
    }

    public void moveHero(int deltaRow, int deltaCol) {
        Hero hero = session.getHero();
        if (hero != null) hero.setFacing(deltaRow, deltaCol);
        executeAction(new MoveAction(deltaRow, deltaCol));
        pickUpItemsOnHeroCell();
    }

    private void pickUpItemsOnHeroCell() {
        Hero hero = session.getHero();
        GridCell cell = session.getMap().getCell(hero.getRow(), hero.getCol());
        if (cell == null) return;
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof Item) {
                TakeAction take = new TakeAction((Item) obj);
                for (Effect effect : take.resolve(session)) effect.apply(session);
            }
        }
    }

    public GameSession getSession() {
        return session;
    }

    public boolean heroShoot() {
        Hero hero = session.getHero();
        if (hero == null || hero.getMana() < 10) return false;
        hero.useMana(10);
        return true;
    }

    public boolean searchObject(StaticObject so, int row, int col) {
        if (so == null || so.isSearched()) return false;
        so.markSearched();
        Item hidden = so.getHiddenItem();
        if (hidden != null) {
            GridCell cell = session.getMap().getCell(row, col);
            if (cell != null) cell.addObject(hidden);
        }
        return true;
    }

    public boolean breakObject(int row, int col) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null || cell.getStaticObject() == null) return false;
        cell.setStaticObject(null);
        return true;
    }

    public void interactWithObject(int row, int col, Item item) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) return;
        if (session.getHero().getInventory().addItem(item)) {
            cell.removeObject(item);
        }
    }

    public void useFirstConsumable() {
        Hero hero = session.getHero();
        for (Item item : hero.getInventory().getItems()) {
            if (item instanceof Consumable) {
                hero.useConsumable((Consumable) item);
                return;
            }
        }
    }

    public void readScroll(Scroll sc) {
        session.getHero().getInventory().removeItem(sc);
    }

    public void wearItem(Wearable wearable) {
        session.getHero().equipWearable(wearable);
    }

    public void unequipWearable(Wearable wearable) {
        Hero hero = session.getHero();
        if (hero.isWearableEquipped(wearable)) {
            hero.setDefensePower(hero.getDefensePower() - wearable.getDefenseBonus());
            hero.equipWearable(null);
        }
    }
}
