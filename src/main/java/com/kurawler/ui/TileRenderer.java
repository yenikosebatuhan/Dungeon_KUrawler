package com.kurawler.ui;

import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.ResourceAssetCatalog;
import com.kurawler.model.WeaponCatalog;
import com.kurawler.model.object.Wearable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TileRenderer {

    private static final int SPRITE_SIZE = 48;

    private final int tileSize;

    /** What kind of model object an editor catalog asset becomes when placed. */
    public enum AssetKind {
        WEAPON, ITEM, CONTAINER, STATIC, HERO, KNIGHT, GUARD, SORCERER, ASSASSIN, NPC, TERRAIN_WALL, TERRAIN_FLOOR,
        PORTAL
    }

    /** A placeable asset entry surfaced in the level editor palette. */
    public static final class EditorAsset {
        public final String name;
        public final String category;
        public final AssetKind kind;

        public EditorAsset(String name, String category, AssetKind kind) {
            this.name = name;
            this.category = category;
            this.kind = kind;
        }
    }

    // Sliced sprite sheets — every usable tile is exposed in the editor
    private SpriteAtlas itemsAtlas;
    private SpriteAtlas containersAtlas;
    private SpriteAtlas charactersAtlas;
    private final LinkedHashMap<String, Image> atlasRegistry = new LinkedHashMap<>();
    private final List<EditorAsset> editorAssets = new ArrayList<>();

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
    private Image guardSprite;
    private Image guardSprite2;
    private Image sorcererSprite;
    private Image sorcererSprite2;
    private Image assassinSprite;
    private Image assassinSprite2;
    private Image[] heroWalkFrames;
    private Image[] knightWalkFrames;
    private Image[] assassinWalkFrames;

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
    private Image[] sparkleFrames;

    // Extra DCSS assets exposed in the level editor
    private Image goldChestSprite;
    private Image stairsDownSprite;
    private Image stairsUpSprite;
    private Image statueSprite;
    private Image fountainSprite;
    private Image sarcophagusSprite;
    private Image lanternSprite;
    private Image goldPileSprite;
    private Image wallStoneSprite;
    private Image wallDarkSprite;
    private Image wallBrownSprite;
    private Image[] floorStoneSprites;

    public TileRenderer(int tileSize) {
        this.tileSize = tileSize;

        characters = new SpriteSheet("/assets/characters_x3.png", SPRITE_SIZE);
        walls = new SpriteSheet("/assets/walls_x3.png", SPRITE_SIZE);
        statics = new SpriteSheet("/assets/walls and statics x3.png", SPRITE_SIZE);
        items = new SpriteSheet("/assets/items_x3.png", SPRITE_SIZE);
        weapons = new SpriteSheet("/assets/weapons_x3.png", SPRITE_SIZE);
        containers = new SpriteSheet("/assets/containers_x3.png", SPRITE_SIZE);

        loadSprites();
        buildEditorCatalog();
    }

    /**
     * Builds the full level-editor asset catalog by slicing every sprite sheet
     * into its individual tiles. Each non-empty tile becomes a placeable asset
     * registered by a stable, friendly name so it renders identically in the
     * palette, on the map, and after save/load.
     */
    private void buildEditorCatalog() {
        itemsAtlas = new SpriteAtlas("/assets/items_x3.png", SPRITE_SIZE);
        containersAtlas = new SpriteAtlas("/assets/containers_x3.png", SPRITE_SIZE);
        charactersAtlas = new SpriteAtlas("/assets/characters_x3.png", SPRITE_SIZE);

        Image wsSheet = loadImg("/assets/walls and statics x3.png");

        // --- Walls / Floors: each is a distinct, theme-matching sprite (no random
        //     per-tile variants). Brick = main tileset, Dark = rock, Brown = orange brick.
        editorAssets.add(new EditorAsset("Wall", "Walls/Floors", AssetKind.TERRAIN_WALL));
        editorAssets.add(new EditorAsset("Dark Wall", "Walls/Floors", AssetKind.TERRAIN_WALL));
        editorAssets.add(new EditorAsset("Brown Wall", "Walls/Floors", AssetKind.TERRAIN_WALL));

        // Decorated solid wall variants pulled straight from the tileset's wall row
        // (vine, water grate, barred window, mooring ring, coloured torch sconces).
        if (wsSheet != null) {
            addWallVariant(wsSheet, "Vine Wall", 3, 1);
            addWallVariant(wsSheet, "Water Grate", 4, 1);
            addWallVariant(wsSheet, "Caged Wall", 5, 1);
            addWallVariant(wsSheet, "Ring Wall", 6, 1);
            addWallVariant(wsSheet, "Red Torch Wall", 11, 1);
            addWallVariant(wsSheet, "Green Torch Wall", 12, 1);
            addWallVariant(wsSheet, "Blue Torch Wall", 13, 1);
        }

        // Every floor tile from the tileset's floor block — plain first (default),
        // then all the cobble/rubble/gravel variants. Each is its own placeable floor.
        if (wsSheet != null) {
            addFloorTile(wsSheet, "Stone Floor", 4, 3);
            addFloorTile(wsSheet, "Smooth Floor", 1, 2);
            addFloorTile(wsSheet, "Speckled Floor", 0, 2);
            addFloorTile(wsSheet, "Dusty Floor", 2, 2);
            addFloorTile(wsSheet, "Cobbled Floor", 1, 3);
            addFloorTile(wsSheet, "Gravel Floor", 2, 3);
            addFloorTile(wsSheet, "Stone Rubble", 0, 3);
            addFloorTile(wsSheet, "Cobble Border", 0, 4);
            addFloorTile(wsSheet, "Rubble Floor", 2, 4);
            addFloorTile(wsSheet, "Rocky Floor", 3, 4);
            addFloorTile(wsSheet, "Pebble Floor", 0, 5);
            addFloorTile(wsSheet, "Worn Floor", 2, 5);
        }

        // --- Statics: use the clean, individually-cropped dc_* prop tiles (the big
        //     "walls and statics" sheet is made of multi-tile structures, so slicing it
        //     produces meaningless half-wall fragments). Each file maps to its real name.
        addNamedSprites("Statics", AssetKind.STATIC, new String[][] {
                { "/assets/dc_column.png", "Stone Column" },
                { "/assets/dc_column2.png", "Carved Column" },
                { "/assets/dc_column3.png", "Ruined Column" },
                { "/assets/dc_pool.png", "Gargoyle Pool" },
                { "/assets/dc_fountain_blue.png", "Stone Fountain" },
                { "/assets/dc_fountain_sparkling.png", "Magic Fountain" },
                { "/assets/dc_bookshelf.png", "Bookshelf" },
                { "/assets/dc_bookshelf_book.png", "Book Stack" },
                { "/assets/dc_book.png", "Reading Lectern" },
                { "/assets/dc_pedestal.png", "Stone Pedestal" },
                { "/assets/dc_statue.png", "Stone Statue" },
                { "/assets/dc_statue_sword.png", "Knight Statue" },
                { "/assets/dc_sarcophagus.png", "Sarcophagus" },
                { "/assets/dc_door_closed.png", "Wooden Door" },
                { "/assets/dc_door_open.png", "Open Door" },
                { "/assets/dc_door_runed.png", "Runed Door" },
                { "/assets/dc_stairs_down.png", "Stairs Down" },
                { "/assets/dc_stairs_up.png", "Stairs Up" },
                { "/assets/dc_lantern.png", "Lantern" },
                { "/assets/dc_lamp.png", "Oil Lamp" },
                { "/assets/dc_gold.png", "Gold Pile" },
                { "/assets/dc_orb.png", "Glowing Orb" },
                { "/assets/dc_orb2.png", "Blue Orb" },
                { "/assets/dc_orb3.png", "Dark Orb" },
                { "/assets/dc_gateway_enter.png", "Gateway" },
                { "/assets/dc_portal_blue.png", "Blue Portal" },
                { "/assets/dc_portal_magic.png", "Magic Portal" },
        });

        // --- Complete multi-tile structures cropped whole from the tileset (the big
        //     wooden door, arches, window, carpets, barrels) so they are never sliced
        //     into half-wall fragments. Rendered aspect-preserved.
        if (wsSheet != null) {
            addStructure(wsSheet, "Great Door", 152, 14, 96, 90);
            addStructure(wsSheet, "Stone Archway", 40, 11, 96, 109);
            addStructure(wsSheet, "Barred Window", 88, 8, 96, 112);
            addStructure(wsSheet, "Red Carpet", 360, 24, 79, 92);
            addStructure(wsSheet, "Tan Carpet", 392, 24, 79, 92);
            addStructure(wsSheet, "Barrel Stack", 152, 56, 80, 72);
        }
        registerWallAndStaticResourceTiles();

        // --- Weapons: verified DCSS tiles (game + editor); sheet extras are trimmed per cell.
        addNamedSprites("Weapons", AssetKind.WEAPON, new String[][] {
                { "/assets/dc_sword.png", "Sword" },
                { "/assets/dc_dagger.png", "Dagger" },
                { "/assets/dc_axe.png", "Axe" },
                { "/assets/dc_bow.png", "Bow" },
                { "/assets/dc_staff.png", "Staff" },
                { "/assets/dc_hammer.png", "Hammer" },
        });
        Image weaponsSheet = loadImg("/assets/weapons_x3.png");
        if (weaponsSheet != null) {
            registerWeaponsFromAtlas(weaponsSheet);
            registerWeaponNameAliases();
        }
        // --- Items / Containers: use the pre-sliced resource folders for issue 143.
        registerItemResourceTiles();
        registerContainerResourceTiles();
        // Gameplay wearables (Entry Hall etc.) — match MapFactory item names exactly.
        addNamedSprites("Wearables", AssetKind.ITEM, new String[][] {
                { "/assets/dc_armor.png", "Armor" },
                { "/assets/dc_shield.png", "Shield" },
                { "/assets/dc_ring.png", "Ring" },
        });

        // --- Navigation: portals / stairs / doors linking maps together.
        addNamedSprites("Navigation", AssetKind.PORTAL, new String[][] {
                { "/assets/dc_stairs_down.png", "Stairs Down" },
                { "/assets/dc_stairs_up.png",   "Stairs Up" },
                { "/assets/dc_door_closed.png",  "Door" },
                { "/assets/dc_portal_blue.png",  "Portal" },
                { "/assets/dc_portal_magic.png", "Magic Portal" },
        });

        // --- Characters: functional placeables first, then NPC standees named by class/row.
        addCharacterAsset("/assets/characters/tile018.png", "Hero", AssetKind.HERO);
        addCharacterAsset("/assets/characters/MainChar012.png", "Hero Blonde", AssetKind.HERO);
        addCharacterAsset("/assets/characters/MainChar013.png", "Hero Yellow", AssetKind.HERO);
        addCharacterAsset("/assets/characters/tile009.png", "Knight", AssetKind.KNIGHT);
        addCharacterAsset("/assets/characters/MainChar005.png", "Horned Guard Brown", AssetKind.GUARD);
        addCharacterAsset("/assets/characters/MainChar006.png", "Horned Guard Blue", AssetKind.GUARD);
        addCharacterAsset("/assets/characters/MainChar003.png", "Sorcerer Purple", AssetKind.SORCERER);
        addCharacterAsset("/assets/characters/MainChar004.png", "Sorcerer Red", AssetKind.SORCERER);
        addCharacterAsset("/assets/characters/MainChar016.png", "Assassin Blue Hood", AssetKind.ASSASSIN);
        addCharacterAsset("/assets/characters/MainChar015.png", "Assassin Red Hood", AssetKind.ASSASSIN);
    }

    /** Resolves a unique catalog name and registers it once. */
    private String registerUnique(String base) {
        String name = base;
        int dup = 1;
        while (atlasRegistry.containsKey(name)) {
            dup++;
            name = base + " " + dup;
        }
        return name;
    }

    private interface TileNamer {
        String nameFor(int col, int row);
    }

    /** Slices a clean grid sheet, naming each non-empty tile via {@code namer}. */
    private void addSlicedSheet(SpriteAtlas atlas, String category, AssetKind kind, TileNamer namer) {
        if (atlas == null || !atlas.isLoaded()) {
            return;
        }
        for (int r = 0; r < atlas.getRows(); r++) {
            for (int c = 0; c < atlas.getCols(); c++) {
                if (!atlas.isNonEmpty(c, r)) {
                    continue;
                }
                String name = registerUnique(namer.nameFor(c, r));
                atlasRegistry.put(name, atlas.getTileTrimmed(c, r));
                editorAssets.add(new EditorAsset(name, category, kind));
            }
        }
    }


    private void registerWeaponsFromAtlas(Image sheet) {
        for (String[] row : WeaponCatalog.ENTRIES) {
            if (row == null || row.length < 5) {
                continue;
            }
            addWeaponCrop(sheet, row[0],
                    Integer.parseInt(row[1]), Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]), Integer.parseInt(row[4]));
        }
    }

    private void addWeaponCrop(Image sheet, String name, int bx, int by, int bw, int bh) {
        Image crop = trimTileCrop(cropPx(sheet, bx, by, bw, bh));
        if (crop == null || crop.getWidth() * crop.getHeight() < 120) {
            return;
        }
        if (atlasRegistry.containsKey(name)) {
            return;
        }
        atlasRegistry.put(name, crop);
        editorAssets.add(new EditorAsset(name, "Weapons", AssetKind.WEAPON));
    }

    private void registerItemResourceTiles() {
        for (String[] entry : ResourceAssetCatalog.ITEM_TILES) {
            addResourceSprite("/assets/items/" + entry[0] + ".png", entry[1], "Items", AssetKind.ITEM);
        }
    }

    private void registerContainerResourceTiles() {
        for (String[] entry : ResourceAssetCatalog.CONTAINER_TILES) {
            addResourceSprite("/assets/containers/" + entry[0] + ".png", entry[1], "Containers", AssetKind.CONTAINER);
        }
    }

    private void addCharacterAsset(String path, String name, AssetKind kind) {
        addResourceSprite(path, name, "Characters", kind);
    }

    private void registerWallAndStaticResourceTiles() {
        for (int i = 0; i < 240; i++) {
            String id = String.format("tile%03d", i);
            String name = ResourceAssetCatalog.wallStaticName(id);
            AssetKind kind = ResourceAssetCatalog.isTerrainWall(name) ? AssetKind.TERRAIN_WALL
                    : ResourceAssetCatalog.isTerrainFloor(name) ? AssetKind.TERRAIN_FLOOR
                    : AssetKind.STATIC;
            String category = kind == AssetKind.STATIC ? "Statics" : "Walls/Floors";
            addResourceSprite("/assets/wall and statics/" + id + ".png", name, category, kind);
        }
    }

    private void addResourceSprite(String path, String name, String category, AssetKind kind) {
        Image img = loadImg(path);
        if (!hasOpaquePixels(img, 8)) {
            return;
        }
        String unique = registerUnique(name);
        atlasRegistry.put(unique, img);
        editorAssets.add(new EditorAsset(unique, category, kind));
    }

    private boolean hasOpaquePixels(Image img, int minimum) {
        if (img == null || img.getPixelReader() == null) {
            return false;
        }
        PixelReader pr = img.getPixelReader();
        int opaque = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (pr.getColor(x, y).getOpacity() > 0.18 && ++opaque >= minimum) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isResourceCellTile(String name) {
        if (name == null) {
            return false;
        }
        for (int i = 0; i <= name.length() - 5; i++) {
            if (Character.isDigit(name.charAt(i))
                    && Character.isDigit(name.charAt(i + 1))
                    && name.charAt(i + 2) == '-'
                    && Character.isDigit(name.charAt(i + 3))
                    && Character.isDigit(name.charAt(i + 4))) {
                return true;
            }
        }
        return false;
    }

    private void registerWeaponNameAliases() {
        aliasWeapon("War Axe", "Battle Axe");
        aliasWeapon("Rusty Dagger", "Silver Dagger");
        aliasWeapon("Ceremonial Dagger", "Silver Dagger");
        aliasWeapon("Chipped Sword", "Tan Sword");
        aliasWeapon("Jeweled Sword", "Golden Sword");
        aliasWeapon("Ornate Sword", "Ornate Golden Sword");
    }

    private void aliasWeapon(String alias, String source) {
        if (alias == null || source == null || atlasRegistry.containsKey(alias)) {
            return;
        }
        Image img = atlasRegistry.get(source);
        if (img != null) {
            atlasRegistry.put(alias, img);
        }
    }

    /** Direct pixel crop on an x3 sheet (no base-pixel scaling). */
    private Image cropSheetPx(Image src, int x, int y, int w, int h) {
        if (src == null || w <= 0 || h <= 0) {
            return null;
        }
        PixelReader pr = src.getPixelReader();
        if (pr == null) {
            return null;
        }
        if (x < 0 || y < 0 || x + w > src.getWidth() || y + h > src.getHeight()) {
            return null;
        }
        return new WritableImage(pr, x, y, w, h);
    }

    /** Registers a single floor tile (passable) under the Walls/Floors category. */
    private void addFloorTile(Image sheet, String name, int col, int row) {
        Image crop = trimTileCrop(cropPx(sheet, col * 16, row * 16, 16, 16));
        if (crop == null) {
            return;
        }
        String unique = registerUnique(name);
        atlasRegistry.put(unique, crop);
        editorAssets.add(new EditorAsset(unique, "Walls/Floors", AssetKind.TERRAIN_FLOOR));
    }

    /** Registers a single decorated wall tile (solid) under the Walls/Floors category. */
    private void addWallVariant(Image sheet, String name, int col, int row) {
        // 1px inset avoids bleeding from neighbouring cells on the x3 sheet.
        Image crop = trimTileCrop(cropPx(sheet, col * 16 + 1, row * 16 + 1, 14, 14));
        if (crop == null) {
            return;
        }
        String unique = registerUnique(name);
        atlasRegistry.put(unique, crop);
        editorAssets.add(new EditorAsset(unique, "Walls/Floors", AssetKind.TERRAIN_WALL));
    }

    /** Crops a multi-tile region (base-pixel coords) out of an x3 sheet and registers it. */
    private void addStructure(Image sheet, String name, int bx, int by, int bw, int bh) {
        Image crop = trimTileCrop(cropPx(sheet, bx, by, bw, bh));
        if (crop == null) {
            return;
        }
        String unique = registerUnique(name);
        atlasRegistry.put(unique, crop);
        editorAssets.add(new EditorAsset(unique, "Statics", AssetKind.STATIC));
    }

    /** Crop helper for the x3 ("3x scaled") sheets: input coords are in base pixels. */
    private Image cropPx(Image src, int bx, int by, int bw, int bh) {
        if (src == null) {
            return null;
        }
        PixelReader pr = src.getPixelReader();
        if (pr == null) {
            return null;
        }
        int s = 3;
        int x = bx * s, y = by * s, w = bw * s, h = bh * s;
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > src.getWidth() || y + h > src.getHeight()) {
            return null;
        }
        return new WritableImage(pr, x, y, w, h);
    }

    private Image trimTileCrop(Image crop) {
        return SpriteAtlas.trimOpaqueBounds(crop);
    }

    /** Registers individual, pre-cropped sprite files under their real display names. */
    private void addNamedSprites(String category, AssetKind kind, String[][] fileNamePairs) {
        for (String[] pair : fileNamePairs) {
            Image img = loadImg(pair[0]);
            if (img == null) {
                continue;
            }
            img = SpriteAtlas.trimOpaqueBounds(img);
            String name = registerUnique(pair[1]);
            atlasRegistry.put(name, img);
            editorAssets.add(new EditorAsset(name, category, kind));
        }
    }

    private static String gridName(String[][] grid, int col, int row, String fallback) {
        if (row >= 0 && row < grid.length && col >= 0 && col < grid[row].length) {
            String n = grid[row][col];
            if (n != null && !n.isEmpty()) {
                return n;
            }
        }
        return fallback;
    }

    private static String characterName(int row) {
        switch (row) {
            case 0: return "Robed Acolyte";
            case 1: return "Cyan Mage";
            case 2: return "Adventurer";
            case 3: return "Blue Wizard";
            default: return "Dungeon Dweller";
        }
    }

    // === Real per-cell names derived from inspecting the actual sprite sheets ===

    // items_x3.png (6 cols x 4 rows): row0 keys, row1 potions+rings, row2 skulls/gems/coins, row3 orb/book/scrolls
    private static final String[][] ITEM_GRID = {
            { "Skull Key", "Silver Key", "Gold Key", "Brass Key", "Key Ring", "Ornate Key" },
            { "Health Potion", "Mana Potion", "Energy Potion", "Ruby Ring", "Emerald Ring", "Sapphire Ring" },
            { "Bone Skull", "Cracked Skull", "Gold Gem", "Amber Gem", "Topaz Gem", "Gold Coins" },
            { "Crystal Orb", "Spell Book", "Glyph Scroll", "Rune Scroll", "Sigil Scroll", "Curse Scroll" },
    };

    // containers_x3.png (4 cols x 6 rows): chests/crates/pouches and their open variants
    private static final String[][] CONTAINER_GRID = {
            { "Dark Chest", "Gilded Chest", "Wooden Crate", "Tall Crate" },
            { "Iron Chest", "Banded Chest", "Wooden Box", "Storage Box" },
            { "Open Dark Chest", "Open Gilded Chest", "", "" },
            { "Oak Chest", "Crimson Chest", "Silver Chest", "Open Crate" },
            { "Bronze Chest", "Copper Chest", "Amber Chest", "Leather Pouch" },
            { "Open Bronze Chest", "Open Copper Chest", "Open Amber Chest", "Coin Pouch" },
    };


    /** All placeable editor assets, in display order (grouped by category). */
    public List<EditorAsset> getEditorAssets() {
        return new ArrayList<>(editorAssets);
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
        if ((img = loadImg("/assets/dc_bookshelf.png"))    != null) bookshelfSprite  = SpriteAtlas.trimOpaqueBounds(img);
        if ((img = loadImg("/assets/dc_pedestal.png"))    != null) pedestalSprite   = img;
        if ((img = loadImg("/assets/dc_pedestal.png"))    != null) altarSprite      = img;
        if ((img = loadImg("/assets/dc_door_closed.png")) != null) doorSprite       = img;
        if ((img = loadImg("/assets/dc_door_open.png"))   != null) doorOpenSprite   = img;
        // Extra dungeon statics / terrain exposed in the level editor
        if ((img = loadImg("/assets/dc_chest_stone.png")) != null) goldChestSprite  = img;
        if ((img = loadImg("/assets/dc_stairs_down.png")) != null) stairsDownSprite = img;
        if ((img = loadImg("/assets/dc_stairs_up.png"))   != null) stairsUpSprite   = img;
        if ((img = loadImg("/assets/dc_statue.png"))      != null) statueSprite     = img;
        if ((img = loadImg("/assets/dc_fountain_blue.png")) != null) fountainSprite = img;
        if ((img = loadImg("/assets/dc_sarcophagus.png")) != null) sarcophagusSprite = img;
        if ((img = loadImg("/assets/dc_lantern.png"))     != null) lanternSprite    = img;
        if ((img = loadImg("/assets/dc_gold.png"))        != null) goldPileSprite   = img;
        if ((img = loadImg("/assets/dc_wall_dark.png"))   != null) wallDarkSprite   = img;
        if ((img = loadImg("/assets/dc_wall_brown.png"))  != null) wallBrownSprite  = img;

        // Themed dungeon brick + dark stone floors cropped from the project's main
        // tileset (the sandy dc_floor_stone tiles were off-theme and near-identical).
        if (statics != null && statics.isLoaded()) {
            Image brick = statics.getSprite(1, 1);
            if (brick != null) wallStoneSprite = brick;
            // Floor is one cohesive dark-purple stone material. [0] is the clean, flat
            // tile (the dominant one); the rest are subtly cobbled/worn variants of the
            // SAME stone so the floor looks like a real, naturally-worn dungeon rather
            // than one flat colour. They are blended deterministically by position.
            Image df0 = statics.getSprite(4, 3); // clean flat stone
            Image df1 = statics.getSprite(1, 3); // cobbled
            Image df2 = statics.getSprite(2, 3); // cobbled
            Image df3 = statics.getSprite(0, 4); // lightly worn
            Image df4 = statics.getSprite(3, 3); // cobbled edge
            if (df0 != null && df1 != null && df2 != null && df3 != null && df4 != null) {
                floorStoneSprites = new Image[]{ df0, df1, df2, df3, df4 };
            }
        }
        if (floorStoneSprites == null) {
            Image f1 = loadImg("/assets/dc_floor_stone.png");
            Image f2 = loadImg("/assets/dc_floor_stone2.png");
            Image f3 = loadImg("/assets/dc_floor_stone3.png");
            if (f1 != null || f2 != null || f3 != null) {
                floorStoneSprites = new Image[]{
                        f1 != null ? f1 : (f2 != null ? f2 : f3),
                        f2 != null ? f2 : (f1 != null ? f1 : f3),
                        f3 != null ? f3 : (f1 != null ? f1 : f2),
                };
            }
        }
        // Sparkle animation frames for searchable indicator
        Image s1 = loadImg("/assets/dc_fx_sparkle1.png");
        Image s2 = loadImg("/assets/dc_fx_sparkle2.png");
        Image s3 = loadImg("/assets/dc_fx_sparkle3.png");
        if (s1 != null && s2 != null && s3 != null) {
            sparkleFrames = new Image[]{s1, s2, s3};
        }
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
        heroWalkFrames = loadCharacterFrames(18, 9);
        knightWalkFrames = loadCharacterFrames(9, 9);
        assassinWalkFrames = null;

        heroSprite = firstFrame(heroWalkFrames, "/assets/characters/MainChar012.png");
        heroSpriteRight = frameOr(heroWalkFrames, 1, heroSprite);
        heroSpriteLeft = frameOr(heroWalkFrames, 2, heroSprite);
        heroSpriteUp = frameOr(heroWalkFrames, 8, heroSprite);

        knightSprite = firstFrame(knightWalkFrames, "/assets/characters/MainChar005.png");
        knightSprite2 = frameOr(knightWalkFrames, 1, knightSprite);
        guardSprite = loadImg("/assets/characters/MainChar005.png");
        guardSprite2 = loadImg("/assets/characters/MainChar006.png");

        sorcererSprite = loadImg("/assets/characters/MainChar003.png");
        sorcererSprite2 = loadImg("/assets/characters/MainChar004.png");
        assassinSprite = loadImg("/assets/characters/MainChar016.png");
        assassinSprite2 = loadImg("/assets/characters/MainChar015.png");
        if (assassinSprite == null) assassinSprite = assassinSprite2;

        if (heroSpriteUp == null) heroSpriteUp = heroSprite;
        if (heroSpriteLeft == null) heroSpriteLeft = heroSprite;
        if (heroSpriteRight == null) heroSpriteRight = heroSprite;
        if (knightSprite2 == null) knightSprite2 = knightSprite;
        if (guardSprite == null) guardSprite = guardSprite2;
        if (guardSprite2 == null) guardSprite2 = guardSprite;
        if (sorcererSprite2 == null) sorcererSprite2 = sorcererSprite;
        if (assassinSprite2 == null) assassinSprite2 = assassinSprite;
    }

    private Image[] loadCharacterFrames(int firstTile, int count) {
        Image[] frames = new Image[count];
        for (int i = 0; i < count; i++) {
            frames[i] = loadImg(String.format("/assets/characters/tile%03d.png", firstTile + i));
        }
        return frames;
    }

    private Image firstFrame(Image[] frames, String fallbackPath) {
        Image frame = frameOr(frames, 0, null);
        return frame != null ? frame : loadImg(fallbackPath);
    }

    private Image frameOr(Image[] frames, int index, Image fallback) {
        if (frames != null && index >= 0 && index < frames.length && frames[index] != null) {
            return frames[index];
        }
        return fallback;
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
        if (!weapons.isLoaded()) {
            return;
        }
        Image sheet = loadImg("/assets/weapons_x3.png");
        if (sheet != null) {
            swordSprite = cropPx(sheet, 99, 10, 10, 21);
            daggerSprite = cropPx(sheet, 5, 2, 6, 13);
            axeSprite = cropPx(sheet, 53, 74, 9, 21);
            staffSprite = cropPx(sheet, 3, 26, 10, 37);
            hammerSprite = cropPx(sheet, 19, 39, 10, 24);
            bowSprite = cropPx(sheet, 101, 132, 7, 25);
        }
        if (swordSprite == null) swordSprite = weapons.getSprite(0, 0);
        if (axeSprite == null) axeSprite = weapons.getSprite(0, 1);
        if (bowSprite == null) bowSprite = weapons.getSprite(0, 4);
        if (staffSprite == null) staffSprite = weapons.getSprite(2, 3);
        if (daggerSprite == null) daggerSprite = weapons.getSprite(0, 2);
        if (hammerSprite == null) hammerSprite = weapons.getSprite(8, 3);
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
        // Bookshelf: use dc_bookshelf.png (loadDcssOverrides) — sheet (12,4) is a door plank.
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
        drawPatternedFloor(gc, screenX, screenY);
    }

    // Per-tile floor-variant distribution: mostly the clean flat stone (index 0) with
    // cobbled/worn variants sprinkled in, so the dungeon floor looks naturally worn
    // instead of either one flat colour or a repeating geometric pattern.
    private static final int[] FLOOR_BLEND = {0, 0, 0, 0, 0, 1, 2, 3, 0, 4, 0, 2, 1, 0, 3, 0};

    /**
     * Draws the default floor by picking, deterministically from the cell position,
     * one of the cohesive stone-floor variants. No random flicker, no geometric grid:
     * the same cell always shows the same stone, giving a stable, natural dungeon look.
     */
    private void drawPatternedFloor(GraphicsContext gc, int screenX, int screenY) {
        gc.setImageSmoothing(false);
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);

        Image tile = floorVariantFor(screenX / tileSize, screenY / tileSize);
        if (tile != null) {
            gc.drawImage(tile, screenX, screenY, tileSize, tileSize);
        }
    }

    private Image floorVariantFor(int col, int row) {
        if (floorStoneSprites == null || floorStoneSprites.length == 0) {
            return (floorSprites != null && floorSprites.length > 0) ? floorSprites[0] : null;
        }
        int h = col * 341873128 + row * 132897987;
        h ^= (h >>> 13);
        h *= 0x5bd1e995;
        h ^= (h >>> 11);
        int idx = FLOOR_BLEND[Math.floorMod(h, FLOOR_BLEND.length)];
        if (idx >= floorStoneSprites.length) {
            idx = 0;
        }
        return floorStoneSprites[idx];
    }

    public void drawWall(GraphicsContext gc, int screenX, int screenY) {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        gc.setImageSmoothing(false);
        // A placed wall is always the same brick — no per-tile randomness.
        if (wallStoneSprite != null) {
            gc.drawImage(wallStoneSprite, screenX, screenY, tileSize, tileSize);
        } else if (wallSprites != null && wallSprites.length > 0) {
            gc.drawImage(wallSprites[0], screenX, screenY, tileSize, tileSize);
        } else {
            gc.setFill(Color.web("#6b5b4f"));
            gc.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }

    // ---------- CHARACTERS ----------

    public void drawHero(GraphicsContext gc, int screenX, int screenY) {
        drawCharacterSprite(gc, heroSprite, screenX, screenY, Color.web("#4caf50"));
    }

    public void drawHeroByName(GraphicsContext gc, int screenX, int screenY, String name) {
        Image sprite = characterSpriteForName(name);
        drawCharacterSprite(gc, sprite != null ? sprite : heroSprite, screenX, screenY, Color.web("#4caf50"));
    }

    public void drawHeroFacing(GraphicsContext gc, int screenX, int screenY, int facingR, int facingC) {
        drawHeroFacing(gc, screenX, screenY, facingR, facingC, 0);
    }

    /** walkFrame 0 = idle, 1 = alternate walk frame. */
    public void drawHeroFacing(GraphicsContext gc, int screenX, int screenY,
                               int facingR, int facingC, int walkFrame) {
        Image idle = heroSprite != null ? heroSprite : heroSpriteRight;
        Image walk;
        if (facingC < 0) {
            walk = walkFrame == 1 && heroSpriteLeft != null ? heroSpriteLeft : idle;
            drawCharacterSpriteMirrored(gc, walk, screenX, screenY, true, false);
        } else if (facingR < 0) {
            walk = walkFrame == 1 && heroSpriteUp != null ? heroSpriteUp : idle;
            drawCharacterSprite(gc, walk, screenX, screenY, Color.web("#4caf50"));
        } else {
            walk = walkFrame == 1 && heroSpriteRight != null ? heroSpriteRight : idle;
            drawCharacterSprite(gc, walk, screenX, screenY, Color.web("#4caf50"));
        }
    }

    public void drawShadowClone(GraphicsContext gc, int screenX, int screenY) {
        double oldAlpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(0.45);
        gc.setFill(Color.web("#2638ff", 0.35));
        gc.fillRect(screenX, screenY, tileSize, tileSize);
        drawCharacterSprite(gc, heroSprite, screenX, screenY, Color.web("#4f66ff"));
        gc.setGlobalAlpha(oldAlpha);
    }

    public void drawEnemy(GraphicsContext gc, int screenX, int screenY, String enemyType) {
        drawEnemy(gc, screenX, screenY, enemyType, false, null, 0);
    }

    public void drawEnemy(GraphicsContext gc, int screenX, int screenY, String enemyType, boolean mirrorH) {
        drawEnemy(gc, screenX, screenY, enemyType, mirrorH, null, 0);
    }

    public void drawEnemy(GraphicsContext gc, int screenX, int screenY,
            String enemyType, boolean mirrorH, String visualName) {
        drawEnemy(gc, screenX, screenY, enemyType, mirrorH, visualName, 0);
    }

    /** walkFrame 0 = idle, 1 = alternate walk frame. */
    public void drawEnemy(GraphicsContext gc, int screenX, int screenY,
            String enemyType, boolean mirrorH, String visualName, int walkFrame) {
        if ("Knight".equals(enemyType)) {
            Image idle = characterSpriteForName(visualName);
            if (idle == null) idle = knightSprite != null ? knightSprite : knightSprite2;
            Image sprite = (walkFrame == 1 && knightSprite2 != null) ? knightSprite2 : idle;
            if (sprite == null) {
                drawCharacterSprite(gc, null, screenX, screenY, Color.web("#d32f2f"));
                return;
            }
            if (mirrorH) {
                drawCharacterSpriteMirrored(gc, sprite, screenX, screenY, true, false);
            } else {
                drawCharacterSprite(gc, sprite, screenX, screenY, Color.web("#d32f2f"));
            }
        } else if ("Guard".equals(enemyType)) {
            Image sprite = characterSpriteForName(visualName);
            if (sprite == null) sprite = ((screenX + screenY) % 2 == 0) ? guardSprite : guardSprite2;
            drawCharacterSprite(gc, sprite, screenX, screenY, Color.web("#8d6e63"));
        } else if ("Assassin".equals(enemyType)) {
            Image sprite = characterSpriteForName(visualName);
            if (sprite == null) sprite = ((screenX + screenY) % 2 == 0) ? assassinSprite : assassinSprite2;
            drawCharacterSprite(gc, sprite, screenX, screenY, Color.web("#455a64"));
        } else {
            Image sprite = characterSpriteForName(visualName);
            if (sprite == null) sprite = ((screenX + screenY) % 2 == 0) ? sorcererSprite : sorcererSprite2;
            drawCharacterSprite(gc, sprite, screenX, screenY, Color.web("#7b1fa2"));
        }
    }

    private Image characterSpriteForName(String name) {
        if (name == null) {
            return null;
        }
        Image sprite = atlasRegistry.get(name);
        if (sprite != null) {
            return sprite;
        }
        String n = name.toLowerCase();
        if (n.contains("hero")) return heroSprite;
        if (n.contains("knight")) return knightSprite;
        if (n.contains("guard")) return guardSprite;
        if (n.contains("assassin")) return assassinSprite;
        if (n.contains("sorcerer")) return sorcererSprite;
        return null;
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
        if (item == null) {
            return;
        }
        // Catalog/atlas sprites: scale to fit the tile (preserve aspect) so large
        // weapons/items are not clipped or oversized — sit centered with margin.
        Image atlas = atlasRegistry.get(item.getName());
        if (atlas != null) {
            gc.setImageSmoothing(false);
            drawScaledFit(gc, atlas, screenX, screenY, tileSize, tileSize * 0.18);
            return;
        }
        if (item instanceof com.kurawler.model.object.Consumable) {
            com.kurawler.model.object.Consumable cons = (com.kurawler.model.object.Consumable) item;
            String pt = cons.getManaAmount() > 0 ? "ManaPotion"
                    : cons.getEnergyAmount() > 0 ? "EnergyPotion"
                    : "HealthPotion";
            drawConsumable(gc, screenX, screenY, pt);
            return;
        }
        Image sprite = getSpriteForItem(item);
        if (sprite != null) {
            drawScaledFit(gc, sprite, screenX, screenY, tileSize, tileSize * 0.18);
        } else {
            gc.setFill(Color.web("#9c27b0"));
            gc.fillRoundRect(screenX + 6, screenY + 4, tileSize - 12, tileSize - 8, 6, 6);
        }
    }

    public void drawWeaponByName(GraphicsContext gc, int screenX, int screenY, String weaponName) {
        gc.setImageSmoothing(false);
        if (weaponName != null) {
            Image atlas = atlasRegistry.get(weaponName);
            if (atlas != null) {
                drawScaledFit(gc, atlas, screenX, screenY, tileSize, tileSize * 0.18);
                return;
            }
        }
        Image sprite = swordSprite;
        if (weaponName != null) {
            String lower = weaponName.toLowerCase();
            if (lower.contains("axe")) sprite = axeSprite;
            else if (lower.contains("bow")) sprite = bowSprite;
            else if (lower.contains("arrow")) {
                Image arrowImg = atlasRegistry.get("Arrow");
                if (arrowImg != null) {
                    drawScaledFit(gc, arrowImg, screenX, screenY, tileSize, tileSize * 0.18);
                    return;
                }
            } else if (lower.contains("flail ball")) {
                Image flailImg = atlasRegistry.get("Flail Ball");
                if (flailImg != null) {
                    drawScaledFit(gc, flailImg, screenX, screenY, tileSize, tileSize * 0.18);
                    return;
                }
            } else if (lower.contains("bomb")) {
                Image bombImg = atlasRegistry.get("Bomb");
                if (bombImg != null) {
                    drawScaledFit(gc, bombImg, screenX, screenY, tileSize, tileSize * 0.18);
                    return;
                }
            }
            else if (lower.contains("staff") || lower.contains("spell")) sprite = staffSprite;
            else if (lower.contains("dagger") || lower.contains("knife")) sprite = daggerSprite;
            else if (lower.contains("hammer") || lower.contains("mace")) sprite = hammerSprite;
        }
        drawScaledFit(gc, sprite, screenX, screenY, tileSize, tileSize * 0.18);
    }

    // ---------- CONTAINERS ----------

    public void drawContainer(GraphicsContext gc, int screenX, int screenY) {
        drawSprite(gc, chestSprite, screenX, screenY, Color.web("#795548"));
    }

    public void drawContainerByName(GraphicsContext gc, int screenX, int screenY, String name) {
        gc.setImageSmoothing(false);
        Image sprite = spriteForAsset(name);
        if (sprite == null) {
            sprite = chestSprite;
        }
        if (sprite != null && atlasRegistry.containsKey(name)) {
            gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
        } else {
            drawSprite(gc, sprite, screenX, screenY, Color.web("#795548"));
        }
    }


    private boolean isShelfLikeName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return n.contains("bookshelf") || n.contains("wall shelf") || n.contains("shelf");
    }

    /** Padding for catalog sprites so large structures and small items both fit cleanly. */
    private double catalogDrawPadding(String name, Image sprite) {
        if (isShelfLikeName(name)) {
            return 2;
        }
        if (name != null && atlasRegistry.containsKey(name) && sprite != null) {
            double aspect = sprite.getWidth() / Math.max(1.0, sprite.getHeight());
            if (aspect > 1.35 || aspect < 0.72) {
                return 4;
            }
            return tileSize * 0.10;
        }
        return tileSize * 0.08;
    }

    // ---------- STATIC OBJECTS (searchable) ----------

    public void drawSearchable(GraphicsContext gc, int screenX, int screenY, String name) {
        gc.setImageSmoothing(false);
        Image sprite = spriteForAsset(name);
        if (sprite != null) {
            double pad = catalogDrawPadding(name, sprite);
            drawScaledFit(gc, sprite, screenX, screenY, tileSize, pad);
        } else if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("bookshelf") || lower.contains("shelf")) {
                drawBookshelfProp(gc, screenX, screenY);
            } else if (lower.contains("pool") || lower.contains("gargoyle")) {
                drawPoolProp(gc, screenX, screenY);
            } else if (lower.contains("column") || lower.contains("pillar")) {
                drawColumnProp(gc, screenX, screenY);
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
            gc.setImageSmoothing(false);
            drawScaledFit(gc, columnSprite, screenX, screenY, tileSize, tileSize * 0.06);
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
            gc.setImageSmoothing(false);
            drawScaledFit(gc, poolSprite, screenX, screenY, tileSize, tileSize * 0.06);
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
            gc.setImageSmoothing(false);
            drawScaledFit(gc, bookshelfSprite, screenX, screenY, tileSize, 2);
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
        // Labels removed — sparkle corners + "?" badge communicate searchability
    }

    /**
     * Clear door graphic — asset sheet coords looked like a chest/shelf.
     * Gold frame + DOOR label so it is not confused with searchable furniture.
     */

    /** Rotate the GraphicsContext around the tile centre, run {@code draw}, then restore. */
    public void applyRotation(GraphicsContext gc, int screenX, int screenY, int rotation, Runnable draw) {
        if (rotation % 360 == 0) { draw.run(); return; }
        gc.save();
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        gc.translate(cx, cy);
        gc.rotate(rotation);
        gc.translate(-cx, -cy);
        draw.run();
        gc.restore();
    }

    /**
     * Unified portal render: atlas sprite by name first, then door/stair fallback.
     * This handles all MapPortal types — stairs, portals, doors.
     */
    public void drawPortalOrNavSprite(GraphicsContext gc, int screenX, int screenY,
            com.kurawler.model.object.MapPortal portal) {
        String name = portal.getName();
        Image sprite = atlasRegistry.get(name);
        if (sprite != null) {
            gc.setImageSmoothing(false);
            gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
            return;
        }
        // Fallback: stairs → stair shape, door/portal → drawPortal
        if (portal.isStairsMode()) {
            drawNavigationSprite(gc, screenX, screenY, name);
        } else {
            drawPortal(gc, screenX, screenY, portal.isUnlocked(), name);
        }
    }

    /** Draws a navigation object (stairs, portal) using its atlas sprite if available. */
    public void drawNavigationSprite(GraphicsContext gc, int screenX, int screenY, String name) {
        if (name != null) {
            Image sprite = atlasRegistry.get(name);
            if (sprite == null) {
                // fuzzy fallback by keyword
                String n = name.toLowerCase();
                if (n.contains("down")) sprite = atlasRegistry.get("Stairs Down");
                else if (n.contains("up")) sprite = atlasRegistry.get("Stairs Up");
                else if (n.contains("magic")) sprite = atlasRegistry.get("Magic Portal");
                else if (n.contains("portal")) sprite = atlasRegistry.get("Portal");
                else sprite = atlasRegistry.get("Door");
            }
            if (sprite != null) {
                gc.setImageSmoothing(false);
                gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
                return;
            }
        }
        // fallback: draw a simple stair shape
        gc.setFill(Color.web("#4a3c28"));
        gc.fillRect(screenX + 4, screenY + 4, tileSize - 8, tileSize - 8);
        gc.setFill(Color.web("#c8a850"));
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 9));
        gc.fillText("STAIR", screenX + 4, screenY + tileSize - 4);
    }

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
        if (sparkleFrames != null) {
            int frame = (int) ((System.currentTimeMillis() / 350) % sparkleFrames.length);
            Image sparkle = sparkleFrames[frame];
            // Four corner sparkles, scaled small
            int sz = 14;
            gc.drawImage(sparkle, screenX,                  screenY,                  sz, sz);
            gc.drawImage(sparkle, screenX + tileSize - sz,  screenY,                  sz, sz);
            gc.drawImage(sparkle, screenX,                  screenY + tileSize - sz,  sz, sz);
            gc.drawImage(sparkle, screenX + tileSize - sz,  screenY + tileSize - sz,  sz, sz);
        } else {
            // Fallback: subtle thin glow border
            gc.setStroke(Color.web("#ffd54f", 0.45));
            gc.setLineWidth(1.5);
            gc.strokeRect(screenX + 2, screenY + 2, tileSize - 4, tileSize - 4);
        }
        // Small "?" badge in top-right corner
        gc.setFill(Color.web("#0a0908", 0.75));
        gc.fillOval(screenX + tileSize - 12, screenY + 1, 11, 11);
        gc.setFill(Color.web("#ffd54f"));
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 8));
        gc.fillText("?", screenX + tileSize - 9, screenY + 9);
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

    /** Staff / scepter spell — violet crystal burst, distinct from the plain hero bolt. */
    public void drawStaffBoltProjectile(GraphicsContext gc, int screenX, int screenY) {
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        double radius = tileSize * 0.16;
        gc.setFill(Color.web("#7c4dff", 0.35));
        gc.fillOval(cx - radius * 1.8, cy - radius * 1.8, radius * 3.6, radius * 3.6);
        gc.setFill(Color.web("#b388ff", 0.55));
        gc.fillOval(cx - radius * 1.2, cy - radius * 1.2, radius * 2.4, radius * 2.4);
        gc.setFill(Color.web("#e1bee7"));
        gc.fillOval(cx - radius * 0.55, cy - radius * 0.55, radius * 1.1, radius * 1.1);
        gc.setStroke(Color.web("#ea80fc"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    /** Gold-themed arrow shot (dungeon HUD palette), oriented to travel direction. */
    public void drawArrowProjectile(GraphicsContext gc, int screenX, int screenY, int dirRow, int dirCol) {
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        gc.save();
        if (dirCol < 0) {
            gc.translate(cx, cy);
            gc.scale(-1, 1);
            gc.translate(-cx, -cy);
        } else if (dirRow < 0) {
            gc.translate(cx, cy);
            gc.rotate(-90);
            gc.translate(-cx, -cy);
        } else if (dirRow > 0) {
            gc.translate(cx, cy);
            gc.rotate(90);
            gc.translate(-cx, -cy);
        }
        gc.setStroke(Color.web("#8b7530", 0.7));
        gc.setLineWidth(2);
        gc.strokeLine(cx - tileSize * 0.2, cy, cx + tileSize * 0.22, cy);
        gc.setFill(Color.web("#e8c84a"));
        gc.fillPolygon(
                new double[]{cx + tileSize * 0.22, cx + tileSize * 0.08, cx + tileSize * 0.08},
                new double[]{cy, cy - 5, cy + 5},
                3);
        gc.setFill(Color.web("#c8a840", 0.35));
        gc.fillOval(cx - tileSize * 0.15, cy - 4, tileSize * 0.12, 8);
        gc.restore();
    }

    public void drawArrowProjectile(GraphicsContext gc, int screenX, int screenY) {
        drawArrowProjectile(gc, screenX, screenY, 0, 1);
    }

    /** Ember bomb toss effect. */
    public void drawBombProjectile(GraphicsContext gc, int screenX, int screenY) {
        double cx = screenX + tileSize / 2.0;
        double cy = screenY + tileSize / 2.0;
        double r = tileSize * 0.14;
        gc.setFill(Color.web("#ff6b35", 0.35));
        gc.fillOval(cx - r * 1.6, cy - r * 1.6, r * 3.2, r * 3.2);
        gc.setFill(Color.web("#2a1810"));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.setFill(Color.web("#e8c84a"));
        gc.fillOval(cx - r * 0.35, cy - r * 0.55, r * 0.5, r * 0.35);
        gc.setStroke(Color.web("#ff9f43"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
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
        if (item != null) {
            Image atlas = atlasRegistry.get(item.getName());
            if (atlas != null) {
                return atlas;
            }
        }
        if (item instanceof Weapon) {
            String n = item.getName().toLowerCase();
            if (n.contains("axe"))                         return axeSprite;
            if (n.contains("bow"))                         return bowSprite;
            if (n.contains("arrow")) {
                Image arrow = atlasRegistry.get("Arrow");
                return arrow != null ? arrow : bowSprite;
            }
            if (n.contains("flail ball")) {
                Image flail = atlasRegistry.get("Flail Ball");
                return flail != null ? flail : hammerSprite;
            }
            if (n.contains("bomb")) {
                Image bomb = atlasRegistry.get("Bomb");
                return bomb != null ? bomb : gemSprite;
            }
            if (n.contains("staff") || n.contains("spell")) return staffSprite;
            if (n.contains("dagger") || n.contains("knife")) return daggerSprite;
            if (n.contains("hammer") || n.contains("mace")) return hammerSprite;
            return swordSprite;
        }
        if (item instanceof Wearable wearable) {
            if (wearable.isShield()) return shieldSprite;
            if (wearable.isRing()) return ringSprite;
            return armorSprite;
        }
        if (item instanceof Consumable) {
            String n = item.getName().toLowerCase();
            if (n.contains("mana"))   return potionSprite2;
            if (n.contains("energy")) return energyPotionSprite != null ? energyPotionSprite : ringSprite;
            return potionSprite;
        }
        if (item instanceof Scroll) return scrollSprite;
        if (item instanceof Key) {
            String n = item.getName().toLowerCase();
            if (n.contains("gem") || n.contains("crystal") || n.contains("jewel")
                    || n.contains("orb") || n.contains("idol") || n.contains("relic")) {
                return gemSprite;
            }
            return keySprite;
        }
        return gemSprite;
    }

    /**
     * Palette preview for the level editor. Uses the real sprite art for every
     * placeable asset via the shared {@link #spriteForAsset(String)} lookup,
     * preserving aspect ratio so thin sprites (weapons) are not stretched.
     */
    public void drawEditorPaletteIcon(GraphicsContext gc, double size, String objectName) {
        if (gc == null || objectName == null) {
            return;
        }
        int s = (int) size;
        gc.setImageSmoothing(false);
        gc.clearRect(0, 0, s, s);
        gc.setFill(Color.web("#141428"));
        gc.fillRect(0, 0, s, s);

        if ("Wall".equals(objectName)) {
            Image w = wallStoneSprite != null ? wallStoneSprite
                    : (wallSprites != null && wallSprites.length > 0 ? wallSprites[0] : null);
            if (w != null) {
                gc.drawImage(w, 1, 1, s - 2, s - 2);
                return;
            }
        }
        if ("Floor".equals(objectName)) {
            Image f = (floorStoneSprites != null ? floorStoneSprites[0] : null);
            if (f == null && floorSprites != null && floorSprites.length > 0) {
                f = floorSprites[0];
            }
            if (f != null) {
                gc.drawImage(f, 1, 1, s - 2, s - 2);
                return;
            }
        }
        if ("Hero".equals(objectName)) {
            drawCharacterPreview(gc, heroSprite, s);
            return;
        }

        if ("Erase".equals(objectName)) {
            gc.setFill(Color.web("#3a1a1a"));
            gc.fillRoundRect(2, 2, s - 4, s - 4, 5, 5);
            gc.setStroke(Color.web("#ff6b6b"));
            gc.setLineWidth(2);
            gc.strokeLine(7, 7, s - 7, s - 7);
            gc.strokeLine(s - 7, 7, 7, s - 7);
            return;
        }

        Image wall = wallSpriteFor(objectName);
        if (wall != null) {
            if (isResourceCellTile(objectName)) {
                gc.setFill(Color.web("#4a2736"));
                gc.fillRect(0, 0, s, s);
                gc.drawImage(wall, 0, 0, s, s);
                return;
            }
            drawScaledFit(gc, wall, 0, 0, s, atlasRegistry.containsKey(objectName) ? 2 : 1);
            return;
        }
        Image floor = floorSpriteFor(objectName);
        if (floor != null) {
            if (isResourceCellTile(objectName)) {
                gc.setFill(Color.web("#4a2736"));
                gc.fillRect(0, 0, s, s);
                gc.drawImage(floor, 0, 0, s, s);
                return;
            }
            drawScaledFit(gc, floor, 0, 0, s, atlasRegistry.containsKey(objectName) ? 2 : 1);
            return;
        }

        Image sprite = spriteForAsset(objectName);
        if (sprite != null) {
            drawScaledFit(gc, sprite, 0, 0, s, palettePaddingFor(objectName));
            return;
        }

        // Character previews
        switch (objectName) {
            case "Adventurer":
                drawCharacterPreview(gc, heroSprite, s);
                return;
            case "Knight":
                drawCharacterPreview(gc, knightSprite, s);
                return;
            case "Sorcerer":
                drawCharacterPreview(gc, sorcererSprite, s);
                return;
            default:
                break;
        }

        gc.setFill(Color.web("#553355"));
        gc.fillRoundRect(4, 4, s - 8, s - 8, 4, 4);
    }

    /** Small icons (weapons/items) get a little breathing room; props fill the chip. */
    private double palettePaddingFor(String name) {
        if (name == null) {
            return 3;
        }
        String n = name.toLowerCase();
        if (n.contains("ring") || n.contains("gem") || n.contains("key")
                || n.contains("dagger") || n.contains("scroll")) {
            return 5;
        }
        if (n.contains("sword") || n.contains("axe") || n.contains("bow")
                || n.contains("staff") || n.contains("hammer") || n.contains("potion")
                || n.contains("elixir") || n.contains("apple")) {
            return 4;
        }
        return 2;
    }

    /**
     * Single source of truth mapping a placeable asset id/name to its sprite.
     * Used by both palette previews and in-editor map rendering so they always match.
     */
    public Image spriteForAsset(String id) {
        if (id == null) {
            return null;
        }
        Image atlas = atlasRegistry.get(id);
        if (atlas != null) {
            return atlas;
        }
        String n = id.toLowerCase();

        // --- Characters ---
        if (n.contains("hero")) return heroSprite;
        if (n.contains("knight")) return knightSprite;
        if (n.contains("guard")) return guardSprite;
        if (n.contains("assassin")) return assassinSprite != null ? assassinSprite : heroSprite;
        if (n.contains("sorcerer")) return sorcererSprite;

        // --- Items / weapons (resolved before statics to avoid keyword clashes) ---
        if (n.contains("spell book") || n.contains("scroll") || n.contains("tome")) return scrollSprite;
        if (n.contains("dagger") || n.contains("knife")) return daggerSprite;
        if (n.contains("axe")) return axeSprite;
        if (n.contains("bow")) return bowSprite;
        if (n.contains("staff") || n.contains("wand")) return staffSprite;
        if (n.contains("hammer") || n.contains("mace")) return hammerSprite;
        if (n.contains("sword") || n.contains("blade")) return swordSprite;
        if (n.contains("shield")) return shieldSprite;
        if (n.contains("armor") || n.contains("mail") || n.contains("plate")) return armorSprite;
        if (n.contains("ring") || n.contains("amulet")) return ringSprite;
        if (n.contains("gem") || n.contains("crystal") || n.contains("jewel")) return gemSprite;
        if (n.contains("mana")) return potionSprite2;
        if (n.contains("energy")) return energyPotionSprite != null ? energyPotionSprite : potionSprite;
        if (n.contains("potion") || n.contains("elixir") || n.contains("apple") || n.contains("flask")) return potionSprite;

        // --- Containers ---
        if (n.contains("chest")) {
            if (n.contains("open")) return chestOpenSprite;
            if (n.contains("gold") || n.contains("stone") || n.contains("locked")) {
                return goldChestSprite != null ? goldChestSprite : chestSprite;
            }
            return chestSprite;
        }
        if (n.contains("barrel")) return barrelSprite;
        if (n.contains("crate")) return crateSprite;
        if (n.contains("pouch") || n.contains("sack")) return crateSprite;

        // --- Statics ---
        if (n.contains("stairs") && n.contains("down")) return stairsDownSprite;
        if (n.contains("stairs") && n.contains("up")) return stairsUpSprite;
        if (n.contains("open door")) return doorOpenSprite;
        if (n.contains("door")) return doorSprite;
        if (n.contains("sarcophagus")) return sarcophagusSprite;
        if (n.contains("altar")) return sarcophagusSprite != null ? sarcophagusSprite : pedestalSprite;
        if (n.contains("pedestal")) return pedestalSprite;
        if (n.contains("statue")) return statueSprite;
        if (n.contains("fountain")) return fountainSprite;
        if (n.contains("lantern") || n.contains("lamp") || n.contains("torch") || n.contains("sconce")) return lanternSprite;
        if (n.contains("pool") || n.contains("gargoyle")) return poolSprite;
        if (n.contains("bookshelf") || n.contains("wall shelf") || n.contains("shelf")) return bookshelfSprite;
        if (n.contains("column") || n.contains("pillar")) return columnSprite;

        // --- Misc keys / loot ---
        if (n.contains("gold")) return goldPileSprite != null ? goldPileSprite : gemSprite;
        if (n.contains("key")) return keySprite;
        return null;
    }

    private Image wallSpriteFor(String name) {
        if (name == null) {
            return null;
        }
        Image reg = atlasRegistry.get(name);
        if (reg != null) {
            return reg;
        }
        String n = name.toLowerCase();
        if (n.contains("brown wall")) return wallBrownSprite;
        if (n.contains("dark wall")) return wallDarkSprite;
        if (n.equals("wall") || n.contains("stone wall") || n.contains("brick")) return wallStoneSprite;
        return null;
    }

    private Image floorSpriteFor(String name) {
        if (name == null) {
            return null;
        }
        String n = name.toLowerCase();
        // Named floor tiles are registered individually in the catalog.
        if (n.contains("floor") || n.contains("cobble") || n.contains("gravel")
                || n.contains("rubble") || n.contains("pebble")) {
            Image reg = atlasRegistry.get(name);
            if (reg != null) return reg;
        }
        if (floorStoneSprites == null) {
            return null;
        }
        if (n.contains("crack")) return floorStoneSprites[1];
        if (n.contains("cobble") || n.contains("moss")) return floorStoneSprites[2];
        if (n.contains("stone floor")) return floorStoneSprites[0];
        return null;
    }

    /** Aspect-preserving, centered draw within a square box (nearest-neighbour friendly). */
    private void drawScaledFit(GraphicsContext gc, Image img, double x, double y, double box, double pad) {
        if (img == null) {
            return;
        }
        gc.setImageSmoothing(false);
        double avail = box - pad * 2;
        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw <= 0 || ih <= 0) {
            return;
        }
        double scale = avail / Math.max(iw, ih);
        double w = iw * scale;
        double h = ih * scale;
        double dx = x + (box - w) / 2.0;
        double dy = y + (box - h) / 2.0;
        gc.drawImage(img, dx, dy, w, h);
    }

    /** Wall tile using a named DCSS variant when available, else the shared dungeon wall. */
    public void drawWallByName(GraphicsContext gc, int screenX, int screenY, String name) {
        Image w = wallSpriteFor(name);
        if (w != null) {
            gc.setImageSmoothing(false);
            if (isResourceCellTile(name)) {
                drawPatternedFloor(gc, screenX, screenY);
                gc.drawImage(w, screenX, screenY, tileSize, tileSize);
            } else if (atlasRegistry.containsKey(name)) {
                gc.setFill(Color.web("#1a1a2e"));
                gc.fillRect(screenX, screenY, tileSize, tileSize);
                drawScaledFit(gc, w, screenX, screenY, tileSize, 1);
            } else {
                gc.setFill(Color.web("#1a1a2e"));
                gc.fillRect(screenX, screenY, tileSize, tileSize);
                gc.drawImage(w, screenX, screenY, tileSize, tileSize);
            }
        } else {
            drawWall(gc, screenX, screenY);
        }
    }

    /** Floor tile using a named DCSS variant when available, else the shared dungeon floor. */
    public void drawFloorByName(GraphicsContext gc, int screenX, int screenY, String name) {
        String n = name == null ? "" : name.toLowerCase();
        // The ubiquitous base floor uses the designed flagstone pattern; named
        // variants (cracked, cobblestone, mossy...) keep their own sprite.
        if (n.isEmpty() || n.equals("floor") || n.contains("stone floor")) {
            drawPatternedFloor(gc, screenX, screenY);
            return;
        }
        Image f = floorSpriteFor(name);
        if (f != null) {
            gc.setImageSmoothing(false);
            if (isResourceCellTile(name)) {
                drawPatternedFloor(gc, screenX, screenY);
                gc.drawImage(f, screenX, screenY, tileSize, tileSize);
            } else if (atlasRegistry.containsKey(name)) {
                gc.setFill(Color.web("#1a1a2e"));
                gc.fillRect(screenX, screenY, tileSize, tileSize);
                drawScaledFit(gc, f, screenX, screenY, tileSize, 1);
            } else {
                gc.setFill(Color.web("#1a1a2e"));
                gc.fillRect(screenX, screenY, tileSize, tileSize);
                gc.drawImage(f, screenX, screenY, tileSize, tileSize);
            }
        } else {
            drawFloor(gc, screenX, screenY);
        }
    }

    /**
     * Draws a decorative / portal static (door, stairs, statue, fountain, pedestal,
     * altar, column, lantern...) using its real sprite, plus the searchable marker
     * when applicable. Floor must already be drawn underneath.
     */
    public void drawStaticDecor(GraphicsContext gc, int screenX, int screenY,
            com.kurawler.model.object.StaticObject so) {
        if (so == null) {
            return;
        }
        Image sprite = spriteForAsset(so.getName());
        if (sprite != null) {
            gc.setImageSmoothing(false);
            double oldAlpha = gc.getGlobalAlpha();
            if (so.wasSearched()) {
                gc.setGlobalAlpha(0.62);
            }
            String propName = so.getName();
            if (isResourceCellTile(propName)) {
                gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
            } else {
                double pad = catalogDrawPadding(propName, sprite);
                boolean wide = Math.abs(sprite.getWidth() - sprite.getHeight()) > 1;
                if (wide || atlasRegistry.containsKey(propName)) {
                    drawScaledFit(gc, sprite, screenX, screenY, tileSize, pad);
                } else {
                    gc.drawImage(sprite, screenX, screenY, tileSize, tileSize);
                }
            }
            gc.setGlobalAlpha(oldAlpha);
        } else if (!so.isPassable()) {
            // Unknown solid prop — fall back to a column-like pillar.
            drawColumnProp(gc, screenX, screenY);
        }
        if (so.isSearchable()) {
            drawSearchableMarker(gc, screenX, screenY);
        }
    }

    /** True when a static is just plain dungeon wall terrain. */
    public boolean isWallName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return n.equals("wall") || n.contains("dark wall") || n.contains("brown wall")
                || n.contains("vine wall") || n.contains("grate") || n.contains("caged wall")
                || n.contains("ring wall") || n.contains("torch wall");
    }

    /** True when a static is just plain dungeon floor terrain. */
    public boolean isFloorName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return n.contains("floor") || n.contains("cobble") || n.contains("gravel")
                || n.contains("rubble") || n.contains("pebble");
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

    private void drawCharacterSprite(GraphicsContext gc, Image sprite, int sx, int sy, Color fallback) {
        if (sprite == null) {
            gc.setFill(fallback);
            gc.fillRoundRect(sx + tileSize * 0.30, sy + tileSize * 0.18,
                    tileSize * 0.40, tileSize * 0.68, 6, 6);
            return;
        }
        gc.setImageSmoothing(false);
        drawBottomAlignedFit(gc, sprite, sx, sy, tileSize, tileSize * 0.06, tileSize * 0.08);
    }

    private void drawCharacterSpriteMirrored(GraphicsContext gc, Image sprite, int sx, int sy,
            boolean flipH, boolean flipV) {
        if (sprite == null) {
            return;
        }
        if (!flipH && !flipV) {
            drawCharacterSprite(gc, sprite, sx, sy, Color.TRANSPARENT);
            return;
        }
        double cx = sx + tileSize / 2.0;
        double cy = sy + tileSize / 2.0;
        gc.save();
        gc.translate(cx, cy);
        gc.scale(flipH ? -1 : 1, flipV ? -1 : 1);
        gc.translate(-cx, -cy);
        drawCharacterSprite(gc, sprite, sx, sy, Color.TRANSPARENT);
        gc.restore();
    }

    private void drawCharacterPreview(GraphicsContext gc, Image sprite, double box) {
        if (sprite == null) {
            return;
        }
        gc.setImageSmoothing(false);
        drawBottomAlignedFit(gc, sprite, 0, 0, box, box * 0.10, box * 0.10);
    }

    private void drawBottomAlignedFit(GraphicsContext gc, Image img, double x, double y,
            double box, double sidePad, double bottomPad) {
        if (img == null) {
            return;
        }
        int[] bounds = SpriteAtlas.opaqueBounds(img);
        if (bounds == null) {
            return;
        }
        double sx = bounds[0];
        double sy = bounds[1];
        double iw = bounds[2] - bounds[0] + 1;
        double ih = bounds[3] - bounds[1] + 1;
        double targetH = box - bottomPad - sidePad;
        double maxW = box - sidePad * 2;
        double scale = Math.min(targetH / ih, maxW / iw);
        double w = iw * scale;
        double h = ih * scale;
        double dx = x + (box - w) / 2.0;
        double dy = y + box - bottomPad - h;
        gc.drawImage(img, sx, sy, iw, ih, dx, dy, w, h);
    }

    private void drawSpriteMirrored(GraphicsContext gc, Image sprite, int sx, int sy,
                                    boolean flipH, boolean flipV) {
        if (sprite == null) {
            return;
        }
        if (!flipH && !flipV) {
            gc.drawImage(sprite, sx, sy, tileSize, tileSize);
            return;
        }
        double cx = sx + tileSize / 2.0;
        double cy = sy + tileSize / 2.0;
        gc.save();
        gc.translate(cx, cy);
        gc.scale(flipH ? -1 : 1, flipV ? -1 : 1);
        gc.translate(-cx, -cy);
        gc.drawImage(sprite, sx, sy, tileSize, tileSize);
        gc.restore();
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
