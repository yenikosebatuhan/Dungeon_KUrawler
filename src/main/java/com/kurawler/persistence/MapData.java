package com.kurawler.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable data representation of a map (for save/load).
 */
public class MapData {

    private int width;
    private int height;
    private List<ObjectPlacement> placements;

    public MapData() {
        this.placements = new ArrayList<>();
    }

    public MapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.placements = new ArrayList<>();
    }

    public void addPlacement(String objectType, String objectName, int row, int col) {
        placements.add(new ObjectPlacement(objectType, objectName, row, col));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<ObjectPlacement> getPlacements() {
        return placements;
    }

    /** Inner class representing a single object placement. */
    public static class ObjectPlacement {
        private final String objectType;
        private final String objectName;
        private final int row;
        private final int col;

        public ObjectPlacement(String objectType, String objectName, int row, int col) {
            this.objectType = objectType;
            this.objectName = objectName;
            this.row = row;
            this.col = col;
        }

        public String getObjectType() { return objectType; }
        public String getObjectName() { return objectName; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }
}
