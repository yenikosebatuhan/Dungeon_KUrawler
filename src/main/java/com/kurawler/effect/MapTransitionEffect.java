package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.object.MapPortal;

/**
 * Moves the hero to another map when they step through a {@link MapPortal}.
 */
public class MapTransitionEffect implements Effect {

    private final MapPortal portal;

    public MapTransitionEffect(MapPortal portal) {
        this.portal = portal;
    }

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

    @Override
    public String describe() {
        return "Enter map " + portal.getTargetMapId() + " through " + portal.getName();
    }
}
