package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;

/**
 * A temporary clone of the Hero that moves in the opposite direction.
 * Enemies treat it as the real Hero. Expires after {@link #LIFETIME_MS} ms.
 *
 * Spec (Phase II – A7):
 *   - Created when the player READs a ShadowCloneScroll.
 *   - Spawns adjacent to the hero.
 *   - Each time the hero moves, the clone moves one step in the reverse direction.
 *   - Disappears after 7 seconds.
 */
public class ShadowClone extends Character {

    public static final long LIFETIME_MS = 7_000L;

    private long remainingMs;

    public ShadowClone(Hero hero) {
        super("ShadowClone", hero.getMaxHealth(), 0, 0, 0, 0, 0);
        this.remainingMs = LIFETIME_MS;
    }

    /**
     * Moves the clone one step in the direction opposite to the hero's last move.
     * Silently stays put if the reversed target is out of bounds or blocked.
     *
     * @param heroDirection the direction the hero just moved ("UP","DOWN","LEFT","RIGHT")
     * @param map           the current game map
     */
    public void moveReverse(String heroDirection, Map map) {
        if (heroDirection == null || map == null) return;
        int dRow = 0, dCol = 0;
        switch (heroDirection.toUpperCase()) {
            case "UP":    dRow =  1; break;
            case "DOWN":  dRow = -1; break;
            case "LEFT":  dCol =  1; break;
            case "RIGHT": dCol = -1; break;
            default: return;
        }
        int targetRow = getRow() + dRow;
        int targetCol = getCol() + dCol;
        GridCell target = map.getCell(targetRow, targetCol);
        if (target != null && !target.isBlocked()) {
            moveToCell(target);
        }
    }

    /**
     * Advances the clone's lifetime countdown.
     *
     * @param deltaMs milliseconds elapsed since the last game update
     */
    public void tick(long deltaMs) {
        remainingMs -= deltaMs;
    }

    /** Returns true once the 7-second lifetime has elapsed. */
    public boolean isExpired() {
        return remainingMs <= 0;
    }

    /** Removes the clone from the map grid (clears its occupied cell). */
    public void despawn() {
        moveToCell(null);
    }

    public long getRemainingMs() { return remainingMs; }
}
