package fr.levelsfr.cobblemonpokesnackscontrols.mixin;

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityLifecycleMixin {
    /*
     * Minecraft 1.21.1 uses different runtime member names between the
     * Mojmap/NeoForge environment and Fabric's intermediary namespace.
     *
     * NeoForge/Mojmap: setLevel
     * Fabric intermediary: method_31662
     *
     * Explicit aliases avoid relying on the common refmap for this vanilla
     * lifecycle hook, which is what caused the dedicated Fabric server crash.
     */
    @Inject(
            method = {"setLevel", "method_31662"},
            at = @At("TAIL"),
            remap = false,
            require = 1
    )
    private void cobblemonPokeSnacksControls$registerLoadedSnack(Level level, CallbackInfo callbackInfo) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (level instanceof ServerLevel serverLevel && self instanceof PokeSnackBlockEntity pokeSnackBlockEntity) {
            CobblemonSnackHooks.ensureSnackRegistered(serverLevel, pokeSnackBlockEntity);
        }
    }

    /*
     * Same namespace-safe handling for block-entity removal:
     *
     * NeoForge/Mojmap: setRemoved
     * Fabric intermediary: method_11012
     */
    @Inject(
            method = {"setRemoved", "method_11012"},
            at = @At("HEAD"),
            remap = false,
            require = 1
    )
    private void cobblemonPokeSnacksControls$unregisterRemovedSnack(CallbackInfo callbackInfo) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self instanceof PokeSnackBlockEntity pokeSnackBlockEntity) {
            Level level = pokeSnackBlockEntity.getLevel();
            if (level != null && !level.isClientSide()) {
                CobblemonSnackHooks.onSnackRemoved(level.dimension(), pokeSnackBlockEntity.getBlockPos());
            }
        }
    }
}
