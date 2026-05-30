package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import java.util.Collections;
import java.util.List;

public class DiscardAction implements Action {

    private final Item item;

    public DiscardAction(Item item) { this.item = item; }

    @Override
    public List<Effect> resolve(GameSession session) {
        return Collections.singletonList(new Effect() {
            @Override public void apply(GameSession s) {
                s.getHero().getInventory().removeItem(item);
            }
            @Override public String describe() { return "Discard " + item.getName(); }
        });
    }
}
