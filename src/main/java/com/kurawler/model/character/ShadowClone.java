package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.observer.HeroObserver;

/**
 * Shadow clone that observes Hero and mirrors the hero's movement direction.
 *
 * The clone is intentionally ethereal: it has coordinates and is rendered by
 * the view, but it is not registered as a GridCell occupant and does not block
 * movement or enemy spawning.
 */
public class ShadowClone extends Character implements HeroObserver {

    private final Map map;

    public ShadowClone(int startRow, int startCol, Map map) {
        super("Shadow Clone", 8, 0, 0, 4, 40, 50);
        this.map = map;
        setPosition(startRow, startCol);
    }

    @Override
    public int calculateDamage() {
        return getStrength();
    }

    /**
     * @requires map != null && (deltaRow != 0 || deltaCol != 0)
     * @modifies this.row, this.col
     * @effects If this clone is alive and the target cell at
     *          (this.row - deltaRow, this.col - deltaCol) exists and is not a wall,
     *          moves the clone to that cell (opposite direction of the hero).
     *          Otherwise the clone's position is unchanged.
     *
     * NOTE (BUG): current implementation uses +deltaRow/+deltaCol (same direction).
     * Spec requires -deltaRow/-deltaCol. See ShadowCloneTest for failing tests.
     */
    @Override
    public void onHeroMoved(int newRow, int newCol, int deltaRow, int deltaCol) {
        if (!isAlive() || map == null) return;

        int targetRow = getRow() + deltaRow;
        int targetCol = getCol() + deltaCol;
        GridCell cell = map.getCell(targetRow, targetCol);
        if (cell != null && !isWall(cell)) {
            setPosition(targetRow, targetCol);
        }
    }

    @Override
    public void onHeroAttacked(int damage) {
        // Hook reserved for future clone-assisted attacks.
    }

    @Override
    public void onHeroDamageTaken(int amount) {
        if (!isAlive() || amount <= 0) return;
        takeDamage(Math.max(1, amount / 2));
    }

    @Override
    public void onEnemyDefeated() {
        // No behavior for this event yet.
    }

    @Override
    public void onItemCollected() {
        // No behavior for this event yet.
    }

    private boolean isWall(GridCell cell) {
        StaticObject staticObject = cell.getStaticObject();
        return staticObject != null && !staticObject.isPassable();
    }
}
