package com.kurawler.model.object;

/**
 * Readable item that can trigger a simple gameplay effect.
 */
public class Scroll extends Item {
    private final String text;
    private final int healAmount;

    public Scroll(String name, String description, String text, int healAmount) {
        super(name, description);
        this.text = text;
        this.healAmount = healAmount;
    }

    public String getText() {
        return text;
    }

    public int getHealAmount() {
        return healAmount;
    }
}
