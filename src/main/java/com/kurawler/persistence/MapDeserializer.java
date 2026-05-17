package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
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
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
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
            List<Object> cells = (List<Object>) root.get("cells");
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
        hero.setHealth((int) heroJson.get("hp"));
        hero.setMana((int) heroJson.get("mana"));
        hero.setEnergy((int) heroJson.get("energy"));
        hero.setStrength((int) heroJson.get("str"));
        int savedDefense = (int) heroJson.get("def");
        hero.setDefensePower(savedDefense);

        String equippedName = (String) heroJson.get("equippedWeapon");
        String equippedWearableName = heroJson.containsKey("equippedWearable")
                ? (String) heroJson.get("equippedWearable") : "";
        Wearable wearableToEquip = null;

        List<Object> invJson = (List<Object>) heroJson.get("inventory");
        for (Object entry : invJson) {
            LinkedHashMap<String, Object> objJson = (LinkedHashMap<String, Object>) entry;
            GameObject obj = deserializeObject(objJson);
            if (obj instanceof Item) {
                hero.getInventory().addItem((Item) obj);
                if (obj instanceof Weapon && obj.getName().equals(equippedName)) {
                    hero.setEquippedWeapon((Weapon) obj);
                } else if (obj instanceof Wearable && obj.getName().equals(equippedWearableName)) {
                    wearableToEquip = (Wearable) obj;
                }
            }
        }
        if (wearableToEquip != null) {
            hero.setDefensePower(savedDefense - wearableToEquip.getDefenseBonus());
            hero.equipWearable(wearableToEquip);
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
            enemy.setHealth(getIntOrZero(enemyJson, "hp"));
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
        return null;
    }

    @SuppressWarnings("unchecked")
    private void restoreShadowClone(LinkedHashMap<String, Object> root, GameSession session) {
        if (!root.containsKey("shadowClone") || root.get("shadowClone") == null) {
            Hero hero = session.getHero();
            if (hero != null) {
                session.setShadowClone(new ShadowClone(hero.getRow(), hero.getCol(), session.getMap()));
            }
            return;
        }

        LinkedHashMap<String, Object> cloneJson =
                (LinkedHashMap<String, Object>) root.get("shadowClone");
        ShadowClone shadowClone = new ShadowClone((int) cloneJson.get("row"),
                (int) cloneJson.get("col"), session.getMap());
        if (cloneJson.containsKey("hp")) {
            shadowClone.setHealth((int) cloneJson.get("hp"));
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

    public Map loadMapFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            src = content;
            pos = 0;
            LinkedHashMap<String, Object> root = parseObject();

            int width  = (int) root.get("width");
            int height = (int) root.get("height");
            Map map = new Map(width, height);

            @SuppressWarnings("unchecked")
            List<Object> cells = (List<Object>) root.get("cells");
            for (Object cellObj : cells) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> cell = (LinkedHashMap<String, Object>) cellObj;
                deserializeCell(cell, map);
            }

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
            boolean soPassable = soJson.containsKey("passable") ? (boolean) soJson.get("passable") : true;
            boolean soSearchable = soJson.containsKey("searchable") ? (boolean) soJson.get("searchable") : false;
            StaticObject staticObject = new StaticObject(soName, soDesc, soPassable, soSearchable);
            if (soJson.containsKey("hiddenItem") && soJson.get("hiddenItem") != null) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> hiddenJson =
                        (LinkedHashMap<String, Object>) soJson.get("hiddenItem");
                GameObject hidden = deserializeObject(hiddenJson);
                if (hidden instanceof Item) {
                    staticObject.setHiddenItem((Item) hidden);
                }
            }
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

        switch (type) {
            case "Weapon":
                int damage = json.containsKey("damage") ? (int) json.get("damage") : 0;
                return new Weapon(name, description, damage);
            case "Consumable":
                int heal = json.containsKey("healAmount") ? (int) json.get("healAmount") : 0;
                return new Consumable(name, description, heal);
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
                return new Scroll(name, description, text, scrollHeal);
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
