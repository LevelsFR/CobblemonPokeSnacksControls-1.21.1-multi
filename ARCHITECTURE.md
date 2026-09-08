# Architecture

## Rule

Cobblemon owns Poke Snacks. This mod does not add a custom snack block, spawner, spawn pool, item, recipe, texture, or client-authoritative queue.

## Modules

`config` loads, normalizes, migrates, saves, and recovers server JSON config under the mod-specific config subdirectory. The former root-level JSON file is moved automatically when the new path is empty. Bad files are backed up with collision-safe names before defaults are regenerated.

`snack` owns runtime state: dimension, position, persistent placement sequence, active/standby status, queue position, cluster membership, and summary counts. Registration repairs duplicated placement sequences and cluster recomputation uses spatial buckets plus exact native overlap checks.

`effect` maps Cobblemon `SpawnBait.Effect` identifiers to supported categories and returns adjusted copies for the current native calculation. Category caps are enforced against aggregate adjusted totals where supported, with signed magnitude handling isolated to Cobblemon 1.8's Size effect.

`command` exposes queue, status, stats, debug nearest, and reload commands.

`compat.jade` is isolated behind Jade plugin discovery. Required loader entrypoints do not reference Jade classes.

`client.PokeSnacksConfigScreen` builds the shared Cloth Config UI. Fabric exposes it through the optional Mod Menu entrypoint, while NeoForge registers its client-only config screen extension point. Dedicated-server entrypoints do not load Cloth Config classes.

## Mixins

`BlockEntityLifecycleMixin` targets vanilla `BlockEntity` and injects descriptor-qualified `setLevel(Lnet/minecraft/world/level/Level;)V` and `setRemoved()V`. The production Fabric refmap remaps those to intermediary `method_31662` and `method_11012`.

`PokeSnackBlockMixin` targets Cobblemon `PokeSnackBlock`, registers placed snacks after native placement, and cancels native random ticks only when strict standby pausing is enabled and the snack is outside active capacity.

`PokeSnackBlockEntityMixin` persists placement sequence NBT, adjusts copied bait effects returned by Cobblemon, and intercepts Cobblemon's post-spawn `affectSpawn` callback. A callback aimed at a standby snack is transferred exactly once to the nearest loaded active snack in the same cluster; the original callback is then cancelled. Cobblemon 1.8 passes the adjusted effects to both Bite Time calculations and its native `PokeSnackSpawnerFactory` context.

The mixin config uses `defaultRequire = 1` and references the generated `common-common-refmap.json`.

## Lifecycle

Fabric uses `ServerLifecycleEvents.SERVER_STARTING`; NeoForge uses `ServerStartingEvent`. Runtime snack state is cleared there before world/block-entity registration can occur. Runtime state is also cleared on server stopping.

Loaded block entities register when their level is attached, newly placed snacks register after placement, and tick access calls registration as a fallback.

## Clusters

`NativeSnackArea` mirrors Cobblemon 1.8's native radius of 8 horizontally and vertically. Spatial cells reduce unnecessary comparisons, but exact overlap still decides membership. Transitive connected components are preserved.

Cluster IDs are runtime-only hashes of dimension and component positions, so unrelated components do not collide merely because they share a minimum placement sequence.

## Validation

`./gradlew clean build` passed with Java 21. The produced Fabric and NeoForge jars contain the mixin config and `common-common-refmap.json`; no source dumps, temporary Cobblemon source extraction, logs, local paths, or obvious secrets were found in the release jars.
