package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;

public abstract class Enemy extends Character {

    /** Tile this enemy just left — avoids immediate step-back oscillation. */
    private int previousRow = -1;
    private int previousCol = -1;

    protected Enemy(String name, int maxHealth, int attackPower, int defensePower,
                    int strength, int maxMana, int maxEnergy) {
        super(name, maxHealth, attackPower, defensePower, strength, maxMana, maxEnergy);
    }

    public abstract void decideAction(Character target, Map map, GameSession session);

    @Override
    public void moveToCell(GridCell newCell) {
        if (newCell != null && getCurrentCell() != null) {
            previousRow = getRow();
            previousCol = getCol();
        }
        super.moveToCell(newCell);
    }

    public boolean wasAtPreviousTile(int row, int col) {
        return previousRow >= 0 && row == previousRow && col == previousCol;
    }
}
