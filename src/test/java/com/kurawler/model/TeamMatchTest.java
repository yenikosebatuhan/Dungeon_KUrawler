package com.kurawler.model;

import com.kurawler.engine.TeamMatchGameLoop;
import com.kurawler.model.character.Character;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.Knight;
import com.kurawler.model.object.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamMatchTest {

    private Map map;
    private TeamMatchSession session;

    @BeforeEach
    void setUp() {
        map = new Map(10, 10);
        session = new TeamMatchSession(map);
        session.initializeMatch();
    }

    // --- Team composition ---

    @Test
    void teamAHasFourMembers() {
        assertEquals(4, session.getTeamA().getMembers().size());
    }

    @Test
    void teamBHasFourMembers() {
        assertEquals(4, session.getTeamB().getMembers().size());
    }

    @Test
    void sessionModeIsTeamMatch() {
        assertEquals("TeamMatch", session.getCurrentMode());
    }

    // --- Win conditions ---

    @Test
    void teamBWinsWhenTeamAIsEliminated() {
        for (Character m : session.getTeamA().getMembers()) {
            m.takeDamage(m.getHealth());
        }
        assertTrue(session.checkWinCondition());
        assertEquals("Team B Wins!", session.getMatchResult());
    }

    @Test
    void teamAWinsWhenTeamBIsEliminated() {
        for (Character m : session.getTeamB().getMembers()) {
            m.takeDamage(m.getHealth());
        }
        assertTrue(session.checkWinCondition());
        assertEquals("Team A Wins!", session.getMatchResult());
    }

    @Test
    void noWinWhenBothTeamsAlive() {
        assertFalse(session.checkWinCondition());
    }

    @Test
    void gameOverWhenHeroDies() {
        session.getHero().takeDamage(session.getHero().getHealth());
        assertTrue(session.isGameOver());
    }

    // --- Team lookup ---

    @Test
    void getTeamForCharacterReturnsCorrectTeam() {
        Character member = session.getTeamA().getMembers().get(0);
        assertEquals(session.getTeamA(), session.getTeamForCharacter(member));
    }

    @Test
    void getOpposingTeamReturnsBForMemberOfA() {
        Character memberA = session.getTeamA().getMembers().get(0);
        assertEquals(session.getTeamB(), session.getOpposingTeam(memberA));
    }

    @Test
    void getTeamForCharacterReturnsNullForStranger() {
        assertNull(session.getTeamForCharacter(new Knight()));
    }

    // --- Weapon placement ---

    @Test
    void weaponsArePlacedOnMap() {
        assertFalse(session.getMatchWeapons().isEmpty());
    }

    @Test
    void findNearestWeaponReturnsWeaponWhenPresent() {
        Character anyMember = session.getTeamA().getMembers().get(0);
        Weapon nearest = session.findNearestWeapon(anyMember);
        assertNotNull(nearest);
    }

    // --- AI: weapon acquisition ---

    @Test
    void knightEquipsWeaponWhenOnSameCell() {
        Knight knight = new Knight();
        Weapon sword = new Weapon("Sword", "Sharp", 5);

        knight.moveToCell(map.getCell(5, 5));
        map.getCell(5, 5).addObject(sword);

        // Simulate the equip logic directly (same as TeamMatchGameLoop.seekAndPickWeapon)
        GridCell cell = session.getMap().getCell(knight.getRow(), knight.getCol());
        for (var obj : List.copyOf(cell.getObjects())) {
            if (obj instanceof Weapon && knight.getEquippedMatchWeapon() == null) {
                knight.equipMatchWeapon((Weapon) obj);
                cell.removeObject(obj);
                break;
            }
        }

        assertNotNull(knight.getEquippedMatchWeapon());
        assertEquals("Sword", knight.getEquippedMatchWeapon().getName());
    }

    @Test
    void knightAttackPowerIncreasesAfterEquip() {
        Knight knight = new Knight();
        int basePower = knight.getAttackPower();
        Weapon sword = new Weapon("Sword", "Sharp", 6);
        knight.equipMatchWeapon(sword);
        assertTrue(knight.getAttackPower() > basePower);
    }

    // --- AI: GameLoop ticks ---

    @Test
    void gameLoopStopsAfterWinCondition() {
        TeamMatchGameLoop loop = new TeamMatchGameLoop(session);
        loop.start();
        assertTrue(loop.isRunning());

        // Eliminate team A to trigger win condition
        for (Character m : session.getTeamA().getMembers()) {
            m.takeDamage(m.getHealth());
        }

        loop.update(100);
        assertFalse(loop.isRunning());
    }

    @Test
    void gameLoopDoesNotRunWhenStopped() {
        TeamMatchGameLoop loop = new TeamMatchGameLoop(session);
        loop.stop();
        assertFalse(loop.isRunning());
        // update must be a no-op — no exception expected
        assertDoesNotThrow(() -> loop.update(100));
    }

    @Test
    void validateMapReturnsTrueForLargeEnoughMap() {
        assertTrue(session.validateMap());
    }

    @Test
    void validateMapReturnsFalseForTinyMap() {
        TeamMatchSession tiny = new TeamMatchSession(new Map(2, 2));
        assertFalse(tiny.validateMap());
    }
}
