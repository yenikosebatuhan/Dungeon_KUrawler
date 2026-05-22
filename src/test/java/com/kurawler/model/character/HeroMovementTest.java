package com.kurawler.model.character;

import com.kurawler.engine.CollisionDetector;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.StaticObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for the required features described in Phase II – A7:
 *   1. Hero movement (all directions, edge inputs)
 *   2. Obstacle (wall) collision
 *   3. Shadow Clone movement blocking (ShadowClone is a NEW required feature)
 *   4. Map-bounds enforcement
 */
class HeroMovementTest {

    // 5 × 5 map; valid rows 0-4, valid cols 0-4
    private static final int ROWS = 5;
    private static final int COLS = 5;

    private Map map;
    private Hero hero;

    @BeforeEach
    void setUp() {
        map  = new Map(COLS, ROWS);
        hero = new Hero("TestHero");
        hero.moveToCell(map.getCell(2, 2)); // center of the grid
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Hero movement tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class HeroMovement {

        @Test
        void moveUp_succeeds() {
            assertTrue(hero.move("UP", map, null));
        }

        @Test
        void moveDown_succeeds() {
            assertTrue(hero.move("DOWN", map, null));
        }

        @Test
        void moveLeft_succeeds() {
            assertTrue(hero.move("LEFT", map, null));
        }

        @Test
        void moveRight_succeeds() {
            assertTrue(hero.move("RIGHT", map, null));
        }

        @Test
        void moveUp_updatesRowCorrectly() {
            hero.move("UP", map, null);
            assertEquals(1, hero.getRow());
            assertEquals(2, hero.getCol());
        }

        @Test
        void moveRight_updatesColCorrectly() {
            hero.move("RIGHT", map, null);
            assertEquals(2, hero.getRow());
            assertEquals(3, hero.getCol());
        }

        @Test
        void move_returnsFalse_forNullDirection() {
            assertFalse(hero.move(null, map, null));
        }

        @Test
        void move_returnsFalse_forNullMap() {
            assertFalse(hero.move("UP", null, null));
        }

        @Test
        void move_returnsFalse_forUnknownDirection() {
            assertFalse(hero.move("JUMP", map, null));
        }

        @Test
        void move_isCaseInsensitive() {
            assertTrue(hero.move("up", map, null));
            assertTrue(hero.move("Down", map, null)); // hero is now at (1,2); DOWN → (2,2)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Obstacle collision tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ObstacleCollision {

        @Test
        void moveIntoWall_returnsFalse() {
            placeWall(1, 2); // directly above hero
            assertFalse(hero.move("UP", map, null));
        }

        @Test
        void heroPosition_unchanged_whenBlockedByWall() {
            placeWall(1, 2);
            hero.move("UP", map, null);
            assertEquals(2, hero.getRow());
            assertEquals(2, hero.getCol());
        }

        @Test
        void moveIntoPassableObject_succeeds() {
            // A passable decoration must not block movement
            StaticObject bush = new StaticObject("Bush", "Harmless", true, false);
            map.getCell(1, 2).setStaticObject(bush);
            assertTrue(hero.move("UP", map, null));
        }

        @Test
        void wallOnLeft_doesNotAffect_upwardMove() {
            placeWall(2, 1); // left of hero, not in the UP path
            assertTrue(hero.move("UP", map, null));
        }

        private void placeWall(int row, int col) {
            StaticObject wall = new StaticObject("Wall", "Solid wall", false, false);
            map.getCell(row, col).setStaticObject(wall);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Shadow Clone movement blocking tests  (Phase II – A7, NEW feature)
    //
    // Required spec:
    //   • ShadowClone occupies a grid cell → hero cannot enter that cell.
    //   • ShadowClone moves in the direction opposite to the hero's last move.
    //   • ShadowClone expires after LIFETIME_MS milliseconds.
    //   • Despawned clone no longer blocks movement.
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ShadowCloneBlocking {

        @Test
        void shadowClone_blocksHeroMovement() {
            ShadowClone clone = spawnCloneAt(1, 2); // directly above hero
            assertFalse(hero.move("UP", map, null));
        }

        @Test
        void heroPosition_unchanged_whenBlockedByClone() {
            spawnCloneAt(1, 2);
            hero.move("UP", map, null);
            assertEquals(2, hero.getRow());
            assertEquals(2, hero.getCol());
        }

        @Test
        void shadowClone_noLongerBlocks_afterDespawn() {
            ShadowClone clone = spawnCloneAt(1, 2);
            assertFalse(hero.move("UP", map, null)); // blocked

            clone.despawn();                         // clone removes itself from cell

            assertTrue(hero.move("UP", map, null)); // now free
        }

        @Test
        void shadowClone_movesReverse_heroMovesUp() {
            // hero UP  →  clone should go DOWN
            ShadowClone clone = spawnCloneAt(2, 3);
            clone.moveReverse("UP", map);
            assertEquals(3, clone.getRow()); // moved down one row
            assertEquals(3, clone.getCol());
        }

        @Test
        void shadowClone_movesReverse_heroMovesDown() {
            // hero DOWN  →  clone should go UP
            ShadowClone clone = spawnCloneAt(2, 3);
            clone.moveReverse("DOWN", map);
            assertEquals(1, clone.getRow()); // moved up one row
        }

        @Test
        void shadowClone_movesReverse_heroMovesLeft() {
            // hero LEFT  →  clone should go RIGHT
            // Clone at col 0; reverse target is col 1, which is free (hero is at col 2)
            ShadowClone clone = spawnCloneAt(2, 0);
            clone.moveReverse("LEFT", map);
            assertEquals(1, clone.getCol()); // moved right one col
        }

        @Test
        void shadowClone_movesReverse_heroMovesRight() {
            // hero RIGHT  →  clone should go LEFT
            // Clone at col 4; reverse target is col 3, which is free (hero is at col 2)
            ShadowClone clone = spawnCloneAt(2, 4);
            clone.moveReverse("RIGHT", map);
            assertEquals(3, clone.getCol()); // moved left one col
        }

        @Test
        void shadowClone_staysInPlace_whenReverseMoveIsOutOfBounds() {
            // Clone at bottom row: reversing UP (= DOWN) would go out of bounds
            ShadowClone clone = spawnCloneAt(4, 3);
            clone.moveReverse("UP", map); // reverse is DOWN → row 5, out of bounds
            assertEquals(4, clone.getRow()); // must not move
        }

        @Test
        void shadowClone_isNotExpired_initially() {
            ShadowClone clone = new ShadowClone(hero);
            assertFalse(clone.isExpired());
        }

        @Test
        void shadowClone_isExpired_afterFullLifetime() {
            ShadowClone clone = new ShadowClone(hero);
            clone.tick(ShadowClone.LIFETIME_MS);
            assertTrue(clone.isExpired());
        }

        @Test
        void shadowClone_isNotExpired_beforeFullLifetime() {
            ShadowClone clone = new ShadowClone(hero);
            clone.tick(ShadowClone.LIFETIME_MS - 1);
            assertFalse(clone.isExpired());
        }

        @Test
        void shadowClone_remainingMs_decreases_onTick() {
            ShadowClone clone = new ShadowClone(hero);
            clone.tick(1_000L);
            assertEquals(ShadowClone.LIFETIME_MS - 1_000L, clone.getRemainingMs());
        }

        /** Convenience: place a fresh clone at (row, col) on the shared map. */
        private ShadowClone spawnCloneAt(int row, int col) {
            ShadowClone clone = new ShadowClone(hero);
            clone.moveToCell(map.getCell(row, col));
            return clone;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Map bounds tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class MapBounds {

        @Test
        void moveUp_returnsFalse_atTopBoundary() {
            hero.moveToCell(map.getCell(0, 2));
            assertFalse(hero.move("UP", map, null));
        }

        @Test
        void moveDown_returnsFalse_atBottomBoundary() {
            hero.moveToCell(map.getCell(4, 2));
            assertFalse(hero.move("DOWN", map, null));
        }

        @Test
        void moveLeft_returnsFalse_atLeftBoundary() {
            hero.moveToCell(map.getCell(2, 0));
            assertFalse(hero.move("LEFT", map, null));
        }

        @Test
        void moveRight_returnsFalse_atRightBoundary() {
            hero.moveToCell(map.getCell(2, 4));
            assertFalse(hero.move("RIGHT", map, null));
        }

        @Test
        void collisionDetector_returnsTrue_negativeRow() {
            CollisionDetector cd = new CollisionDetector(map);
            assertTrue(cd.checkCollision(-1, 0));
        }

        @Test
        void collisionDetector_returnsTrue_negativeCol() {
            CollisionDetector cd = new CollisionDetector(map);
            assertTrue(cd.checkCollision(0, -1));
        }

        @Test
        void collisionDetector_returnsTrue_rowBeyondHeight() {
            CollisionDetector cd = new CollisionDetector(map);
            assertTrue(cd.checkCollision(ROWS, 0));
        }

        @Test
        void collisionDetector_returnsTrue_colBeyondWidth() {
            CollisionDetector cd = new CollisionDetector(map);
            assertTrue(cd.checkCollision(0, COLS));
        }

        @Test
        void collisionDetector_returnsFalse_forOpenInBoundsCell() {
            CollisionDetector cd = new CollisionDetector(map);
            assertFalse(cd.checkCollision(0, 0)); // no hero, no wall here
        }

        @Test
        void collisionDetector_returnsTrue_forWallInBounds() {
            StaticObject wall = new StaticObject("Wall", "Solid", false, false);
            map.getCell(0, 0).setStaticObject(wall);
            CollisionDetector cd = new CollisionDetector(map);
            assertTrue(cd.checkCollision(0, 0));
        }
    }
}
