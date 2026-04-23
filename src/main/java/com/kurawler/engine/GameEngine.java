package com.kurawler.engine;

import com.kurawler.action.Action;
import com.kurawler.ai.EnemyAI;
import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Enemy;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine: Runs the game loop and delegates actions to the controller.
 * Responsibilities: game loop, movement coordination, combat coordination, enemy spawn stubs.
 */
public class GameEngine {

    private final GameController controller;
    private final EnemyAI enemyAI;
    private boolean running;

    public GameEngine(GameController controller) {
        this.controller = controller;
        this.enemyAI = new EnemyAI();
        this.running = false;
    }

    /**
     * One game tick: process player action, then each enemy's AI turn.
     * Matches UC-5 sequence diagram flow:
     *   Engine → EnemyAI.decideAction() → Controller.executeAction()
     */
    public void tick(Action playerAction) {
        if (playerAction != null) {
            controller.executeAction(playerAction);
        }
        processEnemyTurns();
    }

    /**
     * Iterates over all living enemies and lets the AI decide their action.
     * Each action is executed through the same Controller pipeline as player actions.
     */
    private void processEnemyTurns() {
        GameSession session = controller.getSession();
        List<Enemy> snapshot = new ArrayList<>(session.getEnemies());

        for (Enemy enemy : snapshot) {
            if (!enemy.isAlive()) continue;

            Action aiAction = enemyAI.decideAction(enemy, session);
            if (aiAction != null) {
                controller.executeAction(aiAction);
            }
        }
    }

    public void start() {
        this.running = true;
        System.out.println("[Engine] Game started.");
    }

    public void stop() {
        this.running = false;
        System.out.println("[Engine] Game stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public GameController getController() {
        return controller;
    }
}
