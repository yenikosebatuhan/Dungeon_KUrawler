package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.SearchObjectEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;

public class SearchAction implements Action {
    private final StaticObject object;

    public SearchAction(StaticObject object) {
        this.object = object;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (object != null && object.isSearchable()) {
            effects.add(new SearchObjectEffect(object));
        }
        return effects;
    }
}
