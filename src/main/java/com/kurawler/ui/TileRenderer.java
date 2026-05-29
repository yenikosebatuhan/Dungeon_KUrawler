package com.kurawler.ui;

import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
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
    private Image bookshelfSprite;
    private Image doorSprite;
    private Image doorOpenSprite;
    private Image energyPotionSprite;

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
        loadDcssOverrides();
    }

    private Image loadImg(String path) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            Image img = new Image(is);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadDcssOverrides() {
        Image img;
        // Weapons — replace sprite-sheet guesses with verified DCSS tiles
        if ((img = loadImg("/assets/dc_sword.png"))   != null) swordSprite   = img;
        if ((img = loadImg("/assets/dc_dagger.png"))  != null) daggerSprite  = img;
        if ((img = loadImg("/assets/dc_axe.png"))     != null) axeSprite     = img;
        if ((img = loadImg("/assets/dc_bow.png"))     != null) bowSprite     = img;
        if ((img = loadImg("/assets/dc_staff.png"))   != null) staffSprite   = img;
        if ((img = loadImg("/assets/dc_hammer.png"))  != null) hammerSprite  = img;
        // Wearable
        if ((img = loadImg("/assets/dc_armor.png"))   != null) armorSprite   = img;
        if ((img = loadImg("/assets/dc_shield.png"))  != null) shieldSprite  = img;
        // Items
        if ((img = loadImg("/assets/dc_key.png"))        != null) keySprite        = img;
        if ((img = loadImg("/assets/dc_potion_hp.png"))  != null) potionSprite     = img;
        if ((img = loadImg("/assets/dc_potion_mp.png"))  != null) potionSprite2    = img;
        if ((img = loadImg("/assets/dc_potion_en.png"))  != null) energyPotionSprite = img;
        if ((img = loadImg("/assets/dc_scroll.png"))     != null) scrollSprite     = img;
        if ((img = loadImg("/assets/dc_gem.png"))        != null) gemSprite        = img;
        if ((img = loadImg("/assets/dc_ring.png"))       != null) ringSprite       = img;
        // Containers
        if ((img = loadImg("/assets/dc_chest.png"))      != null) chestSprite      = img;
        if ((img = loadImg("/assets/dc_chest_open.png")) != null) chestOpenSprite  = img;
        if ((img = loadImg("/assets/dc_crate.png"))      != null) crateSprite      = img;
        // Statics / dungeon
        if ((img = loadImg("/assets/dc_pool.png"))        != null) poolSprite       = img;
        if ((img = loadImg("/assets/dc_column.png"))      != null) columnSprite     = img;
        if ((img = loadImg("/assets/dc_bookshelf.png"))   != null) bookshelfSprite  = img;
        if ((img = loadImg("/assets/dc_pedestal.png"))    != null) pedestalSprite   = img;
        if ((img = loadImg("/assets/dc_pedestal.png"))    != null) altarSprite      = img;
        if ((img = loadImg("/assets/dc_door_closed.png")) != null) doorSprite       = img;
        if ((img = loadImg("/assets/dc_door_open.png"))   != null) doorOpenSprite   = img;
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
        armorSprite = weapons.getSprite(4, 3);
        shieldSprite = weapons.getSprite(6, 3);
        if (axeSprite == null) axeSprite = swordSprite;
        if (bowSprite == null) bowSprite = swordSprite;
        if (staffSprite == null) staffSprite = swordSprite;
        if (daggerSprite == null) daggerSprite = swordSprite;
        if (hammerSprite == null) hammerSprite = swordSprite;
        if (shieldSprite == null) shieldSprite = swordSprite;
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
        bookshelfSprite = statics.getSprite(12, 4);
        doorSprite = statics.getSprite(9, 4);
        doorOpenSprite = statics.getSprite(10, 4);
        if (poolSprite == null) poolSprite = statics.getSprite(6, 5);
        if (pedestalSprite == null) pedestalSprite = statics.getSprite(7, 5);
        if (bookshelfSprite == null) bookshelfSprite = crateSprite;
        if (doorSprite == null) doorSprite = columnSprite;
        if (doorOpenSprite == null) doorOpenSprite = doorSprite;
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
                sprite = energyPotionSprite != null ? energyPotionSprite : ringSprite;
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

    public void drawItemObject(GraphicsContext gc, int screenX, int screenY,
            com.kurawler.model.object.Item item) {
        if (item instanceof com.kurawler.model.object.Weapon) {
            drawWeaponByName(gc, screenX, screenY, item.getName());
        } else if (item instanceof com.kurawler.model.object.Wearable) {
            drawItem(gc, screenX, screenY, "Wearable");
        } else if (item instanceof com.kurawler.model.object.Scroll) {
            drawItem(gc, screenX, screenY, "Scroll");
        } else if (item instanceof com.kurawler.model.object.Consumable) {
            com.kurawler.model.object.Consumable cons = (com.kurawler.model.object.Consumable) item;
            String pt = cons.getManaAmount() > 0 ? "ManaPotion"
                    : cons.getEnergyAmount() > 0 ? "EnergyPotion"
                    : "HealthPotion";
            drawConsumable(gc, screenX, screenY, pt);
        } else if (item instanceof com.kurawler.model.object.Key) {
            drawItem(gc, screenX, screenY, "Key");
        } else {
            drawItem(gc, screenX, screenY, "Item");
        }
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
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("bookshelf")) {
                drawBookshelfProp(gc, screenX, screenY);
            } else if (lower.contains("pool") || lower.contains("gargoyle")) {
                drawPoolProp(gc, screenX, screenY);
            } else if (lower.contains("column") || lower.contains("pillar")) {
                drawColumnProp(gc, screenX, screenY);
            } else if (lower.contains("shelf")) {
                drawBookshelfProp(gc, screenX, screenY);
            } else {
                drawSprite(gc, pedestalSprite, screenX, screenY, Color.web("#887744"));
            }
        } else {
            drawSprite(gc, pedestalSprite, screenX, screenY, Color.web("#887744"));
        }
        drawSearchableMarker(gc, screenX, screenY);
        drawSearchableLabel(gc, screenX, screenY, name);
    }

    private void drawColumnProp(GraphicsContext gc, int screenX, int screenY) {
        if (columnSprite != null) {
            gc.drawImage(columnSprite, screenX, screenY, tileSize, tileSize);
            return;
        }
        double cx = screenX + tileSize / 2.0 - 6;
        double baseY = screenY + tileSize - 10;
        gc.setFill(Color.web("#6a6a7a"));
        gc.fillRect(cx, baseY - 22, 12, 22);
        gc.setFill(Color.web("#8a8a9a"));
        gc.fillRect(cx + 2, baseY - 20, 8, 18);
        gc.setStroke(Color.web("#4a4a5a"));
        gc.setLineWidth(1);
        gc.strokeLine(cx + 6, baseY - 22, cx + 6, baseY - 28);
    }

    private void drawPoolProp(GraphicsContext gc, int screenX, int screenY) {
        if (poolSprite != null) {
            gc.drawImage(poolSprite, screenX, screenY, tileSize, tileSize);
            return;
        }
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0 + 4;
        gc.setFill(Color.web("#1a4a6a", 0.9));
        gc.fillOval(cx - 14, cy - 8, 28, 16);
        gc.setFill(Color.web("#4fc3f7", 0.5));
        gc.fillOval(cx - 10, cy - 6, 20, 10);
    }

    private void drawBookshelfProp(GraphicsContext gc, int screenX, int screenY) {
        if (bookshelfSprite != null) {
            gc.drawImage(bookshelfSprite, screenX, screenY, tileSize, tileSize);
            return;
        }
        double x = screenX + 8;
        double y = screenY + 4;
        double w = tileSize - 16;
        double h = tileSize - 8;
        gc.setFill(Color.web("#4a3520"));
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.web("#6d4c2a"));
        gc.setLineWidth(1);
        for (int i = 1; i < 4; i++) {
            double ly = y + (h * i) / 4.0;
            gc.strokeLine(x, ly, x + w, ly);
        }
        gc.setFill(Color.web("#8b4513"));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                gc.fillRect(x + 3 + col * (w - 6) / 4.0, y + 4 + row * 6, 4, 8);
            }
        }
    }

    private void drawSearchableLabel(GraphicsContext gc, int screenX, int screenY, String name) {
        if (name == null) {
            return;
        }
        String lower = name.toLowerCase();
        String label;
        if (lower.contains("bookshelf")) {
            label = "BOOKSHELF";
        } else if (lower.contains("pool") || lower.contains("gargoyle")) {
            label = "POOL";
        } else if (lower.contains("column") || lower.contains("pillar")) {
            label = "COLUMN";
        } else if (lower.contains("shelf")) {
            label = "SHELF";
        } else {
            label = "SEARCH";
        }
        gc.setFill(Color.web("#ffd54f"));
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 8));
        gc.fillText(label, screenX + 4, screenY + tileSize - 6);
    }

    /**
     * Clear door graphic — asset sheet coords looked like a chest/shelf.
     * Gold frame + DOOR label so it is not confused with searchable furniture.
     */
    public void drawPortal(GraphicsContext gc, int screenX, int screenY, boolean open, String portalName) {
        Image sprite = open ? doorOpenSprite : doorSprite;
        if (sprite != null) {
            gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
        } else {
            int pad = 6;
            double x = screenX + pad, y = screenY + pad;
            double w = tileSize - pad * 2, h = tileSize - pad * 2;
            gc.setFill(Color.web("#1a1208", 0.85));
            gc.fillRect(x, y, w, h);
            gc.setStroke(Color.web(open ? "#ffd54f" : "#e74c3c"));
            gc.setLineWidth(2.5);
            gc.strokeRect(x + 2, y + 2, w - 4, h - 4);
            gc.setFill(Color.web(open ? "#8b6914" : "#4a3520"));
            double gap = open ? w * 0.22 : 0;
            gc.fillRect(x + 4, y + 4, (w - 8 - gap) / 2, h - 8);
            gc.fillRect(x + 4 + (w - 8 - gap) / 2 + gap, y + 4, (w - 8 - gap) / 2, h - 8);
        }
        gc.setFill(Color.web("#ffd54f"));
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 9));
        String label = (portalName != null && portalName.toLowerCase().contains("iron")) ? "IRON DOOR" : "DOOR";
        gc.fillText(label, screenX + tileSize / 2.0 - (label.length() > 4 ? 22 : 14), screenY + tileSize - 4);
        if (open) {
            gc.fillText("\u2192", screenX + tileSize / 2.0 - 4, screenY + 10);
        }
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
        gc.setStroke(Color.web("#ffd54f", 0.85));
        gc.setLineWidth(2);
        gc.setLineDashes(5, 4);
        gc.strokeRect(screenX + 2, screenY + 2, tileSize - 4, tileSize - 4);
        gc.setLineDashes(null);
        gc.setFill(Color.web("#ffd54f"));
        gc.fillOval(screenX + tileSize - 11, screenY + 2, 9, 9);
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillText("?", screenX + tileSize - 9, screenY + 9);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 8));
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

    /** Returns the best sprite Image for rendering an Item in the inventory popup. */
    public Image getSpriteForItem(Item item) {
        if (item instanceof Weapon) {
            String n = item.getName().toLowerCase();
            if (n.contains("axe"))                         return axeSprite;
            if (n.contains("bow"))                         return bowSprite;
            if (n.contains("staff") || n.contains("spell")) return staffSprite;
            if (n.contains("dagger") || n.contains("knife")) return daggerSprite;
            if (n.contains("hammer") || n.contains("mace")) return hammerSprite;
            return swordSprite;
        }
        if (item instanceof Wearable) {
            String n = item.getName().toLowerCase();
            if (n.contains("shield")) return shieldSprite;
            return armorSprite;
        }
        if (item instanceof Consumable) {
            String n = item.getName().toLowerCase();
            if (n.contains("mana"))   return potionSprite2;
            if (n.contains("energy")) return energyPotionSprite != null ? energyPotionSprite : ringSprite;
            return potionSprite;
        }
        if (item instanceof Scroll) return scrollSprite;
        if (item instanceof Key)    return keySprite;
        return gemSprite;
    }

    /** Small preview for level-editor palette chips (PBI-16). */
    public void drawEditorPaletteIcon(GraphicsContext gc, double size, String objectName) {
        if (gc == null || objectName == null) {
            return;
        }
        int s = (int) size;
        gc.clearRect(0, 0, s, s);
        gc.setFill(Color.web("#141428"));
        gc.fillRect(0, 0, s, s);

        switch (objectName) {
            case "Wall":
                drawScaled(gc, pickWallSprite(0, 0), s);
                break;
            case "Floor":
                drawScaled(gc, pickFloorSprite(0, 0), s);
                break;
            case "Gargoyle Pool":
                drawScaled(gc, poolSprite, s);
                break;
            case "Ancient Bookshelf":
            case "Wall Shelf":
                drawScaled(gc, bookshelfSprite != null ? bookshelfSprite : crateSprite, s);
                break;
            case "Stone Column":
                drawScaled(gc, columnSprite, s);
                break;
            case "Sword":
                drawScaled(gc, swordSprite, s);
                break;
            case "Spell Book":
                drawScaled(gc, scrollSprite, s);
                break;
            case "Health Potion":
                drawScaled(gc, potionSprite, s);
                break;
            case "Armor":
                drawScaled(gc, armorSprite != null ? armorSprite : shieldSprite, s);
                break;
            case "Golden Key":
                drawScaled(gc, keySprite, s);
                break;
            case "Chest":
                drawScaled(gc, chestSprite, s);
                break;
            case "Pouch":
                drawScaled(gc, crateSprite, s);
                break;
            case "Adventurer":
                drawScaled(gc, heroSprite, s);
                break;
            case "Knight":
                drawScaled(gc, knightSprite, s);
                break;
            case "Sorcerer":
                drawScaled(gc, sorcererSprite, s);
                break;
            case "Erase":
                gc.setFill(Color.web("#4a2020"));
                gc.fillRoundRect(2, 2, s - 4, s - 4, 4, 4);
                gc.setStroke(Color.web("#ff6b6b"));
                gc.strokeLine(4, 4, s - 4, s - 4);
                gc.strokeLine(s - 4, 4, 4, s - 4);
                break;
            default:
                gc.setFill(Color.web("#553355"));
                gc.fillRoundRect(4, 4, s - 8, s - 8, 4, 4);
                break;
        }
    }

    private Image pickWallSprite(int sx, int sy) {
        if (wallSprites != null && wallSprites.length > 0) {
            return wallSprites[tileHash(sx, sy) % wallSprites.length];
        }
        return null;
    }

    private Image pickFloorSprite(int sx, int sy) {
        if (floorSprites != null && floorSprites.length > 0) {
            return floorSprites[tileHash(sx, sy) % floorSprites.length];
        }
        return null;
    }

    private void drawScaled(GraphicsContext gc, Image img, int size) {
        if (img != null) {
            gc.drawImage(img, 0, 0, size, size);
        }
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
