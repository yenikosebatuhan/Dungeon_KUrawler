package com.kurawler.model;

import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single cell in the game's grid-based map.
 * Each cell can contain a static object, multiple game objects (items/projectiles),
 * and at most one character.
 */
public class GridCell {
    private final int xCoordinate;
    private final int yCoordinate;
    private StaticObject staticObject; // 0..1 relationship
    private List<GameObject> gameObjects; // 0..* relationship
    private Character occupyingCharacter; // 0..1 relationship
    
    /**
     * Constructs a GridCell at the specified coordinates.
     * @param x The x-coordinate of this cell
     * @param y The y-coordinate of this cell
     */
    public GridCell(int x, int y) {
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.gameObjects = new ArrayList<>();
        this.staticObject = null;
        this.occupyingCharacter = null;
    }
    
    /**
     * Gets the x-coordinate of this cell.
     * @return The x-coordinate
     */
    public int getX() {
        return xCoordinate;
    }
    
    /**
     * Gets the y-coordinate of this cell.
     * @return The y-coordinate
     */
    public int getY() {
        return yCoordinate;
    }
    
    /**
     * Gets the static object in this cell, if any.
     * @return The StaticObject, or null if none exists
     */
    public StaticObject getStaticObject() {
        return staticObject;
    }
    public boolean isWalkable() {
        // Walkable is just the opposite of blocked
        return !isBlocked();
    }
    
    public int getRow() {
        return getY();
    }
    
    public int getCol() {
        return getX();
    }
    
    /**
     * Sets or removes the static object in this cell.
     * @param obj The StaticObject to place, or null to remove
     */
    public void setStaticObject(StaticObject obj) {
        this.staticObject = obj;
    }
    
    /**
     * Adds a game object (item, projectile, etc.) to this cell.
     * @param obj The GameObject to add
     * @throws IllegalArgumentException if obj is null
     */
    public void addGameObject(GameObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot add null GameObject to cell");
        }
        this.gameObjects.add(obj);
    }
    
    /**
     * Removes a game object from this cell.
     * @param obj The GameObject to remove
     * @return true if the object was removed, false if it wasn't present
     */
    public boolean removeGameObject(GameObject obj) {
        return this.gameObjects.remove(obj);
    }
    
    /**
     * Gets all game objects in this cell.
     * @return List of GameObjects (may be empty but never null)
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }
    
    /**
     * Gets the character occupying this cell, if any.
     * @return The Character, or null if none exists
     */
    public Character getOccupyingCharacter() {
        return occupyingCharacter;
    }
    
    /**
     * Sets or removes the character occupying this cell.
     * @param character The Character to place, or null to remove
     */
    public void setOccupyingCharacter(Character character) {
        this.occupyingCharacter = character;
    }
    
    /**
     * Checks if this cell blocks character movement.
     * A cell is blocked if it contains an impassable static object
     * or is already occupied by a character.
     * @return true if blocked, false if passable
     */
    public boolean isBlocked() {
        // Blocked if there's an impassable static object
        if (staticObject != null && !staticObject.isPassable()) {
            return true;
        }
        // Blocked if a character is already occupying this cell
        if (occupyingCharacter != null) {
            return true;
        }
        return false;
    }
    
    /**
     * Checks if this cell is completely empty (no objects or characters).
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return staticObject == null && 
               occupyingCharacter == null && 
               gameObjects.isEmpty();
    }
    
    @Override
    public String toString() {
        return "GridCell{pos=(" + xCoordinate + "," + yCoordinate + 
               "), static=" + (staticObject != null ? staticObject.getName() : "none") +
               ", char=" + (occupyingCharacter != null ? "occupied" : "empty") +
               ", objects=" + gameObjects.size() + "}";
    }
}