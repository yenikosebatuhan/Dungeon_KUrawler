package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Scroll;

/**
 * Effect: Consumes a scroll, applying its specific magical properties.
 */
public class ReadScrollEffect implements Effect {
    private final Scroll scroll;

    /**
     * @requires scroll != null
     * @modifies this.scroll
     * @effects Stores the scroll to be consumed for use in apply().
     */
    public ReadScrollEffect(Scroll scroll) {
        this.scroll = scroll;
    }

    /**
     * Applies the scroll's effect to the game session.
     *
     * @requires session != null && session.getHero() != null && scroll != null
     * @modifies session.getHero().getInventory(), session.getHero() (health), 
     * session (shadowClone property), and hero's observers.
     * @effects Removes the scroll from the hero's inventory.
     * If the scroll provides healing, increases the hero's HP.
     * If the scroll is a "Shadow Clone" scroll, instantiates a new ShadowClone
     * at the hero's current position, assigns it to the session, and registers 
     * it as an observer to the hero.
     */
    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        
        // Validate and remove item from inventory
        if (hero == null || scroll == null || !hero.getInventory().removeItem(scroll)) {
            return;
        }
        
        // 1. Handle Healing Scrolls
        if (scroll.getHealAmount() > 0) {
            hero.heal(scroll.getHealAmount());
            System.out.println("[Effect] Read scroll. Healed hero by " + scroll.getHealAmount());
        }
        // 2. Handle Shadow Clone Scrolls
        if (scroll.getName().toLowerCase().contains("shadow") || scroll.getName().toLowerCase().contains("clone")) {
            
            // Create the clone at the hero's current row and col
            ShadowClone clone = new ShadowClone(hero.getRow(), hero.getCol(), session.getMap());
            
            // Save the clone to the active session
            session.setShadowClone(clone);
            
            System.out.println("[Effect] Shadow Clone spawned at (" + hero.getRow() + ", " + hero.getCol() + ")");
        }
    }

    @Override
    public String describe() {
        return "Read " + (scroll != null ? scroll.getName() : "nothing");
    }
}
