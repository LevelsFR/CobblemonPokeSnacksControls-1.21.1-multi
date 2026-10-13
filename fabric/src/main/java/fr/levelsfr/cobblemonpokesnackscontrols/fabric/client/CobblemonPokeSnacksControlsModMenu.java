package fr.levelsfr.cobblemonpokesnackscontrols.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fr.levelsfr.cobblemonpokesnackscontrols.client.PokeSnacksConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

public final class CobblemonPokeSnacksControlsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> FabricLoader.getInstance().isModLoaded("cloth-config")
                ? PokeSnacksConfigScreen.create(parent)
                : parent;
    }
}
