package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Scroll;

public class ReadScrollEffect implements Effect {
    private final Scroll scroll;

    public ReadScrollEffect(Scroll scroll) {
        this.scroll = scroll;
    }

    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || scroll == null || !hero.getInventory().removeItem(scroll)) {
            return;
        }
        if (scroll.getHealAmount() > 0) {
            hero.heal(scroll.getHealAmount());
        }
    }

    @Override
    public String describe() {
        return "Read " + (scroll != null ? scroll.getName() : "nothing");
    }
}
