package com.kurawler.model.achievement;

/** Stub — achievement sistemi başka branch'te geliştiriliyor. */
public class Achievement {
    private final String name;
    private final String description;
    private boolean unlocked;

    public Achievement(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
    public void unlock() { this.unlocked = true; }
}
