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

    @Override
    public void decideAction(Hero hero, Map map, GameSession session) {
        if (hero == null || map == null || !isAlive() || !hero.isAlive()) return;
        ticksSinceLastAttack++;
        
        int dist = Math.abs(hero.getRow() - getRow()) + Math.abs(hero.getCol() - getCol());
        boolean teleported = false;
        if (dist <= 1 && RNG.nextFloat() < TELEPORT_CHANCE) {
            System.out.println("[AI] Sorcerer: Teleporting");
            teleportToRandomWalkable(map, session, hero);
            teleported = true;
        }
        
        if (!teleported && ticksSinceLastAttack >= ATTACK_COOLDOWN) {
            System.out.println("[AI] Sorcerer: Casting spell at hero");
            castSpell(hero, map);
            ticksSinceLastAttack = 0;
        }
    }

    public Projectile castSpell(Hero hero, Map map) {
        if (hero == null || !hero.isAlive() || map == null) return null;
        if (!hasLineOfSight(hero, map)) return null;
        int dr = Integer.signum(hero.getRow() - getRow());
        int dc = Integer.signum(hero.getCol() - getCol());
        Projectile p = new Projectile("Arcane Bolt", "Crackling magic energy", SPELL_DAMAGE, dr, dc);
        hero.takeDamage(SPELL_DAMAGE);
        this.lastSpell = p;
        return p;
    }

    private boolean hasLineOfSight(Hero hero, Map map) {
        int r = getRow(), c = getCol();
        int dr = Integer.signum(hero.getRow() - r);
        int dc = Integer.signum(hero.getCol() - c);
        if (dr == 0 && dc == 0) return false;
        
        if (dr != 0 && dc != 0 && Math.abs(hero.getRow()-r) != Math.abs(hero.getCol()-c)) return false;
        r += dr; c += dc;
        while (r != hero.getRow() || c != hero.getCol()) {
            GridCell cell = map.getCell(r, c);
            if (cell == null) return false;
            
            if (cell.getStaticObject() != null && !cell.getStaticObject().isPassable()) return false;
            r += dr; c += dc;
        }
        return true;
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
        
        moveToCell(pick);
    }

    public float getTeleportChance() { return TELEPORT_CHANCE; }
    public int getAttackCooldown() { return ATTACK_COOLDOWN; }
    public int getSpellDamage() { return SPELL_DAMAGE; }
    public Projectile getLastSpell() { return lastSpell; }
}