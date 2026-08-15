package fr.levelsfr.cobblemonpokesnackscontrols.effect;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;

import java.util.Objects;
import java.util.function.Supplier;

public final class SnackEffectRuleEngine {
    private final Supplier<SnackControlConfig> configSupplier;

    public SnackEffectRuleEngine(Supplier<SnackControlConfig> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    }

    public ResolvedEffectRule resolve(SnackEffectCategory category) {
        SnackControlConfig config = configSupplier.get();
        if (!config.general.enabled) {
            return ResolvedEffectRule.VANILLA;
        }
        return ResolvedEffectRule.from(config.effects.rule(category));
    }
}
