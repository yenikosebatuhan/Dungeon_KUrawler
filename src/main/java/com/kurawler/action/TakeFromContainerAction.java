package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.StaticContainer;
import java.util.Collections;
import java.util.List;

public class TakeFromContainerAction implements Action {

    private final Object container;
    private final Item item;

    public TakeFromContainerAction(Object container, Item item) {
        this.container = container;
        this.item = item;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        return Collections.singletonList(new Effect() {
            @Override public void apply(GameSession s) {
                boolean removed = false;
                if (container instanceof StaticContainer)
                    removed = ((StaticContainer) container).removeItem(item);
                else if (container instanceof PortableContainer)
                    removed = ((PortableContainer) container).removeItem(item);
                if (removed) s.getHero().getInventory().addItem(item);
            }
            @Override public String describe() { return "Take " + item.getName(); }
        });
    }
}
