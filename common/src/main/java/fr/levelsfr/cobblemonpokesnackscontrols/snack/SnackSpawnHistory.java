package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent;
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.ourstory.oscobblemon.entity.PokemonEntities;
import com.ourstory.oscobblemon.event.EventSubscriptionGroup;
import com.ourstory.oscobblemon.pokemon.PokemonIdentity;
import com.ourstory.oscobblemon.pokemon.PokemonSnapshot;
import fr.levelsfr.cobblemonpokesnackscontrols.CobblemonPokeSnacksControls;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Tracks the latest successful Pokémon spawn attributed to each native Poké Snack. */
public final class SnackSpawnHistory {
    private final Map<SnackKey, RecentSpawn> recentSpawns = new HashMap<>();
    private final EventSubscriptionGroup subscriptions = new EventSubscriptionGroup();

    public synchronized void startTracking() {
        if (subscriptions.isEmpty()) {
            subscriptions.track(CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.subscribe(this::onSpawn));
        }
        recentSpawns.clear();
    }

    public void stopTracking() {
        subscriptions.unsubscribeAll();
        clear();
    }

    public synchronized void clear() {
        recentSpawns.clear();
    }

    public synchronized void remove(ResourceKey<Level> dimension, BlockPos position) {
        recentSpawns.remove(new SnackKey(dimension, position));
    }

    public synchronized Optional<RecentSpawn> lastSpawn(ResourceKey<Level> dimension, BlockPos position) {
        return Optional.ofNullable(recentSpawns.get(new SnackKey(dimension, position)));
    }

    private void onSpawn(PokeSnackSpawnPokemonEvent.Post event) {
        PokeSnackBlockEntity snack = event.getPokeSnackBlockEntity();
        if (!(snack.getLevel() instanceof ServerLevel level)) {
            return;
        }

        var config = CobblemonPokeSnacksControls.configManager().get();
        if (!config.general.enabled) {
            return;
        }

        Pokemon pokemon = PokemonEntities.pokemon(event.getPokemonEntity());
        PokemonSnapshot snapshot = PokemonSnapshot.from(pokemon);
        RecentSpawn recentSpawn = new RecentSpawn(
                snapshot,
                PokemonIdentity.markIds(pokemon).stream()
                        .map(Object::toString)
                        .filter(markId -> !snapshot.alpha() || !"cobblemon:mark_alpha".equals(markId))
                        .sorted()
                        .toList()
        );
        synchronized (this) {
            recentSpawns.put(new SnackKey(level.dimension(), snack.getBlockPos()), recentSpawn);
        }

        if (config.general.notifyRareSpawns && recentSpawn.isRare()) {
            notifyPlacer(level, snack, recentSpawn);
        }
    }

    private static void notifyPlacer(ServerLevel level, PokeSnackBlockEntity snack, RecentSpawn recentSpawn) {
        UUID placerId = snack.getPlacedBy();
        if (placerId == null) {
            return;
        }

        ServerPlayer placer = level.getServer().getPlayerList().getPlayer(placerId);
        if (placer == null) {
            return;
        }

        PokemonSnapshot pokemon = recentSpawn.pokemon();
        placer.sendSystemMessage(Component.translatable(
                "message.cobblemon_pokesnacks_controls.rare_spawn",
                pokemon.displayName(),
                snack.getBlockPos().getX(),
                snack.getBlockPos().getY(),
                snack.getBlockPos().getZ(),
                rareTraits(recentSpawn)
        ));
    }

    private static Component rareTraits(RecentSpawn recentSpawn) {
        MutableComponent traits = Component.empty();
        boolean needsSeparator = false;
        if (recentSpawn.pokemon().alpha()) {
            traits.append(Component.translatable("message.cobblemon_pokesnacks_controls.rare_spawn.alpha")
                    .withStyle(ChatFormatting.RED));
            needsSeparator = true;
        }
        if (recentSpawn.pokemon().shiny()) {
            if (needsSeparator) traits.append(Component.literal(", "));
            traits.append(Component.translatable("message.cobblemon_pokesnacks_controls.rare_spawn.shiny")
                    .withStyle(ChatFormatting.GOLD));
            needsSeparator = true;
        }
        if (!recentSpawn.markIds().isEmpty()) {
            if (needsSeparator) traits.append(Component.literal(", "));
            traits.append(Component.translatable("message.cobblemon_pokesnacks_controls.rare_spawn.marked")
                    .withStyle(ChatFormatting.AQUA));
        }
        return traits;
    }

    public record RecentSpawn(PokemonSnapshot pokemon, List<String> markIds) {
        public RecentSpawn {
            Objects.requireNonNull(pokemon, "pokemon");
            markIds = List.copyOf(Objects.requireNonNull(markIds, "markIds"));
        }

        public boolean isRare() {
            return pokemon.alpha() || pokemon.shiny() || !markIds.isEmpty();
        }
    }
}
