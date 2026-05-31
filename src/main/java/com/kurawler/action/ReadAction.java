package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.ReadScrollEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Scroll;
import java.util.ArrayList;
import java.util.List;

/**
 * Action: Hero reads a scroll from the inventory.
 */
public class ReadAction implements Action {
    private final Scroll scroll;

    /**
     * @requires scroll != null
     * @modifies this.scroll
     * @effects Stores the scroll to be read for use in resolve().
     */
    public ReadAction(Scroll scroll) {
        this.scroll = scroll;
    }

    /**
     * Resolves the read action into a list of effects.
     *
     * @requires session != null && session.getHero() != null
     * @modifies none (creates effect objects only)
     * @effects If the scroll is not null and exists in the hero's inventory,
     * returns a list containing a new ReadScrollEffect.
     * Otherwise, returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        Scroll resolved = resolveScrollInInventory(session, scroll);
        if (resolved != null) {
            effects.add(new ReadScrollEffect(resolved));
        }
        return effects;
    }

    private static Scroll resolveScrollInInventory(GameSession session, Scroll scroll) {
        if (session == null || session.getHero() == null || scroll == null) {
            return null;
        }
        if (session.getHero().getInventory().hasItem(scroll)) {
            return scroll;
        }
        String name = scroll.getName();
        for (Item item : session.getHero().getInventory().getItems()) {
            if (item instanceof Scroll && item.getName().equals(name)) {
                return (Scroll) item;
            }
        }
        return null;
    }
}