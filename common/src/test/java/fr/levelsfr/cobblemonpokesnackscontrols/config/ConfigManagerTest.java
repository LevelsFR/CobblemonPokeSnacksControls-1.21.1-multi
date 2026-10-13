package fr.levelsfr.cobblemonpokesnackscontrols.config;

import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDefaultConfig() {
        ConfigManager manager = new ConfigManager(tempDir.resolve("config.json"));

        assertEquals(ConfigManager.ReloadResult.CREATED_DEFAULT, manager.load());
        assertEquals(2, manager.get().activity.maxActiveSnacksPerCluster);
        assertEquals(SnackControlConfig.CURRENT_SCHEMA_VERSION, manager.get().schemaVersion);
    }

    @Test
    void movesLegacyConfigIntoTheModSubdirectory() throws Exception {
        Path legacyPath = tempDir.resolve("cobblemon_pokesnacks_controls.json");
        Path newPath = tempDir.resolve("cobblemon_pokesnacks_controls")
                .resolve("cobblemon_pokesnacks_controls.json");
        Files.writeString(legacyPath, """
                {
                  "activity": {
                    "maxActiveSnacksPerCluster": 7
                  }
                }
                """);

        ConfigManager manager = new ConfigManager(newPath, legacyPath);

        assertEquals(ConfigManager.ReloadResult.LOADED, manager.load());
        assertEquals(7, manager.get().activity.maxActiveSnacksPerCluster);
        assertTrue(Files.exists(newPath));
        assertTrue(!Files.exists(legacyPath));
    }

    @Test
    void migratesLegacyMultiplierAndRewritesNormalizedConfig() throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 1,
                  "effects": {
                    "rules": {
                      "SHINY": {
                        "enabled": true,
                        "multiplier": 2.0,
                        "maximumChance": 0.75
                      }
                    }
                  }
                }
                """);

        ConfigManager manager = new ConfigManager(path);
        assertEquals(ConfigManager.ReloadResult.LOADED, manager.load());
        EffectRule shiny = manager.get().effects.rule(SnackEffectCategory.SHINY);

        assertEquals(2.0D, shiny.valueMultiplier);
        assertEquals(2.0D, shiny.chanceMultiplier);
        assertTrue(Files.readString(path).contains("\"valueMultiplier\""));
        assertTrue(!Files.readString(path).contains("\"multiplier\""));
    }

    @Test
    void migratesSchemaThreeDefaultsForExistingConfigs() throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 2,
                  "activity": {
                    "maxActiveSnacksPerCluster": 2,
                    "activationOrder": "OLDEST_FIRST",
                    "pauseStandbySnacks": true
                  },
                  "jade": {
                    "enabled": true,
                    "showStatus": true,
                    "showCapacity": true
                  }
                }
                """);

        ConfigManager manager = new ConfigManager(path);
        assertEquals(ConfigManager.ReloadResult.LOADED, manager.load());

        assertTrue(manager.get().jade.showQueuePositionToOperators);
        assertEquals(SnackControlConfig.CURRENT_SCHEMA_VERSION, manager.get().schemaVersion);
    }

    @Test
    void schemaFourConfigGetsNeutralCobblemonOneEightRules() throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 4,
                  "effects": {
                    "rules": {
                      "SHINY": {
                        "enabled": false,
                        "valueMultiplier": 0.5,
                        "chanceMultiplier": 0.25,
                        "maximumValue": 2.0,
                        "maximumChance": 0.1
                      }
                    }
                  }
                }
                """);

        ConfigManager manager = new ConfigManager(path);
        assertEquals(ConfigManager.ReloadResult.LOADED, manager.load());

        EffectRule shiny = manager.get().effects.rule(SnackEffectCategory.SHINY);
        assertTrue(!shiny.enabled);
        assertEquals(0.5D, shiny.valueMultiplier);
        assertEquals(0.25D, shiny.chanceMultiplier);
        assertEquals(2.0D, shiny.maximumValue);
        assertEquals(0.1D, shiny.maximumChance);

        for (SnackEffectCategory category : new SnackEffectCategory[]{
                SnackEffectCategory.ALPHA,
                SnackEffectCategory.MARK,
                SnackEffectCategory.SIZE
        }) {
            EffectRule added = manager.get().effects.rule(category);
            assertTrue(added.enabled);
            assertEquals(1.0D, added.valueMultiplier);
            assertEquals(1.0D, added.chanceMultiplier);
            assertEquals(null, added.maximumValue);
            assertEquals(null, added.maximumChance);
        }
        assertEquals(SnackControlConfig.CURRENT_SCHEMA_VERSION, manager.get().schemaVersion);
    }

    @Test
    void recoversCorruptedJsonAndKeepsDiagnosticBackup() throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, "{ broken");

        ConfigManager manager = new ConfigManager(path);

        assertEquals(ConfigManager.ReloadResult.RECOVERED_DEFAULT, manager.load());
        assertTrue(Files.exists(path));
        assertTrue(Files.list(tempDir).anyMatch(file -> file.getFileName().toString().contains(".broken-")));
    }

    @Test
    void recoversWrongJsonValueTypesAndKeepsDiagnosticBackup() throws Exception {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, """
                {
                  "general": {
                    "enabled": {}
                  }
                }
                """);

        ConfigManager manager = new ConfigManager(path);

        assertEquals(ConfigManager.ReloadResult.RECOVERED_DEFAULT, manager.load());
        assertTrue(Files.exists(path));
        assertTrue(Files.list(tempDir).anyMatch(file -> file.getFileName().toString().contains(".broken-")));
    }

    @Test
    void repeatedBrokenLoadsCreateDistinctBackups() throws Exception {
        Path path = tempDir.resolve("config.json");
        ConfigManager manager = new ConfigManager(path);

        Files.writeString(path, "{ broken");
        assertEquals(ConfigManager.ReloadResult.RECOVERED_DEFAULT, manager.load());
        Files.writeString(path, "{ broken again");
        assertEquals(ConfigManager.ReloadResult.RECOVERED_DEFAULT, manager.load());

        long backups = Files.list(tempDir)
                .filter(file -> file.getFileName().toString().contains(".broken-"))
                .count();
        assertEquals(2, backups);
    }
}
