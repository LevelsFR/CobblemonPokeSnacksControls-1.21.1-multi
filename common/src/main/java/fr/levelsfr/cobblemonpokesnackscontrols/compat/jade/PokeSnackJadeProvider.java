package fr.levelsfr.cobblemonpokesnackscontrols.compat.jade;

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import com.cobblemon.mod.common.api.fishing.SpawnBait;
import com.cobblemon.mod.common.api.fishing.SpawnBaitUtils;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonSnackHooks;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackSpawnHistory;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

enum PokeSnackJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final Set<String> COBBLEMON_BAIT_TOOLTIP_TYPES = Set.of(
            "nature",
            "iv",
            "ev",
            "bite_time",
            "gender_chance",
            "level_raise",
            "typing",
            "egg_group",
            "shiny_reroll",
            "drops_reroll",
            "ha_chance",
            "pokemon_chance",
            "friendship",
            "rarity_bucket",
            "alpha_chance",
            "mark_chance",
            "size"
    );
    private static final Map<String, String> COBBLEMON_STAT_KEYS = Map.of(
            "hp", "hp",
            "atk", "attack",
            "def", "defence",
            "spa", "special_attack",
            "spd", "special_defence",
            "spe", "speed"
    );
    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var config = CobblemonPokeSnacksControls.configManager().get();
        if (!config.general.enabled || !config.jade.enabled) {
            return;
        }

        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof PokeSnackBlockEntity pokeSnackBlockEntity) || !pokeSnackBlockEntity.isLure()) {
            return;
        }
        if (accessor.getLevel() instanceof ServerLevel serverLevel) {
            CobblemonSnackHooks.ensureSnackRegistered(serverLevel, pokeSnackBlockEntity);
        }
        writeBaitEffects(data, pokeSnackBlockEntity);
        if (config.jade.showLastSpawn) {
            CobblemonPokeSnacksControls.snackSpawnHistory()
                    .lastSpawn(accessor.getLevel().dimension(), accessor.getPosition())
                    .ifPresent(recentSpawn -> writeLastSpawn(data, recentSpawn, accessor.getLevel().registryAccess()));
        }

        CobblemonPokeSnacksControls.snackActivityManager()
                .state(accessor.getLevel().dimension(), accessor.getPosition())
                .ifPresent(state -> {
                    if (config.jade.showStatus) {
                        data.putString(PokeSnackJadeData.STATUS, state.status().name());
                        data.putBoolean(PokeSnackJadeData.PAUSES_STANDBY, config.activity.pauseStandbySnacks);
                    }
                    if (config.jade.showCapacity) {
                        data.putInt(PokeSnackJadeData.ACTIVE_COUNT, state.activeInCluster());
                        data.putInt(PokeSnackJadeData.ACTIVE_LIMIT, config.activity.maxActiveSnacksPerCluster);
                    }

                    boolean operator = accessor.getPlayer() instanceof ServerPlayer player && player.hasPermissions(2);
                    boolean canSeeQueue = PokeSnackJadeDataPolicy.canSendQueuePosition(
                            config,
                            state.status(),
                            state.queuePosition(),
                            operator
                    );
                    if (canSeeQueue) {
                        data.putInt(PokeSnackJadeData.QUEUE_POSITION, state.queuePosition());
                    }
                });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig pluginConfig) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(PokeSnackJadeData.STATUS)
                && !data.contains(PokeSnackJadeData.ACTIVE_COUNT)
                && !data.contains(PokeSnackJadeData.QUEUE_POSITION)
                && !data.contains(PokeSnackJadeData.LAST_SPAWN_NAME)
                && !data.contains(PokeSnackJadeData.BAIT_EFFECTS, Tag.TAG_LIST)) {
            return;
        }

        appendBaitEffects(tooltip, data);

        if (data.contains(PokeSnackJadeData.STATUS)) {
            SnackStatus status = SnackStatus.valueOf(data.getString(PokeSnackJadeData.STATUS));
            boolean pausesStandby = data.getBoolean(PokeSnackJadeData.PAUSES_STANDBY);
            Component line;
            if (status == SnackStatus.ACTIVE) {
                line = Component.translatable("jade.cobblemon_pokesnacks_controls.status.active").withStyle(ChatFormatting.GREEN);
            } else if (pausesStandby) {
                line = Component.translatable("jade.cobblemon_pokesnacks_controls.status.standby").withStyle(ChatFormatting.YELLOW);
            } else {
                line = Component.translatable("jade.cobblemon_pokesnacks_controls.status.over_capacity").withStyle(ChatFormatting.YELLOW);
            }
            tooltip.add(line);
        }

        if (data.contains(PokeSnackJadeData.ACTIVE_COUNT)
                && data.contains(PokeSnackJadeData.ACTIVE_LIMIT)) {
            tooltip.add(Component.translatable(
                    "jade.cobblemon_pokesnacks_controls.capacity",
                    data.getInt(PokeSnackJadeData.ACTIVE_COUNT),
                    data.getInt(PokeSnackJadeData.ACTIVE_LIMIT)
            ));
        }

        if (data.contains(PokeSnackJadeData.QUEUE_POSITION)) {
            tooltip.add(Component.translatable(
                    "jade.cobblemon_pokesnacks_controls.queue",
                    data.getInt(PokeSnackJadeData.QUEUE_POSITION)
            ).withStyle(ChatFormatting.GRAY));
        }

        appendLastSpawn(tooltip, accessor, data);
    }

    @Override
    public ResourceLocation getUid() {
        return PokeSnackJadeData.UID;
    }

    private static void writeBaitEffects(CompoundTag data, PokeSnackBlockEntity pokeSnackBlockEntity) {
        List<SpawnBait.Effect> effects = SpawnBaitUtils.mergeEffects(pokeSnackBlockEntity.getBaitEffects());
        if (effects.isEmpty()) {
            return;
        }

        ListTag effectTags = new ListTag();
        for (SpawnBait.Effect effect : effects) {
            CompoundTag effectTag = new CompoundTag();
            effectTag.putString(PokeSnackJadeData.BAIT_EFFECT_TYPE, effect.getType().toString());
            ResourceLocation subcategory = effect.getSubcategory();
            if (subcategory != null) {
                effectTag.putString(PokeSnackJadeData.BAIT_EFFECT_SUBCATEGORY, subcategory.toString());
            }
            effectTag.putDouble(PokeSnackJadeData.BAIT_EFFECT_CHANCE, effect.getChance());
            effectTag.putDouble(PokeSnackJadeData.BAIT_EFFECT_VALUE, effect.getValue());
            effectTags.add(effectTag);
        }
        data.put(PokeSnackJadeData.BAIT_EFFECTS, effectTags);
    }

    private static void writeLastSpawn(
            CompoundTag data,
            SnackSpawnHistory.RecentSpawn recentSpawn,
            HolderLookup.Provider registries
    ) {
        var pokemon = recentSpawn.pokemon();
        data.putString(PokeSnackJadeData.LAST_SPAWN_NAME,
                Component.Serializer.toJson(pokemon.displayName(), registries));
        data.putString(PokeSnackJadeData.LAST_SPAWN_SPECIES, pokemon.speciesId().toString());
        data.putString(PokeSnackJadeData.LAST_SPAWN_FORM, pokemon.formId());
        data.putBoolean(PokeSnackJadeData.LAST_SPAWN_SHINY, pokemon.shiny());
        data.putBoolean(PokeSnackJadeData.LAST_SPAWN_ALPHA, pokemon.alpha());

        ListTag marks = new ListTag();
        for (String markId : recentSpawn.markIds()) {
            marks.add(StringTag.valueOf(markId));
        }
        data.put(PokeSnackJadeData.LAST_SPAWN_MARKS, marks);
    }

    private static void appendLastSpawn(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        if (!data.contains(PokeSnackJadeData.LAST_SPAWN_NAME)) {
            return;
        }

        Component displayName;
        try {
            displayName = Component.Serializer.fromJson(
                    data.getString(PokeSnackJadeData.LAST_SPAWN_NAME),
                    accessor.getLevel().registryAccess()
            );
        } catch (RuntimeException exception) {
            displayName = null;
        }
        if (displayName == null) {
            displayName = Component.literal(data.getString(PokeSnackJadeData.LAST_SPAWN_SPECIES));
        }

        tooltip.add(Component.translatable("jade.cobblemon_pokesnacks_controls.last_spawn", displayName)
                .withStyle(ChatFormatting.GRAY));
        String formId = data.getString(PokeSnackJadeData.LAST_SPAWN_FORM);
        if (!formId.isBlank()) {
            tooltip.add(Component.translatable("jade.cobblemon_pokesnacks_controls.last_spawn.form", formId)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (data.getBoolean(PokeSnackJadeData.LAST_SPAWN_SHINY)) {
            tooltip.add(Component.translatable("jade.cobblemon_pokesnacks_controls.last_spawn.shiny")
                    .withStyle(ChatFormatting.GOLD));
        }
        if (data.getBoolean(PokeSnackJadeData.LAST_SPAWN_ALPHA)) {
            tooltip.add(Component.translatable("jade.cobblemon_pokesnacks_controls.last_spawn.alpha")
                    .withStyle(ChatFormatting.RED));
        }

        ListTag marks = data.getList(PokeSnackJadeData.LAST_SPAWN_MARKS, Tag.TAG_STRING);
        if (!marks.isEmpty()) {
            List<String> markNames = new ArrayList<>(marks.size());
            for (int index = 0; index < marks.size(); index++) {
                markNames.add(formatMarkName(marks.getString(index)));
            }
            tooltip.add(Component.translatable(
                    "jade.cobblemon_pokesnacks_controls.last_spawn.marks",
                    String.join(", ", markNames)
            ).withStyle(ChatFormatting.AQUA));
        }
    }

    private static String formatMarkName(String markId) {
        int namespaceSeparator = markId.indexOf(':');
        String path = namespaceSeparator >= 0 ? markId.substring(namespaceSeparator + 1) : markId;
        StringBuilder name = new StringBuilder(path.length());
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return name.toString();
    }

    private static void appendBaitEffects(ITooltip tooltip, CompoundTag data) {
        if (!data.contains(PokeSnackJadeData.BAIT_EFFECTS, Tag.TAG_LIST)) {
            return;
        }

        ListTag effects = data.getList(PokeSnackJadeData.BAIT_EFFECTS, Tag.TAG_COMPOUND);
        if (effects.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("cobblemon.fishing_bait_effect_header").withStyle(ChatFormatting.GOLD));
        for (int index = 0; index < effects.size(); index++) {
            tooltip.add(formatBaitEffect(effects.getCompound(index)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static MutableComponent formatBaitEffect(CompoundTag effectTag) {
        ResourceLocation type = ResourceLocation.tryParse(effectTag.getString(PokeSnackJadeData.BAIT_EFFECT_TYPE));
        ResourceLocation subcategory = effectTag.contains(PokeSnackJadeData.BAIT_EFFECT_SUBCATEGORY)
                ? ResourceLocation.tryParse(effectTag.getString(PokeSnackJadeData.BAIT_EFFECT_SUBCATEGORY))
                : null;
        double chance = effectTag.getDouble(PokeSnackJadeData.BAIT_EFFECT_CHANCE);
        double value = effectTag.getDouble(PokeSnackJadeData.BAIT_EFFECT_VALUE);
        String typePath = type == null ? "inert" : type.getPath();

        if (COBBLEMON_BAIT_TOOLTIP_TYPES.contains(typePath)) {
            if ("nature".equals(typePath)) {
                return Component.translatable(
                        "jade.cobblemon_pokesnacks_controls.bait.effect.nature_stat",
                        formatPercent(chance),
                        displaySubcategory(typePath, subcategory),
                        formatEffectValue(typePath, value)
                );
            }
            return Component.translatable(
                    "cobblemon.fishing_bait_effects." + typePath + ".tooltip",
                    formatPercent(chance),
                    displaySubcategory(typePath, subcategory),
                    formatEffectValue(typePath, value)
            );
        }

        Component displayedType = type == null
                ? Component.translatable("cobblemon.fishing_bait_effects.inert.tooltip")
                : Component.literal(displayResource(type));
        return Component.translatable(
                "jade.cobblemon_pokesnacks_controls.bait.effect.generic",
                formatPercent(chance),
                displayedType,
                formatNumber(value)
        );
    }

    private static Component displaySubcategory(String effectTypePath, ResourceLocation subcategory) {
        if (subcategory == null) {
            return Component.literal("-");
        }

        return switch (effectTypePath) {
            case "typing" -> Component.translatable("cobblemon.type." + normalizedSubcategoryPath(subcategory));
            case "nature" -> Component.translatable("cobblemon.stat." + statLangKey(subcategory) + ".name");
            case "gender_chance" -> Component.translatable("cobblemon.gender." + subcategory.getPath());
            case "iv", "ev" -> Component.translatable("cobblemon.stat." + statLangKey(subcategory) + ".name");
            case "egg_group" -> Component.translatable("cobblemon.egg_group." + normalizedSubcategoryPath(subcategory));
            default -> Component.literal(displayResource(subcategory));
        };
    }

    private static String statLangKey(ResourceLocation subcategory) {
        String path = normalizedSubcategoryPath(subcategory);
        return COBBLEMON_STAT_KEYS.getOrDefault(path, path);
    }

    private static String normalizedSubcategoryPath(ResourceLocation subcategory) {
        String path = subcategory.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String formatEffectValue(String effectTypePath, double value) {
        return "bite_time".equals(effectTypePath) ? formatPercent(value) : formatNumber(value);
    }

    private static String formatPercent(double value) {
        return formatNumber(value * 100.0);
    }

    private static String formatNumber(double value) {
        synchronized (DECIMAL_FORMAT) {
            return DECIMAL_FORMAT.format(value);
        }
    }

    private static String displayResource(ResourceLocation id) {
        String namespace = id.getNamespace();
        String path = id.getPath().replace('_', ' ');
        if ("cobblemon".equals(namespace) || "minecraft".equals(namespace)) {
            return path;
        }
        return namespace + ":" + path;
    }
}
