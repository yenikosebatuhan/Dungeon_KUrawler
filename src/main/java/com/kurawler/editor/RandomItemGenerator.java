package com.kurawler.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;

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

    private List<GridCell> findEmptyCells(Map map) {
        List<GridCell> emptyCells = new ArrayList<>();

        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.isWalkable() && !cell.hasObjects()) {
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
                return new Weapon("Random Sword", "A randomly generated sword", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "A randomly generated potion", 10 + random.nextInt(20));
            case 2:
                return new Wearable("Random Armor", "A randomly generated armor piece", 1 + random.nextInt(3));
            case 3:
                return new Key("Random Key", "A randomly generated key", "key_" + random.nextInt(100));
            default:
                return new PortableContainer("Pouch", "A randomly generated pouch", 3 + random.nextInt(3));
        }
    }
}
