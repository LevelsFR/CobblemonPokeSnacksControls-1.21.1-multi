package fr.levelsfr.cobblemonpokesnackscontrols.effect;

import com.cobblemon.mod.common.api.fishing.SpawnBait;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CobblemonBaitEffectAdapter {
    private CobblemonBaitEffectAdapter() {
    }

    public static List<SpawnBait.Effect> adjust(List<SpawnBait.Effect> nativeEffects) {
        return adjust(nativeEffects, fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks::effectRule);
    }

    static List<SpawnBait.Effect> adjust(
            List<SpawnBait.Effect> nativeEffects,
            Function<SnackEffectCategory, ResolvedEffectRule> ruleResolver
    ) {
        List<SpawnBait.Effect> adjusted = new ArrayList<>(nativeEffects.size());
        Map<SnackEffectCategory, Totals> totals = new EnumMap<>(SnackEffectCategory.class);
        Map<SnackEffectCategory, ResolvedEffectRule> rules = new EnumMap<>(SnackEffectCategory.class);
        for (SpawnBait.Effect effect : nativeEffects) {
            SnackEffectCategory category = category(effect.getType());
            if (category == null) {
                adjusted.add(effect);
                continue;
            }

            ResolvedEffectRule rule = rules.computeIfAbsent(category, ruleResolver);
            if (!rule.enabled()) {
                continue;
            }

            double chance = rule.applyChanceMultiplier(effect.getChance());
            double value = category == SnackEffectCategory.SIZE
                    ? rule.applySignedValueMultiplier(effect.getValue())
                    : rule.applyValueMultiplier(effect.getValue());
            adjusted.add(new SpawnBait.Effect(effect.getType(), effect.getSubcategory(), chance, value));
            totals.computeIfAbsent(category, ignored -> new Totals()).add(chance, value, category == SnackEffectCategory.SIZE);
        }

        for (int index = 0; index < adjusted.size(); index++) {
            SpawnBait.Effect effect = adjusted.get(index);
            SnackEffectCategory category = category(effect.getType());
            if (category == null) {
                continue;
            }
            ResolvedEffectRule rule = rules.get(category);
            Totals total = totals.get(category);
            if (rule == null || total == null) {
                continue;
            }

            double chance = scaleToCap(effect.getChance(), total.chance, rule.maximumChance());
            double value = scaleToCap(effect.getValue(), total.valueMagnitude, rule.maximumValue());
            if (chance != effect.getChance() || value != effect.getValue()) {
                adjusted.set(index, new SpawnBait.Effect(effect.getType(), effect.getSubcategory(), chance, value));
            }
        }
        return adjusted;
    }

    private static double scaleToCap(double value, double total, Double cap) {
        if (cap == null || total <= cap || total <= 0.0D) {
            return value;
        }
        return value * (cap / total);
    }

    public static SnackEffectCategory category(ResourceLocation type) {
        SpawnBait.Effects effects = SpawnBait.Effects.INSTANCE;
        if (type.equals(effects.getTYPING())) return SnackEffectCategory.TYPE_ATTRACTION;
        if (type.equals(effects.getEGG_GROUP())) return SnackEffectCategory.EGG_GROUP_ATTRACTION;
        if (type.equals(effects.getEV())) return SnackEffectCategory.EV_YIELD_FILTER;
        if (type.equals(effects.getRARITY_BUCKET())) return SnackEffectCategory.RARITY_BUCKET;
        if (type.equals(effects.getNATURE())) return SnackEffectCategory.NATURE;
        if (type.equals(effects.getIV())) return SnackEffectCategory.IV;
        if (type.equals(effects.getSHINY_REROLL())) return SnackEffectCategory.SHINY;
        if (type.equals(effects.getHIDDEN_ABILITY_CHANCE())) return SnackEffectCategory.HIDDEN_ABILITY;
        if (type.equals(effects.getGENDER_CHANCE())) return SnackEffectCategory.GENDER;
        if (type.equals(effects.getLEVEL_RAISE())) return SnackEffectCategory.LEVEL;
        if (type.equals(effects.getFRIENDSHIP())) return SnackEffectCategory.FRIENDSHIP;
        if (type.equals(effects.getDROPS_REROLL())) return SnackEffectCategory.DROPS;
        if (type.equals(effects.getBITE_TIME())) return SnackEffectCategory.BITE_TIME;
        if (type.equals(effects.getALPHA_CHANCE())) return SnackEffectCategory.ALPHA;
        if (type.equals(effects.getMARK_CHANCE())) return SnackEffectCategory.MARK;
        if (type.equals(effects.getSIZE())) return SnackEffectCategory.SIZE;
        return null;
    }

    private static final class Totals {
        private double chance;
        private double valueMagnitude;

        private void add(double chance, double value, boolean signed) {
            this.chance += chance;
            this.valueMagnitude += signed ? Math.abs(value) : value;
        }
    }
}
