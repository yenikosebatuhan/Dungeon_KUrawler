package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.WearEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.List;

public class WearAction implements Action {
    private final Wearable wearable;

    public WearAction(Wearable wearable) {
        this.wearable = wearable;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (wearable != null && session.getHero().getInventory().hasItem(wearable)) {
            effects.add(new WearEffect(wearable));
        }
        return effects;
    }
}
