package com.kurawler.model;

import com.kurawler.model.character.Hero;
import java.util.Random;

/**
 * Factory for creating game maps with rooms and corridors.
 * GRASP Creator: MapFactory creates Maps and populates them.
 */
public class MapFactory {

    private static final int DEFAULT_WIDTH = 16;
    private static final int DEFAULT_HEIGHT = 16;

    /** Create the default dungeon map with rooms, corridors, and items. */
    public static GameSession createDefaultMap() {
        Random rng = new Random();
        Map map = new Map(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Start with all walls
        for (int r = 0; r < DEFAULT_HEIGHT; r++) {
            for (int c = 0; c < DEFAULT_WIDTH; c++) {
                map.getCell(r, c).setWalkable(false);
            }
        }

        // Define rooms: {row, col, height, width}
        int[][] rooms = {
            {1, 1, 5, 5},     // Room 0: top-left (hero spawn)
            {1, 10, 4, 5},    // Room 1: top-right
            {8, 1, 5, 4},     // Room 2: mid-left
            {8, 8, 5, 7},     // Room 3: mid-right (large)
            {13, 1, 2, 14},   // Room 4: bottom corridor hall
        };

        // Carve rooms
        for (int[] room : rooms) {
            carveRoom(map, room[0], room[1], room[2], room[3]);
        }

        // Connect rooms with corridors
        // Room 0 -> Room 1 (horizontal at row 3)
        carveHCorridor(map, 3, 6, 10);
        // Room 0 -> Room 2 (vertical at col 3)
        carveVCorridor(map, 6, 8, 3);
        // Room 1 -> Room 3 (vertical at col 12)
        carveVCorridor(map, 5, 8, 12);
        // Room 2 -> Room 3 (horizontal at row 10)
        carveHCorridor(map, 10, 5, 8);
        // Room 2 -> Room 4 (vertical at col 2)
        carveVCorridor(map, 12, 13, 2);
        // Room 3 -> Room 4 (vertical at col 10)
        carveVCorridor(map, 12, 13, 10);

        // Create session
        GameSession session = new GameSession(map);

        // Place hero in room 0
        Hero hero = new Hero("Adventurer");
        hero.setPosition(3, 3);
        session.setHero(hero);

        // Scatter items across rooms
        // Weapons: (name, atkValue, isRanged)
        map.placeObject(2, 2,   new Weapon("Iron Sword", 8, false));
        map.placeObject(10, 2,  new Weapon("War Axe", 12, false));
        map.placeObject(10, 13, new Weapon("Dagger", 5, false));
        map.placeObject(13, 7,  new Weapon("Magic Staff", 10, true)); // ranged
        // Consumables: (name, restoreAmount, statType)
        map.placeObject(2, 12,  new Consumable("Health Potion", 10, Consumable.STAT_HP));
        map.placeObject(1, 14,  new Consumable("Mana Elixir", 20, Consumable.STAT_MANA));
        map.placeObject(9, 12,  new Consumable("Energy Apple", 15, Consumable.STAT_ENERGY));
        // Keys: (name, keyId)
        map.placeObject(9, 10,  new Key("Gold Key", "gold"));

        return session;
    }

    /** Carve a rectangular room (set cells to walkable). */
    private static void carveRoom(Map map, int startRow, int startCol, int height, int width) {
        for (int r = startRow; r < startRow + height && r < map.getHeight(); r++) {
            for (int c = startCol; c < startCol + width && c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null) {
                    cell.setWalkable(true);
                }
            }
        }
    }

    /** Carve a horizontal corridor at a fixed row between two columns. */
    private static void carveHCorridor(Map map, int row, int colFrom, int colTo) {
        int minC = Math.min(colFrom, colTo);
        int maxC = Math.max(colFrom, colTo);
        for (int c = minC; c <= maxC; c++) {
            GridCell cell = map.getCell(row, c);
            if (cell != null) cell.setWalkable(true);
        }
    }

    /** Carve a vertical corridor at a fixed column between two rows. */
    private static void carveVCorridor(Map map, int rowFrom, int rowTo, int col) {
        int minR = Math.min(rowFrom, rowTo);
        int maxR = Math.max(rowFrom, rowTo);
        for (int r = minR; r <= maxR; r++) {
            GridCell cell = map.getCell(r, col);
            if (cell != null) cell.setWalkable(true);
        }
    }
}
