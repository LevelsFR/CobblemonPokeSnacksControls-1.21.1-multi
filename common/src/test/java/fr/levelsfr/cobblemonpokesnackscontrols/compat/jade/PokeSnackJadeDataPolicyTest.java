package fr.levelsfr.cobblemonpokesnackscontrols.compat.jade;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PokeSnackJadeDataPolicyTest {
    @Test
    void normalPlayersNeverReceiveQueuePosition() {
        SnackControlConfig config = new SnackControlConfig();

        assertFalse(PokeSnackJadeDataPolicy.canSendQueuePosition(config, SnackStatus.STANDBY, 1, false));
    }

    @Test
    void operatorsReceiveQueuePositionOnlyForStandbySnacks() {
        SnackControlConfig config = new SnackControlConfig();

        assertTrue(PokeSnackJadeDataPolicy.canSendQueuePosition(config, SnackStatus.STANDBY, 1, true));
        assertFalse(PokeSnackJadeDataPolicy.canSendQueuePosition(config, SnackStatus.ACTIVE, 0, true));
    }

    @Test
    void serverOptionsControlQueuePositionData() {
        SnackControlConfig config = new SnackControlConfig();
        config.jade.showQueuePositionToOperators = false;

        assertFalse(PokeSnackJadeDataPolicy.canSendQueuePosition(config, SnackStatus.STANDBY, 1, true));

        config.jade.showQueuePositionToOperators = true;
        config.jade.enabled = false;
        assertFalse(PokeSnackJadeDataPolicy.canSendQueuePosition(config, SnackStatus.STANDBY, 1, true));
    }
}
