package fr.levelsfr.cobblemonpokesnackscontrols.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Small deduplicating queue for updates that are unsafe in the lifecycle callback
 * where they are discovered.
 *
 * <p>The queue is cleared before consumers run. An update enqueued by a consumer
 * is therefore kept for the next flush instead of being lost or run recursively.</p>
 */
public final class DeferredUpdateQueue<T> {
    private final Set<T> pending = new LinkedHashSet<>();

    public synchronized void enqueue(T value) {
        pending.add(Objects.requireNonNull(value, "value"));
    }

    public void flush(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        List<T> updates;
        synchronized (this) {
            if (pending.isEmpty()) {
                return;
            }
            updates = new ArrayList<>(pending);
            pending.clear();
        }
        updates.forEach(consumer);
    }

    public synchronized void clear() {
        pending.clear();
    }

    synchronized int diagnosticPendingCount() {
        return pending.size();
    }
}
