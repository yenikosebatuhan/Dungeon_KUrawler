package com.kurawler.action;

import com.kurawler.controller.GameController;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TakeItemTest {

    private GameSession session;
    private GameController controller;
    private Hero hero;
    private Map map;

    @BeforeEach
    public void setUp() {
        map = new Map(5, 5);
        session = new GameSession(map);
        hero = new Hero("Test Hero");
        hero.setPosition(0, 0);
        hero.setCurrentCell(map.getCell(0, 0));
        session.setHero(hero);
        controller = new GameController(session);
    }

    @Test
    public void testSuccessfulPickupAndRemoveFromMap() {
        // Acceptance Criteria: Item removed from map and added to inventory
        Item sword = new Weapon("Sword", "Sharp", 5);
        GridCell cell = map.getCell(0, 0);
        cell.addObject(sword);

        controller.interactWithObject(0, 0, sword); 
        assertTrue(hero.getInventory().hasItem(sword), "Item should be in inventory.");
        assertFalse(cell.getObjects().contains(sword), "Item should be removed from the map cell.");
    }

    @Test
    public void testInventoryFullPreventsPickup() {
        // Acceptance Criteria: If inventory is full, item NOT picked up
        GridCell cell = map.getCell(0, 0);
        
        // Fill inventory (8/8)
        for (int i = 0; i < 8; i++) {
            hero.getInventory().addItem(new Weapon("Dummy" + i, "Filler", 1));
        }

        Item extraItem = new Weapon("Extra", "Should not be picked up", 1);
        cell.addObject(extraItem);

        // Try to take
        controller.interactWithObject(0, 0, extraItem);

        assertFalse(hero.getInventory().hasItem(extraItem), "Item should NOT be added to full inventory.");
        assertTrue(cell.getObjects().contains(extraItem), "Item should remain on the map cell.");
    }

    @Test
    public void testRepOkIntegrityAfterTake() {
        // Acceptance Criteria: repOk() passes after every take action
        Item item = new Weapon("Sword", "Sharp", 5);
        GridCell cell = map.getCell(0, 0);
        cell.addObject(item);

        controller.interactWithObject(0, 0, item);

        assertTrue(hero.getInventory().repOk(), "Inventory representation invariant (repOk) must hold.");
    }
}