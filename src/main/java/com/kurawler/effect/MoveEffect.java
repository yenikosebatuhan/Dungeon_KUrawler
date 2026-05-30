package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Character;

/**
 * Effect that moves a character to a new cell, keeping position,
 * currentCell, and the grid's occupant reference in sync via moveToCell().
 */
public class MoveEffect implements Effect {

    private final Character character;
    private final int newRow;
    private final int newCol;

    /**
     * @requires character != null && newRow >= 0 && newCol >= 0
     * @modifies this.character, this.newRow, this.newCol
     * @effects Stores the character and destination coordinates for use in apply().
     */
    public MoveEffect(Character character, int newRow, int newCol) {
        this.character = character;
        this.newRow = newRow;
        this.newCol = newCol;
    }

    /**
     * @requires session != null && character != null && session.getMap() != null
     * @modifies character.row, character.col, character.currentCell,
     *           old cell occupant, new cell occupant, character.energy
     * @effects Moves character to (newRow, newCol) via moveToCell() if the target
     *          cell exists, or setPosition() otherwise. Deducts 1 energy unit.
     */
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

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the character and destination cell.
     */
    @Override
    public String describe() {
        return "Move " + character.getName() + " to (" + newRow + "," + newCol + ")";
    }
}
