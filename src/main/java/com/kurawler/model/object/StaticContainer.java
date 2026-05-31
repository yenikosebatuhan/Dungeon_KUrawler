package com.kurawler.model.object;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.ResourceAssetCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * A container fixed on the grid (e.g., chest, barrel).
 */
public class StaticContainer extends GameObject {

    private final List<Item> contents;
    private boolean isLocked = false;
    private String requiredKeyId = null;

    public StaticContainer(String name, String description) {
        super(name, description);
        this.contents = new ArrayList<>();
    }

    // --- LOCK MECHANISM ---
    public void setLock(String keyId) {
        this.requiredKeyId = keyId;
        this.isLocked = true;
    }

    public boolean isLocked() { return isLocked; }
    public String getRequiredKeyId() { return requiredKeyId; }
    public void unlock() { this.isLocked = false; }

    // --- INVENTORY MANAGEMENT ---
    public void addItem(Item item) { contents.add(item); }

    public Item removeItem(int index) {
        if (index < 0 || index >= contents.size()) return null;
        return contents.remove(index);
    }

    public boolean removeItem(Item item) { return contents.remove(item); }

    public List<Item> getContents() { return new ArrayList<>(contents); }

    /** Live list for in-game take-from-container mutations. */
    public List<Item> getContentsMutable() { return contents; }

    public boolean isEmpty() { return contents.isEmpty(); }

    /** Lootable chests/crates disappear from the map once fully emptied. */
    public boolean shouldDisplayOnMap() {
        return isShelfLike() || !isEmpty();
    }

    /** Removes this container from the grid when it has been looted (not bookshelves). */
    public void removeFromMapIfLooted(Map map) {
        if (map == null || isShelfLike() || !isEmpty()) {
            return;
        }
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.removeObject(this)) {
                    return;
                }
            }
        }
    }

    /** Bookshelves use the static prop sprite on the tile; no chest overlay. */
    public boolean isShelfLike() {
        String n = getName().toLowerCase();
        return n.contains("bookshelf") || n.contains("shelf");
    }

    /** Sprite hint for renderer: empty chest uses open sprite. */
    public String getSpriteName() {
        if (isShelfLike()) {
            return getName();
        }
        return ResourceAssetCatalog.containerSpriteName(getName(), isEmpty());
    }
}
