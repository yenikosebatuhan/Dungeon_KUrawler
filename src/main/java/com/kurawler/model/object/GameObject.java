package com.kurawler.model.object;

/**
 * Abstract base for all objects that can exist on the grid.
 */
public abstract class GameObject {

    private String name;
    private String description;
    private int rotation; // clockwise degrees, normalised to 0/90/180/270

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

    /** Clockwise rotation in degrees (0, 90, 180, 270). */
    public int getRotation() {
        return rotation;
    }

    public void setRotation(int degrees) {
        this.rotation = ((degrees % 360) + 360) % 360;
    }

    @Override
    public String toString() {
        return name;
    }
}
