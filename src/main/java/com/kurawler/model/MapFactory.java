package com.kurawler.model;

import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Guard;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
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
 * Builds the story dungeon: The Stolen Relic — three floors.
 *
 * Story: A priceless relic was seized and locked in a warlord's fortress.
 * The player must push through the Gatehouse and the Armory to reach
 * the Inner Vault where the relic is hidden.
 *
 * Floor chain: gatehouse → armory → innervault
 */
public class MapFactory {

    private static final int WORLD_WIDTH  = 22;
    private static final int WORLD_HEIGHT = 16;

    public static final String MAP_GATEHOUSE  = "gatehouse";
    public static final String MAP_ARMORY     = "armory";
    public static final String MAP_INNERVAULT = "innervault";

    // =========================================================
    // PUBLIC ENTRY POINTS
    // =========================================================

    /**
     * Creates a full three-floor story session starting at the Gatehouse.
     * The win objective (the relic) is hidden in the Royal Vault Chest on
     * the final floor; its name is randomised each new game.
     */
    public static GameSession createDefaultMap() {
        Map gatehouse  = buildGatehouse();
        Map armory     = buildArmory();
        Map innervault = buildInnerVault();

        GameSession session = new GameSession(gatehouse);
        session.registerWorldMap(MAP_GATEHOUSE,  gatehouse);
        session.registerWorldMap(MAP_ARMORY,     armory);
        session.registerWorldMap(MAP_INNERVAULT, innervault);
        session.setMap(gatehouse);
        session.setCurrentMapId(MAP_GATEHOUSE);

        Hero hero = new Hero("Adventurer");
        GridCell spawn = gatehouse.getCell(4, 4);
        if (spawn != null) {
            hero.setPosition(4, 4);
            hero.moveToCell(spawn);
        }
        session.setHero(hero);
        session.setAchievementManager(new AchievementManager());
        session.wireObservers();

        Random rng = new Random();

        placeGatehouseLoot(gatehouse, rng);
        placeArmoryLoot(armory, rng);
        placeInnerVaultLoot(innervault, session, rng);

        scatterRandomMapWeapons(gatehouse,  rng, 4 + rng.nextInt(2));
        scatterRandomMapWeapons(armory,     rng, 5 + rng.nextInt(2));
        scatterRandomMapWeapons(innervault, rng, 3 + rng.nextInt(2));

        placeGatehouseEnemies(gatehouse);
        placeArmoryEnemies(armory);
        placeInnerVaultEnemies(innervault);

        syncEnemiesFromMap(gatehouse, session);

        return session;
    }

    /** Configures a win objective on an arbitrary editor-loaded map. */
    public static void configureObjectiveForMap(Map map, GameSession session) {
        List<StaticContainer> chests = collectChests(map);
        if (!chests.isEmpty()) {
            placeTargetInRandomChest(map, session, chests);
        } else {
            placeTargetObjective(map, session);
        }
    }

    // =========================================================
    // FLOOR BUILDERS
    // =========================================================

    /**
     * Floor 1 — The Gatehouse: open courtyard, two Guards on patrol.
     * The Iron Key (locked in a chest near the east wall) unlocks the iron
     * gate that leads to the Armory.
     */
    private static Map buildGatehouse() {
        Map map = new Map(WORLD_WIDTH, WORLD_HEIGHT);
        fillWalls(map);
        carveRoom(map, 1, 1, 13, 19);   // Large courtyard — rows 1-13, cols 1-19

        setFloor(map, 3, 3, new StaticObject(
                "Ancient Bookshelf", "Dusty tomes — search it.", true, true));
        setFloor(map, 5, 12, new StaticObject(
                "Gargoyle Pool", "A shallow pool — something may be inside.", true, true));
        setFloor(map, 11, 8, new StaticObject(
                "Stone Column", "A cracked pillar — search the base.", true, true));

        map.placeObject(6, 15, new StaticContainer("Iron Chest",
                "A heavy iron chest — the Iron Key must be inside."));
        map.placeObject(2,  9, new StaticContainer("Wooden Chest",
                "A dusty wooden chest left by the guards."));

        // East gate to Armory — requires Iron Key
        setFloor(map, 7, 19, new MapPortal(
                "Iron Gate", "A heavy iron gate. You need the Iron Key to pass.",
                MAP_ARMORY, 7, 2, "iron_key", false));

        return map;
    }

    /**
     * Floor 2 — The Armory: split hall with weapons and cursed soldiers.
     * The Vault Key is hidden in a military shelf in the entry hall;
     * it unlocks the passage to the Inner Vault.
     */
    private static Map buildArmory() {
        Map map = new Map(WORLD_WIDTH, WORLD_HEIGHT);
        fillWalls(map);
        carveRoom(map, 1,  1, 6, 19);   // Entry hall — rows 1-6, cols 1-19
        carveRoom(map, 6,  1, 8,  9);   // Left barracks — rows 6-13, cols 1-9
        carveRoom(map, 6, 11, 8,  9);   // Right armory  — rows 6-13, cols 11-19
        carveHCorridor(map, 7, 9, 11);  // Cross-passage at row 7

        // Vault Key hidden in the left shelf (the KEY mechanic for floor 2)
        StaticObject shelf1 = new StaticObject(
                "Armory Shelf", "Military logbooks and equipment — search it.", true, true);
        shelf1.setHiddenItem(new Key(
                "Vault Key", "Opens the passage to the Inner Vault.", "vault_key"));
        setFloor(map, 3, 5, shelf1);

        StaticObject shelf2 = new StaticObject(
                "Armory Shelf", "Rows of dusty supply ledgers.", true, true);
        setFloor(map, 3, 14, shelf2);

        StaticObject col = new StaticObject(
                "Stone Column", "A supporting pillar — search the base.", true, true);
        col.setHiddenItem(new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        setFloor(map, 10, 6, col);

        setFloor(map, 10, 16, new StaticObject(
                "Gargoyle Pool", "Stagnant, dark water.", true, true));

        map.placeObject(11,  5, new StaticContainer("Footlocker",
                "A soldier's heavy footlocker."));
        map.placeObject( 2, 18, new StaticContainer("Supply Chest",
                "A locked supply chest."));

        // Back portal (west) → Gatehouse
        setFloor(map, 7, 1, new MapPortal(
                "Wooden Door", "Returns to the Gatehouse.",
                MAP_GATEHOUSE, 7, 18, null, true));
        // Forward portal (east) → Inner Vault — requires Vault Key
        setFloor(map, 7, 19, new MapPortal(
                "Reinforced Door", "Sealed passage to the Inner Vault. Needs the Vault Key.",
                MAP_INNERVAULT, 7, 2, "vault_key", false));

        return map;
    }

    /**
     * Floor 3 — The Inner Vault: grand final chamber, the relic is here.
     * One Sorcerer and two Knights guard the Royal Vault Chest.
     * Opening the chest and claiming the relic wins the game.
     */
    private static Map buildInnerVault() {
        Map map = new Map(WORLD_WIDTH, WORLD_HEIGHT);
        fillWalls(map);
        carveRoom(map, 1, 1, 13, 19);   // Grand vault hall — rows 1-13, cols 1-19

        // Decorative impassable pillars flanking the vault chest
        setFloor(map, 3,  5, new StaticObject("Stone Pillar", "A heavy stone column.", false, false));
        setFloor(map, 3, 15, new StaticObject("Stone Pillar", "A heavy stone column.", false, false));

        // Searchable props (decoys + small rewards)
        setFloor(map, 11,  5, new StaticObject(
                "Gargoyle Pool", "Cold, dark water — search it.", true, true));
        setFloor(map, 11, 15, new StaticObject(
                "Gargoyle Pool", "The water glows faintly — search it.", true, true));
        setFloor(map,  8,  3, new StaticObject(
                "Ancient Bookshelf", "Crumbling tomes of forbidden lore.", true, true));

        // Royal Vault Chest — relic (win objective) placed in placeInnerVaultLoot
        map.placeObject( 2, 12, new StaticContainer("Royal Vault Chest",
                "A sealed vault chest. This must be where the relic is kept."));
        map.placeObject(11, 17, new StaticContainer("Wooden Chest",
                "A battered guard chest."));

        // Back portal only (no exit — player wins by claiming the relic)
        setFloor(map, 7, 1, new MapPortal(
                "Reinforced Door", "Returns to the Armory.",
                MAP_ARMORY, 7, 18, null, true));

        return map;
    }

    // =========================================================
    // LOOT PLACEMENT
    // =========================================================

    private static void placeGatehouseLoot(Map map, Random rng) {
        // Iron Chest: Iron Key + a health potion to soften the blow of a first fight
        populateChest(map, 6, 15,
                new Key("Iron Key",
                        "The key to the iron gate — unlocks the passage to the Armory.",
                        "iron_key"),
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));

        // Wooden Chest: early survival supplies
        populateChest(map, 2, 9,
                new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35),
                new Wearable("Leather Armor", "Basic padded armor", 1));

        // Searchable hidden items
        hiddenIn(map, 3, 3,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        hiddenIn(map, 5, 12,
                new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35));
        hiddenIn(map, 11, 8,
                new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0));

        // Floor loot
        map.placeObject( 2,  6, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        map.placeObject( 9, 14, new Consumable("Mana Potion",   "Restores 30 mana", 0, 30, 0));
        map.placeObject(13,  3, new Scroll("Spell Book", "An arcane tome",
                "A healing warmth washes over you.", 12));
    }

    private static void placeArmoryLoot(Map map, Random rng) {
        // Footlocker: heavier armor available here
        populateChest(map, 11, 5,
                new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0),
                new Wearable("Chain Armor", "Heavy linked chain armor", 2));

        // Supply chest: healing + scroll
        populateChest(map, 2, 18,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0),
                new Scroll("Healing Scroll", "Glowing parchment",
                        "Wounds close rapidly.", 14));

        // Decoy items in searchable props
        hiddenIn(map, 3, 14,
                new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35));
        hiddenIn(map, 10, 16,
                new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0));

        // Floor loot
        map.placeObject( 2, 16, new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));
        map.placeObject( 9,  3, new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35));
        map.placeObject(12, 12, new Scroll("Spell Book", "Arcane tome",
                "Light mends your wounds.", 10));
    }

    private static void placeInnerVaultLoot(Map map, GameSession session, Random rng) {
        // Win objective hidden in the Royal Vault Chest
        String[] relicNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String relicName = relicNames[rng.nextInt(relicNames.length)];
        Key relic = new Key(relicName,
                "The stolen relic — claim it to win!", "objective");
        session.setTargetObjective(relic);

        populateChest(map, 2, 12,
                relic,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0),
                new Wearable("Royal Plate", "Gleaming kingly armor", 4));

        // Side chest: strong healing scroll for the final fight
        populateChest(map, 11, 17,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0),
                new Scroll("Ancient Spell", "Tattered but still glowing",
                        "A surge of restoration fills you.", 22));

        // Searchable decoys
        hiddenIn(map, 11,  5,
                new Consumable("Energy Potion", "Restores 35 energy", 0, 0, 35));
        hiddenIn(map, 11, 15,
                new Consumable("Mana Potion", "Restores 30 mana", 0, 30, 0));
        hiddenIn(map,  8,  3,
                new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0));

        // Floor loot
        map.placeObject( 5,  8, new Consumable("Health Potion",  "Restores 20 HP",      20,  0,  0));
        map.placeObject(10, 14, new Consumable("Energy Potion",  "Restores 35 energy",   0,  0, 35));
        map.placeObject( 5, 14, new Consumable("Mana Potion",    "Restores 30 mana",     0, 30,  0));
        map.placeObject( 9,  6, new Wearable("Enchanted Plate",  "Gleaming enchanted plate armor", 3));
    }

    // =========================================================
    // ENEMY PLACEMENT
    // =========================================================

    private static void placeGatehouseEnemies(Map map) {
        placeEnemy(map, new Guard(),  5,  7);
        placeEnemy(map, new Guard(), 10, 11);
    }

    private static void placeArmoryEnemies(Map map) {
        placeEnemy(map, new Guard(),   2,  5);
        placeEnemy(map, new Guard(),   4, 16);
        placeEnemy(map, new Knight(),  9,  5);
    }

    private static void placeInnerVaultEnemies(Map map) {
        placeEnemy(map, new Sorcerer(),  4, 14);
        placeEnemy(map, new Knight(),    6,  4);
        placeEnemy(map, new Knight(),    9, 12);
    }

    private static void placeEnemy(Map map, Enemy enemy, int row, int col) {
        GridCell cell = map.getCell(row, col);
        if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
            enemy.moveToCell(cell);
        }
    }

    /**
     * Clears the session enemy list and re-registers every living enemy
     * currently occupying a cell on the given map.
     * Called at game start and by {@link GameSession#transitionHeroTo}.
     */
    static void syncEnemiesFromMap(Map map, GameSession session) {
        session.clearEnemies();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) continue;
                if (cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy e = (Enemy) cell.getOccupyingCharacter();
                    if (e.isAlive()) session.addEnemy(e);
                }
            }
        }
    }

    // =========================================================
    // EDITOR OBJECTIVE HELPERS
    // =========================================================

    private static void placeTargetInRandomChest(Map map, GameSession session,
                                                  List<StaticContainer> chests) {
        if (chests.isEmpty()) return;
        Random rng = new Random();
        String[] targetNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String name = targetNames[rng.nextInt(targetNames.length)];
        Key target = new Key(name, "A precious artifact — find it to win!", "objective");
        session.setTargetObjective(target);
        chests.get(rng.nextInt(chests.size())).addItem(target);
    }

    private static void placeTargetObjective(Map map, GameSession session) {
        Random rng = new Random();
        String[] targetNames = {"Crystal Orb", "Ancient Relic", "Dragon Gem", "Golden Idol"};
        String name = targetNames[rng.nextInt(targetNames.length)];
        Key target = new Key(name, "A precious artifact — find it to win!", "objective");
        session.setTargetObjective(target);

        int targetSearchableCount = 2 + rng.nextInt(2);
        List<GridCell> searchableSpots = ensureSearchableSpots(map, rng, targetSearchableCount);
        if (searchableSpots.isEmpty()) return;

        GridCell chosen = searchableSpots.get(rng.nextInt(searchableSpots.size()));
        chosen.getStaticObject().setHiddenItem(target);

        List<GridCell> decoyCandidates = new ArrayList<>(searchableSpots);
        decoyCandidates.remove(chosen);
        Collections.shuffle(decoyCandidates, rng);
        int decoyCount = Math.min(targetSearchableCount - 1, decoyCandidates.size());
        for (int i = 0; i < decoyCount; i++) {
            StaticObject so = decoyCandidates.get(i).getStaticObject();
            if (so != null && so.getHiddenItem() == null)
                so.setHiddenItem(createDecoyHiddenItem(rng));
        }
    }

    private static List<GridCell> ensureSearchableSpots(Map map, Random rng, int minimumCount) {
        List<GridCell> searchable = collectSearchableSpots(map);
        if (searchable.size() >= minimumCount) return searchable;

        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || !cell.isWalkable()) continue;
                if (cell.getStaticObject() == null || cell.getStaticObject().isSearchable()) continue;
                if (cell.getOccupyingCharacter() != null) continue;
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
                        && cell.getStaticObject().isSearchable())
                    searchable.add(cell);
            }
        }
        return searchable;
    }

    private static Item createDecoyHiddenItem(Random rng) {
        switch (rng.nextInt(4)) {
            case 0:  return new Consumable("Health Potion",  "Restores 20 HP",      20,  0,  0);
            case 1:  return new Consumable("Energy Potion",  "Restores 35 energy",   0,  0, 35);
            case 2:  return new Scroll("Spell Book", "Faint magic",
                                        "A spark of healing.", 6);
            default: return new Weapon("Dagger", "A small blade", 4);
        }
    }

    // =========================================================
    // WEAPON SCATTER
    // =========================================================

    private static void scatterRandomMapWeapons(Map map, Random rng, int count) {
        List<String> pool = new ArrayList<>();
        for (String[] row : WeaponCatalog.ENTRIES) {
            if (row != null && row.length > 0) pool.add(row[0]);
        }
        Collections.shuffle(pool, rng);
        List<String> chosen = new ArrayList<>();
        for (String name : pool) {
            if (chosen.size() >= count) break;
            if (!chosen.contains(name)) chosen.add(name);
        }
        ensureBowWithArrow(chosen, pool, rng);
        placeWeaponBatch(map, rng, chosen);
    }

    private static void ensureBowWithArrow(List<String> chosen, List<String> pool, Random rng) {
        boolean hasArrow = false, hasBow = false;
        for (String n : chosen) {
            if (WeaponCatalog.isArrow(n)) hasArrow = true;
            if (WeaponCatalog.isBow(n))   hasBow   = true;
        }
        if (hasArrow && !hasBow) chosen.add("Bow");
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
        if (weaponNames.isEmpty()) return;
        List<GridCell> floors = collectEmptyFloorTiles(map);
        Collections.shuffle(floors, rng);
        int floorIdx = 0;
        for (String name : weaponNames) {
            while (floorIdx < floors.size()) {
                GridCell cell = floors.get(floorIdx++);
                if (isNear(cell, 4, 4, 2)) continue;
                map.placeObject(cell.getRow(), cell.getCol(), WeaponCatalog.create(name));
                break;
            }
        }
    }

    // =========================================================
    // MAP / CELL UTILITIES
    // =========================================================

    private static void fillWalls(Map map) {
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                map.getCell(r, c).setStaticObject(
                        new StaticObject("Wall", "Dark stone wall", false, false));
            }
        }
    }

    private static void carveRoom(Map map, int startRow, int startCol, int height, int width) {
        for (int r = startRow; r < startRow + height && r < map.getHeight(); r++) {
            for (int c = startCol; c < startCol + width && c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell != null)
                    cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
            }
        }
    }

    private static void carveHCorridor(Map map, int row, int colFrom, int colTo) {
        int minC = Math.min(colFrom, colTo);
        int maxC = Math.max(colFrom, colTo);
        for (int c = minC; c <= maxC; c++) {
            GridCell cell = map.getCell(row, c);
            if (cell != null)
                cell.setStaticObject(new StaticObject("Floor", "Stone floor", true, false));
        }
    }

    private static void setFloor(Map map, int row, int col, StaticObject tile) {
        GridCell cell = map.getCell(row, col);
        if (cell != null) cell.setStaticObject(tile);
    }

    private static void hiddenIn(Map map, int row, int col, Item item) {
        GridCell cell = map.getCell(row, col);
        if (cell != null && cell.getStaticObject() != null)
            cell.getStaticObject().setHiddenItem(item);
    }

    private static void populateChest(Map map, int row, int col, Item... loot) {
        GridCell cell = map.getCell(row, col);
        if (cell == null) return;
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer) {
                StaticContainer chest = (StaticContainer) obj;
                for (Item item : loot) chest.addItem(item);
                return;
            }
        }
    }

    private static List<StaticContainer> collectChests(Map map) {
        List<StaticContainer> chests = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null) continue;
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof StaticContainer) chests.add((StaticContainer) obj);
                }
            }
        }
        return chests;
    }

    private static List<GridCell> collectEmptyFloorTiles(Map map) {
        List<GridCell> floors = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (isEmptyFloorTile(cell)) floors.add(cell);
            }
        }
        return floors;
    }

    private static boolean isEmptyFloorTile(GridCell cell) {
        if (cell == null || !cell.isWalkable() || cell.hasObjects()) return false;
        if (cell.getOccupyingCharacter() != null) return false;
        StaticObject so = cell.getStaticObject();
        if (so == null || so instanceof MapPortal) return false;
        if (so.isSearchable()) return false;
        return "Floor".equals(so.getName());
    }

    private static boolean isNear(GridCell cell, int row, int col, int radius) {
        return Math.abs(cell.getRow() - row) <= radius
                && Math.abs(cell.getCol() - col) <= radius;
    }
}
