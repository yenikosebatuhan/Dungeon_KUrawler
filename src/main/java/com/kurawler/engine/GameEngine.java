package com.kurawler.engine;

import com.kurawler.action.Action;
import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;

/**
 * Engine: Runs the game loop and delegates actions to the controller.
 * Responsibilities: game loop, movement coordination, combat coordination, enemy spawn stubs.
 */
public class GameEngine {

    private final GameController controller;
    private boolean running;

    public GameEngine(GameController controller) {
        this.controller = controller;
        this.running = false;
    }

    /** Simple game loop — processes one action per tick. */
    public void tick(Action playerAction) {
        if (playerAction != null) {
            controller.executeAction(playerAction);
        }
        // TODO: Process enemy AI actions
        // TODO: Update projectiles
        // TODO: Check win/lose conditions
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
