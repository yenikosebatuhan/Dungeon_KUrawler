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
        // Act: Hero'nun Knight'a vuracağı hasarı hesapla
        // Formül: Max(1, (STR / 2 + ATK) - DEF)
        // Hero vuruyor: (10 / 2 + 5) - 1 = (5 + 5) - 1 = 9
        int calculatedDamage = CombatResolver.calculateFinalDamage(testHero, testKnight);

        // Assert
        assertEquals(9, calculatedDamage, "Hero should deal exactly 9 damage to the Knight.");
    }

    @Test
    public void testKnightAttacksHeroDamageCalculation() {
        // Act: Knight'ın Hero'ya vuracağı hasarı hesapla
        // Knight vuruyor: (8 / 2 + 4) - 2 = (4 + 4) - 2 = 6
        int calculatedDamage = CombatResolver.calculateFinalDamage(testKnight, testHero);

        // Assert
        assertEquals(6, calculatedDamage, "Knight should deal exactly 6 damage to the Hero.");
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