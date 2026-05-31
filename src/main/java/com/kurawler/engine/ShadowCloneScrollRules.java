package com.kurawler.engine;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;

import com.kurawler.model.GameSession;

import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.ShadowCloneScroll;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase II rules for {@link ShadowCloneScroll} on the map: 7 s visibility,
 * removal when an enemy strikes the tile or steps on it.
 */
public final class ShadowCloneScrollRules {

    public static final long SPAWN_INTERVAL_MS = 15_000L;
    public static final long VISIBILITY_MS = 7_000L;

    private ShadowCloneScrollRules() {}

    /** Story campaign maps (Entry Hall, Catacombs, Treasure Vault). */
    public static boolean appliesToSession(GameSession session) {
        if (session == null || session instanceof com.kurawler.model.TeamMatchSession) {
            return false;
        }
        String mapId = session.getCurrentMapId();
        return "hall".equals(mapId) || "catacombs".equals(mapId) || "vault".equals(mapId);
    }


    public static void removeFromCell(GridCell cell) {
        if (cell == null) {
            return;
        }
        List<GameObject> toRemove = new ArrayList<>();
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof ShadowCloneScroll) {
                toRemove.add(obj);
            }
        }
        for (GameObject obj : toRemove) {
            cell.removeObject(obj);
        }
    }

    public static void removeFromCell(Map map, int row, int col) {
        if (map == null) {
            return;
        }
        removeFromCell(map.getCell(row, col));
    }

    /** Removes scrolls whose 7-second map visibility has elapsed. */
    public static void removeExpiredFromMap(Map map) {
        if (map == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int y = 0; y < map.getGridHeight(); y++) {
            for (int x = 0; x < map.getGridWidth(); x++) {
                GridCell cell = map.getCellAt(x, y);
                if (cell == null) {
                    continue;
                }
                List<GameObject> toRemove = new ArrayList<>();
                for (GameObject obj : cell.getObjects()) {
                    if (obj instanceof ShadowCloneScroll) {
                        ShadowCloneScroll scroll = (ShadowCloneScroll) obj;
                        if (now - scroll.getSpawnedAtMs() >= VISIBILITY_MS) {
                            toRemove.add(scroll);
                        }
                    }
                }
                for (GameObject obj : toRemove) {
                    cell.removeObject(obj);
                }
            }
        }
    }
}
