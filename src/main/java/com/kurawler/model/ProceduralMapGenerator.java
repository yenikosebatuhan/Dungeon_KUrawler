package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates randomised dungeons using a simple room-and-corridor algorithm.
 * Each run produces a different layout while guaranteeing connectivity.
 */
public class ProceduralMapGenerator {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 15;
    private static final int MIN_ROOM_SIZE = 3;
    private static final int MAX_ROOM_SIZE = 5;
    private static final int ROOM_ATTEMPTS = 30;

    private final Random rng = new Random();

    public GameSession generate() {
        Map map = new Map(WIDTH, HEIGHT);
        fillWalls(map);

        List<int[]> rooms = carveRooms(map);
        connectRooms(map, rooms);

        GameSession session = new GameSession(map);

        int[] spawnRoom = rooms.get(0);
        int heroR = spawnRoom[0] + spawnRoom[2] / 2;
        int heroC = spawnRoom[1] + spawnRoom[3] / 2;
        Hero hero = new Hero("Adventurer");
        GridCell spawnCell = map.getCell(heroR, heroC);
        hero.moveToCell(spawnCell);
        session.setHero(hero);

        // PDF Phase II: Shadow Clone is summoned via a Shadow Clone Scroll (READ),
        // not spawned at game start. No clone is created here on purpose.
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();

        scatterItems(map, rooms);
        placeObjective(map, rooms, session);

        return session;
    }

    private void fillWalls(Map map) {
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                map.getCell(r, c).setStaticObject(
                        new StaticObject("Wall", "Solid wall", false, false));
            }
        }
    }

    private List<int[]> carveRooms(Map map) {
        List<int[]> rooms = new ArrayList<>();
        for (int attempt = 0; attempt < ROOM_ATTEMPTS; attempt++) {
            int rh = MIN_ROOM_SIZE + rng.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1);
            int rw = MIN_ROOM_SIZE + rng.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1);
            int startR = 1 + rng.nextInt(HEIGHT - rh - 2);
            int startC = 1 + rng.nextInt(WIDTH - rw - 2);

            if (overlapsAny(rooms, startR, startC, rh, rw)) {
                continue;
            }

            for (int r = startR; r < startR + rh; r++) {
                for (int c = startC; c < startC + rw; c++) {
                    map.getCell(r, c).setStaticObject(
                            new StaticObject("Floor", "Stone floor", true, false));
                }
            }
            rooms.add(new int[]{startR, startC, rh, rw});
        }
        return rooms;
    }

    private boolean overlapsAny(List<int[]> rooms, int r, int c, int h, int w) {
        for (int[] room : rooms) {
            if (r < room[0] + room[2] + 1 && r + h + 1 > room[0]
                    && c < room[1] + room[3] + 1 && c + w + 1 > room[1]) {
                return true;
            }
        }
        return false;
    }

    private void connectRooms(Map map, List<int[]> rooms) {
        for (int i = 1; i < rooms.size(); i++) {
            int[] a = rooms.get(i - 1);
            int[] b = rooms.get(i);
            int ar = a[0] + a[2] / 2;
            int ac = a[1] + a[3] / 2;
            int br = b[0] + b[2] / 2;
            int bc = b[1] + b[3] / 2;

            if (rng.nextBoolean()) {
                carveH(map, ar, ac, bc);
                carveV(map, bc, ar, br);
            } else {
                carveV(map, ac, ar, br);
                carveH(map, br, ac, bc);
            }
        }
    }

    private void carveH(Map map, int row, int c1, int c2) {
        int from = Math.min(c1, c2);
        int to = Math.max(c1, c2);
        for (int c = from; c <= to; c++) {
            GridCell cell = map.getCell(row, c);
            if (cell != null) {
                cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }

    private void carveV(Map map, int col, int r1, int r2) {
        int from = Math.min(r1, r2);
        int to = Math.max(r1, r2);
        for (int r = from; r <= to; r++) {
            GridCell cell = map.getCell(r, col);
            if (cell != null) {
                cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }

    private void scatterItems(Map map, List<int[]> rooms) {
        List<GridCell> floors = allFloors(map);
        if (floors.size() < 8) return;

        String[][] weapons = {
                {"Iron Sword", "A sturdy blade", "7"},
                {"War Axe", "Heavy and deadly", "11"},
                {"Dagger", "Fast but weak", "4"},
        };
        for (String[] w : weapons) {
            place(floors, new Weapon(w[0], w[1], Integer.parseInt(w[2])));
        }
        place(floors, new Consumable("Health Potion", "Restores 15 HP", 15, 0, 0));
        place(floors, new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0));
        place(floors, new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35));
        place(floors, new Wearable("Chain Mail", "Decent protection", 3));
        place(floors, new Key("Silver Key", "Opens a silver lock", "silver"));
    }

    private void place(List<GridCell> floors, com.kurawler.model.object.GameObject obj) {
        if (floors.isEmpty()) return;
        GridCell cell = floors.remove(rng.nextInt(floors.size()));
        cell.addObject(obj);
    }

    private void placeObjective(Map map, List<int[]> rooms, GameSession session) {
        Key target = new Key("Crystal Orb", "The legendary artifact — find it to win!", "objective");
        session.setTargetObjective(target);

        List<GridCell> floors = allFloors(map);
        if (floors.isEmpty()) {
            return;
        }

        int searchableCount = Math.min(2 + rng.nextInt(3), floors.size()); // 2..4
        List<GridCell> searchableCells = new ArrayList<>();
        for (int i = 0; i < searchableCount; i++) {
            GridCell cell = floors.remove(rng.nextInt(floors.size()));
            StaticObject pedestal = new StaticObject(
                    "Ancient Pedestal", "Something glimmers inside...", true, true);
            cell.setStaticObject(pedestal);
            searchableCells.add(cell);
        }

        GridCell objectiveCell = searchableCells.get(rng.nextInt(searchableCells.size()));
        objectiveCell.getStaticObject().setHiddenItem(target);

        for (GridCell cell : searchableCells) {
            if (cell == objectiveCell) {
                continue;
            }
            cell.getStaticObject().setHiddenItem(createDecoyHiddenItem());
        }
    }

    private Item createDecoyHiddenItem() {
        switch (rng.nextInt(4)) {
            case 0:
                return new Consumable("Health Potion", "Restores 15 HP", 15, 0, 0);
            case 1:
                return new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35);
            case 2:
                return new Scroll("Spark Scroll", "A crackling note", "A minor shock courses through you.", 4);
            default:
                return new Weapon("Rusty Dagger", "Worn but usable", 3);
        }
    }

    private List<GridCell> allFloors(Map map) {
        List<GridCell> floors = new ArrayList<>();
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                GridCell cell = map.getCell(r, c);
                if (cell.getStaticObject() != null && cell.getStaticObject().isPassable()
                        && cell.getOccupyingCharacter() == null && !cell.hasObjects()) {
                    floors.add(cell);
                }
            }
        }
        return floors;
    }
}
