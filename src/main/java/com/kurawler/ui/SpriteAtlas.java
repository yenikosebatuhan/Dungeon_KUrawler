package com.kurawler.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Slices a sprite sheet into a grid of tiles and detects which tiles actually
 * contain art (non-transparent). Used by the level editor to expose every
 * usable sprite in a sheet as a placeable asset. UI-only — no game logic.
 */
public class SpriteAtlas {

    private final Image sheet;
    private final int tile;
    private final int cols;
    private final int rows;
    private final Image[][] cache;
    private final boolean[][] nonEmpty;

    public SpriteAtlas(String resourcePath, int tileSize) {
        this.tile = tileSize;
        Image loaded = null;
        try {
            loaded = new Image(getClass().getResourceAsStream(resourcePath));
        } catch (Exception e) {
            System.err.println("[SpriteAtlas] Could not load: " + resourcePath);
        }
        if (loaded != null && loaded.isError()) {
            loaded = null;
        }
        this.sheet = loaded;
        this.cols = sheet != null ? (int) (sheet.getWidth() / tile) : 0;
        this.rows = sheet != null ? (int) (sheet.getHeight() / tile) : 0;
        this.cache = new Image[Math.max(0, rows)][Math.max(0, cols)];
        this.nonEmpty = new boolean[Math.max(0, rows)][Math.max(0, cols)];
        scan();
    }

    private void scan() {
        if (sheet == null) {
            return;
        }
        PixelReader reader = sheet.getPixelReader();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int opaque = 0;
                int x0 = c * tile;
                int y0 = r * tile;
                for (int y = 0; y < tile && opaque < 6; y += 2) {
                    for (int x = 0; x < tile && opaque < 6; x += 2) {
                        if (reader.getColor(x0 + x, y0 + y).getOpacity() > 0.18) {
                            opaque++;
                        }
                    }
                }
                nonEmpty[r][c] = opaque >= 6;
            }
        }
    }

    public boolean isLoaded() {
        return sheet != null;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public boolean isNonEmpty(int col, int row) {
        return row >= 0 && row < rows && col >= 0 && col < cols && nonEmpty[row][col];
    }

    public Image getTile(int col, int row) {
        if (sheet == null || row < 0 || row >= rows || col < 0 || col >= cols) {
            return null;
        }
        if (cache[row][col] == null) {
            PixelReader reader = sheet.getPixelReader();
            cache[row][col] = new WritableImage(reader, col * tile, row * tile, tile, tile);
        }
        return cache[row][col];
    }

    /** Grid cell with transparent margins removed so neighbours do not bleed in. */
    public Image getTileTrimmed(int col, int row) {
        return trimOpaqueBounds(getTile(col, row));
    }

    /**
     * Crops to the tight bounding box of opaque pixels (nearest-neighbour safe).
     * Returns {@code src} unchanged when empty or already trimmed.
     */
    /**
     * Inclusive opaque bounds in image pixels, or {@code null} if empty.
     * Returns {@code {minX, minY, maxX, maxY}}.
     */
    public static int[] opaqueBounds(Image src) {
        if (src == null) {
            return null;
        }
        PixelReader pr = src.getPixelReader();
        if (pr == null) {
            return null;
        }
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (pr.getColor(x, y).getOpacity() > 0.18) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX) {
            return null;
        }
        return new int[] { minX, minY, maxX, maxY };
    }

    public static Image trimOpaqueBounds(Image src) {
        if (src == null) {
            return null;
        }
        PixelReader pr = src.getPixelReader();
        if (pr == null) {
            return src;
        }
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        if (w <= 0 || h <= 0) {
            return src;
        }
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (pr.getColor(x, y).getOpacity() > 0.18) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX) {
            return src;
        }
        return new WritableImage(pr, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
