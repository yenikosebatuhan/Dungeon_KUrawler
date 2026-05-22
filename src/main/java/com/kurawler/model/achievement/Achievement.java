package com.kurawler.model.achievement;

/**
 * Immutable achievement definition with mutable unlock state.
 */
public class Achievement {

    private final String name;
    private final String description;
    private boolean unlocked;

    public Achievement(String name, String description) {
        this.name = name;
        this.description = description;
        this.unlocked = false;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
}
