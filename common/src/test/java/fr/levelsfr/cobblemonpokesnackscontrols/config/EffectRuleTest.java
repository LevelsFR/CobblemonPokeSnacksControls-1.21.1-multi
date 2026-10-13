package fr.levelsfr.cobblemonpokesnackscontrols.config;

import fr.levelsfr.cobblemonpokesnackscontrols.effect.ResolvedEffectRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class EffectRuleTest {
    @Test
    void defaultsPreserveNativeValues() {
        EffectRule rule = new EffectRule();
        rule.normalize();
        ResolvedEffectRule resolved = ResolvedEffectRule.from(rule);

        assertEquals(4.0D, resolved.applyValue(4.0D));
        assertEquals(0.25D, resolved.applyChance(0.25D));
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyMultiplierMigratesToValueAndChanceMultipliers() {
        EffectRule rule = new EffectRule();
        rule.multiplier = 2.0D;
        rule.normalize();

        assertNull(rule.multiplier);
        assertEquals(2.0D, rule.valueMultiplier);
        assertEquals(2.0D, rule.chanceMultiplier);
    }

    @Test
    void invalidNumbersAreNormalized() {
        EffectRule rule = new EffectRule();
        rule.valueMultiplier = Double.NaN;
        rule.chanceMultiplier = -1.0D;
        rule.maximumValue = Double.POSITIVE_INFINITY;
        rule.maximumChance = 2.0D;
        rule.normalize();

        assertEquals(1.0D, rule.valueMultiplier);
        assertEquals(1.0D, rule.chanceMultiplier);
        assertNull(rule.maximumValue);
        assertEquals(1.0D, rule.maximumChance);
    }

    @Test
    void resolvedRuleClampsChanceAfterMultiplication() {
        EffectRule rule = new EffectRule();
        rule.chanceMultiplier = 3.0D;
        rule.maximumChance = 0.5D;
        rule.normalize();

        assertEquals(0.5D, ResolvedEffectRule.from(rule).applyChance(0.4D));
    }
}
