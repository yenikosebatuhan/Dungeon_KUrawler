package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.ShadowCloneScroll;
import com.kurawler.model.object.StaticObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShadowClone spec compliance (Phase 2 spec, page 16-17).
 *
 * KNOWN FAILING TESTS (bug-catching):
 *   cloneMovesOppositeToHero*  — current code moves same direction, spec says opposite
 *   cloneTakesNoDamage         — current code mirrors half-damage, spec says no damage
 *
 * These tests are intentionally failing until the bugs are fixed.
 */
class ShadowCloneTest {

    private Map map;
    private Hero hero;
    private ShadowClone clone;

    @BeforeEach
    void setUp() {
        map = new Map(5, 5);
        hero = new Hero("TestHero");
        // Hero at (2,2), clone at (2,2) — clone is ethereal, no cell occupancy
        hero.moveToCell(map.getCell(2, 2));
        clone = new ShadowClone(2, 2, map);
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
        assertEquals(0, new ShadowCloneScroll().getHealAmount());
    }

    // --- Opposite movement (spec: clone moves in OPPOSITE direction of hero) ---
    // BUG: current code uses +deltaCol/+deltaRow instead of -deltaCol/-deltaRow
    // These tests will FAIL until ShadowClone.onHeroMoved is fixed.

    @Test
    void cloneMovesOppositeToHeroRight() {
        // Hero moves RIGHT (deltaCol=+1) → clone must move LEFT (col 2→1)
        int before = clone.getCol();
        hero.move("RIGHT", map, null);
        assertEquals(before - 1, clone.getCol(),
                "BUG: clone moves same direction; spec requires opposite");
    }

    @Test
    void cloneMovesOppositeToHeroDown() {
        // Hero moves DOWN (deltaRow=+1) → clone must move UP (row 2→1)
        int before = clone.getRow();
        hero.move("DOWN", map, null);
        assertEquals(before - 1, clone.getRow(),
                "BUG: clone moves same direction; spec requires opposite");
    }

    @Test
    void cloneMovesOppositeToHeroLeft() {
        // Hero moves LEFT (deltaCol=-1) → clone must move RIGHT (col 2→3)
        int before = clone.getCol();
        hero.move("LEFT", map, null);
        assertEquals(before + 1, clone.getCol(),
                "BUG: clone moves same direction; spec requires opposite");
    }

    @Test
    void cloneMovesOppositeToHeroUp() {
        // Hero moves UP (deltaRow=-1) → clone must move DOWN (row 2→3)
        int before = clone.getRow();
        hero.move("UP", map, null);
        assertEquals(before + 1, clone.getRow(),
                "BUG: clone moves same direction; spec requires opposite");
    }

    // --- Blocked opposite movement (spec: "will try but will not be able to") ---

    @Test
    void cloneStaysWhenOppositeTargetIsOutOfBounds() {
        // Clone at row=0; hero moves UP (deltaRow=-1) → opposite is DOWN (row=-1) → out of bounds → stays
        clone = new ShadowClone(0, 2, map);
        hero.addObserver(clone);
        hero.removeObserver(this.clone);

        int before = clone.getRow();
        hero.move("UP", map, null);
        assertEquals(before, clone.getRow());
    }

    @Test
    void cloneBlockedByWallInOppositeDirection() {
        // Wall at (2,1); hero moves RIGHT (delta=+1) → clone goes LEFT to (2,1) → wall → stays at (2,2)
        StaticObject wall = new StaticObject("Wall", "Stone wall", false, false);
        map.getCell(2, 1).setStaticObject(wall);

        int before = clone.getCol();
        hero.move("RIGHT", map, null);
        assertEquals(before, clone.getCol());
    }

    // --- No damage (spec: "does not receive any damage from the attacks") ---
    // BUG: current code applies half-damage via onHeroDamageTaken.
    // These tests will FAIL until the bug is fixed.

    @Test
    void cloneTakesNoDamageWhenHeroIsHit() {
        int before = clone.getHealth();
        hero.takeDamage(10);
        assertEquals(before, clone.getHealth(),
                "BUG: clone takes half-damage; spec says clone receives no damage");
    }

    @Test
    void cloneTakesNoDamageOnLargeHit() {
        int before = clone.getHealth();
        hero.takeDamage(hero.getMaxHealth());
        assertEquals(before, clone.getHealth(),
                "BUG: clone takes damage; spec says clone is invulnerable");
    }

    // --- Dead / removed clone ---

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
}
