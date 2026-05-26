package com.kurawler.engine;

import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.object.Projectile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CombatResolverTest {

    private Hero testHero;
    private Knight testKnight;

    @BeforeEach
    public void setUp() {
        // Arrange: Her testten önce sıfırdan karakterler oluşturulur.
        testHero = new Hero("Test Hero");
        // Kahramanın başlangıç statlarını sabitliyoruz ki formülü test edebilelim.
        testHero.setStrength(10); // STR / 2 = 5
        testHero.setAttackPower(5); // ATK = 5
        testHero.setDefensePower(2); // DEF = 2

        testKnight = new Knight();
        // Şövalye statları: STR=8, ATK=4, DEF=1 (Knight sınıfından geliyor)
    }

    @Test
    public void testHeroAttacksKnightDamageCalculation() {
        // Hero: STR 10 (base 5) + ATK 5 = 10
        // Knight: DEF 2
        // Formül: (10 - 2) = 8
        int calculatedDamage = CombatResolver.calculateFinalDamage(testHero, testKnight);
        assertEquals(8, calculatedDamage, "Hero should deal exactly 8 damage.");
    }

    @Test
    public void testKnightAttacksHeroDamageCalculation() {
        // Knight: STR 8 (base 4) + ATK 1 = 5
        // Hero: DEF 2
        // Formül: (5 - 2) = 3
        int calculatedDamage = CombatResolver.calculateFinalDamage(testKnight, testHero);
        assertEquals(3, calculatedDamage, "Knight should deal exactly 3 damage to the Hero.");
    }

    @Test
    public void testMinimumDamageConstraint() {
        // Arrange: Kahramanın gücünü çok düşürüp, düşmanın defansını devasa yapıyoruz.
        testHero.setStrength(2); // 2/2 = 1
        testHero.setAttackPower(0);
        testKnight.setDefensePower(50); // Defans çok yüksek

        // Act: Normalde (1 + 0) - 50 = -49 çıkar, ama minimum 1 olmalı.
        int calculatedDamage = CombatResolver.calculateFinalDamage(testHero, testKnight);

        // Assert
        assertEquals(1, calculatedDamage, "Damage must never be less than 1.");
    }

    @Test
    public void testProjectileDamage() {
        // Arrange: 15 hasarlı bir büyü mermisi
        Projectile spell = new Projectile("Fireball", "Burns", 15, 0, 0);
        int initialHeroHp = testHero.getHealth();

        // Act: Mermi kahramana çarpıyor
        // Mermi formülü: Spell Damage - Target DEF (15 - 2 = 13 hasar almalı)
        CombatResolver.applyProjectileDamage(spell, testHero);

        // Assert
        assertEquals(initialHeroHp - 13, testHero.getHealth(), "Hero should take 13 damage from the 15 damage spell after 2 DEF.");
    }
}