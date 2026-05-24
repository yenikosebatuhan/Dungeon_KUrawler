package com.kurawler.model.object;

public class StaticObject extends GameObject {
    private boolean isPassable;
    private boolean isSearchable;
    private Item hiddenItem;
    private boolean searched = false;

    public StaticObject(String name, String description, boolean passable, boolean searchable) {
        super(name, description);
        this.isPassable = passable;
        this.isSearchable = searchable;
    }

    public boolean isPassable() { return isPassable; }
    public boolean isSearchable() { return isSearchable && !searched; }

    public Item getHiddenItem() { return hiddenItem; }
    public void setHiddenItem(Item item) { this.hiddenItem = item; }

    public Item revealHiddenItem() {
        searched = true;
        Item item = hiddenItem;
        hiddenItem = null;
        return item;
    }
}