package com.kurawler.model.object;

import com.kurawler.model.character.Hero;

/**
 * Door / stairs linking to another map in the same {@link com.kurawler.model.GameSession}.
 */
public class MapPortal extends StaticObject {

    private final String targetMapId;
    private final int targetRow;
    private final int targetCol;
    private final String requiredKeyId;
    private final boolean stairsMode;
    private boolean unlocked;

    /** Backwards-compatible constructor (door / portal, not stairs). */
    public MapPortal(String name, String description, String targetMapId,
                     int targetRow, int targetCol, String requiredKeyId, boolean startsOpen) {
        this(name, description, targetMapId, targetRow, targetCol, requiredKeyId, startsOpen, false);
    }

    /** Full constructor. {@code stairsMode=true} makes this portal searchable (stairs). */
    public MapPortal(String name, String description, String targetMapId,
                     int targetRow, int targetCol, String requiredKeyId,
                     boolean startsOpen, boolean stairsMode) {
        super(name, description, stairsMode || startsOpen, stairsMode);
        this.targetMapId = targetMapId;
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        this.requiredKeyId = requiredKeyId;
        this.unlocked = startsOpen;
        this.stairsMode = stairsMode;
    }

    public String getTargetMapId() { return targetMapId; }
    public int getTargetRow() { return targetRow; }
    public int getTargetCol() { return targetCol; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isStairsMode() { return stairsMode; }

    public String getRequiredKeyId() { return requiredKeyId; }

    @Override
    public boolean isSearchable() {
        return stairsMode;
    }

    public boolean tryUnlock(Hero hero) {
        if (unlocked) {
            return true;
        }
        if (requiredKeyId == null) {
            openPortal();
            return true;
        }
        if (hero == null || hero.getInventory() == null) {
            return false;
        }
        for (Item item : hero.getInventory().getItems()) {
            if (item instanceof Key && requiredKeyId.equals(((Key) item).getKeyId())) {
                openPortal();
                return true;
            }
        }
        return false;
    }

    public void openPortal() {
        unlocked = true;
        setPassable(true);
    }

    public String getLockHint() {
        if (unlocked || requiredKeyId == null) {
            return "";
        }
        if ("gold".equals(requiredKeyId)) {
            return "Locked. Find the Golden Key (press E on a bookshelf in Entry Hall or Catacombs).";
        }
        return "Locked — you need the right key.";
    }
}
