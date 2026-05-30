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

    /**
     * @requires controller != null
     * @modifies this.controller, this.gameLoop, this.running
     * @effects Creates a GameLoop bound to the controller's session; sets running to false.
     */
    public GameEngine(GameController controller) {
        this.controller = controller;
        this.gameLoop = new GameLoop(controller.getSession());
        this.running = false;
    }

    /**
     * Process a player action through the Action-Effect pipeline.
     *
     * @requires none
     * @modifies session state (via action effects)
     * @effects Executes playerAction through the controller if non-null.
     */
    public void tick(Action playerAction) {
        if (playerAction != null) {
            controller.executeAction(playerAction);
        }
    }

    /**
     * Called each frame by AnimationTimer to drive AI and spawning.
     *
     * @requires deltaMs >= 0
     * @modifies session state (via gameLoop.update)
     * @effects Delegates one frame of enemy AI, projectile movement, and spawning
     *          to the GameLoop.
     */
    public void update(long deltaMs) {
        gameLoop.update(deltaMs);
    }

    /**
     * @requires none
     * @modifies this.running, session.currentMode
     * @effects Sets running to true and session mode to "Play".
     */
    public void start() {
        this.running = true;
        controller.getSession().setCurrentMode("Play");
    }

    /**
     * @requires none
     * @modifies this.running, gameLoop.isRunning
     * @effects Sets running to false and signals the GameLoop to stop.
     */
    public void stop() {
        this.running = false;
        gameLoop.stop();
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true if the engine is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the GameController used by this engine.
     */
    public GameController getController() {
        return controller;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the GameLoop driven by this engine.
     */
    public GameLoop getGameLoop() {
        return gameLoop;
    }
}
