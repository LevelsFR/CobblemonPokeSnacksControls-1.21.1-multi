package fr.levelsfr.cobblemonpokesnackscontrols.mixin;

import com.cobblemon.mod.common.api.fishing.SpawnBait;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import fr.levelsfr.cobblemonpokesnackscontrols.effect.CobblemonBaitEffectAdapter;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackPlacementSequenceCarrier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PokeSnackBlockEntity.class, remap = false)
public abstract class PokeSnackBlockEntityMixin implements SnackPlacementSequenceCarrier {
    @Unique
    private static final String COBBLEMON_POKE_SNACKS_CONTROLS_SEQUENCE_KEY = "CobblemonPokeSnacksControlsPlacementSequence";

    @Unique
    private long cobblemonPokeSnacksControls$placementSequence = 0L;

    @Override
    public long cobblemonPokeSnacksControls$getPlacementSequence() {
        return cobblemonPokeSnacksControls$placementSequence;
    }

    @Override
    public void cobblemonPokeSnacksControls$setPlacementSequence(long sequence) {
        cobblemonPokeSnacksControls$placementSequence = Math.max(0L, sequence);
    }

    @Inject(
            method = {"saveAdditional", "method_11007"},
            at = @At("TAIL"),
            remap = false,
            require = 1
    )
    private void cobblemonPokeSnacksControls$savePlacementSequence(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callbackInfo
    ) {
        if (cobblemonPokeSnacksControls$placementSequence > 0L) {
            tag.putLong(COBBLEMON_POKE_SNACKS_CONTROLS_SEQUENCE_KEY, cobblemonPokeSnacksControls$placementSequence);
        }
    }

    @Inject(
            method = {"loadAdditional", "method_11014"},
            at = @At("TAIL"),
            remap = false,
            require = 1
    )
    private void cobblemonPokeSnacksControls$loadPlacementSequence(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callbackInfo
    ) {
        if (tag.contains(COBBLEMON_POKE_SNACKS_CONTROLS_SEQUENCE_KEY)) {
            cobblemonPokeSnacksControls$placementSequence = Math.max(0L, tag.getLong(COBBLEMON_POKE_SNACKS_CONTROLS_SEQUENCE_KEY));
        }
    }

    @Inject(method = "getBaitEffects", at = @At("RETURN"), cancellable = true, remap = false)
    private void cobblemonPokeSnacksControls$adjustBaitEffects(CallbackInfoReturnable<List<SpawnBait.Effect>> callbackInfo) {
        callbackInfo.setReturnValue(CobblemonBaitEffectAdapter.adjust(callbackInfo.getReturnValue()));
    }

    @Inject(method = "affectSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobblemonPokeSnacksControls$routeStandbyConsumption(
            SpawnAction<?> action,
            Entity entity,
            CallbackInfo callbackInfo
    ) {
        PokeSnackBlockEntity self = (PokeSnackBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var target = CobblemonSnackHooks.consumptionTarget(serverLevel, self, entity.blockPosition());
        if (target.isPresent() && target.orElseThrow() != self) {
            target.orElseThrow().affectSpawn(action, entity);
            callbackInfo.cancel();
        } else if (target.isEmpty()) {
            callbackInfo.cancel();
        }
    }
}
