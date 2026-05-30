package com.kurawler.engine;

import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Small sound facade for optional wav effects.
 * Missing files fall back to the platform beep so the game remains portable.
 */
public class AudioManager {
    private static final String AUDIO_DIR = "src/main/resources/assets/audio";
    private static final AudioManager INSTANCE = new AudioManager();

    private final Map<String, String> sounds = new HashMap<>();
    private boolean enabled = true;

    private AudioManager() {
        sounds.put("button",   "button.wav");
        sounds.put("attack",   "attack.wav");
        sounds.put("item",     "item.wav");
        sounds.put("enemyDown","enemy_down.wav");
        sounds.put("gameOver", "game_over.wav");
        sounds.put("victory",  "victory.wav");
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns the singleton AudioManager instance.
     */
    public static AudioManager getInstance() {
        return INSTANCE;
    }

    /**
     * @requires true
     * @modifies this.enabled
     * @effects Enables or disables audio playback globally.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @requires none
     * @modifies none
     * @effects Returns true if audio is currently enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @requires soundKey != null
     * @modifies none
     * @effects If enabled and the soundKey maps to a known wav file that exists on disk,
     *          plays that clip asynchronously. Falls back to the system beep if the file
     *          is missing or the key is unrecognised.
     */
    public void play(String soundKey) {
        if (!enabled) return;
        String fileName = sounds.get(soundKey);
        if (fileName == null || !playFile(new File(AUDIO_DIR, fileName))) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private boolean playFile(File file) {
        if (!file.exists()) return false;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
