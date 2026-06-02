package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.achievement.Achievement;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MapSerializer {

    public boolean saveGameState(GameSession session, String filename) {
        String saveName = deriveSaveName(filename);
        return saveGameState(session, filename, saveName);
    }

    public boolean saveGameState(GameSession session, String filename, String saveName) {
        try {
            ensureParentDirectory(filename);
            // Phase 2 spec: save files must be produced via Gson (§Phase2 Save/Load).
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(buildStateJson(session, saveName));
            try (FileWriter writer = new FileWriter(filename)) {
                gson.toJson(element, writer);
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

    private String buildStateJson(GameSession session, String saveName) {
        Map map = session.getMap();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"saveName\": \"").append(escape(saveName)).append("\",\n");
        sb.append("  \"savedAt\": \"").append(Instant.now()).append("\",\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"playDurationTimer\": ").append(session.getPlayDurationTimer()).append(",\n");
        sb.append("  \"currentMode\": \"").append(escape(session.getCurrentMode())).append("\",\n");
        sb.append("  \"difficulty\": \"").append(escape(session.getDifficulty())).append("\",\n");
        sb.append("  \"enemiesDefeatedCount\": ").append(session.getEnemiesDefeatedCount()).append(",\n");
        sb.append("  \"itemsCollectedCount\": ").append(session.getItemsCollectedCount()).append(",\n");
        sb.append("  \"floorsClearedCount\": ").append(session.getFloorsClearedCount()).append(",\n");
        sb.append("  \"targetObjective\": ").append(session.getTargetObjective() != null
                ? buildObjectJson(session.getTargetObjective()) : "null").append(",\n");
        sb.append("  \"hero\": ").append(buildHeroJson(session.getHero())).append(",\n");
        sb.append("  \"shadowClone\": ").append(buildShadowCloneJson(session.getShadowClone())).append(",\n");
        sb.append("  \"achievements\": ").append(buildAchievementsJson(session.getAchievementManager())).append(",\n");
        sb.append("  \"enemies\": ").append(buildEnemiesJson(session)).append(",\n");
        sb.append("  \"cells\": [");

        List<String> entries = new ArrayList<>();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                StaticObject so = cell.getStaticObject();
                if (so != null || cell.hasObjects()) {
                    entries.add(buildCellJson(cell, row, col));
                }
            }
        }

        if (!entries.isEmpty()) {
            sb.append("\n    ").append(String.join(",\n    ", entries)).append("\n  ");
        }
        sb.append("]\n}");
        return sb.toString();
    }

    private void ensureParentDirectory(String filename) throws IOException {
        Path parent = Paths.get(filename).toAbsolutePath().getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
    }

    private String buildEnemiesJson(GameSession session) {
        List<String> entries = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>(session.getEnemies());
        collectMapEnemies(session.getMap(), enemies);
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                entries.add(buildEnemyJson(enemy));
            }
        }
        return "[" + String.join(", ", entries) + "]";
    }

    private void collectMapEnemies(Map map, List<Enemy> enemies) {
        if (map == null) return;
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (cell != null && cell.getOccupyingCharacter() instanceof Enemy) {
                    Enemy enemy = (Enemy) cell.getOccupyingCharacter();
                    if (!enemies.contains(enemy)) {
                        enemies.add(enemy);
                    }
                }
            }
        }
    }

    private String buildEnemyJson(Enemy enemy) {
        String type = enemy instanceof Sorcerer ? "Sorcerer"
                : enemy instanceof Knight ? "Knight"
                : enemy instanceof Guard ? "Guard"
                : enemy instanceof Assassin ? "Assassin" : enemy.getClass().getSimpleName();
        return "{ \"type\": \"" + type + "\""
                + ", \"name\": \"" + jsonEscape(enemy.getName()) + "\""
                + ", \"row\": " + enemy.getRow()
                + ", \"col\": " + enemy.getCol()
                + ", \"hp\": " + enemy.getHealth()
                + ", \"atk\": " + enemy.getAttackPower()
                + ", \"mana\": " + enemy.getMana()
                + ", \"energy\": " + enemy.getEnergy()
                + ", \"str\": " + enemy.getStrength()
                + ", \"def\": " + enemy.getDefensePower()
                + " }";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildShadowCloneJson(ShadowClone shadowClone) {
        if (shadowClone == null) {
            return "null";
        }

        return "{ \"row\": " + shadowClone.getRow()
                + ", \"col\": " + shadowClone.getCol()
                + ", \"hp\": " + shadowClone.getHealth()
                + ", \"remainingMs\": " + shadowClone.getRemainingMs()
                + " }";
    }

    private String buildAchievementsJson(AchievementManager manager) {
        if (manager == null) {
            return "null";
        }

        List<String> unlocked = new ArrayList<>();
        for (Achievement achievement : manager.getAchievements()) {
            if (achievement.isUnlocked()) {
                unlocked.add("\"" + escape(achievement.getName()) + "\"");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("\"totalMoves\": ").append(manager.getTotalMoves());
        sb.append(", \"shadowCloneMoves\": ").append(manager.getShadowCloneMoves());
        sb.append(", \"enemiesKilled\": ").append(manager.getEnemiesKilled());
        sb.append(", \"itemsCollected\": ").append(manager.getItemsCollected());
        sb.append(", \"damageTaken\": ").append(manager.getDamageTaken());
        sb.append(", \"unlocked\": [").append(String.join(", ", unlocked)).append("]");
        sb.append(" }");
        return sb.toString();
    }

    private String buildHeroJson(Hero hero) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("\"name\": \"").append(escape(hero.getName())).append("\"");
        sb.append(", \"row\": ").append(hero.getRow());
        sb.append(", \"col\": ").append(hero.getCol());
        sb.append(", \"hp\": ").append(hero.getHealth());
        sb.append(", \"maxHp\": ").append(hero.getMaxHealth());
        sb.append(", \"mana\": ").append(hero.getMana());
        sb.append(", \"energy\": ").append(hero.getEnergy());
        sb.append(", \"maxEnergy\": ").append(hero.getMaxEnergy());
        sb.append(", \"str\": ").append(hero.getStrength());
        sb.append(", \"def\": ").append(hero.getDefensePower());

        String wpnName = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getName() : "";
        sb.append(", \"equippedWeapon\": \"").append(escape(wpnName)).append("\"");
        String armorName = hero.getEquippedArmor() != null ? hero.getEquippedArmor().getName() : "";
        String shieldName = hero.getEquippedShield() != null ? hero.getEquippedShield().getName() : "";
        String ringName = hero.getEquippedRing() != null ? hero.getEquippedRing().getName() : "";
        sb.append(", \"equippedWearable\": \"").append(escape(armorName)).append("\"");
        sb.append(", \"equippedShield\": \"").append(escape(shieldName)).append("\"");
        sb.append(", \"equippedRing\": \"").append(escape(ringName)).append("\"");

        // Inventory — collect all non-null items
        Inventory inv = hero.getInventory();
        List<String> items = new ArrayList<>();
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                Item item = inv.getItem(r, c);
                if (item != null) {
                    items.add(buildObjectJson(item));
                }
            }
        }
        sb.append(", \"inventory\": [");
        if (!items.isEmpty()) {
            sb.append(String.join(", ", items));
        }
        sb.append("]");
        sb.append(" }");
        return sb.toString();
    }

    // kept for callers that only need map serialization (no hero)
    public boolean saveMapToFile(Map map, String filename) {
        try {
            ensureParentDirectory(filename);
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(buildMapJson(map));
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
        return buildMapJson(map);
    }

    /** Serialise multiple maps as a single world JSON file. */
    public String worldToJson(java.util.Map<String, Map> maps, String startMapId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format\": \"world\",\n");
        sb.append("  \"startMapId\": \"").append(escape(startMapId)).append("\",\n");
        sb.append("  \"maps\": {\n");
        List<String> mapEntries = new ArrayList<>();
        for (java.util.Map.Entry<String, Map> entry : maps.entrySet()) {
            mapEntries.add("    \"" + escape(entry.getKey()) + "\": " + buildMapJson(entry.getValue()));
        }
        sb.append(String.join(",\n", mapEntries));
        sb.append("\n  }\n}");
        return sb.toString();
    }

    /** Save world JSON to the maps directory. Returns true on success. */
    public boolean saveWorldToFile(java.util.Map<String, Map> maps, String startMapId, String filename) {
        Path dir = Paths.get("maps");
        try {
            java.nio.file.Files.createDirectories(dir);
            String json = worldToJson(maps, startMapId);
            try (FileWriter fw = new FileWriter(dir.resolve(filename).toFile())) {
                fw.write(json);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String buildMapJson(Map map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"cells\": [");

        List<String> entries = new ArrayList<>();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                StaticObject so = cell.getStaticObject();
                com.kurawler.model.character.Character ch = cell.getOccupyingCharacter();
                boolean hasChar = (ch instanceof Enemy && !(ch instanceof ShadowClone)) || ch instanceof Hero;
                if (so != null || cell.hasObjects() || hasChar) {
                    entries.add(buildCellJson(cell, row, col, true));
                }
            }
        }

        if (!entries.isEmpty()) {
            sb.append("\n    ").append(String.join(",\n    ", entries)).append("\n  ");
        }
        sb.append("]\n}");
        return sb.toString();
    }

    private String buildCellJson(GridCell cell, int row, int col) {
        return buildCellJson(cell, row, col, false);
    }

    private String buildCellJson(GridCell cell, int row, int col, boolean includeCharacter) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"row\": ").append(row);
        sb.append(", \"col\": ").append(col);
        sb.append(", \"walkable\": ").append(cell.isWalkable());

        StaticObject so = cell.getStaticObject();
        if (so != null) {
            sb.append(", \"staticObject\": {");
            if (so instanceof MapPortal) {
                MapPortal portal = (MapPortal) so;
                sb.append(" \"type\": \"MapPortal\"");
                sb.append(", \"name\": \"").append(escape(portal.getName())).append("\"");
                sb.append(", \"description\": \"").append(escape(portal.getDescription())).append("\"");
                String tid = portal.getTargetMapId();
                sb.append(", \"targetMapId\": \"").append(escape(tid != null ? tid : "")).append("\"");
                sb.append(", \"targetRow\": ").append(portal.getTargetRow());
                sb.append(", \"targetCol\": ").append(portal.getTargetCol());
                if (portal.getRequiredKeyId() != null) {
                    sb.append(", \"requiredKeyId\": \"").append(escape(portal.getRequiredKeyId())).append("\"");
                }
                sb.append(", \"unlocked\": ").append(portal.isUnlocked());
                sb.append(", \"stairsMode\": ").append(portal.isStairsMode());
                if (portal.getRotation() != 0)
                    sb.append(", \"rotation\": ").append(portal.getRotation());
            } else {
                sb.append(" \"name\": \"").append(escape(so.getName())).append("\"");
                sb.append(", \"description\": \"").append(escape(so.getDescription())).append("\"");
                sb.append(", \"passable\": ").append(so.isPassable());
                sb.append(", \"searchable\": ").append(so.isSearchable());
                if (so.getRotation() != 0)
                    sb.append(", \"rotation\": ").append(so.getRotation());
                if (so.getHiddenItem() != null) {
                    sb.append(", \"hiddenItem\": ").append(buildObjectJson(so.getHiddenItem()));
                }
            }
            sb.append(" }");
        }

        List<GameObject> objects = cell.getObjects();
        if (!objects.isEmpty()) {
            List<String> objEntries = new ArrayList<>();
            for (GameObject obj : objects) {
                objEntries.add(buildObjectJson(obj));
            }
            sb.append(", \"objects\": [").append(String.join(", ", objEntries)).append("]");
        }

        if (includeCharacter) {
            com.kurawler.model.character.Character ch = cell.getOccupyingCharacter();
            if (ch instanceof Enemy && !(ch instanceof ShadowClone)) {
                Enemy enemy = (Enemy) ch;
                String type = enemy instanceof Sorcerer ? "Sorcerer"
                        : enemy instanceof Knight  ? "Knight"
                        : enemy instanceof Guard   ? "Guard"
                        : enemy instanceof Assassin ? "Assassin" : "Guard";
                sb.append(", \"character\": { \"type\": \"").append(type).append("\"");
                sb.append(", \"name\": \"").append(escape(enemy.getName())).append("\" }");
            } else if (ch instanceof Hero) {
                sb.append(", \"character\": { \"type\": \"Hero\"");
                sb.append(", \"name\": \"").append(escape(ch.getName())).append("\" }");
            }
        }

        sb.append(" }");
        return sb.toString();
    }

    private String buildObjectJson(GameObject obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"").append(obj.getClass().getSimpleName()).append("\"");
        sb.append(", \"name\": \"").append(escape(obj.getName())).append("\"");
        sb.append(", \"description\": \"").append(escape(obj.getDescription())).append("\"");

        if (obj instanceof Weapon) {
            sb.append(", \"damage\": ").append(((Weapon) obj).getDamage());
        } else if (obj instanceof Consumable) {
            Consumable consumable = (Consumable) obj;
            sb.append(", \"healAmount\": ").append(consumable.getHealAmount());
            sb.append(", \"manaAmount\": ").append(consumable.getManaAmount());
            sb.append(", \"energyAmount\": ").append(consumable.getEnergyAmount());
        } else if (obj instanceof Key) {
            sb.append(", \"keyId\": \"").append(escape(((Key) obj).getKeyId())).append("\"");
        } else if (obj instanceof Wearable) {
            sb.append(", \"defenseBonus\": ").append(((Wearable) obj).getDefenseBonus());
        } else if (obj instanceof Scroll) {
            Scroll scroll = (Scroll) obj;
            sb.append(", \"text\": \"").append(escape(scroll.getText())).append("\"");
            sb.append(", \"healAmount\": ").append(scroll.getHealAmount());
            sb.append(", \"manaAmount\": ").append(scroll.getManaAmount());
            sb.append(", \"energyAmount\": ").append(scroll.getEnergyAmount());
        } else if (obj instanceof PortableContainer) {
            PortableContainer container = (PortableContainer) obj;
            sb.append(", \"capacity\": ").append(container.getCapacity());
            sb.append(", \"contents\": ").append(buildItemListJson(container.getContents()));
        } else if (obj instanceof StaticContainer) {
            StaticContainer container = (StaticContainer) obj;
            sb.append(", \"contents\": ").append(buildItemListJson(container.getContents()));
        }
        if (obj.getRotation() != 0)
            sb.append(", \"rotation\": ").append(obj.getRotation());
        sb.append(" }");
        return sb.toString();
    }

    private String buildItemListJson(List<Item> items) {
        List<String> entries = new ArrayList<>();
        for (Item item : items) {
            entries.add(buildObjectJson(item));
        }
        return "[" + String.join(", ", entries) + "]";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\t", "\\t");
    }
}
