package com.kurawler.action;

import com.kurawler.effect.BreakObjectEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.GridCell;
import com.kurawler.model.MapFactory;
import com.kurawler.model.character.Hero;
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
            double chance = breakSuccessChance(strength);
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
        return BreakRoll.fromStrength(hero.getStrength());
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
