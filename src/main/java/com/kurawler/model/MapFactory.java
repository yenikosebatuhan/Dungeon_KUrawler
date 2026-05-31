package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds the default multi-map dungeon: Entry Hall → Catacombs → Treasure Vault.
 */
public class MapFactory {

    /** All story maps share one size so the play window does not shrink on transition. */
    private static final int WORLD_WIDTH = 22;
    private static final int WORLD_HEIGHT = 16;

    public static GameSession createDefaultMap() {
        Map hall = buildEntryHall();
        Map catacombs = buildCatacombs();
        Map vault = buildTreasureVault();

        GameSession session = new GameSession(hall);
        session.registerWorldMap("hall", hall);
        session.registerWorldMap("catacombs", catacombs);
        session.registerWorldMap("vault", vault);
        session.setMap(hall);
        session.setCurrentMapId("hall");

        Hero hero = new Hero("Adventurer");
        GridCell spawn = hall.getCell(4, 4);
        if (spawn != null) {
            hero.setPosition(4, 4);
            hero.moveToCell(spawn);
        }
        session.setHero(hero);
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();

        scatterDefaultLoot(hall, catacombs, vault);

        Random weaponRng = new Random();
        scatterRandomMapWeapons(hall, weaponRng, 5 + weaponRng.nextInt(2));
        scatterRandomMapWeapons(catacombs, weaponRng, 5 + weaponRng.nextInt(2));
        scatterRandomMapWeapons(vault, weaponRng, 3 + weaponRng.nextInt(2));

        placeVaultKeyInRandomBookshelf(hall, catacombs);

        placeTargetInRandomVaultChest(vault, session);

        return session;
    }

    /** Win objective + hidden target for a custom / editor-loaded map. */
    public static void configureObjectiveForMap(Map map, GameSession session) {
        List<StaticContainer> chests = collectChests(map);
        if (!chests.isEmpty()) {
            placeTargetInRandomChest(map, session, chests);
        } else {
            placeTargetObjective(map, session);
        }
    }

    // --- Three connected maps (PDF-style dungeon, not a tiny grid puzzle) ---

    private static Map newWorldMap() {
        Map map = new Map(WORLD_WIDTH, WORLD_HEIGHT);
        fillWalls(map);
        carveRoom(map, 1, 1, 14, 20);
        carveRoom(map, 10, 2, 4, 6);
        return map;
    }

    private static Map buildEntryHall() {
        Map map = newWorldMap();

        setFloor(map, 3, 3, new StaticObject(
                "Ancient Bookshelf", "Dusty tomes line the shelves — try searching.", true, true));
        setFloor(map, 10, 5, new StaticObject(
                "Gargoyle Pool", "A shallow pool — something may be hidden inside.", true, true));
        setFloor(map, 12, 8, new StaticObject(
                "Stone Column", "A cracked pillar — search the base.", false, true));

        map.getCell(7, 19).setStaticObject(new MapPortal(
                "Wooden Door", "Leads deeper into the dungeon.",
                "catacombs", 7, 2, null, true));

        return map;
    }

    private static Map buildCatacombs() {
        Map map = newWorldMap();

        setFloor(map, 4, 4, new StaticObject(
                "Wall Shelf", "Empty ledges along the wall.", true, true));
        setFloor(map, 10, 5, new StaticObject(
                "Gargoyle Pool", "Stagnant water glistens.", true, true));
        StaticObject catacombsPool = map.getCell(10, 5).getStaticObject();
        if (catacombsPool != null) {
            catacombsPool.setHiddenItem(new Consumable("Health Potion", "Restores 15 HP", 15, 0, 0));
        }
        setFloor(map, 12, 8, new StaticObject(
                "Stone Column", "A cracked pillar — search the base.", false, true));

        map.getCell(7, 1).setStaticObject(new MapPortal(
                "Wooden Door", "Back to the entry hall.",
                "hall", 7, 18, null, true));

        map.getCell(7, 19).setStaticObject(new MapPortal(
                "Iron Door", "A heavy door to the treasure vault.",
                "vault", 7, 2, "gold", false));

        map.placeObject(6, 14, new StaticContainer("Chest", "An old wooden chest"));
        return map;
    }

    private static Map buildTreasureVault() {
        Map map = newWorldMap();

        setFloor(map, 3, 4, new StaticObject(
                "Wall Shelf", "Relics rest on stone shelves — search carefully.", true, true));
        setFloor(map, 10, 6, new StaticObject(
                "Gargoyle Pool", "A dark pool — search the water.", true, true));
        setFloor(map, 12, 10, new StaticObject(
                "Stone Column", "Ancient carvings cover the column.", false, true));

        map.getCell(7, 1).setStaticObject(new MapPortal(
                "Iron Door", "Return to the catacombs.",
                "catacombs", 7, 18, null, true));

        map.placeObject(6, 14, new StaticContainer("Chest", "A sealed treasure chest"));
        map.placeObject(8, 10, new StaticContainer("Chest", "An ornate chest"));
        return map;
    }

    private static void fillWalls(Map map) {
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                map.getCell(r, c).setStaticObject(
                        new StaticObject("Wall", "Dark stone wall", false, false));
            }
        }
    }

  /** Floor + chest loot (similar density to the old single-map default). */
    private static void scatterDefaultLoot(Map hall, Map catacombs, Map vault) {
        hall.placeObject(2, 12, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        hall.placeObject(6, 12, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        hall.placeObject(8, 14, new Consumable("Energy Apple", "Restores energy", 0, 0, 25));
        hall.placeObject(9, 10, new Consumable("Mana Elixir", "Restores 25 mana", 0, 25, 0));
        hall.placeObject(13, 12, new Scroll("Spell Book", "Arcane tome",
                "Runes mend your wounds.", 12));
        hall.placeObject(14, 5, new Wearable("Armor", "Leather armor", 2));
        hall.placeObject(6, 14, new StaticContainer("Chest", "A dusty chest"));

        catacombs.placeObject(4, 5, new Scroll("Spell Book", "Arcane tome",
                "Runes flare and mend your wounds.", 14));
        catacombs.placeObject(5, 10, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        catacombs.placeObject(8, 9, new Consumable("Mana Elixir", "Restores 25 mana", 0, 25, 0));
        catacombs.placeObject(10, 12, new Consumable("Energy Apple", "Restores energy", 0, 0, 20));
        catacombs.placeObject(3, 7, new Wearable("Armor", "Worn chain mail", 2));
        catacombs.placeObject(5, 13, new Scroll("Healing Scroll", "Glowing parchment", "Heals wounds.", 10));

        vault.placeObject(2, 12, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        vault.placeObject(6, 12, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        vault.placeObject(8, 14, new Consumable("Energy Apple", "Restores energy", 0, 0, 30));
        vault.placeObject(9, 10, new Consumable("Mana Elixir", "Restores 30 mana", 0, 30, 0));
        vault.placeObject(13, 12, new Scroll("Spell Book", "Ancient scripture",
                "Warm light restores your wounds.", 10));
        vault.placeObject(14, 5, new Wearable("Armor", "Royal armor", 4));

        populateChest(hall, 6, 14,
                new Consumable("Energy Apple", "Restores energy", 0, 0, 20),
                new Key("Silver Key", "Ornamental key", "silver"));
        populateChest(catacombs, 6, 14,
                new Consumable("Mana Elixir", "Restores 30 mana", 0, 30, 0),
                new Wearable("Armor", "Heavy armor", 3));
        populateChest(vault, 6, 14,
                new Consumable("Energy Apple", "Restores energy", 0, 0, 30));
        populateChest(vault, 8, 10,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0),
                new Scroll("Spell Book", "Faded runes", "A spark of healing.", 8));
    }

    /**
     * Places {@code count} distinct catalog weapons on one map (random each new game).
     * If Arrow is chosen, Bow is included on the same map.
     */
    private static void scatterRandomMapWeapons(Map map, Random rng, int count) {
        List<String> pool = new ArrayList<>();
        for (String[] row : WeaponCatalog.ENTRIES) {
            if (row != null && row.length > 0) {
                pool.add(row[0]);
            }
        }
        Collections.shuffle(pool, rng);
        List<String> chosen = new ArrayList<>();
        for (String name : pool) {
            if (chosen.size() >= count) {
                break;
            }
            if (!chosen.contains(name)) {
                chosen.add(name);
            }
        }
        ensureBowWithArrow(chosen, pool, rng);
        placeWeaponBatch(map, rng, chosen);
    }

    private static void ensureBowWithArrow(List<String> chosen, List<String> pool, Random rng) {
        boolean hasArrow = false;
        boolean hasBow = false;
        for (String n : chosen) {
            if (WeaponCatalog.isArrow(n)) {
                hasArrow = true;
            }
            if (WeaponCatalog.isBow(n)) {
                hasBow = true;
            }
        }
        if (hasArrow && !hasBow) {
            chosen.add("Bow");
        }
        if (!hasArrow && hasBow && chosen.size() < pool.size()) {
            for (String n : pool) {
                if (WeaponCatalog.isArrow(n) && !chosen.contains(n)) {
                    chosen.add(n);
                    break;
                }
            }
        }
    }

    private static void placeWeaponBatch(Map map, Random rng, List<String> weaponNames) {
        if (weaponNames.isEmpty()) {
            return;
        }
        List<GridCell> floors = collectEmptyFloorTiles(map);
        Collections.shuffle(floors, rng);
        int floorIdx = 0;
        for (String name : weaponNames) {
            while (floorIdx < floors.size()) {
                GridCell cell = floors.get(floorIdx++);
                if (isNear(cell, 4, 4, 2)) {
                    continue;
                }
                map.placeObject(cell.getRow(), cell.getCol(), WeaponCatalog.create(name));
                break;
            }
        }
    }

    private static List<GridCell> collectEmptyFloorTiles(Map map) {
        List<GridCell> floors = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (isEmptyFloorTile(cell)) {
                    floors.add(cell);
                }
            }
        }
        return floors;
    }

    private static boolean isEmptyFloorTile(GridCell cell) {
        if (cell == null || !cell.isWalkable() || cell.hasObjects()) {
            return false;
        }
        if (cell.getOccupyingCharacter() != null) {
            return false;
        }
        StaticObject so = cell.getStaticObject();
        if (so == null || so instanceof MapPortal) {
            return false;
        }
        if (so.isSearchable()) {
            return false;
        }
        return "Floor".equals(so.getName());
    }

    private static boolean isNear(GridCell cell, int row, int col, int radius) {
        return Math.abs(cell.getRow() - row) <= radius && Math.abs(cell.getCol() - col) <= radius;
    }

    /**
     * Golden Key (vault door) is hidden in exactly one searchable shelf —
     * Entry Hall bookshelf or Catacombs wall shelf (random each new game).
     */
    private static void placeVaultKeyInRandomBookshelf(Map hall, Map catacombs) {
        Random rng = new Random();
        Key vaultKey = new Key("Golden Key", "Opens the iron vault door", "gold");

        int hallRow = 3;
        int hallCol = 3;
        int catRow = 4;
        int catCol = 4;

        Map chosenMap = rng.nextBoolean() ? hall : catacombs;
        int row = chosenMap == hall ? hallRow : catRow;
        int col = chosenMap == hall ? hallCol : catCol;

        GridCell cell = chosenMap.getCell(row, col);
        if (cell != null && cell.getStaticObject() != null && cell.getStaticObject().isSearchable()) {
            cell.getStaticObject().setHiddenItem(vaultKey);
        }
    }

    /**
     * Win target (objective key) inside a random chest on the treasure vault.
     * Golden Key for the vault door is separate — see {@link #placeVaultKeyInRandomBookshelf}.
     */
    private static void placeTargetInRandomVaultChest(Map vault, GameSession session) {
        placeTargetInRandomChest(vault, session, collectChests(vault));
        Random rng = new Random();
        for (GridCell cell : collectSearchableSpots(vault)) {
            StaticObject so = cell.getStaticObject();
            if (so != null && so.getHiddenItem() == null) {
                so.setHiddenItem(createDecoyHiddenItem(rng));
            }
        }
    }

    private static void placeTargetInRandomChest(Map map, GameSession session, List<StaticContainer> chests) {
        if (chests.isEmpty()) {
            return;
        }
        Random rng = new Random();
        String[] targetNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String name = targetNames[rng.nextInt(targetNames.length)];
        Key target = new Key(name, "A precious artifact — find it to win!", "objective");
        session.setTargetObjective(target);

        StaticContainer chosen = chests.get(rng.nextInt(chests.size()));
        chosen.addItem(target);
    }

    private static List<StaticContainer> collectChests(Map map) {
        List<StaticContainer> chests = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) {
                    continue;
                }
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) {
                        chests.add((StaticContainer) obj);
                    }
                }
            }
        }
        return chests;
    }

    private static void populateChest(Map map, int row, int col, Item... loot) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer) {
                StaticContainer chest = (StaticContainer) obj;
                for (Item item : loot) {
                    chest.addItem(item);
                }
                return;
            }
        }
    }

    private static void setFloor(Map map, int row, int col, StaticObject tile) {
        GridCell cell = map.getCell(row, col);
        if (cell != null) {
            cell.setStaticObject(tile);
        }
    }

    private static void placeTargetObjective(Map map, GameSession session) {
        Random rng = new Random();
        String[] targetNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String name = targetNames[rng.nextInt(targetNames.length)];
        Key target = new Key(name, "A precious artifact — find it to win!", "objective");
        session.setTargetObjective(target);

        int targetSearchableCount = 2 + rng.nextInt(2);
        List<GridCell> searchableSpots = ensureSearchableSpots(map, rng, targetSearchableCount);
        if (searchableSpots.isEmpty()) {
            return;
        }

        GridCell chosen = searchableSpots.get(rng.nextInt(searchableSpots.size()));
        chosen.getStaticObject().setHiddenItem(target);

        List<GridCell> decoyCandidates = new ArrayList<>(searchableSpots);
        decoyCandidates.remove(chosen);
        Collections.shuffle(decoyCandidates, rng);
        int decoyCount = Math.min(targetSearchableCount - 1, decoyCandidates.size());
        for (int i = 0; i < decoyCount; i++) {
            StaticObject so = decoyCandidates.get(i).getStaticObject();
            if (so != null && so.getHiddenItem() == null) {
                so.setHiddenItem(createDecoyHiddenItem(rng));
            }
        }
    }

    private static List<GridCell> ensureSearchableSpots(Map map, Random rng, int minimumCount) {
        List<GridCell> searchable = collectSearchableSpots(map);
        if (searchable.size() >= minimumCount) {
            return searchable;
        }

        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || !cell.isWalkable()) {
                    continue;
                }
                if (cell.getStaticObject() == null || cell.getStaticObject().isSearchable()) {
                    continue;
                }
                if (cell.getOccupyingCharacter() != null) {
                    continue;
                }
                candidates.add(cell);
            }
        }

        Collections.shuffle(candidates, rng);
        int needed = minimumCount - searchable.size();
        for (int i = 0; i < candidates.size() && needed > 0; i++) {
            GridCell cell = candidates.get(i);
            cell.setStaticObject(new StaticObject(
                    "Wall Shelf", "Something might be tucked on the shelf.", true, true));
            searchable.add(cell);
            needed--;
        }
        return searchable;
    }

    private static List<GridCell> collectSearchableSpots(Map map) {
        List<GridCell> searchable = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getStaticObject() != null
                        && cell.getStaticObject().isSearchable()) {
                    searchable.add(cell);
                }
            }
        }
        return searchable;
    }

    private static Item createDecoyHiddenItem(Random rng) {
        switch (rng.nextInt(4)) {
            case 0:
                return new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0);
            case 1:
                return new Consumable("Energy Apple", "Restores energy", 0, 0, 20);
            case 2:
                return new Scroll("Spell Book", "Faint magic", "A spark of healing.", 6);
            default:
                return new Weapon("Dagger", "A small blade", 4);
        }
    }

    private static void carveRoom(Map map, int startRow, int startCol, int height, int width) {
        for (int r = startRow; r < startRow + height && r < map.getHeight(); r++) {
            for (int c = startCol; c < startCol + width && c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null) {
                    cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
                }
            }
        }
    }

    private static void carveHCorridor(Map map, int row, int colFrom, int colTo) {
        int minC = Math.min(colFrom, colTo);
        int maxC = Math.max(colFrom, colTo);
        for (int c = minC; c <= maxC; c++) {
            GridCell cell = map.getCell(row, c);
            if (cell != null) {
                cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }

    private static void carveVCorridor(Map map, int rowFrom, int rowTo, int col) {
        int minR = Math.min(rowFrom, rowTo);
        int maxR = Math.max(rowFrom, rowTo);
        for (int r = minR; r <= maxR; r++) {
            GridCell cell = map.getCell(r, col);
            if (cell != null) {
                cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }
}
