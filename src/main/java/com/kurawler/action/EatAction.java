package com.kurawler.action;

import com.kurawler.effect.EatEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Consumable;
import java.util.Collections;
import java.util.List;

public class EatAction implements Action {
    private final Consumable consumable;

    public EatAction(Consumable consumable) {
        this.consumable = consumable;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        if (!session.getHero().getInventory().hasItem(consumable)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new EatEffect(consumable));
    }
}