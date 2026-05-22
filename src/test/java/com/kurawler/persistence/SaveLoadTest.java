package com.kurawler.persistence;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.Weapon;
import com.kurawler.model.object.Wearable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the Save / Load Game feature (Issue #37).
 *
 * Phase 2 spec requirements tested here:
 *   - JSON serialization produces a valid, complete save file
 *   - Loading restores the full game state (map, hero, enemies, mode, timer, objective)
 *   - Multiple save files are independent and do not interfere with each other
 *   - Each save embeds a player-given name and timestamp for the load screen
 *     (spec p.15: "displays a list of previous saves with their given name
 *      and their date/time of save")
 *
 * Tests marked with a comment "WILL FAIL" expose missing spec features and
 * are intentional — they define what still needs to be implemented.
 */
class SaveLoadTest {

    @TempDir
    Path tempDir;

    private MapSerializer serializer;
    private MapDeserializer deserializer;

    @BeforeEach
    void setUp() {
        serializer  = new MapSerializer();
        deserializer = new MapDeserializer();
    }

    private String path(String name) {
        return tempDir.resolve(name + ".json").toString();
    }

    // ── 1. JSON SERIALIZATION TESTS ──────────────────────────────────────────

    @Nested
    class JsonSerialization {

        @Test
        void saveReturnsTrueAndCreatesNonEmptyFile() {
            GameSession session = MapFactory.createDefaultMap();
            session.setCurrentMode("Play");
            String p = path("save");

            assertTrue(serializer.saveGameState(session, p),
                "saveGameState must return true on success");
            assertTrue(Files.exists(Paths.get(p)), "Save file must exist on disk");
            assertTrue(Paths.get(p).toFile().length() > 0, "Save file must not be empty");
        }

        @Test
        void savedJsonContainsMapDimensions() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            serializer.saveGameState(session, path("dims"));
            String json = Files.readString(Paths.get(path("dims")));

            assertTrue(json.contains("\"width\""),  "JSON must contain map width");
            assertTrue(json.contains("\"height\""), "JSON must contain map height");
            assertTrue(json.contains("\"cells\""),  "JSON must contain cells array");
        }

        @Test
        void savedJsonContainsHeroStatFields() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            session.getHero().setHealth(9);
            session.getHero().setMana(40);
            session.getHero().setEnergy(55);
            serializer.saveGameState(session, path("hero"));
            String json = Files.readString(Paths.get(path("hero")));

            assertTrue(json.contains("\"hp\""),     "JSON must contain hp field");
            assertTrue(json.contains("\"mana\""),   "JSON must contain mana field");
            assertTrue(json.contains("\"energy\""), "JSON must contain energy field");
            assertTrue(json.contains("\"str\""),    "JSON must contain str field");
            assertTrue(json.contains("\"def\""),    "JSON must contain def field");
            assertTrue(json.contains("\"row\""),    "JSON must contain row field");
            assertTrue(json.contains("\"col\""),    "JSON must contain col field");
        }

        @Test
        void savedJsonContainsInventoryItems() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            session.getHero().getInventory().addItem(new Weapon("Iron Sword", "blade", 8));
            session.getHero().getInventory().addItem(new Consumable("Health Potion", "heals", 10));
            serializer.saveGameState(session, path("inv"));
            String json = Files.readString(Paths.get(path("inv")));

            assertTrue(json.contains("\"inventory\""), "JSON must contain inventory array");
            assertTrue(json.contains("Iron Sword"),    "JSON must contain weapon name");
            assertTrue(json.contains("Health Potion"), "JSON must contain consumable name");
        }

        @Test
        void savedJsonContainsGameMode() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            session.setCurrentMode("Play");
            serializer.saveGameState(session, path("mode"));
            String json = Files.readString(Paths.get(path("mode")));

            assertTrue(json.contains("\"currentMode\""), "JSON must contain currentMode");
            assertTrue(json.contains("\"Play\""),        "JSON must contain mode value");
        }

        @Test
        void savedJsonContainsPlayTimer() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            session.setPlayDurationTimer(120);
            serializer.saveGameState(session, path("timer"));
            String json = Files.readString(Paths.get(path("timer")));

            assertTrue(json.contains("\"playDurationTimer\""), "JSON must contain playDurationTimer");
            assertTrue(json.contains("120"),                   "JSON must contain the timer value");
        }

        @Test
        void savedJsonContainsTargetObjective() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            session.setTargetObjective(new Key("Crystal Key", "The objective", "crystal_01"));
            serializer.saveGameState(session, path("obj"));
            String json = Files.readString(Paths.get(path("obj")));

            assertTrue(json.contains("\"targetObjective\""), "JSON must contain targetObjective");
            assertTrue(json.contains("Crystal Key"),         "JSON must contain objective name");
        }

        @Test
        void savedJsonContainsLivingEnemies() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            Knight knight = new Knight();
            GridCell cell = session.getMap().getCell(2, 2);
            if (cell != null && !cell.isBlocked()) {
                knight.moveToCell(cell);
                session.addEnemy(knight);
            }
            serializer.saveGameState(session, path("enemies"));
            String json = Files.readString(Paths.get(path("enemies")));

            assertTrue(json.contains("\"enemies\""), "JSON must contain enemies array");
            assertTrue(json.contains("\"Knight\""),  "JSON must contain enemy type");
        }

        // WILL FAIL — spec p.15: "each save should be able to be named individually
        // by the player" and the load screen shows "their given name and their
        // date/time of save". The serializer must embed both fields in the JSON so
        // the load screen does not rely on parsing filenames.
        @Test
        void savedJsonContainsSaveNameField() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            serializer.saveGameState(session, path("named"));
            String json = Files.readString(Paths.get(path("named")));

            assertTrue(json.contains("\"saveName\""),
                "JSON must embed a saveName field — spec requires player-named saves");
        }

        // WILL FAIL — same spec requirement: load screen must display date/time.
        @Test
        void savedJsonContainsTimestampField() throws IOException {
            GameSession session = MapFactory.createDefaultMap();
            serializer.saveGameState(session, path("ts"));
            String json = Files.readString(Paths.get(path("ts")));

            assertTrue(json.contains("\"savedAt\""),
                "JSON must embed a savedAt timestamp — spec requires date/time on load screen");
        }
    }

    // ── 2. STATE RESTORE TESTS ───────────────────────────────────────────────

    @Nested
    class StateRestore {

        @Test
        void heroPositionIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            GridCell target = before.getMap().getCell(2, 3);
            if (target != null && !target.isBlocked()) {
                before.getHero().moveToCell(target);
            }
            int savedRow = before.getHero().getRow();
            int savedCol = before.getHero().getCol();
            serializer.saveGameState(before, path("hero_pos"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("hero_pos"), after);

            assertEquals(savedRow, after.getHero().getRow(), "Hero row must be restored");
            assertEquals(savedCol, after.getHero().getCol(), "Hero col must be restored");
        }

        @Test
        void heroStatsAreRestored() {
            GameSession before = MapFactory.createDefaultMap();
            Hero hero = before.getHero();
            hero.setHealth(9);
            hero.setMana(40);
            hero.setEnergy(55);
            hero.setStrength(12);
            hero.setDefensePower(4);
            serializer.saveGameState(before, path("stats"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("stats"), after);
            Hero restored = after.getHero();

            assertEquals(9,  restored.getHealth(),      "HP must be restored");
            assertEquals(40, restored.getMana(),         "Mana must be restored");
            assertEquals(55, restored.getEnergy(),       "Energy must be restored");
            assertEquals(12, restored.getStrength(),     "STR must be restored");
            assertEquals(4,  restored.getDefensePower(), "DEF must be restored");
        }

        @Test
        void heroInventoryIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            before.getHero().getInventory().addItem(new Weapon("War Axe", "heavy", 12));
            before.getHero().getInventory().addItem(new Consumable("Health Potion", "heals", 10));
            before.getHero().getInventory().addItem(new Key("Gold Key", "opens door", "gold_01"));
            serializer.saveGameState(before, path("inv_restore"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("inv_restore"), after);

            assertEquals(3, after.getHero().getInventory().getItemCount(),
                "All 3 items must be in inventory after load");
            assertTrue(after.getHero().getInventory().getItems().stream()
                .anyMatch(i -> "War Axe".equals(i.getName())),      "Weapon must be restored");
            assertTrue(after.getHero().getInventory().getItems().stream()
                .anyMatch(i -> "Health Potion".equals(i.getName())), "Consumable must be restored");
            assertTrue(after.getHero().getInventory().getItems().stream()
                .anyMatch(i -> "Gold Key".equals(i.getName())),      "Key must be restored");
        }

        @Test
        void heroEquippedWeaponIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            Weapon sword = new Weapon("Iron Sword", "blade", 8);
            before.getHero().getInventory().addItem(sword);
            before.getHero().setEquippedWeapon(sword);
            serializer.saveGameState(before, path("wpn"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("wpn"), after);

            assertNotNull(after.getHero().getEquippedWeapon(),
                "Equipped weapon must be non-null after load");
            assertEquals("Iron Sword", after.getHero().getEquippedWeapon().getName(),
                "Equipped weapon name must match");
        }

        @Test
        void heroEquippedWearableIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            Wearable armor = new Wearable("Iron Armor", "sturdy", 3);
            before.getHero().getInventory().addItem(armor);
            before.getHero().equipWearable(armor);
            serializer.saveGameState(before, path("armor"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("armor"), after);

            assertNotNull(after.getHero().getEquippedWearable(),
                "Equipped wearable must be non-null after load");
            assertEquals("Iron Armor", after.getHero().getEquippedWearable().getName(),
                "Equipped wearable name must match");
        }

        @Test
        void mapWallsAreRestored() {
            GameSession before = MapFactory.createDefaultMap();
            // (0,0) is always a wall in the default map
            assertNotNull(before.getMap().getCell(0, 0).getStaticObject(),
                "Pre-condition: (0,0) must have a StaticObject");
            assertFalse(before.getMap().getCell(0, 0).getStaticObject().isPassable(),
                "Pre-condition: (0,0) must be impassable");
            serializer.saveGameState(before, path("walls"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("walls"), after);

            assertNotNull(after.getMap().getCell(0, 0).getStaticObject(),
                "Wall StaticObject must be present after load");
            assertFalse(after.getMap().getCell(0, 0).getStaticObject().isPassable(),
                "Wall must remain impassable after load");
        }

        @Test
        void floorItemsAreRestored() {
            GameSession before = MapFactory.createDefaultMap();
            // (2,2) is inside the first carved room — guaranteed to be floor
            GridCell floorCell = before.getMap().getCell(2, 2);
            floorCell.addObject(new Weapon("War Axe", "heavy", 12));
            serializer.saveGameState(before, path("floor"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("floor"), after);

            GridCell restored = after.getMap().getCell(2, 2);
            assertTrue(restored.hasObjects(), "Floor cell must have objects after load");
            assertTrue(restored.getObjects().stream()
                .anyMatch(o -> "War Axe".equals(o.getName())),
                "Specific floor item must be on its correct cell after load");
        }

        @Test
        void enemiesAreRestored() {
            GameSession before = MapFactory.createDefaultMap();
            Knight knight = new Knight();
            knight.setHealth(15);
            GridCell cell = before.getMap().getCell(4, 4);
            if (cell != null && !cell.isBlocked()) {
                knight.moveToCell(cell);
                before.addEnemy(knight);
            }
            serializer.saveGameState(before, path("enemies_r"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("enemies_r"), after);

            assertEquals(1,  after.getEnemies().size(),              "Enemy count must be restored");
            assertEquals(15, after.getEnemies().get(0).getHealth(),  "Enemy HP must be restored");
            assertEquals(4,  after.getEnemies().get(0).getRow(),     "Enemy row must be restored");
            assertEquals(4,  after.getEnemies().get(0).getCol(),     "Enemy col must be restored");
        }

        @Test
        void gameModeIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            before.setCurrentMode("Play");
            serializer.saveGameState(before, path("mode_r"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("mode_r"), after);

            assertEquals("Play", after.getCurrentMode(), "Game mode must be restored");
        }

        @Test
        void playTimerIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            before.setPlayDurationTimer(120);
            serializer.saveGameState(before, path("timer_r"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("timer_r"), after);

            assertEquals(120, after.getPlayDurationTimer(), "Play timer must be restored");
        }

        @Test
        void targetObjectiveIsRestored() {
            GameSession before = MapFactory.createDefaultMap();
            before.setTargetObjective(new Key("Crystal Key", "objective", "crystal_01"));
            serializer.saveGameState(before, path("obj_r"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("obj_r"), after);

            assertNotNull(after.getTargetObjective(), "Target objective must not be null after load");
            assertEquals("Crystal Key", after.getTargetObjective().getName(),
                "Objective name must match after load");
        }

        @Test
        void heroIsRegisteredAsOccupantOfItsCell() {
            GameSession before = MapFactory.createDefaultMap();
            int row = before.getHero().getRow();
            int col = before.getHero().getCol();
            serializer.saveGameState(before, path("cell_link"));

            GameSession after = new GameSession();
            deserializer.loadGameState(path("cell_link"), after);

            assertSame(after.getHero(),
                after.getMap().getCell(row, col).getOccupyingCharacter(),
                "Hero must be the occupyingCharacter of its own cell after load");
        }
    }

    // ── 3. MULTIPLE SAVE FILES TESTS ─────────────────────────────────────────

    @Nested
    class MultipleSaveFiles {

        @Test
        void savingToTwoDifferentPathsCreatesTwoFiles() {
            GameSession s1 = MapFactory.createDefaultMap();
            GameSession s2 = MapFactory.createDefaultMap();

            assertTrue(serializer.saveGameState(s1, path("slot_1")));
            assertTrue(serializer.saveGameState(s2, path("slot_2")));

            assertTrue(Files.exists(Paths.get(path("slot_1"))), "Slot 1 must exist");
            assertTrue(Files.exists(Paths.get(path("slot_2"))), "Slot 2 must exist");
        }

        @Test
        void eachSaveFilePreservesItsOwnState() {
            GameSession s1 = MapFactory.createDefaultMap();
            s1.getHero().setHealth(17);
            GameSession s2 = MapFactory.createDefaultMap();
            s2.getHero().setHealth(5);
            serializer.saveGameState(s1, path("slot_1"));
            serializer.saveGameState(s2, path("slot_2"));

            GameSession loaded1 = new GameSession();
            GameSession loaded2 = new GameSession();
            deserializer.loadGameState(path("slot_1"), loaded1);
            deserializer.loadGameState(path("slot_2"), loaded2);

            assertEquals(17, loaded1.getHero().getHealth(), "Slot 1 must restore HP=17");
            assertEquals(5,  loaded2.getHero().getHealth(), "Slot 2 must restore HP=5");
        }

        @Test
        void loadingOneSaveDoesNotModifyOtherSaveFile() throws IOException {
            GameSession s1 = MapFactory.createDefaultMap();
            s1.getHero().setHealth(17);
            GameSession s2 = MapFactory.createDefaultMap();
            s2.getHero().setHealth(3);
            serializer.saveGameState(s1, path("slot_a"));
            serializer.saveGameState(s2, path("slot_b"));

            long originalSize = Files.size(Paths.get(path("slot_b")));

            deserializer.loadGameState(path("slot_a"), new GameSession());
            deserializer.loadGameState(path("slot_a"), new GameSession());

            assertEquals(originalSize, Files.size(Paths.get(path("slot_b"))),
                "Loading slot A must not change slot B on disk");
            GameSession reloaded = new GameSession();
            deserializer.loadGameState(path("slot_b"), reloaded);
            assertEquals(3, reloaded.getHero().getHealth(),
                "Slot B hero HP must still be 3 after loading slot A");
        }

        @Test
        void loadNonExistentFileReturnsFalse() {
            boolean result = deserializer.loadGameState(
                tempDir.resolve("no_such_file.json").toString(), new GameSession());
            assertFalse(result, "Loading a missing file must return false");
        }

        // WILL FAIL — spec p.15: the load screen must list saves with their
        // player-given name. Each JSON must embed "saveName" so the UI can
        // display it independently of the filename.
        @Test
        void eachSaveEmbedsSaveNameSoLoadScreenCanDisplayIt() throws IOException {
            GameSession s1 = MapFactory.createDefaultMap();
            GameSession s2 = MapFactory.createDefaultMap();
            serializer.saveGameState(s1, path("slot_1"));
            serializer.saveGameState(s2, path("slot_2"));

            String json1 = Files.readString(Paths.get(path("slot_1")));
            String json2 = Files.readString(Paths.get(path("slot_2")));

            assertTrue(json1.contains("\"saveName\""),
                "slot_1 JSON must embed saveName for load screen display");
            assertTrue(json2.contains("\"saveName\""),
                "slot_2 JSON must embed saveName for load screen display");
        }

        // WILL FAIL — spec p.15: the load screen must show date/time of save.
        @Test
        void eachSaveEmbedsTimestampSoLoadScreenCanDisplayIt() throws IOException {
            GameSession s1 = MapFactory.createDefaultMap();
            GameSession s2 = MapFactory.createDefaultMap();
            serializer.saveGameState(s1, path("slot_1"));
            serializer.saveGameState(s2, path("slot_2"));

            String json1 = Files.readString(Paths.get(path("slot_1")));
            String json2 = Files.readString(Paths.get(path("slot_2")));

            assertTrue(json1.contains("\"savedAt\""),
                "slot_1 JSON must embed savedAt timestamp for load screen");
            assertTrue(json2.contains("\"savedAt\""),
                "slot_2 JSON must embed savedAt timestamp for load screen");
        }
    }
}
