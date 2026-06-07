package com.kurawler.persistence;

import com.google.gson.Gson;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.achievement.Achievement;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.ShadowCloneScroll;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Serializes game state and maps to JSON using Gson (Phase 2 requirement). */
public class MapSerializer {

    private final Gson gson = JsonPersistence.gson();

    public boolean saveGameState(GameSession session, String filename) {
        String saveName = deriveSaveName(filename);
        return saveGameState(session, filename, saveName);
    }

    public boolean saveGameState(GameSession session, String filename, String saveName) {
        try {
            ensureParentDirectory(filename);
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(gson.toJson(buildStateMap(session, saveName)));
            }
            System.out.println("[Persistence] Saved game state to " + filename);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String deriveSaveName(String filename) {
        String base = Paths.get(filename).getFileName().toString();
        return base.endsWith(".json") ? base.substring(0, base.length() - 5) : base;
    }

    /** Persists the underlying game mode (TeamMatch / Play), not transient UI states like Paused. */
    private String resolveGameType(GameSession session) {
        if (session instanceof com.kurawler.model.TeamMatchSession) {
            return "TeamMatch";
        }
        String mode = session.getCurrentMode();
        if ("Play".equals(mode) || "Design".equals(mode)) {
            return mode;
        }
        return "Play";
    }

    private LinkedHashMap<String, Object> buildStateMap(GameSession session, String saveName) {
        syncActiveMapIntoWorld(session);
        Map map = session.getMap();
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("saveName", saveName);
        root.put("savedAt", Instant.now().toString());
        root.put("width", map.getWidth());
        root.put("height", map.getHeight());
        root.put("playDurationTimer", session.getPlayDurationTimer());
        root.put("gameType", resolveGameType(session));
        root.put("currentMode", session.getCurrentMode());
        root.put("difficulty", session.getDifficulty());
        root.put("enemiesDefeatedCount", session.getEnemiesDefeatedCount());
        root.put("itemsCollectedCount", session.getItemsCollectedCount());
        root.put("floorsClearedCount", session.getFloorsClearedCount());
        root.put("lastShadowCloneScrollSpawnMs", session.getLastShadowCloneScrollSpawnMs());
        root.put("enemySpawnAccumulatorMs", session.getEnemySpawnAccumulatorMs());
        if (session.getCurrentMapId() != null && !session.getCurrentMapId().isBlank()) {
            root.put("currentMapId", session.getCurrentMapId());
        }
        root.put("targetObjective", session.getTargetObjective() != null
                ? buildObjectMap(session.getTargetObjective()) : null);
        root.put("hero", buildHeroMap(session.getHero()));
        root.put("shadowClone", buildShadowCloneMap(session.getShadowClone()));
        root.put("achievements", buildAchievementsMap(session.getAchievementManager()));
        root.put("enemies", buildEnemiesList(session));
        root.put("cells", buildCellsList(map));
        if (session.getWorldMaps().size() > 1) {
            root.put("worldMaps", buildWorldMapsMap(session));
        }
        return root;
    }

    private void syncActiveMapIntoWorld(GameSession session) {
        String mapId = session.getCurrentMapId();
        if (mapId != null && !mapId.isBlank() && session.getMap() != null) {
            session.registerWorldMap(mapId, session.getMap());
        }
    }

    private LinkedHashMap<String, Object> buildWorldMapsMap(GameSession session) {
        LinkedHashMap<String, Object> worlds = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Map> entry : session.getWorldMaps().entrySet()) {
            worlds.put(entry.getKey(), buildMapSnapshotMap(entry.getValue()));
        }
        return worlds;
    }

    private LinkedHashMap<String, Object> buildMapSnapshotMap(Map map) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("width", map.getWidth());
        snapshot.put("height", map.getHeight());
        snapshot.put("cells", buildCellsList(map));
        return snapshot;
    }

    private List<LinkedHashMap<String, Object>> buildCellsList(Map map) {
        List<LinkedHashMap<String, Object>> entries = new ArrayList<>();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (shouldSerializeCell(cell)) {
                    entries.add(buildCellMap(cell, row, col));
                }
            }
        }
        return entries;
    }

    /** Include any tile with a static object (open doors/portals too), floor loot, or a character. */
    private boolean shouldSerializeCell(GridCell cell) {
        return cell.getStaticObject() != null
                || cell.hasObjects()
                || cell.getOccupyingCharacter() != null;
    }

    private void ensureParentDirectory(String filename) throws IOException {
        Path parent = Paths.get(filename).toAbsolutePath().getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
    }

    private List<LinkedHashMap<String, Object>> buildEnemiesList(GameSession session) {
        List<LinkedHashMap<String, Object>> entries = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>(session.getEnemies());
        collectMapEnemies(session.getMap(), enemies);
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                entries.add(buildEnemyMap(enemy, session));
            }
        }
        return entries;
    }

    private void collectMapEnemies(Map map, List<Enemy> enemies) {
        if (map == null) {
            return;
        }
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.getOccupyingCharacter() instanceof Enemy enemy) {
                    if (!enemies.contains(enemy)) {
                        enemies.add(enemy);
                    }
                }
            }
        }
    }

    private LinkedHashMap<String, Object> buildEnemyMap(Enemy enemy, GameSession session) {
        String type = enemy instanceof Sorcerer ? "Sorcerer"
                : enemy instanceof Knight ? "Knight" : enemy.getClass().getSimpleName();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("type", type);
        json.put("row", enemy.getRow());
        json.put("col", enemy.getCol());
        json.put("hp", enemy.getHealth());
        json.put("mana", enemy.getMana());
        json.put("energy", enemy.getEnergy());
        json.put("str", enemy.getStrength());
        json.put("def", enemy.getDefensePower());
        if (session instanceof com.kurawler.model.TeamMatchSession tm) {
            com.kurawler.model.Team team = tm.getTeamForCharacter(enemy);
            if (team != null) {
                json.put("team", team.getName());
            }
        }
        if (enemy instanceof Knight knight && knight.getEquippedMatchWeapon() != null) {
            json.put("matchWeapon", buildObjectMap(knight.getEquippedMatchWeapon()));
        }
        if (enemy instanceof Sorcerer sorcerer) {
            json.put("spellAttackTimerMs", sorcerer.getSpellAttackTimerMs());
            json.put("teleportTimerMs", sorcerer.getTeleportTimerMs());
            json.put("ringDropped", sorcerer.isRingDropped());
        }
        return json;
    }

    private LinkedHashMap<String, Object> buildShadowCloneMap(ShadowClone shadowClone) {
        if (shadowClone == null) {
            return null;
        }
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("row", shadowClone.getRow());
        json.put("col", shadowClone.getCol());
        json.put("hp", shadowClone.getHealth());
        json.put("remainingMs", shadowClone.getRemainingMs());
        return json;
    }

    private LinkedHashMap<String, Object> buildAchievementsMap(AchievementManager manager) {
        if (manager == null) {
            return null;
        }
        List<String> unlocked = new ArrayList<>();
        for (Achievement achievement : manager.getAchievements()) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement.getName());
            }
        }
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("totalMoves", manager.getTotalMoves());
        json.put("shadowCloneMoves", manager.getShadowCloneMoves());
        json.put("enemiesKilled", manager.getEnemiesKilled());
        json.put("itemsCollected", manager.getItemsCollected());
        json.put("damageTaken", manager.getDamageTaken());
        json.put("unlocked", unlocked);
        return json;
    }

    private LinkedHashMap<String, Object> buildHeroMap(Hero hero) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("name", hero.getName());
        json.put("row", hero.getRow());
        json.put("col", hero.getCol());
        json.put("hp", hero.getHealth());
        json.put("maxHp", hero.getMaxHealth());
        json.put("mana", hero.getMana());
        json.put("energy", hero.getEnergy());
        json.put("maxEnergy", hero.getMaxEnergy());
        json.put("str", hero.getStrength());
        json.put("def", hero.getDefensePower());
        json.put("equippedWeapon", hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "");
        json.put("equippedWearable", hero.getEquippedArmor() != null ? hero.getEquippedArmor().getName() : "");
        json.put("equippedShield", hero.getEquippedShield() != null ? hero.getEquippedShield().getName() : "");
        json.put("equippedRing", hero.getEquippedRing() != null ? hero.getEquippedRing().getName() : "");

        Inventory inv = hero.getInventory();
        List<LinkedHashMap<String, Object>> items = new ArrayList<>();
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                Item item = inv.getItem(r, c);
                if (item != null) {
                    items.add(buildObjectMap(item));
                }
            }
        }
        json.put("inventory", items);
        return json;
    }

    public boolean saveMapToFile(Map map, String filename) {
        try {
            ensureParentDirectory(filename);
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(gson.toJson(buildMapMap(map)));
            }
            System.out.println("[Persistence] Saved map to " + filename);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** JSON snapshot for editor undo / in-memory round-trip. */
    public String mapToJson(Map map) {
        return gson.toJson(buildMapMap(map));
    }

    /** Save world JSON to the maps directory. Returns true on success. */
    public boolean saveWorldToFile(java.util.Map<String, Map> maps, String startMapId, String filename) {
        try {
            ensureParentDirectory(filename);
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(gson.toJson(buildWorldRootMap(maps, startMapId)));
            }
            System.out.println("[Persistence] Saved world to " + filename);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String worldToJson(java.util.Map<String, Map> maps, String startMapId) {
        return gson.toJson(buildWorldRootMap(maps, startMapId));
    }

    private LinkedHashMap<String, Object> buildWorldRootMap(java.util.Map<String, Map> maps, String startMapId) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("format", "world");
        root.put("startMapId", startMapId != null ? startMapId : "");
        LinkedHashMap<String, Object> mapEntries = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Map> entry : maps.entrySet()) {
            mapEntries.put(entry.getKey(), buildMapMap(entry.getValue()));
        }
        root.put("maps", mapEntries);
        return root;
    }

    private LinkedHashMap<String, Object> buildMapMap(Map map) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("width", map.getWidth());
        json.put("height", map.getHeight());
        json.put("cells", buildCellsList(map));
        return json;
    }

    private LinkedHashMap<String, Object> buildCellMap(GridCell cell, int row, int col) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("row", row);
        json.put("col", col);
        json.put("walkable", terrainWalkable(cell));

        StaticObject so = cell.getStaticObject();
        com.kurawler.model.character.Character occupant = cell.getOccupyingCharacter();
        if (so != null) {
            json.put("staticObject", buildStaticObjectMap(so));
        }

        List<GameObject> objects = cell.getObjects();
        if (!objects.isEmpty()) {
            List<LinkedHashMap<String, Object>> objEntries = new ArrayList<>();
            for (GameObject obj : objects) {
                objEntries.add(buildObjectMap(obj));
            }
            json.put("objects", objEntries);
        }

        if (occupant != null) {
            json.put("character", buildMapCharacterMap(occupant));
        }
        return json;
    }

    private LinkedHashMap<String, Object> buildStaticObjectMap(StaticObject so) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("name", so.getName());
        json.put("description", so.getDescription());
        json.put("passable", so.isPassable());
        json.put("searchable", so.isSearchable());
        if (so instanceof MapPortal portal) {
            json.put("kind", "MapPortal");
            json.put("targetMapId", portal.getTargetMapId());
            json.put("targetRow", portal.getTargetRow());
            json.put("targetCol", portal.getTargetCol());
            json.put("requiredKeyId", portal.getRequiredKeyId());
            json.put("unlocked", portal.isUnlocked());
            json.put("stairsMode", portal.isStairsMode());
        }
        if (so.getHiddenItem() != null) {
            json.put("hiddenItem", buildObjectMap(so.getHiddenItem()));
        }
        return json;
    }

    /** Tile terrain walkability without treating the occupying character as blocking. */
    private boolean terrainWalkable(GridCell cell) {
        StaticObject so = cell.getStaticObject();
        if (so == null) {
            return true;
        }
        if (!so.isPassable()) {
            return false;
        }
        return !so.isSearchable();
    }

    private LinkedHashMap<String, Object> buildMapCharacterMap(com.kurawler.model.character.Character character) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("type", character.getClass().getSimpleName());
        json.put("name", character.getName());
        if (character.getRotation() != 0) {
            json.put("rotation", character.getRotation());
        }
        return json;
    }

    private LinkedHashMap<String, Object> buildObjectMap(GameObject obj) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("type", obj.getClass().getSimpleName());
        json.put("name", obj.getName());
        json.put("description", obj.getDescription());

        if (obj instanceof Weapon weapon) {
            json.put("damage", weapon.getDamage());
        } else if (obj instanceof Consumable consumable) {
            json.put("healAmount", consumable.getHealAmount());
        } else if (obj instanceof Key key) {
            json.put("keyId", key.getKeyId());
        } else if (obj instanceof Wearable wearable) {
            json.put("defenseBonus", wearable.getDefenseBonus());
        } else if (obj instanceof ShadowCloneScroll scroll) {
            json.put("text", scroll.getText());
            json.put("healAmount", scroll.getHealAmount());
            json.put("manaAmount", scroll.getManaAmount());
            json.put("energyAmount", scroll.getEnergyAmount());
            json.put("spawnedAtMs", scroll.getSpawnedAtMs());
        } else if (obj instanceof Scroll scroll) {
            json.put("text", scroll.getText());
            json.put("healAmount", scroll.getHealAmount());
        } else if (obj instanceof PortableContainer container) {
            json.put("capacity", container.getCapacity());
            json.put("contents", buildItemList(container.getContents()));
        } else if (obj instanceof StaticContainer container) {
            json.put("contents", buildItemList(container.getContents()));
        }
        return json;
    }

    private List<LinkedHashMap<String, Object>> buildItemList(List<Item> items) {
        List<LinkedHashMap<String, Object>> entries = new ArrayList<>();
        for (Item item : items) {
            entries.add(buildObjectMap(item));
        }
        return entries;
    }
}
