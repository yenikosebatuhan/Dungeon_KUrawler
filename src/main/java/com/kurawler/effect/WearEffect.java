package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Wearable;

public class WearEffect implements Effect {
    private final Wearable wearable;

    public WearEffect(Wearable wearable) {
        this.wearable = wearable;
    }

    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero != null && wearable != null && hero.getInventory().hasItem(wearable)) {
            hero.equipWearable(wearable);
        }
    }

    @Override
    public String describe() {
        return "Wear " + (wearable != null ? wearable.getName() : "nothing");
    }
}
