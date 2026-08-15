# Cobblemon PokeSnacks Controls

Server-side multi-loader sidemod for Minecraft 1.21.1 and Cobblemon 1.7.3.

Cobblemon owns the native Poke Snack block, block entity, bait effects, and spawning behavior. This mod only manages how many loaded native Poke Snacks may progress in an overlapping area and optionally adjusts native bait-effect inputs from server config.

## Requirements

- Java 21
- Minecraft 1.21.1
- Cobblemon 1.7.3
- Fabric Loader 0.17.2+ with Fabric API 0.116.6+1.21.1, or NeoForge 21.1.182+
- Jade is optional

Because v1.2 uses internal mixins, metadata intentionally targets Cobblemon 1.7.3 instead of claiming untested future 1.7.x compatibility.

## Active Capacity

Cobblemon 1.7.3 Poke Snacks use a native area with horizontal radius 8 and vertical radius 8. Two snacks overlap when their native areas touch: 16 blocks overlaps, 17 blocks does not. Clusters are transitive, and dimensions are always independent.

By default, only two snacks in the same cluster are active. Extra loaded snacks remain native blocks and enter the queue. `activity.activationOrder` chooses `OLDEST_FIRST` or `NEWEST_FIRST`.

When `activity.pauseStandbySnacks` is `true`, queued snacks do not advance their native random tick. When it is `false`, over-capacity snacks continue normal Cobblemon gameplay and UI describes them as running over the configured limit instead of paused.

## Persistence

Each snack stores `CobblemonPokeSnacksControlsPlacementSequence` in block entity NBT. v1.2 repairs invalid or duplicated copied-NBT sequences during registration, writes the corrected sequence back to the block entity, and keeps ordering deterministic with dimension and position tie-breakers.

Runtime queue state is rebuilt from loaded snacks on server startup. The runtime registry is cleared at the early server-starting lifecycle event and again on server stop.

## Commands

- `/pokesnackscontrols queue`
- `/pokesnackscontrols status` requires permission level 2
- `/pokesnackscontrols stats` requires permission level 2
- `/pokesnackscontrols reload` requires permission level 2

Reload re-reads server JSON config, refreshes tracked queue state, and reports failure if config recovery or saving fails.

## Configuration

Config path remains `config/cobblemon_pokesnacks_controls.json`. Existing v1.1-style configs are normalized where possible. Legacy `multiplier` migrates to both `valueMultiplier` and `chanceMultiplier`.

Malformed JSON or wrong value types are logged, moved aside with a unique `.broken-<timestamp>-<uuid>` suffix, and replaced with defaults. The broken file is not overwritten silently.

## Effects

Known Cobblemon `SpawnBait.Effect` categories are adjusted as copied values for the current native calculation. Unknown datapack effects pass through unchanged.

Multipliers are applied first. Where a configured final chance/value cap is meaningful for the native effect category, v1.2 caps the aggregate category total so stacked effects cannot exceed the configured maximum after Cobblemon merges them. The mod does not mutate Cobblemon registries, item stack components, or block entity components.

Reloaded effect rules apply when Cobblemon asks the Poke Snack block entity for bait effects again. Any deeper Cobblemon internal cache invalidation still needs production gameplay validation.

## Jade

Jade is optional. Loader entrypoints and common server initialization do not require Jade classes. When present, Jade can show status, capacity, operator queue position, and bait effects using only data sent by the server provider.

Nature bait tooltips report the boosted stat Cobblemon exposes instead of inventing a full Pokemon nature.

## Build

```bash
./gradlew clean build
```

Final remapped jars:

- `fabric/build/libs/cobblemon-pokesnacks-controls-fabric-1.2.jar`
- `neoforge/build/libs/cobblemon-pokesnacks-controls-neoforge-1.2.jar`

## License

PokéSnacks Controls is licensed under the Mozilla Public License 2.0.

## Known Limits

- A production-like dedicated server boot with the final jars was not run in this workspace.
- Reloaded effect settings were covered by automated adapter tests, but in-game confirmation of any Cobblemon-internal cached snack influences remains manual validation work.
