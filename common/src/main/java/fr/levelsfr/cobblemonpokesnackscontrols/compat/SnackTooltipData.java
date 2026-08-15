package fr.levelsfr.cobblemonpokesnackscontrols.compat;

import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackRuntimeState;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackStatus;

/**
 * Loader-neutral payload reserved for the optional Jade integration.
 * Networking and Jade provider registration are part of the next implementation phase.
 */
public record SnackTooltipData(
        SnackStatus status,
        int activeInCluster,
        int maximumActive,
        int queuePosition
) {
    public static SnackTooltipData from(SnackRuntimeState state, int maximumActive) {
        return new SnackTooltipData(state.status(), state.activeInCluster(), maximumActive, state.queuePosition());
    }
}
