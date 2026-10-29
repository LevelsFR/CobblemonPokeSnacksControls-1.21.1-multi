# Cobblemon PokeSnacks Controls

Server-side multi-loader sidemod for Minecraft 1.21.1 and Cobblemon 1.8.2.

Cobblemon owns the native Poke Snack block, block entity, bait effects, and spawning behavior. This mod only manages how many loaded native Poke Snacks may progress in an overlapping area and optionally adjusts native bait-effect inputs from server config.

## Requirements

- Java 21
- Minecraft 1.21.1
- Cobblemon 1.8.2 up to, but not including, 1.8.5
- [OS Cobblemon Library](https://github.com/LevelsFR/OS-Cobblemon-Library) 1.5.2+
- Fabric Loader 0.17.2+ with Fabric API 0.116.6+1.21.1, or NeoForge 21.1.182+
- Cloth Config 15.0.140+ on the client for the in-game configuration screen
- Mod Menu 11.0.3+ on Fabric to expose the configuration button
- Jade is optional

This release uses internal mixins, so metadata intentionally targets Cobblemon 1.8.2 up to, but not including, 1.8.5. OS Cobblemon Library is required on both loaders and supplies immutable Pokémon identity snapshots for the latest-spawn display and rare-spawn alerts.

## Active Capacity

Cobblemon 1.8 Poke Snacks use a native area with horizontal radius 8 and vertical radius 8. Two snacks overlap when their native areas touch: 16 blocks overlaps, 17 blocks does not. Clusters are transitive, and dimensions are always independent.

By default, only two snacks in the same cluster are active. Extra loaded snacks remain native blocks and enter the queue. `activity.activationOrder` chooses `OLDEST_FIRST` or `NEWEST_FIRST`.

When `activity.pauseStandbySnacks` is `true`, queued snacks do not advance their native random tick. When it is `false`, over-capacity snacks continue running and UI describes them as running over the configured limit instead of paused. In both modes, if Cobblemon's post-spawn callback selects an over-capacity snack for a bite, the mod transfers that single bite to the nearest loaded active snack in the same cluster. A standby or over-capacity snack is therefore never consumed while an active replacement exists.

## Persistence

Each snack stores `CobblemonPokeSnacksControlsPlacementSequence` in block entity NBT. The mod repairs invalid or duplicated copied-NBT sequences during registration, writes the corrected sequence back to the block entity, and keeps ordering deterministic with dimension and position tie-breakers.

Runtime queue state is rebuilt from loaded snacks on server startup. The runtime registry is cleared at the early server-starting lifecycle event and again on server stop.

## Commands

- `/pokesnackscontrols queue`
- `/pokesnackscontrols status` requires permission level 2
- `/pokesnackscontrols stats` requires permission level 2
- `/pokesnackscontrols reload` requires permission level 2
- `/pokesnackscontrols snack give <item-id> [item-id ...]` requires permission level 2 and gives the executing player a native Poké Snack lure made from up to eight ingredients.

The snack command combines the Cobblemon bait-effect entries registered for each ingredient by the active datapacks. Use namespaced item IDs; tab completion suggests registry items. Items without registered bait effects cannot be used. The resulting lure keeps the native Cobblemon effects, which still pass through this mod's configured balancing rules.

Reload re-reads server JSON config, refreshes tracked queue state, and reports failure if config recovery or saving fails.

## Configuration

The config is stored at `config/cobblemon_pokesnacks_controls/cobblemon_pokesnacks_controls.json`. On first v2 launch, the previous `config/cobblemon_pokesnacks_controls.json` file is moved there automatically. Existing v1.1-style configs are normalized where possible. Legacy `multiplier` migrates to both `valueMultiplier` and `chanceMultiplier`.

On NeoForge, open the mod list and select the Config button. On Fabric, the same button is provided through Mod Menu. Saving the Cloth Config screen writes the JSON file and immediately refreshes active/standby arbitration. On a remote dedicated server, its authoritative config must still be changed on the server itself.

Malformed JSON or wrong value types are logged, moved aside with a unique `.broken-<timestamp>-<uuid>` suffix, and replaced with defaults. The broken file is not overwritten silently.

## Effects

Known Cobblemon `SpawnBait.Effect` categories are adjusted as copied values for the current native calculation. Unknown datapack effects pass through unchanged. The admin snack command creates an item stack with Cobblemon's native bait-effects component; ordinary balancing does not rewrite stored snack item or block-entity components.

Multipliers are applied first. Where a configured final chance/value cap is meaningful for the native effect category, the mod caps the aggregate category total so stacked effects cannot exceed the configured maximum after Cobblemon merges them. Size uses a signed value: its multiplier and maximum magnitude preserve both positive and negative native effects. The mod does not mutate Cobblemon registries, item stack components, or block entity components.

Cobblemon 1.8 Alpha Chance, Mark Chance, and Size effects are available through the same neutral-by-default rule system. Adjusted effects flow through `PokeSnackBlockEntity.getBaitEffects()` into Cobblemon's native `PokeSnackSpawnerFactory`; the mod does not duplicate the native spawner or rarity buckets.

Reloaded effect rules apply when Cobblemon asks the Poke Snack block entity for bait effects again. Any deeper Cobblemon internal cache invalidation still needs production gameplay validation.

## Jade

Jade is optional. Loader entrypoints and common server initialization do not require Jade classes. When present, Jade can show status, capacity, operator queue position, bait effects, and the last successful Pokémon spawn for each snack using only data sent by the server provider. Spawn history lasts for the current server session and is cleared when the server stops.

Rare-spawn alerts are disabled by default. When enabled in the server config, the online player who placed the snack is notified if it attracts a Shiny, Alpha, or marked Pokémon.

Nature bait tooltips report the boosted stat Cobblemon exposes instead of inventing a full Pokemon nature.

## Build

For source builds, make OS Cobblemon Library 1.5.2 available in Maven Local first. From the library's matching source version, run:

```powershell
.\gradlew.bat publishAllToMavenLocal
```

Then build this project:

```bash
./gradlew clean build
```

Final remapped jars:

- `fabric/build/libs/cobblemon-pokesnacks-controls-fabric-2.2.jar`
- `neoforge/build/libs/cobblemon-pokesnacks-controls-neoforge-2.2.jar`

## License

PokéSnacks Controls is licensed under the Mozilla Public License 2.0.

## Known Limits

- A production-like dedicated server boot with the final jars was not run in this workspace.
- Reloaded effect settings were covered by automated adapter tests, but in-game confirmation of any Cobblemon-internal cached snack influences remains manual validation work.

## Project guide and verified downloads

[Complete project guide](docs/PROJECT_GUIDE.md) — features, dependency metadata, commands, configuration, source map and build instructions.

- [Source repository](https://github.com/LevelsFR/CobblemonPokeSnacksControls-1.21.1-multi/tree/master) (public).
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pokesnacks-controls) — published project verified under LevelsFR.
- [Modrinth](https://modrinth.com/mod/pokesnacks-controls) — approved project in the LevelsFR profile.
