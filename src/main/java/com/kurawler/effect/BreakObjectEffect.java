package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.MapFactory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Effect: Marks a breakable container open or removes a crate/barrel floor prop.
 */
public class BreakObjectEffect implements Effect {
    public static final int BREAK_ENERGY_COST = 5;

    private final int row;
    private final int col;

    /** Spends break attempt energy (success or failure). */
    public static boolean spendAttemptEnergy(Hero hero) {
        return hero != null && hero.useEnergy(BREAK_ENERGY_COST);
    }

    public BreakObjectEffect(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void apply(GameSession session) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null) {
            return;
        }

        for (GameObject obj : new ArrayList<>(cell.getObjects())) {
            if (!(obj instanceof StaticContainer sc) || !sc.isBreakable()) {
                continue;
            }
            // Smash the chest open: scatter its loot onto nearby tiles, then remove it.
            scatterContents(session.getMap(), cell, sc.getContents());
            cell.removeObject(sc);
            return;
        }

        StaticObject object = cell.getStaticObject();
        if (!MapFactory.isBreakableStaticObject(object)) {
            return;
        }
        Item hidden = object.revealHiddenItem();
        if (hidden != null) {
            cell.addObject(hidden);
        }
        cell.setStaticObject(null);
    }

    /**
     * Scatters a broken chest's loot onto walkable tiles within 1–2 cells, one item per
     * tile (nearest tiles first). Any overflow is dropped on the chest's own cell.
     */
    private static void scatterContents(Map map, GridCell origin, List<Item> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        List<GridCell> spots = new ArrayList<>();
        if (map != null) {
            for (int radius = 1; radius <= 2 && spots.size() < contents.size(); radius++) {
                List<GridCell> ring = new ArrayList<>();
                for (int dr = -radius; dr <= radius; dr++) {
                    for (int dc = -radius; dc <= radius; dc++) {
                        if (Math.max(Math.abs(dr), Math.abs(dc)) != radius) {
                            continue; // only the outer ring at this radius
                        }
                        GridCell c = map.getCell(origin.getRow() + dr, origin.getCol() + dc);
                        if (c != null && c != origin && c.isWalkable() && !spots.contains(c)) {
                            ring.add(c);
                        }
                    }
                }
                Collections.shuffle(ring);
                spots.addAll(ring);
            }
        }
        for (int i = 0; i < contents.size(); i++) {
            GridCell target = i < spots.size() ? spots.get(i) : origin;
            target.addObject(contents.get(i));
        }
    }

    @Override
    public String describe() {
        return "Break object at (" + row + "," + col + ") and scatter its contents.";
    }
}
