package com.kurawler.system;

import com.kurawler.action.AttackAction;
import com.kurawler.action.BreakAction;
import com.kurawler.action.EquipAction;
import com.kurawler.editor.LevelEditor;
import com.kurawler.editor.RandomItemGenerator;
import com.kurawler.engine.EnemySpawner;
import com.kurawler.engine.GameEngine;
import com.kurawler.model.object.Ring;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import com.kurawler.action.DiscardAction;
import com.kurawler.action.EatAction;
import com.kurawler.action.MoveAction;
import com.kurawler.action.ReadAction;
import com.kurawler.action.SearchAction;
import com.kurawler.action.TakeAction;
import com.kurawler.action.TakeFromContainerAction;
import com.kurawler.action.WearAction;
import com.kurawler.controller.GameController;
import com.kurawler.engine.CombatResolver;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Projectile;
import com.kurawler.model.object.Consumable;
import com.kurawler.model.object.Key;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.PortableContainer;
import com.kurawler.model.object.Scroll;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;
import com.kurawler.model.object.Wearable;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System (integration) tests for the Kurawler game engine.
 *
 * Each test exercises a full cross-layer scenario:
 *   Action → resolve() → Effect → apply(session)
 * No mocking — real objects only.
 */
class GameSystemTest {

    private static final int ROWS = 7;
    private static final int COLS = 7;

    private Map map;
    private Hero hero;
    private GameSession session;
    private GameController controller;

    @BeforeEach
    void setUp() {
        map = new Map(COLS, ROWS);
        hero = new Hero("Ikbal");
        session = new GameSession(map);
        session.setHero(hero);
        controller = new GameController(session);

        // Place hero at (3,3) — centre of the 7×7 map
        hero.moveToCell(map.getCell(3, 3));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Game session initialisation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class SessionInit {

        @Test
        void freshSession_defaultModeIsDesign() {
            GameSession fresh = new GameSession(map);
            assertEquals("Design", fresh.getCurrentMode());
        }

        @Test
        void freshSession_turnCountIsZero() {
            assertEquals(0, session.getTurnCount());
        }

        @Test
        void freshSession_heroIsAlive() {
            assertTrue(hero.isAlive());
        }

        @Test
        void freshSession_isGameOver_returnsFalse() {
            assertFalse(session.isGameOver());
        }

        @Test
        void startGame_switchesModeTo_Play() {
            controller.startNewGame();
            assertEquals("Play", session.getCurrentMode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Hero movement through the Action–Effect pipeline
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class HeroMovementPipeline {

        @Test
        void moveAction_movedHeroToCorrectCell() {
            controller.executeAction(new MoveAction(-1, 0)); // UP
            assertEquals(2, hero.getRow());
            assertEquals(3, hero.getCol());
        }

        @Test
        void moveAction_advancesTurnCount() {
            controller.executeAction(new MoveAction(0, 1)); // RIGHT
            assertEquals(1, session.getTurnCount());
        }

        @Test
        void consecutiveMoves_updatePositionCorrectly() {
            controller.executeAction(new MoveAction(0, 1));  // RIGHT → (3,4)
            controller.executeAction(new MoveAction(-1, 0)); // UP    → (2,4)
            assertEquals(2, hero.getRow());
            assertEquals(4, hero.getCol());
        }

        @Test
        void moveIntoWall_heroStaysInPlace() {
            // Place a wall directly above hero
            map.getCell(2, 3).setStaticObject(
                new com.kurawler.model.object.StaticObject("Wall", "Solid", false, false));
            int rowBefore = hero.getRow();
            controller.executeAction(new MoveAction(-1, 0));
            assertEquals(rowBefore, hero.getRow());
        }

        @Test
        void moveOutOfBounds_heroStaysInPlace() {
            hero.moveToCell(map.getCell(0, 3)); // top row
            controller.executeAction(new MoveAction(-1, 0)); // try going further UP
            assertEquals(0, hero.getRow());
        }

        @Test
        void energyCost_reducedByMoveAction() {
            int energyBefore = hero.getEnergy();
            controller.executeAction(new MoveAction(0, 1));
            assertTrue(hero.getEnergy() < energyBefore);
        }

        // TC-MOV-08: hero walks onto a tile that has an item on it
        @Test
        void moveOntoItemTile_heroOccupiesSameTile() {
            Key gem = new Key("Gem", "Shiny gem", "gem1");
            map.getCell(2, 3).addObject(gem);
            controller.executeAction(new MoveAction(-1, 0)); // UP → (2,3)
            assertEquals(2, hero.getRow());
            assertEquals(3, hero.getCol());
        }

        @Test
        void moveOntoItemTile_itemRemainsOnCellUntilTaken() {
            Key gem = new Key("Gem", "Shiny gem", "gem2");
            map.getCell(2, 3).addObject(gem);
            controller.executeAction(new MoveAction(-1, 0));
            assertTrue(map.getCell(2, 3).getObjects().contains(gem));
        }

        // TC-MOV-10: hero cannot walk onto a tile occupied by a live enemy
        @Test
        void moveIntoLiveEnemy_heroIsBlocked() {
            Knight knight = new Knight();
            knight.moveToCell(map.getCell(2, 3)); // directly above hero
            session.addEnemy(knight);
            int rowBefore = hero.getRow();
            controller.executeAction(new MoveAction(-1, 0));
            assertEquals(rowBefore, hero.getRow());
        }

        @Test
        void moveIntoLiveEnemy_colUnchanged() {
            Knight knight = new Knight();
            knight.moveToCell(map.getCell(3, 4)); // directly right of hero
            session.addEnemy(knight);
            int colBefore = hero.getCol();
            controller.executeAction(new MoveAction(0, 1));
            assertEquals(colBefore, hero.getCol());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Item pickup flow
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ItemPickup {

        @Test
        void takeAction_itemMovesFromCellToInventory() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            map.getCell(3, 3).addObject(sword);

            controller.executeAction(new TakeAction(sword));

            assertFalse(map.getCell(3, 3).getObjects().contains(sword));
            assertTrue(hero.getInventory().hasItem(sword));
        }

        @Test
        void takeAction_advancesTurnCount() {
            Weapon bow = new Weapon("Bow", "Ranged", 3);
            map.getCell(3, 3).addObject(bow);
            controller.executeAction(new TakeAction(bow));
            assertEquals(1, session.getTurnCount());
        }

        @Test
        void takeAction_itemOnOtherCell_isIgnored() {
            Weapon axe = new Weapon("Axe", "Heavy", 7);
            map.getCell(1, 1).addObject(axe); // different cell
            controller.executeAction(new TakeAction(axe));
            assertFalse(hero.getInventory().hasItem(axe));
        }

        @Test
        void takeAction_inventoryFull_itemRemains() {
            // Fill inventory to capacity (8 slots)
            for (int i = 0; i < 8; i++) {
                hero.getInventory().addItem(new Weapon("W" + i, "", i));
            }
            Weapon extra = new Weapon("Extra", "Overflow", 1);
            map.getCell(3, 3).addObject(extra);
            controller.executeAction(new TakeAction(extra));
            assertTrue(map.getCell(3, 3).getObjects().contains(extra));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Combat system
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class CombatSystem {

        private Knight knight;

        @BeforeEach
        void placeEnemy() {
            knight = new Knight();
            knight.moveToCell(map.getCell(3, 4)); // adjacent to hero at (3,3)
            session.addEnemy(knight);
        }

        @Test
        void attackAction_enemyTakesDamage() {
            int hpBefore = knight.getHealth();
            controller.executeAction(new AttackAction(hero, knight));
            assertTrue(knight.getHealth() < hpBefore);
        }

        @Test
        void attackAction_advancesTurnCount() {
            controller.executeAction(new AttackAction(hero, knight));
            assertEquals(1, session.getTurnCount());
        }

        @Test
        void combatResolver_dealsDamageByFormula() {
            int expectedDmg = Math.max(1, (hero.getStrength() / 2) + hero.getAttackPower()
                    - knight.getDefensePower());
            int actual = CombatResolver.calculateFinalDamage(hero, knight);
            assertEquals(expectedDmg, actual);
        }

        @Test
        void killEnemy_heroAttackUntilDead() {
            // Keep attacking until knight is dead (or energy runs out)
            for (int i = 0; i < 25 && knight.isAlive(); i++) {
                hero.restoreEnergy(10);
                controller.executeAction(new AttackAction(hero, knight));
            }
            assertFalse(knight.isAlive());
        }

        @Test
        void attackDeadEnemy_producesNoEffects() {
            knight.takeDamage(knight.getHealth()); // kill knight
            int turnBefore = session.getTurnCount();
            // AttackAction.resolve() returns empty list for dead defender — turn still advances
            controller.executeAction(new AttackAction(hero, knight));
            assertEquals(turnBefore + 1, session.getTurnCount());
            // Knight's HP must not go negative
            assertEquals(0, knight.getHealth());
        }

        @Test
        void heroDies_gameOverTriggered() {
            hero.takeDamage(hero.getHealth()); // instant kill
            assertTrue(session.isGameOver());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Win condition — hero collects objective
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class WinCondition {

        @Test
        void withoutObjective_checkWinCondition_returnsFalse() {
            assertFalse(session.checkWinCondition());
        }

        @Test
        void objectiveSetButNotCollected_returnsFalse() {
            Key goldenKey = new Key("Golden Key", "Unlock the vault", "gold");
            session.setTargetObjective(goldenKey);
            assertFalse(session.checkWinCondition());
        }

        @Test
        void heroCollectsObjectiveKey_winConditionMet() {
            Key goldenKey = new Key("Golden Key", "Unlock the vault", "gold");
            session.setTargetObjective(goldenKey);
            map.getCell(3, 3).addObject(goldenKey);

            controller.executeAction(new TakeAction(goldenKey));

            assertTrue(session.checkWinCondition());
        }

        @Test
        void heroCollectsWrongKey_winConditionNotMet() {
            Key objective = new Key("Golden Key", "Target", "gold");
            Key wrong = new Key("Rusty Key", "Wrong", "rust");
            session.setTargetObjective(objective);
            hero.getInventory().addItem(wrong);

            assertFalse(session.checkWinCondition());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Consumable healing flow
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ConsumableFlow {

        @Test
        void useConsumable_increasesHeroHP() {
            hero.takeDamage(5);
            int hpAfterDamage = hero.getHealth();
            Consumable potion = new Consumable("Potion", "Heals 5 HP", 5);
            hero.getInventory().addItem(potion);

            hero.useConsumable(potion);

            assertTrue(hero.getHealth() > hpAfterDamage);
        }

        @Test
        void useConsumable_removesItemFromInventory() {
            Consumable potion = new Consumable("Potion", "Heals 5 HP", 5);
            hero.getInventory().addItem(potion);
            hero.useConsumable(potion);
            assertFalse(hero.getInventory().hasItem(potion));
        }

        @Test
        void useConsumable_hpCappedAtMaxHealth() {
            Consumable bigPotion = new Consumable("Big Potion", "Heals 999 HP", 999);
            hero.getInventory().addItem(bigPotion);
            hero.useConsumable(bigPotion);
            assertEquals(hero.getMaxHealth(), hero.getHealth());
        }

        @Test
        void useConsumable_onFullHP_hpUnchanged() {
            Consumable potion = new Consumable("Potion", "Heals 5 HP", 5);
            hero.getInventory().addItem(potion);
            int fullHp = hero.getHealth();
            hero.useConsumable(potion);
            assertEquals(fullHp, hero.getHealth());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Map transition
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class MapTransition {

        private Map secondMap;

        @BeforeEach
        void setupSecondMap() {
            secondMap = new Map(COLS, ROWS);
            session.registerWorldMap("dungeon", secondMap);
            session.setCurrentMapId("entry");
        }

        @Test
        void transitionHeroTo_knownMap_returnsTrue() {
            assertTrue(session.transitionHeroTo("dungeon", 3, 3));
        }

        @Test
        void transitionHeroTo_unknownMap_returnsFalse() {
            assertFalse(session.transitionHeroTo("nowhere", 1, 1));
        }

        @Test
        void transitionHeroTo_updatesCurrentMap() {
            session.transitionHeroTo("dungeon", 3, 3);
            assertEquals(secondMap, session.getMap());
        }

        @Test
        void transitionHeroTo_incrementsFloorCount() {
            int before = session.getFloorsClearedCount();
            session.transitionHeroTo("dungeon", 3, 3);
            assertEquals(before + 1, session.getFloorsClearedCount());
        }

        @Test
        void transitionHeroTo_clearsEnemiesOnOldMap() {
            session.addEnemy(new Knight());
            session.transitionHeroTo("dungeon", 3, 3);
            assertTrue(session.getEnemies().isEmpty());
        }

        @Test
        void lockedPortal_withKey_heroCanEnter() {
            Key key = new Key("Dungeon Key", "Opens dungeon", "dungeon_key");
            hero.getInventory().addItem(key);

            MapPortal portal = new MapPortal("Dungeon Door", "Leads to the dungeon",
                    "dungeon", 3, 3, "dungeon_key", false);

            assertTrue(portal.tryUnlock(hero));
        }

        @Test
        void lockedPortal_withoutKey_heroCannotEnter() {
            MapPortal portal = new MapPortal("Locked Door", "Needs a key",
                    "dungeon", 3, 3, "dungeon_key", false);
            assertFalse(portal.tryUnlock(hero));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Multi-step adventure scenario
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class MultiStepScenario {

        @Test
        void heroMovesFightsAndCollectsKey_winConditionMet() {
            // Set up world: objective key at (3,5), knight at (3,4)
            Key questKey = new Key("Quest Key", "The one key", "quest");
            session.setTargetObjective(questKey);
            map.getCell(3, 5).addObject(questKey);

            Knight knight = new Knight();
            knight.moveToCell(map.getCell(3, 4));
            session.addEnemy(knight);

            // Step 1: fight the knight until dead (energy restored each round)
            for (int i = 0; i < 30 && knight.isAlive(); i++) {
                hero.restoreEnergy(5);
                controller.executeAction(new AttackAction(hero, knight));
            }
            assertFalse(knight.isAlive());

            // Step 2: move RIGHT to (3,4) — knight's old cell is now free
            controller.executeAction(new MoveAction(0, 1)); // → (3,4)
            assertEquals(4, hero.getCol());

            // Step 3: move RIGHT again to (3,5) where the key sits
            controller.executeAction(new MoveAction(0, 1)); // → (3,5)
            assertEquals(5, hero.getCol());

            // Step 4: pick up the quest key
            controller.executeAction(new TakeAction(questKey));
            assertTrue(hero.getInventory().hasItem(questKey));

            // Win!
            assertTrue(session.checkWinCondition());
        }

        @Test
        void turnCountReflectsAllActions() {
            Key key = new Key("Key", "k", "k1");
            map.getCell(3, 3).addObject(key);

            controller.executeAction(new MoveAction(0, 1));  // turn 1
            controller.executeAction(new MoveAction(0, -1)); // turn 2
            controller.executeAction(new TakeAction(key));   // turn 3 (item not on hero cell — no-op effect but turn advances)

            assertEquals(3, session.getTurnCount());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. Item interaction & action availability (TC-ACT-01 through TC-ACT-07)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ItemInteraction {

        // TC-ACT-01: actions on items not in inventory produce no effects
        @Test
        void eatAction_itemNotInInventory_producesNoEffect() {
            Consumable potion = new Consumable("Potion", "Heals 5", 5);
            int hpBefore = hero.getHealth();
            controller.executeAction(new EatAction(potion));
            assertEquals(hpBefore, hero.getHealth());
        }

        @Test
        void wearAction_itemNotInInventory_producesNoEffect() {
            Wearable armor = new Wearable("Armor", "Plate", 5);
            int defBefore = hero.getDefensePower();
            controller.executeAction(new WearAction(armor));
            assertEquals(defBefore, hero.getDefensePower());
        }

        @Test
        void readAction_itemNotInInventory_producesNoEffect() {
            Scroll scroll = new Scroll("Scroll", "Ancient text", "...", 10);
            hero.takeDamage(5);
            int hpBefore = hero.getHealth();
            controller.executeAction(new ReadAction(scroll));
            assertEquals(hpBefore, hero.getHealth());
        }

        // TC-ACT-02: potion in inventory → EAT via pipeline restores HP
        @Test
        void eatAction_potionInInventory_restoresHP() {
            hero.takeDamage(8);
            Consumable potion = new Consumable("Potion", "Heals 5", 5);
            hero.getInventory().addItem(potion);
            int hpBefore = hero.getHealth();
            controller.executeAction(new EatAction(potion));
            assertTrue(hero.getHealth() > hpBefore);
        }

        @Test
        void eatAction_potionInInventory_removesItemAfterUse() {
            Consumable potion = new Consumable("Potion", "Heals 5", 5);
            hero.getInventory().addItem(potion);
            controller.executeAction(new EatAction(potion));
            assertFalse(hero.getInventory().hasItem(potion));
        }

        // TC-ACT-03: scroll in inventory → READ via pipeline applies effect
        @Test
        void readAction_healingScrollInInventory_restoresHP() {
            hero.takeDamage(6);
            Scroll healScroll = new Scroll("Healing Scroll", "Glowing text", "Heal!", 6);
            hero.getInventory().addItem(healScroll);
            int hpBefore = hero.getHealth();
            controller.executeAction(new ReadAction(healScroll));
            assertTrue(hero.getHealth() > hpBefore);
        }

        @Test
        void readAction_scrollInInventory_removesScrollAfterRead() {
            Scroll scroll = new Scroll("Scroll", "Text", "...", 3);
            hero.getInventory().addItem(scroll);
            controller.executeAction(new ReadAction(scroll));
            assertFalse(hero.getInventory().hasItem(scroll));
        }

        // TC-ACT-04: armor in inventory → WEAR via pipeline increases defense
        @Test
        void wearAction_armorInInventory_increasesDefense() {
            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            int defBefore = hero.getDefensePower();
            controller.executeAction(new WearAction(armor));
            assertTrue(hero.getDefensePower() > defBefore);
        }

        @Test
        void wearAction_armorInInventory_armorBecomesEquipped() {
            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            controller.executeAction(new WearAction(armor));
            assertEquals(armor, hero.getEquippedArmor());
        }

        // TC-ACT-05: non-searchable decorative object → SearchAction produces no effect
        @Test
        void searchAction_nonSearchableObject_producesNoEffect() {
            StaticObject decoration = new StaticObject("Vase", "Decorative vase", true, false);
            map.getCell(2, 3).setStaticObject(decoration);
            int turnBefore = session.getTurnCount();
            controller.executeAction(new SearchAction(decoration));
            // Turn advances but no game-state change; object remains unsearched
            assertFalse(decoration.wasSearched());
        }

        // TC-ACT-06: inventory full → TAKE blocked, EAT on existing item still works
        @Test
        void inventoryFull_eatAction_onExistingConsumable_stillWorks() {
            Consumable potion = new Consumable("Potion", "Heals 5", 5);
            hero.getInventory().addItem(potion);
            // Fill remaining 7 slots
            for (int i = 0; i < 7; i++) {
                hero.getInventory().addItem(new Weapon("W" + i, "", i));
            }
            assertTrue(hero.getInventory().isFull());
            hero.takeDamage(5);
            int hpBefore = hero.getHealth();
            controller.executeAction(new EatAction(potion));
            assertTrue(hero.getHealth() > hpBefore);
        }

        @Test
        void inventoryFull_takeAction_onFloorItem_isStillBlocked() {
            for (int i = 0; i < 8; i++) {
                hero.getInventory().addItem(new Weapon("W" + i, "", i));
            }
            Consumable potion = new Consumable("Potion", "Floor potion", 5);
            map.getCell(3, 3).addObject(potion);
            controller.executeAction(new TakeAction(potion));
            assertFalse(hero.getInventory().hasItem(potion));
        }

        // TC-ACT-07: discard item from inventory → removed from inventory, dropped on cell
        @Test
        void discardAction_removesItemFromInventory() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new DiscardAction(sword));
            assertFalse(hero.getInventory().hasItem(sword));
        }

        @Test
        void discardAction_dropsItemOnHeroCell() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new DiscardAction(sword));
            assertTrue(map.getCell(hero.getRow(), hero.getCol()).getObjects().contains(sword));
        }

        @Test
        void discardAction_itemNotInInventory_producesNoEffect() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            int invSizeBefore = hero.getInventory().getItemCount();
            controller.executeAction(new DiscardAction(sword));
            assertEquals(invSizeBefore, hero.getInventory().getItemCount());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. Inventory management (TC-INV-02, TC-INV-04 – TC-INV-10)
    //     TC-INV-01 → ItemPickup.takeAction_itemMovesFromCellToInventory
    //     TC-INV-03 → ItemPickup.takeAction_inventoryFull_itemRemains
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class InventoryManagement {

        // TC-INV-02: take 8 items one by one → inventory fills up
        @Test
        void take8Items_inventoryBecomesExactlyFull() {
            for (int i = 0; i < 8; i++) {
                Weapon w = new Weapon("W" + i, "Weapon " + i, i);
                map.getCell(3, 3).addObject(w);
                controller.executeAction(new TakeAction(w));
            }
            assertTrue(hero.getInventory().isFull());
            assertEquals(8, hero.getInventory().getItemCount());
        }

        @Test
        void take8Items_ninthItemStaysOnFloor() {
            for (int i = 0; i < 8; i++) {
                Weapon w = new Weapon("W" + i, "", i);
                map.getCell(3, 3).addObject(w);
                controller.executeAction(new TakeAction(w));
            }
            Weapon ninth = new Weapon("W8", "", 8);
            map.getCell(3, 3).addObject(ninth);
            controller.executeAction(new TakeAction(ninth));
            assertTrue(map.getCell(3, 3).getObjects().contains(ninth));
        }

        // TC-INV-04: discard → spec: permanently removed from game (cannot be picked up)
        // NOTE: current implementation drops item on hero's cell — this test reveals
        // the spec violation: DiscardEffect.apply() places the item back on the map.
        @Test
        void discard_itemCannotBeRetrievedAfterwards() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new DiscardAction(sword));
            // Spec §2.4.2: discard removes item permanently — should not be on map
            assertFalse(map.getCell(hero.getRow(), hero.getCol()).getObjects().contains(sword),
                    "Discarded item must not remain on the map (spec §2.4.2)");
        }

        // TC-INV-05: take item from open static container
        @Test
        void takeFromStaticContainer_itemMovesToInventory() {
            StaticContainer chest = new StaticContainer("Chest", "Wooden chest");
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            chest.addItem(sword);

            controller.executeAction(new TakeFromContainerAction(chest, sword));

            assertTrue(hero.getInventory().hasItem(sword));
            assertFalse(chest.getContents().contains(sword));
        }

        @Test
        void takeFromStaticContainer_fullInventory_itemStaysInChest() {
            for (int i = 0; i < 8; i++) {
                hero.getInventory().addItem(new Weapon("W" + i, "", i));
            }
            StaticContainer chest = new StaticContainer("Chest", "Wooden chest");
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            chest.addItem(sword);

            controller.executeAction(new TakeFromContainerAction(chest, sword));

            assertTrue(chest.getContents().contains(sword));
        }

        // TC-INV-06: break crate — spec: STR-based probability
        // NOTE: BreakObjectEffect has no STR check; always succeeds when searchable.
        // This test documents the spec violation: low-STR hero should likely fail.
        @Test
        void breakCrate_lowStrHero_alwaysSucceeds_specViolation() {
            hero.setStrength(1); // very low STR — spec says should likely fail
            StaticObject crate = new StaticObject("Crate", "Wooden crate", false, true);
            map.getCell(2, 3).setStaticObject(crate);

            controller.executeAction(new BreakAction(2, 3));

            // Spec §1.2: low STR → likely failure; implementation always breaks it
            assertNotNull(map.getCell(2, 3).getStaticObject(),
                    "Low-STR hero should not reliably break the crate (spec §1.2)");
        }

        // TC-INV-07: break crate with high STR → reveals hidden item
        @Test
        void breakCrate_searchable_removesObjectFromCell() {
            StaticObject crate = new StaticObject("Crate", "Wooden crate", false, true);
            map.getCell(2, 3).setStaticObject(crate);

            controller.executeAction(new BreakAction(2, 3));

            assertNull(map.getCell(2, 3).getStaticObject());
        }

        @Test
        void breakCrate_withHiddenItem_revealsItemOnCell() {
            Weapon loot = new Weapon("Loot Sword", "Hidden blade", 7);
            StaticObject crate = new StaticObject("Crate", "Wooden crate", false, true);
            crate.setHiddenItem(loot);
            map.getCell(2, 3).setStaticObject(crate);

            controller.executeAction(new BreakAction(2, 3));

            assertTrue(map.getCell(2, 3).getObjects().contains(loot));
        }

        @Test
        void breakAction_nonSearchableObject_producesNoEffect() {
            StaticObject wall = new StaticObject("Wall", "Solid wall", false, false);
            map.getCell(2, 3).setStaticObject(wall);

            controller.executeAction(new BreakAction(2, 3));

            assertNotNull(map.getCell(2, 3).getStaticObject());
        }

        // TC-INV-08: portable container (pouch) occupies 1 slot, holds multiple items
        @Test
        void portableContainer_occupiesOneInventorySlot() {
            PortableContainer pouch = new PortableContainer("Pouch", "Small bag", 3);
            hero.getInventory().addItem(pouch);
            assertEquals(1, hero.getInventory().getItemCount());
        }

        @Test
        void portableContainer_canHoldMultipleItemsInside() {
            PortableContainer pouch = new PortableContainer("Pouch", "Small bag", 3);
            Weapon w1 = new Weapon("Dagger", "", 2);
            Weapon w2 = new Weapon("Knife", "", 1);
            pouch.addItem(w1);
            pouch.addItem(w2);
            assertEquals(2, pouch.getContents().size());
        }

        @Test
        void takeFromPortableContainer_movesItemToInventory() {
            PortableContainer pouch = new PortableContainer("Pouch", "Small bag", 3);
            Weapon dagger = new Weapon("Dagger", "", 2);
            pouch.addItem(dagger);
            hero.getInventory().addItem(pouch);

            controller.executeAction(new TakeFromContainerAction(pouch, dagger));

            assertTrue(hero.getInventory().hasItem(dagger));
            assertFalse(pouch.getContents().contains(dagger));
        }

        // TC-INV-09: locked chest without key → spec: cannot open
        // NOTE: TakeFromContainerAction does not check isLocked() — this test reveals
        // the spec violation: items can be taken from a locked chest without a key.
        @Test
        void takeFromLockedChest_withoutKey_shouldBeBlocked() {
            StaticContainer lockedChest = new StaticContainer("Locked Chest", "Iron chest");
            lockedChest.setLock("vault_key");
            Weapon treasure = new Weapon("Golden Sword", "Priceless", 10);
            lockedChest.addItem(treasure);

            controller.executeAction(new TakeFromContainerAction(lockedChest, treasure));

            // Spec §2.3: locked container must reject access without the right key
            assertFalse(hero.getInventory().hasItem(treasure),
                    "Locked chest must not allow item removal without a key (spec §2.3)");
        }

        // TC-INV-10: locked chest with matching key → can open and take item
        @Test
        void takeFromChest_afterUnlock_itemMovesToInventory() {
            StaticContainer chest = new StaticContainer("Chest", "Iron chest");
            chest.setLock("vault_key");
            Weapon treasure = new Weapon("Golden Sword", "Priceless", 10);
            chest.addItem(treasure);

            chest.unlock();
            controller.executeAction(new TakeFromContainerAction(chest, treasure));

            assertTrue(hero.getInventory().hasItem(treasure));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. Hero character stats (TC-STAT-01 through TC-STAT-11, §2.4.1)
    //     TC-STAT-08 → CombatSystem.heroDies_gameOverTriggered (already exists)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class HeroStats {

        // TC-STAT-01
        @Test
        void freshHero_startingHP_equals17() {
            assertEquals(17, hero.getMaxHealth());
            assertEquals(17, hero.getHealth());
        }

        // TC-STAT-02
        @Test
        void freshHero_startingMana_equals80() {
            assertEquals(80, hero.getMaxMana());
            assertEquals(80, hero.getMana());
        }

        // TC-STAT-03
        @Test
        void freshHero_baseDefense_equals2_withNoArmor() {
            assertNull(hero.getEquippedArmor());
            assertEquals(2, hero.getDefensePower());
        }

        // TC-STAT-04
        @Test
        void freshHero_strength_isBetween8and15Inclusive() {
            int str = hero.getStrength();
            assertTrue(str >= 8 && str <= 15,
                    "Expected STR in [8,15] but was " + str);
        }

        // TC-STAT-05: across 10 fresh heroes at least 2 must have different STR values
        @Test
        void multipleNewGames_strValuesAreRandom() {
            int first = new Hero("H0").getStrength();
            boolean varied = false;
            for (int i = 1; i < 10; i++) {
                if (new Hero("H" + i).getStrength() != first) {
                    varied = true;
                    break;
                }
            }
            assertTrue(varied, "STR must vary across new games (spec §2.4.1)");
        }

        // TC-STAT-06: DEF = base(2) + armor.defenseBonus
        @Test
        void equipArmor_defenseEqualsBasePlusArmorBonus() {
            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            controller.executeAction(new WearAction(armor));
            assertEquals(2 + armor.getDefenseBonus(), hero.getDefensePower());
        }

        @Test
        void unequipArmor_defenseReturnsTOBase() {
            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            controller.executeAction(new WearAction(armor));
            hero.unequipWearable(armor);
            assertEquals(2, hero.getDefensePower());
        }

        // TC-STAT-07: hero takes damage from enemy → HP decreases by max(1, atk - def)
        @Test
        void enemyHitsHero_hpDecreasesPerFormula() {
            Knight knight = new Knight(); // ATK=1, STR=1, DEF=1
            int expectedDmg = Math.max(1,
                    (knight.getStrength() / 2) + knight.getAttackPower() - hero.getDefensePower());
            int hpBefore = hero.getHealth();

            CombatResolver.performMeleeAttack(knight, hero);

            assertEquals(hpBefore - expectedDmg, hero.getHealth());
        }

        @Test
        void enemyHit_damage_neverBelowOne() {
            // Hero DEF > enemy ATK → formula would go negative; must clamp to 1
            Knight knight = new Knight();
            hero.setDefensePower(100); // massively over-armored
            int hpBefore = hero.getHealth();

            CombatResolver.performMeleeAttack(knight, hero);

            assertEquals(hpBefore - 1, hero.getHealth());
        }

        // TC-STAT-09: eating energy consumable increases energy bar, capped at max
        @Test
        void eatEnergyConsumable_energyIncreases() {
            hero.useEnergy(20);
            int energyBefore = hero.getEnergy();
            Consumable energyDrink = new Consumable("Energy Drink", "Restores energy", 0, 0, 15);
            hero.getInventory().addItem(energyDrink);

            controller.executeAction(new EatAction(energyDrink));

            assertTrue(hero.getEnergy() > energyBefore);
        }

        @Test
        void eatEnergyConsumable_energyDoesNotExceedMax() {
            Consumable bigDrink = new Consumable("Big Drink", "Huge energy", 0, 0, 999);
            hero.getInventory().addItem(bigDrink);

            controller.executeAction(new EatAction(bigDrink));

            assertEquals(hero.getMaxEnergy(), hero.getEnergy());
        }

        // TC-STAT-10: each move step depletes exactly 1 energy
        @Test
        void eachMoveCostsExactlyOneEnergy() {
            int energyStart = hero.getEnergy();
            controller.executeAction(new MoveAction(0, 1));
            assertEquals(energyStart - 1, hero.getEnergy());
        }

        @Test
        void multipleMovesDepletesEnergyProportionally() {
            int steps = 5;
            int energyStart = hero.getEnergy();
            // Move back and forth so hero stays in bounds
            for (int i = 0; i < steps; i++) {
                controller.executeAction(i % 2 == 0 ? new MoveAction(0, 1) : new MoveAction(0, -1));
            }
            assertEquals(energyStart - steps, hero.getEnergy());
        }

        @Test
        void zeroEnergy_heroCannotMove() {
            hero.setEnergy(0);
            int rowBefore = hero.getRow();
            controller.executeAction(new MoveAction(-1, 0));
            assertEquals(rowBefore, hero.getRow());
        }

        // TC-STAT-11: casting a spell (heroShoot) costs RANGED_MANA_COST = 10
        @Test
        void heroShoot_manaDecreasesBy10() {
            int manaBefore = hero.getMana();
            controller.heroShoot();
            assertEquals(manaBefore - 10, hero.getMana());
        }

        @Test
        void heroShoot_insufficientMana_doesNotFire() {
            hero.setMana(5); // below RANGED_MANA_COST=10
            boolean fired = controller.heroShoot();
            assertFalse(fired);
            assertEquals(5, hero.getMana());
        }

        @Test
        void heroShoot_projectileAddedToSession() {
            assertTrue(session.getProjectiles().isEmpty());
            controller.heroShoot();
            assertFalse(session.getProjectiles().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. Knight enemy AI behavior (TC-KNI-01 through TC-KNI-08, §2.5.1)
    //     TC-KNI-07 (attack frequency) omitted — not measurable at unit level.
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class KnightAI {

        private Knight knight;

        @BeforeEach
        void placeKnight() {
            knight = new Knight();
        }

        // TC-KNI-01
        @Test
        void freshKnight_startingHP_equals20() {
            assertEquals(20, knight.getMaxHealth());
            assertEquals(20, knight.getHealth());
        }

        @Test
        void freshKnight_sightRadius_equals5() {
            assertEquals(5, knight.getSightRadius());
        }

        // TC-KNI-02: hero more than 5 Euclidean tiles away → knight random-walks
        // (does NOT move directly toward hero)
        @Test
        void beyondSightRadius_decideAction_doesNotCrash() {
            // Hero at (0,0), knight at (6,6) — Euclidean ≈ 8.5 > 5
            knight.moveToCell(map.getCell(6, 6));
            hero.moveToCell(map.getCell(0, 0));
            assertDoesNotThrow(() -> knight.decideAction(hero, map, session));
        }

        @Test
        void beyondSightRadius_knight_doesNotApproachHeroDirectly() {
            // Knight at (6,6), hero at (0,0) — distance > 5, random walk
            // After one tick knight should NOT be at (0,0) or have jumped to hero
            knight.moveToCell(map.getCell(6, 6));
            hero.moveToCell(map.getCell(0, 0));
            knight.decideAction(hero, map, session);
            // Knight must stay on a legal cell, not teleport to hero
            assertFalse(knight.getRow() == 0 && knight.getCol() == 0);
        }

        // TC-KNI-03: hero within 5 Euclidean tiles → knight steps toward hero
        @Test
        void withinSightRadius_knightStepsTowardHero_horizontally() {
            // Knight at (3,6), hero at (3,3) — distance = 3 ≤ 5, same row
            // dCol = -3 → knight should step LEFT → col becomes 5
            knight.moveToCell(map.getCell(3, 6));
            hero.moveToCell(map.getCell(3, 3));
            knight.decideAction(hero, map, session);
            assertEquals(5, knight.getCol()); // moved one step toward hero
        }

        @Test
        void withinSightRadius_knightStepsTowardHero_vertically() {
            // Knight at (6,3), hero at (3,3) — distance = 3 ≤ 5, same col
            // dRow = -3 → knight should step UP → row becomes 5
            knight.moveToCell(map.getCell(6, 3));
            hero.moveToCell(map.getCell(3, 3));
            knight.decideAction(hero, map, session);
            assertEquals(5, knight.getRow());
        }

        // TC-KNI-04: knight adjacent to hero (Manhattan = 1) → knight attacks hero
        @Test
        void adjacentToHero_knightAttacksHero_hpDecreases() {
            knight.moveToCell(map.getCell(3, 4)); // one right of hero (3,3)
            int hpBefore = hero.getHealth();
            knight.decideAction(hero, map, session);
            assertTrue(hero.getHealth() < hpBefore);
        }

        @Test
        void adjacentToHero_knightDoesNotMove_itAttacks() {
            knight.moveToCell(map.getCell(3, 4));
            knight.decideAction(hero, map, session);
            // Knight stays adjacent — attacks, not moves
            assertEquals(4, knight.getCol());
        }

        // TC-KNI-05: knight DEF reduces hero-to-knight incoming damage by 1
        @Test
        void knightDEF_reducesIncomingDamage() {
            // Knight DEF = 1; hero does max(1, STR/2 + ATK - 1) damage
            int rawDmg = (hero.getStrength() / 2) + hero.getAttackPower();
            int expectedDmg = Math.max(1, rawDmg - knight.getDefensePower());
            int hpBefore = knight.getHealth();
            CombatResolver.performMeleeAttack(hero, knight);
            assertEquals(hpBefore - expectedDmg, knight.getHealth());
        }

        @Test
        void knightAttack_heroDefReducesDamage_minimumOneApplied() {
            // Hero DEF = 2, Knight ATK = 1, STR = 1 → max(1, 0+1-2) = 1
            int expectedDmg = Math.max(1,
                    (knight.getStrength() / 2) + knight.getAttackPower() - hero.getDefensePower());
            int hpBefore = hero.getHealth();
            CombatResolver.performMeleeAttack(knight, hero);
            assertEquals(hpBefore - expectedDmg, hero.getHealth());
        }

        // TC-KNI-06: kill knight → spec: removed from map cell and enemy count decrements
        // NOTE: DamageEffect does not clear the cell or call session.removeEnemy() for melee
        // kills — both assertions below reveal spec violations.
        @Test
        void killKnight_cellBecomesEmpty() {
            knight.moveToCell(map.getCell(3, 4));
            session.addEnemy(knight);
            for (int i = 0; i < 30 && knight.isAlive(); i++) {
                hero.restoreEnergy(5);
                controller.executeAction(new AttackAction(hero, knight));
            }
            assertFalse(knight.isAlive());
            // Spec §2.5.1: dead knight must vacate its cell
            assertNull(map.getCell(3, 4).getOccupyingCharacter(),
                    "Dead knight must be removed from its map cell (spec §2.5.1)");
        }

        @Test
        void killKnight_activeEnemyCountDecrements() {
            knight.moveToCell(map.getCell(3, 4));
            session.addEnemy(knight);
            int countBefore = session.getActiveEnemyCount();
            for (int i = 0; i < 30 && knight.isAlive(); i++) {
                hero.restoreEnergy(5);
                controller.executeAction(new AttackAction(hero, knight));
            }
            // Spec §2.5.1: active enemy count must decrement on kill
            assertEquals(countBefore - 1, session.getActiveEnemyCount(),
                    "Active enemy count must decrement when knight is killed (spec §2.5.1)");
        }

        // TC-KNI-08: wall between hero and knight within sight radius
        @Test
        void wallDirectlyInPath_knightIsBlocked() {
            // Knight at (3,2), hero at (3,5), wall at (3,3) — distance = 3 ≤ 5
            knight.moveToCell(map.getCell(3, 2));
            hero.moveToCell(map.getCell(3, 5));
            map.getCell(3, 3).setStaticObject(
                    new StaticObject("Wall", "Solid wall", false, false));

            knight.decideAction(hero, map, session);

            // Primary step blocked; fallback tryStep(dRow, 0) = tryStep(0,0) is no-op
            assertEquals(2, knight.getCol()); // knight stays in place
        }

        @Test
        void wallBesidePath_knightCanNavigateAround() {
            // Knight at (3,2), hero at (3,5), wall at (3,3) but row 2 is open
            // Primary step to (3,3) blocked; fallback step to (2,2) if dRow != 0
            // Place hero diagonally so dRow and dCol are both nonzero
            knight.moveToCell(map.getCell(4, 2));
            hero.moveToCell(map.getCell(3, 5));  // dRow=-1, dCol=3 → euclid≈3.2
            map.getCell(3, 2).setStaticObject(
                    new StaticObject("Wall", "Solid wall", false, false));
            int colBefore = knight.getCol();

            knight.decideAction(hero, map, session);

            // Knight should at least attempt to move (primary or fallback step)
            assertFalse(knight.getRow() == 4 && knight.getCol() == colBefore,
                    "Knight must attempt to navigate around the wall");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. Sorcerer enemy AI behavior (TC-SOR-01 through TC-SOR-10, §2.5.2)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class SorcererAI {

        private Sorcerer sorcerer;

        @BeforeEach
        void placeSorcerer() {
            sorcerer = new Sorcerer();
        }

        // TC-SOR-01
        @Test
        void freshSorcerer_HP_equals10() {
            assertEquals(10, sorcerer.getMaxHealth());
            assertEquals(10, sorcerer.getHealth());
        }

        @Test
        void freshSorcerer_DEF_equals0() {
            assertEquals(0, sorcerer.getDefensePower());
        }

        @Test
        void freshSorcerer_spellDamage_equals8() {
            assertEquals(8, sorcerer.getSpellDamage());
        }

        // TC-SOR-02: after ATTACK_COOLDOWN ticks with clear LoS, bolt is added to session
        @Test
        void afterCooldownTicks_sorcererFiresBolt() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            session.setCurrentMode("Play");
            for (int i = 0; i < sorcerer.getAttackCooldown(); i++) {
                sorcerer.decideAction(hero, map, session);
            }
            assertFalse(session.getProjectiles().isEmpty(), "Sorcerer must fire after cooldown");
        }

        @Test
        void beforeCooldownExpires_noProjectileFired() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            for (int i = 0; i < sorcerer.getAttackCooldown() - 1; i++) {
                sorcerer.decideAction(hero, map, session);
            }
            assertTrue(session.getProjectiles().isEmpty());
        }

        // TC-SOR-03: cooldown constant is at least 5 ticks
        @Test
        void attackCooldown_isAtLeast5Ticks() {
            assertTrue(sorcerer.getAttackCooldown() >= 5);
        }

        @Test
        void afterFiringBolt_cooldownResetsToZero() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            for (int i = 0; i < sorcerer.getAttackCooldown(); i++) {
                sorcerer.decideAction(hero, map, session);
            }
            int countAfterFirstShot = session.getProjectiles().size();
            sorcerer.decideAction(hero, map, session); // one tick after reset
            assertEquals(countAfterFirstShot, session.getProjectiles().size());
        }

        // TC-SOR-04: wall in LoS blocks the spell
        @Test
        void wallInLineOfSight_castSpellReturnsNull() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            map.getCell(3, 3).setStaticObject(
                    new StaticObject("Wall", "Solid wall", false, false));
            assertNull(sorcerer.castSpell(hero, map), "Sorcerer must not fire through a wall");
        }

        @Test
        void clearLineOfSight_castSpellSucceeds() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            assertNotNull(sorcerer.castSpell(hero, map));
        }

        // TC-SOR-05: arcane bolt deals 8 damage, reduced by hero DEF
        @Test
        void arcaneBolt_baseDamage_equals8() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            Projectile bolt = sorcerer.castSpell(hero, map);
            assertNotNull(bolt);
            assertEquals(8, bolt.getDamage());
        }

        @Test
        void arcaneBolt_damageReducedByHeroDEF() {
            sorcerer.moveToCell(map.getCell(3, 0));
            hero.moveToCell(map.getCell(3, 6));
            Projectile bolt = sorcerer.castSpell(hero, map);
            assertNotNull(bolt);
            int expectedDmg = Math.max(1, bolt.getDamage() - hero.getDefensePower());
            int hpBefore = hero.getHealth();
            CombatResolver.applyProjectileDamage(bolt, hero);
            assertEquals(hpBefore - expectedDmg, hero.getHealth());
        }

        // TC-SOR-06 & TC-SOR-08: spec says timer-based teleport independent of LoS;
        // code only teleports when adjacent (dist <= 1).
        @Test
        void teleportChance_is50Percent() {
            assertEquals(0.5f, sorcerer.getTeleportChance(), 0.001f);
        }

        @Test
        void whenAdjacent_sorcererMayTeleport() {
            sorcerer.moveToCell(map.getCell(3, 4));
            int origRow = sorcerer.getRow(), origCol = sorcerer.getCol();
            boolean teleported = false;
            for (int i = 0; i < 50; i++) {
                sorcerer.decideAction(hero, map, session);
                if (sorcerer.getRow() != origRow || sorcerer.getCol() != origCol) {
                    teleported = true;
                    break;
                }
                sorcerer.moveToCell(map.getCell(3, 4)); // reset for next tick
            }
            assertTrue(teleported, "Sorcerer must teleport at least once when adjacent over 50 ticks");
        }

        @Test
        void whenNotAdjacent_sorcererDoesNotTeleport_specViolation() {
            // Spec §2.5.2: teleport should occur independent of adjacency
            // Code: teleport only when dist <= 1 — spec violation
            sorcerer.moveToCell(map.getCell(0, 0));
            int row = sorcerer.getRow(), col = sorcerer.getCol();
            sorcerer.decideAction(hero, map, session);
            assertTrue(sorcerer.getRow() == row && sorcerer.getCol() == col,
                    "Spec §2.5.2 violation: teleport requires adjacency in code, spec says otherwise");
        }

        // TC-SOR-07: teleport destination is walkable and not hero's tile
        @Test
        void teleportDestination_isNotHeroTile() {
            sorcerer.moveToCell(map.getCell(3, 4));
            for (int i = 0; i < 50; i++) {
                sorcerer.decideAction(hero, map, session);
                assertFalse(sorcerer.getRow() == hero.getRow() && sorcerer.getCol() == hero.getCol(),
                        "Sorcerer teleported onto hero's tile");
                sorcerer.moveToCell(map.getCell(3, 4));
            }
        }

        @Test
        void teleportDestination_landOnLegalCell() {
            sorcerer.moveToCell(map.getCell(3, 4));
            for (int i = 0; i < 50; i++) {
                sorcerer.decideAction(hero, map, session);
                GridCell dest = map.getCell(sorcerer.getRow(), sorcerer.getCol());
                assertNotNull(dest, "Sorcerer landed out of bounds");
                sorcerer.moveToCell(map.getCell(3, 4));
            }
        }

        // TC-SOR-09: kill Sorcerer → ring drop mechanism
        @Test
        void killSorcerer_isNoLongerAlive() {
            sorcerer.takeDamage(sorcerer.getHealth());
            assertFalse(sorcerer.isAlive());
        }

        @Test
        void killSorcerer_ringDropFlag_initiallyFalse() {
            assertFalse(sorcerer.isRingDropped());
        }

        @Test
        void killSorcerer_markRingDropped_setsFlag() {
            sorcerer.takeDamage(sorcerer.getHealth());
            sorcerer.markRingDropped();
            assertTrue(sorcerer.isRingDropped());
        }

        @Test
        void markRingDropped_isIdempotent() {
            sorcerer.markRingDropped();
            sorcerer.markRingDropped();
            assertTrue(sorcerer.isRingDropped());
        }

        // TC-SOR-10: Sorcerer never walks — position unchanged when not adjacent
        @Test
        void nonAdjacent_beforeCooldown_sorcererNeverMoves() {
            sorcerer.moveToCell(map.getCell(0, 0));
            int rowBefore = sorcerer.getRow(), colBefore = sorcerer.getCol();
            for (int i = 0; i < sorcerer.getAttackCooldown() - 1; i++) {
                sorcerer.decideAction(hero, map, session);
            }
            assertEquals(rowBefore, sorcerer.getRow(), "Sorcerer must not walk");
            assertEquals(colBefore, sorcerer.getCol(), "Sorcerer must not walk");
        }

        @Test
        void acrossManyTicks_sorcererNeverWalks() {
            sorcerer.moveToCell(map.getCell(0, 0));
            hero.moveToCell(map.getCell(0, 6));
            int startRow = sorcerer.getRow(), startCol = sorcerer.getCol();
            for (int i = 0; i < sorcerer.getAttackCooldown() * 20; i++) {
                sorcerer.decideAction(hero, map, session);
            }
            assertEquals(startRow, sorcerer.getRow(), "Sorcerer must never walk toward hero");
            assertEquals(startCol, sorcerer.getCol(), "Sorcerer must never walk toward hero");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 14. Enemy spawning system (TC-SPW-01 through TC-SPW-06, §2.5)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class EnemySpawning {

        private EnemySpawner spawner;

        @BeforeEach
        void setUp() {
            spawner = new EnemySpawner(session);
            session.setCurrentMode("Play");
        }

        // TC-SPW-01: spawn attempt fires at exactly SPAWN_INTERVAL (9 000 ms)
        @Test
        void spawnInterval_constant_equals9Seconds() {
            assertEquals(9_000L, EnemySpawner.SPAWN_INTERVAL);
        }

        @Test
        void afterOneInterval_timerResets() {
            spawner.update(EnemySpawner.SPAWN_INTERVAL);
            assertEquals(0, spawner.getTimeSinceLastSpawn());
        }

        @Test
        void beforeInterval_timerAccumulates_noSpawn() {
            spawner.update(EnemySpawner.SPAWN_INTERVAL - 1);
            assertEquals(EnemySpawner.SPAWN_INTERVAL - 1, spawner.getTimeSinceLastSpawn());
        }

        @Test
        void partialUpdate_doesNotTriggerSpawn() {
            int enemyCountBefore = session.getActiveEnemyCount();
            spawner.update(EnemySpawner.SPAWN_INTERVAL / 2);
            // Timer hasn't reached interval → no spawn
            assertEquals(enemyCountBefore, session.getActiveEnemyCount());
        }

        // TC-SPW-02: distribution ~60% Knight, ~30% Sorcerer, ~10% nothing (±10% tolerance)
        @Test
        void spawnDistribution_knights60_sorcerers30_nothing10() {
            int knights = 0, sorcerers = 0, nothing = 0;
            int trials = 1000;

            for (int i = 0; i < trials; i++) {
                session.clearEnemies();
                int before = session.getEnemies().size();
                spawner.resetTimer();
                spawner.update(EnemySpawner.SPAWN_INTERVAL);
                int after = session.getEnemies().size();
                if (after > before) {
                    if (session.getEnemies().get(after - 1) instanceof Knight) knights++;
                    else sorcerers++;
                } else {
                    nothing++;
                }
            }

            // Check proportions within ±10% tolerance
            double knightPct   = knights   / (double) trials;
            double sorcPct     = sorcerers / (double) trials;
            double nothingPct  = nothing   / (double) trials;

            assertTrue(knightPct  >= 0.50 && knightPct  <= 0.70,
                    "Knight spawn rate out of range [50%,70%]: " + knightPct);
            assertTrue(sorcPct    >= 0.20 && sorcPct    <= 0.40,
                    "Sorcerer spawn rate out of range [20%,40%]: " + sorcPct);
            assertTrue(nothingPct >= 0.00 && nothingPct <= 0.20,
                    "No-spawn rate out of range [0%,20%]: " + nothingPct);
        }

        // TC-SPW-03: active enemy count never exceeds MAX_ACTIVE_ENEMIES (5)
        @Test
        void maxActiveEnemies_constant_equals5() {
            assertEquals(5, EnemySpawner.MAX_ACTIVE_ENEMIES);
        }

        @Test
        void whenAtCap_spawnIsSkipped() {
            // Manually fill to cap
            for (int i = 0; i < EnemySpawner.MAX_ACTIVE_ENEMIES; i++) {
                Knight k = new Knight();
                k.moveToCell(map.getCell(0, i));
                session.addEnemy(k);
            }
            assertEquals(EnemySpawner.MAX_ACTIVE_ENEMIES, session.getActiveEnemyCount());

            spawner.update(EnemySpawner.SPAWN_INTERVAL);

            assertEquals(EnemySpawner.MAX_ACTIVE_ENEMIES, session.getActiveEnemyCount(),
                    "Enemy count must not exceed MAX_ACTIVE_ENEMIES after spawn attempt");
        }

        @Test
        void activeEnemyCount_neverExceedsCap_overManySpawns() {
            for (int i = 0; i < 20; i++) {
                spawner.update(EnemySpawner.SPAWN_INTERVAL);
                assertTrue(session.getActiveEnemyCount() <= EnemySpawner.MAX_ACTIVE_ENEMIES,
                        "Count exceeded cap at iteration " + i);
            }
        }

        // TC-SPW-04: kill all enemies → next interval spawns a new one
        @Test
        void afterKillingAllEnemies_nextIntervalSpawnsNewEnemy() {
            // Fill enemies then clear them (simulate all killed)
            for (int i = 0; i < EnemySpawner.MAX_ACTIVE_ENEMIES; i++) {
                Knight k = new Knight();
                k.moveToCell(map.getCell(0, i));
                session.addEnemy(k);
            }
            session.clearEnemies();
            assertEquals(0, session.getActiveEnemyCount());

            // Run spawn intervals until an enemy appears (up to 20 tries for 10% nothing chance)
            boolean spawned = false;
            for (int i = 0; i < 20; i++) {
                spawner.update(EnemySpawner.SPAWN_INTERVAL);
                if (session.getActiveEnemyCount() > 0) { spawned = true; break; }
            }
            assertTrue(spawned, "An enemy must spawn within 20 intervals after clearing all enemies");
        }

        // TC-SPW-05: spawn location is an edge cell (row 0, last row, col 0, or last col)
        @Test
        void spawnLocation_isOnMapEdge() {
            int lastRow = map.getHeight() - 1;
            int lastCol = map.getWidth() - 1;

            // Try until a spawn occurs (guard against 10% nothing roll)
            for (int i = 0; i < 20 && session.getEnemies().isEmpty(); i++) {
                session.clearEnemies();
                spawner.update(EnemySpawner.SPAWN_INTERVAL);
            }

            if (!session.getEnemies().isEmpty()) {
                for (var enemy : session.getEnemies()) {
                    int r = enemy.getRow(), c = enemy.getCol();
                    boolean onEdge = r == 0 || r == lastRow || c == 0 || c == lastCol;
                    assertTrue(onEdge,
                            "Enemy spawned at non-edge cell (" + r + "," + c + ")");
                }
            }
        }

        // TC-SPW-06: all edge cells blocked → spawner falls back gracefully (no crash)
        @Test
        void allEdgesCellsBlocked_spawnerDoesNotCrash() {
            // Block all edge cells with walls
            int lastRow = map.getHeight() - 1;
            int lastCol = map.getWidth() - 1;
            for (int c = 0; c < map.getWidth(); c++) {
                map.getCell(0, c).setStaticObject(
                        new StaticObject("Wall", "Block", false, false));
                map.getCell(lastRow, c).setStaticObject(
                        new StaticObject("Wall", "Block", false, false));
            }
            for (int r = 1; r < lastRow; r++) {
                map.getCell(r, 0).setStaticObject(
                        new StaticObject("Wall", "Block", false, false));
                map.getCell(r, lastCol).setStaticObject(
                        new StaticObject("Wall", "Block", false, false));
            }

            assertDoesNotThrow(() -> spawner.update(EnemySpawner.SPAWN_INTERVAL),
                    "Spawner must not crash when no edge cells are available");
        }

        @Test
        void nullSession_spawnerConstructor_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> new EnemySpawner(null));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 15. Combat mechanics & damage formula (TC-CMB-01 through TC-CMB-10, §3, §2.6)
    //     TC-CMB-03 → KnightAI.knightAttack_heroDefReducesDamage_minimumOneApplied
    //     TC-CMB-06 → CombatSystem.heroDies_gameOverTriggered
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class CombatMechanics {

        private Knight knight;

        @BeforeEach
        void setup() {
            knight = new Knight();
            knight.moveToCell(map.getCell(3, 4)); // adjacent to hero at (3,3)
            session.addEnemy(knight);
        }

        // TC-CMB-01: bare-hand attack (no weapon) — formula: max(1, STR/2 - enemy.DEF)
        @Test
        void bareHandAttack_damageMatchesFormulaWithNoWeapon() {
            assertNull(hero.getEquippedWeapon());
            int expectedDmg = Math.max(1,
                    hero.getStrength() / 2 + hero.getAttackPower() - knight.getDefensePower());
            int hpBefore = knight.getHealth();
            CombatResolver.performMeleeAttack(hero, knight);
            assertEquals(hpBefore - expectedDmg, knight.getHealth());
        }

        @Test
        void bareHandAttack_neverDealsZeroDamage() {
            hero.setStrength(0);
            int hpBefore = knight.getHealth();
            CombatResolver.performMeleeAttack(hero, knight);
            assertTrue(knight.getHealth() < hpBefore, "Minimum 1 damage must be dealt");
        }

        // TC-CMB-02: equipped weapon increases damage above bare-hand
        @Test
        void equippedWeapon_increasesDamageAboveBareHand() {
            int bareHandDmg = CombatResolver.calculateFinalDamage(hero, knight);

            Weapon sword = new Weapon("Sword", "Sharp blade", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new EquipAction(sword));

            int armedDmg = CombatResolver.calculateFinalDamage(hero, knight);
            assertTrue(armedDmg > bareHandDmg,
                    "Armed damage must exceed bare-hand damage");
        }

        @Test
        void equippedWeapon_damageFormula_includesWeaponATK() {
            Weapon sword = new Weapon("Sword", "Sharp blade", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new EquipAction(sword));

            int expected = Math.max(1,
                    hero.getStrength() / 2 + hero.getAttackPower() + sword.getDamage()
                    - knight.getDefensePower());
            assertEquals(expected, CombatResolver.calculateFinalDamage(hero, knight));
        }

        // TC-CMB-04: armor reduces incoming damage
        @Test
        void equippingArmor_reducesIncomingDamage() {
            // Use Sorcerer (ATK=8, STR=5) so damage > 1 without armor
            Sorcerer sor = new Sorcerer();
            int dmgWithoutArmor = CombatResolver.calculateFinalDamage(sor, hero);

            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            controller.executeAction(new WearAction(armor));
            int dmgWithArmor = CombatResolver.calculateFinalDamage(sor, hero);

            assertTrue(dmgWithArmor < dmgWithoutArmor,
                    "Armor must reduce incoming damage");
        }

        @Test
        void armor_defBonusAddedToHeroDEF() {
            Wearable armor = new Wearable("Plate Armor", "Heavy plate", 4);
            hero.getInventory().addItem(armor);
            int defBefore = hero.getDefensePower();
            controller.executeAction(new WearAction(armor));
            assertEquals(defBefore + armor.getDefenseBonus(), hero.getDefensePower());
        }

        // TC-CMB-05: ring DEF bonus reflected in reduced incoming damage
        @Test
        void equippingRing_addsDEFBonus() {
            Ring ring = Ring.sorcererDrop(); // +1 DEF
            hero.getInventory().addItem(ring);
            int defBefore = hero.getDefensePower();
            controller.executeAction(new WearAction(ring));
            assertEquals(defBefore + ring.getDefenseBonus(), hero.getDefensePower());
        }

        @Test
        void equippingRing_reducesIncomingDamage() {
            Sorcerer sor = new Sorcerer();
            int dmgWithoutRing = CombatResolver.calculateFinalDamage(sor, hero);

            Ring ring = new Ring("Power Ring", "Magical ring", 3);
            hero.getInventory().addItem(ring);
            controller.executeAction(new WearAction(ring));
            int dmgWithRing = CombatResolver.calculateFinalDamage(sor, hero);

            assertTrue(dmgWithRing < dmgWithoutRing);
        }

        // TC-CMB-07: projectile fired in facing direction
        @Test
        void heroShoot_projectileFiredInFacingDirection() {
            // Hero moves right → facing (0, +1)
            controller.executeAction(new MoveAction(0, 1)); // hero now at (3,4)
            // Move back so hero is at (3,3) facing right
            hero.moveToCell(map.getCell(3, 3));

            controller.heroShoot();

            assertFalse(session.getProjectiles().isEmpty());
            Projectile bolt = session.getProjectiles().get(0);
            assertEquals(hero.getFacingRow(), bolt.getDirectionRow());
            assertEquals(hero.getFacingCol(), bolt.getDirectionCol());
        }

        @Test
        void heroShoot_projectileStartsAdjacentToHero() {
            controller.heroShoot();
            assertFalse(session.getProjectiles().isEmpty());
            Projectile bolt = session.getProjectiles().get(0);
            assertEquals(hero.getRow() + hero.getFacingRow(), bolt.getPositionRow());
            assertEquals(hero.getCol() + hero.getFacingCol(), bolt.getPositionCol());
        }

        // TC-CMB-08: projectile blocked by wall — enemy behind wall takes no damage
        @Test
        void projectile_blockedByWall_enemyBehindSafe() {
            // Hero at (3,3) facing right, wall at (3,5), enemy at (3,6)
            map.getCell(3, 5).setStaticObject(
                    new StaticObject("Wall", "Solid wall", false, false));
            Knight distant = new Knight();
            distant.moveToCell(map.getCell(3, 6));
            session.addEnemy(distant);
            int enemyHpBefore = distant.getHealth();

            GameEngine engine = new GameEngine(controller);
            engine.start(); // sets mode to "Play"
            controller.heroShoot(); // fires right (facingCol=+1 default)

            // Trigger enough frames to run updateProjectiles (PROJECTILE_TICK_INTERVAL=8)
            for (int i = 0; i < 8; i++) {
                engine.update(16);
            }

            assertEquals(enemyHpBefore, distant.getHealth(),
                    "Enemy behind wall must take no damage from blocked projectile");
        }

        // TC-CMB-09: Hero.attack() on enemy outside melee reach (Manhattan > 1) → false
        @Test
        void attackOutsideMeleeReach_returnsFalse() {
            Knight distant = new Knight();
            distant.moveToCell(map.getCell(3, 5)); // Manhattan dist = 2 from hero at (3,3)
            assertFalse(hero.attack(distant),
                    "Hero.attack() must return false when target is beyond melee reach");
        }

        @Test
        void attackOutsideMeleeReach_enemyHPUnchanged() {
            Knight distant = new Knight();
            distant.moveToCell(map.getCell(1, 1)); // far away
            int hpBefore = distant.getHealth();
            hero.attack(distant);
            assertEquals(hpBefore, distant.getHealth());
        }

        // TC-CMB-10: equipping a second weapon automatically unequips the first
        @Test
        void equipSecondWeapon_firstWeaponIsUnequipped() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            Weapon axe   = new Weapon("Axe",   "Heavy", 7);
            hero.getInventory().addItem(sword);
            hero.getInventory().addItem(axe);

            controller.executeAction(new EquipAction(sword)); // equip sword
            assertEquals(sword, hero.getEquippedWeapon());

            controller.executeAction(new EquipAction(axe));  // equip axe
            assertEquals(axe, hero.getEquippedWeapon());
        }

        @Test
        void equipSecondWeapon_firstRemainsInInventory() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            Weapon axe   = new Weapon("Axe",   "Heavy", 7);
            hero.getInventory().addItem(sword);
            hero.getInventory().addItem(axe);

            controller.executeAction(new EquipAction(sword));
            controller.executeAction(new EquipAction(axe));

            // First weapon not discarded — still in inventory
            assertTrue(hero.getInventory().hasItem(sword));
        }

        @Test
        void equipSameWeaponTwice_unequipsIt() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            hero.getInventory().addItem(sword);
            controller.executeAction(new EquipAction(sword)); // equip
            controller.executeAction(new EquipAction(sword)); // toggle unequip
            assertNull(hero.getEquippedWeapon());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 16. Level Editor (TC-ED-01 through TC-ED-10, §4.2)
    //     TC-ED-08 → SessionInit.startGame_switchesModeTo_Play (already covered)
    //     TC-ED-09 → UI-level navigation, not testable at system level
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class LevelEditorTests {

        private LevelEditor editor;

        @BeforeEach
        void setUp() {
            editor = new LevelEditor(map);
        }

        // TC-ED-01: place item on walkable floor tile → appears on cell
        @Test
        void placeItem_onWalkableCell_appearsOnCell() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            boolean placed = editor.placeObject(2, 2, sword);
            assertTrue(placed);
            assertTrue(map.getCell(2, 2).getObjects().contains(sword));
        }

        @Test
        void placeStaticObject_onEmptyCell_appearsAsStaticObject() {
            StaticObject crate = new StaticObject("Crate", "Wooden crate", false, true);
            boolean placed = editor.placeObject(1, 1, crate);
            assertTrue(placed);
            assertEquals(crate, map.getCell(1, 1).getStaticObject());
        }

        // TC-ED-02: place item on wall tile → rejected
        @Test
        void placeItem_onWallCell_isRejected() {
            StaticObject wall = new StaticObject("Wall", "Solid wall", false, false);
            map.getCell(0, 0).setStaticObject(wall);
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            boolean placed = editor.placeObject(0, 0, sword);
            assertFalse(placed);
            assertFalse(map.getCell(0, 0).getObjects().contains(sword));
        }

        @Test
        void placeImpassableStaticObject_onOccupiedCell_isRejected() {
            StaticObject wall1 = new StaticObject("Wall", "First wall", false, false);
            StaticObject wall2 = new StaticObject("Wall2", "Second wall", false, false);
            map.getCell(0, 0).setStaticObject(wall1);
            boolean placed = editor.placeObject(0, 0, wall2);
            assertFalse(placed);
            assertEquals(wall1, map.getCell(0, 0).getStaticObject());
        }

        // TC-ED-03 / TC-ED-04: save to JSON then reload restores objects exactly
        @Test
        void saveAndLoad_roundTrip_restoresWeaponOnCell() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            map.getCell(2, 2).addObject(sword);

            MapSerializer serializer = new MapSerializer();
            MapDeserializer deserializer = new MapDeserializer();
            String json = serializer.mapToJson(map);
            Map restored = deserializer.loadMapFromJson(json);

            assertNotNull(restored);
            assertFalse(restored.getCell(2, 2).getObjects().isEmpty(),
                    "Weapon on cell must be restored after save/load");
        }

        @Test
        void saveAndLoad_roundTrip_restoresStaticObject() {
            StaticObject crate = new StaticObject("Crate", "Wooden", false, true);
            map.getCell(1, 1).setStaticObject(crate);

            MapSerializer serializer = new MapSerializer();
            String json = serializer.mapToJson(map);
            Map restored = new MapDeserializer().loadMapFromJson(json);

            assertNotNull(restored);
            assertNotNull(restored.getCell(1, 1).getStaticObject(),
                    "StaticObject must be restored after save/load");
        }

        @Test
        void mapToJson_producesNonEmptyString() {
            String json = new MapSerializer().mapToJson(map);
            assertNotNull(json);
            assertFalse(json.isBlank());
        }

        // TC-ED-05: addRandomItemsPerSpec → exactly 5 floor items + 1 hidden item
        @Test
        void addRandomItemsPerSpec_places5FloorItems() {
            RandomItemGenerator.PopulateResult result = editor.addRandomItemsPerSpec();
            assertEquals(5, result.floorItemsPlaced,
                    "populatePerDesignDocument must place exactly 5 floor items");
        }

        @Test
        void addRandomItemsPerSpec_placesOneHiddenItem() {
            RandomItemGenerator.PopulateResult result = editor.addRandomItemsPerSpec();
            assertTrue(result.hiddenItemPlaced,
                    "populatePerDesignDocument must place 1 hidden searchable item");
        }

        @Test
        void addRandomItemsPerSpec_nullMap_returnsZeros() {
            RandomItemGenerator gen = new RandomItemGenerator();
            RandomItemGenerator.PopulateResult result = gen.populatePerDesignDocument(null);
            assertEquals(0, result.floorItemsPlaced);
            assertFalse(result.hiddenItemPlaced);
        }

        // TC-ED-06: random weapon ATK in [3,7], armor DEF in [1,3]
        @Test
        void randomWeapon_ATK_isWithinSpecRange() {
            RandomItemGenerator gen = new RandomItemGenerator();
            for (int i = 0; i < 200; i++) {
                Map testMap = new Map(10, 10);
                gen.generateRandomItems(testMap, 1);
                for (int r = 0; r < 10; r++) {
                    for (int c = 0; c < 10; c++) {
                        for (var obj : testMap.getCell(r, c).getObjects()) {
                            if (obj instanceof Weapon w) {
                                assertTrue(w.getDamage() >= 3 && w.getDamage() <= 7,
                                        "Weapon ATK out of [3,7]: " + w.getDamage());
                            }
                        }
                    }
                }
            }
        }

        @Test
        void randomArmor_DEF_isWithinSpecRange() {
            RandomItemGenerator gen = new RandomItemGenerator();
            for (int i = 0; i < 200; i++) {
                Map testMap = new Map(10, 10);
                gen.generateRandomItems(testMap, 1);
                for (int r = 0; r < 10; r++) {
                    for (int c = 0; c < 10; c++) {
                        for (var obj : testMap.getCell(r, c).getObjects()) {
                            if (obj instanceof Wearable w && !(obj instanceof Ring)) {
                                assertTrue(w.getDefenseBonus() >= 1 && w.getDefenseBonus() <= 3,
                                        "Armor DEF out of [1,3]: " + w.getDefenseBonus());
                            }
                        }
                    }
                }
            }
        }

        // TC-ED-07: clearMap removes all placed objects
        @Test
        void clearMap_removesAllItemsFromCells() {
            Weapon sword = new Weapon("Sword", "Sharp", 5);
            map.getCell(2, 2).addObject(sword);
            map.getCell(3, 3).setStaticObject(
                    new StaticObject("Crate", "Wooden", false, true));

            editor.clearMap();

            for (int r = 0; r < map.getHeight(); r++) {
                for (int c = 0; c < map.getWidth(); c++) {
                    GridCell cell = map.getCell(r, c);
                    assertNull(cell.getStaticObject(), "Cell (" + r + "," + c + ") still has staticObject");
                    assertNull(cell.getOccupyingCharacter());
                    assertTrue(cell.getObjects().isEmpty(), "Cell (" + r + "," + c + ") still has objects");
                }
            }
        }

        @Test
        void clearMap_canBeUndone() {
            editor.placeObject(2, 2, new Weapon("Sword", "Sharp", 5));
            editor.clearMap();
            assertTrue(editor.canUndo());
            editor.undo();
            assertFalse(map.getCell(2, 2).getObjects().isEmpty());
        }

        // TC-ED-10: container roll ≥ 8 adds loot; roll < 8 stops
        @Test
        void containerLootRoll_mayAddItemsInsideContainer() {
            // Over 100 StaticContainer placements, the roll (1-10 >= 8 means ~30% chance)
            // means at least one container should get loot
            RandomItemGenerator gen = new RandomItemGenerator();
            boolean anyLoot = false;
            for (int i = 0; i < 100 && !anyLoot; i++) {
                Map testMap = new Map(10, 10);
                RandomItemGenerator.PopulateResult result = gen.populatePerDesignDocument(testMap);
                if (result.containerLootAdded > 0) anyLoot = true;
            }
            assertTrue(anyLoot,
                    "Roll >= 8 must occasionally add loot to containers over 100 runs");
        }

        @Test
        void containerLootRoll_neverExceedsReasonableCount() {
            // Each roll adds at most ~log(0.3) / log(0.3) items on average.
            // Pathological infinite loop would fail here; sanity check only.
            RandomItemGenerator gen = new RandomItemGenerator();
            for (int i = 0; i < 50; i++) {
                Map testMap = new Map(10, 10);
                RandomItemGenerator.PopulateResult result = gen.populatePerDesignDocument(testMap);
                assertTrue(result.containerLootAdded >= 0 && result.containerLootAdded <= 50,
                        "Container loot count suspiciously large: " + result.containerLootAdded);
            }
        }

        // PlacementValidator unit-level edge cases
        @Test
        void validator_nullObject_returnsFalse() {
            com.kurawler.editor.PlacementValidator v = new com.kurawler.editor.PlacementValidator();
            assertFalse(v.isValidPlacement(null, map.getCell(0, 0)));
        }

        @Test
        void validator_nullCell_returnsFalse() {
            com.kurawler.editor.PlacementValidator v = new com.kurawler.editor.PlacementValidator();
            assertFalse(v.isValidPlacement(new Weapon("W", "", 1), null));
        }
    }
}
