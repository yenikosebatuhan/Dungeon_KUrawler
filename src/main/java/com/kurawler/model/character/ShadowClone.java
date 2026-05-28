package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.observer.HeroObserver;

/**
 * Shadow clone that observes the Hero and mirrors its movement in reverse.
 * Enemies treat it as the real Hero. Expires after {@link #LIFETIME_MS} ms.
 *
 * The clone is intentionally ethereal: it has coordinates and is rendered by
 * the view, but it is not registered as a GridCell occupant and does not block
 * movement or enemy spawning.
 */
public class ShadowClone extends Character implements HeroObserver {

    public static final long LIFETIME_MS = 7_000L;

    private final Map map;
    private long remainingMs;

    public ShadowClone(int startRow, int startCol, Map map) {
        super("Shadow Clone", 8, 0, 0, 4, 40, 50);
        this.map = map;
        this.remainingMs = LIFETIME_MS;
        setPosition(startRow, startCol);
    }

    @Override
    public int calculateDamage() {
        return getStrength();
    }

    /**
     * @requires map != null && (deltaRow != 0 || deltaCol != 0)
     * @modifies this.row, this.col
     * @effects Moves the clone one step in the direction opposite to the hero
     * (i.e., -deltaRow, -deltaCol). If the target cell is out of bounds
     * or a wall, the clone's position is unchanged.
     */
    @Override
    public void onHeroMoved(int newRow, int newCol, int deltaRow, int deltaCol) {
        if (!isAlive() || map == null) return;

        // --- BUG-01 FIX: Move in reverse direction ---
        // As per PBI-07: must move clone to (row - deltaRow, col - deltaCol)
        int targetRow = getRow() - deltaRow;
        int targetCol = getCol() - deltaCol;
        
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
        // --- BUG-02 FIX: Clone should be invulnerable ---
        // Do absolutely nothing. The clone must not take damage when the hero takes damage.
    }

    @Override
    public void onEnemyDefeated() {
        // No behavior for this event yet.
    }

    @Override
    public void onItemCollected() {
        // No behavior for this event yet.
    }

    /**
     * Advances the clone's lifetime countdown.
     *
     * @param deltaMs milliseconds elapsed since the last game update
     */
    public void tick(long deltaMs) {
        remainingMs -= deltaMs;
        // Expired clone logic is handled in the GameLoop (it sets clone to null)
    }

    /** Returns true once the 7-second lifetime has elapsed. */
    public boolean isExpired() {
        return remainingMs <= 0;
    }

    public long getRemainingMs() { return remainingMs; }

    private boolean isWall(GridCell cell) {
        StaticObject obj = cell.getStaticObject();
        return obj != null && !obj.isPassable();
    }
}