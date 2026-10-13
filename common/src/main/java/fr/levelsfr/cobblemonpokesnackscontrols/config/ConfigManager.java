package fr.levelsfr.cobblemonpokesnackscontrols.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public final class ConfigManager {
    private static final DateTimeFormatter BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(EffectRule.class, new EffectRuleJsonAdapter())
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private final Path path;
    private final Path legacyPath;
    private volatile SnackControlConfig config = new SnackControlConfig();

    public ConfigManager(Path path) {
        this(path, null);
    }

    public ConfigManager(Path path, Path legacyPath) {
        this.path = Objects.requireNonNull(path, "path");
        this.legacyPath = legacyPath;
    }

    public synchronized ReloadResult load() {
        try {
            Files.createDirectories(path.getParent());
            Path sourcePath = migrateLegacyConfig();
            if (!Files.exists(sourcePath)) {
                config = new SnackControlConfig();
                config.normalize();
                return save() ? ReloadResult.CREATED_DEFAULT : ReloadResult.SAVE_FAILED;
            }

            try (Reader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
                SnackControlConfig loaded = GSON.fromJson(reader, SnackControlConfig.class);
                if (loaded == null) {
                    throw new JsonParseException("Configuration root cannot be null");
                }
                loaded.normalize();
                config = loaded;
            }

            // Rewrite after normalization so newly introduced fields appear in existing configs.
            return save() ? ReloadResult.LOADED : ReloadResult.SAVE_FAILED;
        } catch (IOException | RuntimeException exception) {
            CobblemonPokeSnacksControls.LOGGER.error("Failed to load configuration from {}. Restoring defaults.", path, exception);
            backupBrokenConfig(path);
            config = new SnackControlConfig();
            config.normalize();
            return save() ? ReloadResult.RECOVERED_DEFAULT : ReloadResult.RECOVERY_SAVE_FAILED;
        }
    }

    public synchronized boolean save() {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            CobblemonPokeSnacksControls.LOGGER.error("Failed to save configuration to {}.", path, exception);
            return false;
        }
    }

    public SnackControlConfig get() {
        return config;
    }

    public Path path() {
        return path;
    }

    private Path migrateLegacyConfig() throws IOException {
        if (Files.exists(path) || legacyPath == null || !Files.exists(legacyPath)) {
            return path;
        }

        try {
            Files.move(legacyPath, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(legacyPath, path);
        }
        CobblemonPokeSnacksControls.LOGGER.info("Moved legacy configuration from {} to {}.", legacyPath, path);
        return path;
    }

    private void backupBrokenConfig(Path brokenPath) {
        if (!Files.exists(brokenPath)) {
            return;
        }
        Path backup = brokenPath.resolveSibling(brokenPath.getFileName() + ".broken-"
                + BACKUP_SUFFIX.format(LocalDateTime.now())
                + "-"
                + UUID.randomUUID());
        try {
            Files.move(brokenPath, backup);
        } catch (IOException exception) {
            CobblemonPokeSnacksControls.LOGGER.warn("Could not back up broken configuration {}.", brokenPath, exception);
        }
    }

    public enum ReloadResult {
        LOADED,
        CREATED_DEFAULT,
        RECOVERED_DEFAULT,
        SAVE_FAILED,
        RECOVERY_SAVE_FAILED;

        public boolean success() {
            return this != SAVE_FAILED && this != RECOVERY_SAVE_FAILED;
        }
    }
}
