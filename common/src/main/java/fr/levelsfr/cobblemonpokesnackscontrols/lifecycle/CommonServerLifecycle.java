package fr.levelsfr.cobblemonpokesnackscontrols.lifecycle;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import net.minecraft.server.MinecraftServer;

public final class CommonServerLifecycle {
    private CommonServerLifecycle() {
    }

    public static void onServerStarting(MinecraftServer server) {
        CobblemonPokeSnacksControls.snackActivityManager().clear();
        CobblemonPokeSnacksControls.LOGGER.info("Poke Snack runtime registry cleared before world loading.");
    }

    public static void onServerStopping(MinecraftServer server) {
        CobblemonPokeSnacksControls.snackActivityManager().clear();
    }
}
