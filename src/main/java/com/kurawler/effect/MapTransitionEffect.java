package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.MapPortal;

/**
 * Moves the hero to another map when they step through a {@link MapPortal}.
 */
public class MapTransitionEffect implements Effect {

    private final MapPortal portal;

    /**
     * @requires portal != null
     * @modifies this.portal
     * @effects Stores the portal for use in apply().
     */
    public MapTransitionEffect(MapPortal portal) {
        this.portal = portal;
    }

    /**
     * @requires session != null && portal != null
     * @modifies session.currentMap, hero.position, hero.currentCell
     * @effects Transitions the hero to the target map and cell specified by the
     *          portal. Sets a portal message in session if the transition fails.
     */
    @Override
    public void apply(GameSession session) {
        boolean ok = session.transitionHeroTo(
                portal.getTargetMapId(), portal.getTargetRow(), portal.getTargetCol());
        if (ok) {
            System.out.println("[Effect] Entered " + portal.getTargetMapId()
                    + " via " + portal.getName());
        } else {
            session.setLastPortalMessage("Could not enter " + portal.getName() + ".");
        }
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns a string naming the target map and portal.
     */
    @Override
    public String describe() {
        return "Enter map " + portal.getTargetMapId() + " through " + portal.getName();
    }
}
