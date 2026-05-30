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
            heroSprite = characters.getSprite(3, 0); // full hero sprite (68% fill, h=48 w=45)
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

    public int getTileSize() {
        return tileSize;
    }

    /** Render hero facing direction (facingRow/Col currently unused — same sprite for all dirs). */
    public void drawHeroFacing(GraphicsContext gc, int screenX, int screenY, int facingRow, int facingCol) {
        drawHero(gc, screenX, screenY);
    }

    /** Render a shadow clone (translucent hero tint). */
    public void drawShadowClone(GraphicsContext gc, int screenX, int screenY) {
        gc.save();
        gc.setGlobalAlpha(0.45);
        gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
        drawHero(gc, screenX, screenY);
        gc.restore();
    }

    /** Render a searchable static object indicator. */
    public void drawSearchable(GraphicsContext gc, int screenX, int screenY, String name) {
        gc.setFill(Color.web("#4a3a18", 0.6));
        gc.fillRect(screenX + 4, screenY + 4, tileSize - 8, tileSize - 8);
        gc.setStroke(Color.web("#c8a840", 0.8));
        gc.setLineWidth(1.5);
        gc.strokeRect(screenX + 4, screenY + 4, tileSize - 8, tileSize - 8);
        gc.setFont(javafx.scene.text.Font.font("Serif", 10));
        gc.setFill(Color.web("#e8c84a"));
        gc.fillText("?", screenX + tileSize / 2.0 - 3, screenY + tileSize / 2.0 + 4);
    }

    /** Render an enemy. Type is "Knight" or "Sorcerer". */
    public void drawEnemy(GraphicsContext gc, int screenX, int screenY, String type) {
        Color c = "Sorcerer".equals(type) ? Color.web("#9b59b6") : Color.web("#e74c3c");
        gc.setFill(c.deriveColor(0, 1, 0.3, 1));
        gc.fillRoundRect(screenX + 6, screenY + 2, tileSize - 12, tileSize - 4, 6, 6);
        gc.setFill(c);
        gc.fillOval(screenX + 14, screenY + 4, tileSize - 28, tileSize - 28);
    }

    /** Render an HP bar above a tile. */
    public void drawHpBar(GraphicsContext gc, int screenX, int screenY, int hp, int maxHp) {
        if (maxHp <= 0) return;
        double ratio = Math.max(0, Math.min(1.0, (double) hp / maxHp));
        double barW = tileSize - 8;
        double barH = 5;
        double barX = screenX + 4;
        double barY = screenY - 7;
        gc.setFill(Color.web("#330000"));
        gc.fillRect(barX, barY, barW, barH);
        Color fill = ratio > 0.5 ? Color.web("#27ae60") : ratio > 0.25 ? Color.web("#f39c12") : Color.web("#e74c3c");
        gc.setFill(fill);
        gc.fillRect(barX, barY, barW * ratio, barH);
    }

    /** Render an enemy projectile. */
    public void drawProjectile(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#e74c3c"));
        gc.fillOval(screenX + tileSize / 2.0 - 4, screenY + tileSize / 2.0 - 4, 8, 8);
    }

    /** Render a hero projectile (bolt). */
    public void drawHeroProjectile(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#f1c40f"));
        gc.fillOval(screenX + tileSize / 2.0 - 5, screenY + tileSize / 2.0 - 5, 10, 10);
        gc.setStroke(Color.web("#e8c84a", 0.5));
        gc.setLineWidth(1);
        gc.strokeOval(screenX + tileSize / 2.0 - 7, screenY + tileSize / 2.0 - 7, 14, 14);
    }

    /** Render a weapon on the floor, choosing sprite by name. */
    public void drawWeaponByName(GraphicsContext gc, int screenX, int screenY, String name) {
        drawItem(gc, screenX, screenY, "Weapon");
    }

    /** Render a container on the floor, choosing sprite by name. */
    public void drawContainerByName(GraphicsContext gc, int screenX, int screenY, String name) {
        drawContainer(gc, screenX, screenY);
    }
}
