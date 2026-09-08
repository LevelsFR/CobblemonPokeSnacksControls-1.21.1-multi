package fr.levelsfr.cobblemonpokesnackscontrols.effect;

import fr.levelsfr.cobblemonpokesnackscontrols.config.EffectRule;

public record ResolvedEffectRule(
        boolean enabled,
        double valueMultiplier,
        double chanceMultiplier,
        Double maximumValue,
        Double maximumChance
) {
    public static final ResolvedEffectRule VANILLA = new ResolvedEffectRule(true, 1.0D, 1.0D, null, null);

    public static ResolvedEffectRule from(EffectRule rule) {
        return new ResolvedEffectRule(rule.enabled, rule.valueMultiplier, rule.chanceMultiplier, rule.maximumValue, rule.maximumChance);
    }

    public double applyValue(double nativeValue) {
        if (!enabled) {
            return 0.0D;
        }
        double adjusted = applyValueMultiplier(nativeValue);
        return maximumValue == null ? adjusted : Math.min(adjusted, maximumValue);
    }

    public double applyChance(double nativeChance) {
        if (!enabled) {
            return 0.0D;
        }
        double adjusted = applyChanceMultiplier(nativeChance);
        return maximumChance == null ? adjusted : Math.min(adjusted, maximumChance);
    }

    public double applyValueMultiplier(double nativeValue) {
        if (!enabled) {
            return 0.0D;
        }
        return Math.max(0.0D, nativeValue * valueMultiplier);
    }

    public double applySignedValueMultiplier(double nativeValue) {
        if (!enabled) {
            return 0.0D;
        }
        return nativeValue * valueMultiplier;
    }

    public double applyChanceMultiplier(double nativeChance) {
        if (!enabled) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, nativeChance * chanceMultiplier));
    }
}
