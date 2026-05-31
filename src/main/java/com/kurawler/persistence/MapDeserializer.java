package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.achievement.AchievementManager;
import com.kurawler.model.character.Assassin;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Guard;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.MapPortal;
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
 * Loads a Map from a JSON file written by MapSerializer.
 * Uses a hand-rolled recursive-descent parser so no 3rd-party libraries are needed.
 *
 * Parser state is held in instance fields (pos, src) and is NOT thread-safe,
 * but that is fine for a single-player game that loads one map at a time.
 */
public class MapDeserializer {

    // ── Parser state ─────────────────────────────────────────────────────────
    private String src;
    private int pos;

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
            src = content;
            pos = 0;
            LinkedHashMap<String, Object> root = parseObject();

            int width  = (int) root.get("width");
            int height = (int) root.get("height");
            Map map = new Map(width, height);

            @SuppressWarnings("unchecked")
            List<Object> cells = root.containsKey("cells") && root.get("cells") != null
                    ? (List<Object>) root.get("cells") : new ArrayList<>();
            for (Object cellObj : cells) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> cell = (LinkedHashMap<String, Object>) cellObj;
                deserializeCell(cell, map);
            }

            session.setMap(map);
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
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreSessionFields(LinkedHashMap<String, Object> root, GameSession session) {
        if (root.containsKey("playDurationTimer")) {
            session.setPlayDurationTimer((int) root.get("playDurationTimer"));
        }
        if (root.containsKey("currentMode")) {
            session.setCurrentMode((String) root.get("currentMode"));
        }
        if (root.containsKey("difficulty")) {
            session.setDifficulty((String) root.get("difficulty"));
        }
        if (root.containsKey("enemiesDefeatedCount")) {
            session.setEnemiesDefeatedCount((int) root.get("enemiesDefeatedCount"));
        }
        if (root.containsKey("itemsCollectedCount")) {
            session.setItemsCollectedCount((int) root.get("itemsCollectedCount"));
        }
        if (root.containsKey("floorsClearedCount")) {
            session.setFloorsClearedCount((int) root.get("floorsClearedCount"));
        }
        if (root.containsKey("targetObjective") && root.get("targetObjective") != null) {
            GameObject target = deserializeObject(
                    (LinkedHashMap<String, Object>) root.get("targetObjective"));
            if (target instanceof Item) {
                session.setTargetObjective((Item) target);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreHero(LinkedHashMap<String, Object> root, GameSession session) {
        if (!root.containsKey("hero")) return;
        LinkedHashMap<String, Object> heroJson = (LinkedHashMap<String, Object>) root.get("hero");

        String name = (String) heroJson.get("name");
        Hero hero = new Hero(name);

        int heroRow = (int) heroJson.get("row");
        int heroCol = (int) heroJson.get("col");
        hero.setPosition(heroRow, heroCol);
        GridCell heroCell = session.getMap().getCell(heroRow, heroCol);
        if (heroCell != null) {
            hero.setCurrentCell(heroCell);
            heroCell.setOccupyingCharacter(hero);
        }
        if (heroJson.containsKey("maxHp")) {
            hero.setMaxHealth((int) heroJson.get("maxHp"));
        }
        hero.setHealth((int) heroJson.get("hp"));
        hero.setMana((int) heroJson.get("mana"));
        if (heroJson.containsKey("maxEnergy")) {
            hero.setMaxEnergy((int) heroJson.get("maxEnergy"));
        } else {
            hero.setMaxEnergy(GameSession.maxEnergyForDifficulty(session.getDifficulty()));
        }
        hero.setEnergy((int) heroJson.get("energy"));
        hero.setStrength((int) heroJson.get("str"));
        int savedDefense = (int) heroJson.get("def");
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
            LinkedHashMap<String, Object> objJson = (LinkedHashMap<String, Object>) entry;
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
    private void restoreEnemies(LinkedHashMap<String, Object> root, GameSession session) {
        session.clearEnemies();
        if (!root.containsKey("enemies")) return;

        for (Object entry : (List<Object>) root.get("enemies")) {
            LinkedHashMap<String, Object> enemyJson = (LinkedHashMap<String, Object>) entry;
            Enemy enemy = createEnemy((String) enemyJson.get("type"));
            if (enemy == null) continue;

            int row = (int) enemyJson.get("row");
            int col = (int) enemyJson.get("col");
            if (enemyJson.containsKey("name")) {
                enemy.setName((String) enemyJson.get("name"));
            }
            enemy.setHealth(getIntOrZero(enemyJson, "hp"));
            if (enemyJson.containsKey("atk")) {
                enemy.setAttackPower((int) enemyJson.get("atk"));
            }
            enemy.setMana(getIntOrZero(enemyJson, "mana"));
            enemy.setEnergy(getIntOrZero(enemyJson, "energy"));
            if (enemyJson.containsKey("str")) {
                enemy.setStrength((int) enemyJson.get("str"));
            }
            if (enemyJson.containsKey("def")) {
                enemy.setDefensePower((int) enemyJson.get("def"));
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
    private void restoreShadowClone(LinkedHashMap<String, Object> root, GameSession session) {
        if (!root.containsKey("shadowClone") || root.get("shadowClone") == null) {
            session.setShadowClone(null);
            return;
        }

        LinkedHashMap<String, Object> cloneJson =
                (LinkedHashMap<String, Object>) root.get("shadowClone");
        ShadowClone shadowClone = new ShadowClone((int) cloneJson.get("row"),
                (int) cloneJson.get("col"), session.getMap());
        if (cloneJson.containsKey("hp")) {
            shadowClone.setHealth((int) cloneJson.get("hp"));
        }
        if (cloneJson.containsKey("remainingMs")) {
            shadowClone.setRemainingMs(((Number) cloneJson.get("remainingMs")).longValue());
        }
        session.setShadowClone(shadowClone);
    }

    @SuppressWarnings("unchecked")
    private void restoreAchievements(LinkedHashMap<String, Object> root, GameSession session) {
        AchievementManager manager = new AchievementManager();

        if (root.containsKey("achievements") && root.get("achievements") != null) {
            LinkedHashMap<String, Object> achievementJson =
                    (LinkedHashMap<String, Object>) root.get("achievements");
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

    private int getIntOrZero(LinkedHashMap<String, Object> values, String key) {
        return values.containsKey(key) ? (int) values.get(key) : 0;
    }

    public Map loadMapFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            src = json;
            pos = 0;
            return parseMapRoot(parseObject());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map loadMapFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            src = content;
            pos = 0;
            Map map = parseMapRoot(parseObject());
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

    /** Result of loading a world file: all maps keyed by ID, plus the start map ID. */
    public static class WorldData {
        public final java.util.LinkedHashMap<String, Map> maps;
        public final String startMapId;
        public WorldData(java.util.LinkedHashMap<String, Map> maps, String startMapId) {
            this.maps = maps;
            this.startMapId = startMapId;
        }
    }

    public WorldData loadWorldFromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            src = json;
            pos = 0;
            LinkedHashMap<String, Object> root = parseObject();
            return parseWorldRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public WorldData loadWorldFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            return loadWorldFromJson(content);
        } catch (NoSuchFileException e) {
            System.out.println("[Persistence] No world file found: " + filename);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private WorldData parseWorldRoot(LinkedHashMap<String, Object> root) {
        String startMapId = root.containsKey("startMapId") ? (String) root.get("startMapId") : "";
        java.util.LinkedHashMap<String, Map> maps = new java.util.LinkedHashMap<>();
        LinkedHashMap<String, Object> mapsJson = (LinkedHashMap<String, Object>) root.get("maps");
        if (mapsJson != null) {
            for (java.util.Map.Entry<String, Object> entry : mapsJson.entrySet()) {
                src = ""; // reset so parseMapRoot uses the already-parsed object
                Map map = parseMapRoot((LinkedHashMap<String, Object>) entry.getValue());
                maps.put(entry.getKey(), map);
            }
        }
        return new WorldData(maps, startMapId);
    }

    private Map parseMapRoot(LinkedHashMap<String, Object> root) {
        int width = (int) root.get("width");
        int height = (int) root.get("height");
        Map map = new Map(width, height);

        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) root.get("cells");
        for (Object cellObj : cells) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> cell = (LinkedHashMap<String, Object>) cellObj;
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
        String saveName = null;
        String savedAt  = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            for (String line : lines) {
                String t = line.trim();
                if (saveName == null && t.startsWith("\"saveName\"")) {
                    saveName = extractStringValue(t);
                }
                if (savedAt == null && t.startsWith("\"savedAt\"")) {
                    savedAt = extractStringValue(t);
                }
                if (saveName != null && savedAt != null) break;
                if (t.startsWith("\"width\"")) break; // past metadata section
            }
        } catch (IOException ignored) {}
        return new String[]{saveName, savedAt};
    }

    private String extractStringValue(String jsonLine) {
        int colon = jsonLine.indexOf(':');
        if (colon < 0) return null;
        String after = jsonLine.substring(colon + 1).trim();
        if (after.startsWith("\"")) {
            int end = after.indexOf('"', 1);
            if (end > 0) return after.substring(1, end);
        }
        return null;
    }

    // ── Deserialization helpers ───────────────────────────────────────────────

    private void deserializeCell(LinkedHashMap<String, Object> cellJson, Map map) {
        int row = (int) cellJson.get("row");
        int col = (int) cellJson.get("col");
        GridCell cell = map.getCell(row, col);
        if (cell == null) return;

        if (cellJson.containsKey("staticObject")) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> soJson = (LinkedHashMap<String, Object>) cellJson.get("staticObject");
            String soName = (String) soJson.get("name");
            String soDesc = soJson.containsKey("description") ? (String) soJson.get("description") : "";
            StaticObject staticObject;
            if ("MapPortal".equals(soJson.get("type"))) {
                String targetMapId = soJson.containsKey("targetMapId") ? (String) soJson.get("targetMapId") : "";
                int targetRow = soJson.containsKey("targetRow") ? (int) soJson.get("targetRow") : 0;
                int targetCol = soJson.containsKey("targetCol") ? (int) soJson.get("targetCol") : 0;
                String reqKey = soJson.containsKey("requiredKeyId") ? (String) soJson.get("requiredKeyId") : null;
                boolean unlocked = soJson.containsKey("unlocked") ? (boolean) soJson.get("unlocked") : true;
                boolean stairsMode = soJson.containsKey("stairsMode") ? (boolean) soJson.get("stairsMode") : false;
                staticObject = new MapPortal(soName, soDesc,
                        targetMapId.isEmpty() ? null : targetMapId,
                        targetRow, targetCol, reqKey, unlocked, stairsMode);
            } else {
                boolean soPassable = soJson.containsKey("passable") ? (boolean) soJson.get("passable") : true;
                boolean soSearchable = soJson.containsKey("searchable") ? (boolean) soJson.get("searchable") : false;
                staticObject = new StaticObject(soName, soDesc, soPassable, soSearchable);
                if (soJson.containsKey("hiddenItem") && soJson.get("hiddenItem") != null) {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, Object> hiddenJson =
                            (LinkedHashMap<String, Object>) soJson.get("hiddenItem");
                    GameObject hidden = deserializeObject(hiddenJson);
                    if (hidden instanceof Item) {
                        staticObject.setHiddenItem((Item) hidden);
                    }
                }
            }
            if (soJson.containsKey("rotation"))
                staticObject.setRotation((int) soJson.get("rotation"));
            cell.setStaticObject(staticObject);
        } else if (!(boolean) cellJson.get("walkable")) {
            // Backward compat: old saves only stored "walkable": false for walls
            cell.setStaticObject(new StaticObject("Wall", "Dark wall", false, false));
        }

        if (cellJson.containsKey("objects")) {
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) cellJson.get("objects");
            for (Object objEntry : objects) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> objJson = (LinkedHashMap<String, Object>) objEntry;
                GameObject obj = deserializeObject(objJson);
                if (obj != null) {
                    cell.addObject(obj);
                }
            }
        }
    }

    private GameObject deserializeObject(LinkedHashMap<String, Object> json) {
        String type        = (String) json.get("type");
        String name        = (String) json.get("name");
        String description = json.containsKey("description") ? (String) json.get("description") : "";

        return withRestoredRotation(deserializeObjectInner(json, type, name, description), json);
    }

    private GameObject deserializeObjectInner(LinkedHashMap<String, Object> json,
            String type, String name, String description) {
        switch (type) {
            case "Weapon":
                int damage = json.containsKey("damage") ? (int) json.get("damage") : 0;
                return new Weapon(name, description, damage);
            case "Consumable":
                int heal = json.containsKey("healAmount") ? (int) json.get("healAmount") : 0;
                int mana = json.containsKey("manaAmount") ? (int) json.get("manaAmount") : 0;
                int energy = json.containsKey("energyAmount") ? (int) json.get("energyAmount") : 0;
                return new Consumable(name, description, heal, mana, energy);
            case "StaticObject":
                return new StaticObject(name, description, true, false);
            case "Key":
                String keyId = json.containsKey("keyId") ? (String) json.get("keyId") : "";
                return new Key(name, description, keyId);
            case "Wearable":
                int defenseBonus = json.containsKey("defenseBonus")
                        ? (int) json.get("defenseBonus") : 0;
                return new Wearable(name, description, defenseBonus);
            case "Scroll":
                String text = json.containsKey("text") ? (String) json.get("text") : description;
                int scrollHeal = json.containsKey("healAmount") ? (int) json.get("healAmount") : 0;
                int scrollMana = json.containsKey("manaAmount") ? (int) json.get("manaAmount") : 0;
                int scrollEnergy = json.containsKey("energyAmount") ? (int) json.get("energyAmount") : 0;
                return new Scroll(name, description, text, scrollHeal, scrollMana, scrollEnergy);
            case "PortableContainer":
                int capacity = json.containsKey("capacity") ? (int) json.get("capacity") : 4;
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

    /** Apply rotation to a deserialized object if the field was saved. */
    private GameObject withRestoredRotation(GameObject obj, LinkedHashMap<String, Object> json) {
        if (obj != null && json.containsKey("rotation"))
            obj.setRotation((int) json.get("rotation"));
        return obj;
    }

    @SuppressWarnings("unchecked")
    private void restoreContainerContents(PortableContainer container,
            LinkedHashMap<String, Object> json) {
        if (!json.containsKey("contents")) return;
        for (Object entry : (List<Object>) json.get("contents")) {
            GameObject item = deserializeObject((LinkedHashMap<String, Object>) entry);
            if (item instanceof Item) {
                container.addItem((Item) item);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreContainerContents(StaticContainer container,
            LinkedHashMap<String, Object> json) {
        if (!json.containsKey("contents")) return;
        for (Object entry : (List<Object>) json.get("contents")) {
            GameObject item = deserializeObject((LinkedHashMap<String, Object>) entry);
            if (item instanceof Item) {
                container.addItem((Item) item);
            }
        }
    }

    // ── Recursive-descent JSON parser ─────────────────────────────────────────
    // Handles the subset of JSON we produce: objects, arrays, strings, ints, booleans.

    private LinkedHashMap<String, Object> parseObject() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        skip('{');
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            skip(':');
            skipWs();
            map.put(key, parseValue());
            skipWs();
            if (peek() == '}') { pos++; break; }
            skip(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        skip('[');
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWs();
            list.add(parseValue());
            skipWs();
            if (peek() == ']') { pos++; break; }
            skip(',');
        }
        return list;
    }

    private Object parseValue() {
        skipWs();
        char c = peek();
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') return parseNull();
        return parseInt();
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalStateException("Expected null at pos " + pos);
    }

    private String parseString() {
        skip('"');
        StringBuilder sb = new StringBuilder();
        while (peek() != '"') {
            char c = src.charAt(pos++);
            if (c == '\\') {
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        pos++; // closing "
        return sb.toString();
    }

    private boolean parseBoolean() {
        if (src.startsWith("true", pos))  { pos += 4; return true;  }
        if (src.startsWith("false", pos)) { pos += 5; return false; }
        throw new IllegalStateException("Expected boolean at pos " + pos);
    }

    private int parseInt() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        return Integer.parseInt(src.substring(start, pos));
    }

    private char peek() {
        return src.charAt(pos);
    }

    private void skip(char expected) {
        if (src.charAt(pos) != expected) {
            throw new IllegalStateException(
                "Expected '" + expected + "' but found '" + src.charAt(pos) + "' at pos " + pos);
        }
        pos++;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }
}
