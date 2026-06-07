package com.kurawler.model.object;

import java.util.List;

/**
 * Readable item that can trigger a simple gameplay effect.
 */
public class Scroll extends Item {

    /** Mana spent each time the hero casts from a spell book (PDF: not consumable, mana per cast). */
    public static final int SPELL_BOOK_MANA_COST = 40;

    private final String text;
    private final int healAmount;
    private final int manaAmount;
    private final int energyAmount;

    public Scroll(String name, String description, String text, int healAmount) {
        this(name, description, text, healAmount, 0, 0);
    }

    public Scroll(String name, String description, String text,
                  int healAmount, int manaAmount, int energyAmount) {
        super(name, description);
        this.text = text;
        this.healAmount = healAmount;
        this.manaAmount = manaAmount;
        this.energyAmount = energyAmount;
    }

    public String getText() { return text; }
    public int getHealAmount() { return healAmount; }
    public int getManaAmount() { return manaAmount; }
    public int getEnergyAmount() { return energyAmount; }

    public boolean isSpellBook() {
        String n = getName() == null ? "" : getName().toLowerCase();
        return n.contains("spell book") || n.contains("spell");
    }

    @Override
    public List<ItemAction> getActions() {
        return List.of(ItemAction.TAKE, ItemAction.READ);
    }
}
