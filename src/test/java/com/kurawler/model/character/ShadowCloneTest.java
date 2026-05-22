package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.ShadowCloneScroll;
import com.kurawler.model.object.StaticObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShadowCloneTest {

    private Map map;
    private Hero hero;
    private ShadowClone clone;

    @BeforeEach
    void setUp() {
        map = new Map(5, 5);
        hero = new Hero("TestHero");
        hero.moveToCell(map.getCell(2, 2));
        clone = new ShadowClone(2, 0, map);
        hero.addObserver(clone);
    }

    // --- Registration ---

    @Test
    void cloneRegisteredAsObserver() {
        assertTrue(hero.getObservers().contains(clone));
    }

    @Test
    void shadowCloneScrollHasCorrectName() {
        assertEquals("Shadow Clone Scroll", new ShadowCloneScroll().getName());
    }

    @Test
    void shadowCloneScrollHealAmountIsZero() {
        // Clone summon is handled externally; scroll itself grants no heal
        assertEquals(0, new ShadowCloneScroll().getHealAmount());
    }

    // --- Movement (same delta as hero) ---

    @Test
    void cloneFollowsHeroRight() {
        int before = clone.getCol();
        hero.move("RIGHT", map, null);  // delta col = +1
        assertEquals(before + 1, clone.getCol());
    }

    @Test
    void cloneFollowsHeroDown() {
        int before = clone.getRow();
        hero.move("DOWN", map, null);   // delta row = +1
        assertEquals(before + 1, clone.getRow());
    }

    // --- Blocked movement ---

    @Test
    void cloneStaysWhenTargetIsOutOfBounds() {
        // Clone at col=0; LEFT delta would take clone to col=-1 (null cell)
        int before = clone.getCol();
        hero.move("LEFT", map, null);   // delta col = -1
        assertEquals(before, clone.getCol());
    }

    @Test
    void cloneBlockedByWall() {
        StaticObject wall = new StaticObject("Wall", "Stone wall", false, false);
        map.getCell(2, 1).setStaticObject(wall);

        int before = clone.getCol();
        hero.move("RIGHT", map, null);  // clone target (2,1) is a wall
        assertEquals(before, clone.getCol());
    }

    // --- Despawn (dead clone) ---

    @Test
    void deadCloneDoesNotMove() {
        clone.takeDamage(clone.getHealth());
        assertFalse(clone.isAlive());

        int before = clone.getCol();
        hero.move("RIGHT", map, null);
        assertEquals(before, clone.getCol());
    }

    @Test
    void removedCloneDoesNotMove() {
        hero.removeObserver(clone);

        int before = clone.getCol();
        hero.move("RIGHT", map, null);
        assertEquals(before, clone.getCol());
    }

    // --- Damage mirroring ---

    @Test
    void cloneTakesHalfHeroDamage() {
        int before = clone.getHealth();
        hero.takeDamage(10);            // clone should take max(1, 10/2) = 5
        assertEquals(before - 5, clone.getHealth());
    }

    @Test
    void cloneTakesMinimumOneDamage() {
        int before = clone.getHealth();
        hero.takeDamage(1);
        assertTrue(clone.getHealth() < before);
    }
}
