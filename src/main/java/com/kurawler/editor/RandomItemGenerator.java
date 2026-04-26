package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
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
            cell.addObject(generateRandomItem());
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
        cell.addObject(item);
        return item;
    }

    private List<GridCell> findEmptyCells(Map map) {
        List<GridCell> emptyCells = new ArrayList<>();

        for (int x = 0; x < map.getGridWidth(); x++) {
            for (int y = 0; y < map.getGridHeight(); y++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell != null && !cell.isBlocked() && cell.getObjects().isEmpty()) {
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
                return new Weapon("Random Sword", "A random sword", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "Restores HP", 10 + random.nextInt(20));
            case 2:
                return new Wearable("Random Armor", "Random armor", 1 + random.nextInt(3));
            case 3:
                return new Key("Random Key", "A random key", "key_" + random.nextInt(100));
            default:
                return new PortableContainer("Pouch", "A small pouch", 3 + random.nextInt(3));
        }
    }
}
