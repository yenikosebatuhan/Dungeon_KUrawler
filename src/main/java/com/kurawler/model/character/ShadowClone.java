package com.kurawler.model.character;

/** Stub — shadow clone sistemi başka branch'te geliştiriliyor. */
public class ShadowClone extends Character {

    public ShadowClone(int row, int col) {
        super("Shadow Clone", 1, 0, 0, 0, 0, 0);
        setPosition(row, col);
    }

    @Override
    public int calculateDamage() { return 0; }
}
