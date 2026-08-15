package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class SnackPlacementSequenceData extends SavedData {
    private static final String DATA_NAME = CobblemonPokeSnacksControls.MOD_ID + "_placement_sequences";
    private static final String NEXT_SEQUENCE_KEY = "NextSequence";
    private static final Factory<SnackPlacementSequenceData> FACTORY = new Factory<>(
            SnackPlacementSequenceData::new,
            SnackPlacementSequenceData::load,
            DataFixTypes.LEVEL
    );

    private long nextSequence = 1L;

    public static SnackPlacementSequenceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static SnackPlacementSequenceData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        SnackPlacementSequenceData data = new SnackPlacementSequenceData();
        if (tag.contains(NEXT_SEQUENCE_KEY)) {
            data.nextSequence = Math.max(1L, tag.getLong(NEXT_SEQUENCE_KEY));
        }
        return data;
    }

    public synchronized long next() {
        long sequence = nextSequence++;
        setDirty();
        return sequence;
    }

    public synchronized void observe(long sequence) {
        if (sequence >= nextSequence) {
            nextSequence = sequence + 1L;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putLong(NEXT_SEQUENCE_KEY, nextSequence);
        return tag;
    }
}
