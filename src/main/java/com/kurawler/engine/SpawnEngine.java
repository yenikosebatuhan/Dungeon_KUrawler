package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;

/**
 * Stub for enemy spawning logic.
 */
public class SpawnEngine {

    /** Spawn an enemy at the given position. Stub implementation. */
    public Enemy spawnEnemy(GameSession session, String type, int row, int col) {
        Enemy enemy;
        switch (type.toLowerCase()) {
            case "knight":
                enemy = new Knight();
                break;
            case "sorcerer":
                enemy = new Sorcerer();
                break;
            default:
                enemy = new Knight();
                break;
        }
        enemy.setPosition(row, col);
        System.out.println("[Spawn] " + enemy.getName() + " spawned at (" + row + "," + col + ").");
        return enemy;
    }
}
