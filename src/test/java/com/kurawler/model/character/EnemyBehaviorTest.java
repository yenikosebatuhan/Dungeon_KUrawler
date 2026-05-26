package com.kurawler.model.character;

import com.kurawler.action.AttackAction;
import com.kurawler.effect.DamageEffect;
import com.kurawler.effect.Effect;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EnemyBehaviorTest {

	@Test
    public void testKnightSpawnAndBaseStats() {
        Knight knight = new Knight();
        assertEquals("Knight", knight.getName());
        assertEquals(20, knight.getMaxHealth());
        assertEquals(1, knight.getAttackPower());
        assertEquals(2, knight.getDefensePower(), "Knight defense power should be 2."); // Güncellendi
        assertEquals(8, knight.getStrength());
        assertTrue(knight.isAlive());
    }

    @Test
    public void testSorcererSpawnAndBaseStats() {
        // Arrange & Act
        Sorcerer sorcerer = new Sorcerer();

        // Assert: PDF'e göre belirlenen Sorcerer başlangıç değerleri
        assertEquals("Sorcerer", sorcerer.getName());
        assertEquals(10, sorcerer.getMaxHealth());
        assertEquals(8, sorcerer.getAttackPower());
        assertEquals(0, sorcerer.getDefensePower());
        assertEquals(5, sorcerer.getStrength());
    }

    @Test
    public void testSorcererRingDropState() {
        // Arrange
        Sorcerer sorcerer = new Sorcerer();

        // Assert: Doğduğu an yüzük düşmemiş (false) olmalı
        assertFalse(sorcerer.isRingDropped(), "Sorcerer should not have dropped the ring upon spawning.");

        // Act
        sorcerer.markRingDropped();

        // Assert: Drop metodu çağrıldıktan sonra (true) olmalı
        assertTrue(sorcerer.isRingDropped(), "Sorcerer should mark ring as dropped after markRingDropped is called.");
    }

    @Test
    public void testAttackActionPipelineProducesEffect() {
        // Arrange
        Hero hero = new Hero("Tester");
        Knight knight = new Knight();
        AttackAction attackAction = new AttackAction(hero, knight);

        // Act: Aksiyonu resolve edip Effect listesini alıyoruz
        List<Effect> effects = attackAction.resolve(null); // Session mocklanmadığı için null geçilebilir, resolve içinde session kullanılmıyor.

        // Assert
        assertNotNull(effects);
        assertEquals(1, effects.size(), "Attack action should produce exactly 1 effect.");
        assertTrue(effects.get(0) instanceof DamageEffect, "The produced effect must be a DamageEffect.");
    }
}