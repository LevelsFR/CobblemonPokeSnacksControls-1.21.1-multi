package fr.levelsfr.cobblemonpokesnackscontrols.mixin;

import com.cobblemon.mod.common.block.PokeSnackBlock;
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokeSnackBlock.class, remap = false)
public abstract class PokeSnackBlockMixin {
    @Inject(
            method = "setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL"),
            remap = true
    )
    private void cobblemonPokeSnacksControls$registerAfterPlacement(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack,
            CallbackInfo callbackInfo
    ) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof PokeSnackBlockEntity blockEntity) {
            CobblemonSnackHooks.ensureSnackRegistered(serverLevel, blockEntity);
        }
    }

    @Inject(
            method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void cobblemonPokeSnacksControls$skipStandbySnackTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo callbackInfo
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PokeSnackBlockEntity pokeSnackBlockEntity
                && !CobblemonSnackHooks.mayAdvanceNativeSnack(level, pokeSnackBlockEntity)) {
            callbackInfo.cancel();
        }
    }
}
