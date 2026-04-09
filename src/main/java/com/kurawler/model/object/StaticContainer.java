package com.kurawler.model.object;

import java.util.ArrayList;
import java.util.List;

/**
 * A container fixed on the grid (e.g., chest, barrel).
 */
public class StaticContainer extends GameObject {

    private final List<Item> contents;

    public StaticContainer(String name, String description) {
        super(name, description);
        this.contents = new ArrayList<>();
    }

    public void addItem(Item item) {
        contents.add(item);
    }

    public Item removeItem(int index) {
        if (index < 0 || index >= contents.size()) {
            return null;
        }
        return contents.remove(index);
    }

    public List<Item> getContents() {
        return new ArrayList<>(contents);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }
}
