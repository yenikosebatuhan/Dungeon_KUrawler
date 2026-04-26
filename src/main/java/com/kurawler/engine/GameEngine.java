package com.kurawler.engine;

import com.kurawler.action.Action;
import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;

/**
 * Facade that owns both the GameController (Action-Effect pipeline)
 * and the GameLoop (AI, spawning, projectiles).
 * UI creates a GameEngine, then calls start()/stop() and tick().
 */
public class GameEngine {

    private final GameController controller;
    private final GameLoop gameLoop;
    private boolean running;

    public GameEngine(GameController controller) {
        this.controller = controller;
        this.gameLoop = new GameLoop(controller.getSession());
        this.running = false;
    }

    /** Process a player action through the Action-Effect pipeline. */
    public void tick(Action playerAction) {
        if (playerAction != null) {
            controller.executeAction(playerAction);
        }
    }

    /** Called each frame by AnimationTimer to drive AI and spawning. */
    public void update(long deltaMs) {
        gameLoop.update(deltaMs);
    }

    public void start() {
        this.running = true;
        controller.getSession().setCurrentMode("Play");
    }

    public void stop() {
        this.running = false;
        gameLoop.stop();
    }

    public boolean isRunning() {
        return running;
    }

    public GameController getController() {
        return controller;
    }

    public GameLoop getGameLoop() {
        return gameLoop;
    }
}
