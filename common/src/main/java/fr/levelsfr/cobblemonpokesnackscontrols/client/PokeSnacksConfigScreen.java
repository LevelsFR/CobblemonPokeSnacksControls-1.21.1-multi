package fr.levelsfr.cobblemonpokesnackscontrols.client;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.config.EffectRule;
import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectCategory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

public final class PokeSnacksConfigScreen {
    private static final String PREFIX = "config.cobblemon_pokesnacks_controls.";

    private PokeSnacksConfigScreen() {
    }

    public static Screen create(Screen parent) {
        var manager = CobblemonPokeSnacksControls.configManager();
        SnackControlConfig config = manager.get();
        SnackControlConfig defaults = new SnackControlConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(text("title"))
                .setDoesConfirmSave(true);
        ConfigEntryBuilder entries = builder.entryBuilder();

        addGeneral(builder, entries, config, defaults);
        addActivity(builder, entries, config, defaults);
        addJade(builder, entries, config, defaults);
        addEffects(builder, entries, config, defaults);

        builder.setSavingRunnable(() -> {
            config.normalize();
            if (!manager.save()) {
                CobblemonPokeSnacksControls.LOGGER.error("Could not save configuration from the Cloth Config screen.");
            }
            CobblemonPokeSnacksControls.snackActivityManager().refreshAll();
        });
        return builder.build();
    }

    private static void addGeneral(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            SnackControlConfig config,
            SnackControlConfig defaults
    ) {
        ConfigCategory category = builder.getOrCreateCategory(text("category.general"));
        category.addEntry(entries.startBooleanToggle(text("general.enabled"), config.general.enabled)
                .setDefaultValue(defaults.general.enabled)
                .setSaveConsumer(value -> config.general.enabled = value)
                .build());
        category.addEntry(entries.startBooleanToggle(text("general.debug_logging"), config.general.debugLogging)
                .setDefaultValue(defaults.general.debugLogging)
                .setSaveConsumer(value -> config.general.debugLogging = value)
                .build());
        category.addEntry(entries.startBooleanToggle(text("general.notify_rare_spawns"), config.general.notifyRareSpawns)
                .setDefaultValue(defaults.general.notifyRareSpawns)
                .setTooltip(text("general.notify_rare_spawns.tooltip"))
                .setSaveConsumer(value -> config.general.notifyRareSpawns = value)
                .build());
    }

    private static void addActivity(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            SnackControlConfig config,
            SnackControlConfig defaults
    ) {
        ConfigCategory category = builder.getOrCreateCategory(text("category.activity"));
        category.addEntry(entries.startIntField(text("activity.max_active"), config.activity.maxActiveSnacksPerCluster)
                .setDefaultValue(defaults.activity.maxActiveSnacksPerCluster)
                .setMin(1)
                .setSaveConsumer(value -> config.activity.maxActiveSnacksPerCluster = value)
                .build());
        category.addEntry(entries.startEnumSelector(
                        text("activity.activation_order"),
                        SnackControlConfig.ActivationOrder.class,
                        config.activity.activationOrder)
                .setDefaultValue(defaults.activity.activationOrder)
                .setEnumNameProvider(value -> text("activity.activation_order."
                        + value.name().toLowerCase(Locale.ROOT)))
                .setSaveConsumer(value -> config.activity.activationOrder = value)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        text("activity.pause_standby"),
                        config.activity.pauseStandbySnacks)
                .setDefaultValue(defaults.activity.pauseStandbySnacks)
                .setTooltip(text("activity.pause_standby.tooltip"))
                .setSaveConsumer(value -> config.activity.pauseStandbySnacks = value)
                .build());
    }

    private static void addJade(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            SnackControlConfig config,
            SnackControlConfig defaults
    ) {
        ConfigCategory category = builder.getOrCreateCategory(text("category.jade"));
        category.addEntry(entries.startBooleanToggle(text("jade.enabled"), config.jade.enabled)
                .setDefaultValue(defaults.jade.enabled)
                .setSaveConsumer(value -> config.jade.enabled = value)
                .build());
        category.addEntry(entries.startBooleanToggle(text("jade.show_status"), config.jade.showStatus)
                .setDefaultValue(defaults.jade.showStatus)
                .setSaveConsumer(value -> config.jade.showStatus = value)
                .build());
        category.addEntry(entries.startBooleanToggle(text("jade.show_capacity"), config.jade.showCapacity)
                .setDefaultValue(defaults.jade.showCapacity)
                .setSaveConsumer(value -> config.jade.showCapacity = value)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        text("jade.show_queue_position"),
                        config.jade.showQueuePositionToOperators)
                .setDefaultValue(defaults.jade.showQueuePositionToOperators)
                .setSaveConsumer(value -> config.jade.showQueuePositionToOperators = value)
                .build());
        category.addEntry(entries.startBooleanToggle(text("jade.show_last_spawn"), config.jade.showLastSpawn)
                .setDefaultValue(defaults.jade.showLastSpawn)
                .setSaveConsumer(value -> config.jade.showLastSpawn = value)
                .build());
    }

    private static void addEffects(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            SnackControlConfig config,
            SnackControlConfig defaults
    ) {
        ConfigCategory category = builder.getOrCreateCategory(text("category.effects"));
        for (SnackEffectCategory effectCategory : SnackEffectCategory.values()) {
            EffectRule rule = config.effects.rule(effectCategory);
            EffectRule defaultRule = defaults.effects.rule(effectCategory);
            SubCategoryBuilder effect = entries.startSubCategory(text(
                    "effects.category." + effectCategory.name().toLowerCase(Locale.ROOT)));

            effect.add(entries.startBooleanToggle(text("effects.enabled"), rule.enabled)
                    .setDefaultValue(defaultRule.enabled)
                    .setSaveConsumer(value -> rule.enabled = value)
                    .build());
            effect.add(entries.startDoubleField(text("effects.value_multiplier"), rule.valueMultiplier)
                    .setDefaultValue(defaultRule.valueMultiplier)
                    .setMin(0.0D)
                    .setErrorSupplier(PokeSnacksConfigScreen::finiteNumberError)
                    .setSaveConsumer(value -> rule.valueMultiplier = value)
                    .build());
            effect.add(entries.startDoubleField(text("effects.chance_multiplier"), rule.chanceMultiplier)
                    .setDefaultValue(defaultRule.chanceMultiplier)
                    .setMin(0.0D)
                    .setErrorSupplier(PokeSnacksConfigScreen::finiteNumberError)
                    .setSaveConsumer(value -> rule.chanceMultiplier = value)
                    .build());
            effect.add(entries.startStrField(text("effects.maximum_value"), optionalNumber(rule.maximumValue))
                    .setDefaultValue("")
                    .setTooltip(text("effects.optional_number.tooltip"))
                    .setErrorSupplier(value -> optionalNumberError(value, 0.0D, null))
                    .setSaveConsumer(value -> rule.maximumValue = parseOptionalNumber(value))
                    .build());
            effect.add(entries.startStrField(text("effects.maximum_chance"), optionalNumber(rule.maximumChance))
                    .setDefaultValue("")
                    .setTooltip(text("effects.optional_number.tooltip"))
                    .setErrorSupplier(value -> optionalNumberError(value, 0.0D, 1.0D))
                    .setSaveConsumer(value -> rule.maximumChance = parseOptionalNumber(value))
                    .build());
            category.addEntry(effect.build());
        }
    }

    private static Optional<Component> finiteNumberError(double value) {
        return Double.isFinite(value) ? Optional.empty() : Optional.of(text("error.number"));
    }

    private static Optional<Component> optionalNumberError(String rawValue, double minimum, Double maximum) {
        String value = normalizeNumber(rawValue);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                return Optional.of(text("error.number"));
            }
            if (parsed < minimum) {
                return Optional.of(Component.translatable(PREFIX + "error.minimum", minimum));
            }
            if (maximum != null && parsed > maximum) {
                return Optional.of(Component.translatable(PREFIX + "error.maximum", maximum));
            }
            return Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.of(text("error.number"));
        }
    }

    private static Double parseOptionalNumber(String rawValue) {
        String value = normalizeNumber(rawValue);
        return value.isEmpty() ? null : Double.parseDouble(value);
    }

    private static String normalizeNumber(String value) {
        return value.trim().replace(',', '.');
    }

    private static String optionalNumber(Double value) {
        return value == null ? "" : Double.toString(value);
    }

    private static Component text(String suffix) {
        return Component.translatable(PREFIX + suffix);
    }
}
