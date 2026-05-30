package com.kurawler.action;

import com.kurawler.effect.BreakObjectEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import java.util.ArrayList;
import java.util.List;

public class BreakAction implements Action {
    private final int row;
    private final int col;

    public BreakAction(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        GridCell cell = session.getMap().getCell(row, col);
        if (cell != null && cell.getStaticObject() != null
                && cell.getStaticObject().isSearchable()) {
            effects.add(new BreakObjectEffect(row, col));
        }
        return effects;
    }
}
