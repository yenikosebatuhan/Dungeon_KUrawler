package com.kurawler.engine;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Enemy;
import com.kurawler.model.character.Knight;
import com.kurawler.model.character.Sorcerer;

/**
 * Stub for enemy spawning logic.
 */
public class SpawnEngine {

    /**
     * Spawn an enemy at the given position.
     *
     * @requires session != null && type != null && row >= 0 && col >= 0
     * @modifies enemy.row, enemy.col
     * @effects Creates a Knight or Sorcerer based on type (case-insensitive),
     *          sets its position to (row, col), and returns it. Defaults to Knight
     *          for unrecognised types.
     */
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
