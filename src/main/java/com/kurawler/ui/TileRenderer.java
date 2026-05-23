package com.kurawler.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class TileRenderer {

    private static final int SPRITE_SIZE = 48;

    private final int tileSize;

    private final SpriteSheet characters;
    private final SpriteSheet walls;
    private final SpriteSheet statics;
    private final SpriteSheet items;
    private final SpriteSheet weapons;
    private final SpriteSheet containers;

    // Walls & floors — multiple variants for visual variety
    private Image[] wallSprites;
    private Image[] floorSprites;

    // Characters
    private Image heroSprite;
    private Image heroSpriteUp;
    private Image heroSpriteLeft;
    private Image heroSpriteRight;
    private Image knightSprite;
    private Image knightSprite2;
    private Image sorcererSprite;
    private Image sorcererSprite2;

    // Items
    private Image potionSprite;
    private Image potionSprite2;
    private Image keySprite;
    private Image scrollSprite;
    private Image gemSprite;
    private Image ringSprite;

    // Weapons
    private Image swordSprite;
    private Image axeSprite;
    private Image bowSprite;
    private Image staffSprite;
    private Image daggerSprite;
    private Image hammerSprite;

    // Wearable
    private Image armorSprite;
    private Image shieldSprite;

    // Containers
    private Image chestSprite;
    private Image chestOpenSprite;
    private Image barrelSprite;
    private Image crateSprite;

    // Statics (searchable objects, decorations)
    private Image poolSprite;
    private Image pedestalSprite;
    private Image altarSprite;
    private Image columnSprite;

    public TileRenderer(int tileSize) {
        this.tileSize = tileSize;

        characters = new SpriteSheet("/assets/characters_x3.png", SPRITE_SIZE);
        walls = new SpriteSheet("/assets/walls_x3.png", SPRITE_SIZE);
        statics = new SpriteSheet("/assets/walls and statics x3.png", SPRITE_SIZE);
        items = new SpriteSheet("/assets/items_x3.png", SPRITE_SIZE);
        weapons = new SpriteSheet("/assets/weapons_x3.png", SPRITE_SIZE);
        containers = new SpriteSheet("/assets/containers_x3.png", SPRITE_SIZE);

        loadSprites();
    }

    private void loadSprites() {
        loadWallsAndFloors();
        loadCharacters();
        loadItems();
        loadWeapons();
        loadContainers();
        loadStatics();
    }

    private void loadWallsAndFloors() {
        if (!walls.isLoaded()) return;
        wallSprites = new Image[]{
                walls.getSprite(0, 1),
                walls.getSprite(1, 1),
                walls.getSprite(2, 1),
                walls.getSprite(3, 1),
                walls.getSprite(4, 1),
        };
        floorSprites = new Image[]{
                walls.getSprite(0, 3),
                walls.getSprite(1, 3),
                walls.getSprite(2, 3),
                walls.getSprite(3, 3),
        };
        removeNulls(wallSprites);
        removeNulls(floorSprites);
    }

    private void loadCharacters() {
        if (!characters.isLoaded()) return;
        heroSprite = characters.getSprite(3, 0);
        heroSpriteUp = characters.getSprite(5, 0);
        heroSpriteLeft = characters.getSprite(4, 0);
        heroSpriteRight = characters.getSprite(3, 0);

        knightSprite = characters.getSprite(6, 1);
        knightSprite2 = characters.getSprite(7, 1);
        sorcererSprite = characters.getSprite(3, 1);
        sorcererSprite2 = characters.getSprite(4, 1);

        if (heroSpriteUp == null) heroSpriteUp = heroSprite;
        if (heroSpriteLeft == null) heroSpriteLeft = heroSprite;
        if (heroSpriteRight == null) heroSpriteRight = heroSprite;
        if (knightSprite2 == null) knightSprite2 = knightSprite;
        if (sorcererSprite2 == null) sorcererSprite2 = sorcererSprite;
    }

    private void loadItems() {
        if (!items.isLoaded()) return;
        // items_x3.png is a 6-col x 4-row sheet. getSprite(col, row).
        // Row 0 = keys, Row 1 = potions/rings, Row 2 = skulls/gems/coins, Row 3 = orb/book/scrolls
        potionSprite = items.getSprite(0, 1);   // red potion
        potionSprite2 = items.getSprite(1, 1);  // blue potion
        keySprite = items.getSprite(2, 0);      // gold key
        scrollSprite = items.getSprite(2, 3);   // parchment scroll (psi rune)
        gemSprite = items.getSprite(2, 2);      // gem
        ringSprite = items.getSprite(4, 1);     // green ring
        if (scrollSprite == null) scrollSprite = potionSprite;
        if (gemSprite == null) gemSprite = potionSprite;
    }

    private void loadWeapons() {
        if (!weapons.isLoaded()) return;
        swordSprite = weapons.getSprite(0, 0);
        axeSprite = weapons.getSprite(1, 0);
        bowSprite = weapons.getSprite(2, 0);
        staffSprite = weapons.getSprite(3, 0);
        daggerSprite = weapons.getSprite(4, 0);
        hammerSprite = weapons.getSprite(5, 0);
        armorSprite = weapons.getSprite(4, 2);
        shieldSprite = weapons.getSprite(0, 8);
        if (axeSprite == null) axeSprite = swordSprite;
        if (bowSprite == null) bowSprite = swordSprite;
        if (staffSprite == null) staffSprite = swordSprite;
        if (daggerSprite == null) daggerSprite = swordSprite;
        if (hammerSprite == null) hammerSprite = swordSprite;
        if (armorSprite == null) armorSprite = shieldSprite;
    }

    private void loadContainers() {
        if (!containers.isLoaded()) return;
        chestSprite = containers.getSprite(0, 0);
        chestOpenSprite = containers.getSprite(0, 1);
        barrelSprite = containers.getSprite(0, 2);
        crateSprite = containers.getSprite(0, 3);
        if (chestOpenSprite == null) chestOpenSprite = chestSprite;
        if (barrelSprite == null) barrelSprite = chestSprite;
        if (crateSprite == null) crateSprite = chestSprite;
    }

    private void loadStatics() {
        if (!statics.isLoaded()) return;
        poolSprite = statics.getSprite(8, 5);
        pedestalSprite = statics.getSprite(10, 5);
        altarSprite = statics.getSprite(12, 5);
        columnSprite = statics.getSprite(14, 5);
        if (poolSprite == null) poolSprite = statics.getSprite(6, 5);
        if (pedestalSprite == null) pedestalSprite = statics.getSprite(7, 5);
    }

    private void removeNulls(Image[] arr) {
        Image first = null;
        for (Image img : arr) {
            if (img != null) { first = img; break; }
        }
        if (first != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) arr[i] = first;
            }
        }
    }

    // ---------- FLOOR + WALL ----------

    public void drawFloor(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        if (floorSprites != null && floorSprites.length > 0) {
            int variant = tileHash(screenX, screenY) % floorSprites.length;
            gc.drawImage(floorSprites[variant], screenX, screenY, tileSize, tileSize);
        }
    }

    public void drawWall(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        if (wallSprites != null && wallSprites.length > 0) {
            int variant = tileHash(screenX, screenY) % wallSprites.length;
            gc.drawImage(wallSprites[variant], screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#6b5b4f"));
            gc.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }

    // ---------- CHARACTERS ----------

    public void drawHero(GraphicsContext gc, int screenX, int screenY) {
        drawSprite(gc, heroSprite, screenX, screenY, Color.web("#4caf50"));
    }

    public void drawHeroFacing(GraphicsContext gc, int screenX, int screenY, int facingR, int facingC) {
        Image sprite;
        if (facingR < 0) sprite = heroSpriteUp;
        else if (facingR > 0) sprite = heroSprite;
        else if (facingC < 0) sprite = heroSpriteLeft;
        else sprite = heroSpriteRight;
        if (sprite == null) sprite = heroSprite;
        drawSprite(gc, sprite, screenX, screenY, Color.web("#4caf50"));
    }

    public void drawShadowClone(GraphicsContext gc, int screenX, int screenY) {
        double oldAlpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(0.45);
        gc.setFill(Color.web("#2638ff", 0.35));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        drawSprite(gc, heroSprite, screenX, screenY, Color.web("#4f66ff"));
        gc.setGlobalAlpha(oldAlpha);
    }

    public void drawEnemy(GraphicsContext gc, int screenX, int screenY, String enemyType) {
        if ("Knight".equals(enemyType)) {
            Image sprite = ((screenX + screenY) % 2 == 0) ? knightSprite : knightSprite2;
            drawSprite(gc, sprite, screenX, screenY, Color.web("#d32f2f"));
        } else {
            Image sprite = ((screenX + screenY) % 2 == 0) ? sorcererSprite : sorcererSprite2;
            drawSprite(gc, sprite, screenX, screenY, Color.web("#7b1fa2"));
        }
    }

    // ---------- ITEMS ----------

    public void drawItem(GraphicsContext gc, int screenX, int screenY, String itemType) {
        Image sprite;
        Color fallback;
        switch (itemType) {
            case "Weapon":   sprite = swordSprite;  fallback = Color.web("#ff9800"); break;
            case "Consumable": sprite = potionSprite; fallback = Color.web("#e91e63"); break;
            case "Key":      sprite = keySprite;    fallback = Color.web("#ffeb3b"); break;
            case "Scroll":   sprite = scrollSprite; fallback = Color.web("#64b5f6"); break;
            case "Wearable": sprite = armorSprite;  fallback = Color.web("#78909c"); break;
            case "Gem":      sprite = gemSprite;    fallback = Color.web("#ab47bc"); break;
            default:         sprite = gemSprite;    fallback = Color.web("#9c27b0"); break;
        }
        drawSprite(gc, sprite, screenX, screenY, fallback);
    }

    public void drawConsumable(GraphicsContext gc, int screenX, int screenY, String potionType) {
        Image sprite;
        Color glowColor;
        switch (potionType) {
            case "ManaPotion":
                sprite = potionSprite2;
                glowColor = Color.web("#e040fb");
                break;
            case "EnergyPotion":
                sprite = ringSprite;
                glowColor = Color.web("#66bb6a");
                break;
            default: // HealthPotion
                sprite = potionSprite;
                glowColor = Color.web("#ef5350");
                break;
        }
        gc.setFill(Color.color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 0.30));
        gc.fillOval(screenX + 6, screenY + 6, tileSize - 12, tileSize - 12);
        drawSprite(gc, sprite, screenX, screenY, glowColor);
    }

    public void drawWeaponByName(GraphicsContext gc, int screenX, int screenY, String weaponName) {
        Image sprite = swordSprite;
        if (weaponName != null) {
            String lower = weaponName.toLowerCase();
            if (lower.contains("axe")) sprite = axeSprite;
            else if (lower.contains("bow")) sprite = bowSprite;
            else if (lower.contains("staff") || lower.contains("spell")) sprite = staffSprite;
            else if (lower.contains("dagger") || lower.contains("knife")) sprite = daggerSprite;
            else if (lower.contains("hammer") || lower.contains("mace")) sprite = hammerSprite;
        }
        drawSprite(gc, sprite, screenX, screenY, Color.web("#ff9800"));
    }

    // ---------- CONTAINERS ----------

    public void drawContainer(GraphicsContext gc, int screenX, int screenY) {
        drawSprite(gc, chestSprite, screenX, screenY, Color.web("#795548"));
    }

    public void drawContainerByName(GraphicsContext gc, int screenX, int screenY, String name) {
        Image sprite = chestSprite;
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("barrel")) sprite = barrelSprite;
            else if (lower.contains("crate")) sprite = crateSprite;
            else if (lower.contains("open")) sprite = chestOpenSprite;
        }
        drawSprite(gc, sprite, screenX, screenY, Color.web("#795548"));
    }

    // ---------- STATIC OBJECTS (searchable) ----------

    public void drawSearchable(GraphicsContext gc, int screenX, int screenY, String name) {
        Image sprite = pedestalSprite;
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("pool") || lower.contains("gargoyle")) sprite = poolSprite;
            else if (lower.contains("altar")) sprite = altarSprite;
            else if (lower.contains("column") || lower.contains("pillar")) sprite = columnSprite;
        }
        drawSprite(gc, sprite, screenX, screenY, Color.web("#887744"));
        drawSearchableMarker(gc, screenX, screenY);
    }

    // ---------- VISUAL EFFECTS ----------

    public void drawHpBar(GraphicsContext gc, int screenX, int screenY, int hp, int maxHp) {
        if (maxHp <= 0) return;
        double barW = tileSize - 8;
        double barH = 4;
        double x = screenX + 4;
        double y = screenY - 2;
        double ratio = Math.max(0, Math.min(1.0, (double) hp / maxHp));

        gc.setFill(Color.web("#333333", 0.7));
        gc.fillRect(x, y, barW, barH);
        Color barColor = ratio > 0.5 ? Color.web("#4caf50")
                : ratio > 0.25 ? Color.web("#ff9800") : Color.web("#f44336");
        gc.setFill(barColor);
        gc.fillRect(x, y, barW * ratio, barH);
        gc.setStroke(Color.web("#000000", 0.4));
        gc.setLineWidth(0.5);
        gc.strokeRect(x, y, barW, barH);
    }

    public void drawSearchableMarker(GraphicsContext gc, int screenX, int screenY) {
        gc.setStroke(Color.web("#ffd54f", 0.5));
        gc.setLineWidth(1);
        gc.setLineDashes(4, 3);
        gc.strokeRect(screenX + 3, screenY + 3, tileSize - 6, tileSize - 6);
        gc.setLineDashes(null);
        gc.setFill(Color.web("#ffd54f", 0.7));
        gc.fillOval(screenX + tileSize - 10, screenY + 2, 7, 7);
    }

    public void drawHeroProjectile(GraphicsContext gc, int screenX, int screenY) {
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        double radius = tileSize * 0.15;
        gc.setFill(Color.web("#64ffda"));
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.setFill(Color.web("#00e676", 0.45));
        gc.fillOval(cx - radius * 1.4, cy - radius * 1.4, radius * 2.8, radius * 2.8);
    }

    public void drawProjectile(GraphicsContext gc, int screenX, int screenY) {
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        double radius = tileSize * 0.18;
        gc.setFill(Color.web("#ce93d8"));
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.setFill(Color.web("#e040fb", 0.5));
        gc.fillOval(cx - radius * 1.5, cy - radius * 1.5, radius * 3, radius * 3);
    }

    public int getTileSize() {
        return tileSize;
    }

    // ---------- HELPERS ----------

    private void drawSprite(GraphicsContext gc, Image sprite, int sx, int sy, Color fallback) {
        if (sprite != null) {
            gc.drawImage(sprite, sx, sy, tileSize, tileSize);
        } else {
            gc.setFill(fallback);
            gc.fillRoundRect(sx + 6, sy + 4, tileSize - 12, tileSize - 8, 6, 6);
        }
    }

    private void drawSmallSprite(GraphicsContext gc, Image sprite, int sx, int sy, Color fallback) {
        int inset = tileSize / 4;
        int size = tileSize - inset * 2;
        if (sprite != null) {
            gc.drawImage(sprite, sx + inset, sy + inset, size, size);
        } else {
            gc.setFill(fallback);
            double cx = sx + tileSize / 2.0;
            double cy = sy + tileSize / 2.0;
            double s = tileSize * 0.22;
            gc.fillPolygon(
                    new double[]{cx, cx + s, cx, cx - s},
                    new double[]{cy - s, cy, cy + s, cy}, 4);
        }
    }

    private void drawMediumSprite(GraphicsContext gc, Image sprite, int sx, int sy, Color fallback) {
        int inset = tileSize / 6;
        int size = tileSize - inset * 2;
        if (sprite != null) {
            gc.drawImage(sprite, sx + inset, sy + inset, size, size);
        } else {
            gc.setFill(fallback);
            gc.fillRoundRect(sx + inset, sy + inset, size, size, 4, 4);
        }
    }

    private int tileHash(int x, int y) {
        int h = (x * 73856093) ^ (y * 19349663);
        return Math.abs(h);
    }
}
