package fr.levelsfr.cobblemonpokesnackscontrols.effect;

import com.cobblemon.mod.common.api.fishing.SpawnBait;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class CobblemonBaitEffectAdapterTest {
    @Test
    void mapsEveryV1CobblemonEffectCategory() {
        SpawnBait.Effects effects = SpawnBait.Effects.INSTANCE;

        assertEquals(SnackEffectCategory.TYPE_ATTRACTION, CobblemonBaitEffectAdapter.category(effects.getTYPING()));
        assertEquals(SnackEffectCategory.EGG_GROUP_ATTRACTION, CobblemonBaitEffectAdapter.category(effects.getEGG_GROUP()));
        assertEquals(SnackEffectCategory.EV_YIELD_FILTER, CobblemonBaitEffectAdapter.category(effects.getEV()));
        assertEquals(SnackEffectCategory.RARITY_BUCKET, CobblemonBaitEffectAdapter.category(effects.getRARITY_BUCKET()));
        assertEquals(SnackEffectCategory.NATURE, CobblemonBaitEffectAdapter.category(effects.getNATURE()));
        assertEquals(SnackEffectCategory.IV, CobblemonBaitEffectAdapter.category(effects.getIV()));
        assertEquals(SnackEffectCategory.SHINY, CobblemonBaitEffectAdapter.category(effects.getSHINY_REROLL()));
        assertEquals(SnackEffectCategory.HIDDEN_ABILITY, CobblemonBaitEffectAdapter.category(effects.getHIDDEN_ABILITY_CHANCE()));
        assertEquals(SnackEffectCategory.GENDER, CobblemonBaitEffectAdapter.category(effects.getGENDER_CHANCE()));
        assertEquals(SnackEffectCategory.LEVEL, CobblemonBaitEffectAdapter.category(effects.getLEVEL_RAISE()));
        assertEquals(SnackEffectCategory.FRIENDSHIP, CobblemonBaitEffectAdapter.category(effects.getFRIENDSHIP()));
        assertEquals(SnackEffectCategory.DROPS, CobblemonBaitEffectAdapter.category(effects.getDROPS_REROLL()));
        assertEquals(SnackEffectCategory.BITE_TIME, CobblemonBaitEffectAdapter.category(effects.getBITE_TIME()));
    }

    @Test
    void unknownEffectsArePreservedInOrder() {
        SpawnBait.Effect unknown = new SpawnBait.Effect(
                ResourceLocation.fromNamespaceAndPath("datapack", "custom_effect"),
                (ResourceLocation) null,
                0.25D,
                2.0D
        );

        List<SpawnBait.Effect> adjusted = CobblemonBaitEffectAdapter.adjust(List.of(unknown));

        assertEquals(1, adjusted.size());
        assertSame(unknown, adjusted.getFirst());
        assertNull(CobblemonBaitEffectAdapter.category(unknown.getType()));
    }

    @Test
    void categoryChanceCapAppliesToStackedFinalTotal() {
        ResourceLocation type = SpawnBait.Effects.INSTANCE.getSHINY_REROLL();
        List<SpawnBait.Effect> adjusted = CobblemonBaitEffectAdapter.adjust(
                List.of(
                        new SpawnBait.Effect(type, (ResourceLocation) null, 0.2D, 1.0D),
                        new SpawnBait.Effect(type, (ResourceLocation) null, 0.2D, 1.0D),
                        new SpawnBait.Effect(type, (ResourceLocation) null, 0.2D, 1.0D)
                ),
                category -> new ResolvedEffectRule(true, 1.0D, 1.0D, null, 0.2D)
        );

        double totalChance = adjusted.stream().mapToDouble(SpawnBait.Effect::getChance).sum();

        assertEquals(0.2D, totalChance, 0.0000001D);
        assertEquals(3, adjusted.size());
    }
}
