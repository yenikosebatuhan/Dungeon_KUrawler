package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Scroll;

/**
 * Effect: Consumes a scroll, applying its specific magical properties.
 */
public class ReadScrollEffect implements Effect {
    private final Scroll scroll;

    public ReadScrollEffect(Scroll scroll) {
        this.scroll = scroll;
    }

    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || scroll == null || session.getMap() == null) {
            return;
        }

        Scroll toConsume = resolveScrollInInventory(hero, scroll);
        if (toConsume == null || !hero.getInventory().removeItem(toConsume)) {
            return;
        }

        String name = toConsume.getName() == null ? "" : toConsume.getName().toLowerCase();
        int heal = toConsume.getHealAmount();
        int hpBefore = hero.getHealth();

        if (name.contains("shadow") || name.contains("clone")) {
            int[] spawn = findCloneSpawnCell(session.getMap(), hero.getRow(), hero.getCol());
            ShadowClone clone = new ShadowClone(spawn[0], spawn[1], session.getMap());
            session.setShadowClone(clone);
            session.setLastPortalMessage("Shadow Clone summoned!");
            return;
        }

        if (name.contains("spell book") || name.contains("spell")) {
            int manaGain = Math.max(15, heal);
            hero.heal(heal);
            hero.restoreMana(manaGain);
            int healed = hero.getHealth() - hpBefore;
            if (healed > 0) {
                session.setLastPortalMessage("Spell Book: +" + healed + " HP, +" + manaGain + " MP.");
            } else {
                session.setLastPortalMessage("Spell Book: restored " + manaGain + " MP.");
            }
            return;
        }

        if (heal > 0) {
            hero.heal(heal);
            int healed = hero.getHealth() - hpBefore;
            session.setLastPortalMessage("Read scroll: +" + healed + " HP.");
            return;
        }

        session.setLastPortalMessage("Read " + toConsume.getName() + " — the runes fade.");
    }

    /** Prefer an adjacent walkable tile so the clone is visible beside the hero. */
    private static int[] findCloneSpawnCell(Map map, int heroRow, int heroCol) {
        int[][] offsets = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
                {0, 0}
        };
        for (int[] off : offsets) {
            int r = heroRow + off[0];
            int c = heroCol + off[1];
            GridCell cell = map.getCell(r, c);
            if (cell != null && cell.isWalkable() && cell.getOccupyingCharacter() == null) {
                return new int[] { r, c };
            }
        }
        return new int[] { heroRow, heroCol };
    }

    private static Scroll resolveScrollInInventory(Hero hero, Scroll scroll) {
        if (hero.getInventory().hasItem(scroll)) {
            return scroll;
        }
        String targetName = scroll.getName();
        for (Item item : hero.getInventory().getItems()) {
            if (item instanceof Scroll && item.getName().equals(targetName)) {
                return (Scroll) item;
            }
        }
        return null;
    }

    @Override
    public String describe() {
        return "Read " + (scroll != null ? scroll.getName() : "nothing");
    }
}
