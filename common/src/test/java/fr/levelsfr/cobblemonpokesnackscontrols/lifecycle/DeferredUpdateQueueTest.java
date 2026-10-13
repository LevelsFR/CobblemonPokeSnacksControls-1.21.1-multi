package fr.levelsfr.cobblemonpokesnackscontrols.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DeferredUpdateQueueTest {
    @Test
    void enqueueDoesNotRunUpdateBeforeFlushAndDeduplicatesIt() {
        DeferredUpdateQueue<String> queue = new DeferredUpdateQueue<>();
        List<String> applied = new ArrayList<>();

        queue.enqueue("legacy-snack");
        queue.enqueue("legacy-snack");

        assertEquals(1, queue.diagnosticPendingCount());
        assertEquals(List.of(), applied);

        queue.flush(applied::add);

        assertEquals(List.of("legacy-snack"), applied);
        assertEquals(0, queue.diagnosticPendingCount());
    }

    @Test
    void updateEnqueuedDuringFlushWaitsForNextFlush() {
        DeferredUpdateQueue<String> queue = new DeferredUpdateQueue<>();
        List<String> applied = new ArrayList<>();
        queue.enqueue("first");

        queue.flush(value -> {
            applied.add(value);
            queue.enqueue("second");
        });

        assertEquals(List.of("first"), applied);
        assertEquals(1, queue.diagnosticPendingCount());

        queue.flush(applied::add);

        assertEquals(List.of("first", "second"), applied);
        assertEquals(0, queue.diagnosticPendingCount());
    }

    @Test
    void clearDropsDeferredUpdates() {
        DeferredUpdateQueue<String> queue = new DeferredUpdateQueue<>();
        List<String> applied = new ArrayList<>();
        queue.enqueue("stale-snack");

        queue.clear();
        queue.flush(applied::add);

        assertEquals(List.of(), applied);
        assertEquals(0, queue.diagnosticPendingCount());
    }
}
