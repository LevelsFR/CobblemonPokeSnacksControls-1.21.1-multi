package fr.levelsfr.cobblemonpokesnackscontrols.neoforge.client;

import fr.levelsfr.cobblemonpokesnackscontrols.client.PokeSnacksConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class CobblemonPokeSnacksControlsNeoForgeClient {
    private CobblemonPokeSnacksControlsNeoForgeClient() {
    }

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> PokeSnacksConfigScreen.create(parent)
        );
    }
}
