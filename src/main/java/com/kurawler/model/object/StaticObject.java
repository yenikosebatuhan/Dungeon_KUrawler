package com.kurawler.model.object;

public class StaticObject extends GameObject {
    private boolean isPassable;
    private boolean isSearchable;
    private Item hiddenItem;

    public StaticObject(String name, String description, boolean passable, boolean searchable) {
        super(name, description);
        this.isPassable = passable;
        this.isSearchable = searchable;
    }

    public boolean isPassable() { return isPassable; }
    public boolean isSearchable() { return isSearchable; }

    public Item getHiddenItem() { return hiddenItem; }
    public void setHiddenItem(Item item) { this.hiddenItem = item; }

    public Item revealHiddenItem() {
        Item item = hiddenItem;
        hiddenItem = null;
        return item;
    }
}