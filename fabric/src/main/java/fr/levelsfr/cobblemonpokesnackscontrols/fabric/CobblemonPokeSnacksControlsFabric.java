package fr.levelsfr.cobblemonpokesnackscontrols.fabric;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.command.CommandRegistry;
import fr.levelsfr.cobblemonpokesnackscontrols.lifecycle.CommonServerLifecycle;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CobblemonPokeSnacksControlsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CobblemonPokeSnacksControls.initialize(FabricLoader.getInstance().getConfigDir());
        CommandRegistrationCallback.EVENT.register(CommandRegistry::register);
        ServerLifecycleEvents.SERVER_STARTING.register(CommonServerLifecycle::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(CommonServerLifecycle::onServerStopping);
    }
}
