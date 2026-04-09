package com.kurawler.model.character;

/**
 * Melee enemy. Spec: 20HP, chases hero within 5 cells, melee attack.
 */
public class Knight extends Enemy {

    public Knight() {
        super("Knight", 20, 4, 1, 8, 0, 80);
        // HP=20, ATK=4 (spec: deals 4HP), DEF=1 (armor absorbs 1),
        // STR=8, Mana=0, Energy=80
    }
}
