package com.kurawler.model.character;

import com.kurawler.engine.CollisionDetector;
import com.kurawler.model.Map;
import com.kurawler.model.object.StaticObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for Phase II required features:
 *   1. Hero movement (all directions, edge inputs)
 *   2. Obstacle (wall) collision blocking hero
 *   3. Shadow Clone movement — clone moves opposite to hero, blocked by walls/bounds
 *   4. Map-bounds enforcement
 *
 * ShadowClone design (Phase II A7):
 *   - Ethereal: uses setPosition(), does NOT occupy GridCells, does not block hero.
 *   - Implements HeroObserver — reacts to onHeroMoved() by moving in reverse.
 *   - Expires after LIFETIME_MS milliseconds via tick().
 */
class HeroMovementTest {

    private static final int ROWS = 5;
    private static final int COLS = 5;

    private Map map;
    private Hero hero;

    @BeforeEach
    void setUp() {
        map  = new Map(COLS, ROWS);
        hero = new Hero("TestHero");
        hero.moveToCell(map.getCell(2, 2)); // center
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
        void moveUp_updatesRow() {
            hero.move("UP", map, null);
            assertEquals(1, hero.getRow());
            assertEquals(2, hero.getCol());
        }

        @Test
        void moveRight_updatesCol() {
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
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Obstacle collision tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ObstacleCollision {

        @Test
        void moveIntoWall_returnsFalse() {
            placeWall(1, 2);
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
            StaticObject bush = new StaticObject("Bush", "Harmless", true, false);
            map.getCell(1, 2).setStaticObject(bush);
            assertTrue(hero.move("UP", map, null));
        }

        @Test
        void wallInAdjacentDirection_doesNotAffectMove() {
            placeWall(2, 1); // wall to the LEFT; should not affect moving UP
            assertTrue(hero.move("UP", map, null));
        }

        private void placeWall(int row, int col) {
            map.getCell(row, col).setStaticObject(
                new StaticObject("Wall", "Solid wall", false, false));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Shadow Clone movement blocking tests  (Phase II A7 — NEW feature)
    //
    // "Blocking" here means the clone's OWN movement is blocked:
    //   • by walls        → clone stays in place
    //   • by map bounds   → clone stays in place
    //
    // The clone is ethereal (does not block the hero).
    // It moves in the direction opposite to the hero via onHeroMoved().
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ShadowCloneMovement {

        @Test
        void clone_movesOpposite_whenHeroMovesUp() {
            // hero delta: row -1 → clone target row: 2 - (-1) = 3
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.onHeroMoved(1, 2, -1, 0);
            assertEquals(3, clone.getRow());
        }

        @Test
        void clone_movesOpposite_whenHeroMovesDown() {
            // hero delta: row +1 → clone target row: 2 - 1 = 1
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.onHeroMoved(3, 2, 1, 0);
            assertEquals(1, clone.getRow());
        }

        @Test
        void clone_movesOpposite_whenHeroMovesLeft() {
            // hero delta: col -1 → clone target col: 0 - (-1) = 1
            ShadowClone clone = new ShadowClone(3, 0, map);
            clone.onHeroMoved(2, 1, 0, -1);
            assertEquals(1, clone.getCol());
        }

        @Test
        void clone_movesOpposite_whenHeroMovesRight() {
            // hero delta: col +1 → clone target col: 4 - 1 = 3
            ShadowClone clone = new ShadowClone(3, 4, map);
            clone.onHeroMoved(2, 3, 0, 1);
            assertEquals(3, clone.getCol());
        }

        @Test
        void clone_blockedByWall_staysInPlace() {
            // Wall below clone; hero moves UP → clone tries DOWN → blocked
            map.getCell(3, 3).setStaticObject(
                new StaticObject("Wall", "Solid", false, false));
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.onHeroMoved(1, 2, -1, 0); // reverse is DOWN (row+1)
            assertEquals(2, clone.getRow()); // must not move through wall
        }

        @Test
        void clone_blockedByMapBounds_staysInPlace() {
            // Clone at bottom row; hero moves UP → clone tries DOWN → out of bounds
            ShadowClone clone = new ShadowClone(4, 3, map);
            clone.onHeroMoved(1, 2, -1, 0);
            assertEquals(4, clone.getRow()); // stays in place
        }

        @Test
        void clone_blockedAtLeftBound_staysInPlace() {
            // Clone at leftmost col; hero moves RIGHT → clone tries LEFT → out of bounds
            ShadowClone clone = new ShadowClone(3, 0, map);
            clone.onHeroMoved(2, 3, 0, 1);
            assertEquals(0, clone.getCol());
        }

        @Test
        void clone_isEthereal_heroCanMoveToClonePosition() {
            // Clone at (1,2); hero should still be able to move UP to (1,2)
            new ShadowClone(1, 2, map); // setPosition only, no cell occupancy
            assertTrue(hero.move("UP", map, null));
        }

        @Test
        void clone_isNotExpired_initially() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            assertFalse(clone.isExpired());
        }

        @Test
        void clone_isExpired_afterFullLifetime() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.tick(ShadowClone.LIFETIME_MS);
            assertTrue(clone.isExpired());
        }

        @Test
        void clone_isNotExpired_oneMillisecondBeforeLifetime() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.tick(ShadowClone.LIFETIME_MS - 1);
            assertFalse(clone.isExpired());
        }

        @Test
        void clone_remainingMs_decreasesOnTick() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.tick(1_000L);
            assertEquals(ShadowClone.LIFETIME_MS - 1_000L, clone.getRemainingMs());
        }

        @Test
        void clone_isInvulnerable_whenHeroTakesDamage() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            int before = clone.getHealth();
            clone.onHeroDamageTaken(10); // spec: clone takes NO damage
            assertEquals(before, clone.getHealth());
        }

        @Test
        void clone_doesNotMove_whenDead() {
            ShadowClone clone = new ShadowClone(2, 3, map);
            clone.setHealth(0); // kill the clone
            clone.onHeroMoved(1, 2, -1, 0);
            assertEquals(2, clone.getRow()); // dead clone must not move
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
            assertTrue(new CollisionDetector(map).checkCollision(-1, 0));
        }

        @Test
        void collisionDetector_returnsTrue_negativeCol() {
            assertTrue(new CollisionDetector(map).checkCollision(0, -1));
        }

        @Test
        void collisionDetector_returnsTrue_rowBeyondHeight() {
            assertTrue(new CollisionDetector(map).checkCollision(ROWS, 0));
        }

        @Test
        void collisionDetector_returnsTrue_colBeyondWidth() {
            assertTrue(new CollisionDetector(map).checkCollision(0, COLS));
        }

        @Test
        void collisionDetector_returnsFalse_forOpenInBoundsCell() {
            assertFalse(new CollisionDetector(map).checkCollision(0, 0));
        }

        @Test
        void collisionDetector_returnsTrue_forWallInBounds() {
            map.getCell(0, 0).setStaticObject(
                new StaticObject("Wall", "Solid", false, false));
            assertTrue(new CollisionDetector(map).checkCollision(0, 0));
        }
    }
}
