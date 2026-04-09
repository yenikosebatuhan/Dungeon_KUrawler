package com.kurawler.model.object;

/**
 * A moving object on the grid (e.g., arrow, fireball).
 */
public class Projectile extends GameObject {

    private int damage;
    private int directionRow; // -1, 0, or 1
    private int directionCol; // -1, 0, or 1

    public Projectile(String name, String description, int damage, int dirRow, int dirCol) {
        super(name, description);
        this.damage = damage;
        this.directionRow = dirRow;
        this.directionCol = dirCol;
    }

    public int getDamage() {
        return damage;
    }

    public int getDirectionRow() {
        return directionRow;
    }

    public int getDirectionCol() {
        return directionCol;
    }
}
