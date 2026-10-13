package fr.levelsfr.cobblemonpokesnackscontrols.config;

import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectCategory;

import java.util.EnumMap;
import java.util.Map;

public final class SnackControlConfig {
    public static final int CURRENT_SCHEMA_VERSION = 6;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public General general = new General();
    public Activity activity = new Activity();
    public Effects effects = new Effects();
    public Jade jade = new Jade();

    public void normalize() {
        int loadedSchemaVersion = schemaVersion;
        schemaVersion = CURRENT_SCHEMA_VERSION;
        if (general == null) general = new General();
        if (activity == null) activity = new Activity();
        if (effects == null) effects = new Effects();
        if (jade == null) jade = new Jade();

        activity.normalize(loadedSchemaVersion);
        effects.normalize();
        jade.normalize(loadedSchemaVersion);
    }

    public static final class General {
        public boolean enabled = true;
        public boolean debugLogging = false;
        public boolean notifyRareSpawns = false;
    }

    public static final class Activity {
        public int maxActiveSnacksPerCluster = 2;
        public ActivationOrder activationOrder = ActivationOrder.OLDEST_FIRST;
        public boolean pauseStandbySnacks = true;

        private void normalize(int loadedSchemaVersion) {
            maxActiveSnacksPerCluster = Math.max(1, maxActiveSnacksPerCluster);
            if (activationOrder == null) activationOrder = ActivationOrder.OLDEST_FIRST;
        }
    }

    public static final class Effects {
        public Map<SnackEffectCategory, EffectRule> rules = new EnumMap<>(SnackEffectCategory.class);

        public Effects() {
            ensureAllCategories();
        }

        public EffectRule rule(SnackEffectCategory category) {
            ensureAllCategories();
            return rules.get(category);
        }

        private void normalize() {
            if (rules == null) {
                rules = new EnumMap<>(SnackEffectCategory.class);
            } else if (!(rules instanceof EnumMap)) {
                Map<SnackEffectCategory, EffectRule> converted = new EnumMap<>(SnackEffectCategory.class);
                converted.putAll(rules);
                rules = converted;
            }
            ensureAllCategories();
            rules.values().forEach(EffectRule::normalize);
        }

        private void ensureAllCategories() {
            for (SnackEffectCategory category : SnackEffectCategory.values()) {
                rules.computeIfAbsent(category, ignored -> new EffectRule());
            }
        }
    }

    public static final class Jade {
        public boolean enabled = true;
        public boolean showStatus = true;
        public boolean showCapacity = true;
        public boolean showQueuePositionToOperators = true;
        public boolean showLastSpawn = true;

        private void normalize(int loadedSchemaVersion) {
            if (loadedSchemaVersion < 4) {
                showQueuePositionToOperators = true;
            }
        }
    }

    public enum ActivationOrder {
        OLDEST_FIRST,
        NEWEST_FIRST
    }
}
