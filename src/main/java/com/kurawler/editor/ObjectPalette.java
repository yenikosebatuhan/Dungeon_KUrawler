package com.kurawler.editor;

import java.util.ArrayList;
import java.util.List;

import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;

public class ObjectPalette {

    private final List<GameObject> availableObjects;
    private final List<Character> availableCharacters;

    public ObjectPalette() {
        this.availableObjects = new ArrayList<>();
        this.availableCharacters = new ArrayList<>();
        initializePalette();
    }

    private void initializePalette() {
        availableObjects.add(new StaticObject("Wall", "A solid wall that blocks movement"));
        availableObjects.add(new StaticObject("Gargoyle Pool", "A searchable stone pool"));

        availableObjects.add(new Weapon("Sword", "A sharp melee weapon", 5));
        availableObjects.add(new Weapon("Spell Book", "A magical weapon focus", 8));
        availableObjects.add(new Consumable("Health Potion", "Restores health", 20));
        availableObjects.add(new Wearable("Armor", "Adds defense", 3));
        availableObjects.add(new Key("Golden Key", "Opens the golden lock", "key_001"));

        availableObjects.add(new StaticContainer("Chest", "A fixed storage chest"));
        availableObjects.add(new PortableContainer("Pouch", "A small portable container", 4));

        availableCharacters.add(new Hero("Adventurer"));
        availableCharacters.add(new Knight());
        availableCharacters.add(new Sorcerer());
    }

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

        for (GameObject obj : availableObjects) {
            if (obj.getName().equals(name)) {
                return obj;
            }
        }
        return null;
    }

    public Character getCharacterByName(String name) {
        if (name == null) {
            return null;
        }

        for (Character character : availableCharacters) {
            if (character.getName().equals(name)) {
                return character;
            }
        }
        return null;
    }
}
