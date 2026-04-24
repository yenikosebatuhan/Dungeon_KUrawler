package com.kurawler.editor;

import com.kurawler.model.Consumable;
import com.kurawler.model.Item;
import com.kurawler.model.Map;
import com.kurawler.model.Weapon;
import com.kurawler.model.object.GameObject;
import java.util.Random;

/**
 * Level editor for placing/removing objects and generating random items.
 * Controller delegates editor operations here.
 */
public class LevelEditor {

    private Map map;
    private final Random random;

    public LevelEditor(Map map) {
        this.map = map;
        this.random = new Random();
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public boolean placeObject(int row, int col, GameObject obj) {
        return map.placeObject(row, col, obj);
    }

    public boolean removeObject(int row, int col, GameObject obj) {
        return map.removeObject(row, col, obj);
    }

    /** Generate a random item and place it at the given cell. */
    public Item generateRandomItem(int row, int col) {
        Item item;
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                item = new Weapon("Random Sword", 5 + random.nextInt(15), false);
                break;
            case 1:
                item = new Consumable("Random Potion", 10 + random.nextInt(30), Consumable.STAT_HP);
                break;
            default:
                item = new Weapon("Random Dagger", 3 + random.nextInt(10), false);
                break;
        }
        map.placeObject(row, col, item);
        System.out.println("[Editor] Placed " + item.getName() + " at (" + row + "," + col + ").");
        return item;
    }
}
