package com.kurawler.model.achievement;

import com.kurawler.model.GameSession;
import com.kurawler.model.Map;
import com.kurawler.model.character.Hero;
import com.kurawler.model.character.ShadowClone;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementManagerTest {

    private Map map;
    private Hero hero;
    private GameSession session;
    private AchievementManager manager;

    @BeforeEach
    void setUp() {
        map = new Map(5, 5);
        hero = new Hero("TestHero");
        hero.moveToCell(map.getCell(2, 2));

        session = new GameSession(map);
        session.setHero(hero);
        manager = new AchievementManager();
        session.setAchievementManager(manager);
        session.wireObservers();
    }

    @Test
    void ordinaryHeroMovesDoNotUnlockShadowDancer() {
        moveHeroBackAndForth(30);

        assertEquals(30, manager.getTotalMoves());
        assertEquals(0, manager.getShadowCloneMoves());
        assertFalse(isUnlocked("Shadow Dancer"));
    }

    @Test
    void cloneMovesUnlockShadowDancer() {
        session.setShadowClone(new ShadowClone(2, 2, map));

        moveHeroBackAndForth(30);

        assertEquals(30, manager.getShadowCloneMoves());
        assertTrue(isUnlocked("Shadow Dancer"));
    }

    @Test
    void replacedCloneNoLongerContributesProgress() {
        ShadowClone replaced = new ShadowClone(2, 2, map);
        ShadowClone active = new ShadowClone(2, 2, map);
        session.setShadowClone(replaced);
        session.setShadowClone(active);

        assertFalse(hero.getObservers().contains(replaced));
        assertTrue(hero.getObservers().contains(active));

        replaced.onHeroMoved(2, 3, 0, 1);
        assertEquals(0, manager.getShadowCloneMoves());

        active.onHeroMoved(2, 3, 0, 1);
        assertEquals(1, manager.getShadowCloneMoves());
    }

    @Test
    void saveLoadPreservesCloneMovesWithoutSpawningClone(@TempDir Path tempDir) {
        for (int i = 0; i < 5; i++) {
            manager.onShadowCloneMoved();
        }
        Path saveFile = tempDir.resolve("shadow-dancer-save.json");

        assertTrue(new MapSerializer().saveGameState(session, saveFile.toString()));

        GameSession loaded = new GameSession();
        assertTrue(new MapDeserializer().loadGameState(saveFile.toString(), loaded));
        assertNull(loaded.getShadowClone());
        assertEquals(5, loaded.getAchievementManager().getShadowCloneMoves());
    }

    private void moveHeroBackAndForth(int moves) {
        for (int i = 0; i < moves; i++) {
            assertTrue(hero.move(i % 2 == 0 ? "RIGHT" : "LEFT", map, session));
        }
    }

    private boolean isUnlocked(String name) {
        return manager.getAchievements().stream()
                .filter(achievement -> achievement.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .isUnlocked();
    }
}
