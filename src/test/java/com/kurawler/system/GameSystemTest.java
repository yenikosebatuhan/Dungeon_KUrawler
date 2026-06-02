package com.kurawler.system;

import com.kurawler.action.AttackAction;
import com.kurawler.action.DiscardAction;
import com.kurawler.action.EatAction;
import com.kurawler.action.MoveAction;
import com.kurawler.action.ReadAction;
import com.kurawler.action.SearchAction;
import com.kurawler.action.TakeAction;
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
import com.kurawler.model.object.Scroll;
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
}
