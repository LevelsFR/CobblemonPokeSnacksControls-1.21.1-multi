package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import java.util.UUID;

public record SnackRuntimeState(
        SnackKey key,
        UUID placedBy,
        long placementSequence,
        SnackStatus status,
        int clusterSize,
        int activeInCluster,
        int queuePosition,
        long clusterId
) {
    public boolean active() {
        return status == SnackStatus.ACTIVE;
    }
}
