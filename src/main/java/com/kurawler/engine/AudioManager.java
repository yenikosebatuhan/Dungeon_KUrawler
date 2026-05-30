package com.kurawler.engine;

/** Stub — ses sistemi başka branch'te geliştiriliyor. */
public class AudioManager {
    private static AudioManager instance;

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    public void play(String soundName) {}
}
