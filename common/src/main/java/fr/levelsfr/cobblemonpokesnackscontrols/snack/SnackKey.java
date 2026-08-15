package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

public record SnackKey(ResourceKey<Level> dimension, BlockPos position) {
    public SnackKey {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        position = position.immutable();
    }
}
