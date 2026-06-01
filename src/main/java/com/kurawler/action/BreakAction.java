package com.kurawler.action;

import com.kurawler.effect.BreakObjectEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.StaticContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class BreakAction implements Action {
    /** Energy spent on every break attempt (success or failure). */
    public static final int BREAK_ENERGY_COST = 5;

    /** Hero STR range at new game (see Hero rollRandomStartingStrength). */
    public static final int BREAK_STR_MIN = 8;
    public static final int BREAK_STR_MAX = 15;
    /** +12.5% break chance per STR above 7 (STR 8 → 12.5%, … STR 15 → 100%). */
    public static final double CHANCE_PER_STR_STEP = 0.125;

    private final int row;
    private final int col;

    public static final class BreakRoll {
        public enum Kind { NO_TARGET, NO_ENERGY, FAILED, SUCCESS }

        private final Kind kind;
        private final int strength;
        private final String chanceLabel;
        private final int rollPercent;

        private BreakRoll(Kind kind, int strength, String chanceLabel, int rollPercent) {
            this.kind = kind;
            this.strength = strength;
            this.chanceLabel = chanceLabel;
            this.rollPercent = rollPercent;
        }

        public static BreakRoll noTarget() {
            return new BreakRoll(Kind.NO_TARGET, 0, "0", 0);
        }

        public static BreakRoll noEnergy() {
            return new BreakRoll(Kind.NO_ENERGY, 0, "0", 0);
        }

        public static BreakRoll fromStrength(int strength) {
            return fromStrength(strength, 1.0);
        }

        /** {@code difficulty} in (0,1]: lower = harder (locked / iron chests). */
        public static BreakRoll fromStrength(int strength, double difficulty) {
            double chance = breakSuccessChance(strength) * difficulty;
            chance = Math.max(0.0, Math.min(1.0, chance));
            String chanceLabel = String.format(Locale.US, "%.1f", chance * 100.0);
            double roll = ThreadLocalRandom.current().nextDouble();
            int rollPct = (int) Math.round(roll * 100.0);
            Kind kind = roll < chance ? Kind.SUCCESS : Kind.FAILED;
            return new BreakRoll(kind, strength, chanceLabel, rollPct);
        }

        public Kind getKind() {
            return kind;
        }

        public boolean succeeded() {
            return kind == Kind.SUCCESS;
        }

        public String message() {
            return switch (kind) {
                case NO_TARGET -> "Nothing to break here.";
                case NO_ENERGY -> "Not enough energy to break (need "
                        + BreakAction.BREAK_ENERGY_COST + " EN).";
                case SUCCESS -> String.format(
                        "Broke it! (%s%% chance at STR %d)", chanceLabel, strength);
                case FAILED -> String.format(
                        "Break failed — rolled %d (need under %s%% at STR %d). Try again.",
                        rollPercent, chanceLabel, strength);
            };
        }
    }

    public BreakAction(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Linear break chance: STR 8 → 12.5%, each +1 STR adds 12.5%, STR 15 → 100%.
     */
    public static double breakSuccessChance(int strength) {
        if (strength < BREAK_STR_MIN) {
            return 0.0;
        }
        if (strength >= BREAK_STR_MAX) {
            return 1.0;
        }
        return (strength - (BREAK_STR_MIN - 1)) * CHANCE_PER_STR_STEP;
    }

    public static BreakRoll attempt(GameSession session, int row, int col) {
        GridCell cell = session.getMap().getCell(row, col);
        if (cell == null || !MapFactory.hasBreakableTarget(cell)) {
            return BreakRoll.noTarget();
        }
        Hero hero = session.getHero();
        if (hero == null) {
            return BreakRoll.noTarget();
        }
        if (!spendAttemptEnergy(hero)) {
            return BreakRoll.noEnergy();
        }
        return BreakRoll.fromStrength(hero.getStrength(), breakDifficulty(cell));
    }

    /**
     * Difficulty multiplier for the breakable target on this cell (1.0 = easiest).
     * Locked containers and heavy iron/steel chests are far harder to smash open,
     * so a player without the key must rely on high STR and repeated attempts.
     */
    public static double breakDifficulty(GridCell cell) {
        if (cell == null) {
            return 1.0;
        }
        for (GameObject obj : cell.getObjects()) {
            if (obj instanceof StaticContainer sc) {
                double factor = 1.0;
                String n = sc.getName().toLowerCase(Locale.US);
                if (sc.isLocked()) {
                    factor *= 0.5;
                }
                if (n.contains("iron") || n.contains("steel")) {
                    factor *= 0.4;
                } else if (n.contains("chest")) {
                    factor *= 0.7;
                }
                return factor;
            }
        }
        return 1.0;
    }

    /** Spends break attempt energy (success or failure). */
    public static boolean spendAttemptEnergy(Hero hero) {
        return hero != null && hero.useEnergy(BREAK_ENERGY_COST);
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        List<Effect> effects = new ArrayList<>();
        BreakRoll roll = attempt(session, row, col);
        if (roll.succeeded()) {
            effects.add(new BreakObjectEffect(row, col));
        }
        return effects;
    }
}
