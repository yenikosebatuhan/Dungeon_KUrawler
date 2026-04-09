package com.kurawler.model.object;

/**
 * Abstract base for all objects that can exist on the grid.
 */
public abstract class GameObject {

    private String name;
    private String description;

    protected GameObject(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
