package com.kurawler.controller;

import com.kurawler.action.Action;
import com.kurawler.action.AttackAction;
import com.kurawler.action.EatAction;
import com.kurawler.action.MoveAction;
import com.kurawler.action.TakeAction;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
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

    public GameController(GameSession session) {
        this.session = session;
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
     * Called by the UI when the player attacks an adjacent enemy.
     * Finds the enemy at the target cell and creates an AttackAction.
     */
    public boolean attackEnemy(int deltaRow, int deltaCol) {
        Hero hero = session.getHero();
        int targetRow = hero.getRow() + deltaRow;
        int targetCol = hero.getCol() + deltaCol;

        Enemy enemy = session.getEnemyAt(targetRow, targetCol);
        if (enemy == null || !enemy.isAlive()) {
            return false;
        }

        AttackAction attack = new AttackAction(hero, enemy);
        executeAction(attack);

        if (!enemy.isAlive()) {
            session.removeEnemy(enemy);
        }
        return true;
    }

    /**
     * Called by the UI when the player uses a consumable from inventory.
     * Creates an EatAction that heals the hero.
     */
    public boolean useItem(Consumable consumable) {
        if (consumable == null) {
            return false;
        }
        EatAction eat = new EatAction(consumable);
        executeAction(eat);
        return true;
    }

    public GameSession getSession() {
        return session;
    }
}
