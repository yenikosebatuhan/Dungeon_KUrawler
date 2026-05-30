package com.kurawler.effect;

import com.kurawler.model.GameSession;

/**
 * Effect = what happens as a result of an Action.
 * Effects are the ONLY things that modify the game state.
 */
public interface Effect {

    /**
     * Execute this effect, modifying the game session state.
     *
     * @requires session != null
     * @modifies session (specific fields depend on the concrete implementation)
     * @effects Applies this effect to the game session, mutating the relevant state.
     */
    void apply(GameSession session);

    /**
     * Human-readable description of what this effect does.
     *
     * @requires none
     * @modifies none
     * @effects Returns a non-null string describing this effect.
     */
    String describe();
}