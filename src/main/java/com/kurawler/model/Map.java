package com.kurawler.model;

import com.kurawler.model.object.GameObject;

public class Map {
    private final int width;
    private final int height;
    private final GridCell[][] grid;

    public Map(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new GridCell[height][width];
        initializeGrid();
    }

    private void initializeGrid() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                grid[row][col] = new GridCell(row, col);
            }
        }
    }

    public GridCell getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) return null;
        return grid[row][col];
    }
    
    // --- ADAPTERS FOR CANER'S TESTS ---
    public GridCell getCellAt(int x, int y) { return getCell(y, x); }
    public int getGridWidth() { return width; }
    public int getGridHeight() { return height; }
    public boolean isInBounds(int x, int y) { return x >= 0 && x < width && y >= 0 && y < height; }

    public boolean placeObject(int row, int col, GameObject obj) {
        GridCell cell = getCell(row, col);
        if (cell == null) return false;
        cell.addObject(obj);
        return true;
    }

    public boolean removeObject(int row, int col, GameObject obj) {
        GridCell cell = getCell(row, col);
        if (cell == null) return false;
        return cell.removeObject(obj);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}