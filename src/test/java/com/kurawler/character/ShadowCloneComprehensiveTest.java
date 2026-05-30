package com.kurawler.model.character;

import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.StaticObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShadowCloneComprehensiveTest {

    private Map map;
    private Hero hero;
    private ShadowClone clone;

    @BeforeEach
    public void setUp() {
        map = new Map(10, 10);
        // Haritayı yürünebilir zeminlerle doldur
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                map.getCell(r, c).setStaticObject(new StaticObject("Floor", "Desc", true, false));
            }
        }

        hero = new Hero("Hero");
        hero.setPosition(5, 5);
        hero.setCurrentCell(map.getCell(5, 5));

        clone = new ShadowClone(5, 5, map);
        hero.addObserver(clone);
    }

    // ==========================================
    // BUG-01: Wrong Movement Direction Tests
    // ==========================================

    @Test
    public void cloneMovesOppositeToHeroUp() {
        // Hero yukarı gider (deltaRow = -1)
        hero.moveToCell(map.getCell(4, 5));
        // Klon aşağı gitmeli (5 - (-1) = 6)
        assertEquals(6, clone.getRow());
        assertEquals(5, clone.getCol());
    }

    @Test
    public void cloneMovesOppositeToHeroDown() {
        // Hero aşağı gider (deltaRow = 1)
        hero.moveToCell(map.getCell(6, 5));
        // Klon yukarı gitmeli (5 - 1 = 4)
        assertEquals(4, clone.getRow());
        assertEquals(5, clone.getCol());
    }

    @Test
    public void cloneMovesOppositeToHeroLeft() {
        // Hero sola gider (deltaCol = -1)
        hero.moveToCell(map.getCell(5, 4));
        // Klon sağa gitmeli (5 - (-1) = 6)
        assertEquals(5, clone.getRow());
        assertEquals(6, clone.getCol());
    }

    @Test
    public void cloneMovesOppositeToHeroRight() {
        // Hero sağa gider (deltaCol = 1)
        hero.moveToCell(map.getCell(5, 6));
        // Klon sola gitmeli (5 - 1 = 4)
        assertEquals(5, clone.getRow());
        assertEquals(4, clone.getCol());
    }

    // ==========================================
    // BUG-02: Clone Invulnerability Tests
    // ==========================================

    @Test
    public void cloneTakesNoDamageWhenHeroIsHit() {
        int initialHp = clone.getHealth();
        hero.takeDamage(10);
        assertEquals(initialHp, clone.getHealth(), "Clone should not take damage when Hero is hit.");
    }

    @Test
    public void cloneTakesNoDamageOnLargeHit() {
        int initialHp = clone.getHealth();
        hero.takeDamage(999); // Devasa hasar
        assertEquals(initialHp, clone.getHealth(), "Clone must remain invulnerable even on massive hits.");
    }

    // ==========================================
    // Additional Clone Behaviour Checks
    // ==========================================

    @Test
    public void cloneBlockedByWalls() {
        // Klonun gitmek isteyeceği hedef hücreye duvar koyalım (Aşağı: 6,5)
        map.getCell(6, 5).setStaticObject(new StaticObject("Wall", "Blocks movement", false, false));

        // Hero yukarı gidince klon duvar olan (6,5)'e inmeye çalışacak
        hero.moveToCell(map.getCell(4, 5));

        // Duvara çarptığı için konumu değişmemeli (5,5 kalmalı)
        assertEquals(5, clone.getRow(), "Clone should be blocked by walls.");
        assertEquals(5, clone.getCol(), "Clone should be blocked by walls.");
    }

    @Test
    public void cloneBlockedByOOB() {
        // Klonu en alt köşeye taşı (9, 9)
        clone.setPosition(9, 9);
        hero.setPosition(9, 9);

        // Hero sol yukarı giderse (8,8), klon sağ aşağı inmek isteyecek (10, 10 - OOB)
        hero.moveToCell(map.getCell(8, 8));

        // Harita dışına çıkamadığı için konumu değişmemeli (9,9 kalmalı)
        assertEquals(9, clone.getRow(), "Clone should not move out of bounds.");
        assertEquals(9, clone.getCol(), "Clone should not move out of bounds.");
    }

    @Test
    public void cloneExpiresAfterLifetime() {
        assertFalse(clone.isExpired(), "Clone should not be expired initially.");
        
        // Lifetime süresi kadar (veya biraz fazla) tick yolla
        clone.tick(ShadowClone.LIFETIME_MS + 100);
        
        assertTrue(clone.isExpired(), "Clone should be expired after its lifetime passes.");
    }

    @Test
    public void cloneIsEthereal() {
        // PBI AC: "Clone is ethereal: hero can walk through it."
        // Klon hücresinde occupyingCharacter olarak kaydedilmemeli.
        GridCell cell = map.getCell(clone.getRow(), clone.getCol());
        
        assertNull(cell.getOccupyingCharacter(), "Clone should not occupy the GridCell (Ethereal).");
        assertFalse(cell.isBlocked(), "Cell containing only a shadow clone should NOT be blocked.");
    }
}