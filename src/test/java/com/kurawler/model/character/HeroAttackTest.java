package com.kurawler.model.character;

import com.kurawler.model.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroAttackTest {

    private Map map;
    private Hero hero;
    private Knight knight;

    @BeforeEach
    void setUp() {
        map = new Map(5, 5);
        hero = new Hero("TestHero");
        hero.moveToCell(map.getCell(2, 2));
        knight = new Knight();
    }

    @Test
    void adjacentAttackDamagesEnemyAndSpendsEnergy() {
        knight.moveToCell(map.getCell(2, 3));
        int healthBefore = knight.getHealth();
        int energyBefore = hero.getEnergy();

        assertTrue(hero.attack(knight));
        assertTrue(knight.getHealth() < healthBefore);
        assertEquals(energyBefore - 1, hero.getEnergy());
    }

    @Test
    void nonAdjacentAttackDoesNotSpendEnergy() {
        knight.moveToCell(map.getCell(2, 4));
        int healthBefore = knight.getHealth();
        int energyBefore = hero.getEnergy();

        assertFalse(hero.attack(knight));
        assertEquals(healthBefore, knight.getHealth());
        assertEquals(energyBefore, hero.getEnergy());
    }

    @Test
    void attackWithoutEnergyDoesNotDamageEnemy() {
        knight.moveToCell(map.getCell(2, 3));
        hero.setEnergy(0);
        int healthBefore = knight.getHealth();

        assertFalse(hero.attack(knight));
        assertEquals(healthBefore, knight.getHealth());
    }

    @Test
    void attackRejectsNullAndDeadTargets() {
        int energyBefore = hero.getEnergy();

        assertFalse(hero.attack(null));

        knight.moveToCell(map.getCell(2, 3));
        knight.takeDamage(knight.getHealth());
        assertFalse(hero.attack(knight));
        assertEquals(energyBefore, hero.getEnergy());
    }
}
