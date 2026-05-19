package com.kurawler.model.object;

/**
 * Scroll that, when READ from the hero's inventory, summons a Shadow Clone
 * (Phase II — Shadow Clone feature).
 *
 * <p>The Phase II brief specifies:
 * <em>"Every 15 seconds a scroll of Shadow Clone appears on a random
 * location of the map regardless of how many other scrolls are already
 * available on the map."</em></p>
 *
 * <p>This class is a marker subclass of {@link Scroll}; the actual Shadow
 * Clone spawn logic when the scroll is consumed lives in the action/effect
 * pipeline (READ + ReadScrollEffect or a dedicated SummonShadowCloneEffect).</p>
 */
public class ShadowCloneScroll extends Scroll {

    public ShadowCloneScroll() {
        super(
            "Shadow Clone Scroll",
            "A flickering parchment. Reading it conjures a shadow of yourself.",
            "The ink shifts as you read — a dark mirror peels away from you.",
            0   // no direct heal effect; clone summon handled separately
        );
    }
}
