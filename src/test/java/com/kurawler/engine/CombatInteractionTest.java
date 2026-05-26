package com.kurawler.engine;

import com.kurawler.action.AttackAction;
import com.kurawler.controller.GameController;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;
import com.kurawler.model.object.Wearable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CombatInteractionTest {

    private GameSession session;
    private GameController controller;
    private Hero hero;
    private Map map;

    /**
     * Initializes a fresh game environment before each test.
     *
     * @requires none
     * @modifies session, controller, hero, map
     * @effects Creates a 5x5 map, places the hero at (2,2), and instantiates the controller.
     */
    @BeforeEach
    public void setUp() {
        map = new Map(5, 5);
        for(int r = 0; r < 5; r++) {
            for(int c = 0; c < 5; c++) {
                map.getCell(r, c).setStaticObject(new com.kurawler.model.object.StaticObject("Floor", "Floor", true, false));
            }
        }
        session = new GameSession(map);
        hero = new Hero("Test Hero");
        hero.setPosition(2, 2);
        hero.setCurrentCell(map.getCell(2, 2));
        session.setHero(hero);
        controller = new GameController(session);
    }

    /**
     * Tests that attacking an enemy successfully deducts 1 energy from the hero.
     *
     * @requires hero has sufficient energy
     * @modifies hero's energy state
     * @effects Asserts that resolving an AttackAction reduces energy exactly by 1.
     */
    @Test
    public void testAttackActionConsumesEnergy() {
        Knight knight = new Knight();
        int initialEnergy = hero.getEnergy();

        AttackAction attack = new AttackAction(hero, knight);
        attack.resolve(session);

        assertEquals(initialEnergy - 1, hero.getEnergy(), "Hero should lose exactly 1 energy per attack.");
    }

    /**
     * Tests that the hero cannot attack if they have 0 energy.
     *
     * @requires hero has 0 energy
     * @modifies none
     * @effects Asserts that the AttackAction returns an empty effect list and deals no damage.
     */
    @Test
    public void testAttackActionFailsWithoutEnergy() {
        Knight knight = new Knight();
        int initialKnightHp = knight.getHealth();
        hero.setEnergy(0);

        AttackAction attack = new AttackAction(hero, knight);
        List<Effect> effects = attack.resolve(session);

        assertTrue(effects.isEmpty(), "No effects should be produced without energy.");
        assertEquals(initialKnightHp, knight.getHealth(), "Knight should take no damage.");
    }

    /**
     * Tests that attacking an empty cell via the controller fails safely.
     *
     * @requires target cell is empty
     * @modifies none
     * @effects Asserts that controller.attackEnemyAt returns false and consumes no energy.
     */
    @Test
    public void testControllerAttackEmptyCell() {
        int initialEnergy = hero.getEnergy();
        boolean success = controller.attackEnemyAt(2, 3); // Empty cell

        assertFalse(success, "Attacking an empty cell should return false.");
        assertEquals(initialEnergy, hero.getEnergy(), "Energy should not be consumed for invalid attacks.");
    }

    /**
     * Tests that defeating an enemy removes them from the map (ghost sprite cleanup).
     *
     * @requires enemy's health is low enough to be killed in one hit
     * @modifies map, session enemy list
     * @effects Asserts that the enemy is removed from the cell and the session after reaching 0 HP.
     */
    @Test
    public void testDefeatedEnemyRemovedFromCell() {
        Knight knight = new Knight();
        knight.setHealth(1); // 1 HP left
        GridCell targetCell = map.getCell(2, 3);
        knight.setPosition(2, 3);
        knight.setCurrentCell(targetCell);
        targetCell.setOccupyingCharacter(knight);
        session.addEnemy(knight);

        // Hero's STR is at least 8, so damage will exceed 1
        controller.attackEnemyAt(2, 3);

        assertFalse(knight.isAlive(), "Knight should be dead.");
        assertNull(targetCell.getOccupyingCharacter(), "Ghost sprite cleanup: Cell should be empty.");
        assertFalse(session.getEnemies().contains(knight), "Enemy should be removed from session list.");
    }

    /**
     * Tests the specific loot drop rule for Sorcerers.
     *
     * @requires a Sorcerer with 1 HP
     * @modifies target cell's object list
     * @effects Asserts that defeating a Sorcerer spawns exactly one "Sorcerer's Ring" on their cell.
     */
    @Test
    public void testSorcererRingDrop() {
        Sorcerer sorcerer = new Sorcerer();
        sorcerer.setHealth(1);
        GridCell targetCell = map.getCell(2, 3);
        sorcerer.setPosition(2, 3);
        sorcerer.setCurrentCell(targetCell);
        targetCell.setOccupyingCharacter(sorcerer);
        session.addEnemy(sorcerer);

        controller.attackEnemyAt(2, 3);

        assertFalse(sorcerer.isAlive(), "Sorcerer should be dead.");
        
        // Check for the ring
        boolean ringFound = targetCell.getObjects().stream()
                .anyMatch(obj -> obj instanceof Wearable && obj.getName().equals("Sorcerer's Ring"));
        
        assertTrue(ringFound, "A Sorcerer's Ring must be dropped on the cell.");
        assertTrue(sorcerer.isRingDropped(), "Sorcerer state should mark ring as dropped.");
    }

    /**
     * Tests that the weapon's damage is correctly added to the hero's total damage output.
     * * @requires Hero has a weapon equipped with 10 damage.
     * @modifies target knight's health
     * @effects Asserts that the hero deals higher damage when a weapon is equipped.
     */
    @Test
    public void testEquippedWeaponAddsDamage() {
        Knight knight = new Knight();
        knight.setDefensePower(2); // Knight DEF nerf sonrası 2
        hero.setStrength(10); // STR 10 -> base 5
        
        com.kurawler.model.object.Weapon sword = new com.kurawler.model.object.Weapon("Steel Sword", "Sharp", 10);
        hero.setEquippedWeapon(sword);
        
        // Formül: (STR 5 + ATK 0 + WPN 10) - DEF 2 = 13
        // HP 20 - 13 = 7
        int initialHp = knight.getHealth();
        controller.attackEnemyAt(knight.getRow(), knight.getCol());
        
        assertEquals(initialHp - 13, knight.getHealth(), "Damage should be 13.");
    }

    @Test
    public void testDefensePowerReducesDamage() {
        Knight knight = new Knight();
        knight.setDefensePower(2); // Knight DEF nerf sonrası 2
        hero.setStrength(10); // Base 5
        hero.setEquippedWeapon(null);
        
        // Formül: (STR 5 + ATK 0) - DEF 2 = 3
        // HP 20 - 3 = 17
        int initialHp = knight.getHealth();
        controller.attackEnemyAt(knight.getRow(), knight.getCol());
        
        assertEquals(initialHp - 3, knight.getHealth(), "Damage should be 3.");
    }
}