package com.kurawler.model;

import com.kurawler.model.object.Wearable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** Body armor DEF bonuses (+3 … +8 per piece, rolled when the item is created). */
public final class WearableCatalog {

    public static final int ARMOR_DEF_MIN = 3;
    public static final int ARMOR_DEF_MAX = 8;

    /** { displayName, typicalDefense } — typical values inside 3–8 for named tiers. */
    public static final String[][] BODY_ARMOR = {
            {"Leather Armor", "3"},
            {"Studded Leather", "4"},
            {"Chain Armor", "4"},
            {"Chain Mail", "5"},
            {"Scale Mail", "5"},
            {"Plate Harness", "6"},
            {"Enchanted Plate", "7"},
            {"Heavy Armor", "7"},
            {"Knight's Plate", "7"},
            {"Royal Plate", "8"},
            {"Royal Armor", "8"},
            {"Adamant Plate", "8"},
            {"Armor", "5"},
    };

    public static final String[] RANDOM_ARMOR_NAMES = {
            "Leather Armor",
            "Studded Leather",
            "Chain Mail",
            "Scale Mail",
            "Plate Harness",
            "Enchanted Plate",
            "Knight's Plate",
            "Royal Plate",
            "Adamant Plate",
    };

    private WearableCatalog() {}

    public static boolean isBodyArmorName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        if (n.contains("shield") || n.contains("ring") || n.contains("amulet")) {
            return false;
        }
        return n.contains("armor") || n.contains("mail") || n.contains("plate");
    }

    /** Random DEF in [ARMOR_DEF_MIN, ARMOR_DEF_MAX] for a new body-armor instance. */
    public static int rollArmorDefense(Random rng) {
        Random source = rng != null ? rng : ThreadLocalRandom.current();
        int span = ARMOR_DEF_MAX - ARMOR_DEF_MIN + 1;
        return ARMOR_DEF_MIN + source.nextInt(span);
    }

    public static int defenseForArmor(String name) {
        if (name == null || name.isEmpty()) {
            return ARMOR_DEF_MIN;
        }
        for (String[] row : BODY_ARMOR) {
            if (row[0].equalsIgnoreCase(name)) {
                return Integer.parseInt(row[1]);
            }
        }
        String n = name.toLowerCase();
        if (n.contains("adamant") || n.contains("royal")) {
            return 8;
        }
        if (n.contains("knight") || n.contains("enchanted") || n.contains("heavy")) {
            return 7;
        }
        if (n.contains("plate") || n.contains("harness") || n.contains("scale")) {
            return 6;
        }
        if (n.contains("chain") || n.contains("mail")) {
            return 5;
        }
        if (n.contains("studded") || n.contains("leather")) {
            return 4;
        }
        return ARMOR_DEF_MIN;
    }

    public static Wearable createArmor(String name) {
        return createArmor(name, null, null);
    }

    public static Wearable createArmor(String name, String flavorDescription) {
        return createArmor(name, flavorDescription, null);
    }

    public static Wearable createArmor(String name, String flavorDescription, Random rng) {
        int def = rollArmorDefense(rng);
        String desc;
        if (flavorDescription != null && !flavorDescription.isBlank()) {
            desc = flavorDescription.trim() + " (+" + def + " DEF)";
        } else {
            desc = name + " (+" + def + " DEF)";
        }
        return new Wearable(name, desc, def);
    }

    public static Wearable createRandomArmor(Random rng) {
        String name = RANDOM_ARMOR_NAMES[rng.nextInt(RANDOM_ARMOR_NAMES.length)];
        return createArmor(name, null, rng);
    }

    public static Wearable createShield(String name) {
        String n = (name == null || name.isBlank()) ? "Shield" : name;
        return new Wearable(n, "Shield (+3 DEF)", 3);
    }
}
