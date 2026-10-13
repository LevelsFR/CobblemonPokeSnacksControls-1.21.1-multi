package fr.levelsfr.cobblemonpokesnackscontrols.command;

import com.cobblemon.mod.common.CobblemonItemComponents;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.fishing.SpawnBaitEffects;
import com.cobblemon.mod.common.item.components.BaitEffectsComponent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.NativeSnackArea;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackActivityManager;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackRuntimeState;
import fr.levelsfr.cobblemonpokesnackscontrols.snack.SnackStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;

public final class CommandRegistry {
    private CommandRegistry() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(commandRoot("pokesnackscontrols"));
        dispatcher.register(commandRoot("psc"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandRoot(String name) {
        return literal(name)
                .executes(command -> showQueue(command.getSource()))
                .then(literal("queue").executes(command -> showQueue(command.getSource())))
                .then(literal("status").requires(source -> source.hasPermission(2)).executes(command -> showStatus(command.getSource())))
                .then(literal("stats").requires(source -> source.hasPermission(2)).executes(command -> showStats(command.getSource())))
                .then(literal("debug").requires(source -> source.hasPermission(2))
                        .then(literal("nearest").executes(command -> debugNearest(command.getSource()))))
                .then(literal("reload").requires(source -> source.hasPermission(2)).executes(command -> reload(command.getSource())))
                .then(literal("snack")
                        .then(literal("give").requires(source -> source.hasPermission(2))
                                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                                        .<CommandSourceStack, String>argument("ingredients", StringArgumentType.greedyString())
                                        .suggests(CommandRegistry::suggestSnackIngredients)
                                        .executes(CommandRegistry::giveSnack))));
    }

    private static CompletableFuture<Suggestions> suggestSnackIngredients(
            CommandContext<CommandSourceStack> command,
            SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        int ingredientStart = lastSpace + 1;
        String ingredientPrefix = remaining.substring(ingredientStart);
        SuggestionsBuilder ingredientBuilder = builder.createOffset(builder.getStart() + ingredientStart);

        List<ResourceLocation> ingredients = new ArrayList<>();
        for (ResourceLocation itemId : BuiltInRegistries.ITEM.keySet()) {
            if (!itemId.toString().startsWith(ingredientPrefix)) {
                continue;
            }

            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item != null && item != Items.AIR
                    && !SpawnBaitEffects.getBaitIdentifiersFromItem(new ItemStack(item).getItemHolder()).isEmpty()) {
                ingredients.add(itemId);
            }
        }
        return SharedSuggestionProvider.suggestResource(ingredients, ingredientBuilder);
    }

    private static int giveSnack(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String[] ingredientIds = StringArgumentType.getString(command, "ingredients").trim().split("\\s+");
        if (ingredientIds.length > 8) {
            source.sendFailure(Component.translatable("command.cobblemon_pokesnacks_controls.snack.too_many_ingredients"));
            return 0;
        }

        List<ResourceLocation> baitEffectIds = new ArrayList<>();
        for (String ingredientId : ingredientIds) {
            ResourceLocation itemId = ResourceLocation.tryParse(ingredientId);
            Item item = itemId == null ? null : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null || item == Items.AIR) {
                source.sendFailure(Component.translatable(
                        "command.cobblemon_pokesnacks_controls.snack.invalid_ingredient",
                        ingredientId
                ));
                return 0;
            }

            List<ResourceLocation> ingredientEffects = SpawnBaitEffects.getBaitIdentifiersFromItem(
                    new ItemStack(item).getItemHolder()
            );
            if (ingredientEffects.isEmpty()) {
                source.sendFailure(Component.translatable(
                        "command.cobblemon_pokesnacks_controls.snack.no_bait_effects",
                        itemId
                ));
                return 0;
            }
            baitEffectIds.addAll(ingredientEffects);
        }

        ItemStack snack = new ItemStack(CobblemonItems.POKE_SNACK);
        snack.set(CobblemonItemComponents.BAIT_EFFECTS, new BaitEffectsComponent(baitEffectIds));
        if (!player.getInventory().add(snack)) {
            player.drop(snack, false);
        }

        int effectCount = baitEffectIds.size();
        source.sendSuccess(() -> Component.translatable(
                "command.cobblemon_pokesnacks_controls.snack.given",
                ingredientIds.length,
                effectCount
        ), false);
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        var config = CobblemonPokeSnacksControls.configManager().get();
        var summary = CobblemonPokeSnacksControls.snackActivityManager().summary();
        source.sendSuccess(() -> Component.translatable(
                "command.cobblemon_pokesnacks_controls.status",
                summary.trackedSnacks(),
                summary.activeSnacks(),
                summary.standbySnacks(),
                summary.clusters(),
                config.activity.maxActiveSnacksPerCluster,
                config.general.enabled ? "enabled" : "disabled"
        ), false);
        return summary.trackedSnacks();
    }

    private static int showStats(CommandSourceStack source) {
        var config = CobblemonPokeSnacksControls.configManager().get();
        var stats = CobblemonPokeSnacksControls.snackActivityManager().stats();
        source.sendSuccess(() -> Component.literal("> ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("command.cobblemon_pokesnacks_controls.stats.title")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.tracked", stats.trackedSnacks(), ChatFormatting.GOLD), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.active", stats.activeSnacks(), ChatFormatting.GREEN), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.standby", stats.standbySnacks(), ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.clusters", stats.clusters(), ChatFormatting.AQUA), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.dimensions", stats.dimensions(), ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.largest_cluster", stats.largestCluster(), ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.stats.recomputes", stats.recomputes(), ChatFormatting.GRAY), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.stats.config",
                Component.literal(config.general.enabled ? "enabled" : "disabled")
                        .withStyle(config.general.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
                        .append(Component.literal(", debugLogging=" + config.general.debugLogging).withStyle(ChatFormatting.GRAY))
        ), false);
        return stats.trackedSnacks();
    }

    private static int debugNearest(CommandSourceStack source) {
        BlockPos origin = BlockPos.containing(source.getPosition());
        var state = CobblemonPokeSnacksControls.snackActivityManager()
                .nearestState(source.getLevel().dimension(), origin);
        if (state.isEmpty()) {
            source.sendSuccess(() -> queueLine()
                    .append(Component.translatable("command.cobblemon_pokesnacks_controls.debug.nearest.empty")
                            .withStyle(ChatFormatting.YELLOW)), false);
            return 0;
        }

        SnackRuntimeState nearest = state.orElseThrow();
        BlockPos position = nearest.key().position();
        int distance = SnackActivityManager.blockDistance(origin, position);
        source.sendSuccess(() -> Component.literal("> ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("command.cobblemon_pokesnacks_controls.debug.nearest.title")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.debug.nearest.position",
                Component.literal(position.getX() + " " + position.getY() + " " + position.getZ())
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
        ), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.distance", distance, ChatFormatting.GOLD), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.dimension", nearest.key().dimension().location(), ChatFormatting.AQUA), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.status", nearest.status(), nearest.active() ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.cluster", nearest.clusterId(), ChatFormatting.GRAY), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.capacity", nearest.activeInCluster() + " / " + nearest.clusterSize(), ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.queue", nearest.queuePosition(), nearest.queuePosition() == 0 ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> statLine("command.cobblemon_pokesnacks_controls.debug.nearest.sequence", nearest.placementSequence(), ChatFormatting.GRAY), false);
        return 1;
    }

    private static int showQueue(CommandSourceStack source) {
        BlockPos origin = BlockPos.containing(source.getPosition());
        var queue = CobblemonPokeSnacksControls.snackActivityManager()
                .nearestQueue(source.getLevel().dimension(), origin);
        if (queue.isEmpty()) {
            source.sendSuccess(() -> queueLine()
                    .append(Component.translatable("command.cobblemon_pokesnacks_controls.queue.empty")
                            .withStyle(ChatFormatting.YELLOW)), false);
            return 0;
        }

        var view = queue.orElseThrow();
        source.sendSuccess(() -> Component.literal("> ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("command.cobblemon_pokesnacks_controls.queue.title")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.queue.total",
                view.clusterSize(),
                ChatFormatting.GOLD
        ), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.queue.active",
                view.activeSnacks(),
                ChatFormatting.GREEN
        ), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.queue.waiting",
                view.standbySnacks(),
                ChatFormatting.LIGHT_PURPLE
        ), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.queue.nearest",
                Component.literal(String.valueOf(view.nearestDistanceBlocks()))
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        .append(Component.literal(" "))
                        .append(Component.translatable("command.cobblemon_pokesnacks_controls.queue.blocks")
                                .withStyle(ChatFormatting.GRAY))
        ), false);
        source.sendSuccess(() -> statLine(
                "command.cobblemon_pokesnacks_controls.queue.radius",
                NativeSnackArea.NATIVE_POKE_SNACK_HORIZONTAL_RADIUS + "/" + NativeSnackArea.NATIVE_POKE_SNACK_VERTICAL_RADIUS,
                ChatFormatting.AQUA
        ), false);
        source.sendSuccess(() -> queueLine()
                .append(Component.translatable("command.cobblemon_pokesnacks_controls.queue.entries")
                        .withStyle(ChatFormatting.AQUA)), false);
        int displayIndex = 1;
        for (SnackRuntimeState state : view.states()) {
            Component line = snackLine(displayIndex, state);
            displayIndex++;
            source.sendSuccess(() -> line, false);
        }
        return view.clusterSize();
    }

    private static int reload(CommandSourceStack source) {
        var result = CobblemonPokeSnacksControls.configManager().load();
        if (result.success()) {
            CobblemonPokeSnacksControls.snackActivityManager().refreshAll();
            source.sendSuccess(() -> Component.translatable(
                    "command.cobblemon_pokesnacks_controls.reload",
                    result.name()
            ), true);
            return 1;
        }
        source.sendFailure(Component.translatable(
                "command.cobblemon_pokesnacks_controls.reload.failed",
                result.name()
        ));
        return 0;
    }

    private static MutableComponent queueLine() {
        return Component.literal("> ").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent label(String translationKey) {
        return Component.translatable(translationKey)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent value(Object value, ChatFormatting color) {
        return Component.literal(String.valueOf(value)).withStyle(color, ChatFormatting.BOLD);
    }

    private static MutableComponent statLine(String translationKey, Object rawValue, ChatFormatting valueColor) {
        return statLine(translationKey, value(rawValue, valueColor));
    }

    private static MutableComponent statLine(String translationKey, Component styledValue) {
        return queueLine()
                .append(label(translationKey))
                .append(styledValue);
    }

    private static MutableComponent snackLine(int displayIndex, SnackRuntimeState state) {
        MutableComponent line = Component.literal(">   #")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(String.valueOf(displayIndex)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "));
        if (state.status() == SnackStatus.ACTIVE) {
            return line.append(Component.translatable("command.cobblemon_pokesnacks_controls.queue.entry.active")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        if (!CobblemonPokeSnacksControls.configManager().get().activity.pauseStandbySnacks) {
            return line.append(Component.translatable(
                            "command.cobblemon_pokesnacks_controls.queue.entry.over_capacity",
                            state.queuePosition())
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
        return line.append(Component.translatable(
                        "command.cobblemon_pokesnacks_controls.queue.entry.standby",
                        state.queuePosition())
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }
}
