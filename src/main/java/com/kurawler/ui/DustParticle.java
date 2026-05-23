package com.kurawler.ui;

import java.util.Random;

class DustParticle {

    private static final Random RNG = new Random();

    double x, y;
    private final double vx, vy;
    private double alpha;
    private double radius;
    private final double radiusDecay;

    private static final int LIFETIME_FRAMES = 22;
    private int framesLeft = LIFETIME_FRAMES;

    DustParticle(double originX, double originY) {
        double angle  = RNG.nextDouble() * 2 * Math.PI;
        double speed  = 0.6 + RNG.nextDouble() * 1.4;
        vx     = Math.cos(angle) * speed;
        vy     = Math.sin(angle) * speed;
        x      = originX + vx * 3;
        y      = originY + vy * 3;
        alpha  = 0.55 + RNG.nextDouble() * 0.2;
        radius = 2.0 + RNG.nextDouble() * 2.5;
        radiusDecay = radius / LIFETIME_FRAMES;
    }

    void update() {
        x += vx;
        y += vy;
        alpha  = (double) framesLeft / LIFETIME_FRAMES * 0.75;
        radius = Math.max(0, radius - radiusDecay);
        framesLeft--;
    }

    boolean isExpired() {
        return framesLeft <= 0;
    }

    double getAlpha()  { return alpha; }
    double getRadius() { return radius; }
}
