package fr.levelsfr.cobblemonpokesnackscontrols;

import fr.levelsfr.cobblemonpokesnackscontrols.config.ConfigManager;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectRuleEngine;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackActivityManager;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackRuntimeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CobblemonPokeSnacksControls {
    public static final String MOD_ID = "cobblemon_pokesnacks_controls";
    public static final String MOD_NAME = "Cobblemon PokéSnacks Controls";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static ConfigManager configManager;
    private static SnackActivityManager snackActivityManager;
    private static SnackEffectRuleEngine effectRuleEngine;

    private CobblemonPokeSnacksControls() {
    }

    public static void initialize(Path configDirectory) {
        Objects.requireNonNull(configDirectory, "configDirectory");
        if (!INITIALIZED.compareAndSet(false, true)) {
            LOGGER.warn("{} was initialized more than once; ignoring the duplicate call.", MOD_NAME);
            return;
        }

        Path configPath = configDirectory.resolve(MOD_ID).resolve(MOD_ID + ".json");
        Path legacyConfigPath = configDirectory.resolve(MOD_ID + ".json");
        configManager = new ConfigManager(configPath, legacyConfigPath);
        configManager.load();
        snackActivityManager = new SnackActivityManager(configManager::get);
        snackActivityManager.addListener(CobblemonPokeSnacksControls::logSnackStateChange);
        effectRuleEngine = new SnackEffectRuleEngine(configManager::get);

        LOGGER.info("Initialized {} with {} active snacks allowed per native-area cluster.",
                MOD_NAME,
                configManager.get().activity.maxActiveSnacksPerCluster);
    }

    public static ConfigManager configManager() {
        ensureInitialized();
        return configManager;
    }

    public static SnackActivityManager snackActivityManager() {
        ensureInitialized();
        return snackActivityManager;
    }

    public static SnackEffectRuleEngine effectRuleEngine() {
        ensureInitialized();
        return effectRuleEngine;
    }

    private static void ensureInitialized() {
        if (!INITIALIZED.get()) {
            throw new IllegalStateException(MOD_NAME + " has not been initialized yet");
        }
    }

    private static void logSnackStateChange(SnackRuntimeState previousState, SnackRuntimeState newState) {
        if (!configManager.get().general.debugLogging || previousState.status() == newState.status()) {
            return;
        }

        var position = newState.key().position();
        LOGGER.info(
                "Poke Snack transition {} -> {} at {} {} {} in {} (cluster {}, active {}/{}, queue position {}, sequence {}).",
                previousState.status(),
                newState.status(),
                position.getX(),
                position.getY(),
                position.getZ(),
                newState.key().dimension().location(),
                newState.clusterId(),
                newState.activeInCluster(),
                newState.clusterSize(),
                newState.queuePosition(),
                newState.placementSequence()
        );
    }
}
