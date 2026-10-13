package fr.levelsfr.cobblemonpokesnackscontrols.lifecycle;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import net.minecraft.server.MinecraftServer;

public final class CommonServerLifecycle {
    private CommonServerLifecycle() {
    }

    public static void onServerStarting(MinecraftServer server) {
        CobblemonSnackHooks.clearDeferredUpdates();
        CobblemonPokeSnacksControls.snackActivityManager().clear();
        CobblemonPokeSnacksControls.LOGGER.info("Poke Snack runtime registry cleared before world loading.");
    }

    public static void onServerStopping(MinecraftServer server) {
        CobblemonSnackHooks.flushDeferredUpdates(server);
        CobblemonSnackHooks.clearDeferredUpdates();
        CobblemonPokeSnacksControls.snackActivityManager().clear();
    }
}
