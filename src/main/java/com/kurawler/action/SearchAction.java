package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.effect.SearchObjectEffect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;

public class SearchAction implements Action {
    private final StaticObject object;

    /**
     * @requires object != null
     * @modifies this.object
     * @effects Stores the static object to search for use in resolve().
     */
    public SearchAction(StaticObject object) {
        this.object = object;
    }

    /**
     * @requires session != null
     * @modifies none
     * @effects Returns a singleton list containing a SearchObjectEffect if
     *          object is non-null and searchable; otherwise returns an empty list.
     */
    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        if (object != null && object.isSearchable()) {
            effects.add(new SearchObjectEffect(object));
        }
        return effects;
    }
}
