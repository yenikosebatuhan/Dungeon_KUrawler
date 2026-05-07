package com.kurawler.model.observer;

/**
 * Subject role in the hero-centered Observer design.
 */
public interface HeroSubject {
    void addObserver(HeroObserver observer);
    void removeObserver(HeroObserver observer);
}
