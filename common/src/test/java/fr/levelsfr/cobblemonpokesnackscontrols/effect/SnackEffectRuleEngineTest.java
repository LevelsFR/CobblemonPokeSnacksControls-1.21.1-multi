package fr.levelsfr.cobblemonpokesnackscontrols.effect;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SnackEffectRuleEngineTest {
    @Test
    void disabledModResolvesEveryCategoryAsVanilla() {
        SnackControlConfig config = new SnackControlConfig();
        config.general.enabled = false;
        config.effects.rule(SnackEffectCategory.SHINY).enabled = false;
        config.effects.rule(SnackEffectCategory.SHINY).valueMultiplier = 0.0D;
        config.effects.rule(SnackEffectCategory.SHINY).chanceMultiplier = 0.0D;
        config.effects.rule(SnackEffectCategory.SHINY).maximumValue = 0.0D;
        config.effects.rule(SnackEffectCategory.SHINY).maximumChance = 0.0D;

        SnackEffectRuleEngine engine = new SnackEffectRuleEngine(() -> config);
        for (SnackEffectCategory category : SnackEffectCategory.values()) {
            ResolvedEffectRule rule = engine.resolve(category);
            assertTrue(rule.enabled());
            assertEquals(1.0D, rule.valueMultiplier());
            assertEquals(1.0D, rule.chanceMultiplier());
            assertNull(rule.maximumValue());
            assertNull(rule.maximumChance());
        }
    }
}
