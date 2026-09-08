package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import net.minecraft.core.BlockPos;

public record NativeSnackArea(BlockPos center, int horizontalRadius, int verticalRadius) {
    public static final int NATIVE_POKE_SNACK_HORIZONTAL_RADIUS = 8;
    public static final int NATIVE_POKE_SNACK_VERTICAL_RADIUS = 8;

    public static NativeSnackArea forPokeSnack(BlockPos center) {
        return forPokeSnack(center, NATIVE_POKE_SNACK_HORIZONTAL_RADIUS, NATIVE_POKE_SNACK_VERTICAL_RADIUS);
    }

    public static NativeSnackArea forPokeSnack(BlockPos center, int horizontalRadius, int verticalRadius) {
        return new NativeSnackArea(
                center,
                horizontalRadius,
                verticalRadius
        );
    }

    public NativeSnackArea {
        center = center.immutable();
        horizontalRadius = Math.max(0, horizontalRadius);
        verticalRadius = Math.max(0, verticalRadius);
    }

    public boolean overlaps(NativeSnackArea other) {
        return Math.abs((long) center.getX() - other.center.getX()) <= (long) horizontalRadius + other.horizontalRadius
                && Math.abs((long) center.getY() - other.center.getY()) <= (long) verticalRadius + other.verticalRadius
                && Math.abs((long) center.getZ() - other.center.getZ()) <= (long) horizontalRadius + other.horizontalRadius;
    }
}
