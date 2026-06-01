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
    /** Breakable chests/crates must be smashed open before loot can be taken. */
    private boolean broken = false;

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

    /**
     * Looting a container no longer removes it from the map: an emptied chest stays
     * in place (rendered with its open/empty sprite). Only a successful BREAK removes
     * a container (see {@link com.kurawler.effect.BreakObjectEffect}).
     */
    public void removeFromMapIfLooted(Map map) {
        // Intentionally a no-op — chests remain after being opened and emptied.
    }

    /** Bookshelves use the static prop sprite on the tile; no chest overlay. */
    public boolean isShelfLike() {
        String n = getName().toLowerCase();
        return n.contains("bookshelf") || n.contains("shelf");
    }

    /** Crates/chests/barrels can be broken; bookshelves are opened/searched only. */
    public boolean isBreakable() {
        return !isShelfLike();
    }

    /** Any container on a tile blocks movement until removed from the cell. */
    public boolean blocksMovement() {
        return true;
    }

    public boolean isBroken() {
        return broken;
    }

    /** Bookshelves open freely; chests/crates need a successful BREAK first. */
    public boolean isOpened() {
        return isShelfLike() || broken;
    }

    public void markBroken() {
        this.broken = true;
    }

    /** Sprite hint for renderer: empty chest uses open sprite. */
    public String getSpriteName() {
        if (isShelfLike()) {
            return getName();
        }
        return ResourceAssetCatalog.containerSpriteName(getName(), isEmpty() || isBroken());
    }
}
