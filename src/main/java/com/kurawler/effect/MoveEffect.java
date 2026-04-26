package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Character;

/**
 * Effect that moves a character to a new position.
 */
public class MoveEffect implements Effect {

    private final Character character;
    private final int newRow;
    private final int newCol;

    public MoveEffect(Character character, int newRow, int newCol) {
        this.character = character;
        this.newRow = newRow;
        this.newCol = newCol;
    }

    @Override
    public void apply(GameSession session) {
        GridCell targetCell = session.getMap().getCell(newRow, newCol);
        if (targetCell != null) {
            character.moveToCell(targetCell);
        } else {
            character.setPosition(newRow, newCol);
        }
        character.useEnergy(1);
        System.out.println("[Effect] " + character.getName() + " moved to (" + newRow + "," + newCol
                + ") | Energy: " + character.getEnergy() + "/" + character.getMaxEnergy());
    }

    @Override
    public String describe() {
        return "Move " + character.getName() + " to (" + newRow + "," + newCol + ")";
    }
}
