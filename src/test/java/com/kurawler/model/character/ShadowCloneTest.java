package com.kurawler.model.character;

import com.kurawler.model.Map;
import com.kurawler.model.GridCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShadowCloneTest {

    private Map map;
    private Hero hero;
    private ShadowClone clone;

    @BeforeEach
    public void setUp() {
        map = new Map(10, 10);
        // Fill map with empty floors to avoid null pointers
        for(int r=0; r<10; r++) {
            for(int c=0; c<10; c++) {
                map.getCell(r, c).setStaticObject(new com.kurawler.model.object.StaticObject("Floor", "Desc", true, false));
            }
        }
        
        hero = new Hero("Hero");
        hero.setPosition(5, 5);
        
        clone = new ShadowClone(5, 5, map);
        hero.addObserver(clone);
    }

@Test
    public void testCloneMovesOppositeToHeroUp() {
        // Hero'nun hareketini simüle et
        // moveToCell metodu HeroObserver'ı (dolayısıyla klonu) tetikleyecektir
        GridCell targetCell = map.getCell(4, 5);
        hero.moveToCell(targetCell); 
        
        // Hero (5,5) -> (4,5) hareket ettiğinde deltaRow = -1 olur.
        // Klon (5,5) -> (5 - (-1), 5) = (6, 5) hareket etmelidir.
        assertEquals(6, clone.getRow(), "Clone should move DOWN when Hero moves UP.");
        assertEquals(5, clone.getCol());
    }

    @Test
    public void testCloneTakesNoDamageWhenHeroIsHit() {
        int initialHp = clone.getHealth();
        
        // Hero takes 20 damage
        hero.takeDamage(20);
        
        assertEquals(initialHp, clone.getHealth(), "Clone must NOT take damage when Hero is hit (BUG-02).");
    }
}