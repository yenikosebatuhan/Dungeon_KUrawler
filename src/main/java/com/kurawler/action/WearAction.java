package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.WearEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.List;

public class WearAction implements Action {
    private final Wearable wearable;

    /**
     * @requires wearable != null
     * @modifies this.wearable
     * @effects Stores the wearable item for use in resolve().
     */
    public WearAction(Wearable wearable) {
        this.wearable = wearable;
    }

    /**
     * @requires session != null && session.getHero() != null
     * @modifies none
     * @effects Returns a singleton list containing a WearEffect if wearable is
     *          in the hero's inventory; otherwise returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (wearable != null && session.getHero().getInventory().hasItem(wearable)) {
            effects.add(new WearEffect(wearable));
        }
        return effects;
    }
}
