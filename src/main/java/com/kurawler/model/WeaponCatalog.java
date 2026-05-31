package com.kurawler.model;

import com.kurawler.model.object.Weapon;
import java.util.Random;

/**
 * weapons.png (160×160) atlas entries — names and base-pixel crop rects for weapons_x3.png.
 * Diagonal band entries use fixed 16×16 cells; others use tight bounding boxes.
 */
public final class WeaponCatalog {

    /** { displayName, x, y, w, h } in the 160px base image. */
    public static final String[][] ENTRIES = {
            {"Dagger", "5", "2", "6", "13"},
            {"Runed Cyan Sword", "19", "10", "10", "21"},
            {"Cyan Sword", "35", "10", "10", "21"},
            {"Cyan Gem Sword", "51", "10", "10", "21"},
            {"Bronze Sword", "68", "10", "8", "21"},
            {"Rune Sword", "83", "10", "10", "21"},
            {"Steel Sword", "99", "10", "10", "21"},
            {"Steel Gem Sword", "115", "10", "10", "21"},
            {"Broadsword", "131", "6", "10", "25"},
            {"Longsword", "149", "4", "8", "27"},
            {"Wooden Staff", "3", "26", "10", "37"},
            {"War Hammer", "19", "39", "10", "24"},
            {"Spiked Mace", "35", "41", "10", "22"},
            {"Spiked Club", "51", "39", "10", "24"},
            {"Golden Sword", "67", "42", "10", "21"},
            {"Ornate Golden Sword", "83", "34", "10", "29"},
            {"Rapier", "101", "36", "6", "27"},
            {"Steel Shortsword", "115", "39", "10", "24"},
            {"Iron Sword", "131", "39", "10", "24"},
            {"Tan Sword", "147", "42", "10", "21"},
            {"Teal Sword", "5", "66", "6", "29"},
            {"Cyan Shortsword", "19", "70", "10", "25"},
            {"Cleaver", "34", "65", "12", "30"},
            {"Hand Axe", "53", "74", "9", "21"},
            {"Dark Rapier", "68", "74", "8", "21"},
            {"Blue Shortsword", "85", "74", "6", "21"},
            {"Diag Steel Sword", "96", "64", "16", "16"},
            {"Ruby Scepter", "112", "64", "16", "16"},
            {"Dark Ruby Scepter", "128", "64", "16", "16"},
            {"Diag Wooden Sword", "144", "64", "16", "16"},
            {"Diag Gold Sword", "96", "80", "16", "16"},
            {"Diag Copper Sword", "112", "80", "16", "16"},
            {"Diag Red Sword", "128", "80", "16", "16"},
            {"Golden Hammer", "144", "80", "16", "16"},
            {"Silver Dagger", "6", "105", "5", "22"},
            {"White Shortsword", "22", "108", "8", "19"},
            {"Estoc", "37", "97", "9", "30"},
            {"Ornate Silver Sword", "51", "98", "10", "29"},
            {"Shadow Sword", "67", "106", "10", "21"},
            {"Emerald Sword", "83", "106", "10", "21"},
            {"Red Sword", "99", "106", "10", "21"},
            {"Bloodied Copper Sword", "112", "96", "16", "16"},
            {"Blue Pickaxe", "128", "96", "16", "16"},
            {"Battle Axe", "144", "96", "16", "16"},
            {"Iron Hammer", "112", "112", "16", "16"},
            {"Pickaxe", "128", "112", "16", "16"},
            {"Copper Club", "144", "112", "16", "16"},
            {"Golden Gem Sword", "3", "137", "10", "22"},
            {"Golden Greatsword", "19", "129", "10", "30"},
            {"Ruby Staff", "36", "129", "8", "30"},
            {"Emerald Staff", "52", "129", "8", "30"},
            {"Crystal Staff", "69", "129", "6", "30"},
            {"Arrow", "84", "138", "7", "21"},
            {"Bow", "101", "132", "7", "25"},
            {"Short Blue Dagger", "112", "141", "8", "18"},
            {"Mallet", "120", "141", "8", "18"},
            {"Twin Daggers", "137", "130", "14", "13"},
            {"Flail Ball", "19", "39", "10", "24"},
            {"Bomb", "145", "146", "14", "13"},
    };

    public enum Kind {
        MELEE, BOW, ARROW, BOMB
    }

    private WeaponCatalog() {
    }

    public static Kind kindOf(String name) {
        if (name == null) {
            return Kind.MELEE;
        }
        String n = name.trim();
        if ("Bow".equalsIgnoreCase(n)) {
            return Kind.BOW;
        }
        if ("Arrow".equalsIgnoreCase(n)) {
            return Kind.ARROW;
        }
        if ("Bomb".equalsIgnoreCase(n)) {
            return Kind.BOMB;
        }
        return Kind.MELEE;
    }

    public static boolean isBow(String name) {
        return kindOf(name) == Kind.BOW;
    }

    public static boolean isArrow(String name) {
        return kindOf(name) == Kind.ARROW;
    }

    public static boolean isBomb(String name) {
        return kindOf(name) == Kind.BOMB;
    }

    public static boolean isFlailBall(String name) {
        return name != null && "Flail Ball".equalsIgnoreCase(name.trim());
    }

    public static boolean isRangedThrowable(String name) {
        Kind k = kindOf(name);
        return k == Kind.BOW || k == Kind.ARROW || k == Kind.BOMB;
    }

    /** Staff / scepter — Space fires a line attack in the facing direction. */
    public static boolean isStaffLike(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return n.contains("staff") || n.contains("scepter");
    }

    public static Weapon create(String name) {
        return new Weapon(name, "Found in the dungeon.", damageFor(name));
    }

    public static Weapon random(Random rng) {
        String name = ENTRIES[rng.nextInt(ENTRIES.length)][0];
        return create(name);
    }

    public static int damageFor(String name) {
        if (name == null) {
            return 5;
        }
        String n = name.toLowerCase();
        if (n.contains("greatsword") || n.contains("cleaver") || n.contains("battle axe")) {
            return 11;
        }
        if (n.contains("hammer") || n.contains("mace") || n.contains("axe") || n.contains("pickaxe")
                || n.contains("flail ball") || n.contains("club")) {
            return 9;
        }
        if (n.contains("staff") || n.contains("scepter") || n.contains("bow")) {
            return 7;
        }
        if (n.contains("bomb")) {
            return 10;
        }
        if (n.contains("dagger") || n.contains("arrow")) {
            return 4;
        }
        if (n.contains("iron") || n.contains("steel") || n.contains("golden") || n.contains("broad")) {
            return 8;
        }
        return 6;
    }
}
