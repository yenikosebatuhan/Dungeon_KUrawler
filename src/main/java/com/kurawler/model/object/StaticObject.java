package com.kurawler.model.object;

public class StaticObject extends GameObject {
    private boolean isPassable;
    private boolean isSearchable;

    public StaticObject(String name, String description, boolean passable, boolean searchable) {
        super(name, description);
        this.isPassable = passable;
        this.isSearchable = searchable;
    }

    public boolean isPassable() { return isPassable; }
    public boolean isSearchable() { return isSearchable; }
}