package com.kurawler.model;

import com.kurawler.model.object.GameObject;

/**
 * Represents the dungeon grid.
 * Creator: Map creates GridCells (GRASP Creator).
 * Information Expert: Map knows about grid layout.
 */
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
                grid[row][col] = new GridCell(row, col); // Creator: Map creates GridCells
            }
        }
    }

    public GridCell getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return null;
        }
        return grid[row][col];
    }

    public boolean placeObject(int row, int col, GameObject obj) {
        GridCell cell = getCell(row, col);
        if (cell == null) {
            return false;
        }
        cell.addObject(obj);
        return true;
    }

    public boolean removeObject(int row, int col, GameObject obj) {
        GridCell cell = getCell(row, col);
        if (cell == null) {
            return false;
        }
        return cell.removeObject(obj);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
