package com.kurawler.editor;

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
import java.util.ArrayList;
import java.util.List;

public class ObjectPalette {

    private final List<GameObject> availableObjects;
    private final List<Character> availableCharacters;

    public ObjectPalette() {
        this.availableObjects = new ArrayList<>();
        this.availableCharacters = new ArrayList<>();
        initializePalette();
    }

    private void initializePalette() {
        availableObjects.add(createObjectByName("Wall"));
        availableObjects.add(createObjectByName("Gargoyle Pool"));
        availableObjects.add(createObjectByName("Sword"));
        availableObjects.add(createObjectByName("Spell Book"));
        availableObjects.add(createObjectByName("Health Potion"));
        availableObjects.add(createObjectByName("Armor"));
        availableObjects.add(createObjectByName("Golden Key"));
        availableObjects.add(createObjectByName("Chest"));
        availableObjects.add(createObjectByName("Pouch"));

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
            if (obj.getName().equalsIgnoreCase(name)) {
                return createObjectByName(obj.getName());
            }
        }
        return null;
    }

    public Character getCharacterByName(String name) {
        if (name == null) {
            return null;
        }

        for (Character character : availableCharacters) {
            if (character.getName().equalsIgnoreCase(name)) {
                return createCharacterByName(character.getName());
            }
        }
        return null;
    }

    private GameObject createObjectByName(String name) {
        switch (name) {
            case "Wall":
                return new StaticObject("Wall", "A solid wall that blocks movement", false, false);
            case "Gargoyle Pool":
                return new StaticObject("Gargoyle Pool", "A searchable stone pool", true, true);
            case "Sword":
                return new Weapon("Sword", "A sharp blade", 5);
            case "Spell Book":
                return new Weapon("Spell Book", "A tome of arcane power", 8);
            case "Health Potion":
                return new Consumable("Health Potion", "Restores 20 HP", 20);
            case "Armor":
                return new Wearable("Armor", "Protective armor", 3);
            case "Golden Key":
                return new Key("Golden Key", "Opens a golden lock", "key_001");
            case "Chest":
                return new StaticContainer("Chest", "A wooden chest");
            case "Pouch":
                return new PortableContainer("Pouch", "A small pouch", 4);
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
