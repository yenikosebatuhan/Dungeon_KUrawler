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
    private Runnable onMove;

    /**
     * @requires map != null && startRow >= 0 && startCol >= 0
     * @modifies this (via Character constructor), this.map, this.remainingMs
     * @effects Creates a ShadowClone at (startRow, startCol) with LIFETIME_MS lifetime.
     */
    public ShadowClone(int startRow, int startCol, Map map) {
        super("Shadow Clone", 8, 0, 0, 4, 40, 50);
        this.map = map;
        this.remainingMs = LIFETIME_MS;
        setPosition(startRow, startCol);
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns this.strength (clone does not use weapon damage).
     */
    @Override
    public int calculateDamage() {
        return getStrength();
    }

    /**
     * @requires map != null && (deltaRow != 0 || deltaCol != 0)
     * @modifies this.row, this.col
     * @effects Moves the clone one step in the direction opposite to the hero
     *          (-deltaRow, -deltaCol). If the target cell is out of bounds or a
     *          wall, the clone's position is unchanged.
     */
    @Override
    public void onHeroMoved(int newRow, int newCol, int deltaRow, int deltaCol) {
        if (!isAlive() || map == null) return;

        int targetRow = getRow() - deltaRow;
        int targetCol = getCol() - deltaCol;

        GridCell cell = map.getCell(targetRow, targetCol);
        if (cell != null && !isWall(cell)) {
            setPosition(targetRow, targetCol);
            if (onMove != null) {
                onMove.run();
            }
        }
    }

    /**
     * @requires none
     * @modifies this.onMove
     * @effects Sets the callback invoked after each successful clone movement.
     */
    public void setOnMove(Runnable onMove) {
        this.onMove = onMove;
    }

    /**
     * @requires none
     * @modifies none
     * @effects No-op — reserved for future clone-assisted attack behavior.
     */
    @Override
    public void onHeroAttacked(int damage) {
    }

    /**
     * @requires none
     * @modifies none
     * @effects No-op — the clone is invulnerable and does not react to hero damage.
     */
    @Override
    public void onHeroDamageTaken(int amount) {
    }

    /**
     * @requires none
     * @modifies none
     * @effects No-op — the clone has no behavior on enemy defeat.
     */
    @Override
    public void onEnemyDefeated() {
    }

    /**
     * @requires none
     * @modifies none
     * @effects No-op — the clone has no behavior on item collection.
     */
    @Override
    public void onItemCollected() {
    }

    /**
     * Advances the clone's lifetime countdown.
     *
     * @requires deltaMs >= 0
     * @modifies this.remainingMs
     * @effects Decrements remainingMs by deltaMs. The GameLoop sets the clone
     *          reference to null when isExpired() returns true.
     */
    public void tick(long deltaMs) {
        if (deltaMs > 0) {
            remainingMs -= deltaMs;
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true once the 7-second lifetime has elapsed.
     */
    public boolean isExpired() {
        return remainingMs <= 0;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the remaining lifetime in milliseconds.
     */
    public long getRemainingMs() { return remainingMs; }

    /** Restores lifetime after load (persistence only). */
    public void setRemainingMs(long remainingMs) {
        this.remainingMs = Math.max(0, remainingMs);
    }

    private boolean isWall(GridCell cell) {
        StaticObject obj = cell.getStaticObject();
        return obj != null && !obj.isPassable();
    }
}
