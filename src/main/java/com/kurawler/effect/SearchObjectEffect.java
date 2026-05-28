package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticObject;

public class SearchObjectEffect implements Effect {
    private final StaticObject object;

    public SearchObjectEffect(StaticObject object) {
        this.object = object;
    }

    @Override
    public void apply(GameSession session) {
        if (object == null || !object.isSearchable()) {
            return;
        }

        Item found = object.revealHiddenItem();
        object.setSearchable(false);

        if (found == null) {
            return;
        }

        boolean added = session.getHero().getInventory().addItem(found);
        if (added) {
            session.getHero().notifyItemCollected();
        } else {
            object.setHiddenItem(found);
        }
    }

    @Override
    public String describe() {
        return "Search " + (object != null ? object.getName() : "nothing");
    }
}
