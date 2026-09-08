package fr.levelsfr.cobblemonpokesnackscontrols.snack;

import fr.levelsfr.cobblemonpokesnackscontrols.config.SnackControlConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Server-authoritative registry for active and standby Poké Snacks.
 *
 * <p>This class already implements deterministic cluster arbitration. The future Cobblemon hooks only need to
 * register/unregister block entities and ask {@link #isActive(ResourceKey, BlockPos)} before a native snack tick.
 * It does not replace Cobblemon's spawner or effect calculations.</p>
 */
public final class SnackActivityManager {
    private static final int CLUSTER_CELL_SIZE = NativeSnackArea.NATIVE_POKE_SNACK_HORIZONTAL_RADIUS * 2 + 1;

    private final Supplier<SnackControlConfig> configSupplier;
    private final Map<SnackKey, Entry> entries = new HashMap<>();
    private final List<SnackStateListener> listeners = new CopyOnWriteArrayList<>();
    private long recomputeCount;

    public SnackActivityManager(Supplier<SnackControlConfig> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    }

    public synchronized SnackRuntimeState register(ResourceKey<Level> dimension, BlockPos position, UUID placedBy) {
        Entry existing = entries.get(new SnackKey(dimension, position));
        if (existing != null && Objects.equals(existing.placedBy, placedBy)) {
            return existing.state;
        }
        return registerResolved(dimension, position, placedBy, allocateSequence()).state();
    }

    public synchronized SnackRuntimeState register(ResourceKey<Level> dimension, BlockPos position, UUID placedBy, long placementSequence) {
        return registerResolved(dimension, position, placedBy, placementSequence).state();
    }

    public synchronized RegistrationResult registerResolved(ResourceKey<Level> dimension, BlockPos position, UUID placedBy, long placementSequence) {
        SnackKey key = new SnackKey(dimension, position);
        long normalizedSequence = normalizeSequence(key, placementSequence);
        Entry existing = entries.get(key);
        if (existing == null) {
            entries.put(key, new Entry(key, placedBy, normalizedSequence));
        } else if (existing.placementSequence != normalizedSequence || !Objects.equals(existing.placedBy, placedBy)) {
            entries.put(key, new Entry(key, placedBy, normalizedSequence));
        } else {
            return new RegistrationResult(existing.state, existing.placementSequence, existing.placementSequence != placementSequence);
        }
        recomputeDimension(dimension);
        Entry registered = entries.get(key);
        return new RegistrationResult(registered.state, registered.placementSequence, registered.placementSequence != placementSequence);
    }

    public synchronized void unregister(ResourceKey<Level> dimension, BlockPos position) {
        SnackKey key = new SnackKey(dimension, position);
        if (entries.remove(key) != null) {
            recomputeDimension(dimension);
        }
    }

    public synchronized boolean isActive(ResourceKey<Level> dimension, BlockPos position) {
        Entry entry = entries.get(new SnackKey(dimension, position));
        return entry != null && entry.state.active();
    }

    public synchronized Optional<SnackRuntimeState> state(ResourceKey<Level> dimension, BlockPos position) {
        Entry entry = entries.get(new SnackKey(dimension, position));
        return entry == null ? Optional.empty() : Optional.of(entry.state);
    }

    public synchronized List<SnackRuntimeState> activeConsumptionCandidates(
            ResourceKey<Level> dimension,
            BlockPos requestedSnack,
            BlockPos consumerPosition
    ) {
        Entry requested = entries.get(new SnackKey(dimension, requestedSnack));
        if (requested == null) {
            return List.of();
        }

        Comparator<SnackRuntimeState> activationOrder = Comparator.comparingLong(SnackRuntimeState::placementSequence);
        if (configSupplier.get().activity.activationOrder == SnackControlConfig.ActivationOrder.NEWEST_FIRST) {
            activationOrder = activationOrder.reversed();
        }

        return entries.values().stream()
                .map(entry -> entry.state)
                .filter(SnackRuntimeState::active)
                .filter(state -> state.key().dimension().equals(dimension))
                .filter(state -> state.clusterId() == requested.state.clusterId())
                .sorted(Comparator
                        .comparingInt((SnackRuntimeState state) -> blockDistance(consumerPosition, state.key().position()))
                        .thenComparing(activationOrder)
                        .thenComparingInt(state -> state.key().position().getX())
                        .thenComparingInt(state -> state.key().position().getY())
                        .thenComparingInt(state -> state.key().position().getZ()))
                .toList();
    }

    public synchronized Collection<SnackRuntimeState> snapshot() {
        return entries.values().stream().map(entry -> entry.state).toList();
    }

    public synchronized Optional<QueueView> nearestQueue(ResourceKey<Level> dimension, BlockPos origin) {
        return snapshot().stream()
                .filter(state -> state.key().dimension().equals(dimension))
                .collect(HashMap<Long, List<SnackRuntimeState>>::new,
                        (clusters, state) -> clusters.computeIfAbsent(state.clusterId(), ignored -> new ArrayList<>()).add(state),
                        HashMap::putAll)
                .values()
                .stream()
                .map(states -> QueueView.from(origin, states))
                .min(Comparator.comparingInt(QueueView::nearestDistanceBlocks));
    }

    public synchronized Optional<SnackRuntimeState> nearestState(ResourceKey<Level> dimension, BlockPos origin) {
        return snapshot().stream()
                .filter(state -> state.key().dimension().equals(dimension))
                .min(Comparator
                        .comparingInt((SnackRuntimeState state) -> blockDistance(origin, state.key().position()))
                        .thenComparingLong(SnackRuntimeState::placementSequence));
    }

    public synchronized Summary summary() {
        Collection<SnackRuntimeState> states = snapshot();
        long active = states.stream().filter(SnackRuntimeState::active).count();
        long standby = states.size() - active;
        long clusters = states.stream()
                .filter(state -> state.activeInCluster() > 0)
                .map(state -> state.key().dimension().location() + ":" + state.clusterId())
                .distinct()
                .count();
        return new Summary(states.size(), (int) active, (int) standby, (int) clusters);
    }

    public synchronized Stats stats() {
        Collection<SnackRuntimeState> states = snapshot();
        Summary summary = summary();
        long dimensions = states.stream()
                .map(state -> state.key().dimension())
                .distinct()
                .count();
        int largestCluster = states.stream()
                .mapToInt(SnackRuntimeState::clusterSize)
                .max()
                .orElse(0);
        return new Stats(
                summary.trackedSnacks(),
                summary.activeSnacks(),
                summary.standbySnacks(),
                summary.clusters(),
                (int) dimensions,
                largestCluster,
                recomputeCount
        );
    }

    public synchronized void refreshAll() {
        Set<ResourceKey<Level>> dimensions = new HashSet<>();
        entries.keySet().forEach(key -> dimensions.add(key.dimension()));
        dimensions.forEach(this::recomputeDimension);
    }

    public synchronized void clear() {
        entries.clear();
    }

    synchronized long diagnosticRecomputeCount() {
        return recomputeCount;
    }

    public void addListener(SnackStateListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(SnackStateListener listener) {
        listeners.remove(listener);
    }

    private void recomputeDimension(ResourceKey<Level> dimension) {
        recomputeCount++;
        SnackControlConfig config = configSupplier.get();
        List<Entry> dimensionEntries = entries.values().stream()
                .filter(entry -> entry.key.dimension().equals(dimension))
                .sorted(ENTRY_KEY_COMPARATOR)
                .toList();

        Map<SnackKey, SnackRuntimeState> newStates = new HashMap<>();
        for (List<Entry> cluster : buildClusters(dimensionEntries)) {
            Comparator<Entry> comparator = ENTRY_ORDER_COMPARATOR;
            if (config.activity.activationOrder == SnackControlConfig.ActivationOrder.NEWEST_FIRST) {
                comparator = ENTRY_NEWEST_ORDER_COMPARATOR;
            }

            List<Entry> ordered = cluster.stream().sorted(comparator).toList();
            int activeCount = Math.min(config.activity.maxActiveSnacksPerCluster, ordered.size());
            long clusterId = clusterId(dimension, ordered);
            for (int index = 0; index < ordered.size(); index++) {
                Entry entry = ordered.get(index);
                SnackStatus status = index < activeCount ? SnackStatus.ACTIVE : SnackStatus.STANDBY;
                int queuePosition = status == SnackStatus.ACTIVE ? 0 : index - activeCount + 1;
                newStates.put(entry.key, new SnackRuntimeState(
                        entry.key,
                        entry.placedBy,
                        entry.placementSequence,
                        status,
                        ordered.size(),
                        activeCount,
                        queuePosition,
                        clusterId
                ));
            }
        }

        for (Entry entry : dimensionEntries) {
            SnackRuntimeState previous = entry.state;
            SnackRuntimeState next = newStates.get(entry.key);
            entry.state = next;
            if (!Objects.equals(previous, next)) {
                listeners.forEach(listener -> listener.onStateChanged(previous, next));
            }
        }
    }

    public record Summary(int trackedSnacks, int activeSnacks, int standbySnacks, int clusters) {
    }

    public record RegistrationResult(SnackRuntimeState state, long placementSequence, boolean sequenceChanged) {
    }

    public record Stats(
            int trackedSnacks,
            int activeSnacks,
            int standbySnacks,
            int clusters,
            int dimensions,
            int largestCluster,
            long recomputes
    ) {
    }

    public record QueueView(
            long clusterId,
            int clusterSize,
            int activeSnacks,
            int standbySnacks,
            int nearestDistanceBlocks,
            List<SnackRuntimeState> states
    ) {
        private static QueueView from(BlockPos origin, List<SnackRuntimeState> states) {
            List<SnackRuntimeState> ordered = states.stream()
                    .sorted(Comparator
                            .comparing((SnackRuntimeState state) -> state.status() == SnackStatus.ACTIVE ? 0 : 1)
                            .thenComparingInt(SnackRuntimeState::queuePosition)
                            .thenComparingLong(SnackRuntimeState::placementSequence))
                    .toList();
            int active = (int) ordered.stream().filter(SnackRuntimeState::active).count();
            int nearestDistance = ordered.stream()
                    .mapToInt(state -> blockDistance(origin, state.key().position()))
                    .min()
                    .orElse(0);
            long clusterId = ordered.isEmpty() ? 0L : ordered.getFirst().clusterId();
            return new QueueView(
                    clusterId,
                    ordered.size(),
                    active,
                    ordered.size() - active,
                    nearestDistance,
                    List.copyOf(ordered)
            );
        }
    }

    public static int blockDistance(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        return (int) Math.floor(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static List<List<Entry>> buildClusters(List<Entry> entries) {
        List<List<Entry>> clusters = new ArrayList<>();
        Set<SnackKey> visited = new HashSet<>();
        Map<GridCell, List<Entry>> spatialIndex = buildSpatialIndex(entries);
        for (Entry seed : entries) {
            if (!visited.add(seed.key)) {
                continue;
            }

            List<Entry> cluster = new ArrayList<>();
            Deque<Entry> pending = new ArrayDeque<>();
            pending.add(seed);

            while (!pending.isEmpty()) {
                Entry current = pending.removeFirst();
                cluster.add(current);
                NativeSnackArea currentArea = area(current);

                for (Entry candidate : nearbyEntries(spatialIndex, current.key.position())) {
                    if (visited.contains(candidate.key)) {
                        continue;
                    }
                    if (currentArea.overlaps(area(candidate))) {
                        visited.add(candidate.key);
                        pending.addLast(candidate);
                    }
                }
            }

            clusters.add(cluster);
        }
        return clusters;
    }

    private long normalizeSequence(SnackKey key, long requestedSequence) {
        if (requestedSequence <= 0L || isSequenceOwnedByAnotherSnack(key, requestedSequence)) {
            return allocateSequence();
        }
        return requestedSequence;
    }

    private boolean isSequenceOwnedByAnotherSnack(SnackKey key, long sequence) {
        return entries.values().stream()
                .anyMatch(entry -> entry.placementSequence == sequence && !entry.key.equals(key));
    }

    private long allocateSequence() {
        return entries.values().stream()
                .mapToLong(entry -> entry.placementSequence)
                .max()
                .orElse(0L) + 1L;
    }

    private static Map<GridCell, List<Entry>> buildSpatialIndex(List<Entry> entries) {
        Map<GridCell, List<Entry>> index = new HashMap<>();
        for (Entry entry : entries) {
            index.computeIfAbsent(GridCell.from(entry.key.position()), ignored -> new ArrayList<>()).add(entry);
        }
        index.values().forEach(bucket -> bucket.sort(ENTRY_KEY_COMPARATOR));
        return index;
    }

    private static List<Entry> nearbyEntries(Map<GridCell, List<Entry>> spatialIndex, BlockPos origin) {
        GridCell originCell = GridCell.from(origin);
        List<Entry> nearby = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    List<Entry> bucket = spatialIndex.get(new GridCell(originCell.x + dx, originCell.y + dy, originCell.z + dz));
                    if (bucket != null) {
                        nearby.addAll(bucket);
                    }
                }
            }
        }
        nearby.sort(ENTRY_KEY_COMPARATOR);
        return nearby;
    }

    private static long clusterId(ResourceKey<Level> dimension, List<Entry> ordered) {
        long hash = 0xcbf29ce484222325L;
        hash = fnv(hash, dimension.location().toString().hashCode());
        for (Entry entry : ordered.stream().sorted(ENTRY_KEY_COMPARATOR).toList()) {
            BlockPos position = entry.key.position();
            hash = fnv(hash, position.getX());
            hash = fnv(hash, position.getY());
            hash = fnv(hash, position.getZ());
        }
        return hash == 0L ? 1L : hash;
    }

    private static long fnv(long hash, int value) {
        long result = hash;
        result ^= value;
        result *= 0x100000001b3L;
        return result;
    }

    private static NativeSnackArea area(Entry entry) {
        return NativeSnackArea.forPokeSnack(entry.key.position());
    }

    private static final class Entry {
        private final SnackKey key;
        private final UUID placedBy;
        private final long placementSequence;
        private SnackRuntimeState state;

        private Entry(SnackKey key, UUID placedBy, long placementSequence) {
            this.key = key;
            this.placedBy = placedBy;
            this.placementSequence = placementSequence;
            this.state = new SnackRuntimeState(key, placedBy, placementSequence, SnackStatus.ACTIVE, 1, 1, 0, placementSequence);
        }
    }

    private record GridCell(int x, int y, int z) {
        private static GridCell from(BlockPos position) {
            return new GridCell(
                    Math.floorDiv(position.getX(), CLUSTER_CELL_SIZE),
                    Math.floorDiv(position.getY(), CLUSTER_CELL_SIZE),
                    Math.floorDiv(position.getZ(), CLUSTER_CELL_SIZE)
            );
        }
    }

    private static final Comparator<Entry> ENTRY_KEY_COMPARATOR = Comparator
            .comparing((Entry entry) -> entry.key.dimension().location().toString())
            .thenComparingInt(entry -> entry.key.position().getX())
            .thenComparingInt(entry -> entry.key.position().getY())
            .thenComparingInt(entry -> entry.key.position().getZ());

    private static final Comparator<Entry> ENTRY_ORDER_COMPARATOR = Comparator
            .comparingLong((Entry entry) -> entry.placementSequence)
            .thenComparing(ENTRY_KEY_COMPARATOR);

    private static final Comparator<Entry> ENTRY_NEWEST_ORDER_COMPARATOR = Comparator
            .comparingLong((Entry entry) -> entry.placementSequence)
            .reversed()
            .thenComparing(ENTRY_KEY_COMPARATOR);
}
