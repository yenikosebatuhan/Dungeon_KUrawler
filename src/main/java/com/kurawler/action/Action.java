package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import java.util.List;

/**
 * Action = what the user does.
 * Actions do NOT modify state directly.
 * Instead, they produce a list of Effects that modify state.
 *
 * This is the core of the Action-Effect separation pattern.
 */
public interface Action {

    /**
     * Resolve this action into a list of effects.
     * The action inspects the game session to determine what effects should occur,
     * but does NOT modify any state.
     *
     * @requires session != null
     * @modifies none (pure query — all state changes happen only via returned Effects)
     * @effects Returns a non-null list of Effect objects encoding the state
     *          transitions this action should produce; returns an empty list if
     *          the action cannot be performed in the current session state.
     */
    List<Effect> resolve(GameSession session);
}
