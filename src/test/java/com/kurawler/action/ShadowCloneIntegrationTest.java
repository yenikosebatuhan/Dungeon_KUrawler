package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.model.object.Scroll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShadowCloneIntegrationTest {

    private Map map;
    private GameSession session;
    private Hero hero;

    @BeforeEach
    public void setUp() {
        // 10x10 boş bir harita oluştur
        map = new Map(10, 10);
        for(int r=0; r<10; r++) {
            for(int c=0; c<10; c++) {
                map.getCell(r, c).setStaticObject(new com.kurawler.model.object.StaticObject("Floor", "Desc", true, false));
            }
        }
        
        session = new GameSession(map);
        hero = new Hero("Test Hero");
        hero.setPosition(5, 5);
        hero.setCurrentCell(map.getCell(5, 5));
        session.setHero(hero);
    }

    @Test
    public void testShadowCloneFullLifecycle() {
        // --- ADIM 1: SCROLL OKUMA VE KLONUN DOĞMASI ---
        Scroll cloneScroll = new Scroll("Shadow Scroll", "Summons a clone", "summon_type", 0);
        hero.getInventory().addItem(cloneScroll); // Çantaya ekle

        // Scroll'u oku
        ReadAction readAction = new ReadAction(cloneScroll);
        List<Effect> effects = readAction.resolve(session);
        for (Effect e : effects) {
            e.apply(session);
        }

        ShadowClone clone = session.getShadowClone();
        assertNotNull(clone, "Klon yaratılmalı ve session'a eklenmeli.");
        assertEquals(5, clone.getRow(), "Klon Hero'nun konumunda (5,5) doğmalı.");
        assertEquals(5, clone.getCol());
        assertFalse(hero.getInventory().hasItem(cloneScroll), "Scroll okunduktan sonra çantadan silinmeli.");

        // --- ADIM 2: TERS HAREKET (BUG-01) ---
        // Hero'yu (4,5) noktasına (UP) hareket ettir
        hero.moveToCell(map.getCell(4, 5));

        // Hero UP gittiği için (deltaRow = -1), Klon DOWN gitmeli (5 - (-1) = 6)
        assertEquals(6, clone.getRow(), "Klon Hero'ya ters yönde (aşağı) hareket etmeli.");
        assertEquals(5, clone.getCol());

        // --- ADIM 3: HASAR ALMAZLIK (BUG-02) ---
        int initialCloneHp = clone.getHealth();
        
        // Hero 20 hasar alıyor
        hero.takeDamage(20);
        
        // Klonun canı değişmemeli
        assertEquals(initialCloneHp, clone.getHealth(), "Hero hasar aldığında Klon hasar ALMAMALI.");
    }
}