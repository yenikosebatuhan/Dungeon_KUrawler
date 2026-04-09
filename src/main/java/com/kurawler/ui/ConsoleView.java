package com.kurawler.ui;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.character.Hero;

/**
 * UI layer — displays game state to console.
 * Contains NO game logic (GRASP Controller pattern).
 * All operations are delegated to the GameController.
 */
public class ConsoleView {

    /** Render the current game state to the console. */
    public void render(GameSession session) {
        Hero hero = session.getHero();
        System.out.println("\n=== Dungeon KUrawler - Turn " + session.getTurnCount() + " ===");
        System.out.println("Hero: " + hero.getName()
                + " | HP: " + hero.getHealth() + "/" + hero.getMaxHealth()
                + " | Pos: (" + hero.getRow() + "," + hero.getCol() + ")");
        System.out.println(hero.getInventory());
        renderMap(session);
    }

    private void renderMap(GameSession session) {
        var map = session.getMap();
        Hero hero = session.getHero();

        System.out.println("Map (" + map.getWidth() + "x" + map.getHeight() + "):");
        for (int r = 0; r < map.getHeight(); r++) {
            StringBuilder row = new StringBuilder("  ");
            for (int c = 0; c < map.getWidth(); c++) {
                if (r == hero.getRow() && c == hero.getCol()) {
                    row.append("H ");
                } else {
                    GridCell cell = map.getCell(r, c);
                    if (cell.hasObjects()) {
                        row.append("? ");
                    } else {
                        row.append(". ");
                    }
                }
            }
            System.out.println(row);
        }
    }

    public void showMessage(String message) {
        System.out.println("[UI] " + message);
    }
}
