package com.kurawler.model.object;

import java.util.ArrayList;
import java.util.List;

public class PortableContainer extends Item {

    private final List<Item> contents;
    private final int capacity;

    public PortableContainer(String name, String description, int capacity) {
        super(name, description);
        this.capacity = capacity;
        this.contents = new ArrayList<>();
    }

    public boolean addItem(Item item) {
        if (contents.size() >= capacity) {
            return false;
        }
        contents.add(item);
        return true;
    }

    public List<Item> getContents() {
        return new ArrayList<>(contents);
    }

    public List<Item> getContentsMutable() {
        return contents;
    }

    public boolean removeItem(Item item) {
        return contents.remove(item);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public int getCapacity() {
        return capacity;
    }
}
