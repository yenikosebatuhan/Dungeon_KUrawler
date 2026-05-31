package com.kurawler.model.object;

/**
 * Readable item that can trigger a simple gameplay effect.
 */
public class Scroll extends Item {
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
}
