package com.kurawler.editor;

import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides editor-ready object templates compatible with the current model.
 */
public class ObjectPalette {

    private final List<GameObject> availableObjects;

    public ObjectPalette() {
        this.availableObjects = new ArrayList<>();
        initializePalette();
    }

    private void initializePalette() {
        availableObjects.add(new StaticObject("Wall", "Blocks movement."));
        availableObjects.add(new StaticObject("Statue", "Decorative static object."));
        availableObjects.add(new StaticContainer("Chest", "A fixed container for loot."));
        availableObjects.add(new PortableContainer("Pouch", "A small portable container.", 4));
        availableObjects.add(new Weapon("Sword", "A simple melee weapon.", 5));
        availableObjects.add(new Weapon("Spell Book", "A magical tome used as a weapon.", 8));
        availableObjects.add(new Consumable("Health Potion", "Restores health.", 20));
        availableObjects.add(new Wearable("Armor", "Increases defense.", 3));
        availableObjects.add(new Key("Golden Key", "Opens a locked mechanism.", "key_001"));
    }

    public List<GameObject> getAvailableObjects() {
        return Collections.unmodifiableList(availableObjects);
    }

    public GameObject getObjectByName(String name) {
        for (GameObject obj : availableObjects) {
            if (obj.getName().equalsIgnoreCase(name)) {
                return copyOf(obj);
            }
        }
        return null;
    }

    private GameObject copyOf(GameObject obj) {
        if (obj instanceof StaticObject) {
            return new StaticObject(obj.getName(), obj.getDescription());
        }
        if (obj instanceof StaticContainer) {
            return new StaticContainer(obj.getName(), obj.getDescription());
        }
        if (obj instanceof PortableContainer) {
            PortableContainer container = (PortableContainer) obj;
            return new PortableContainer(obj.getName(), obj.getDescription(), container.getCapacity());
        }
        if (obj instanceof Weapon) {
            Weapon weapon = (Weapon) obj;
            return new Weapon(obj.getName(), obj.getDescription(), weapon.getDamage());
        }
        if (obj instanceof Consumable) {
            Consumable consumable = (Consumable) obj;
            return new Consumable(obj.getName(), obj.getDescription(), consumable.getHealAmount());
        }
        if (obj instanceof Wearable) {
            Wearable wearable = (Wearable) obj;
            return new Wearable(obj.getName(), obj.getDescription(), wearable.getDefenseBonus());
        }
        if (obj instanceof Key) {
            Key key = (Key) obj;
            return new Key(obj.getName(), obj.getDescription(), key.getKeyId());
        }
        return obj;
    }
}
