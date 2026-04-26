package com.kurawler.editor;

import com.kurawler.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomItemGenerator {

    private final Random random;

    public RandomItemGenerator() {
        this.random = new Random();
    }

    public void generateRandomItems(Map map, int count) {
        if (map == null || count <= 0) {
            return;
        }

        List<GridCell> emptyCells = findEmptyCells(map);

        if (emptyCells.isEmpty()) {
            return;
        }

        int itemsToGenerate = Math.min(count, emptyCells.size());

        for (int i = 0; i < itemsToGenerate; i++) {
            int cellIndex = random.nextInt(emptyCells.size());
            GridCell cell = emptyCells.remove(cellIndex);
            cell.addGameObject(generateRandomItem());
        }
    }

    public Item generateAndPlaceRandomItem(Map map, int x, int y) {
        if (map == null) {
            return null;
        }

        GridCell cell = map.getCellAt(x, y);
        if (cell == null || cell.isBlocked()) {
            return null;
        }

        Item item = generateRandomItem();
        cell.addGameObject(item);
        return item;
    }

    private List<GridCell> findEmptyCells(Map map) {
        List<GridCell> emptyCells = new ArrayList<>();

        for (int x = 0; x < map.getGridWidth(); x++) {
            for (int y = 0; y < map.getGridHeight(); y++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell != null && cell.isEmpty()) {
                    emptyCells.add(cell);
                }
            }
        }

        return emptyCells;
    }

    private Item generateRandomItem() {
        int type = random.nextInt(5);

        switch (type) {
            case 0:
                return new Weapon("Random Sword", 3 + random.nextInt(5), false);
            case 1:
                return new Consumable("Health Potion", 10 + random.nextInt(20), Consumable.STAT_HP);
            case 2:
                return new Wearable("Random Armor", 1 + random.nextInt(3), Wearable.MOD_DEF);
            case 3:
                return new Key("Random Key", "key_" + random.nextInt(100));
            default:
                return new PortableContainer("Pouch", 3 + random.nextInt(3));
        }
    }
}
