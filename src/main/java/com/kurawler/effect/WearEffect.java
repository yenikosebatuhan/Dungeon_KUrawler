package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Wearable;

/**
 * Effect: Equips a wearable item on the hero, applying its defense bonus.
 */
public class WearEffect implements Effect {
    private final Wearable wearable;

    /**
     * @requires wearable != null
     * @modifies this.wearable
     * @effects Stores the wearable to equip for use in apply().
     */
    public WearEffect(Wearable wearable) {
        this.wearable = wearable;
    }

    /**
     * @requires session != null && session.getHero() != null
     * @modifies hero.equippedWearable, hero.defensePower
     * @effects Equips wearable on the hero if it is in the hero's inventory,
     *          increasing defense power by the wearable's defense bonus.
     */
    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero != null && wearable != null && hero.getInventory().hasItem(wearable)) {
            hero.equipWearable(wearable);
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the wearable being equipped.
     */
    @Override
    public String describe() {
        return "Wear " + (wearable != null ? wearable.getName() : "nothing");
    }
}
