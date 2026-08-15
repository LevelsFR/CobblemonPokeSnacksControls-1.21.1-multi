package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SnackActivityManagerTest {
    @Test
    void nativeAreasOverlapAtExactlySixteenBlocks() {
        assertTrue(NativeSnackArea.forPokeSnack(new BlockPos(0, 64, 0))
                .overlaps(NativeSnackArea.forPokeSnack(new BlockPos(16, 64, 0))));
    }

    @Test
    void nativeAreasDoNotOverlapAtSeventeenBlocks() {
        assertFalse(NativeSnackArea.forPokeSnack(new BlockPos(0, 64, 0))
                .overlaps(NativeSnackArea.forPokeSnack(new BlockPos(17, 64, 0))));
    }

    @Test
    void nativeAreasOverlapVertically() {
        assertTrue(NativeSnackArea.forPokeSnack(new BlockPos(0, 64, 0))
                .overlaps(NativeSnackArea.forPokeSnack(new BlockPos(0, 80, 0))));
    }

    @Test
    void overlappingAreasFormATransitiveClusterAndUseOldestFirst() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        manager.register(Level.OVERWORLD, new BlockPos(0, 64, 0), null);
        manager.register(Level.OVERWORLD, new BlockPos(16, 64, 0), null);
        manager.register(Level.OVERWORLD, new BlockPos(32, 64, 0), null);

        assertTrue(manager.state(Level.OVERWORLD, new BlockPos(0, 64, 0)).orElseThrow().active());
        assertTrue(manager.state(Level.OVERWORLD, new BlockPos(16, 64, 0)).orElseThrow().active());

        SnackRuntimeState standby = manager.state(Level.OVERWORLD, new BlockPos(32, 64, 0)).orElseThrow();
        assertFalse(standby.active());
        assertEquals(1, standby.queuePosition());
        assertEquals(3, standby.clusterSize());
    }

    @Test
    void nativeRadiusIsLockedToCobblemonOneSevenThree() {
        assertEquals(8, NativeSnackArea.COBBLEMON_1_7_3_HORIZONTAL_RADIUS);
        assertEquals(8, NativeSnackArea.COBBLEMON_1_7_3_VERTICAL_RADIUS);
    }

    @Test
    void nearestQueueReturnsOrderedClusterEntries() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.maxActiveSnacksPerCluster = 1;
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(16, 64, 0);
        BlockPos distant = new BlockPos(100, 64, 0);

        manager.register(Level.OVERWORLD, first, null, 1);
        manager.register(Level.OVERWORLD, second, null, 2);
        manager.register(Level.OVERWORLD, distant, null, 3);

        SnackActivityManager.QueueView queue = manager.nearestQueue(Level.OVERWORLD, new BlockPos(3, 64, 0)).orElseThrow();

        assertEquals(2, queue.clusterSize());
        assertEquals(1, queue.activeSnacks());
        assertEquals(1, queue.standbySnacks());
        assertEquals(first, queue.states().get(0).key().position());
        assertEquals(second, queue.states().get(1).key().position());
    }

    @Test
    void nearestStateReturnsClosestTrackedSnackInDimension() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos overworldFar = new BlockPos(100, 64, 0);
        BlockPos overworldNear = new BlockPos(8, 64, 0);
        BlockPos netherCloser = new BlockPos(1, 64, 0);

        manager.register(Level.OVERWORLD, overworldFar, null, 1);
        manager.register(Level.OVERWORLD, overworldNear, null, 2);
        manager.register(Level.NETHER, netherCloser, null, 3);

        SnackRuntimeState nearest = manager.nearestState(Level.OVERWORLD, new BlockPos(0, 64, 0)).orElseThrow();

        assertEquals(overworldNear, nearest.key().position());
    }

    @Test
    void statsExposeTrackedClustersDimensionsAndLargestCluster() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.maxActiveSnacksPerCluster = 1;
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        manager.register(Level.OVERWORLD, new BlockPos(0, 64, 0), null, 1);
        manager.register(Level.OVERWORLD, new BlockPos(16, 64, 0), null, 2);
        manager.register(Level.OVERWORLD, new BlockPos(100, 64, 0), null, 3);
        manager.register(Level.NETHER, new BlockPos(0, 64, 0), null, 4);

        SnackActivityManager.Stats stats = manager.stats();

        assertEquals(4, stats.trackedSnacks());
        assertEquals(3, stats.activeSnacks());
        assertEquals(1, stats.standbySnacks());
        assertEquals(3, stats.clusters());
        assertEquals(2, stats.dimensions());
        assertEquals(2, stats.largestCluster());
        assertTrue(stats.recomputes() > 0);
    }

    @Test
    void firstStandbySnackTakesOverWhenAnActiveSnackDisappears() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(16, 64, 0);
        BlockPos third = new BlockPos(32, 64, 0);

        manager.register(Level.OVERWORLD, first, null);
        manager.register(Level.OVERWORLD, second, null);
        manager.register(Level.OVERWORLD, third, null);
        manager.unregister(Level.OVERWORLD, first);

        assertTrue(manager.state(Level.OVERWORLD, third).orElseThrow().active());
        assertEquals(2, manager.state(Level.OVERWORLD, third).orElseThrow().activeInCluster());
    }

    @Test
    void newestFirstUsesNewestSnacksAsActiveCapacity() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.activationOrder = SnackControlConfig.ActivationOrder.NEWEST_FIRST;
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(16, 64, 0);
        BlockPos third = new BlockPos(32, 64, 0);

        manager.register(Level.OVERWORLD, first, null, 1);
        manager.register(Level.OVERWORLD, second, null, 2);
        manager.register(Level.OVERWORLD, third, null, 3);

        assertFalse(manager.state(Level.OVERWORLD, first).orElseThrow().active());
        assertTrue(manager.state(Level.OVERWORLD, second).orElseThrow().active());
        assertTrue(manager.state(Level.OVERWORLD, third).orElseThrow().active());
    }

    @Test
    void dimensionsAreSeparateEvenAtSamePosition() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.maxActiveSnacksPerCluster = 1;
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos position = new BlockPos(0, 64, 0);

        manager.register(Level.OVERWORLD, position, null, 1);
        manager.register(Level.NETHER, position, null, 2);

        assertTrue(manager.state(Level.OVERWORLD, position).orElseThrow().active());
        assertTrue(manager.state(Level.NETHER, position).orElseThrow().active());
    }

    @Test
    void registrationIsIdempotentAndCanRestoreSequence() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos position = new BlockPos(0, 64, 0);

        manager.register(Level.OVERWORLD, position, null, 42);
        SnackRuntimeState firstState = manager.state(Level.OVERWORLD, position).orElseThrow();
        long recomputes = manager.diagnosticRecomputeCount();
        SnackRuntimeState repeatedState = manager.register(Level.OVERWORLD, position, null, 42);

        assertEquals(1, manager.snapshot().size());
        assertSame(firstState, repeatedState);
        assertEquals(recomputes, manager.diagnosticRecomputeCount());
        assertEquals(42, manager.state(Level.OVERWORLD, position).orElseThrow().placementSequence());
    }

    @Test
    void duplicatedPersistedSequenceIsRepairedForDifferentSnacks() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos original = new BlockPos(0, 64, 0);
        BlockPos clone = new BlockPos(16, 64, 0);

        SnackActivityManager.RegistrationResult first = manager.registerResolved(Level.OVERWORLD, original, null, 42);
        SnackActivityManager.RegistrationResult second = manager.registerResolved(Level.OVERWORLD, clone, null, 42);

        assertFalse(first.sequenceChanged());
        assertTrue(second.sequenceChanged());
        assertEquals(43, second.placementSequence());
        assertEquals(42, manager.state(Level.OVERWORLD, original).orElseThrow().placementSequence());
        assertEquals(43, manager.state(Level.OVERWORLD, clone).orElseThrow().placementSequence());
    }

    @Test
    void duplicatedSequenceRepairKeepsOldestFirstOnOriginalOwner() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.maxActiveSnacksPerCluster = 1;
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos higher = new BlockPos(16, 64, 0);
        BlockPos lower = new BlockPos(0, 64, 0);

        manager.register(Level.OVERWORLD, higher, null, 7);
        manager.register(Level.OVERWORLD, lower, null, 7);

        assertTrue(manager.state(Level.OVERWORLD, higher).orElseThrow().active());
        assertFalse(manager.state(Level.OVERWORLD, lower).orElseThrow().active());
    }

    @Test
    void independentClustersDoNotShareIdsWhenMinimumSequencesMatch() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(100, 64, 0);

        manager.register(Level.OVERWORLD, first, null, 10);
        manager.register(Level.NETHER, second, null, 10);

        long firstCluster = manager.state(Level.OVERWORLD, first).orElseThrow().clusterId();
        long secondCluster = manager.state(Level.NETHER, second).orElseThrow().clusterId();

        assertTrue(firstCluster != secondCluster);
    }

    @Test
    void changedRegistrationDataRecomputesClusters() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);
        BlockPos position = new BlockPos(0, 64, 0);

        manager.register(Level.OVERWORLD, position, null, 42);
        long recomputes = manager.diagnosticRecomputeCount();
        manager.register(Level.OVERWORLD, position, null, 43);

        assertEquals(recomputes + 1, manager.diagnosticRecomputeCount());
        assertEquals(43, manager.state(Level.OVERWORLD, position).orElseThrow().placementSequence());
    }

    @Test
    void clusterSplitsAfterBridgeSnackIsRemoved() {
        SnackControlConfig config = new SnackControlConfig();
        config.activity.maxActiveSnacksPerCluster = 1;
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos left = new BlockPos(0, 64, 0);
        BlockPos bridge = new BlockPos(16, 64, 0);
        BlockPos right = new BlockPos(32, 64, 0);

        manager.register(Level.OVERWORLD, left, null, 1);
        manager.register(Level.OVERWORLD, bridge, null, 2);
        manager.register(Level.OVERWORLD, right, null, 3);
        assertFalse(manager.state(Level.OVERWORLD, right).orElseThrow().active());

        manager.unregister(Level.OVERWORLD, bridge);

        assertTrue(manager.state(Level.OVERWORLD, left).orElseThrow().active());
        assertTrue(manager.state(Level.OVERWORLD, right).orElseThrow().active());
    }

    @Test
    void distantSnackUsesItsOwnClusterCapacity() {
        SnackControlConfig config = new SnackControlConfig();
        SnackActivityManager manager = new SnackActivityManager(() -> config);

        BlockPos firstCluster = new BlockPos(0, 64, 0);
        BlockPos secondCluster = new BlockPos(100, 64, 0);

        manager.register(Level.OVERWORLD, firstCluster, null);
        manager.register(Level.OVERWORLD, secondCluster, null);

        assertTrue(manager.state(Level.OVERWORLD, firstCluster).orElseThrow().active());
        assertTrue(manager.state(Level.OVERWORLD, secondCluster).orElseThrow().active());
        assertEquals(1, manager.state(Level.OVERWORLD, secondCluster).orElseThrow().clusterSize());
    }
}
