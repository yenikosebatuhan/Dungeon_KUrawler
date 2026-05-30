package com.kurawler.model.achievement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Stub — achievement sistemi başka branch'te geliştiriliyor. */
public class AchievementManager {
    private final List<Achievement> achievements = new ArrayList<>();
    private Consumer<Achievement> onUnlock;

    public List<Achievement> getAchievements() { return achievements; }

    public void setOnUnlock(Consumer<Achievement> callback) {
        this.onUnlock = callback;
    }

    public void unlock(String name) {
        for (Achievement a : achievements) {
            if (a.getName().equals(name) && !a.isUnlocked()) {
                a.unlock();
                if (onUnlock != null) onUnlock.accept(a);
            }
        }
    }
}
