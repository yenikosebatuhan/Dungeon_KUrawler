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
    private boolean unlocked;

    public MapPortal(String name, String description, String targetMapId,
                     int targetRow, int targetCol, String requiredKeyId, boolean startsOpen) {
        super(name, description, startsOpen, false);
        this.targetMapId = targetMapId;
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        this.requiredKeyId = requiredKeyId;
        this.unlocked = startsOpen;
    }

    public String getTargetMapId() { return targetMapId; }
    public int getTargetRow() { return targetRow; }
    public int getTargetCol() { return targetCol; }
    public boolean isUnlocked() { return unlocked; }

    /** Doors are never searchable — only ENTER / walk through. */
    @Override
    public boolean isSearchable() {
        return false;
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
            return "Locked. Find the Golden Key (SEARCH a shelf in Entry Hall or Catacombs).";
        }
        return "Locked — you need the right key.";
    }
}
