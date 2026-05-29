package com.kurawler.editor;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalog of every placeable asset in the level editor, grouped into categories.
 *
 * <p>Each catalog entry is identified by its display name, which is also the
 * {@code GameObject.getName()} of the created instance. The renderer uses the
 * same name to resolve the sprite, so palette previews and placed tiles match.
 */
public class ObjectPalette {

    /** Editor palette categories, in display order. */
    public enum Category {
        WEAPONS("Weapons"),
        WEARABLES("Wearables"),
        ITEMS("Items"),
        CONTAINERS("Containers"),
        STATICS("Statics"),
        TERRAIN("Walls/Floors"),
        CHARACTERS("Characters");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** A single placeable asset definition. */
    public static final class Entry {
        public final String name;
        public final Category category;
        public final boolean isCharacter;

        Entry(String name, Category category, boolean isCharacter) {
            this.name = name;
            this.category = category;
            this.isCharacter = isCharacter;
        }
    }

    private final List<Entry> catalog = new ArrayList<>();
    private final List<GameObject> availableObjects = new ArrayList<>();
    private final List<Character> availableCharacters = new ArrayList<>();

    public ObjectPalette() {
        initializeCatalog();
    }

    private void initializeCatalog() {
        // Weapons
        addObject("Sword", Category.WEAPONS);
        addObject("Dagger", Category.WEAPONS);
        addObject("Axe", Category.WEAPONS);
        addObject("Bow", Category.WEAPONS);
        addObject("Staff", Category.WEAPONS);
        addObject("Hammer", Category.WEAPONS);

        // Wearables
        addObject("Armor", Category.WEARABLES);
        addObject("Shield", Category.WEARABLES);

        // Items
        addObject("Golden Key", Category.ITEMS);
        addObject("Health Potion", Category.ITEMS);
        addObject("Mana Potion", Category.ITEMS);
        addObject("Energy Potion", Category.ITEMS);
        addObject("Spell Scroll", Category.ITEMS);
        addObject("Gem", Category.ITEMS);
        addObject("Ring", Category.ITEMS);

        // Containers
        addObject("Chest", Category.CONTAINERS);
        addObject("Open Chest", Category.CONTAINERS);
        addObject("Crate", Category.CONTAINERS);
        addObject("Barrel", Category.CONTAINERS);
        addObject("Gold Chest", Category.CONTAINERS);

        // Dungeon statics
        addObject("Stone Column", Category.STATICS);
        addObject("Gargoyle Pool", Category.STATICS);
        addObject("Bookshelf", Category.STATICS);
        addObject("Pedestal", Category.STATICS);
        addObject("Altar", Category.STATICS);
        addObject("Statue", Category.STATICS);
        addObject("Fountain", Category.STATICS);
        addObject("Lantern", Category.STATICS);
        addObject("Door", Category.STATICS);
        addObject("Open Door", Category.STATICS);
        addObject("Stairs Down", Category.STATICS);
        addObject("Stairs Up", Category.STATICS);

        // Walls / floors
        addObject("Wall", Category.TERRAIN);
        addObject("Floor", Category.TERRAIN);
        addObject("Dark Wall", Category.TERRAIN);
        addObject("Brown Wall", Category.TERRAIN);
        addObject("Stone Floor", Category.TERRAIN);
        addObject("Cracked Floor", Category.TERRAIN);
        addObject("Mossy Floor", Category.TERRAIN);

        // Characters
        addCharacter("Adventurer");
        addCharacter("Knight");
        addCharacter("Sorcerer");
    }

    private void addObject(String name, Category category) {
        catalog.add(new Entry(name, category, false));
        GameObject obj = createObjectByName(name);
        if (obj != null) {
            availableObjects.add(obj);
        }
    }

    private void addCharacter(String name) {
        catalog.add(new Entry(name, Category.CHARACTERS, true));
        Character character = createCharacterByName(name);
        if (character != null) {
            availableCharacters.add(character);
        }
    }

    // --- Catalog accessors (used by the redesigned EditorView) ---

    public List<Entry> getCatalog() {
        return new ArrayList<>(catalog);
    }

    public List<Category> getCategoriesInOrder() {
        List<Category> ordered = new ArrayList<>();
        for (Category c : Category.values()) {
            for (Entry e : catalog) {
                if (e.category == c) {
                    ordered.add(c);
                    break;
                }
            }
        }
        return ordered;
    }

    public List<Entry> getEntriesByCategory(Category category) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : catalog) {
            if (e.category == category) {
                result.add(e);
            }
        }
        return result;
    }

    // --- Backward-compatible accessors ---

    public List<GameObject> getAvailableObjects() {
        return new ArrayList<>(availableObjects);
    }

    public List<Character> getAvailableCharacters() {
        return new ArrayList<>(availableCharacters);
    }

    public GameObject getObjectByName(String name) {
        if (name == null) {
            return null;
        }
        for (Entry e : catalog) {
            if (!e.isCharacter && e.name.equalsIgnoreCase(name)) {
                return createObjectByName(e.name);
            }
        }
        return null;
    }

    public Character getCharacterByName(String name) {
        if (name == null) {
            return null;
        }
        for (Entry e : catalog) {
            if (e.isCharacter && e.name.equalsIgnoreCase(name)) {
                return createCharacterByName(e.name);
            }
        }
        return null;
    }

    // --- Factories ---

    private GameObject createObjectByName(String name) {
        switch (name) {
            // Weapons
            case "Sword":
                return new Weapon("Sword", "A sharp, reliable blade", 5);
            case "Dagger":
                return new Weapon("Dagger", "A quick, light blade", 4);
            case "Axe":
                return new Weapon("Axe", "A heavy war axe", 8);
            case "Bow":
                return new Weapon("Bow", "A ranged shortbow", 6);
            case "Staff":
                return new Weapon("Staff", "An arcane battle staff", 6);
            case "Hammer":
                return new Weapon("Hammer", "A crushing warhammer", 9);

            // Wearables
            case "Armor":
                return new Wearable("Armor", "Protective armor (+5 DEF)", Wearable.ARMOR_DEFENSE_BONUS);
            case "Shield":
                return new Wearable("Shield", "A sturdy shield (+3 DEF)", 3);
            case "Ring":
                return new Wearable("Ring", "A magical ring (+1 DEF)", 1);

            // Items
            case "Golden Key":
                return new Key("Golden Key", "Opens a golden lock", "gold");
            case "Health Potion":
                return new Consumable("Health Potion", "Restores 20 HP", 20, 0, 0);
            case "Mana Potion":
                return new Consumable("Mana Potion", "Restores 25 mana", 0, 25, 0);
            case "Energy Potion":
                return new Consumable("Energy Potion", "Restores 25 energy", 0, 0, 25);
            case "Spell Scroll":
                return new Scroll("Spell Scroll", "A tome of arcane power",
                        "The glyphs flare with restorative magic.", 12);
            case "Gem":
                return new Key("Gem", "A precious gem worth a fortune", "gem");

            // Containers
            case "Chest":
                return new StaticContainer("Chest", "A wooden chest");
            case "Open Chest":
                return new StaticContainer("Open Chest", "An opened chest");
            case "Crate":
                return new StaticContainer("Crate", "A wooden crate");
            case "Barrel":
                return new StaticContainer("Barrel", "A wooden barrel");
            case "Gold Chest": {
                StaticContainer chest = new StaticContainer("Gold Chest", "A locked golden chest");
                chest.setLock("gold");
                return chest;
            }

            // Dungeon statics
            case "Stone Column":
                return new StaticObject("Stone Column", "A cracked pillar — search the base", false, true);
            case "Gargoyle Pool":
                return new StaticObject("Gargoyle Pool", "A searchable stone pool", true, true);
            case "Bookshelf":
                return new StaticObject("Bookshelf", "Dusty tomes — try searching", true, true);
            case "Pedestal":
                return new StaticObject("Pedestal", "A stone display pedestal", true, false);
            case "Altar":
                return new StaticObject("Altar", "An ancient stone altar", true, false);
            case "Statue":
                return new StaticObject("Statue", "A carved stone statue", false, false);
            case "Fountain":
                return new StaticObject("Fountain", "A bubbling fountain", false, false);
            case "Lantern":
                return new StaticObject("Lantern", "A hanging lantern", true, false);
            case "Door":
                return new StaticObject("Door", "A closed door", false, false);
            case "Open Door":
                return new StaticObject("Open Door", "An open doorway", true, false);
            case "Stairs Down":
                return new StaticObject("Stairs Down", "Stairs leading down", true, false);
            case "Stairs Up":
                return new StaticObject("Stairs Up", "Stairs leading up", true, false);

            // Walls / floors
            case "Wall":
                return new StaticObject("Wall", "A solid wall that blocks movement", false, false);
            case "Floor":
                return new StaticObject("Floor", "Stone floor tile", true, false);
            case "Dark Wall":
                return new StaticObject("Dark Wall", "Dark stone wall", false, false);
            case "Brown Wall":
                return new StaticObject("Brown Wall", "Brown brick wall", false, false);
            case "Stone Floor":
                return new StaticObject("Stone Floor", "Stone floor tile", true, false);
            case "Cracked Floor":
                return new StaticObject("Cracked Floor", "Cracked stone floor", true, false);
            case "Mossy Floor":
                return new StaticObject("Mossy Floor", "Mossy stone floor", true, false);

            default:
                return null;
        }
    }

    private Character createCharacterByName(String name) {
        switch (name) {
            case "Adventurer":
                return new Hero("Adventurer");
            case "Knight":
                return new Knight();
            case "Sorcerer":
                return new Sorcerer();
            default:
                return null;
        }
    }
}
