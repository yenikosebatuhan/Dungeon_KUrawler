package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.ReadScrollEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Scroll;
import java.util.ArrayList;
import java.util.List;

public class ReadAction implements Action {
    private final Scroll scroll;

    public ReadAction(Scroll scroll) {
        this.scroll = scroll;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (scroll != null && session.getHero().getInventory().hasItem(scroll)) {
            effects.add(new ReadScrollEffect(scroll));
        }
        return effects;
    }
}
