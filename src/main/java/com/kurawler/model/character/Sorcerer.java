package com.kurawler.model.character;

/**
 * Ranged enemy. Spec: 10HP, teleports, shoots projectiles dealing 8HP.
 */
public class Sorcerer extends Enemy {

    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 5, 60, 40);
        // HP=10, ATK=8 (projectile damage), DEF=0 (no armor),
        // STR=5, Mana=60, Energy=40
    }
}
