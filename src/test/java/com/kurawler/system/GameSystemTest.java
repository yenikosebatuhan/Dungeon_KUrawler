package com.kurawler.system;

import com.kurawler.action.AttackAction;
import com.kurawler.action.BreakAction;
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
}
