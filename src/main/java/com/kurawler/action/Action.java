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
     */
    List<Effect> resolve(GameSession session);
}
