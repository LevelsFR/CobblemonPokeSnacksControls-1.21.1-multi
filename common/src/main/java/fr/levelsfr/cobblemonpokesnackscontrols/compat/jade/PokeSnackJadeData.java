package fr.levelsfr.cobblemonpokesnackscontrols.compat.jade;

import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import net.minecraft.resources.ResourceLocation;

final class PokeSnackJadeData {
    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            CobblemonPokeSnacksControls.MOD_ID,
            "poke_snack_status"
    );

    static final String STATUS = "CPSC_Status";
    static final String ACTIVE_COUNT = "CPSC_ActiveCount";
    static final String ACTIVE_LIMIT = "CPSC_ActiveLimit";
    static final String QUEUE_POSITION = "CPSC_QueuePosition";
    static final String PAUSES_STANDBY = "CPSC_PausesStandby";
    static final String BAIT_EFFECTS = "CPSC_BaitEffects";
    static final String BAIT_EFFECT_TYPE = "Type";
    static final String BAIT_EFFECT_SUBCATEGORY = "Subcategory";
    static final String BAIT_EFFECT_CHANCE = "Chance";
    static final String BAIT_EFFECT_VALUE = "Value";

    private PokeSnackJadeData() {
    }
}
