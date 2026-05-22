package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
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
                if (cell != null && !cell.isBlocked() && !cell.hasObjects()) {
                    emptyCells.add(cell);
                }
            }
        }

        return emptyCells;
    }

    private Item generateRandomItem() {
        int type = random.nextInt(8);

        switch (type) {
            case 0:
                return new Weapon("Random Sword", "A randomly forged blade", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "Restores HP", 10 + random.nextInt(20), 0, 0);
            case 2:
                return new Consumable("Mana Elixir", "Restores mana", 0, 15 + random.nextInt(15), 0);
            case 3:
                return new Consumable("Energy Apple", "Restores energy", 0, 0, 15 + random.nextInt(15));
            case 4:
                return new Wearable("Random Armor", "Protective gear", 1 + random.nextInt(3));
            case 5:
                return new Key("Random Key", "A mysterious key", "key_" + random.nextInt(100));
            case 6:
                return new Scroll("Healing Scroll", "A parchment covered in glowing runes",
                        "Warm light restores your wounds.", 8 + random.nextInt(10));
            default:
                return new PortableContainer("Pouch", "A small pouch", 3 + random.nextInt(3));
        }
    }
}
