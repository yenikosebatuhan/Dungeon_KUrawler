package com.kurawler.persistence;

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
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
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
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(buildStateJson(session, saveName));
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
                boolean hasSpecialStatic = so != null && (!so.isPassable() || so.isSearchable());
                if (hasSpecialStatic || cell.hasObjects()) {
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
                : enemy instanceof Knight ? "Knight" : enemy.getClass().getSimpleName();
        return "{ \"type\": \"" + type + "\""
                + ", \"row\": " + enemy.getRow()
                + ", \"col\": " + enemy.getCol()
                + ", \"hp\": " + enemy.getHealth()
                + ", \"mana\": " + enemy.getMana()
                + ", \"energy\": " + enemy.getEnergy()
                + ", \"str\": " + enemy.getStrength()
                + ", \"def\": " + enemy.getDefensePower()
                + " }";
    }

    private String buildShadowCloneJson(ShadowClone shadowClone) {
        if (shadowClone == null) {
            return "null";
        }

        return "{ \"row\": " + shadowClone.getRow()
                + ", \"col\": " + shadowClone.getCol()
                + ", \"hp\": " + shadowClone.getHealth()
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
        sb.append(", \"mana\": ").append(hero.getMana());
        sb.append(", \"energy\": ").append(hero.getEnergy());
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
                boolean hasSpecialStatic = so != null && (!so.isPassable() || so.isSearchable());
                if (hasSpecialStatic || cell.hasObjects()) {
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

    private String buildCellJson(GridCell cell, int row, int col) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"row\": ").append(row);
        sb.append(", \"col\": ").append(col);
        sb.append(", \"walkable\": ").append(cell.isWalkable());

        StaticObject so = cell.getStaticObject();
        if (so != null && (!so.isPassable() || so.isSearchable())) {
            sb.append(", \"staticObject\": {");
            sb.append(" \"name\": \"").append(escape(so.getName())).append("\"");
            sb.append(", \"description\": \"").append(escape(so.getDescription())).append("\"");
            sb.append(", \"passable\": ").append(so.isPassable());
            sb.append(", \"searchable\": ").append(so.isSearchable());
            if (so.getHiddenItem() != null) {
                sb.append(", \"hiddenItem\": ").append(buildObjectJson(so.getHiddenItem()));
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
            sb.append(", \"healAmount\": ").append(((Consumable) obj).getHealAmount());
        } else if (obj instanceof Key) {
            sb.append(", \"keyId\": \"").append(escape(((Key) obj).getKeyId())).append("\"");
        } else if (obj instanceof Wearable) {
            sb.append(", \"defenseBonus\": ").append(((Wearable) obj).getDefenseBonus());
        } else if (obj instanceof Scroll) {
            Scroll scroll = (Scroll) obj;
            sb.append(", \"text\": \"").append(escape(scroll.getText())).append("\"");
            sb.append(", \"healAmount\": ").append(scroll.getHealAmount());
        } else if (obj instanceof PortableContainer) {
            PortableContainer container = (PortableContainer) obj;
            sb.append(", \"capacity\": ").append(container.getCapacity());
            sb.append(", \"contents\": ").append(buildItemListJson(container.getContents()));
        } else if (obj instanceof StaticContainer) {
            StaticContainer container = (StaticContainer) obj;
            sb.append(", \"contents\": ").append(buildItemListJson(container.getContents()));
        }
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
