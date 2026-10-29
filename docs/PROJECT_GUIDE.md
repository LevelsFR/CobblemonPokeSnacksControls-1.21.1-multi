# Cobblemon PokeSnacks Controls — project guide

Server-side capacity, queue and bait-effect controls for native Cobblemon Poké Snacks.

Source and publication snapshot: **2026-10-29**. This guide describes the checked-out source; a download page can contain an earlier release.

## Status and official links

- [Source repository](https://github.com/LevelsFR/CobblemonPokeSnacksControls-1.21.1-multi/tree/master) (public).
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pokesnacks-controls) — published project verified under LevelsFR.
- [Modrinth](https://modrinth.com/mod/pokesnacks-controls) — approved project in the LevelsFR profile.

## Features in the current source

- Transitive overlap clusters, deterministic active capacity and standby ordering.
- Optional standby pausing and safe bite transfer to an active snack.
- Configurable copied bait-effect values, multipliers and meaningful aggregate caps.
- Persistent placement ordering and runtime state rebuilt from loaded snacks.
- Native lure creation, queue diagnostics, Cloth Config and optional Jade integration.

## Compatibility and dependencies

| Component | Value |
| --- | --- |
| Source version | `2.2.1` |
| Minecraft baseline | `1.21.1` |
| JDK | `21` |
| Active branch | `master` |

| Loader | Dependency | Declared constraint | Requirement |
| --- | --- | --- | --- |
| Fabric | `fabricloader` | `>=0.17.2` | required |
| Fabric | `fabric-api` | `>=0.116.6+1.21.1` | required |
| Fabric | `minecraft` | `1.21.1` | required |
| Fabric | `java` | `>=21` | required |
| Fabric | `cobblemon` | `>=1.8.2 <1.8.5` | required |
| Fabric | `os_cobblemon_library` | `>=1.5.2` | required |
| Fabric | `cloth-config` | `>=15.0.140` | recommends |
| Fabric | `modmenu` | `>=11.0.3` | recommends |
| Fabric | `jade` | `*` | suggests |
| NeoForge | `minecraft` | `[1.21.1,1.21.2)` | required; BOTH |
| NeoForge | `neoforge` | `[21.1.182,)` | required; BOTH |
| NeoForge | `cobblemon` | `[1.8.2,1.8.5)` | required; BOTH |
| NeoForge | `os_cobblemon_library` | `[1.5.2,)` | required; BOTH |
| NeoForge | `jade` | `[0,)` | optional; BOTH |
| NeoForge | `cloth_config` | `[15.0.140,)` | required; CLIENT |

The table reflects loader metadata. Build-time version pins are in [gradle.properties](../gradle.properties). A dependency used only for compilation or optional integration is not automatically required for every installation.

## Installation and first use

1. Choose a released JAR matching both the Minecraft version and loader, when this project has a release page.
2. Install the required dependencies declared for that loader. A source-only development version may require locally built dependencies.
3. Use a separate development instance for unreleased builds. Keep client-side integrations on the correct side and follow the main README for server installation.
4. Start the game/server once to create configuration, then use the project settings or documented commands.

### Command entry points

- `/pokesnackscontrols queue`
- `/pokesnackscontrols status`
- `/pokesnackscontrols stats`
- `/pokesnackscontrols reload`

Commands with arguments have contextual completion in game. Permission checks and the complete argument tree are defined in the linked command sources below.

## Command implementation reference

These are literal builder nodes found in each source file, not independent full command syntaxes. Read the builder tree for nesting, arguments and permissions.

| Source | Declared command nodes |
| --- | --- |
| [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/command/CommandRegistry.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/command/CommandRegistry.java) | `queue`, `status`, `stats`, `debug`, `nearest`, `reload`, `snack`, `give` |

## Configuration and saved data

Configuration and persistence entry points are listed in the source map below and in the existing project documentation.

### Selected source defaults

These values are field initializers from the current configuration classes. Nested sections, validation, migrations and server synchronization can affect the final effective value.

[common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRule.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRule.java)

- `enabled = true`
- `valueMultiplier = 1.0D`
- `chanceMultiplier = 1.0D`

[common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/SnackControlConfig.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/SnackControlConfig.java)

- `CURRENT_SCHEMA_VERSION = 6`
- `schemaVersion = CURRENT_SCHEMA_VERSION`
- `enabled = true`
- `debugLogging = false`
- `notifyRareSpawns = false`
- `maxActiveSnacksPerCluster = 2`
- `pauseStandbySnacks = true`
- `enabled = true`
- `showStatus = true`
- `showCapacity = true`
- `showQueuePositionToOperators = true`
- `showLastSpawn = true`

Keep world data and player data with the matching world backup. Use the mod-specific reload/configuration route documented in the main README; changing an arbitrary file does not imply live reloading. Do not copy development-world data into the source repository.

## Source map

| Directory | Responsibility |
| --- | --- |
| [common](../common) | Shared behavior and resources; loader-independent contracts where the architecture provides them. |
| [fabric](../fabric) | Fabric entrypoints, metadata and platform integration. |
| [neoforge](../neoforge) | NeoForge entrypoints, metadata and platform integration. |
| [gradle](../gradle) | Committed wrapper and build configuration. |

Metadata: [fabric/src/main/resources/fabric.mod.json](../fabric/src/main/resources/fabric.mod.json), [neoforge/src/main/resources/META-INF/neoforge.mods.toml](../neoforge/src/main/resources/META-INF/neoforge.mods.toml).

| Package | Entry points to inspect |
| --- | --- |
| `fr/levelsfr/cobblemonpokesnackscontrols/command` | [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/command/CommandRegistry.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/command/CommandRegistry.java) |
| `fr/levelsfr/cobblemonpokesnackscontrols/compat` | [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/SnackTooltipData.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/SnackTooltipData.java) |
| `fr/levelsfr/cobblemonpokesnackscontrols/compat/jade` | [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeData.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeData.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeDataPolicy.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeDataPolicy.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadePlugin.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadePlugin.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeProvider.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/compat/jade/PokeSnackJadeProvider.java) |
| `fr/levelsfr/cobblemonpokesnackscontrols/config` | [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/ConfigManager.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/ConfigManager.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRule.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRule.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRuleJsonAdapter.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/EffectRuleJsonAdapter.java), [common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/SnackControlConfig.java](../common/src/main/java/fr/levelsfr/cobblemonpokesnackscontrols/config/SnackControlConfig.java) |

## Build from source

Run commands from the repository root using a matching JDK and the committed Gradle wrapper.

```powershell
.\gradlew.bat :fabric:build :neoforge:build
```

Loader JARs are normally written under the corresponding `build/libs` directory. Aggregate projects document their collected artifact layout in the main README or build helper.

The build pins OS Cobblemon Library `1.5.2`. Inspect the build repositories before compiling; projects that resolve it from Maven Local need the matching local publication. See the [library Maven Local guide](https://github.com/LevelsFR/OS-Cobblemon-Library/blob/levelsfr/oscl-1.5.2-loader-detection/docs/LOCAL_MAVEN.md).

## Documentation index

- [ARCHITECTURE.md](../ARCHITECTURE.md)
- [CHANGELOG.md](../CHANGELOG.md)
- [README.md](../README.md)

## Validation and reporting a problem

A source audit, successful compilation, automated checks and in-game validation are different forms of evidence. This documentation update alone does not establish runtime compatibility. Preserve the project-specific remaining release gates.

When reporting a problem, include:

- Exact mod version, Minecraft version, loader and dependency versions.
- Client, integrated server or dedicated-server context.
- Minimal reproduction steps and the relevant log/stack trace.
- Configuration or datapack excerpts required to reproduce it, with credentials and personal data removed.

Before a release, compile every affected loader and configured version, inspect packaged metadata/resources, and perform the relevant client and dedicated-server checks. Rendering, interactions, networking and persistence need real runtime validation.

## License and attribution

See [LICENSE](../LICENSE). Retain upstream notices and third-party asset attribution.
