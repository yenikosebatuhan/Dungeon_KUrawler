package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.TakeFromContainerEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero transfers an Item from a Container into their Inventory.
 */
public class TakeFromContainerAction implements Action {

    private final Object container;
    private final Item item;

    public TakeFromContainerAction(Object container, Item item) {
        this.container = container;
        this.item = item;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || hero.getInventory().isFull()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new TakeFromContainerEffect(container, item));
    }
}
