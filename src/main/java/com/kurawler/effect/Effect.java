package com.kurawler.effect;

import com.kurawler.model.GameSession;

/**
 * Effect = what happens as a result of an Action.
 * Effects are the ONLY things that modify the game state.
 */
public interface Effect {

    /** Execute this effect, modifying the game session state. */
    void apply(GameSession session);

    /** Human-readable description of what this effect does. */
    String describe();
}