package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.SearchObjectEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;

public class SearchAction implements Action {
    private final StaticObject object;
    private final int row;
    private final int col;

    public SearchAction(StaticObject object, int row, int col) {
        this.object = object;
        this.row = row;
        this.col = col;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (object != null && object.isSearchable()) {
            effects.add(new SearchObjectEffect(object, row, col));
        }
        return effects;
    }
}
