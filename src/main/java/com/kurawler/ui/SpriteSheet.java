package com.kurawler.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Utility for loading and extracting sprites from sprite sheet images.
 * UI-only class — no game logic.
 */
public class SpriteSheet {

    private final Image sheet;
    private final int tileSize;

    public SpriteSheet(String resourcePath, int tileSize) {
        this.tileSize = tileSize;
        Image loaded = null;
        try {
            loaded = new Image(getClass().getResourceAsStream(resourcePath));
        } catch (Exception e) {
            System.err.println("[SpriteSheet] Could not load: " + resourcePath);
        }
        this.sheet = loaded;
    }

    /** Extract a single sprite at grid position (col, row) from the sheet. */
    public Image getSprite(int col, int row) {
        if (sheet == null) return null;
        PixelReader reader = sheet.getPixelReader();
        int x = col * tileSize;
        int y = row * tileSize;
        if (x + tileSize > sheet.getWidth() || y + tileSize > sheet.getHeight()) {
            return null;
        }
        return new WritableImage(reader, x, y, tileSize, tileSize);
    }

    public boolean isLoaded() {
        return sheet != null;
    }

    public int getTileSize() {
        return tileSize;
    }
}
