package com.kurawler.model.character;

import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.Map;
import com.kurawler.model.object.Projectile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Sorcerer extends Enemy {
    private static final float TELEPORT_CHANCE = 0.5f;
    private static final int ATTACK_COOLDOWN = 5;
    private static final int SPELL_DAMAGE = 8;
    private static final Random RNG = new Random();

    private int ticksSinceLastAttack = 0;
    private Projectile lastSpell;

    public Sorcerer() {
        super("Sorcerer", 10, 8, 0, 5, 60, 40);
    }

    public void decideAction(Hero hero, Map map, GameSession session) {
        if (hero == null || map == null || !isAlive() || !hero.isAlive()) return;
        if (RNG.nextFloat() < TELEPORT_CHANCE) {
            System.out.println("[AI] Sorcerer: Teleporting");
            teleportToRandomWalkable(map, session, hero);
        }
        
        ticksSinceLastAttack++;
        if (ticksSinceLastAttack >= ATTACK_COOLDOWN) {
            System.out.println("[AI] Sorcerer: Casting spell at hero");
            castSpell(hero);
            ticksSinceLastAttack = 0;
        }
    }

    public Projectile castSpell(Hero hero) {
        if (hero == null || !hero.isAlive()) return null;
        int dr = Integer.signum(hero.getRow() - getRow());
        int dc = Integer.signum(hero.getCol() - getCol());
        Projectile p = new Projectile("Arcane Bolt", "Crackling magic energy", SPELL_DAMAGE, dr, dc);
        hero.takeDamage(SPELL_DAMAGE);
        this.lastSpell = p;
        return p;
    }

    private void teleportToRandomWalkable(Map map, GameSession session, Hero hero) {
        List<GridCell> candidates = new ArrayList<>();
        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || cell.isBlocked()) continue;
                if (hero.getRow() == r && hero.getCol() == c) continue;
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) return;
        GridCell pick = candidates.get(RNG.nextInt(candidates.size()));
        
        if (getCurrentCell() != null) getCurrentCell().setOccupyingCharacter(null);
        pick.setOccupyingCharacter(this);
        setCurrentCell(pick);
        setPosition(pick.getRow(), pick.getCol());
    }

    public float getTeleportChance() { return TELEPORT_CHANCE; }
    public int getAttackCooldown() { return ATTACK_COOLDOWN; }
    public int getSpellDamage() { return SPELL_DAMAGE; }
    public Projectile getLastSpell() { return lastSpell; }
}