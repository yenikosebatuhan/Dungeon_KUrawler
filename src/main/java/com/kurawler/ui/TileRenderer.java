package com.kurawler.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Responsible for loading all game sprites and drawing tiles onto a Canvas.
 * Uses x3 sprite sheets (48px native) for crisp rendering.
 * UI-only class — no game logic.
 */
public class TileRenderer {

    private static final int SPRITE_SIZE = 48; // x3 sheets have 48px tiles

    private final int tileSize; // rendered tile size on screen

    // Sprite sheets (x3 variants for crisp 48px tiles)
    private final SpriteSheet characters;
    private final SpriteSheet walls;
    private final SpriteSheet items;
    private final SpriteSheet weapons;
    private final SpriteSheet containers;

    // Cached sprites
    private Image wallSprite;
    private Image floorSprite;
    private Image heroSprite;
    private Image knightSprite;
    private Image sorcererSprite;
    private Image swordSprite;
    private Image potionSprite;
    private Image keySprite;
    private Image chestSprite;

    public TileRenderer(int tileSize) {
        this.tileSize = tileSize;

        characters = new SpriteSheet("/assets/characters_x3.png", SPRITE_SIZE);
        walls = new SpriteSheet("/assets/walls_x3.png", SPRITE_SIZE);
        items = new SpriteSheet("/assets/items_x3.png", SPRITE_SIZE);
        weapons = new SpriteSheet("/assets/weapons_x3.png", SPRITE_SIZE);
        containers = new SpriteSheet("/assets/containers_x3.png", SPRITE_SIZE);

        loadSprites();
    }

    private void loadSprites() {
        // walls_x3: 30 cols x 8 rows
        // Row 1 = 100% filled wall blocks (col 0-27 all solid)
        // Row 0 = top decorations only (25% fill — NOT walls)
        // Row 2+ = mixed floor/wall variants
        if (walls.isLoaded()) {
            wallSprite = walls.getSprite(0, 1);    // solid brick wall (row 1, 100% fill)
            floorSprite = walls.getSprite(0, 3);   // solid floor tile (row 3, 100% fill)
        }
        // characters_x3: 9 cols x 8 rows
        // Cols 0-2 row 0 = tiny feet only (12% fill — NOT full character!)
        // Cols 3-6 row 0 = full-size character sprites (68-71% fill, 48x48)
        // Col 3 = front-facing hero
        if (characters.isLoaded()) {
            heroSprite = characters.getSprite(3, 0);
            knightSprite = characters.getSprite(0, 1);
            sorcererSprite = characters.getSprite(0, 3);
        }
        // weapons_x3: 10 cols x 10 rows
        if (weapons.isLoaded()) {
            swordSprite = weapons.getSprite(0, 0);
        }
        // items_x3: 6 cols x 4 rows
        if (items.isLoaded()) {
            potionSprite = items.getSprite(5, 0);  // largest potion (30% fill)
            keySprite = items.getSprite(0, 3);     // key item (12% fill)
        }
        // containers_x3: 4 cols x 6 rows
        if (containers.isLoaded()) {
            chestSprite = containers.getSprite(0, 0); // chest (82% fill)
        }
    }

    /** Draw a floor tile. Base color + sprite overlay. */
    public void drawFloor(GraphicsContext gc, int screenX, int screenY) {
        // Always draw a base color first (sprite may have transparent pixels)
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        if (floorSprite != null) {
            gc.drawImage(floorSprite, screenX, screenY, tileSize, tileSize);
        }
    }

    /** Draw a wall tile. Base color + sprite overlay. */
    public void drawWall(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        if (wallSprite != null) {
            gc.drawImage(wallSprite, screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#6b5b4f"));
            gc.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }

    /** Draw the hero sprite on top of floor. */
    public void drawHero(GraphicsContext gc, int screenX, int screenY) {
        if (heroSprite != null) {
            gc.drawImage(heroSprite, screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#4caf50"));
            gc.fillRoundRect(screenX + 8, screenY + 4, tileSize - 16, tileSize - 8, 8, 8);
        }
    }

    /** Draw an ethereal clone using the hero sprite with a blue tint. */
    public void drawShadowClone(GraphicsContext gc, int screenX, int screenY) {
        double oldAlpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(0.45);
        gc.setFill(Color.web("#2638ff", 0.35));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        if (heroSprite != null) {
            gc.drawImage(heroSprite, screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#4f66ff"));
            gc.fillRoundRect(screenX + 8, screenY + 4, tileSize - 16, tileSize - 8, 8, 8);
        }
        gc.setGlobalAlpha(oldAlpha);
    }

    /** Draw an item sprite. Type determines which sprite to use. */
    public void drawItem(GraphicsContext gc, int screenX, int screenY, String itemType) {
        Image sprite;
        Color fallback;
        switch (itemType) {
            case "Weapon":
                sprite = swordSprite;
                fallback = Color.web("#ff9800");
                break;
            case "Consumable":
                sprite = potionSprite;
                fallback = Color.web("#e91e63");
                break;
            case "Key":
                sprite = keySprite;
                fallback = Color.web("#ffeb3b");
                break;
            default:
                sprite = potionSprite;
                fallback = Color.web("#9c27b0");
                break;
        }
        if (sprite != null) {
            gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
        } else {
            // Diamond-shaped fallback
            gc.setFill(fallback);
            double cx = screenX + tileSize / 2.0;
            double cy = screenY + tileSize / 2.0;
            double s = tileSize * 0.3;
            gc.fillPolygon(
                new double[]{cx, cx + s, cx, cx - s},
                new double[]{cy - s, cy, cy + s, cy},
                4
            );
        }
    }

    /** Draw a container sprite. */
    public void drawContainer(GraphicsContext gc, int screenX, int screenY) {
        if (chestSprite != null) {
            gc.drawImage(chestSprite, screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#795548"));
            gc.fillRect(screenX + 8, screenY + 12, tileSize - 16, tileSize - 18);
        }
    }

    /** Draw an enemy using sprite sheet. Colored fallback if sprites unavailable. */
    public void drawEnemy(GraphicsContext gc, int screenX, int screenY, String enemyType) {
        Image sprite = "Knight".equals(enemyType) ? knightSprite : sorcererSprite;

        if (sprite != null) {
            gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
        } else {
            double inset = tileSize * 0.15;
            double size = tileSize - 2 * inset;
            if ("Knight".equals(enemyType)) {
                gc.setFill(Color.web("#d32f2f"));
                gc.fillRect(screenX + inset, screenY + inset, size, size);
            } else {
                gc.setFill(Color.web("#7b1fa2"));
                gc.fillOval(screenX + inset, screenY + inset, size, size);
            }
        }
    }

    public int getTileSize() {
        return tileSize;
    }
}
