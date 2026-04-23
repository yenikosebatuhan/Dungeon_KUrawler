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

/**
 * Generates editor items for random placement.
 */
public class RandomItemGenerator {

    private final Random random;

    public RandomItemGenerator() {
        this.random = new Random();
    }

    public void generateRandomItems(Map map, int count) {
        List<GridCell> emptyCells = findAvailableCells(map);
        int itemsToGenerate = Math.min(count, emptyCells.size());

        for (int i = 0; i < itemsToGenerate; i++) {
            GridCell cell = emptyCells.remove(random.nextInt(emptyCells.size()));
            cell.addObject(generateRandomItem());
        }
    }

    public Item generateAndPlaceRandomItem(Map map, int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null || !cell.isWalkable()) {
            return null;
        }

        Item item = generateRandomItem();
        cell.addObject(item);
        return item;
    }

    private List<GridCell> findAvailableCells(Map map) {
        List<GridCell> availableCells = new ArrayList<>();
        if (map == null) {
            return availableCells;
        }

        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.isWalkable() && !cell.hasObjects()) {
                    availableCells.add(cell);
                }
            }
        }

        return availableCells;
    }

    private Item generateRandomItem() {
        int type = random.nextInt(5);
        switch (type) {
            case 0:
                return new Weapon("Random Sword", "A randomly generated sword.", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "Restores a random amount of health.", 10 + random.nextInt(20));
            case 2:
                return new Wearable("Random Armor", "Protective gear from the editor.", 1 + random.nextInt(3));
            case 3:
                return new Key("Random Key", "A randomly generated key.", "key_" + random.nextInt(100));
            default:
                return new PortableContainer("Pouch", "A randomly generated pouch.", 3 + random.nextInt(3));
        }
    }
}
