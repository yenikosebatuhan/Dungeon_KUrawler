package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapFactory {
    private static final int DEFAULT_WIDTH = 16;
    private static final int DEFAULT_HEIGHT = 16;

    public static GameSession createDefaultMap() {
        Map map = new Map(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        for (int r = 0; r < DEFAULT_HEIGHT; r++) {
            for (int c = 0; c < DEFAULT_WIDTH; c++) {
                map.getCell(r, c).setStaticObject(new StaticObject("Wall", "Dark wall", false, false));
            }
        }

        int[][] rooms = {{1, 1, 5, 5}, {1, 10, 4, 5}, {8, 1, 5, 4}, {8, 8, 5, 7}, {13, 1, 2, 14}};
        for (int[] room : rooms) carveRoom(map, room[0], room[1], room[2], room[3]);

        carveHCorridor(map, 3, 6, 10);
        carveVCorridor(map, 6, 8, 3);
        carveVCorridor(map, 5, 8, 12);
        carveHCorridor(map, 10, 5, 8);
        carveVCorridor(map, 12, 13, 2);
        carveVCorridor(map, 12, 13, 10);

        GameSession session = new GameSession(map);
        Hero hero = new Hero("Adventurer");
        
        GridCell spawnCell = map.getCell(3, 3);
        if (spawnCell != null) {
            hero.setPosition(3, 3);
            hero.setCurrentCell(spawnCell);
            spawnCell.setOccupyingCharacter(hero);
        }
        session.setHero(hero);
        // PDF Phase II: a Shadow Clone must only appear after the player READs a
        // Shadow Clone Scroll (which auto-spawns every 15 s), not at game start.
        // No clone is created here on purpose.
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();

        map.placeObject(2, 2, new Weapon("Iron Sword", "A sturdy blade", 8));
        map.placeObject(2, 12, new Consumable("Health Potion", "Restores 10 HP", 10, 0, 0));
        map.placeObject(1, 14, new Consumable("Mana Elixir", "Restores 20 mana", 0, 20, 0));
        map.placeObject(10, 2, new Weapon("War Axe", "A heavy axe", 12));
        map.placeObject(9, 10, new Key("Gold Key", "Opens a golden door", "gold"));
        map.placeObject(10, 13, new Weapon("Dagger", "A small dagger", 5));
        map.placeObject(9, 12, new Consumable("Energy Apple", "Restores energy", 0, 0, 25));
        map.placeObject(8, 10, new Scroll("Healing Scroll", "A parchment covered in glowing runes",
                "Ancient magic knits your wounds back together.", 12));

        placeTargetObjective(map, session);

        return session;
    }

    private static void placeTargetObjective(Map map, GameSession session) {
        Random rng = new Random();
        String[] targetNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String name = targetNames[rng.nextInt(targetNames.length)];
        Key target = new Key(name, "A precious artifact — find it to win!", "objective");
        session.setTargetObjective(target);

        List<GridCell> searchableSpots = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getStaticObject() != null
                        && cell.getStaticObject().isSearchable()) {
                    searchableSpots.add(cell);
                }
            }
        }

        if (searchableSpots.isEmpty()) {
            int[][] candidates = {{9, 9}, {10, 10}, {8, 3}, {2, 13}};
            for (int[] pos : candidates) {
                GridCell cell = map.getCell(pos[0], pos[1]);
                if (cell != null && cell.isWalkable()) {
                    StaticObject pedestal = new StaticObject(
                            "Mysterious Pedestal", "Something glimmers inside...", true, true);
                    pedestal.setHiddenItem(target);
                    cell.setStaticObject(pedestal);
                    return;
                }
            }
        } else {
            GridCell chosen = searchableSpots.get(rng.nextInt(searchableSpots.size()));
            chosen.getStaticObject().setHiddenItem(target);
        }
    }

    private static void carveRoom(Map map, int startRow, int startCol, int height, int width) {
        for (int r = startRow; r < startRow + height && r < map.getHeight(); r++) {
            for (int c = startCol; c < startCol + width && c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null) cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }

    private static void carveHCorridor(Map map, int row, int colFrom, int colTo) {
        int minC = Math.min(colFrom, colTo);
        int maxC = Math.max(colFrom, colTo);
        for (int c = minC; c <= maxC; c++) {
            GridCell cell = map.getCell(row, c);
            if (cell != null) cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
        }
    }

    private static void carveVCorridor(Map map, int rowFrom, int rowTo, int col) {
        int minR = Math.min(rowFrom, rowTo);
        int maxR = Math.max(rowFrom, rowTo);
        for (int r = minR; r <= maxR; r++) {
            GridCell cell = map.getCell(r, col);
            if (cell != null) cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
        }
    }
}
