package com.kurawler.model.character;

import com.kurawler.engine.ShadowCloneScrollRules;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;

public abstract class Enemy extends Character {

    /** Tile this enemy just left — never step back immediately (prevents A↔B oscillation). */
    private int previousRow = -1;
    private int previousCol = -1;

    protected Enemy(String name, int maxHealth, int attackPower, int defensePower,
                    int strength, int maxMana, int maxEnergy) {
        super(name, maxHealth, attackPower, defensePower, strength, maxMana, maxEnergy);
    }

    public abstract void decideAction(Character target, Map map, GameSession session);

    /** PDF §17: enemies must not step onto the ethereal shadow clone tile. */
    protected boolean canEnemyEnterCell(int row, int col, GameSession session) {
        return session == null || !session.isActiveShadowCloneAt(row, col);
    }

    protected boolean tryEnemyStep(int dRow, int dCol, Map map, GameSession session) {
        if (dRow == 0 && dCol == 0) {
            return false;
        }
        int nr = getRow() + dRow;
        int nc = getCol() + dCol;
        GridCell cell = map.getCell(nr, nc);
        if (cell == null || cell.isBlocked() || !canEnemyEnterCell(nr, nc, session)) {
            return false;
        }
        moveToCell(cell);
        return true;
    }

    @Override
    public void moveToCell(GridCell newCell) {
        if (newCell != null && getCurrentCell() != null) {
            previousRow = getRow();
            previousCol = getCol();
        }
        super.moveToCell(newCell);
        if (newCell != null) {
            ShadowCloneScrollRules.removeFromCell(newCell);
        }
    }

    protected boolean isPreviousTile(int row, int col) {
        return previousRow >= 0 && row == previousRow && col == previousCol;
    }

    public boolean wasAtPreviousTile(int row, int col) {
        return isPreviousTile(row, col);
    }

    protected int getPreviousRow() { return previousRow; }
    protected int getPreviousCol() { return previousCol; }
}
