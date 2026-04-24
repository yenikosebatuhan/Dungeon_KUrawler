package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Inventory;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Weapon;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapSerializer {

    public boolean saveGameState(GameSession session, String filename) {
        try {
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(buildStateJson(session));
            }
            System.out.println("[Persistence] Saved game state to " + filename);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String buildStateJson(GameSession session) {
        Map map = session.getMap();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"hero\": ").append(buildHeroJson(session.getHero())).append(",\n");
        sb.append("  \"cells\": [");

        List<String> entries = new ArrayList<>();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                GridCell cell = map.getCell(row, col);
                if (!cell.isWalkable() || cell.hasObjects()) {
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
                if (!cell.isWalkable() || cell.hasObjects()) {
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
        }
        sb.append(" }");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\t", "\\t");
    }
}
