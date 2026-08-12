package org.sawiq.chestdiff.snapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ContainerSnapshot(
        String snapshotId,
        Instant capturedAt,
        long gameTick,
        String title,
        int slotCount,
        List<SlotState> slots
) {
    public ContainerSnapshot {
        snapshotId = Objects.requireNonNullElseGet(snapshotId, () -> UUID.randomUUID().toString());
        Objects.requireNonNull(capturedAt, "capturedAt");
        title = Objects.requireNonNullElse(title, "Container");
        if (slotCount < 0) {
            throw new IllegalArgumentException("Slot count cannot be negative");
        }
        slots = List.copyOf(Objects.requireNonNullElse(slots, List.of()));
        for (SlotState slot : slots) {
            if (slot.index() >= slotCount) {
                throw new IllegalArgumentException("Slot index exceeds snapshot size");
            }
        }
    }

    public static ContainerSnapshot create(long gameTick, String title, int slotCount, List<SlotState> slots) {
        return new ContainerSnapshot(null, Instant.now(), gameTick, title, slotCount, slots);
    }

    public Map<ItemFingerprint, Integer> aggregateTotals() {
        Map<ItemFingerprint, Integer> totals = new HashMap<>();
        for (SlotState slot : slots) {
            totals.merge(slot.fingerprint(), slot.count(), Integer::sum);
        }
        return Collections.unmodifiableMap(totals);
    }

    public Map<Integer, SlotState> slotsByIndex() {
        Map<Integer, SlotState> indexed = new HashMap<>();
        for (SlotState slot : slots) {
            indexed.put(slot.index(), slot);
        }
        return Collections.unmodifiableMap(indexed);
    }

    public List<Integer> slotIndexes(ItemFingerprint fingerprint) {
        List<Integer> indexes = new ArrayList<>();
        for (SlotState slot : slots) {
            if (slot.fingerprint().equals(fingerprint)) {
                indexes.add(slot.index());
            }
        }
        return List.copyOf(indexes);
    }

    public Optional<SlotState> slot(int index) {
        return slots.stream().filter(slot -> slot.index() == index).findFirst();
    }

    public boolean hasSameContents(ContainerSnapshot other) {
        return other != null
                && slotCount == other.slotCount
                && slotsByIndex().equals(other.slotsByIndex());
    }
}
