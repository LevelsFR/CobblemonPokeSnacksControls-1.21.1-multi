package fr.levelsfr.cobblemonpokesnackscontrols;

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.ResolvedEffectRule;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectCategory;
import fr.levelsfr.cobblemonpokesnackscontrols.lifecycle.DeferredUpdateQueue;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackPlacementSequenceCarrier;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackPlacementSequenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Stable loader-neutral boundary for the upcoming Cobblemon mixins/events.
 * Keeping all hook calls here prevents gameplay-specific code from leaking into loader entrypoints.
 */
public final class CobblemonSnackHooks {
    private static final DeferredUpdateQueue<PokeSnackBlockEntity> DEFERRED_SEQUENCE_SAVES =
            new DeferredUpdateQueue<>();

    private CobblemonSnackHooks() {
    }

    public static void onSnackAvailable(ResourceKey<Level> dimension, BlockPos position, UUID placedBy) {
        CobblemonPokeSnacksControls.snackActivityManager().register(dimension, position, placedBy);
    }

    public static void onSnackAvailable(ResourceKey<Level> dimension, BlockPos position, UUID placedBy, long placementSequence) {
        CobblemonPokeSnacksControls.snackActivityManager().register(dimension, position, placedBy, placementSequence);
    }

    public static void onSnackRemoved(ResourceKey<Level> dimension, BlockPos position) {
        CobblemonPokeSnacksControls.snackActivityManager().unregister(dimension, position);
        CobblemonPokeSnacksControls.snackSpawnHistory().remove(dimension, position);
    }

    public static void ensureSnackRegistered(ServerLevel level, PokeSnackBlockEntity blockEntity) {
        if (!blockEntity.isLure()) {
            return;
        }
        long sequence = 0L;
        if (blockEntity instanceof SnackPlacementSequenceCarrier carrier) {
            sequence = carrier.cobblemonPokeSnacksControls$getPlacementSequence();
            SnackPlacementSequenceData sequenceData = SnackPlacementSequenceData.get(level.getServer());
            if (sequence <= 0L) {
                sequence = sequenceData.next();
                carrier.cobblemonPokeSnacksControls$setPlacementSequence(sequence);
                deferSequenceSave(blockEntity);
            } else {
                sequenceData.observe(sequence);
            }
            var result = CobblemonPokeSnacksControls.snackActivityManager()
                    .registerResolved(level.dimension(), blockEntity.getBlockPos(), blockEntity.getPlacedBy(), sequence);
            if (result.sequenceChanged()) {
                sequence = result.placementSequence();
                carrier.cobblemonPokeSnacksControls$setPlacementSequence(sequence);
                deferSequenceSave(blockEntity);
            }
            sequenceData.observe(sequence);
            return;
        }
        onSnackAvailable(level.dimension(), blockEntity.getBlockPos(), blockEntity.getPlacedBy());
    }

    /**
     * Runs loader-neutral end-of-tick work after chunk post-loading has completed.
     * Calling {@link BlockEntity#setChanged()} directly from {@code setLevel()} can
     * synchronously request the chunk which is currently being post-loaded and
     * deadlock the server thread.
     */
    public static void onServerTickEnd(MinecraftServer server) {
        flushDeferredUpdates(server);
        CobblemonPokeSnacksControls.snackActivityManager().flushDirtyDimensions();
    }

    public static void flushDeferredUpdates(MinecraftServer server) {
        DEFERRED_SEQUENCE_SAVES.flush(blockEntity -> {
            if (!blockEntity.isRemoved()
                    && blockEntity.getLevel() instanceof ServerLevel level
                    && level.getServer() == server) {
                blockEntity.setChanged();
            }
        });
    }

    public static void clearDeferredUpdates() {
        DEFERRED_SEQUENCE_SAVES.clear();
    }

    private static void deferSequenceSave(PokeSnackBlockEntity blockEntity) {
        DEFERRED_SEQUENCE_SAVES.enqueue(blockEntity);
    }

    public static boolean mayAdvanceNativeSnack(ResourceKey<Level> dimension, BlockPos position) {
        var config = CobblemonPokeSnacksControls.configManager().get();
        if (!config.general.enabled || !config.activity.pauseStandbySnacks) {
            return true;
        }
        return CobblemonPokeSnacksControls.snackActivityManager()
                .state(dimension, position)
                .map(state -> state.active())
                .orElse(true);
    }

    public static boolean mayAdvanceNativeSnack(ServerLevel level, PokeSnackBlockEntity blockEntity) {
        ensureSnackRegistered(level, blockEntity);
        return mayAdvanceNativeSnack(level.dimension(), blockEntity.getBlockPos());
    }

    public static Optional<PokeSnackBlockEntity> consumptionTarget(
            ServerLevel level,
            PokeSnackBlockEntity requestedSnack,
            BlockPos consumerPosition
    ) {
        var config = CobblemonPokeSnacksControls.configManager().get();
        if (!config.general.enabled) {
            return Optional.of(requestedSnack);
        }

        ensureSnackRegistered(level, requestedSnack);
        var manager = CobblemonPokeSnacksControls.snackActivityManager();
        var requestedState = manager.state(level.dimension(), requestedSnack.getBlockPos());
        if (requestedState.isEmpty() || requestedState.orElseThrow().active()) {
            return Optional.of(requestedSnack);
        }

        for (var candidate : manager.activeConsumptionCandidates(
                level.dimension(),
                requestedSnack.getBlockPos(),
                consumerPosition
        )) {
            BlockEntity blockEntity = level.getBlockEntity(candidate.key().position());
            if (blockEntity instanceof PokeSnackBlockEntity pokeSnackBlockEntity && pokeSnackBlockEntity.isLure()) {
                return Optional.of(pokeSnackBlockEntity);
            }
        }
        return Optional.empty();
    }

    public static ResolvedEffectRule effectRule(SnackEffectCategory category) {
        return CobblemonPokeSnacksControls.effectRuleEngine().resolve(category);
    }
}
