package fr.levelsfr.cobblemonpokesnackscontrols.compat.jade;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackStatus;

public final class PokeSnackJadeDataPolicy {
    private PokeSnackJadeDataPolicy() {
    }

    static boolean canSendQueuePosition(
            SnackControlConfig config,
            SnackStatus status,
            int queuePosition,
            boolean operator
    ) {
        return config.general.enabled
                && config.jade.enabled
                && config.jade.showQueuePositionToOperators
                && status == SnackStatus.STANDBY
                && queuePosition > 0
                && operator;
    }
}
