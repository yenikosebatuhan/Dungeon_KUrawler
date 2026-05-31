package com.kurawler.model;

import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Wearable;

/** Names and model mappings for the pre-sliced non-weapon resource asset folders. */
public final class ResourceAssetCatalog {

    public static final String[][] ITEM_TILES = {
            {"tile000", "Bone Key"}, {"tile001", "Silver Key"}, {"tile002", "Gold Key"},
            {"tile003", "Copper Key"}, {"tile004", "Key Ring"}, {"tile005", "Brass Key"},
            {"tile006", "Health Potion"}, {"tile007", "Mana Potion"}, {"tile008", "Energy Potion"},
            {"tile009", "Ruby Ring"}, {"tile010", "Emerald Ring"}, {"tile011", "Sapphire Ring"},
            {"tile012", "Bone Skull"}, {"tile013", "Cracked Skull"}, {"tile014", "Gold Gem"},
            {"tile015", "Small Gold Gem"}, {"tile016", "Amber Gem"}, {"tile017", "Gold Coins"},
            {"tile018", "Crystal Orb"}, {"tile019", "Spell Book"}, {"tile020", "Glyph Scroll"},
            {"tile021", "Rune Scroll"}, {"tile022", "Sigil Scroll"}, {"tile023", "Curse Scroll"},
    };

    public static final String[][] CONTAINER_TILES = {
            {"tile000", "Blue Banded Chest"}, {"tile001", "Gold Banded Chest"},
            {"tile002", "Low Wooden Crate"}, {"tile003", "Tall Wooden Crate"},
            {"tile004", "Open Blue Chest"}, {"tile005", "Open Gold Chest"},
            {"tile006", "Wooden Box"}, {"tile007", "Storage Box"},
            {"tile008", "Full Blue Chest"}, {"tile009", "Full Gold Chest"},
            {"tile010", "Small Wooden Box"}, {"tile011", "Red Wooden Crate"},
            {"tile012", "Bronze Treasure Chest"}, {"tile013", "Red Locked Chest"},
            {"tile014", "Blue Locked Chest"}, {"tile015", "Red Crate"},
            {"tile016", "Copper Chest"}, {"tile017", "Orange Chest"},
            {"tile018", "Open Copper Chest"}, {"tile019", "Leather Pouch"},
            {"tile020", "Open Bronze Chest"}, {"tile021", "Open Orange Chest"},
            {"tile022", "Full Copper Chest"}, {"tile023", "Magic Pouch"},
    };

    public static Item createItem(String name) {
        String n = lower(name);
        if (n.equals("armor") || n.contains("armor") || n.contains("mail") || n.contains("plate")) {
            return new Wearable(name, "Body armor (+5 DEF)", 5);
        }
        if (n.equals("shield") || n.contains("shield")) {
            return new Wearable(name, "Shield (+3 DEF)", 3);
        }
        if (n.equals("ring")) {
            return new Wearable(name, "Ring (+1 DEF)", 1);
        }
        if (n.contains("bone key")) return new Key(name, "Opens bone-marked dungeon locks", "bone");
        if (n.contains("silver key")) return new Key(name, "Opens silver locks", "silver");
        if (n.contains("gold key")) return new Key(name, "Opens gold locks", "gold");
        if (n.contains("copper key")) return new Key(name, "Opens copper locks", "copper");
        if (n.contains("key ring")) return new Key(name, "A master ring for common locks", "keyring");
        if (n.contains("brass key")) return new Key(name, "Opens brass locks", "brass");

        if (n.contains("health potion")) return new Consumable(name, "Restores 25 HP", 25, 0, 0);
        if (n.contains("mana potion")) return new Consumable(name, "Restores 30 mana", 0, 30, 0);
        if (n.contains("energy potion")) return new Consumable(name, "Restores 35 energy", 0, 0, 35);

        if (n.contains("ruby ring")) return new Wearable(name, "Ruby ring (+2 DEF)", 2);
        if (n.contains("emerald ring")) return new Wearable(name, "Emerald ring (+3 DEF)", 3);
        if (n.contains("sapphire ring")) return new Wearable(name, "Sapphire ring (+4 DEF)", 4);

        if (n.contains("bone skull")) return new Key(name, "A grim relic used by bone shrines", "bone_relic");
        if (n.contains("cracked skull")) return new Key(name, "A cracked relic that marks a cursed cache", "cursed_relic");
        if (n.contains("gold gem")) return new Key(name, "A valuable gold gem treasure", "gold_gem");
        if (n.contains("small gold gem")) return new Key(name, "A small gold gem treasure", "small_gold_gem");
        if (n.contains("amber gem")) return new Key(name, "An amber gem treasure", "amber_gem");
        if (n.contains("gold coins")) return new Key(name, "Spendable dungeon gold", "gold_coins");
        if (n.contains("crystal orb")) return new Scroll(name, "Crystal focus (+14 HP)", "The orb releases a restorative pulse.", 14, 0, 0);
        if (n.contains("spell book")) return new Scroll(name, "Spell book (+18 HP)", "A full spell restores a large burst of health.", 18, 0, 0);
        if (n.contains("glyph scroll")) return new Scroll(name, "Glyph scroll (+25 Mana)", "Glyphs flare and recharge your arcane reserves.", 0, 25, 0);
        if (n.contains("rune scroll")) return new Scroll(name, "Rune scroll (+30 Energy)", "Runes hum and replenish physical stamina.", 0, 0, 30);
        if (n.contains("sigil scroll")) return new Scroll(name, "Sigil scroll (+12 HP)", "A sigil seals deeper wounds.", 12, 0, 0);
        if (n.contains("curse scroll")) return new Scroll(name, "Curse scroll (+4 HP, +15 Mana)", "Dangerous magic restores both mind and flesh.", 4, 15, 0);

        if (n.contains("mana")) return new Consumable(name, "Restores mana", 0, 20, 0);
        if (n.contains("energy")) return new Consumable(name, "Restores energy", 0, 0, 20);
        if (n.contains("potion")) return new Consumable(name, "Restores health", 20, 0, 0);
        if (n.contains("key")) return new Key(name, keyDescription(n), keyId(n));
        if (n.contains("ring") || n.contains("amulet")) return new Wearable(name, "A magical trinket", 1);
        if (n.contains("scroll") || n.contains("book")) return new Scroll(name, "Ancient writing", scrollText(n), 5);
        return new Key(name, treasureDescription(n), "treasure");
    }

    public static StaticContainer createContainer(String name) {
        StaticContainer c = new StaticContainer(name, containerDescription(name));
        String n = lower(name);
        if (n.contains("blue locked")) {
            c.setLock("silver");
        } else if (n.contains("red locked")) {
            c.setLock("gold");
        } else if (n.equals("copper chest")) {
            c.setLock("copper");
        } else if (n.equals("bronze treasure chest")) {
            c.setLock("brass");
        } else if (n.equals("magic pouch")) {
            c.setLock("keyring");
        } else if (n.equals("leather pouch")) {
            c.setLock("bone");
        }
        return c;
    }

    public static String containerSpriteName(String name, boolean empty) {
        if (name == null || !empty) {
            return name;
        }
        String n = lower(name);
        if (n.contains("open")) {
            return name;
        }
        if (n.contains("blue")) return "Open Blue Chest";
        if (n.contains("gold")) return "Open Gold Chest";
        if (n.contains("bronze")) return "Open Bronze Chest";
        if (n.contains("copper")) return "Open Copper Chest";
        if (n.contains("orange") || n.contains("amber")) return "Open Orange Chest";
        return name;
    }

    public static StaticObject createStatic(String name) {
        String n = lower(name);
        boolean passable = n.contains("floor") || n.contains("rug") || n.contains("carpet")
                || n.contains("candle") || n.contains("sign") || n.contains("stairs")
                || n.contains("portal") || n.contains("gateway");
        boolean searchable = n.contains("crate") || n.contains("barrel") || n.contains("box")
                || n.contains("bookshelf") || n.contains("book") || n.contains("pool")
                || n.contains("niche") || n.contains("statue") || n.contains("sarcophagus")
                || n.contains("pedestal") || n.contains("sign");
        return new StaticObject(name, staticDescription(n), passable, searchable);
    }

    public static String wallStaticName(String tileId) {
        int idx = tileIndex(tileId);
        int row = idx / 30;
        int col = idx % 30;
        String suffix = two(row) + "-" + two(col);
        if (idx < 0) return "Dungeon Tile";
        if (row <= 1) return "Wall Cap " + suffix;
        if (row == 2) return decoratedWallName(col, suffix);
        if (row == 3) return upperStructureName(col, suffix);
        if (row == 4 || row == 5) return floorOrRugName(idx, row, col, suffix);
        if (row == 6) return propName(row, col, suffix);
        if (row == 7) return lowerPropName(col, suffix);
        return "Dungeon Tile " + suffix;
    }

    public static boolean isTerrainWall(String name) {
        String n = lower(name);
        return n.contains("wall") || n.contains("arch") || n.contains("door frame")
                || n.contains("window frame") || n.contains("stone pillar edge");
    }

    public static boolean isTerrainFloor(String name) {
        String n = lower(name);
        return n.contains("floor") || n.contains("ground");
    }

    private static String decoratedWallName(int col, String suffix) {
        if (col == 3) return "Vine Wall " + suffix;
        if (col == 4) return "Lower Grate Wall " + suffix;
        if (col == 5) return "Barred Wall " + suffix;
        if (col == 6 || col == 7 || col == 8) return "Ring Wall " + suffix;
        if (col >= 11 && col <= 13) return "Torch Wall " + suffix;
        if (col >= 16 && col <= 23) return "Banner Wall " + suffix;
        if (col == 29) return "Stone Column Wall " + suffix;
        return "Brick Wall " + suffix;
    }

    private static String upperStructureName(int col, String suffix) {
        if (col >= 8 && col <= 11) return "Square Arch Wall " + suffix;
        if (col >= 12 && col <= 15) return "Window Frame Wall " + suffix;
        if (col >= 16 && col <= 24) return "Door Frame Wall " + suffix;
        if (col >= 25 && col <= 27) return "Rug Panel " + suffix;
        if (col >= 28) return "Stone Rug Panel " + suffix;
        return "Dungeon Ground " + suffix;
    }

    private static String floorOrRugName(int idx, int row, int col, String suffix) {
        if (col >= 0 && col <= 3) return "Dungeon Floor " + suffix;
        if (col >= 8 && col <= 15) return "Arch Wall " + suffix;
        if (col >= 16 && col <= 24) return "Doorway Wall " + suffix;
        if (col >= 25 && col <= 29) return (idx < 150 ? "Red Rug " : "Tan Rug ") + suffix;
        return "Dungeon Floor " + suffix;
    }

    private static String propName(int row, int col, String suffix) {
        if (col >= 8 && col <= 15) return "Arch Wall " + suffix;
        if (col == 17 || col == 18) return "Barrel " + suffix;
        if (col == 20 || col == 21 || col == 22) return "Dungeon Floor " + suffix;
        if (col >= 24 && col <= 29) return "Rug Border " + suffix;
        return "Dungeon Tile " + suffix;
    }

    private static String lowerPropName(int col, String suffix) {
        if (col >= 1 && col <= 8) return "Candle " + suffix;
        if (col == 11 || col == 12) return "Stone Pillar Edge " + suffix;
        if (col >= 16 && col <= 18) return "Stairs " + suffix;
        if (col >= 19 && col <= 22) return "Storage Crate " + suffix;
        if (col >= 24 && col <= 29) return "Rug Corner " + suffix;
        return "Dungeon Tile " + suffix;
    }

    private static int tileIndex(String tileId) {
        try {
            return Integer.parseInt(tileId.replace("tile", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : Integer.toString(n);
    }

    private static String keyDescription(String n) {
        if (n.contains("bone")) return "A bone-carved key";
        if (n.contains("silver")) return "Opens a silver lock";
        if (n.contains("gold")) return "Opens a golden lock";
        if (n.contains("copper")) return "Opens an old copper lock";
        return "Unlocks something";
    }

    private static String keyId(String n) {
        if (n.contains("silver")) return "silver";
        if (n.contains("gold")) return "gold";
        if (n.contains("copper")) return "copper";
        if (n.contains("bone")) return "bone";
        return "key";
    }

    private static String scrollText(String n) {
        if (n.contains("curse")) return "The ink crawls across the parchment.";
        if (n.contains("glyph")) return "Glyphs flare with stored magic.";
        if (n.contains("rune")) return "Runes hum with quiet power.";
        return "Faded arcane writing covers the page.";
    }

    private static String treasureDescription(String n) {
        if (n.contains("coin")) return "A pile of gold coins";
        if (n.contains("orb")) return "A polished crystal orb";
        if (n.contains("gem")) return "A valuable gemstone";
        if (n.contains("skull")) return "A grim dungeon curio";
        return "A valuable treasure";
    }

    private static String containerDescription(String name) {
        String n = lower(name);
        if (n.contains("pouch")) return "A small pouch";
        if (n.contains("crate") || n.contains("box")) return "A storage crate";
        if (n.contains("open")) return "An open chest";
        if (n.contains("full")) return "A chest full of treasure";
        return "A dungeon chest";
    }

    private static String staticDescription(String n) {
        if (n.contains("wall")) return "A dungeon wall piece";
        if (n.contains("floor") || n.contains("ground")) return "A dungeon floor tile";
        if (n.contains("rug") || n.contains("carpet")) return "A woven dungeon rug";
        if (n.contains("candle")) return "A small candle";
        if (n.contains("barrel")) return "A searchable barrel";
        if (n.contains("crate")) return "A searchable crate";
        return "A dungeon prop";
    }

    private ResourceAssetCatalog() {
    }
}
