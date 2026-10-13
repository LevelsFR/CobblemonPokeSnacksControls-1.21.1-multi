package fr.levelsfr.cobblemonpokesnackscontrols.config;

public final class EffectRule {
    public boolean enabled = true;
    /**
     * Legacy schema v1 field. Kept for one normalization pass so existing configs migrate without losing intent.
     */
    @Deprecated
    public Double multiplier = null;
    public double valueMultiplier = 1.0D;
    public double chanceMultiplier = 1.0D;
    public Double maximumValue = null;
    public Double maximumChance = null;

    public void normalize() {
        if (multiplier != null) {
            double migrated = finiteNonNegative(multiplier, 1.0D);
            valueMultiplier = migrated;
            chanceMultiplier = migrated;
            multiplier = null;
        }
        valueMultiplier = finiteNonNegative(valueMultiplier, 1.0D);
        chanceMultiplier = finiteNonNegative(chanceMultiplier, 1.0D);
        maximumValue = nullableFiniteNonNegative(maximumValue);
        maximumChance = nullableClamp(maximumChance, 0.0D, 1.0D);
    }

    private static double finiteNonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0D ? value : fallback;
    }

    private static Double nullableFiniteNonNegative(Double value) {
        if (value == null) {
            return null;
        }
        return Double.isFinite(value) && value >= 0.0D ? value : null;
    }

    private static Double nullableClamp(Double value, double minimum, double maximum) {
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
