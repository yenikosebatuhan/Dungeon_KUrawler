package com.kurawler.model.object;

/**
 * A magical ring that the hero can wear (WEAR action) for a DEF bonus.
 * Rings are commonly dropped by defeated Sorcerers (PDF §2.5.2:
 * "He may or may not have a magical ring which adds to his DEF stat ...
 *  Drop item upon defeat (like the ring)").
 */
public class Ring extends Wearable {

    public Ring(String name, String description, int defenseBonus) {
        super(name, description, defenseBonus);
    }

    /** Convenience factory for a typical sorcerer drop. */
    public static Ring sorcererDrop() {
        return new Ring(
            "Sorcerer's Ring",
            "A faintly glowing band of dark silver. +1 DEF when worn.",
            1
        );
    }
}
