package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Scroll;

/**
 * Effect: Applies scroll magic. Most scrolls are consumed; spell books are reusable
 * and spend mana on each cast (PDF §2.4).
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

        Scroll target = resolveScrollInInventory(hero, scroll);
        if (target == null) {
            return;
        }

        String name = target.getName() == null ? "" : target.getName().toLowerCase();
        int hpBefore = hero.getHealth();

        if (name.contains("shadow") || name.contains("clone")) {
            if (!hero.getInventory().removeItem(target)) {
                return;
            }
            int[] spawn = findCloneSpawnCell(session.getMap(), hero.getRow(), hero.getCol());
            ShadowClone clone = new ShadowClone(spawn[0], spawn[1], session.getMap());
            session.setShadowClone(clone);
            session.setLastPortalMessage("Shadow Clone summoned!");
            return;
        }

        if (target.isSpellBook()) {
            int manaCost = Scroll.SPELL_BOOK_MANA_COST;
            if (hero.getMana() < manaCost) {
                session.setLastPortalMessage(
                        "Not enough mana to cast! (Need " + manaCost + " MP)");
                return;
            }
            hero.useMana(manaCost);
            int healAmount = Consumable.HEALTH_POTION_HEAL + 2;
            hero.heal(healAmount);
            int healed = hero.getHealth() - hpBefore;
            session.setLastPortalMessage("Spell Book: -" + manaCost + " MP, +" + healed + " HP.");
            return;
        }

        if (!hero.getInventory().removeItem(target)) {
            return;
        }

        int heal = target.getHealAmount();
        if (heal > 0) {
            hero.heal(heal);
            int healed = hero.getHealth() - hpBefore;
            session.setLastPortalMessage("Read scroll: +" + healed + " HP.");
            return;
        }

        session.setLastPortalMessage("Read " + target.getName() + " — the runes fade.");
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
