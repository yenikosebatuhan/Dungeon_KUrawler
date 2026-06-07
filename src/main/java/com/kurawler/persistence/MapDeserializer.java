package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Assassin;
import com.kurawler.model.character.Guard;
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
import com.kurawler.model.object.ShadowCloneScroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Loads a Map from JSON files written by {@link MapSerializer} using Gson.
 */
public class MapDeserializer {

    public static final class WorldData {
        public final LinkedHashMap<String, Map> maps = new LinkedHashMap<>();
        public String startMapId;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * @requires filename != null
     *           && the file at filename exists and is readable
     *           && the file contains valid JSON produced by MapSerializer.saveGameState
     *           && session != null
     *
     * @modifies session.map, session.hero, session.enemies, session.shadowClone,
     *           session.achievementManager, session.playDurationTimer,
     *           session.currentMode, session.targetObjective,
     *           session.activeEnemyCount,
     *           GridCell.occupyingCharacter for every cell that holds a
     *           restored character (hero or enemy)
     *
     * @effects  Parses the JSON save file at filename and fully replaces the
     *           state of session:
     *           - session.map is set to a new Map(width, height) with all cells
     *             reconstructed from the "cells" array (StaticObjects, hidden
     *             items, and floor objects restored);
     *           - session.hero is replaced by a new Hero whose position, stats
     *             (HP, mana, energy, STR, DEF), inventory items, equipped weapon,
     *             and equipped wearable match the saved values; the hero's cell
     *             has its occupyingCharacter set to the new Hero;
     *           - all previously tracked enemies are cleared and replaced by new
     *             Enemy instances (Knight or Sorcerer) with saved stats, each
     *             placed on its saved cell via moveToCell;
     *           - session.shadowClone, achievements, playDurationTimer,
     *             currentMode, and targetObjective are restored from the file;
     *           - observer relationships are re-wired via session.wireObservers();
     *           - returns true on success.
     *           If filename does not exist, returns false and session is unchanged.
     *           If an IOException occurs, prints the stack trace, returns false,
     *           and session may be partially modified.
     */
    public boolean loadGameState(String filename, GameSession session) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            java.util.Map<String, Object> root = JsonPersistence.parseObject(content);

            int width = JsonTypeHelper.asInt(root.get("width"));
            int height = JsonTypeHelper.asInt(root.get("height"));
            Map map = new Map(width, height);

            @SuppressWarnings("unchecked")
            List<Object> cells = root.containsKey("cells") && root.get("cells") != null
                    ? (List<Object>) root.get("cells") : new ArrayList<>();
            for (Object cellObj : cells) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> cell = (java.util.Map<String, Object>) cellObj;
                deserializeCell(cell, map);
            }

            restoreWorldMaps(root, session, map);
            restoreSessionFields(root, session);
            restoreHero(root, session);
            restoreEnemies(root, session);
            restoreShadowClone(root, session);
            restoreAchievements(root, session);
            session.wireObservers();

            System.out.println("[Persistence] Loaded game state from " + filename);
            return true;
        } catch (NoSuchFileException e) {
            System.out.println("[Persistence] No save file found: " + filename);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println("[Persistence] Failed to parse save file: " + filename);
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreSessionFields(java.util.Map<String, Object> root, GameSession session) {
        if (root.containsKey("playDurationTimer")) {
            session.setPlayDurationTimer(JsonTypeHelper.asInt(root.get("playDurationTimer")));
        }
        if (root.containsKey("currentMode")) {
            session.setCurrentMode((String) root.get("currentMode"));
        }
        if (root.containsKey("difficulty")) {
            session.setDifficulty((String) root.get("difficulty"));
        }
        if (root.containsKey("gameType")) {
            session.setSavedGameType((String) root.get("gameType"));
        }
        if (root.containsKey("lastShadowCloneScrollSpawnMs")) {
            session.setLastShadowCloneScrollSpawnMs(
                    JsonTypeHelper.asLong(root.get("lastShadowCloneScrollSpawnMs")));
        }
        if (root.containsKey("enemySpawnAccumulatorMs")) {
            session.setEnemySpawnAccumulatorMs(
                    JsonTypeHelper.asLong(root.get("enemySpawnAccumulatorMs")));
        }
        if (root.containsKey("enemiesDefeatedCount")) {
            session.setEnemiesDefeatedCount(JsonTypeHelper.asInt(root.get("enemiesDefeatedCount")));
        }
        if (root.containsKey("itemsCollectedCount")) {
            session.setItemsCollectedCount(JsonTypeHelper.asInt(root.get("itemsCollectedCount")));
        }
        if (root.containsKey("floorsClearedCount")) {
            session.setFloorsClearedCount(JsonTypeHelper.asInt(root.get("floorsClearedCount")));
        }
        if (root.containsKey("targetObjective") && root.get("targetObjective") != null) {
            GameObject target = deserializeObject(
                    (java.util.Map<String, Object>) root.get("targetObjective"));
            if (target instanceof Item) {
                session.setTargetObjective((Item) target);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreWorldMaps(java.util.Map<String, Object> root, GameSession session, Map fallbackMap) {
        session.getWorldMaps().clear();
        if (root.containsKey("worldMaps") && root.get("worldMaps") instanceof java.util.Map<?, ?> rawWorlds) {
            for (java.util.Map.Entry<?, ?> entry : rawWorlds.entrySet()) {
                String mapId = String.valueOf(entry.getKey());
                java.util.Map<String, Object> snapshot = (java.util.Map<String, Object>) entry.getValue();
                int worldWidth = JsonTypeHelper.asInt(snapshot.get("width"));
                int worldHeight = JsonTypeHelper.asInt(snapshot.get("height"));
                Map worldMap = new Map(worldWidth, worldHeight);
                List<Object> worldCells = snapshot.containsKey("cells") && snapshot.get("cells") != null
                        ? (List<Object>) snapshot.get("cells") : new ArrayList<>();
                for (Object cellObj : worldCells) {
                    deserializeCell((java.util.Map<String, Object>) cellObj, worldMap);
                }
                session.registerWorldMap(mapId, worldMap);
            }
        }

        String mapId = root.containsKey("currentMapId") ? (String) root.get("currentMapId") : null;
        if (mapId != null && session.getWorldMaps().containsKey(mapId)) {
            session.setCurrentMapId(mapId);
            session.setMap(session.getWorldMaps().get(mapId));
            return;
        }

        session.setMap(fallbackMap);
        if (mapId != null && !mapId.isBlank()) {
            session.setCurrentMapId(mapId);
            session.registerWorldMap(mapId, fallbackMap);
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreHero(java.util.Map<String, Object> root, GameSession session) {
        if (!root.containsKey("hero")) return;
        java.util.Map<String, Object> heroJson = (java.util.Map<String, Object>) root.get("hero");

        String name = (String) heroJson.get("name");
        Hero hero = new Hero(name);

        int heroRow = JsonTypeHelper.asInt(heroJson.get("row"));
        int heroCol = JsonTypeHelper.asInt(heroJson.get("col"));
        hero.setPosition(heroRow, heroCol);
        GridCell heroCell = session.getMap().getCell(heroRow, heroCol);
        if (heroCell != null) {
            hero.setCurrentCell(heroCell);
            heroCell.setOccupyingCharacter(hero);
        }
        if (heroJson.containsKey("maxHp")) {
            hero.setMaxHealth(JsonTypeHelper.asInt(heroJson.get("maxHp")));
        }
        hero.setHealth(JsonTypeHelper.asInt(heroJson.get("hp")));
        hero.setMana(JsonTypeHelper.asInt(heroJson.get("mana")));
        if (heroJson.containsKey("maxEnergy")) {
            hero.setMaxEnergy(JsonTypeHelper.asInt(heroJson.get("maxEnergy")));
        } else {
            hero.setMaxEnergy(GameSession.maxEnergyForDifficulty(session.getDifficulty()));
        }
        hero.setEnergy(JsonTypeHelper.asInt(heroJson.get("energy")));
        hero.setStrength(JsonTypeHelper.asInt(heroJson.get("str")));
        int savedDefense = JsonTypeHelper.asInt(heroJson.get("def"));
        hero.setDefensePower(savedDefense);

        String equippedName = (String) heroJson.get("equippedWeapon");
        String equippedWearableName = heroJson.containsKey("equippedWearable")
                ? (String) heroJson.get("equippedWearable") : "";
        String equippedShieldName = heroJson.containsKey("equippedShield")
                ? (String) heroJson.get("equippedShield") : "";
        String equippedRingName = heroJson.containsKey("equippedRing")
                ? (String) heroJson.get("equippedRing") : "";
        Wearable armorToEquip = null;
        Wearable shieldToEquip = null;
        Wearable ringToEquip = null;

        List<Object> invJson = (List<Object>) heroJson.get("inventory");
        for (Object entry : invJson) {
            java.util.Map<String, Object> objJson = (java.util.Map<String, Object>) entry;
            GameObject obj = deserializeObject(objJson);
            if (obj instanceof Item) {
                hero.getInventory().addItem((Item) obj);
                if (obj instanceof Weapon && obj.getName().equals(equippedName)) {
                    hero.setEquippedWeapon((Weapon) obj);
                } else if (obj instanceof Wearable) {
                    Wearable w = (Wearable) obj;
                    if (!equippedWearableName.isEmpty() && obj.getName().equals(equippedWearableName)) {
                        armorToEquip = w;
                    }
                    if (!equippedShieldName.isEmpty() && obj.getName().equals(equippedShieldName)) {
                        shieldToEquip = w;
                    }
                    if (!equippedRingName.isEmpty() && obj.getName().equals(equippedRingName)) {
                        ringToEquip = w;
                    }
                }
            }
        }
        int baseDefense = savedDefense;
        if (armorToEquip != null) {
            baseDefense -= armorToEquip.getDefenseBonus();
        }
        if (shieldToEquip != null) {
            baseDefense -= shieldToEquip.getDefenseBonus();
        }
        if (ringToEquip != null) {
            baseDefense -= ringToEquip.getDefenseBonus();
        }
        hero.setDefensePower(baseDefense);
        if (armorToEquip != null) {
            hero.equipWearable(armorToEquip);
        }
        if (shieldToEquip != null) {
            hero.equipWearable(shieldToEquip);
        }
        if (ringToEquip != null) {
            hero.equipWearable(ringToEquip);
        }

        session.setHero(hero);
        session.resetActiveEnemyCount();
    }

    @SuppressWarnings("unchecked")
    private void restoreEnemies(java.util.Map<String, Object> root, GameSession session) {
        session.clearEnemies();
        if (!root.containsKey("enemies")) return;

        for (Object entry : (List<Object>) root.get("enemies")) {
            java.util.Map<String, Object> enemyJson = (java.util.Map<String, Object>) entry;
            Enemy enemy = createEnemy((String) enemyJson.get("type"));
            if (enemy == null) continue;

            int row = JsonTypeHelper.asInt(enemyJson.get("row"));
            int col = JsonTypeHelper.asInt(enemyJson.get("col"));
            if (enemyJson.containsKey("name")) {
                enemy.setName((String) enemyJson.get("name"));
            }
            enemy.setHealth(getIntOrZero(enemyJson, "hp"));
            if (enemyJson.containsKey("atk")) {
                enemy.setAttackPower(JsonTypeHelper.asInt(enemyJson.get("atk")));
            }
            enemy.setMana(getIntOrZero(enemyJson, "mana"));
            enemy.setEnergy(getIntOrZero(enemyJson, "energy"));
            if (enemyJson.containsKey("str")) {
                enemy.setStrength(JsonTypeHelper.asInt(enemyJson.get("str")));
            }
            if (enemyJson.containsKey("def")) {
                enemy.setDefensePower(JsonTypeHelper.asInt(enemyJson.get("def")));
            }
            if (enemyJson.containsKey("team")) {
                session.setEnemyTeamName(enemy, (String) enemyJson.get("team"));
            }
            if (enemy instanceof Knight knight && enemyJson.containsKey("matchWeapon")) {
                GameObject weaponObj = deserializeObject(
                        (java.util.Map<String, Object>) enemyJson.get("matchWeapon"));
                if (weaponObj instanceof Weapon weapon) {
                    knight.equipMatchWeapon(weapon);
                }
            }
            if (enemy instanceof Sorcerer sorcerer) {
                if (enemyJson.containsKey("spellAttackTimerMs")) {
                    sorcerer.setSpellAttackTimerMs(
                            JsonTypeHelper.asLong(enemyJson.get("spellAttackTimerMs")));
                }
                if (enemyJson.containsKey("teleportTimerMs")) {
                    sorcerer.setTeleportTimerMs(
                            JsonTypeHelper.asLong(enemyJson.get("teleportTimerMs")));
                }
                if (enemyJson.containsKey("ringDropped")
                        && Boolean.TRUE.equals(enemyJson.get("ringDropped"))) {
                    sorcerer.markRingDropped();
                }
            }

            GridCell cell = session.getMap().getCell(row, col);
            if (cell != null && !cell.isBlocked()) {
                enemy.moveToCell(cell);
                session.addEnemy(enemy);
            }
        }
    }

    private Enemy createEnemy(String type) {
        if ("Sorcerer".equals(type)) return new Sorcerer();
        if ("Knight".equals(type)) return new Knight();
        if ("Guard".equals(type)) return new Guard();
        if ("Assassin".equals(type)) return new Assassin();
        return null;
    }

    @SuppressWarnings("unchecked")
    private void restoreShadowClone(java.util.Map<String, Object> root, GameSession session) {
        if (!root.containsKey("shadowClone") || root.get("shadowClone") == null) {
            session.setShadowClone(null);
            return;
        }

        java.util.Map<String, Object> cloneJson =
                (java.util.Map<String, Object>) root.get("shadowClone");
        ShadowClone shadowClone = new ShadowClone(JsonTypeHelper.asInt(cloneJson.get("row")),
                JsonTypeHelper.asInt(cloneJson.get("col")), session.getMap());
        if (cloneJson.containsKey("hp")) {
            shadowClone.setHealth(JsonTypeHelper.asInt(cloneJson.get("hp")));
        }
        if (cloneJson.containsKey("remainingMs")) {
            shadowClone.setRemainingMs(JsonTypeHelper.asLong(cloneJson.get("remainingMs")));
        }
        session.setShadowClone(shadowClone);
    }

    @SuppressWarnings("unchecked")
    private void restoreAchievements(java.util.Map<String, Object> root, GameSession session) {
        AchievementManager manager = new AchievementManager();

        if (root.containsKey("achievements") && root.get("achievements") != null) {
            java.util.Map<String, Object> achievementJson =
                    (java.util.Map<String, Object>) root.get("achievements");
            List<String> unlocked = new ArrayList<>();
            if (achievementJson.containsKey("unlocked")) {
                for (Object name : (List<Object>) achievementJson.get("unlocked")) {
                    unlocked.add((String) name);
                }
            }
            manager.restoreProgress(
                    getIntOrZero(achievementJson, "totalMoves"),
                    getIntOrZero(achievementJson, "shadowCloneMoves"),
                    getIntOrZero(achievementJson, "enemiesKilled"),
                    getIntOrZero(achievementJson, "itemsCollected"),
                    getIntOrZero(achievementJson, "damageTaken"),
                    unlocked);
        }

        session.setAchievementManager(manager);
    }

    private int getIntOrZero(java.util.Map<String, Object> values, String key) {
        if (!values.containsKey(key) || values.get(key) == null) {
            return 0;
        }
        return JsonTypeHelper.asInt(values.get(key));
    }

    public Map loadMapFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return parseMapRoot(JsonPersistence.parseObject(json));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map loadMapFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            Map map = parseMapRoot(JsonPersistence.parseObject(content));
            System.out.println("[Persistence] Loaded map from " + filename);
            return map;
        } catch (NoSuchFileException e) {
            System.out.println("[Persistence] No save file found: " + filename);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public WorldData loadWorldFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            java.util.Map<String, Object> root = JsonPersistence.parseObject(content);
            WorldData data = parseWorldRoot(root);
            if (data != null) {
                System.out.println("[Persistence] Loaded world from " + filename);
            }
            return data;
        } catch (NoSuchFileException e) {
            System.out.println("[Persistence] No world file found: " + filename);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("[Persistence] Failed to parse world file: " + filename);
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private WorldData parseWorldRoot(java.util.Map<String, Object> root) {
        if (!root.containsKey("maps") || !(root.get("maps") instanceof java.util.Map<?, ?> rawMaps)) {
            return null;
        }
        WorldData data = new WorldData();
        data.startMapId = root.containsKey("startMapId") ? (String) root.get("startMapId") : null;
        java.util.Map<String, Object> mapsJson = (java.util.Map<String, Object>) rawMaps;
        for (java.util.Map.Entry<String, Object> entry : mapsJson.entrySet()) {
            if (!(entry.getValue() instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }
            data.maps.put(entry.getKey(), parseMapRoot((java.util.Map<String, Object>) rawMap));
        }
        return data.maps.isEmpty() ? null : data;
    }

    private Map parseMapRoot(java.util.Map<String, Object> root) {
        int width = JsonTypeHelper.asInt(root.get("width"));
        int height = JsonTypeHelper.asInt(root.get("height"));
        Map map = new Map(width, height);

        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) root.get("cells");
        for (Object cellObj : cells) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> cell = (java.util.Map<String, Object>) cellObj;
            deserializeCell(cell, map);
        }
        return map;
    }

    /**
     * Reads just the saveName and savedAt fields from a save file without
     * deserializing the full game state. Used by the load dialog to populate
     * its list without the cost of loading every save fully.
     *
     * @requires filename != null
     * @effects  Returns a two-element array [saveName, savedAt] read from the
     *           JSON. Either element may be null if the field is absent or the
     *           file cannot be read.
     */
    public String[] readSaveMetadata(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            java.util.Map<String, Object> root = JsonPersistence.parseObject(content);
            String saveName = root.containsKey("saveName") ? (String) root.get("saveName") : null;
            String savedAt = root.containsKey("savedAt") ? (String) root.get("savedAt") : null;
            return new String[]{saveName, savedAt};
        } catch (IOException ignored) {
            return new String[]{null, null};
        }
    }

    /**
     * Reads the persisted game type from a save file without full deserialization.
     * Returns null when the field is absent (older saves).
     */
    public String readGameType(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            java.util.Map<String, Object> root = JsonPersistence.parseObject(content);
            return root.containsKey("gameType") ? (String) root.get("gameType") : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    // ── Deserialization helpers ───────────────────────────────────────────────

    private void deserializeCell(java.util.Map<String, Object> cellJson, Map map) {
        int row = JsonTypeHelper.asInt(cellJson.get("row"));
        int col = JsonTypeHelper.asInt(cellJson.get("col"));
        GridCell cell = map.getCell(row, col);
        if (cell == null) return;

        if (cellJson.containsKey("staticObject")) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> soJson = (java.util.Map<String, Object>) cellJson.get("staticObject");
            cell.setStaticObject(deserializeStaticObject(soJson));
        } else if (!JsonTypeHelper.asBoolean(cellJson.get("walkable"))
                && !cellJson.containsKey("character")) {
            // Backward compat: old saves only stored "walkable": false for walls
            cell.setStaticObject(new StaticObject("Wall", "Dark wall", false, false));
        }

        if (cellJson.containsKey("objects")) {
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) cellJson.get("objects");
            for (Object objEntry : objects) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> objJson = (java.util.Map<String, Object>) objEntry;
                GameObject obj = deserializeObject(objJson);
                if (obj != null) {
                    cell.addObject(obj);
                }
            }
        }

        if (cellJson.containsKey("character")) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> charJson =
                    (java.util.Map<String, Object>) cellJson.get("character");
            com.kurawler.model.character.Character character = deserializeMapCharacter(charJson);
            if (character == null) {
                return;
            }
            // Older map files marked hero tiles as walkable:false; that created a fake wall above.
            if (character instanceof Hero
                    && !cellJson.containsKey("staticObject")
                    && cell.getStaticObject() != null) {
                cell.setStaticObject(null);
            }
            if (character instanceof Hero || !cell.isBlocked()) {
                character.moveToCell(cell);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private com.kurawler.model.character.Character deserializeMapCharacter(
            java.util.Map<String, Object> charJson) {
        String type = (String) charJson.get("type");
        com.kurawler.model.character.Character character;
        if ("Hero".equals(type)) {
            String name = charJson.containsKey("name") ? (String) charJson.get("name") : "Hero";
            character = new Hero(name);
        } else {
            character = createEnemy(type);
            if (character == null) {
                return null;
            }
            if (charJson.containsKey("name")) {
                character.setName((String) charJson.get("name"));
            }
        }
        if (charJson.containsKey("rotation")) {
            character.setRotation(JsonTypeHelper.asInt(charJson.get("rotation")));
        }
        return character;
    }

    @SuppressWarnings("unchecked")
    private StaticObject deserializeStaticObject(java.util.Map<String, Object> soJson) {
        String soName = (String) soJson.get("name");
        String soDesc = soJson.containsKey("description") ? (String) soJson.get("description") : "";
        boolean soPassable = soJson.containsKey("passable")
                ? JsonTypeHelper.asBoolean(soJson.get("passable")) : true;
        boolean soSearchable = soJson.containsKey("searchable")
                ? JsonTypeHelper.asBoolean(soJson.get("searchable")) : false;

        StaticObject staticObject;
        if ("MapPortal".equals(soJson.get("kind")) || soJson.containsKey("targetMapId")) {
            String targetMapId = (String) soJson.get("targetMapId");
            int targetRow = getIntOrZero(soJson, "targetRow");
            int targetCol = getIntOrZero(soJson, "targetCol");
            String requiredKeyId = null;
            if (soJson.containsKey("requiredKeyId") && soJson.get("requiredKeyId") != null) {
                requiredKeyId = (String) soJson.get("requiredKeyId");
            }
            boolean unlocked = soJson.containsKey("unlocked")
                    ? JsonTypeHelper.asBoolean(soJson.get("unlocked")) : soPassable;
            boolean stairsMode = soJson.containsKey("stairsMode")
                    ? JsonTypeHelper.asBoolean(soJson.get("stairsMode")) : false;
            staticObject = new MapPortal(soName, soDesc, targetMapId, targetRow, targetCol,
                    requiredKeyId, unlocked, stairsMode);
        } else {
            staticObject = new StaticObject(soName, soDesc, soPassable, soSearchable);
        }

        if (soJson.containsKey("hiddenItem") && soJson.get("hiddenItem") != null) {
            java.util.Map<String, Object> hiddenJson =
                    (java.util.Map<String, Object>) soJson.get("hiddenItem");
            GameObject hidden = deserializeObject(hiddenJson);
            if (hidden instanceof Item) {
                staticObject.setHiddenItem((Item) hidden);
            }
        }
        return staticObject;
    }

    private GameObject deserializeObject(java.util.Map<String, Object> json) {
        String type        = (String) json.get("type");
        String name        = (String) json.get("name");
        String description = json.containsKey("description") ? (String) json.get("description") : "";

        switch (type) {
            case "Weapon":
                int damage = json.containsKey("damage")
                        ? JsonTypeHelper.asInt(json.get("damage")) : 0;
                return new Weapon(name, description, damage);
            case "Consumable":
                int heal = json.containsKey("healAmount")
                        ? JsonTypeHelper.asInt(json.get("healAmount")) : 0;
                return new Consumable(name, description, heal);
            case "StaticObject":
                return new StaticObject(name, description, true, false);
            case "Key":
                String keyId = json.containsKey("keyId") ? (String) json.get("keyId") : "";
                return new Key(name, description, keyId);
            case "Wearable":
                int defenseBonus = json.containsKey("defenseBonus")
                        ? JsonTypeHelper.asInt(json.get("defenseBonus")) : 0;
                return new Wearable(name, description, defenseBonus);
            case "Scroll":
                String text = json.containsKey("text") ? (String) json.get("text") : description;
                int scrollHeal = json.containsKey("healAmount")
                        ? JsonTypeHelper.asInt(json.get("healAmount")) : 0;
                return new Scroll(name, description, text, scrollHeal);
            case "ShadowCloneScroll":
                long spawnedAtMs = json.containsKey("spawnedAtMs")
                        ? JsonTypeHelper.asLong(json.get("spawnedAtMs"))
                        : System.currentTimeMillis();
                return new ShadowCloneScroll(spawnedAtMs);
            case "PortableContainer":
                int capacity = json.containsKey("capacity")
                        ? JsonTypeHelper.asInt(json.get("capacity")) : 4;
                PortableContainer portable = new PortableContainer(name, description, capacity);
                restoreContainerContents(portable, json);
                return portable;
            case "StaticContainer":
                StaticContainer fixed = new StaticContainer(name, description);
                restoreContainerContents(fixed, json);
                return fixed;
            default:
                System.out.println("[Persistence] Unknown object type: " + type);
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreContainerContents(PortableContainer container,
            java.util.Map<String, Object> json) {
        if (!json.containsKey("contents")) return;
        for (Object entry : (List<Object>) json.get("contents")) {
            GameObject item = deserializeObject((java.util.Map<String, Object>) entry);
            if (item instanceof Item) {
                container.addItem((Item) item);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreContainerContents(StaticContainer container,
            java.util.Map<String, Object> json) {
        if (!json.containsKey("contents")) return;
        for (Object entry : (List<Object>) json.get("contents")) {
            GameObject item = deserializeObject((java.util.Map<String, Object>) entry);
            if (item instanceof Item) {
                container.addItem((Item) item);
            }
        }
    }
}

