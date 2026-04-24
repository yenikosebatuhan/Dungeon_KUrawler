package com.kurawler.model;

/**
 * Represents the game's grid-based map structure.
 * The map is composed of a 2D array of GridCell objects and provides
 * safe coordinate-based access to cells.
 */
public class Map {
    private final int gridWidth;
    private final int gridHeight;
    private final GridCell[][] grid;
    
    /**
     * Constructs a Map with the specified dimensions.
     * All cells are automatically initialized.
     * @param width The width of the map (number of columns)
     * @param height The height of the map (number of rows)
     * @throws IllegalArgumentException if width or height is less than 1
     */
    public Map(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                "Map dimensions must be at least 1x1. Got: " + width + "x" + height
            );
        }
        this.gridWidth = width;
        this.gridHeight = height;
        this.grid = new GridCell[width][height];
        initializeGrid();
    }
    
    /**
     * Initializes all grid cells with their coordinate positions.
     * This is called automatically during construction.
     */
    private void initializeGrid() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                grid[x][y] = new GridCell(x, y);
            }
        }
    }
    public GridCell getCell(int row, int col) {
        // Their 'col' is our 'x', their 'row' is our 'y'
        return getCellAt(col, row); 
    }
    
    public int getWidth() {
        return getGridWidth();
    }
    
    public int getHeight() {
        return getGridHeight();
    }
    /**
     * Retrieves the GridCell at the specified coordinates.
     * Performs bounds checking to prevent array access violations.
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return The GridCell at (x, y), or null if coordinates are out of bounds
     */
    public GridCell getCellAt(int x, int y) {
        // Bounds checking: return null for invalid coordinates
        if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) {
            return null;
        }
        return grid[x][y];
    }
    
    /**
     * Gets the width of the map.
     * @return The number of columns in the grid
     */
    public int getGridWidth() {
        return gridWidth;
    }
    
    /**
     * Gets the height of the map.
     * @return The number of rows in the grid
     */
    public int getGridHeight() {
        return gridHeight;
    }
    
    /**
     * Gets direct access to the underlying grid array.
     * Use with caution - prefer getCellAt() for safe access.
     * @return The 2D array of GridCells
     */
    public GridCell[][] getGrid() {
        return grid;
    }
    
    /**
     * Checks if the given coordinates are within map bounds.
     * @param x The x-coordinate to check
     * @param y The y-coordinate to check
     * @return true if coordinates are valid, false otherwise
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }
    
    @Override
    public String toString() {
        return "Map{size=" + gridWidth + "x" + gridHeight + 
               ", cells=" + (gridWidth * gridHeight) + "}";
    }
}