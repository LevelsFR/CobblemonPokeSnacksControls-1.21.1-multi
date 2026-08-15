package fr.levelsfr.cobblemonpokesnackscontrols.neoforge.client;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import net.neoforged.fml.ModContainer;

public final class CobblemonPokeSnacksControlsNeoForgeClient {
    private CobblemonPokeSnacksControlsNeoForgeClient() {
    }

    public static void registerConfigScreen(ModContainer container) {
        CobblemonPokeSnacksControls.LOGGER.info("Client config screen is disabled; edit the server JSON config and use /pokesnackscontrols reload.");
    }
}
