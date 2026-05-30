package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Random loot for the level editor — PDF §4.2:
 * 5 floor items, 1 hidden in a searchable (non-container) spot,
 * container fill rule (roll 1–10, add loot while roll ≥ 8).
 */
public class RandomItemGenerator {

    private static final int FLOOR_ITEM_COUNT = 5;

    private final Random random;

    public RandomItemGenerator() {
        this.random = new Random();
    }

    public static final class PopulateResult {
        public final int floorItemsPlaced;
        public final boolean hiddenItemPlaced;
        public final int containerLootAdded;

        public PopulateResult(int floorItemsPlaced, boolean hiddenItemPlaced, int containerLootAdded) {
            this.floorItemsPlaced = floorItemsPlaced;
            this.hiddenItemPlaced = hiddenItemPlaced;
            this.containerLootAdded = containerLootAdded;
        }
    }

    public PopulateResult populatePerDesignDocument(Map map) {
        if (map == null) {
            return new PopulateResult(0, false, 0);
        }

        int containerLoot = 0;
        int placed = 0;

        List<GridCell> floorCells = findEmptyFloorCells(map);
        Collections.shuffle(floorCells, random);

        for (int i = 0; i < FLOOR_ITEM_COUNT && i < floorCells.size(); i++) {
            GridCell cell = floorCells.get(i);
            GameObject obj = generateRandomFloorObject();
            cell.addObject(obj);
            placed++;
            containerLoot += fillNewContainer(obj);
        }

        boolean hidden = placeOneHiddenSearchableItem(map);
        containerLoot += fillEmptyContainersAlreadyOnMap(map);

        return new PopulateResult(placed, hidden, containerLoot);
    }

    public void generateRandomItems(Map map, int count) {
        if (map == null || count <= 0) {
            return;
        }
        List<GridCell> emptyCells = findEmptyFloorCells(map);
        int n = Math.min(count, emptyCells.size());
        for (int i = 0; i < n; i++) {
            int idx = random.nextInt(emptyCells.size());
            GridCell cell = emptyCells.remove(idx);
            GameObject obj = generateRandomFloorObject();
            cell.addObject(obj);
            fillNewContainer(obj);
        }
    }

    public GameObject generateAndPlaceRandomItem(Map map, int x, int y) {
        if (map == null) {
            return null;
        }
        GridCell cell = map.getCellAt(x, y);
        if (cell == null || cell.isBlocked() || cell.hasObjects()) {
            return null;
        }
        GameObject obj = generateRandomFloorObject();
        cell.addObject(obj);
        fillNewContainer(obj);
        return obj;
    }

    private int fillNewContainer(GameObject obj) {
        if (obj instanceof PortableContainer) {
            return rollContainerLoot((PortableContainer) obj);
        }
        if (obj instanceof StaticContainer) {
            return rollContainerLoot((StaticContainer) obj);
        }
        return 0;
    }

    private int fillEmptyContainersAlreadyOnMap(Map map) {
        int total = 0;
        for (int y = 0; y < map.getGridHeight(); y++) {
            for (int x = 0; x < map.getGridWidth(); x++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell == null) {
                    continue;
                }
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) {
                        StaticContainer sc = (StaticContainer) obj;
                        if (sc.isEmpty()) {
                            total += rollContainerLoot(sc);
                        }
                    }
                }
            }
        }
        return total;
    }

    private int rollContainerLoot(PortableContainer container) {
        int added = 0;
        int roll;
        do {
            roll = random.nextInt(10) + 1;
            if (roll >= 8) {
                container.addItem(generateRandomLootItem());
                added++;
            }
        } while (roll >= 8);
        return added;
    }

    private int rollContainerLoot(StaticContainer container) {
        int added = 0;
        int roll;
        do {
            roll = random.nextInt(10) + 1;
            if (roll >= 8) {
                container.addItem(generateRandomLootItem());
                added++;
            }
        } while (roll >= 8);
        return added;
    }

    private boolean placeOneHiddenSearchableItem(Map map) {
        List<GridCell> searchable = findSearchableNonContainerCells(map);
        if (searchable.isEmpty()) {
            GridCell cell = pickRandomWalkableCell(map);
            if (cell == null) {
                return false;
            }
            cell.setStaticObject(new StaticObject(
                    "Gargoyle Pool", "A searchable stone pool", true, true));
            cell.getStaticObject().setHiddenItem(generateRandomLootItem());
            return true;
        }
        GridCell chosen = searchable.get(random.nextInt(searchable.size()));
        StaticObject so = chosen.getStaticObject();
        if (so.getHiddenItem() != null) {
            return false;
        }
        so.setHiddenItem(generateRandomLootItem());
        return true;
    }

    private List<GridCell> findEmptyFloorCells(Map map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 0; y < map.getGridHeight(); y++) {
            for (int x = 0; x < map.getGridWidth(); x++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell != null && cell.isWalkable()
                        && cell.getOccupyingCharacter() == null
                        && !cell.hasObjects()) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private List<GridCell> findSearchableNonContainerCells(Map map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 0; y < map.getGridHeight(); y++) {
            for (int x = 0; x < map.getGridWidth(); x++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell == null || cell.getOccupyingCharacter() != null) {
                    continue;
                }
                StaticObject so = cell.getStaticObject();
                if (so == null || !so.isSearchable()) {
                    continue;
                }
                if (hasContainerOnCell(cell)) {
                    continue;
                }
                cells.add(cell);
            }
        }
        return cells;
    }

    private boolean hasContainerOnCell(GridCell cell) {
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer || obj instanceof PortableContainer) {
                return true;
            }
        }
        return false;
    }

    private GridCell pickRandomWalkableCell(Map map) {
        List<GridCell> walkable = new ArrayList<>();
        for (int y = 0; y < map.getGridHeight(); y++) {
            for (int x = 0; x < map.getGridWidth(); x++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
                    walkable.add(cell);
                }
            }
        }
        if (walkable.isEmpty()) {
            return null;
        }
        return walkable.get(random.nextInt(walkable.size()));
    }

    /** Floor spawn: items plus static containers (chest/crate). */
    private GameObject generateRandomFloorObject() {
        int type = random.nextInt(10);
        switch (type) {
            case 0:
                return new Weapon("Random Sword", "A randomly forged blade", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "Restores HP",
                        10 + random.nextInt(20), 0, 0);
            case 2:
                return new Consumable("Mana Elixir", "Restores mana",
                        0, 15 + random.nextInt(15), 0);
            case 3:
                return new Consumable("Energy Apple", "Restores energy",
                        0, 0, 15 + random.nextInt(15));
            case 4:
                return new Wearable("Random Armor", "Protective gear", 1 + random.nextInt(3));
            case 5:
                return new Key("Random Key", "A mysterious key", "key_" + random.nextInt(100));
            case 6:
                return new Scroll("Healing Scroll", "A parchment covered in glowing runes",
                        "Warm light restores your wounds.", 8 + random.nextInt(10));
            case 7:
                return new StaticContainer("Chest", "A wooden chest");
            case 8:
                return new PortableContainer("Pouch", "A small pouch", 3 + random.nextInt(3));
            default:
                return new StaticContainer("Crate", "A wooden crate");
        }
    }

    /** Pick-up loot only (never a static chest/crate). */
    private Item generateRandomLootItem() {
        int type = random.nextInt(7);
        switch (type) {
            case 0:
                return new Weapon("Random Sword", "A randomly forged blade", 3 + random.nextInt(5));
            case 1:
                return new Consumable("Health Potion", "Restores HP",
                        10 + random.nextInt(20), 0, 0);
            case 2:
                return new Consumable("Mana Elixir", "Restores mana",
                        0, 15 + random.nextInt(15), 0);
            case 3:
                return new Wearable("Random Armor", "Protective gear", 1 + random.nextInt(3));
            case 4:
                return new Key("Random Key", "A mysterious key", "key_" + random.nextInt(100));
            case 5:
                return new Scroll("Healing Scroll", "A parchment covered in glowing runes",
                        "Warm light restores your wounds.", 8 + random.nextInt(10));
            default:
                return new PortableContainer("Pouch", "A small pouch", 3 + random.nextInt(3));
        }
    }
}
