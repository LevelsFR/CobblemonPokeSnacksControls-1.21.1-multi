package fr.levelsfr.cobblemonpokesnackscontrols;

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.ResolvedEffectRule;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.SnackEffectCategory;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackPlacementSequenceCarrier;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackPlacementSequenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Stable loader-neutral boundary for the upcoming Cobblemon mixins/events.
 * Keeping all hook calls here prevents gameplay-specific code from leaking into loader entrypoints.
 */
public final class CobblemonSnackHooks {
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
                blockEntity.setChanged();
            } else {
                sequenceData.observe(sequence);
            }
            var result = CobblemonPokeSnacksControls.snackActivityManager()
                    .registerResolved(level.dimension(), blockEntity.getBlockPos(), blockEntity.getPlacedBy(), sequence);
            if (result.sequenceChanged()) {
                sequence = result.placementSequence();
                carrier.cobblemonPokeSnacksControls$setPlacementSequence(sequence);
                blockEntity.setChanged();
            }
            sequenceData.observe(sequence);
            return;
        }
        onSnackAvailable(level.dimension(), blockEntity.getBlockPos(), blockEntity.getPlacedBy());
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

    public static ResolvedEffectRule effectRule(SnackEffectCategory category) {
        return CobblemonPokeSnacksControls.effectRuleEngine().resolve(category);
    }
}
