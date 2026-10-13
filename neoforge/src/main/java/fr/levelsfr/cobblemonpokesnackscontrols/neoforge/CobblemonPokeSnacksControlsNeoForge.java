package fr.levelsfr.cobblemonpokesnackscontrols.neoforge;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import fr.levelsfr.cobblemonpokesnackscontrols.command.CommandRegistry;
import fr.levelsfr.cobblemonpokesnackscontrols.lifecycle.CommonServerLifecycle;
import fr.levelsfr.cobblemonpokesnackscontrols.neoforge.client.CobblemonPokeSnacksControlsNeoForgeClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(CobblemonPokeSnacksControls.MOD_ID)
public final class CobblemonPokeSnacksControlsNeoForge {
    public CobblemonPokeSnacksControlsNeoForge(ModContainer container) {
        CobblemonPokeSnacksControls.initialize(FMLPaths.CONFIGDIR.get());
        if ("CLIENT".equals(FMLEnvironment.dist.name())) {
            CobblemonPokeSnacksControlsNeoForgeClient.registerConfigScreen(container);
        }
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        CommandRegistry.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CommonServerLifecycle.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CommonServerLifecycle.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event) {
        CobblemonSnackHooks.onServerTickEnd(event.getServer());
    }
}
