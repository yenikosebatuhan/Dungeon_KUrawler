package com.kurawler.model;

import com.kurawler.model.object.Item;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private static final int MAX_CAPACITY = 8;
    private List<Item> items;
    
    public Inventory() {
        this.items = new ArrayList<>(MAX_CAPACITY);
    }
    
    public boolean addItem(Item item) {
        if (items.size() >= MAX_CAPACITY) return false;
        items.add(item);
        return true;
    }
    
    public boolean hasItem(Item item) {
        return items.contains(item);
    }
    
    // The Hero class needs this method to consume potions!
    public boolean removeItem(Item item) {
        return items.remove(item);
    }
    
    public List<Item> getItems() {
        return items;
    }
}