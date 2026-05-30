package com.kurawler.model.object;

import java.util.ArrayList;
import java.util.List;

/**
 * A container fixed on the grid (e.g., chest, barrel).
 */
public class StaticContainer extends GameObject {

    private final List<Item> contents;
    private boolean isLocked = false;
    private String requiredKeyId = null;

    public StaticContainer(String name, int capacity) {
        this(name, name);
    }

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

    /** Sprite hint for renderer: empty chest uses open sprite. */
    public String getSpriteName() {
        return isEmpty() ? getName() + " open" : getName();
    }

    public boolean removeItem(Item item) {
        return contents.remove(item);
    }

    // Lock support
    private boolean locked;
    private String requiredKeyId;

    public boolean isLocked() { return locked; }
    public String getRequiredKeyId() { return requiredKeyId; }
    public void lock(String keyId) { this.locked = true; this.requiredKeyId = keyId; }
    public void unlock() { this.locked = false; }
}
