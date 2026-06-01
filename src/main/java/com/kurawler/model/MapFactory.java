package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
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

    /**
     * Story map size — matches play viewport (1248×810 window, 48px tiles,
     * 100px HUD, 34px info bar): 26×14 cells.
     */
    private static final int WORLD_WIDTH = 26;
    private static final int WORLD_HEIGHT = 14;
    /** Keep loot off the wall ring and the tile right beside it. */
    private static final int LOOT_INSET = 2;

    /**
     * Chest names whose closed sprite and "Open …" sprite both exist in the container
     * atlas (see ResourceAssetCatalog.CONTAINER_TILES + containerSpriteName), so a chest
     * looks like the same chest whether it is closed or opened.
     */
    private static final String[] LOOT_CHEST_NAMES = {
            "Gold Banded Chest", "Bronze Treasure Chest", "Copper Chest",
            "Orange Chest", "Blue Banded Chest"
    };

    public static GameSession createDefaultMap() {
        Random doorRng = new Random();
        DoorSlot hallToCat = randomPerimeterDoor(doorRng);
        DoorSlot catFromHall = oppositePerimeterDoor(hallToCat);
        DoorSlot catToVault = randomPerimeterDoor(doorRng);
        int guard = 0;
        while (sameDoorSlot(catFromHall, catToVault) && guard++ < 20) {
            catToVault = randomPerimeterDoor(doorRng);
        }
        DoorSlot vaultFromCat = oppositePerimeterDoor(catToVault);

        Map hall = buildEntryHall(hallToCat);
        Map catacombs = buildCatacombs(catFromHall, catToVault, hallToCat);
        Map vault = buildTreasureVault(vaultFromCat, catToVault);

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

        populateStoryBookshelfLoot(hall, catacombs);
        fillBookshelfLoot(vault, 3, 4, new Random(), false);

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

    /**
     * Restarts the floor the hero is on: removes enemies/projectiles and moves the hero
     * back to that map's default spawn. Map layout and loot are left as-is.
     */
    public static boolean restartCurrentFloor(GameSession session) {
        if (session == null) {
            return false;
        }
        Hero hero = session.getHero();
        Map map = session.getMap();
        if (hero == null || map == null) {
            return false;
        }

        purgeEnemiesFromMap(map);
        session.clearEnemies();
        session.getProjectiles().clear();
        session.setShadowClone(null);

        GridCell spawn = defaultSpawnCell(map, session.getCurrentMapId());
        if (spawn == null) {
            return false;
        }
        if (hero.getCurrentCell() != null) {
            hero.getCurrentCell().setOccupyingCharacter(null);
        }
        hero.moveToCell(spawn);
        return true;
    }

    private static void purgeEnemiesFromMap(Map map) {
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.getOccupyingCharacter() instanceof Enemy) {
                    cell.setOccupyingCharacter(null);
                }
            }
        }
    }

    private static GridCell defaultSpawnCell(Map map, String mapId) {
        if ("hall".equals(mapId)) {
            GridCell hallSpawn = map.getCell(4, 4);
            if (hallSpawn != null && hallSpawn.isWalkable()) {
                return hallSpawn;
            }
        }
        for (int r = 1; r < map.getHeight() - 1; r++) {
            for (int c = 1; c < map.getWidth() - 1; c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
                    return cell;
                }
            }
        }
        return null;
    }

    // --- Three connected maps (PDF-style dungeon, not a tiny grid puzzle) ---

    /** Solid walls on the outer border; playable floor one tile inside (classic dungeon room). */
    private static Map newWorldMap() {
        Map map = new Map(WORLD_WIDTH, WORLD_HEIGHT);
        fillWalls(map);
        carveInterior(map);
        return map;
    }

    private static void carveInterior(Map map) {
        for (int r = 1; r < map.getHeight() - 1; r++) {
            for (int c = 1; c < map.getWidth() - 1; c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null) {
                    cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
                }
            }
        }
    }

    /** Map-transition door embedded in the outer wall ring. */
    private static void placeWallPortal(Map map, int row, int col, MapPortal portal) {
        GridCell cell = map.getCell(row, col);
        if (cell != null && portal != null) {
            cell.setStaticObject(portal);
        }
    }

    /** A door tile on the outer wall ring. */
    private static final class DoorSlot {
        final int row;
        final int col;

        DoorSlot(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    /** Random door on north, south, east, or west wall (not on corners). */
    private static DoorSlot randomPerimeterDoor(Random rng) {
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0:
                return new DoorSlot(0, 1 + rng.nextInt(WORLD_WIDTH - 2));
            case 1:
                return new DoorSlot(WORLD_HEIGHT - 1, 1 + rng.nextInt(WORLD_WIDTH - 2));
            case 2:
                return new DoorSlot(1 + rng.nextInt(WORLD_HEIGHT - 2), WORLD_WIDTH - 1);
            default:
                return new DoorSlot(1 + rng.nextInt(WORLD_HEIGHT - 2), 0);
        }
    }

    /** Opposite wall tile so the linked map door faces this one. */
    private static DoorSlot oppositePerimeterDoor(DoorSlot door) {
        if (door.row == 0) {
            return new DoorSlot(WORLD_HEIGHT - 1, door.col);
        }
        if (door.row == WORLD_HEIGHT - 1) {
            return new DoorSlot(0, door.col);
        }
        if (door.col == 0) {
            return new DoorSlot(door.row, WORLD_WIDTH - 1);
        }
        return new DoorSlot(door.row, 0);
    }

    private static boolean sameDoorSlot(DoorSlot a, DoorSlot b) {
        return a.row == b.row && a.col == b.col;
    }

    private static Map buildEntryHall(DoorSlot exitToCatacombs) {
        Map map = newWorldMap();
        DoorSlot catEntry = oppositePerimeterDoor(exitToCatacombs);

        placeBookshelf(map, 3, 3,
                "Ancient Bookshelf", "Dusty tomes line the shelves — press E to browse.");
        setFloor(map, 10, 5, new StaticObject(
                "Gargoyle Pool", "A shallow pool — something may be hidden inside.", true, true));
        setFloor(map, 12, 8, new StaticObject(
                "Stone Column", "A cracked pillar — search the base.", false, true));

        // Breakable floor prop: smash it open (BREAK) to reveal what's inside.
        StaticObject hallCrate = new StaticObject(
                "Wooden Crate", "A flimsy crate — could be smashed open.", false, true);
        hallCrate.setHiddenItem(new Consumable("Health Potion", "Restores 10 HP",
                Consumable.HEALTH_POTION_HEAL, 0, 0));
        setFloor(map, 6, 10, hallCrate);

        placeWallPortal(map, exitToCatacombs.row, exitToCatacombs.col, new MapPortal(
                "Wooden Door", "Leads deeper into the dungeon.",
                "catacombs", catEntry.row, catEntry.col, null, true));

        return map;
    }

    private static Map buildCatacombs(DoorSlot hallLink, DoorSlot vaultLink, DoorSlot hallReturn) {
        Map map = newWorldMap();
        DoorSlot vaultEntry = oppositePerimeterDoor(vaultLink);

        placeBookshelf(map, 4, 4,
                "Wall Shelf", "Ledges along the wall — press E to browse.");
        setFloor(map, 10, 5, new StaticObject(
                "Gargoyle Pool", "Stagnant water glistens.", true, true));
        StaticObject catacombsPool = map.getCell(10, 5).getStaticObject();
        if (catacombsPool != null) {
            catacombsPool.setHiddenItem(new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0));
        }
        setFloor(map, 12, 8, new StaticObject(
                "Stone Column", "A cracked pillar — search the base.", false, true));

        StaticObject catCrate = new StaticObject(
                "Barrel", "An old barrel — smash it to see inside.", false, true);
        catCrate.setHiddenItem(new Consumable("Mana Potion", "Restores 25 mana", 0, 25, 0));
        setFloor(map, 6, 11, catCrate);

        placeWallPortal(map, hallLink.row, hallLink.col, new MapPortal(
                "Wooden Door", "Back to the entry hall.",
                "hall", hallReturn.row, hallReturn.col, null, true));

        placeWallPortal(map, vaultLink.row, vaultLink.col, new MapPortal(
                "Iron Door", "A heavy door to the treasure vault.",
                "vault", vaultEntry.row, vaultEntry.col, "gold", false));

        return map;
    }

    private static Map buildTreasureVault(DoorSlot catacombsLink, DoorSlot catacombsReturn) {
        Map map = newWorldMap();

        placeBookshelf(map, 3, 4,
                "Wall Shelf", "Relics rest on stone shelves — press E to browse.");
        setFloor(map, 10, 6, new StaticObject(
                "Gargoyle Pool", "A dark pool — search the water.", true, true));
        setFloor(map, 12, 10, new StaticObject(
                "Stone Column", "Ancient carvings cover the column.", false, true));

        StaticObject vaultCrate = new StaticObject(
                "Wooden Crate", "A reinforced crate — break it open.", false, true);
        vaultCrate.setHiddenItem(new Consumable("Energy Potion", "Restores 40 energy",
                0, 0, Consumable.ENERGY_POTION_ENERGY));
        setFloor(map, 6, 12, vaultCrate);

        placeWallPortal(map, catacombsLink.row, catacombsLink.col, new MapPortal(
                "Iron Door", "Return to the catacombs.",
                "catacombs", catacombsReturn.row, catacombsReturn.col, null, true));

        return map;
    }

    /** Describes the primary exit portal direction for the info bar. */
    public static String primaryExitHint(Map map, String mapId) {
        if (map == null || mapId == null) {
            return "";
        }
        String forwardTarget;
        switch (mapId) {
            case "hall":       forwardTarget = "catacombs"; break;
            case "catacombs":  forwardTarget = "vault"; break;
            case "vault":      forwardTarget = "catacombs"; break;
            default:           return "Find the exit door.";
        }
        int lastRow = map.getHeight() - 1;
        int lastCol = map.getWidth() - 1;
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                StaticObject so = map.getCell(r, c).getStaticObject();
                if (!(so instanceof MapPortal)) {
                    continue;
                }
                MapPortal portal = (MapPortal) so;
                if (!forwardTarget.equals(portal.getTargetMapId())) {
                    continue;
                }
                String dir;
                if (c == lastCol) {
                    dir = "east";
                } else if (c == 0) {
                    dir = "west";
                } else if (r == 0) {
                    dir = "north";
                } else if (r == lastRow) {
                    dir = "south";
                } else {
                    dir = "nearby";
                }
                if ("hall".equals(mapId)) {
                    return "EXIT: " + dir + " DOOR (\u2192). Press E on a bookshelf for the Golden Key. "
                            + "Win item is in a vault chest.";
                }
                return "EXIT: " + dir + " DOOR (\u2192).";
            }
        }
        return "Find the exit door.";
    }

    private static void fillWalls(Map map) {
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                map.getCell(r, c).setStaticObject(
                        new StaticObject("Wall", "Dark stone wall", false, false));
            }
        }
    }

    /** Floor + chest loot spread randomly across each map (not clustered in one corner). */
    private static void scatterDefaultLoot(Map hall, Map catacombs, Map vault) {
        Random rng = new Random();
        scatterFloorLoot(hall, rng, new GameObject[]{
                new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0),
                new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0),
                new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY),
                new Consumable("Mana Potion", "Restores 25 mana", 0, 25, 0),
                new Scroll("Spell Book", "Arcane tome", "Runes mend your wounds.", 12),
                WearableCatalog.createArmor("Leather Armor", "Leather armor", rng),
                WearableCatalog.createShield("Shield"),
        });
        scatterChestLoot(hall, rng, new ChestLoot[]{
                new ChestLoot("A dusty chest",
                        new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY),
                        new Key("Silver Key", "Ornamental key", "silver")),
        });

        scatterFloorLoot(catacombs, rng, new GameObject[]{
                new Scroll("Spell Book", "Arcane tome", "Runes flare and mend your wounds.", 14),
                new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0),
                new Consumable("Mana Potion", "Restores 25 mana", 0, 25, 0),
                new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY),
                WearableCatalog.createArmor("Chain Mail", "Worn chain mail", rng),
                new Scroll("Healing Scroll", "Glowing parchment", "Heals wounds.", 10),
        });
        scatterChestLoot(catacombs, rng, new ChestLoot[]{
                new ChestLoot("An old wooden chest",
                        new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0),
                        WearableCatalog.createArmor("Heavy Armor", "Heavy armor", rng)),
        });

        scatterFloorLoot(vault, rng, new GameObject[]{
                new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0),
                new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0),
                new Scroll("Spell Book", "Ancient scripture",
                        "Warm light restores your wounds.", 10),
                WearableCatalog.createArmor("Royal Armor", "Royal armor", rng),
        });
        scatterChestLoot(vault, rng, new ChestLoot[]{
                new ChestLoot("A sealed treasure chest",
                        new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY)),
                new ChestLoot("An ornate chest",
                        new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0),
                        new Scroll("Spell Book", "Faded runes", "A spark of healing.", 8)),
        });
    }

    private static final class ChestLoot {
        final String description;
        final Item[] items;

        ChestLoot(String description, Item... items) {
            this.description = description;
            this.items = items;
        }
    }

    private static void scatterFloorLoot(Map map, Random rng, GameObject[] loot) {
        List<GridCell> floors = collectLootFloorTiles(map);
        Collections.shuffle(floors, rng);
        int idx = 0;
        for (GameObject obj : loot) {
            while (idx < floors.size()) {
                GridCell cell = floors.get(idx++);
                if (cell.hasObjects()) {
                    continue;
                }
                map.placeObject(cell.getRow(), cell.getCol(), obj);
                break;
            }
        }
    }

    private static void scatterChestLoot(Map map, Random rng, ChestLoot[] chests) {
        List<GridCell> floors = collectLootFloorTiles(map);
        Collections.shuffle(floors, rng);
        int idx = 0;
        for (ChestLoot chest : chests) {
            while (idx < floors.size()) {
                GridCell cell = floors.get(idx++);
                if (cell.hasObjects()) {
                    continue;
                }
                int r = cell.getRow();
                int c = cell.getCol();
                String chestName = LOOT_CHEST_NAMES[rng.nextInt(LOOT_CHEST_NAMES.length)];
                map.placeObject(r, c, new StaticContainer(chestName, chest.description));
                populateChest(map, r, c, chest.items);
                break;
            }
        }
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
        List<GridCell> floors = collectLootFloorTiles(map);
        Collections.shuffle(floors, rng);
        int floorIdx = 0;
        for (String name : weaponNames) {
            while (floorIdx < floors.size()) {
                GridCell cell = floors.get(floorIdx++);
                if (cell.hasObjects()) {
                    continue;
                }
                map.placeObject(cell.getRow(), cell.getCol(), WeaponCatalog.create(name));
                break;
            }
        }
    }

    private static List<GridCell> collectLootFloorTiles(Map map) {
        List<GridCell> floors = new ArrayList<>();
        for (int r = LOOT_INSET; r < map.getHeight() - LOOT_INSET; r++) {
            for (int c = LOOT_INSET; c < map.getWidth() - LOOT_INSET; c++) {
                GridCell cell = map.getCell(r, c);
                if (isLootFloorTile(cell, map) && !cell.hasObjects()) {
                    floors.add(cell);
                }
            }
        }
        return floors;
    }

    private static boolean isLootFloorTile(GridCell cell, Map map) {
        if (cell == null || !cell.isWalkable()) {
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
        if (!"Floor".equals(so.getName())) {
            return false;
        }
        int r = cell.getRow();
        int c = cell.getCol();
        return r >= LOOT_INSET && r < map.getHeight() - LOOT_INSET
                && c >= LOOT_INSET && c < map.getWidth() - LOOT_INSET;
    }

    /**
     * Bookshelf prop + loot container on the same tile (opened like a chest with E).
     */
    private static void placeBookshelf(Map map, int row, int col, String propName, String description) {
        // Searchable flag drives sparkle markers; loot is opened via the Bookshelf container (E / OPEN).
        setFloor(map, row, col, new StaticObject(propName, description, true, true));
        GridCell cell = map.getCell(row, col);
        if (cell != null) {
            cell.addObject(new StaticContainer("Bookshelf", description));
        }
    }

    private static StaticContainer findBookshelfContainer(Map map, int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return null;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer && ((StaticContainer) obj).isShelfLike()) {
                return (StaticContainer) obj;
            }
        }
        return null;
    }

    /**
     * Each story bookshelf holds 1–2 items. The Golden Key (vault iron door) is placed in
     * either the Entry Hall bookshelf or the Catacombs shelf (random each new game).
     */
    private static void populateStoryBookshelfLoot(Map hall, Map catacombs) {
        Random rng = new Random();
        boolean keyInHall = rng.nextBoolean();
        fillBookshelfLoot(hall, 3, 3, rng, keyInHall);
        fillBookshelfLoot(catacombs, 4, 4, rng, !keyInHall);
    }

    private static void fillBookshelfLoot(Map map, int row, int col, Random rng, boolean withVaultKey) {
        StaticContainer shelf = findBookshelfContainer(map, row, col);
        if (shelf == null) {
            return;
        }
        if (withVaultKey) {
            shelf.addItem(new Key("Golden Key", "Opens the iron vault door", "gold"));
        }
        int lootCount = 1 + rng.nextInt(2);
        java.util.HashSet<String> placed = new java.util.HashSet<>();
        for (Item existing : shelf.getContents()) {
            if (existing != null) {
                placed.add(existing.getName());
            }
        }
        for (int i = 0; i < lootCount; i++) {
            for (int attempt = 0; attempt < 8; attempt++) {
                Item item = createBookshelfLootItem(rng);
                if (!placed.contains(item.getName())) {
                    shelf.addItem(item);
                    placed.add(item.getName());
                    break;
                }
            }
        }
    }

    private static Item createBookshelfLootItem(Random rng) {
        switch (rng.nextInt(5)) {
            case 0:
                return new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0);
            case 1:
                return new Consumable("Mana Potion", "Restores 25 mana", 0, 25, 0);
            case 2:
                return new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY);
            case 3:
                return new Scroll("Spell Book", "Arcane tome", "Runes mend your wounds.", 10);
            default:
                return new Scroll("Healing Scroll", "Glowing parchment", "Heals wounds.", 10);
        }
    }

    /**
     * Win target (objective key) inside a random chest on the treasure vault.
     * Golden Key for the vault door is in a story bookshelf — see {@link #populateStoryBookshelfLoot}.
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
                return new Consumable("Health Potion", "Restores 10 HP", Consumable.HEALTH_POTION_HEAL, 0, 0);
            case 1:
                return new Consumable("Energy Potion", "Restores 40 EN", 0, 0, Consumable.ENERGY_POTION_ENERGY);
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

    public static boolean isShelfStaticObject(StaticObject so) {
        if (so == null) {
            return false;
        }
        String n = so.getName().toLowerCase();
        return n.contains("bookshelf") || n.contains("shelf");
    }

    /** Floor props that can be smashed (crate/barrel), not bookshelves/pools/columns. */
    public static boolean isBreakableStaticObject(StaticObject so) {
        if (so == null || !so.isSearchable() || isShelfStaticObject(so)) {
            return false;
        }
        String n = so.getName().toLowerCase();
        return n.contains("crate") || n.contains("barrel") || n.contains("box");
    }

    public static boolean hasBreakableTarget(GridCell cell) {
        if (cell == null) {
            return false;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer sc && sc.isBreakable() && !sc.isBroken()) {
                return true;
            }
        }
        return isBreakableStaticObject(cell.getStaticObject());
    }

    /** Returns the bookshelf container on the tile, creating one if only a legacy hidden-item shelf exists. */
    public static StaticContainer resolveBookshelfContainer(Map map, int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) {
            return null;
        }
        StaticContainer existing = findBookshelfContainer(map, row, col);
        if (existing != null) {
            return existing;
        }
        StaticObject so = cell.getStaticObject();
        if (!isShelfStaticObject(so)) {
            return null;
        }
        StaticContainer shelf = new StaticContainer("Bookshelf", so.getDescription());
        Item hidden = so.revealHiddenItem();
        if (hidden != null) {
            shelf.addItem(hidden);
        }
        cell.addObject(shelf);
        return shelf;
    }
}
