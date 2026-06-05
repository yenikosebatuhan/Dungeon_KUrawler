package com.kurawler.model.object;

/**
 * A moving object on the grid (e.g., arrow, fireball).
 * Travels one cell per tick in a fixed direction until it
 * collides with a wall or a character.
 *
 * <p>Movement logic is handled by the engine ({@code GameLoop.updateProjectiles});
 * this class only stores state and provides position accessors.</p>
 */
public class Projectile extends GameObject {

    private int damage;
    private int directionRow;
    private int directionCol;
    private int speed;
    private int positionRow;
    private int positionCol;
    private boolean active;
    /** Tiles moved after spawn; compared to maxRange (0 = unlimited). */
    private int stepsTraveled;
    private int maxRange;
    /** Team Match: name of firing team ("Team A" / "Team B") so allies are never damaged. */
    private String sourceTeamName;

    /**
     * Creates a projectile with a name, description, damage, and direction.
     *
     * @param name        display name (e.g., "Arcane Bolt")
     * @param description flavour text
     * @param damage      raw damage dealt on hit
     * @param dirRow      row direction component (-1, 0, or 1)
     * @param dirCol      column direction component (-1, 0, or 1)
     */
    public Projectile(String name, String description, int damage, int dirRow, int dirCol) {
        super(name, description);
        this.damage = damage;
        this.directionRow = dirRow;
        this.directionCol = dirCol;
        this.speed = 1;
        this.active = true;
    }

    /**
     * Creates a projectile with explicit speed.
     *
     * @param damage    raw damage dealt on hit
     * @param direction travel direction as a string ("UP","DOWN","LEFT","RIGHT")
     * @param speed     cells moved per tick
     */
    public Projectile(int damage, String direction, int speed) {
        super("Projectile", "A projectile");
        this.damage = damage;
        this.speed = speed;
        this.active = true;
        setDirectionFromString(direction);
    }

    /**
     * Sets the grid position of this projectile.
     *
     * @param row the row coordinate
     * @param col the column coordinate
     */
    public void setPosition(int row, int col) {
        this.positionRow = row;
        this.positionCol = col;
    }

    /**
     * Marks this projectile as inactive (to be removed by the engine).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Returns the raw damage this projectile deals on hit.
     *
     * @return the damage value
     */
    public int getDamage() {
        return damage;
    }

    /**
     * Returns the row direction component (-1, 0, or 1).
     *
     * @return row direction
     */
    public int getDirectionRow() {
        return directionRow;
    }

    /**
     * Returns the column direction component (-1, 0, or 1).
     *
     * @return column direction
     */
    public int getDirectionCol() {
        return directionCol;
    }

    /**
     * Returns the current row position on the grid.
     *
     * @return row coordinate
     */
    public int getPositionRow() {
        return positionRow;
    }

    /**
     * Returns the current column position on the grid.
     *
     * @return column coordinate
     */
    public int getPositionCol() {
        return positionCol;
    }

    /**
     * Returns the travel speed in cells per tick.
     *
     * @return speed value
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Returns whether this projectile is still active.
     *
     * @return true if active, false if it should be removed
     */
    public boolean isActive() {
        return active;
    }

    /** @param range max tiles to travel after spawn; 0 means no limit */
    public void setMaxRange(int range) {
        this.maxRange = Math.max(0, range);
    }

    public int getMaxRange() {
        return maxRange;
    }

    public void incrementStepsTraveled() {
        stepsTraveled++;
    }

    public boolean hasExceededRange() {
        return maxRange > 0 && stepsTraveled >= maxRange;
    }

    public void setSourceTeamName(String teamName) {
        this.sourceTeamName = teamName;
    }

    public String getSourceTeamName() {
        return sourceTeamName;
    }

    /**
     * Parses a direction string into row/col direction components.
     *
     * @param direction one of "UP", "DOWN", "LEFT", "RIGHT" (case-insensitive)
     */
    private void setDirectionFromString(String direction) {
        if (direction == null) {
            this.directionRow = 0;
            this.directionCol = 0;
            return;
        }
        switch (direction.toUpperCase()) {
            case "UP":
                this.directionRow = -1;
                this.directionCol = 0;
                break;
            case "DOWN":
                this.directionRow = 1;
                this.directionCol = 0;
                break;
            case "LEFT":
                this.directionRow = 0;
                this.directionCol = -1;
                break;
            case "RIGHT":
                this.directionRow = 0;
                this.directionCol = 1;
                break;
            default:
                this.directionRow = 0;
                this.directionCol = 0;
                break;
        }
    }
}
